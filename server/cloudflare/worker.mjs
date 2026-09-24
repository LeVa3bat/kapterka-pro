// KAPTERKA PRO — server-authoritative payment / license / registry backend.
// Cloudflare Worker port of server/yandex-cloud-function.js.
//
// Trust boundary: every privileged credential lives only in Worker secrets.
// The Android app and the site never hold YooKassa, Firebase service-account,
// Telegram, Brevo or admin secrets.
//
// Worker secrets / vars:
//   YOOKASSA_SHOP_ID, YOOKASSA_SECRET_KEY, PAYMENT_AMOUNT_RUB
//   FIREBASE_PROJECT_ID, FIREBASE_SERVICE_ACCOUNT_B64 | FIREBASE_SERVICE_ACCOUNT_JSON
//   ADMIN_API_SECRET_SHA256, ADMIN_SESSION_SECRET
//   BREVO_API_KEY, EMAIL_SENDER_EMAIL, EMAIL_SENDER_NAME (optional)
//   TG_BOT_TOKEN, TG_ADMIN_CHAT_ID (optional, server-side notifications only)
// Optional bindings (wrangler.toml): AUTH_LIMITER, API_LIMITER (Workers rate limiting).

export const SERVICE_NAME = 'kapterka-payment-api';
export const API_VERSION = 2;

const CHECKSUM_CHARS = '23456789ABCDEFGHJKLMNPQRSTUVWXYZ';
const DAY_MS = 24 * 60 * 60 * 1000;
const LICENSE_DURATION_MS = 30 * DAY_MS;
const ADMIN_TOKEN_TTL_MS = 15 * 60 * 1000;
const ADMIN_AUTH_WINDOW_MS = 5 * 60 * 1000;
const ADMIN_AUTH_MAX_FAILURES = 5;
const LICENSE_EMAIL_RATE_LIMIT_MS = 60 * 1000;
const MAX_REQUEST_BODY_BYTES = 64 * 1024;
const UPSTREAM_TIMEOUT_MS = 12000;
// Google Apps Script can take 10-20 s on a cold start.
const MAIL_RELAY_TIMEOUT_MS = 28000;
// Server-only registry collections. Older app versions write to the legacy
// `licenses` / `fighters` collections directly, so nothing there is trusted.
const LICENSES = 'srv_licenses';
const FIGHTERS = 'srv_fighters';
const EMAIL_CODES = 'srv_email_codes';
const EMAIL_CODE_TTL_MS = 10 * 60 * 1000;
const EMAIL_CODE_RESEND_MS = 60 * 1000;
const EMAIL_CODE_MAX_ATTEMPTS = 5;
const EMAIL_CODES_PER_HOUR = 5;
// Read-only: only used to tell an upgrade from a new registration in Telegram.
const LEGACY_FIGHTERS = 'fighters';
const LICENSE_KEY_RE = /^KAPT-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}$/;
const ALLOWED_ORIGINS = new Set(['https://kapterka-pro.ru', 'https://www.kapterka-pro.ru']);

// Per-isolate state. Cloudflare may run several isolates, so the in-memory
// limits are a second line of defence behind the optional rate-limit bindings.
let cachedGoogleToken = { value: '', expiresAt: 0, account: '' };
const adminAuthFailures = new Map();
const licenseEmailLastSentAt = new Map();

// ---------------------------------------------------------------- config

export function getConfig(env = {}) {
  const amount = Number(env.PAYMENT_AMOUNT_RUB || 490);
  return {
    shopId: String(env.YOOKASSA_SHOP_ID || '1450722'),
    secretKey: String(env.YOOKASSA_SECRET_KEY || ''),
    amountRub: Number.isFinite(amount) && amount > 0 ? amount : 490,
    projectId: String(env.FIREBASE_PROJECT_ID || 'kapterka-pro'),
    serviceAccount: readServiceAccount(env),
    adminSecretSha256: String(env.ADMIN_API_SECRET_SHA256 || '').trim().toLowerCase(),
    // Simpler alternative for the owner: one plain ADMIN_PASSWORD secret.
    adminPassword: String(env.ADMIN_PASSWORD || ''),
    adminSessionSecret: String(
      env.ADMIN_SESSION_SECRET ||
      (String(env.ADMIN_PASSWORD || '').length >= 12 ? `kapterka-admin-session|${env.ADMIN_PASSWORD}` : '')
    ),
    brevoKey: String(env.BREVO_API_KEY || ''),
    // Alternative mail route: the owner's Gmail via a Google Apps Script web app.
    mailRelayUrl: /^https:\/\/script\.google\.com\/macros\/s\/[\w-]+\/exec$/.test(String(env.MAIL_RELAY_URL || '').trim())
      ? String(env.MAIL_RELAY_URL).trim()
      : '',
    mailRelaySecret: String(env.MAIL_RELAY_SECRET || '').trim(),
    senderEmail: String(env.EMAIL_SENDER_EMAIL || ''),
    senderName: String(env.EMAIL_SENDER_NAME || 'Каптёрка ПРО'),
    tgBot: String(env.TG_BOT_TOKEN || ''),
    tgChat: String(env.TG_ADMIN_CHAT_ID || '')
  };
}

function readServiceAccount(env) {
  let raw = String(env.FIREBASE_SERVICE_ACCOUNT_JSON || '');
  if (!raw && env.FIREBASE_SERVICE_ACCOUNT_B64) {
    try {
      raw = new TextDecoder().decode(base64ToBytes(String(env.FIREBASE_SERVICE_ACCOUNT_B64)));
    } catch (_) {
      raw = '';
    }
  }
  if (!raw) return null;
  try {
    const parsed = JSON.parse(raw);
    return parsed.client_email && parsed.private_key ? parsed : null;
  } catch (_) {
    return null;
  }
}

// ---------------------------------------------------------------- helpers

function corsHeaders(request) {
  const origin = request.headers.get('Origin') || '';
  const headers = {
    'Access-Control-Allow-Headers': 'Content-Type',
    'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
    Vary: 'Origin'
  };
  if (ALLOWED_ORIGINS.has(origin)) headers['Access-Control-Allow-Origin'] = origin;
  return headers;
}

function json(request, status, body, extra = {}) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      'Content-Type': 'application/json; charset=utf-8',
      'Cache-Control': 'no-store',
      'X-Content-Type-Options': 'nosniff',
      'Referrer-Policy': 'no-referrer',
      ...corsHeaders(request),
      ...extra
    }
  });
}

export function cleanText(value, max = 160) {
  return String(value ?? '').replace(/[\u0000-\u001F\u007F]/g, ' ').trim().slice(0, max);
}

export function cleanEmail(value) {
  const email = cleanText(value, 160).toLowerCase();
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) ? email : '';
}

function escapeHtml(value) {
  return String(value ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

function maskUnitKey(value) {
  const clean = String(value || '').trim();
  if (!clean) return 'Не указан';
  if (clean.length <= 5) return '***';
  return clean.slice(0, 5) + '***' + clean.slice(-2);
}

function moscowTime(ms) {
  // UTC+3 without relying on Intl time-zone data.
  const d = new Date(ms + 3 * 60 * 60 * 1000);
  const pad = (n) => String(n).padStart(2, '0');
  return `${pad(d.getUTCDate())}.${pad(d.getUTCMonth() + 1)}.${d.getUTCFullYear()} ${pad(d.getUTCHours())}:${pad(d.getUTCMinutes())}`;
}

function maskKey(key) {
  const parts = String(key || '').split('-');
  return parts.length === 4 ? `${parts[0]}-****-****-${parts[3]}` : '****';
}

function bytesToHex(bytes) {
  return Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('');
}

function base64ToBytes(b64) {
  const bin = atob(b64.replace(/\s+/g, ''));
  const out = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) out[i] = bin.charCodeAt(i);
  return out;
}

function base64url(input) {
  const bytes = typeof input === 'string' ? new TextEncoder().encode(input) : input;
  let bin = '';
  for (const b of bytes) bin += String.fromCharCode(b);
  return btoa(bin).replace(/=+$/, '').replace(/\+/g, '-').replace(/\//g, '_');
}

async function sha256Hex(text) {
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(String(text)));
  return bytesToHex(new Uint8Array(digest));
}

async function hmacHex(secret, text) {
  const key = await crypto.subtle.importKey(
    'raw', new TextEncoder().encode(secret), { name: 'HMAC', hash: 'SHA-256' }, false, ['sign']
  );
  const sig = await crypto.subtle.sign('HMAC', key, new TextEncoder().encode(text));
  return bytesToHex(new Uint8Array(sig));
}

function constantTimeEqual(a, b) {
  const x = String(a);
  const y = String(b);
  let diff = x.length ^ y.length;
  const len = Math.max(x.length, y.length);
  for (let i = 0; i < len; i++) diff |= (x.charCodeAt(i) | 0) ^ (y.charCodeAt(i) | 0);
  return diff === 0;
}

function randomHex(bytes) {
  const buf = new Uint8Array(bytes);
  crypto.getRandomValues(buf);
  return bytesToHex(buf);
}

async function fetchWithTimeout(url, init = {}, timeoutMs = UPSTREAM_TIMEOUT_MS) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    return await fetch(url, { ...init, signal: controller.signal });
  } finally {
    clearTimeout(timer);
  }
}

// ---------------------------------------------------------------- license keys

export function computeKeyChecksum(p1, p2) {
  const s = `KAPT-${p1}-${p2}-KAPT3RKA_881_MILITARY`;
  let h1 = 0x811c9dc5 >>> 0;
  let h2 = 0x5a2d1e39 >>> 0;
  for (let i = 0; i < s.length; i++) {
    const code = s.charCodeAt(i);
    h1 = Math.imul(h1 ^ code, 0x01000193) >>> 0;
    h2 = (Math.imul(h2 + code, 31) + 0x45) >>> 0;
  }
  return (
    CHECKSUM_CHARS[(h1 >>> 24) & 0x1f] +
    CHECKSUM_CHARS[(h1 >>> 16) & 0x1f] +
    CHECKSUM_CHARS[(h2 >>> 24) & 0x1f] +
    CHECKSUM_CHARS[(h2 >>> 16) & 0x1f]
  );
}

// Deterministic: the same succeeded payment always maps to the same key.
// Must stay identical to the key issued by the 3.5.0 backend.
export async function keyForPayment(paymentId) {
  const normalized = String(paymentId || '').toUpperCase().replace(/[^A-Z0-9]/g, '');
  const seed = (await sha256Hex(normalized)).toUpperCase();
  const p1 = seed.slice(0, 4);
  const p2 = seed.slice(4, 8);
  return `KAPT-${p1}-${p2}-${computeKeyChecksum(p1, p2)}`;
}

function generateAdminLicenseKey() {
  const buf = new Uint8Array(8);
  crypto.getRandomValues(buf);
  const pick = (i) => CHECKSUM_CHARS[buf[i] & 0x1f];
  const p1 = pick(0) + pick(1) + pick(2) + pick(3);
  const p2 = pick(4) + pick(5) + pick(6) + pick(7);
  return `KAPT-${p1}-${p2}-${computeKeyChecksum(p1, p2)}`;
}

// ---------------------------------------------------------------- upstreams

async function sendTelegram(cfg, text) {
  if (!cfg.tgBot || !cfg.tgChat) return false;
  try {
    const r = await fetchWithTimeout(`https://api.telegram.org/bot${cfg.tgBot}/sendMessage`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ chat_id: cfg.tgChat, text, parse_mode: 'HTML' })
    });
    return r.ok;
  } catch (_) {
    return false;
  }
}

async function requestYooKassa(cfg, method, apiPath, data = null, idempotenceKey = null) {
  if (!cfg.secretKey) throw new Error('YooKassa is not configured');
  const headers = {
    Authorization: 'Basic ' + btoa(`${cfg.shopId}:${cfg.secretKey}`),
    'Idempotence-Key': idempotenceKey || crypto.randomUUID()
  };
  if (data) headers['Content-Type'] = 'application/json';
  const r = await fetchWithTimeout('https://api.yookassa.ru' + apiPath, {
    method,
    headers,
    body: data ? JSON.stringify(data) : undefined
  });
  const parsed = await r.json().catch(() => ({}));
  if (!r.ok) {
    const error = new Error(`YooKassa HTTP ${r.status}`);
    error.status = r.status;
    throw error;
  }
  return parsed;
}

async function importPrivateKey(pem) {
  const body = String(pem)
    .replace(/-----BEGIN PRIVATE KEY-----/, '')
    .replace(/-----END PRIVATE KEY-----/, '')
    .replace(/\s+/g, '');
  return crypto.subtle.importKey(
    'pkcs8', base64ToBytes(body), { name: 'RSASSA-PKCS1-v1_5', hash: 'SHA-256' }, false, ['sign']
  );
}

async function googleAccessToken(cfg) {
  const now = Date.now();
  const account = cfg.serviceAccount;
  if (!account) throw new Error('Firebase service account is not configured');
  if (
    cachedGoogleToken.value &&
    cachedGoogleToken.account === account.client_email &&
    cachedGoogleToken.expiresAt > now + 60000
  ) {
    return cachedGoogleToken.value;
  }
  const iat = Math.floor(now / 1000);
  const unsigned =
    base64url(JSON.stringify({ alg: 'RS256', typ: 'JWT' })) + '.' +
    base64url(JSON.stringify({
      iss: account.client_email,
      scope: 'https://www.googleapis.com/auth/datastore',
      aud: 'https://oauth2.googleapis.com/token',
      iat,
      exp: iat + 3600
    }));
  const key = await importPrivateKey(account.private_key);
  const sig = new Uint8Array(await crypto.subtle.sign('RSASSA-PKCS1-v1_5', key, new TextEncoder().encode(unsigned)));
  const r = await fetchWithTimeout('https://oauth2.googleapis.com/token', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      grant_type: 'urn:ietf:params:oauth:grant-type:jwt-bearer',
      assertion: `${unsigned}.${base64url(sig)}`
    }).toString()
  });
  const parsed = await r.json().catch(() => ({}));
  if (!r.ok || !parsed.access_token) throw new Error('Google OAuth token request failed');
  cachedGoogleToken = {
    value: parsed.access_token,
    account: account.client_email,
    expiresAt: now + Math.max(300, Number(parsed.expires_in || 3600) - 60) * 1000
  };
  return cachedGoogleToken.value;
}

function toFirestoreValue(value) {
  if (typeof value === 'number') return { integerValue: String(Math.trunc(value)) };
  if (typeof value === 'boolean') return { booleanValue: value };
  return { stringValue: String(value ?? '') };
}

function fromFirestoreFields(fields = {}) {
  const out = {};
  for (const [name, field] of Object.entries(fields)) {
    if ('stringValue' in field) out[name] = field.stringValue;
    else if ('integerValue' in field) out[name] = Number(field.integerValue);
    else if ('doubleValue' in field) out[name] = Number(field.doubleValue);
    else if ('booleanValue' in field) out[name] = Boolean(field.booleanValue);
    else if ('timestampValue' in field) out[name] = Date.parse(field.timestampValue) || 0;
  }
  return out;
}

class Firestore {
  constructor(cfg) {
    this.cfg = cfg;
    this.base =
      `https://firestore.googleapis.com/v1/projects/${encodeURIComponent(cfg.projectId)}` +
      '/databases/(default)/documents';
  }

  async request(method, path, body) {
    const token = await googleAccessToken(this.cfg);
    const headers = { Authorization: `Bearer ${token}` };
    if (body) headers['Content-Type'] = 'application/json';
    return fetchWithTimeout(this.base + path, {
      method,
      headers,
      body: body ? JSON.stringify(body) : undefined
    });
  }

  // `collection` may be a nested path such as "units/<key>/members".
  collPath(collection) {
    return '/' + String(collection).split('/').map(encodeURIComponent).join('/');
  }

  docPath(collection, id) {
    return `${this.collPath(collection)}/${encodeURIComponent(id)}`;
  }

  async hasAnyDocument(collection) {
    const r = await this.request('GET', `${this.collPath(collection)}?pageSize=1`);
    if (r.status === 404) return false;
    if (!r.ok) throw new Error(`Firestore list failed: HTTP ${r.status}`);
    const parsed = await r.json();
    return Array.isArray(parsed.documents) && parsed.documents.length > 0;
  }

  async get(collection, id) {
    const r = await this.request('GET', this.docPath(collection, id));
    if (r.status === 404) return null;
    if (!r.ok) throw new Error(`Firestore read failed: HTTP ${r.status}`);
    const doc = await r.json();
    return { id, ...fromFirestoreFields(doc.fields) };
  }

  // Merge-patch the given fields. `mustExist`: true → update only, false → create only.
  async patch(collection, id, data, { mustExist } = {}) {
    const fields = {};
    const params = [];
    for (const [key, value] of Object.entries(data)) {
      fields[key] = toFirestoreValue(value);
      params.push('updateMask.fieldPaths=' + encodeURIComponent(key));
    }
    if (mustExist === true) params.push('currentDocument.exists=true');
    if (mustExist === false) params.push('currentDocument.exists=false');
    const r = await this.request('PATCH', `${this.docPath(collection, id)}?${params.join('&')}`, { fields });
    if (mustExist === false && !r.ok) {
      const detail = await r.json().catch(() => ({}));
      const reason = String(detail?.error?.status || '');
      if (r.status === 409 || reason === 'ALREADY_EXISTS' || reason === 'FAILED_PRECONDITION') {
        const err = new Error('ALREADY_EXISTS');
        err.code = 'ALREADY_EXISTS';
        throw err;
      }
    }
    if (!r.ok) throw new Error(`Firestore write failed: HTTP ${r.status}`);
    return true;
  }

  async remove(collection, id) {
    const r = await this.request('DELETE', this.docPath(collection, id));
    if (!r.ok && r.status !== 404) throw new Error(`Firestore delete failed: HTTP ${r.status}`);
    return true;
  }

  async queryEqual(collection, field, value, limit = 20) {
    const r = await this.request('POST', ':runQuery', {
      structuredQuery: {
        from: [{ collectionId: collection }],
        where: { fieldFilter: { field: { fieldPath: field }, op: 'EQUAL', value: { stringValue: value } } },
        limit
      }
    });
    if (!r.ok) throw new Error(`Firestore query failed: HTTP ${r.status}`);
    const rows = await r.json();
    return (rows || [])
      .map((row) => row.document)
      .filter(Boolean)
      .map((doc) => ({
        id: decodeURIComponent(String(doc.name || '').split('/').pop() || ''),
        ...fromFirestoreFields(doc.fields)
      }));
  }

  // Every document of a collection id anywhere in the database (no filter, no index needed).
  async collectionGroup(collectionId, limit = 5000) {
    const r = await this.request('POST', ':runQuery', {
      structuredQuery: { from: [{ collectionId, allDescendants: true }], limit }
    });
    if (!r.ok) throw new Error(`Firestore group query failed: HTTP ${r.status}`);
    const rows = await r.json();
    return (rows || []).filter((row) => row.document).map((row) => {
      const parts = String(row.document.name || '').split('/documents/')[1]?.split('/') || [];
      return { path: parts, updateTime: Date.parse(row.document.updateTime) || 0, ...fromFirestoreFields(row.document.fields) };
    });
  }

  async list(collection, pageSize = 300) {
    const out = [];
    let pageToken = '';
    for (let page = 0; page < 20; page++) {
      const qs = `?pageSize=${pageSize}` + (pageToken ? `&pageToken=${encodeURIComponent(pageToken)}` : '');
      const r = await this.request('GET', `${this.collPath(collection)}${qs}`);
      if (!r.ok) throw new Error(`Firestore list failed: HTTP ${r.status}`);
      const parsed = await r.json();
      for (const doc of parsed.documents || []) {
        out.push({
          id: decodeURIComponent(String(doc.name || '').split('/').pop() || ''),
          ...fromFirestoreFields(doc.fields)
        });
      }
      pageToken = parsed.nextPageToken || '';
      if (!pageToken) break;
    }
    return out;
  }
}

function emailHtml(title, lines, highlight) {
  const esc = (v) => escapeHtml(v).replace(/"/g, '&quot;');
  const body = lines.map((l) => `<p style="margin:0 0 12px;color:#334155;font-size:15px;line-height:1.5">${esc(l)}</p>`).join('');
  const box = highlight
    ? `<div style="margin:18px 0;padding:16px;border-radius:14px;background:#0f2a1f;color:#6ee7b7;font:700 26px/1.2 monospace;letter-spacing:3px;text-align:center">${esc(highlight)}</div>`
    : '';
  return `<!doctype html><html><body style="margin:0;background:#f1f5f9;font-family:Segoe UI,Roboto,Arial,sans-serif">
<div style="max-width:520px;margin:24px auto;background:#fff;border-radius:18px;overflow:hidden;border:1px solid #e2e8f0">
<div style="padding:20px 24px;background:linear-gradient(135deg,#1f7a57,#0f766e);color:#fff;font-weight:800;font-size:20px">Каптёрка ПРО</div>
<div style="padding:24px"><h2 style="margin:0 0 14px;color:#0f172a;font-size:19px">${esc(title)}</h2>${body}${box}
<p style="margin:18px 0 0;color:#94a3b8;font-size:12px">Если вы не запрашивали это письмо, просто удалите его. Сайт: https://kapterka-pro.ru/</p></div></div></body></html>`;
}

export function emailReady(cfg) {
  return Boolean((cfg.mailRelayUrl && cfg.mailRelaySecret.length >= 16) || (cfg.brevoKey && cfg.senderEmail));
}

async function sendEmail(cfg, { toEmail, toName, subject, title, lines, highlight }) {
  if (!emailReady(cfg)) throw new Error('Email provider is not configured');
  const text = [title, '', ...lines, ...(highlight ? ['', highlight] : []), '', 'https://kapterka-pro.ru/'].join('\n');
  if (cfg.mailRelayUrl && cfg.mailRelaySecret.length >= 16) {
    const r = await fetchWithTimeout(cfg.mailRelayUrl, {
      method: 'POST',
      headers: { 'Content-Type': 'text/plain;charset=utf-8' },
      body: JSON.stringify({
        secret: cfg.mailRelaySecret,
        to: toEmail,
        name: toName || '',
        subject,
        text,
        html: emailHtml(title, lines, highlight)
      }),
      redirect: 'follow'
    }, MAIL_RELAY_TIMEOUT_MS);
    const raw = await r.text().catch(() => '');
    let parsed = {};
    try { parsed = JSON.parse(raw); } catch (_) { /* HTML login page etc. */ }
    if (!r.ok || parsed.ok !== true) {
      const reason = parsed.error || (/<html/i.test(raw) ? 'RELAY_NOT_PUBLIC' : `HTTP_${r.status}`);
      const err = new Error(`Mail relay failed: HTTP ${r.status} ${reason}`);
      err.reason = reason;
      throw err;
    }
    return true;
  }
  const r = await fetchWithTimeout('https://api.brevo.com/v3/smtp/email', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'api-key': cfg.brevoKey },
    body: JSON.stringify({
      sender: { name: cfg.senderName, email: cfg.senderEmail },
      to: [{ email: toEmail, name: toName || 'Пользователь' }],
      subject,
      textContent: text,
      htmlContent: emailHtml(title, lines, highlight)
    })
  });
  if (!r.ok) throw new Error(`Brevo HTTP ${r.status}`);
  return true;
}

function mailReason(error) {
  const r = String(error?.reason || '');
  return /^[A-Z0-9_]{2,40}$/.test(r) ? r : 'SEND_FAILED';
}

async function sendLicenseEmail(cfg, { toEmail, callsign, licenseKey, days }) {
  return sendEmail(cfg, {
    toEmail,
    toName: callsign,
    subject: `Ваш лицензионный ключ «Каптёрка ПРО» (${days} дн.)`,
    title: `Здравствуйте, ${callsign || 'пользователь'}!`,
    lines: [
      'Спасибо за оплату. Ваш лицензионный ключ «Каптёрка ПРО»:',
      `Срок действия: ${days} суток. Ключ привязан к вашему устройству — сохраните это письмо.`
    ],
    highlight: licenseKey
  });
}

// ---------------------------------------------------------------- domain

function normalizeLicense(doc) {
  if (!doc) return null;
  return {
    licenseKey: String(doc.licenseKey || doc.id || ''),
    fighterId: String(doc.fighterId || ''),
    callsign: String(doc.callsign || ''),
    email: String(doc.email || ''),
    paymentId: String(doc.paymentId || ''),
    expiresAt: Number(doc.expiresAt || 0),
    status: String(doc.status || '')
  };
}

function normalizeFighter(doc) {
  if (!doc) return null;
  return {
    id: String(doc.fighterId || doc.id || ''),
    callsign: String(doc.callsign || ''),
    role: String(doc.role || 'Старшина подразделения'),
    unitName: String(doc.unitName || ''),
    unitKey: String(doc.unitKey || ''),
    licenseKey: String(doc.licenseKey || ''),
    expiresAt: Number(doc.expiresAt || 0),
    registeredAt: Number(doc.registeredAt || 0),
    lastSeenAt: Number(doc.lastSeenAt || 0),
    email: String(doc.email || ''),
    deviceModel: String(doc.deviceModel || '')
  };
}

// A registry entry is a real user once any licence is tied to it.
export function fighterHasLicense(fighter, licenses = []) {
  if (!fighter) return false;
  if (fighter.licenseKey || fighter.expiresAt > 0) return true;
  const email = cleanEmail(fighter.email);
  return licenses.some((l) => (l.fighterId && l.fighterId === fighter.id) || (email && cleanEmail(l.email) === email));
}

function isActive(license, now = Date.now()) {
  return Boolean(license && license.status === 'ACTIVE' && license.expiresAt > now);
}

async function activeLicenseByEmail(db, email, fighterId = '') {
  const rows = (await db.queryEqual(LICENSES, 'email', email, 20)).map(normalizeLicense);
  return rows
    .filter((l) => isActive(l) && (!fighterId || l.fighterId === fighterId))
    .sort((a, b) => b.expiresAt - a.expiresAt)[0] || null;
}

export function amountMatchesTariff(payment, amountRub) {
  const currency = String(payment?.amount?.currency || '').toUpperCase();
  const value = Number(payment?.amount?.value || 0);
  return currency === 'RUB' && Math.abs(value - amountRub) < 0.001;
}

function paymentTimestampMillis(payment) {
  const parsed = Date.parse(payment?.captured_at || payment?.created_at || '');
  return Number.isFinite(parsed) ? parsed : Date.now();
}

// ---------------------------------------------------------------- Firebase ID tokens

const FIREBASE_JWKS_URL =
  'https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com';
let cachedJwks = { keys: null, expiresAt: 0 };

function decodeJwtPart(part) {
  const b64 = part.replace(/-/g, '+').replace(/_/g, '/');
  return JSON.parse(new TextDecoder().decode(base64ToBytes(b64 + '==='.slice((b64.length + 3) % 4))));
}

async function firebaseJwks() {
  const now = Date.now();
  if (cachedJwks.keys && cachedJwks.expiresAt > now) return cachedJwks.keys;
  const r = await fetchWithTimeout(FIREBASE_JWKS_URL);
  if (!r.ok) throw new Error(`JWKS HTTP ${r.status}`);
  const parsed = await r.json();
  const maxAge = Number((/max-age=(\d+)/.exec(r.headers.get('Cache-Control') || '') || [])[1] || 3600);
  cachedJwks = { keys: parsed.keys || [], expiresAt: now + Math.min(maxAge, 6 * 3600) * 1000 };
  return cachedJwks.keys;
}

// Returns the Firebase uid for a valid ID token of this project, or '' otherwise.
export async function verifyFirebaseIdToken(cfg, token) {
  const parts = String(token || '').split('.');
  if (parts.length !== 3 || token.length > 4096) return '';
  let header;
  let claims;
  try {
    header = decodeJwtPart(parts[0]);
    claims = decodeJwtPart(parts[1]);
  } catch (_) {
    return '';
  }
  if (header.alg !== 'RS256' || !header.kid) return '';
  const nowSec = Math.floor(Date.now() / 1000);
  if (
    claims.aud !== cfg.projectId ||
    claims.iss !== `https://securetoken.google.com/${cfg.projectId}` ||
    !(Number(claims.exp) > nowSec) ||
    !(Number(claims.iat) <= nowSec + 300) ||
    typeof claims.sub !== 'string' || !claims.sub || claims.sub.length > 128
  ) {
    return '';
  }
  const jwk = (await firebaseJwks()).find((k) => k.kid === header.kid);
  if (!jwk) return '';
  const key = await crypto.subtle.importKey(
    'jwk', { kty: jwk.kty, n: jwk.n, e: jwk.e, alg: 'RS256', ext: true },
    { name: 'RSASSA-PKCS1-v1_5', hash: 'SHA-256' }, false, ['verify']
  );
  const sig = base64ToBytes(parts[2].replace(/-/g, '+').replace(/_/g, '/') + '==='.slice((parts[2].length + 3) % 4));
  const valid = await crypto.subtle.verify(
    'RSASSA-PKCS1-v1_5', key, sig, new TextEncoder().encode(`${parts[0]}.${parts[1]}`)
  );
  return valid ? claims.sub : '';
}

// Unit keys are Firestore document ids chosen by clients. Legacy keys are short.
const UNIT_KEY_RE = /^[A-Za-z0-9_\-Ѐ-ӿ]{3,120}$/;
const UNIT_DATA_COLLECTIONS = ['devices', 'warehouse_points', 'stock_records', 'operation_records', 'inventory_items'];

// ---------------------------------------------------------------- admin session

async function adminSecretMatches(cfg, secret) {
  const expected = /^[a-f0-9]{64}$/.test(cfg.adminSecretSha256)
    ? cfg.adminSecretSha256
    : cfg.adminPassword.length >= 12 ? await sha256Hex(cfg.adminPassword) : '';
  if (!expected) return false;
  return constantTimeEqual(await sha256Hex(String(secret || '')), expected);
}

async function issueAdminToken(cfg) {
  if (!cfg.adminSessionSecret) return '';
  const payload = `${Date.now() + ADMIN_TOKEN_TTL_MS}.${randomHex(12)}`;
  return `${payload}.${await hmacHex(cfg.adminSessionSecret, payload)}`;
}

export async function verifyAdminToken(cfg, token) {
  if (!cfg.adminSessionSecret) return false;
  const parts = String(token || '').split('.');
  if (parts.length !== 3) return false;
  const [exp, nonce, signature] = parts;
  const expiresAt = Number(exp);
  if (!Number.isFinite(expiresAt) || expiresAt <= Date.now() || nonce.length < 12 || !/^[a-f0-9]{64}$/i.test(signature)) {
    return false;
  }
  return constantTimeEqual(await hmacHex(cfg.adminSessionSecret, `${exp}.${nonce}`), signature.toLowerCase());
}

function clientIp(request) {
  return cleanText(request.headers.get('CF-Connecting-IP') || 'unknown', 64);
}

function adminAuthBlocked(ip, now = Date.now()) {
  for (const [key, entry] of adminAuthFailures) {
    if (now - entry.windowStartedAt >= ADMIN_AUTH_WINDOW_MS) adminAuthFailures.delete(key);
  }
  const entry = adminAuthFailures.get(ip);
  return Boolean(entry && entry.failures >= ADMIN_AUTH_MAX_FAILURES);
}

function recordAdminAuthFailure(ip, now = Date.now()) {
  const entry = adminAuthFailures.get(ip);
  if (!entry || now - entry.windowStartedAt >= ADMIN_AUTH_WINDOW_MS) {
    adminAuthFailures.set(ip, { failures: 1, windowStartedAt: now });
  } else {
    entry.failures += 1;
  }
}

async function bindingAllows(binding, key) {
  if (!binding || typeof binding.limit !== 'function') return true;
  try {
    const { success } = await binding.limit({ key });
    return success !== false;
  } catch (_) {
    return true;
  }
}

// ---------------------------------------------------------------- handlers

const handlers = {
  async health(ctx) {
    const { cfg } = ctx;
    return ctx.ok({
      ok: true,
      service: SERVICE_NAME,
      apiVersion: API_VERSION,
      secretConfigured: Boolean(cfg.secretKey),
      licenseRegistryConfigured: Boolean(cfg.serviceAccount),
      adminAuthConfigured: /^[a-f0-9]{64}$/.test(cfg.adminSecretSha256) || cfg.adminPassword.length >= 12,
      adminSessionConfigured: Boolean(cfg.adminSessionSecret),
      emailConfigured: emailReady(cfg),
      telegramConfigured: Boolean(cfg.tgBot && cfg.tgChat)
    });
  },

  // Read-only check of the mail relay: never sends a letter. GET hits doGet,
  // POST with a wrong secret must come back as FORBIDDEN JSON.
  async mail_diag(ctx) {
    const { cfg, ip } = ctx;
    if (!(await bindingAllows(ctx.env.AUTH_LIMITER, 'diag:' + ip))) return ctx.fail(429, 'RATE_LIMITED');
    const out = {
      ok: true,
      relay_url_valid: Boolean(cfg.mailRelayUrl),
      relay_secret_len_ok: cfg.mailRelaySecret.length >= 16,
      brevo: Boolean(cfg.brevoKey && cfg.senderEmail)
    };
    if (!cfg.mailRelayUrl) return ctx.ok(out);
    const probe = async (init) => {
      const t0 = Date.now();
      try {
        const r = await fetchWithTimeout(cfg.mailRelayUrl, { redirect: 'follow', ...init }, MAIL_RELAY_TIMEOUT_MS);
        const raw = await r.text();
        let j = null;
        try { j = JSON.parse(raw); } catch (_) {}
        return { status: r.status, ms: Date.now() - t0, json: Boolean(j), ok: j?.ok ?? null, error: j?.error ?? null, html: /<html/i.test(raw) };
      } catch (e) {
        return { status: 0, ms: Date.now() - t0, failed: String(e?.name || 'Error') };
      }
    };
    const post = (secret) => probe({
      method: 'POST',
      headers: { 'Content-Type': 'text/plain;charset=utf-8' },
      // "nobody@invalid" fails the relay's address check, so nothing is sent.
      body: JSON.stringify({ secret, to: 'nobody@invalid', subject: 'diag', text: 'diag' })
    });
    out.post_wrong_secret = await post('diagnostic-wrong-secret');
    // INVALID_EMAIL here means the secret matches; FORBIDDEN means it does not.
    out.post_real_secret = await post(cfg.mailRelaySecret);
    out.secret_matches = out.post_real_secret.error === 'INVALID_EMAIL';
    return ctx.ok(out);
  },

  async fighter_upsert(ctx) {
    const { body, db } = ctx;
    const fighterId = cleanText(body.fighter_id, 100);
    const callsign = cleanText(body.callsign || 'Боец', 80);
    const unitName = cleanText(body.unit_name || 'Подразделение', 120);
    const unitKey = cleanText(body.unit_key, 120);
    const email = cleanEmail(body.email);
    const deviceModel = cleanText(body.device_model || 'Android', 120);
    if (!fighterId) return ctx.fail(400, 'MISSING_FIGHTER_ID');

    const now = Date.now();
    const existing = normalizeFighter(await db.get(FIGHTERS, fighterId));
    if (existing) {
      const storedEmail = cleanEmail(existing.email);
      if (storedEmail && email && storedEmail === email) {
        // Profile metadata only. License fields are never accepted from a client.
        await db.patch(FIGHTERS, fighterId, { callsign, unitName, unitKey, lastSeenAt: now, deviceModel });
        return ctx.ok({ ok: true, existing: true, profile_updated: true });
      }
      await db.patch(FIGHTERS, fighterId, { lastSeenAt: now, deviceModel });
      return ctx.ok({ ok: true, existing: true, profile_updated: false });
    }

    let active = null;
    if (email) {
      try {
        active = await activeLicenseByEmail(db, email, fighterId);
      } catch (_) {
        active = null;
      }
    }
    await db.patch(FIGHTERS, fighterId, {
      fighterId,
      callsign,
      role: 'Старшина подразделения',
      unitName,
      unitKey,
      email,
      deviceModel,
      registeredAt: now,
      lastSeenAt: now,
      licenseKey: active?.licenseKey || '',
      expiresAt: active?.expiresAt || 0,
      isProActive: isActive(active, now)
    });
    // Users coming from ≤3.5.0 already exist in the legacy registry: report
    // them as an upgrade, not as a brand-new registration.
    let upgraded = false;
    try {
      upgraded = Boolean(await db.get(LEGACY_FIGHTERS, fighterId));
    } catch (_) {
      upgraded = false;
    }
    await sendTelegram(ctx.cfg,
      (upgraded
        ? '🔄 <b>Пользователь перешёл на новую версию «Каптёрки»</b>\n\n'
        : '🎖 <b>Новая регистрация в приложении «Каптёрка»!</b>\n\n') +
      '👤 <b>Позывной:</b> ' + escapeHtml(callsign) + '\n' +
      '🏢 <b>Подразделение:</b> ' + escapeHtml(unitName) + '\n' +
      '🔑 <b>Ключ канала:</b> <code>' + escapeHtml(maskUnitKey(unitKey)) + '</code>\n' +
      '📧 <b>Email:</b> ' + escapeHtml(email || 'Не указан') + '\n' +
      '📅 <b>Время:</b> ' + escapeHtml(moscowTime(now)) + ' (МСК)\n' +
      '📱 <b>Платформа:</b> Android App (' + escapeHtml(deviceModel) + ')'
    );
    return ctx.ok({ ok: true, existing: false });
  },

  async fighter_lookup(ctx) {
    const { body, db } = ctx;
    const fighterId = cleanText(body.fighter_id, 100);
    const email = cleanEmail(body.email);
    if (!fighterId) return ctx.fail(400, 'MISSING_FIGHTER_ID');
    if (!email) return ctx.fail(400, 'INVALID_EMAIL');
    if (!(await ctx.limit('lookup:' + ctx.ip))) return ctx.fail(429, 'RATE_LIMITED');

    const fighter = normalizeFighter(await db.get(FIGHTERS, fighterId));
    if (!fighter || fighter.id !== fighterId) return ctx.fail(404, 'FIGHTER_NOT_FOUND');
    const storedEmail = cleanEmail(fighter.email);
    if (!storedEmail || storedEmail !== email) return ctx.fail(403, 'FIGHTER_IDENTITY_MISMATCH');
    return ctx.ok({ ok: true, fighter: { id: fighter.id, unit_name: fighter.unitName, unit_key: fighter.unitKey } });
  },

  // Registration: send a 6-digit confirmation code to the e-mail.
  async email_code_send(ctx) {
    const { body, db, cfg, ip } = ctx;
    const email = cleanEmail(body.email);
    const fighterId = cleanText(body.fighter_id, 100);
    if (!email) return ctx.fail(400, 'INVALID_EMAIL');
    if (!fighterId) return ctx.fail(400, 'MISSING_FIGHTER_ID');
    if (!emailReady(cfg)) return ctx.fail(503, 'EMAIL_PROVIDER_UNAVAILABLE');
    if (!(await bindingAllows(ctx.env.AUTH_LIMITER, 'mail:' + ip))) return ctx.fail(429, 'RATE_LIMITED', { retry_after_seconds: 60 });

    const id = await sha256Hex(email);
    const now = Date.now();
    const prev = await db.get(EMAIL_CODES, id);
    if (prev && now - Number(prev.sentAt || 0) < EMAIL_CODE_RESEND_MS) {
      return ctx.fail(429, 'EMAIL_CODE_TOO_SOON', {
        retry_after_seconds: Math.ceil((EMAIL_CODE_RESEND_MS - (now - Number(prev.sentAt))) / 1000)
      });
    }
    const hourStart = prev && now - Number(prev.hourStart || 0) < 3600_000 ? Number(prev.hourStart) : now;
    const hourCount = prev && hourStart === Number(prev.hourStart) ? Number(prev.hourCount || 0) : 0;
    if (hourCount >= EMAIL_CODES_PER_HOUR) return ctx.fail(429, 'EMAIL_CODE_HOURLY_LIMIT', { retry_after_seconds: 3600 });

    const buf = new Uint32Array(1);
    crypto.getRandomValues(buf);
    const code = String(100000 + (buf[0] % 900000));
    await db.patch(EMAIL_CODES, id, {
      codeHash: await sha256Hex(`${code}|${email}|${fighterId}`),
      fighterId,
      expiresAt: now + EMAIL_CODE_TTL_MS,
      attempts: 0,
      sentAt: now,
      hourStart,
      hourCount: hourCount + 1
    });
    try {
      await sendEmail(cfg, {
        toEmail: email,
        subject: `Код подтверждения «Каптёрка ПРО»: ${code}`,
        title: `Ваш код: ${code}`,
        lines: [
          `Код подтверждения почты: ${code}`,
          'Введите эти 6 цифр в приложении «Каптёрка ПРО», чтобы завершить регистрацию. Код действует 10 минут.'
        ],
        highlight: code
      });
    } catch (error) {
      console.error('Code email error:', error?.message || error);
      // The code was not delivered: let the user ask again right away.
      await db.patch(EMAIL_CODES, id, { sentAt: 0 }).catch(() => {});
      return ctx.fail(503, 'EMAIL_PROVIDER_UNAVAILABLE', { reason: mailReason(error) });
    }
    return ctx.ok({ ok: true, expires_in_seconds: EMAIL_CODE_TTL_MS / 1000 });
  },

  // Registration: check the code; on success the e-mail is verified and a
  // welcome letter with the unit key is sent.
  async email_code_verify(ctx) {
    const { body, db, cfg } = ctx;
    const email = cleanEmail(body.email);
    const fighterId = cleanText(body.fighter_id, 100);
    const code = cleanText(body.code, 12).replace(/\D/g, '');
    if (!email || !fighterId || code.length !== 6) return ctx.fail(400, 'INVALID_CODE');

    const id = await sha256Hex(email);
    const rec = await db.get(EMAIL_CODES, id);
    const now = Date.now();
    if (!rec || Number(rec.expiresAt || 0) < now) return ctx.fail(410, 'CODE_EXPIRED');
    if (Number(rec.attempts || 0) >= EMAIL_CODE_MAX_ATTEMPTS) return ctx.fail(429, 'TOO_MANY_ATTEMPTS');
    const ok = rec.fighterId === fighterId &&
      constantTimeEqual(await sha256Hex(`${code}|${email}|${fighterId}`), String(rec.codeHash || ''));
    if (!ok) {
      await db.patch(EMAIL_CODES, id, { attempts: Number(rec.attempts || 0) + 1 });
      return ctx.fail(403, 'WRONG_CODE', { attempts_left: EMAIL_CODE_MAX_ATTEMPTS - Number(rec.attempts || 0) - 1 });
    }
    await db.remove(EMAIL_CODES, id);
    await db.patch(FIGHTERS, fighterId, { fighterId, email, emailVerified: true, emailVerifiedAt: now });

    const callsign = cleanText(body.callsign, 80);
    const unitKey = cleanText(body.unit_key, 120);
    const unitName = cleanText(body.unit_name, 120);
    const welcome = sendEmail(cfg, {
        toEmail: email,
        toName: callsign,
        subject: 'Регистрация в «Каптёрка ПРО» подтверждена',
        title: `Добро пожаловать${callsign ? ', ' + callsign : ''}!`,
        lines: [
          'Ваша почта подтверждена. Сохраните это письмо — в нём данные для подключения.',
          unitName ? `Подразделение: ${unitName}` : '',
          `ID бойца: ${fighterId}`,
          'Ключ подразделения (нужен, чтобы подключить второй телефон):'
        ].filter(Boolean),
        highlight: unitKey || '—'
      }).catch((error) => console.error('Welcome email error:', error?.message || error));
    // The welcome letter goes out in the background: the relay can be slow.
    await ctx.background(welcome);
    return ctx.ok({ ok: true, verified: true });
  },

  async license_verify(ctx) {
    const { body, db } = ctx;
    const licenseKey = cleanText(body.license_key, 40).toUpperCase();
    const fighterId = cleanText(body.fighter_id, 100);
    if (!LICENSE_KEY_RE.test(licenseKey)) return ctx.fail(400, 'INVALID_LICENSE_KEY');
    if (!(await ctx.limit('verify:' + ctx.ip))) return ctx.fail(429, 'RATE_LIMITED');

    const license = normalizeLicense(await db.get(LICENSES, licenseKey));
    if (!isActive(license)) return ctx.fail(404, 'LICENSE_NOT_ACTIVE');
    if (license.fighterId && license.fighterId !== fighterId) return ctx.fail(403, 'FIGHTER_MISMATCH');

    // A license bought by a 3.5.0 client has no fighter yet: the first verified
    // 3.6+ installation claims it, so the same key cannot be spread further.
    if (!license.fighterId && fighterId) {
      await db.patch(LICENSES, licenseKey, { fighterId, boundAt: Date.now() }, { mustExist: true });
      license.fighterId = fighterId;
    }
    return ctx.ok({
      ok: true,
      license_key: license.licenseKey,
      expires_at: license.expiresAt,
      fighter_id: license.fighterId
    });
  },

  async license_restore(ctx) {
    const { body, db } = ctx;
    const email = cleanEmail(body.email);
    const fighterId = cleanText(body.fighter_id, 100);
    if (!email) return ctx.fail(400, 'INVALID_EMAIL');
    if (!fighterId) return ctx.fail(400, 'MISSING_FIGHTER_ID');
    if (!(await ctx.limit('restore:' + ctx.ip))) return ctx.fail(429, 'RATE_LIMITED');

    const license = await activeLicenseByEmail(db, email, fighterId);
    if (!license) {
      const other = await activeLicenseByEmail(db, email);
      // Email alone never discloses a reusable key bound to another fighter.
      if (other) return ctx.fail(403, 'LICENSE_RESTORE_IDENTITY_MISMATCH');
      return ctx.fail(404, 'LICENSE_NOT_FOUND');
    }
    return ctx.ok({ ok: true, license_key: license.licenseKey, expires_at: license.expiresAt, fighter_id: license.fighterId });
  },

  async send_license_email(ctx) {
    const { body, db, cfg } = ctx;
    const licenseKey = cleanText(body.license_key, 40).toUpperCase();
    const email = cleanEmail(body.email);
    if (!LICENSE_KEY_RE.test(licenseKey) || !email) return ctx.fail(400, 'INVALID_LICENSE_EMAIL_REQUEST');

    const license = normalizeLicense(await db.get(LICENSES, licenseKey));
    if (!isActive(license)) return ctx.fail(404, 'LICENSE_NOT_ACTIVE');
    if (!license.email || license.email.toLowerCase() !== email) return ctx.fail(403, 'LICENSE_EMAIL_MISMATCH');

    const rateKey = await sha256Hex(licenseKey + '|' + email);
    const now = Date.now();
    const previous = licenseEmailLastSentAt.get(rateKey) || 0;
    if (now - previous < LICENSE_EMAIL_RATE_LIMIT_MS || !(await ctx.limit('email:' + rateKey))) {
      return ctx.fail(429, 'LICENSE_EMAIL_RATE_LIMITED', {
        retry_after_seconds: Math.max(1, Math.ceil((LICENSE_EMAIL_RATE_LIMIT_MS - (now - previous)) / 1000))
      });
    }
    licenseEmailLastSentAt.set(rateKey, now);
    if (licenseEmailLastSentAt.size > 5000) licenseEmailLastSentAt.clear();

    try {
      await sendLicenseEmail(cfg, {
        toEmail: license.email,
        callsign: license.callsign,
        licenseKey,
        days: Math.max(1, Math.ceil((license.expiresAt - now) / DAY_MS))
      });
    } catch (error) {
      licenseEmailLastSentAt.delete(rateKey);
      console.error('License email error:', error?.message || error);
      return ctx.fail(503, 'EMAIL_PROVIDER_UNAVAILABLE');
    }
    await sendTelegram(cfg,
      '✉️ <b>Лицензионное письмо отправлено</b>\n' +
      'Email: <code>' + escapeHtml(license.email) + '</code>\n' +
      'Ключ: <code>' + escapeHtml(maskKey(licenseKey)) + '</code>'
    );
    return ctx.ok({ ok: true });
  },

  // A device proves its Firebase identity and becomes a member of a unit.
  // Firestore rules (strict phase) only let members read/write unit data, so
  // guessing a unit key is only possible through this rate-limited endpoint.
  async unit_join(ctx) {
    const { body, db, cfg, ip } = ctx;
    if (!(await bindingAllows(ctx.env.AUTH_LIMITER, 'join:' + ip))) {
      return ctx.fail(429, 'RATE_LIMITED', { retry_after_seconds: 60 });
    }
    const uid = await verifyFirebaseIdToken(cfg, body.id_token);
    if (!uid) return ctx.fail(401, 'AUTH_REQUIRED');
    if (!(await bindingAllows(ctx.env.AUTH_LIMITER, 'join-uid:' + uid))) {
      return ctx.fail(429, 'RATE_LIMITED', { retry_after_seconds: 60 });
    }
    const unitKey = cleanText(body.unit_key, 120);
    if (!UNIT_KEY_RE.test(unitKey)) return ctx.fail(400, 'INVALID_UNIT_KEY');

    const membersPath = `units/${unitKey}/members`;
    const now = Date.now();
    const member = {
      uid,
      fighterId: cleanText(body.fighter_id, 100),
      callsign: cleanText(body.callsign, 80),
      deviceId: cleanText(body.device_id, 64),
      lastJoinAt: now
    };

    if (await db.get(membersPath, uid)) {
      await db.patch(membersPath, uid, member);
      return ctx.ok({ ok: true, unit_key: unitKey, member: true, created: false });
    }

    let exists = Boolean(await db.get('units', unitKey));
    for (const coll of UNIT_DATA_COLLECTIONS) {
      if (exists) break;
      exists = await db.hasAnyDocument(`units/${unitKey}/${coll}`);
    }
    let created = false;
    if (!exists) {
      if (body.create !== true) return ctx.fail(404, 'UNIT_NOT_FOUND');
      created = true;
    }
    try {
      await db.patch('units', unitKey, { unitKey, createdAt: now, createdBy: uid }, { mustExist: false });
    } catch (error) {
      if (error?.code !== 'ALREADY_EXISTS') throw error;
    }
    await db.patch(membersPath, uid, { ...member, joinedAt: now });
    return ctx.ok({ ok: true, unit_key: unitKey, member: true, created });
  },

  async admin_auth(ctx) {
    const { body, cfg, ip } = ctx;
    if (adminAuthBlocked(ip) || !(await bindingAllows(ctx.env.AUTH_LIMITER, 'admin:' + ip))) {
      return ctx.fail(429, 'ADMIN_AUTH_RATE_LIMITED', { retry_after_seconds: ADMIN_AUTH_WINDOW_MS / 1000 });
    }
    if (!(await adminSecretMatches(cfg, cleanText(body.secret, 256)))) {
      recordAdminAuthFailure(ip);
      return ctx.fail(403, 'ADMIN_AUTH_FAILED');
    }
    adminAuthFailures.delete(ip);
    const token = await issueAdminToken(cfg);
    if (!token) return ctx.fail(503, 'ADMIN_SESSION_NOT_CONFIGURED');
    return ctx.ok({ ok: true, admin_token: token, expires_in_seconds: ADMIN_TOKEN_TTL_MS / 1000 });
  },

  async admin_list_fighters(ctx) {
    if (!(await verifyAdminToken(ctx.cfg, ctx.body.admin_token))) return ctx.fail(403, 'ADMIN_SESSION_INVALID');
    const [fighters, licenses] = await Promise.all([
      ctx.db.list(FIGHTERS).then((rows) => rows.map(normalizeFighter)),
      ctx.db.list(LICENSES).then((rows) => rows.map(normalizeLicense)).catch(() => [])
    ]);
    return ctx.ok({
      ok: true,
      fighters: fighters.map((f) => ({
        deletable: !fighterHasLicense(f, licenses),
        id: f.id,
        callsign: f.callsign,
        role: f.role,
        unit_name: f.unitName,
        unit_key: f.unitKey,
        license_key: f.licenseKey,
        expires_at: f.expiresAt,
        registered_at: f.registeredAt,
        last_seen_at: f.lastSeenAt,
        email: f.email,
        device_model: f.deviceModel
      }))
    });
  },

  // Owner dashboard ("Командный центр"): live usage, licences, revenue.
  async admin_stats(ctx) {
    const { db, cfg } = ctx;
    if (!(await verifyAdminToken(cfg, ctx.body.admin_token))) return ctx.fail(403, 'ADMIN_SESSION_INVALID');
    const now = Date.now();
    const ONLINE_MS = 15 * 60 * 1000;
    const dayStart = (ms) => {
      const d = new Date(ms + 3 * 3600 * 1000); // Moscow day boundaries
      return Date.UTC(d.getUTCFullYear(), d.getUTCMonth(), d.getUTCDate()) - 3 * 3600 * 1000;
    };
    const todayStart = dayStart(now);
    const monthStart = (() => {
      const d = new Date(now + 3 * 3600 * 1000);
      return Date.UTC(d.getUTCFullYear(), d.getUTCMonth(), 1) - 3 * 3600 * 1000;
    })();

    const [devices, srvFighters, legacyFighters, srvLicenses] = await Promise.all([
      db.collectionGroup('devices'),
      db.list(FIGHTERS),
      db.list(LEGACY_FIGHTERS).catch(() => []),
      db.list(LICENSES)
    ]);

    // Devices: presence pings per unit.
    const seen = (d) => Number(d.timestampMillis || 0) || d.updateTime || 0;
    const online = devices.filter((d) => now - seen(d) < ONLINE_MS);
    const unitOf = (d) => d.path[1] || '';
    const onlineList = online
      .sort((a, b) => seen(b) - seen(a))
      .slice(0, 40)
      .map((d) => ({
        callsign: cleanText(d.callsign || 'Без позывного', 40),
        unit_name: cleanText(d.unitName || '', 60),
        device_model: cleanText(d.deviceModel || '', 40),
        last_seen: seen(d)
      }));

    // Users: 3.6 registry is authoritative, legacy registry fills the gaps.
    const users = new Map();
    for (const f of legacyFighters) {
      const id = String(f.fighterId || f.id || '');
      if (id) users.set(id, { registeredAt: Number(f.registeredAt || 0), lastSeenAt: Number(f.lastSeenAt || 0), expiresAt: Number(f.expiresAt || 0), v36: false });
    }
    for (const f of srvFighters) {
      const id = String(f.fighterId || f.id || '');
      if (!id) continue;
      const prev = users.get(id) || { registeredAt: 0, lastSeenAt: 0, expiresAt: 0 };
      users.set(id, {
        registeredAt: prev.registeredAt || Number(f.registeredAt || 0),
        lastSeenAt: Math.max(prev.lastSeenAt, Number(f.lastSeenAt || 0)),
        expiresAt: Math.max(prev.expiresAt, Number(f.expiresAt || 0)),
        v36: true
      });
    }
    const userList = [...users.values()];
    const registrations14 = Array.from({ length: 14 }, (_, i) => {
      const start = todayStart - (13 - i) * DAY_MS;
      return userList.filter((u) => u.registeredAt >= start && u.registeredAt < start + DAY_MS).length;
    });

    // Licences: verified (server registry) + those only known from old app versions.
    const verified = srvLicenses.map(normalizeLicense).filter((l) => isActive(l, now));
    const paidThisMonth = srvLicenses.filter((x) => x.paymentId && Number(x.activatedAt || 0) >= monthStart);
    const legacyActive = userList.filter((u) => !u.v36 && u.expiresAt > now).length;

    return ctx.ok({
      ok: true,
      generated_at: now,
      online: {
        devices_now: online.length,
        units_now: new Set(online.map(unitOf)).size,
        devices_today: devices.filter((d) => seen(d) >= todayStart).length,
        devices_week: devices.filter((d) => now - seen(d) < 7 * DAY_MS).length
      },
      online_list: onlineList,
      users: {
        total: userList.length,
        on_36: userList.filter((u) => u.v36).length,
        new_today: userList.filter((u) => u.registeredAt >= todayStart).length,
        new_week: userList.filter((u) => now - u.registeredAt < 7 * DAY_MS).length
      },
      units_total: new Set(devices.map(unitOf).filter(Boolean)).size,
      licenses: {
        active_verified: verified.length,
        active_legacy: legacyActive,
        expiring_7d: verified.filter((l) => l.expiresAt - now < 7 * DAY_MS).length,
        paid_month: paidThisMonth.length,
        revenue_month_rub: paidThisMonth.length * cfg.amountRub
      },
      registrations_14d: registrations14
    });
  },

  async admin_grant_license(ctx) {
    const { body, db, cfg } = ctx;
    if (!(await verifyAdminToken(cfg, body.admin_token))) return ctx.fail(403, 'ADMIN_SESSION_INVALID');
    const fighterId = cleanText(body.fighter_id, 100);
    const days = Math.min(365, Math.max(1, Number.parseInt(body.days, 10) || 30));
    if (!fighterId) return ctx.fail(400, 'MISSING_FIGHTER_ID');

    const fighter = normalizeFighter(await db.get(FIGHTERS, fighterId));
    if (!fighter) return ctx.fail(404, 'FIGHTER_NOT_FOUND');

    const now = Date.now();
    let licenseKey = cleanText(fighter.licenseKey, 40).toUpperCase();
    const existing = LICENSE_KEY_RE.test(licenseKey) ? normalizeLicense(await db.get(LICENSES, licenseKey)) : null;
    let expiresAt;
    if (isActive(existing, now) && existing.fighterId === fighterId) {
      // "+N days" extends the current license and keeps the same key.
      expiresAt = existing.expiresAt + days * DAY_MS;
      await db.patch(LICENSES, licenseKey, { expiresAt, status: 'ACTIVE' }, { mustExist: true });
    } else {
      licenseKey = generateAdminLicenseKey();
      expiresAt = now + days * DAY_MS;
      await db.patch(LICENSES, licenseKey, {
        licenseKey,
        fighterId,
        callsign: cleanText(fighter.callsign, 80),
        email: cleanEmail(fighter.email),
        paymentId: '',
        amount: 0,
        activatedAt: now,
        expiresAt,
        durationDays: days,
        status: 'ACTIVE',
        source: 'Admin server grant'
      }, { mustExist: false });
    }
    await db.patch(FIGHTERS, fighterId, { licenseKey, expiresAt, isProActive: true });
    return ctx.ok({ ok: true, license_key: licenseKey, expires_at: expiresAt, days });
  },

  // Only test accounts can be removed: anyone who ever had a licence (paid,
  // granted or legacy key) is a real user and stays in the registry.
  async admin_delete_fighter(ctx) {
    const { db } = ctx;
    if (!(await verifyAdminToken(ctx.cfg, ctx.body.admin_token))) return ctx.fail(403, 'ADMIN_SESSION_INVALID');
    const fighterId = cleanText(ctx.body.fighter_id, 100);
    if (!fighterId) return ctx.fail(400, 'MISSING_FIGHTER_ID');
    const fighter = normalizeFighter(await db.get(FIGHTERS, fighterId));
    if (!fighter) return ctx.fail(404, 'FIGHTER_NOT_FOUND');
    const email = cleanEmail(fighter.email);
    const owned = [
      ...(await db.queryEqual(LICENSES, 'fighterId', fighterId, 5)),
      ...(email ? await db.queryEqual(LICENSES, 'email', email, 5) : [])
    ];
    if (fighterHasLicense(fighter, owned.map(normalizeLicense))) {
      return ctx.fail(409, 'FIGHTER_HAS_LICENSE');
    }
    await db.remove(FIGHTERS, fighterId);
    if (email) await db.remove(EMAIL_CODES, await sha256Hex(email)).catch(() => {});
    return ctx.ok({ ok: true });
  },

  async create(ctx) {
    const { body, query, cfg } = ctx;
    const email = cleanEmail(body.email || query.email);
    const callsign = cleanText(body.callsign || query.callsign || 'Пользователь', 80);
    const fighterId = cleanText(body.fighter_id || query.fighter_id, 100);
    if (!email) return ctx.fail(400, 'INVALID_EMAIL');
    if (!(await ctx.limit('create:' + ctx.ip))) return ctx.fail(429, 'RATE_LIMITED');

    const returnUrlRaw = cleanText(body.return_url || query.return_url, 300);
    const returnUrl =
      returnUrlRaw.startsWith('kapterka://payment_success') ||
      returnUrlRaw.startsWith('kapterka-nextsafe://payment_success') ||
      returnUrlRaw.startsWith('https://kapterka-pro.ru/')
        ? returnUrlRaw
        : 'https://kapterka-pro.ru/?payment=check#tabPayment';
    const idempotenceKey = cleanText(body.idempotence_key || query.idempotence_key || crypto.randomUUID(), 64);

    const metadata = { callsign, email, duration_days: '30', product: 'kapterka_pro_30d' };
    if (fighterId) metadata.fighter_id = fighterId;

    // Price comes only from server configuration, never from the client.
    const payment = await requestYooKassa(cfg, 'POST', '/v3/payments', {
      amount: { value: cfg.amountRub.toFixed(2), currency: 'RUB' },
      confirmation: { type: 'redirect', return_url: returnUrl },
      capture: true,
      description: 'Каптёрка ПРО — 30 дней',
      metadata
    }, idempotenceKey);

    await sendTelegram(cfg,
      '💳 <b>Новый платёж «Каптёрка ПРО»</b>\n' +
      'Позывной: <b>' + escapeHtml(callsign) + '</b>\n' +
      'Email: <code>' + escapeHtml(email) + '</code>\n' +
      'Сумма: <b>' + cfg.amountRub + ' ₽</b>\n' +
      'Payment ID: <code>' + escapeHtml(payment.id || '') + '</code>'
    );
    return ctx.ok({
      ok: true,
      payment_id: payment.id || '',
      confirmation_url: payment.confirmation?.confirmation_url || ''
    });
  },

  async check(ctx) {
    const { body, query, db, cfg } = ctx;
    const paymentId = cleanText(body.payment_id || query.payment_id, 100);
    const requestedFighterId = cleanText(body.fighter_id || query.fighter_id, 100);
    if (!paymentId || !/^[A-Za-z0-9-]{8,100}$/.test(paymentId)) return ctx.fail(400, 'MISSING_PAYMENT_ID');
    if (!(await ctx.limit('check:' + ctx.ip))) return ctx.fail(429, 'RATE_LIMITED');

    let payment;
    try {
      payment = await requestYooKassa(cfg, 'GET', `/v3/payments/${encodeURIComponent(paymentId)}`);
    } catch (error) {
      // Unknown payment: not paid (also proves the shop credentials work).
      if (error?.status === 404) return ctx.ok({ ok: true, paid: false, status: 'not_found' });
      if (error?.status === 401 || error?.status === 403) {
        console.error('YooKassa rejected the shop credentials');
        return ctx.fail(502, 'PAYMENT_GATEWAY_AUTH');
      }
      throw error;
    }
    const status = payment.status || 'unknown';
    if (!(status === 'succeeded' && payment.paid === true)) return ctx.ok({ ok: true, paid: false, status });
    if (!amountMatchesTariff(payment, cfg.amountRub)) {
      return ctx.fail(409, 'PAYMENT_AMOUNT_MISMATCH', { paid: false, status });
    }
    if (payment.metadata?.product && payment.metadata.product !== 'kapterka_pro_30d') {
      return ctx.fail(409, 'PAYMENT_PRODUCT_MISMATCH', { paid: false, status });
    }
    const paymentFighterId = cleanText(payment.metadata?.fighter_id, 100);
    if (paymentFighterId && requestedFighterId !== paymentFighterId) {
      return ctx.fail(403, 'FIGHTER_MISMATCH', { paid: false, status });
    }

    const licenseKey = await keyForPayment(paymentId);
    const activatedAt = paymentTimestampMillis(payment);
    const email = cleanEmail(payment.metadata?.email);
    const callsign = cleanText(payment.metadata?.callsign || 'Пользователь', 80);

    let existing;
    try {
      existing = normalizeLicense(await db.get(LICENSES, licenseKey));
    } catch (error) {
      console.error('Existing license read error:', error?.message || error);
      return ctx.fail(503, 'LICENSE_REGISTRY_UNAVAILABLE', { paid: true, status });
    }

    // One succeeded payment = one license. Re-checks never extend or rebind it.
    if (existing && existing.expiresAt > 0) {
      if (existing.fighterId && requestedFighterId && existing.fighterId !== requestedFighterId) {
        return ctx.fail(403, 'LICENSE_FIGHTER_MISMATCH', { paid: false, status });
      }
      return ctx.ok(licenseResponse(status, licenseKey, existing.expiresAt));
    }

    const fighterId = paymentFighterId || requestedFighterId;
    let expiresAt = activatedAt + LICENSE_DURATION_MS;
    try {
      if (fighterId) {
        const fighter = normalizeFighter(await db.get(FIGHTERS, fighterId));
        // Renewal keeps unused paid/admin time.
        if (fighter && fighter.expiresAt > activatedAt) expiresAt = fighter.expiresAt + LICENSE_DURATION_MS;
      }
      await db.patch(LICENSES, licenseKey, {
        licenseKey,
        fighterId,
        callsign,
        email,
        paymentId,
        amount: cfg.amountRub,
        activatedAt,
        expiresAt,
        durationDays: 30,
        status: 'ACTIVE',
        source: 'YooKassa server verification'
      }, { mustExist: false });
    } catch (error) {
      if (error?.code === 'ALREADY_EXISTS') {
        // A concurrent check created it first — return that record unchanged.
        const winner = normalizeLicense(await db.get(LICENSES, licenseKey));
        if (!winner || !(winner.expiresAt > 0)) {
          return ctx.fail(503, 'LICENSE_REGISTRY_UNAVAILABLE', { paid: true, status });
        }
        if (winner.fighterId && requestedFighterId && winner.fighterId !== requestedFighterId) {
          return ctx.fail(403, 'LICENSE_FIGHTER_MISMATCH', { paid: false, status });
        }
        return ctx.ok(licenseResponse(status, licenseKey, winner.expiresAt));
      }
      console.error('License registry error:', error?.message || error);
      return ctx.fail(503, 'LICENSE_REGISTRY_UNAVAILABLE', { paid: true, status });
    }

    if (fighterId) {
      try {
        await db.patch(FIGHTERS, fighterId, {
          licenseKey,
          expiresAt,
          isProActive: expiresAt > Date.now(),
          lastSeenAt: Date.now()
        });
      } catch (error) {
        console.error('Fighter license mirror error:', error?.message || error);
      }
    }
    if (email) {
      try {
        await sendLicenseEmail(cfg, {
          toEmail: email,
          callsign,
          licenseKey,
          days: Math.max(1, Math.ceil((expiresAt - Date.now()) / DAY_MS))
        });
      } catch (error) {
        console.error('Licence email after payment failed:', error?.message || error);
      }
    }
    await sendTelegram(cfg,
      '✅ <b>Оплата подтверждена</b>\n' +
      'Позывной: <b>' + escapeHtml(callsign) + '</b>\n' +
      'Ключ: <code>' + escapeHtml(maskKey(licenseKey)) + '</code>'
    );
    return ctx.ok(licenseResponse(status, licenseKey, expiresAt));
  }
};
handlers.pay = handlers.create;

function licenseResponse(status, licenseKey, expiresAt) {
  return {
    ok: true,
    paid: true,
    status: expiresAt > Date.now() ? status : 'expired',
    license_key: licenseKey,
    key: licenseKey, // 3.5.0 client compatibility
    expires_at: expiresAt
  };
}

// Actions that were removed on purpose. `send_telegram` was an open relay into
// the admin chat; notifications are now emitted only by the server itself.
const REMOVED_ACTIONS = new Set(['send_telegram']);

export default {
  async fetch(request, env, execCtx) {
    if (request.method === 'OPTIONS') return new Response(null, { status: 204, headers: corsHeaders(request) });
    if (request.method !== 'GET' && request.method !== 'POST') {
      return json(request, 405, { ok: false, error: 'METHOD_NOT_ALLOWED' }, { Allow: 'GET, POST, OPTIONS' });
    }

    const url = new URL(request.url);
    const query = Object.fromEntries(url.searchParams.entries());
    let body = {};
    if (request.method === 'POST') {
      const raw = await request.text();
      if (new TextEncoder().encode(raw).length > MAX_REQUEST_BODY_BYTES) {
        return json(request, 413, { ok: false, error: 'PAYLOAD_TOO_LARGE' });
      }
      try {
        const parsed = raw ? JSON.parse(raw) : {};
        body = parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {};
      } catch (_) {
        body = {};
      }
    }

    const action = cleanText(query.action || body.action, 40).toLowerCase();
    const cfg = getConfig(env);
    const ip = clientIp(request);
    const ctx = {
      request,
      env,
      cfg,
      ip,
      // Background work that must not delay the answer (e.g. a welcome letter).
      background: (promise) => (execCtx && typeof execCtx.waitUntil === 'function' ? execCtx.waitUntil(promise) : promise),
      query,
      body,
      db: new Firestore(cfg),
      ok: (payload) => json(request, 200, payload),
      fail: (status, error, extra = {}) => json(request, status, { ok: false, error, ...extra }),
      limit: (key) => bindingAllows(env.API_LIMITER, key)
    };

    if (REMOVED_ACTIONS.has(action)) return ctx.fail(410, 'ACTION_REMOVED');
    const handler = Object.prototype.hasOwnProperty.call(handlers, action) ? handlers[action] : null;
    if (!handler) return ctx.fail(400, 'INVALID_ACTION');
    // State-changing and data-returning actions must be POST; GET is kept for
    // health and for the legacy 3.5.0 create/check calls only.
    if (request.method === 'GET' && !['health', 'create', 'pay', 'check'].includes(action)) {
      return ctx.fail(405, 'POST_REQUIRED');
    }

    try {
      return await handler(ctx);
    } catch (error) {
      // Never forward upstream error text to the caller.
      console.error(`kapterka-api ${action} error:`, error?.message || error);
      return ctx.fail(502, 'UPSTREAM_ERROR');
    }
  }
};

// Test hooks (not reachable over HTTP).
export const __test = {
  reset() {
    cachedGoogleToken = { value: '', expiresAt: 0, account: '' };
    cachedJwks = { keys: null, expiresAt: 0 };
    adminAuthFailures.clear();
    licenseEmailLastSentAt.clear();
  }
};
