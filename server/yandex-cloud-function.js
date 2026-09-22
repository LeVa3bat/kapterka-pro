const https = require('https');
const crypto = require('crypto');

// KAPTERKA PRO — server-authoritative payment/license backend.
// All privileged credentials live only in the cloud-function environment.
const YOOKASSA_SHOP_ID = process.env.YOOKASSA_SHOP_ID || '1450722';
const YOOKASSA_SECRET_KEY = process.env.YOOKASSA_SECRET_KEY || '';
const TG_BOT_TOKEN = process.env.TG_BOT_TOKEN || '';
const TG_ADMIN_CHAT_ID = process.env.TG_ADMIN_CHAT_ID || '';
const PAYMENT_AMOUNT_RUB = Number(process.env.PAYMENT_AMOUNT_RUB || 490);
const FIREBASE_PROJECT_ID = process.env.FIREBASE_PROJECT_ID || 'kapterka-pro';
const FIREBASE_SERVICE_ACCOUNT_JSON = process.env.FIREBASE_SERVICE_ACCOUNT_JSON || '';
const FIREBASE_SERVICE_ACCOUNT_B64 = process.env.FIREBASE_SERVICE_ACCOUNT_B64 || '';
const ADMIN_API_SECRET_SHA256 = String(process.env.ADMIN_API_SECRET_SHA256 || '').trim().toLowerCase();
const ADMIN_SESSION_SECRET = process.env.ADMIN_SESSION_SECRET || '';
const BREVO_API_KEY = process.env.BREVO_API_KEY || '';
const EMAIL_SENDER_NAME = process.env.EMAIL_SENDER_NAME || 'Каптёрка ПРО';
const EMAIL_SENDER_EMAIL = process.env.EMAIL_SENDER_EMAIL || '';

const CHECKSUM_CHARS = '23456789ABCDEFGHJKLMNPQRSTUVWXYZ';
const LICENSE_DURATION_MS = 30 * 24 * 60 * 60 * 1000;
const ADMIN_TOKEN_TTL_MS = 15 * 60 * 1000;
const ADMIN_AUTH_WINDOW_MS = 5 * 60 * 1000;
const ADMIN_AUTH_MAX_FAILURES = 5;
const LICENSE_EMAIL_RATE_LIMIT_MS = 60 * 1000;
const MAX_REQUEST_BODY_BYTES = 64 * 1024;
let cachedGoogleToken = { value: '', expiresAt: 0 };
const adminAuthFailures = new Map();
const licenseEmailLastSentAt = new Map();

function json(statusCode, body, headers = {}) {
  return {
    statusCode,
    headers: {
      'Content-Type': 'application/json; charset=utf-8',
      'Cache-Control': 'no-store',
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Headers': 'Content-Type',
      'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
      ...headers
    },
    body: JSON.stringify(body)
  };
}

function jsonpOrJson(statusCode, body, callback) {
  if (!callback) return json(statusCode, body);
  const safeCallback = String(callback).replace(/[^A-Za-z0-9_$\.]/g, '').slice(0, 80);
  if (!safeCallback) return json(statusCode, body);
  return {
    statusCode,
    headers: {
      'Content-Type': 'application/javascript; charset=utf-8',
      'Cache-Control': 'no-store',
      'Access-Control-Allow-Origin': '*'
    },
    body: `${safeCallback}(${JSON.stringify(body)});`
  };
}

function cleanText(value, max = 160) {
  return String(value || '').replace(/[\u0000-\u001F\u007F]/g, ' ').trim().slice(0, max);
}

function cleanEmail(value) {
  const email = cleanText(value, 160).toLowerCase();
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) ? email : '';
}

function licenseEmailRateKey(licenseKey, email) {
  return crypto
    .createHash('sha256')
    .update(String(licenseKey || '') + '|' + String(email || '').toLowerCase(), 'utf8')
    .digest('hex');
}

function reserveLicenseEmailSend(licenseKey, email, now = Date.now()) {
  if (licenseEmailLastSentAt.size > 5000) {
    for (const [key, sentAt] of licenseEmailLastSentAt.entries()) {
      if (now - sentAt >= LICENSE_EMAIL_RATE_LIMIT_MS) {
        licenseEmailLastSentAt.delete(key);
      }
      if (licenseEmailLastSentAt.size <= 2500) break;
    }
  }

  const key = licenseEmailRateKey(licenseKey, email);
  const previous = licenseEmailLastSentAt.get(key) || 0;
  if (previous > 0 && now - previous < LICENSE_EMAIL_RATE_LIMIT_MS) {
    return { ok: false, key, retryAfterMs: LICENSE_EMAIL_RATE_LIMIT_MS - (now - previous) };
  }

  licenseEmailLastSentAt.set(key, now);
  return { ok: true, key, retryAfterMs: 0 };
}

function escapeTelegramHtml(value) {
  return String(value || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
}

function computeKeyChecksum(p1, p2) {
  const s = `KAPT-${p1}-${p2}-KAPT3RKA_881_MILITARY`;
  let h1 = 0x811c9dc5 >>> 0;
  let h2 = 0x5a2d1e39 >>> 0;
  for (let i = 0; i < s.length; i++) {
    const code = s.charCodeAt(i);
    h1 = Math.imul(h1 ^ code, 0x01000193) >>> 0;
    h2 = (Math.imul(h2 + code, 31) + 0x45) >>> 0;
  }
  return (
    CHECKSUM_CHARS[(h1 >>> 24) & 0x1F] +
    CHECKSUM_CHARS[(h1 >>> 16) & 0x1F] +
    CHECKSUM_CHARS[(h2 >>> 24) & 0x1F] +
    CHECKSUM_CHARS[(h2 >>> 16) & 0x1F]
  );
}

function keyForPayment(paymentId) {
  const normalized = String(paymentId || '').toUpperCase().replace(/[^A-Z0-9]/g, '');
  const seed = crypto.createHash('sha256').update(normalized).digest('hex').toUpperCase();
  const p1 = seed.slice(0, 4);
  const p2 = seed.slice(4, 8);
  return `KAPT-${p1}-${p2}-${computeKeyChecksum(p1, p2)}`;
}

function sendTelegram(text) {
  return new Promise((resolve) => {
    if (!TG_BOT_TOKEN || !TG_ADMIN_CHAT_ID) return resolve(false);

    const payload = JSON.stringify({
      chat_id: TG_ADMIN_CHAT_ID,
      text,
      parse_mode: 'HTML'
    });

    const req = https.request({
      hostname: 'api.telegram.org',
      port: 443,
      path: `/bot${TG_BOT_TOKEN}/sendMessage`,
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(payload)
      },
      timeout: 8000
    }, (res) => {
      res.resume();
      resolve(res.statusCode >= 200 && res.statusCode < 300);
    });

    req.on('timeout', () => {
      req.destroy();
      resolve(false);
    });
    req.on('error', () => resolve(false));
    req.write(payload);
    req.end();
  });
}

function requestYooKassa(method, apiPath, data = null, idempotenceKey = null) {
  return new Promise((resolve, reject) => {
    if (!YOOKASSA_SECRET_KEY) {
      reject(new Error('YOOKASSA_SECRET_KEY is not configured'));
      return;
    }

    const auth = Buffer.from(`${YOOKASSA_SHOP_ID}:${YOOKASSA_SECRET_KEY}`).toString('base64');
    const body = data ? JSON.stringify(data) : '';
    const options = {
      hostname: 'api.yookassa.ru',
      port: 443,
      path: apiPath,
      method,
      headers: {
        Authorization: `Basic ${auth}`,
        'Idempotence-Key': idempotenceKey || crypto.randomUUID()
      },
      timeout: 12000
    };

    if (body) {
      options.headers['Content-Type'] = 'application/json';
      options.headers['Content-Length'] = Buffer.byteLength(body);
    }

    const req = https.request(options, (res) => {
      let responseBody = '';
      res.on('data', (chunk) => { responseBody += chunk; });
      res.on('end', () => {
        let parsed = {};
        try { parsed = responseBody ? JSON.parse(responseBody) : {}; } catch (_) {}
        if (res.statusCode >= 200 && res.statusCode < 300) {
          resolve(parsed);
        } else {
          const error = new Error(`YooKassa returned HTTP ${res.statusCode}`);
          error.statusCode = res.statusCode;
          error.response = parsed;
          reject(error);
        }
      });
    });

    req.on('timeout', () => req.destroy(new Error('YooKassa timeout')));
    req.on('error', reject);
    if (body) req.write(body);
    req.end();
  });
}

function base64url(input) {
  return Buffer.from(input)
    .toString('base64')
    .replace(/=/g, '')
    .replace(/\+/g, '-')
    .replace(/\//g, '_');
}

function readFirebaseServiceAccount() {
  const raw = FIREBASE_SERVICE_ACCOUNT_JSON ||
    (FIREBASE_SERVICE_ACCOUNT_B64
      ? Buffer.from(FIREBASE_SERVICE_ACCOUNT_B64, 'base64').toString('utf8')
      : '');
  if (!raw) return null;
  try {
    const parsed = JSON.parse(raw);
    if (!parsed.client_email || !parsed.private_key) return null;
    return parsed;
  } catch (_) {
    return null;
  }
}

function requestGoogleAccessToken() {
  return new Promise((resolve, reject) => {
    const nowMs = Date.now();
    if (cachedGoogleToken.value && cachedGoogleToken.expiresAt > nowMs + 60000) {
      resolve(cachedGoogleToken.value);
      return;
    }

    const account = readFirebaseServiceAccount();
    if (!account) {
      reject(new Error('Firebase service account is not configured'));
      return;
    }

    const now = Math.floor(nowMs / 1000);
    const header = base64url(JSON.stringify({ alg: 'RS256', typ: 'JWT' }));
    const claims = base64url(JSON.stringify({
      iss: account.client_email,
      scope: 'https://www.googleapis.com/auth/datastore',
      aud: 'https://oauth2.googleapis.com/token',
      iat: now,
      exp: now + 3600
    }));
    const unsigned = `${header}.${claims}`;
    const signer = crypto.createSign('RSA-SHA256');
    signer.update(unsigned);
    signer.end();
    const signature = signer.sign(account.private_key);
    const assertion = `${unsigned}.${base64url(signature)}`;
    const body = new URLSearchParams({
      grant_type: 'urn:ietf:params:oauth:grant-type:jwt-bearer',
      assertion
    }).toString();

    const req = https.request({
      hostname: 'oauth2.googleapis.com',
      port: 443,
      path: '/token',
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
        'Content-Length': Buffer.byteLength(body)
      },
      timeout: 10000
    }, (res) => {
      let responseBody = '';
      res.on('data', (chunk) => { responseBody += chunk; });
      res.on('end', () => {
        let parsed = {};
        try { parsed = JSON.parse(responseBody); } catch (_) {}
        if (res.statusCode >= 200 && res.statusCode < 300 && parsed.access_token) {
          cachedGoogleToken = {
            value: parsed.access_token,
            expiresAt: nowMs + Math.max(300, Number(parsed.expires_in || 3600) - 60) * 1000
          };
          resolve(cachedGoogleToken.value);
        } else {
          reject(new Error('Google OAuth token request failed'));
        }
      });
    });

    req.on('timeout', () => req.destroy(new Error('Google OAuth timeout')));
    req.on('error', reject);
    req.write(body);
    req.end();
  });
}

function firestoreValue(value) {
  if (typeof value === 'number') return { integerValue: String(Math.trunc(value)) };
  if (typeof value === 'boolean') return { booleanValue: value };
  return { stringValue: String(value ?? '') };
}

async function registerLicenseInFirestore(data) {
  const token = await requestGoogleAccessToken();
  const fields = {};
  for (const [key, value] of Object.entries(data)) {
    fields[key] = firestoreValue(value);
  }

  const payload = JSON.stringify({ fields });
  const docId = encodeURIComponent(data.licenseKey);
  const path = `/v1/projects/${encodeURIComponent(FIREBASE_PROJECT_ID)}/databases/(default)/documents/licenses/${docId}`;

  return new Promise((resolve, reject) => {
    const req = https.request({
      hostname: 'firestore.googleapis.com',
      port: 443,
      path,
      method: 'PATCH',
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(payload)
      },
      timeout: 10000
    }, (res) => {
      let responseBody = '';
      res.on('data', (chunk) => { responseBody += chunk; });
      res.on('end', () => {
        if (res.statusCode >= 200 && res.statusCode < 300) resolve(true);
        else reject(new Error(`Firestore license write failed: HTTP ${res.statusCode}`));
      });
    });

    req.on('timeout', () => req.destroy(new Error('Firestore timeout')));
    req.on('error', reject);
    req.write(payload);
    req.end();
  });
}

function paymentTimestampMillis(payment) {
  const raw = payment?.captured_at || payment?.created_at || '';
  const parsed = Date.parse(raw);
  return Number.isFinite(parsed) ? parsed : Date.now();
}

function amountMatchesTariff(payment) {
  const currency = String(payment?.amount?.currency || '').toUpperCase();
  const value = Number(payment?.amount?.value || 0);
  return currency === 'RUB' && Math.abs(value - PAYMENT_AMOUNT_RUB) < 0.001;
}

function adminAuthSourceKey(event) {
  const headers = event?.headers || {};
  const forwarded = String(
    headers['x-forwarded-for'] ||
    headers['X-Forwarded-For'] ||
    ''
  ).split(',')[0].trim();
  return String(
    event?.requestContext?.identity?.sourceIp ||
    event?.requestContext?.http?.sourceIp ||
    forwarded ||
    'unknown'
  ).slice(0, 120);
}

function pruneAdminAuthFailures(now = Date.now()) {
  for (const [key, entry] of adminAuthFailures.entries()) {
    if (!entry || now - entry.windowStartedAt >= ADMIN_AUTH_WINDOW_MS) {
      adminAuthFailures.delete(key);
    }
  }
  if (adminAuthFailures.size > 5000) {
    for (const key of adminAuthFailures.keys()) {
      adminAuthFailures.delete(key);
      if (adminAuthFailures.size <= 2500) break;
    }
  }
}

function adminAuthIsBlocked(sourceKey, now = Date.now()) {
  pruneAdminAuthFailures(now);
  const entry = adminAuthFailures.get(sourceKey);
  return Boolean(
    entry &&
    now - entry.windowStartedAt < ADMIN_AUTH_WINDOW_MS &&
    entry.failures >= ADMIN_AUTH_MAX_FAILURES
  );
}

function recordAdminAuthFailure(sourceKey, now = Date.now()) {
  const current = adminAuthFailures.get(sourceKey);
  if (!current || now - current.windowStartedAt >= ADMIN_AUTH_WINDOW_MS) {
    adminAuthFailures.set(sourceKey, { failures: 1, windowStartedAt: now });
    return;
  }
  current.failures += 1;
  adminAuthFailures.set(sourceKey, current);
}

function clearAdminAuthFailures(sourceKey) {
  adminAuthFailures.delete(sourceKey);
}

function adminSecretMatches(secret) {
  if (!ADMIN_API_SECRET_SHA256 || !/^[a-f0-9]{64}$/.test(ADMIN_API_SECRET_SHA256)) return false;
  const actual = crypto.createHash('sha256').update(String(secret || ''), 'utf8').digest('hex');
  return crypto.timingSafeEqual(Buffer.from(actual, 'hex'), Buffer.from(ADMIN_API_SECRET_SHA256, 'hex'));
}

function issueAdminToken() {
  if (!ADMIN_SESSION_SECRET) return '';
  const expiresAt = Date.now() + ADMIN_TOKEN_TTL_MS;
  const nonce = crypto.randomBytes(12).toString('hex');
  const payload = `${expiresAt}.${nonce}`;
  const signature = crypto.createHmac('sha256', ADMIN_SESSION_SECRET).update(payload).digest('hex');
  return `${payload}.${signature}`;
}

function verifyAdminToken(token) {
  if (!ADMIN_SESSION_SECRET) return false;
  const parts = String(token || '').split('.');
  if (parts.length !== 3) return false;
  const expiresAt = Number(parts[0]);
  const nonce = parts[1];
  const signature = parts[2];
  if (!Number.isFinite(expiresAt) || expiresAt <= Date.now() || nonce.length < 12 || !/^[a-f0-9]{64}$/i.test(signature)) {
    return false;
  }
  const payload = `${parts[0]}.${nonce}`;
  const expected = crypto.createHmac('sha256', ADMIN_SESSION_SECRET).update(payload).digest('hex');
  return crypto.timingSafeEqual(Buffer.from(expected, 'hex'), Buffer.from(signature, 'hex'));
}

function generateAdminLicenseKey() {
  const part = () => Array.from({ length: 4 }, () => CHECKSUM_CHARS[crypto.randomInt(CHECKSUM_CHARS.length)]).join('');
  const p1 = part();
  const p2 = part();
  return `KAPT-${p1}-${p2}-${computeKeyChecksum(p1, p2)}`;
}

async function getFirestoreDocument(collection, docId) {
  const token = await requestGoogleAccessToken();
  const path =
    `/v1/projects/${encodeURIComponent(FIREBASE_PROJECT_ID)}/databases/(default)/documents/` +
    `${encodeURIComponent(collection)}/${encodeURIComponent(docId)}`;

  return new Promise((resolve, reject) => {
    const req = https.request({
      hostname: 'firestore.googleapis.com',
      port: 443,
      path,
      method: 'GET',
      headers: { Authorization: `Bearer ${token}` },
      timeout: 10000
    }, (res) => {
      let responseBody = '';
      res.on('data', (chunk) => { responseBody += chunk; });
      res.on('end', () => {
        if (res.statusCode === 404) return resolve(null);
        if (res.statusCode < 200 || res.statusCode >= 300) {
          return reject(new Error(`Firestore read failed: HTTP ${res.statusCode}`));
        }
        try {
          const parsed = JSON.parse(responseBody || '{}');
          const fields = parsed.fields || {};
          const value = (name) => {
            const field = fields[name] || {};
            if (Object.prototype.hasOwnProperty.call(field, 'stringValue')) return field.stringValue;
            if (Object.prototype.hasOwnProperty.call(field, 'integerValue')) return Number(field.integerValue);
            if (Object.prototype.hasOwnProperty.call(field, 'booleanValue')) return Boolean(field.booleanValue);
            return null;
          };
          resolve({
            licenseKey: value('licenseKey') || docId,
            fighterId: value('fighterId') || '',
            callsign: value('callsign') || '',
            email: value('email') || '',
            expiresAt: Number(value('expiresAt') || 0),
            status: value('status') || ''
          });
        } catch (error) {
          reject(error);
        }
      });
    });
    req.on('timeout', () => req.destroy(new Error('Firestore timeout')));
    req.on('error', reject);
    req.end();
  });
}

function sendLicenseEmailViaBrevo({ toEmail, callsign, licenseKey, days }) {
  return new Promise((resolve, reject) => {
    if (!BREVO_API_KEY || !EMAIL_SENDER_EMAIL) {
      reject(new Error('Email provider is not configured'));
      return;
    }

    const subject = `Ваш лицензионный ключ «Каптёрка ПРО» (${days} дней)`;
    const text = [
      `Здравствуйте, ${callsign || 'пользователь'}!`,
      '',
      'Ваш лицензионный ключ Каптёрка ПРО:',
      licenseKey,
      '',
      `Срок действия: ${days} суток.`,
      'Официальный сайт: https://kapterka-pro.ru/'
    ].join('\n');

    const body = JSON.stringify({
      sender: { name: EMAIL_SENDER_NAME, email: EMAIL_SENDER_EMAIL },
      to: [{ email: toEmail, name: callsign || 'Пользователь' }],
      subject,
      textContent: text
    });

    const req = https.request({
      hostname: 'api.brevo.com',
      port: 443,
      path: '/v3/smtp/email',
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(body),
        'api-key': BREVO_API_KEY
      },
      timeout: 10000
    }, (res) => {
      let responseBody = '';
      res.on('data', (chunk) => { responseBody += chunk; });
      res.on('end', () => {
        if (res.statusCode >= 200 && res.statusCode < 300) resolve(true);
        else reject(new Error(`Brevo returned HTTP ${res.statusCode}`));
      });
    });
    req.on('timeout', () => req.destroy(new Error('Brevo timeout')));
    req.on('error', reject);
    req.write(body);
    req.end();
  });
}

async function queryActiveLicenseByEmail(email) {
  const token = await requestGoogleAccessToken();
  const path = `/v1/projects/${encodeURIComponent(FIREBASE_PROJECT_ID)}/databases/(default)/documents:runQuery`;
  const payload = JSON.stringify({
    structuredQuery: {
      from: [{ collectionId: 'licenses' }],
      where: {
        fieldFilter: {
          field: { fieldPath: 'email' },
          op: 'EQUAL',
          value: { stringValue: email }
        }
      },
      limit: 20
    }
  });

  return new Promise((resolve, reject) => {
    const req = https.request({
      hostname: 'firestore.googleapis.com',
      port: 443,
      path,
      method: 'POST',
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(payload)
      },
      timeout: 10000
    }, (res) => {
      let responseBody = '';
      res.on('data', (chunk) => { responseBody += chunk; });
      res.on('end', () => {
        if (res.statusCode < 200 || res.statusCode >= 300) {
          return reject(new Error(`Firestore query failed: HTTP ${res.statusCode}`));
        }
        try {
          const rows = JSON.parse(responseBody || '[]');
          const parsed = rows
            .map((row) => row.document)
            .filter(Boolean)
            .map((doc) => {
              const fields = doc.fields || {};
              const value = (name) => {
                const field = fields[name] || {};
                if (Object.prototype.hasOwnProperty.call(field, 'stringValue')) return field.stringValue;
                if (Object.prototype.hasOwnProperty.call(field, 'integerValue')) return Number(field.integerValue);
                if (Object.prototype.hasOwnProperty.call(field, 'booleanValue')) return Boolean(field.booleanValue);
                return null;
              };
              const name = String(doc.name || '');
              return {
                licenseKey: value('licenseKey') || decodeURIComponent(name.split('/').pop() || ''),
                fighterId: value('fighterId') || '',
                callsign: value('callsign') || '',
                email: value('email') || '',
                expiresAt: Number(value('expiresAt') || 0),
                status: value('status') || ''
              };
            })
            .filter((item) => item.status === 'ACTIVE' && item.expiresAt > Date.now())
            .sort((a, b) => b.expiresAt - a.expiresAt);
          resolve(parsed[0] || null);
        } catch (error) {
          reject(error);
        }
      });
    });
    req.on('timeout', () => req.destroy(new Error('Firestore query timeout')));
    req.on('error', reject);
    req.write(payload);
    req.end();
  });
}

function firestoreFieldValue(fields, name) {
  const field = (fields || {})[name] || {};
  if (Object.prototype.hasOwnProperty.call(field, 'stringValue')) return field.stringValue;
  if (Object.prototype.hasOwnProperty.call(field, 'integerValue')) return Number(field.integerValue);
  if (Object.prototype.hasOwnProperty.call(field, 'booleanValue')) return Boolean(field.booleanValue);
  if (Object.prototype.hasOwnProperty.call(field, 'timestampValue')) return Date.parse(field.timestampValue) || 0;
  return null;
}

function parseFighterDocument(doc) {
  if (!doc) return null;
  const fields = doc.fields || {};
  const name = String(doc.name || '');
  const docId = decodeURIComponent(name.split('/').pop() || '');
  return {
    id: firestoreFieldValue(fields, 'fighterId') || docId,
    callsign: firestoreFieldValue(fields, 'callsign') || '',
    role: firestoreFieldValue(fields, 'role') || 'Старшина подразделения',
    unitName: firestoreFieldValue(fields, 'unitName') || '',
    unitKey: firestoreFieldValue(fields, 'unitKey') || '',
    licenseKey: firestoreFieldValue(fields, 'licenseKey') || '',
    expiresAt: Number(firestoreFieldValue(fields, 'expiresAt') || 0),
    registeredAt: Number(firestoreFieldValue(fields, 'registeredAt') || 0),
    lastSeenAt: Number(firestoreFieldValue(fields, 'lastSeenAt') || 0),
    email: firestoreFieldValue(fields, 'email') || '',
    deviceModel: firestoreFieldValue(fields, 'deviceModel') || ''
  };
}

async function getFighterById(fighterId) {
  const token = await requestGoogleAccessToken();
  const path =
    `/v1/projects/${encodeURIComponent(FIREBASE_PROJECT_ID)}/databases/(default)/documents/fighters/${encodeURIComponent(fighterId)}`;

  return new Promise((resolve, reject) => {
    const req = https.request({
      hostname: 'firestore.googleapis.com',
      port: 443,
      path,
      method: 'GET',
      headers: { Authorization: `Bearer ${token}` },
      timeout: 10000
    }, (res) => {
      let responseBody = '';
      res.on('data', (chunk) => { responseBody += chunk; });
      res.on('end', () => {
        if (res.statusCode === 404) return resolve(null);
        if (res.statusCode < 200 || res.statusCode >= 300) {
          return reject(new Error(`Firestore fighter read failed: HTTP ${res.statusCode}`));
        }
        try {
          resolve(parseFighterDocument(JSON.parse(responseBody || '{}')));
        } catch (error) {
          reject(error);
        }
      });
    });
    req.on('timeout', () => req.destroy(new Error('Firestore fighter read timeout')));
    req.on('error', reject);
    req.end();
  });
}

async function queryFighterByEmail(email) {
  const token = await requestGoogleAccessToken();
  const path = `/v1/projects/${encodeURIComponent(FIREBASE_PROJECT_ID)}/databases/(default)/documents:runQuery`;
  const payload = JSON.stringify({
    structuredQuery: {
      from: [{ collectionId: 'fighters' }],
      where: {
        fieldFilter: {
          field: { fieldPath: 'email' },
          op: 'EQUAL',
          value: { stringValue: email }
        }
      },
      limit: 10
    }
  });

  return new Promise((resolve, reject) => {
    const req = https.request({
      hostname: 'firestore.googleapis.com',
      port: 443,
      path,
      method: 'POST',
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(payload)
      },
      timeout: 10000
    }, (res) => {
      let responseBody = '';
      res.on('data', (chunk) => { responseBody += chunk; });
      res.on('end', () => {
        if (res.statusCode < 200 || res.statusCode >= 300) {
          return reject(new Error(`Firestore fighter query failed: HTTP ${res.statusCode}`));
        }
        try {
          const rows = JSON.parse(responseBody || '[]');
          resolve(rows.map((row) => parseFighterDocument(row.document)).filter(Boolean));
        } catch (error) {
          reject(error);
        }
      });
    });
    req.on('timeout', () => req.destroy(new Error('Firestore fighter query timeout')));
    req.on('error', reject);
    req.write(payload);
    req.end();
  });
}

async function listFightersFromFirestore() {
  const token = await requestGoogleAccessToken();
  const path =
    `/v1/projects/${encodeURIComponent(FIREBASE_PROJECT_ID)}/databases/(default)/documents/fighters?pageSize=500`;

  return new Promise((resolve, reject) => {
    const req = https.request({
      hostname: 'firestore.googleapis.com',
      port: 443,
      path,
      method: 'GET',
      headers: { Authorization: `Bearer ${token}` },
      timeout: 10000
    }, (res) => {
      let responseBody = '';
      res.on('data', (chunk) => { responseBody += chunk; });
      res.on('end', () => {
        if (res.statusCode < 200 || res.statusCode >= 300) {
          return reject(new Error(`Firestore fighter list failed: HTTP ${res.statusCode}`));
        }
        try {
          const parsed = JSON.parse(responseBody || '{}');
          resolve((parsed.documents || []).map(parseFighterDocument).filter(Boolean));
        } catch (error) {
          reject(error);
        }
      });
    });
    req.on('timeout', () => req.destroy(new Error('Firestore fighter list timeout')));
    req.on('error', reject);
    req.end();
  });
}

async function patchFirestoreDocument(collection, docId, data) {
  const token = await requestGoogleAccessToken();
  const fields = {};
  for (const [key, value] of Object.entries(data)) fields[key] = firestoreValue(value);
  const payload = JSON.stringify({ fields });
  const masks = Object.keys(data)
    .map((key) => `updateMask.fieldPaths=${encodeURIComponent(key)}`)
    .join('&');
  const path =
    `/v1/projects/${encodeURIComponent(FIREBASE_PROJECT_ID)}/databases/(default)/documents/` +
    `${encodeURIComponent(collection)}/${encodeURIComponent(docId)}?${masks}`;

  return new Promise((resolve, reject) => {
    const req = https.request({
      hostname: 'firestore.googleapis.com',
      port: 443,
      path,
      method: 'PATCH',
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(payload)
      },
      timeout: 10000
    }, (res) => {
      res.resume();
      if (res.statusCode >= 200 && res.statusCode < 300) resolve(true);
      else reject(new Error(`Firestore patch failed: HTTP ${res.statusCode}`));
    });
    req.on('timeout', () => req.destroy(new Error('Firestore timeout')));
    req.on('error', reject);
    req.write(payload);
    req.end();
  });
}

async function deleteFirestoreDocument(collection, docId) {
  const token = await requestGoogleAccessToken();
  const path =
    `/v1/projects/${encodeURIComponent(FIREBASE_PROJECT_ID)}/databases/(default)/documents/` +
    `${encodeURIComponent(collection)}/${encodeURIComponent(docId)}`;

  return new Promise((resolve, reject) => {
    const req = https.request({
      hostname: 'firestore.googleapis.com',
      port: 443,
      path,
      method: 'DELETE',
      headers: { Authorization: `Bearer ${token}` },
      timeout: 10000
    }, (res) => {
      res.resume();
      if (res.statusCode >= 200 && res.statusCode < 300) resolve(true);
      else reject(new Error(`Firestore delete failed: HTTP ${res.statusCode}`));
    });
    req.on('timeout', () => req.destroy(new Error('Firestore timeout')));
    req.on('error', reject);
    req.end();
  });
}

module.exports.handler = async function handler(event) {
  const method = String(event?.httpMethod || 'GET').toUpperCase();
  if (method === 'OPTIONS') return json(200, { ok: true });
  if (method !== 'GET' && method !== 'POST') {
    return json(405, { ok: false, error: 'METHOD_NOT_ALLOWED' }, { Allow: 'GET, POST, OPTIONS' });
  }

  const query = event?.queryStringParameters || {};
  let body = {};
  try {
    const raw = event?.isBase64Encoded
      ? Buffer.from(event.body || '', 'base64').toString('utf8')
      : event?.body;

    if (typeof raw === 'string' && Buffer.byteLength(raw, 'utf8') > MAX_REQUEST_BODY_BYTES) {
      return json(413, { ok: false, error: 'PAYLOAD_TOO_LARGE' });
    }

    body = typeof raw === 'string' && raw ? JSON.parse(raw) : (raw || {});
  } catch (_) {
    body = {};
  }

  const action = cleanText(query.action || body.action, 40).toLowerCase();
  const callback = query.callback || '';

  try {
    if (action === 'health') {
      return jsonpOrJson(200, {
        ok: true,
        service: 'kapterka-payment-api',
        shopId: YOOKASSA_SHOP_ID,
        secretConfigured: Boolean(YOOKASSA_SECRET_KEY),
        licenseRegistryConfigured: Boolean(readFirebaseServiceAccount()),
        adminAuthConfigured: Boolean(ADMIN_API_SECRET_SHA256),
        adminSessionConfigured: Boolean(ADMIN_SESSION_SECRET),
        emailConfigured: Boolean(BREVO_API_KEY && EMAIL_SENDER_EMAIL)
      }, callback);
    }

    if (action === 'fighter_upsert') {
      const fighterId = cleanText(body.fighter_id, 100);
      const callsign = cleanText(body.callsign || 'Боец', 80);
      const unitName = cleanText(body.unit_name || 'Подразделение', 120);
      const unitKey = cleanText(body.unit_key, 120);
      const email = cleanEmail(body.email);
      const deviceModel = cleanText(body.device_model || 'Android', 120);

      if (!fighterId) return json(400, { ok: false, error: 'MISSING_FIGHTER_ID' });

      const now = Date.now();
      const existing = await getFighterById(fighterId);

      if (existing) {
        const storedEmail = cleanEmail(existing.email);

        if (storedEmail && email && storedEmail === email) {
          // The same identity boundary used for lookup (fighter id + registered
          // email) may update ordinary profile metadata. License fields remain
          // server-authoritative and are never accepted from Android.
          await patchFirestoreDocument('fighters', fighterId, {
            callsign,
            unitName,
            unitKey,
            lastSeenAt: now,
            deviceModel
          });
          return json(200, { ok: true, existing: true, profile_updated: true });
        }

        // Identity mismatch/legacy blank email: heartbeat only. Do not overwrite
        // identity, unit key, email or license state.
        await patchFirestoreDocument('fighters', fighterId, {
          lastSeenAt: now,
          deviceModel
        });
        return json(200, { ok: true, existing: true, profile_updated: false });
      }

      let activeLicense = null;
      if (email) {
        try { activeLicense = await queryActiveLicenseByEmail(email); } catch (_) {}
      }

      await patchFirestoreDocument('fighters', fighterId, {
        fighterId,
        callsign,
        role: 'Старшина подразделения',
        unitName,
        unitKey,
        email,
        deviceModel,
        registeredAt: now,
        lastSeenAt: now,
        licenseKey: activeLicense?.licenseKey || '',
        expiresAt: activeLicense?.expiresAt || 0,
        isProActive: Boolean(activeLicense && activeLicense.expiresAt > now)
      });

      return json(200, { ok: true, existing: false });
    }

    if (action === 'fighter_lookup') {
      const fighterId = cleanText(body.fighter_id, 100);
      const email = cleanEmail(body.email);
      if (!fighterId) {
        return json(400, { ok: false, error: 'MISSING_FIGHTER_ID' });
      }
      if (!email) {
        return json(400, { ok: false, error: 'INVALID_EMAIL' });
      }

      const fighter = await getFighterById(fighterId);
      if (!fighter || fighter.id !== fighterId) {
        return json(404, { ok: false, error: 'FIGHTER_NOT_FOUND' });
      }

      const storedEmail = cleanEmail(fighter.email);
      if (!storedEmail || storedEmail !== email) {
        return json(403, { ok: false, error: 'FIGHTER_IDENTITY_MISMATCH' });
      }

      return json(200, {
        ok: true,
        fighter: {
          id: fighter.id,
          unit_name: fighter.unitName,
          unit_key: fighter.unitKey
        }
      });
    }

    if (action === 'license_verify') {
      const licenseKey = cleanText(body.license_key, 40).toUpperCase();
      const fighterId = cleanText(body.fighter_id, 100);
      if (!/^(KAPT|KPT)-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}$/.test(licenseKey)) {
        return json(400, { ok: false, error: 'INVALID_LICENSE_KEY' });
      }

      const license = await getFirestoreDocument('licenses', licenseKey);
      if (!license || license.status !== 'ACTIVE' || license.expiresAt <= Date.now()) {
        return json(404, { ok: false, error: 'LICENSE_NOT_ACTIVE' });
      }
      if (license.fighterId && license.fighterId !== fighterId) {
        return json(403, { ok: false, error: 'FIGHTER_MISMATCH' });
      }

      return json(200, {
        ok: true,
        license_key: license.licenseKey,
        expires_at: license.expiresAt,
        fighter_id: license.fighterId || ''
      });
    }

    if (action === 'license_restore') {
      const email = cleanEmail(body.email);
      const fighterId = cleanText(body.fighter_id, 100);
      if (!email) return json(400, { ok: false, error: 'INVALID_EMAIL' });
      if (!fighterId) return json(400, { ok: false, error: 'MISSING_FIGHTER_ID' });

      const license = await queryActiveLicenseByEmail(email);
      if (!license) {
        return json(404, { ok: false, error: 'LICENSE_NOT_FOUND' });
      }

      // Email alone is not enough to disclose a reusable license key.
      // Normal backup/device transfer preserves fighter_personal_id, so a legitimate
      // restored installation can be matched without weakening the trust boundary.
      if (!license.fighterId || license.fighterId !== fighterId) {
        return json(403, {
          ok: false,
          error: 'LICENSE_RESTORE_IDENTITY_MISMATCH'
        });
      }

      return json(200, {
        ok: true,
        license_key: license.licenseKey,
        expires_at: license.expiresAt,
        fighter_id: license.fighterId
      });
    }

    if (action === 'send_license_email') {
      const licenseKey = cleanText(body.license_key, 40).toUpperCase();
      const requestedEmail = cleanEmail(body.email);
      if (!/^KAPT-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}$/.test(licenseKey) || !requestedEmail) {
        return json(400, { ok: false, error: 'INVALID_LICENSE_EMAIL_REQUEST' });
      }

      const license = await getFirestoreDocument('licenses', licenseKey);
      if (!license || license.status !== 'ACTIVE' || license.expiresAt <= Date.now()) {
        return json(404, { ok: false, error: 'LICENSE_NOT_ACTIVE' });
      }
      if (!license.email || license.email.toLowerCase() !== requestedEmail) {
        return json(403, { ok: false, error: 'LICENSE_EMAIL_MISMATCH' });
      }

      const reservation = reserveLicenseEmailSend(licenseKey, requestedEmail);
      if (!reservation.ok) {
        return json(429, {
          ok: false,
          error: 'LICENSE_EMAIL_RATE_LIMITED',
          retry_after_seconds: Math.max(1, Math.ceil(reservation.retryAfterMs / 1000))
        });
      }

      const daysLeft = Math.max(1, Math.ceil((license.expiresAt - Date.now()) / (24 * 60 * 60 * 1000)));
      try {
        await sendLicenseEmailViaBrevo({
          toEmail: license.email,
          callsign: license.callsign,
          licenseKey,
          days: daysLeft
        });
        await sendTelegram(
          '✉️ <b>Лицензионное письмо отправлено</b>\n' +
          'Email: <code>' + escapeTelegramHtml(license.email) + '</code>\n' +
          'Ключ: <code>' + escapeTelegramHtml(licenseKey.slice(0, 5) + '-****-****-' + licenseKey.slice(-4)) + '</code>'
        ).catch(() => false);
        return json(200, { ok: true });
      } catch (emailError) {
        licenseEmailLastSentAt.delete(reservation.key);
        console.error('License email error:', emailError?.message || emailError);
        return json(503, { ok: false, error: 'EMAIL_PROVIDER_UNAVAILABLE' });
      }
    }

    if (action === 'admin_auth') {
      const sourceKey = adminAuthSourceKey(event);
      if (adminAuthIsBlocked(sourceKey)) {
        return json(429, {
          ok: false,
          error: 'ADMIN_AUTH_RATE_LIMITED',
          retry_after_seconds: Math.floor(ADMIN_AUTH_WINDOW_MS / 1000)
        });
      }

      const secret = cleanText(body.secret, 256);
      if (!adminSecretMatches(secret)) {
        recordAdminAuthFailure(sourceKey);
        return json(403, { ok: false, error: 'ADMIN_AUTH_FAILED' });
      }

      clearAdminAuthFailures(sourceKey);
      const adminToken = issueAdminToken();
      if (!adminToken) {
        return json(503, { ok: false, error: 'ADMIN_SESSION_NOT_CONFIGURED' });
      }
      return json(200, {
        ok: true,
        admin_token: adminToken,
        expires_in_seconds: Math.floor(ADMIN_TOKEN_TTL_MS / 1000)
      });
    }

    if (action === 'admin_list_fighters') {
      if (!verifyAdminToken(body.admin_token)) {
        return json(403, { ok: false, error: 'ADMIN_SESSION_INVALID' });
      }
      const fighters = await listFightersFromFirestore();
      return json(200, {
        ok: true,
        fighters: fighters.map((fighter) => ({
          id: fighter.id,
          callsign: fighter.callsign,
          role: fighter.role,
          unit_name: fighter.unitName,
          unit_key: fighter.unitKey,
          license_key: fighter.licenseKey,
          expires_at: fighter.expiresAt,
          registered_at: fighter.registeredAt,
          last_seen_at: fighter.lastSeenAt,
          email: fighter.email,
          device_model: fighter.deviceModel
        }))
      });
    }

    if (action === 'admin_grant_license') {
      if (!verifyAdminToken(body.admin_token)) {
        return json(403, { ok: false, error: 'ADMIN_SESSION_INVALID' });
      }
      const fighterId = cleanText(body.fighter_id, 100);
      const days = Math.min(365, Math.max(1, Number.parseInt(body.days, 10) || 30));
      if (!fighterId) return json(400, { ok: false, error: 'MISSING_FIGHTER_ID' });

      const fighter = await getFighterById(fighterId);
      if (!fighter) {
        return json(404, { ok: false, error: 'FIGHTER_NOT_FOUND' });
      }

      const now = Date.now();
      let licenseKey = cleanText(fighter.licenseKey, 40).toUpperCase();
      let existingLicense = null;

      if (/^KAPT-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}$/.test(licenseKey)) {
        existingLicense = await getFirestoreDocument('licenses', licenseKey);
      }

      let expiresAt;
      if (existingLicense &&
          existingLicense.status === 'ACTIVE' &&
          existingLicense.fighterId === fighterId &&
          existingLicense.expiresAt > now
      ) {
        // "+N days" really extends the current license and keeps the same key.
        expiresAt = existingLicense.expiresAt + days * 24 * 60 * 60 * 1000;
        await patchFirestoreDocument('licenses', licenseKey, {
          expiresAt,
          status: 'ACTIVE'
        });
      } else {
        licenseKey = generateAdminLicenseKey();
        expiresAt = now + days * 24 * 60 * 60 * 1000;
        await registerLicenseInFirestore({
          licenseKey,
          fighterId,
          callsign: cleanText(fighter.callsign || '', 80),
          email: cleanEmail(fighter.email),
          paymentId: '',
          amount: 0,
          activatedAt: now,
          expiresAt,
          durationDays: days,
          status: 'ACTIVE',
          source: 'Admin server grant'
        });
      }

      await patchFirestoreDocument('fighters', fighterId, {
        licenseKey,
        expiresAt,
        isProActive: true
      });

      return json(200, {
        ok: true,
        license_key: licenseKey,
        expires_at: expiresAt,
        days
      });
    }

    if (action === 'admin_delete_fighter') {
      if (!verifyAdminToken(body.admin_token)) {
        return json(403, { ok: false, error: 'ADMIN_SESSION_INVALID' });
      }
      const fighterId = cleanText(body.fighter_id, 100);
      if (!fighterId) return json(400, { ok: false, error: 'MISSING_FIGHTER_ID' });

      // Delete only the registry entry. Licenses and unit data are intentionally preserved.
      await deleteFirestoreDocument('fighters', fighterId);
      return json(200, { ok: true });
    }

    // Compatibility endpoint used by the current public site/app notifications.
    // It sends only to the server-configured admin chat; caller-supplied chat_id is ignored.
    if (action === 'send_telegram') {
      const text = cleanText(body.text || query.text, 3500);
      if (!text) return jsonpOrJson(400, { ok: false, error: 'EMPTY_TEXT' }, callback);
      const delivered = await sendTelegram(text);
      return jsonpOrJson(200, { ok: delivered }, callback);
    }

    if (action === 'create' || action === 'pay') {
      const email = cleanEmail(query.email || body.email);
      const callsign = cleanText(query.callsign || body.callsign || 'Пользователь', 80);
      const fighterId = cleanText(query.fighter_id || body.fighter_id, 100);
      if (!email) return jsonpOrJson(400, { ok: false, error: 'INVALID_EMAIL' }, callback);

      const returnUrlRaw = cleanText(query.return_url || body.return_url, 300);
      const allowedReturnUrl =
        returnUrlRaw.startsWith('kapterka://payment_success') ||
        returnUrlRaw.startsWith('https://kapterka-pro.ru/');
      const returnUrl = allowedReturnUrl
        ? returnUrlRaw
        : 'https://kapterka-pro.ru/?payment=check#tabPayment';

      const idempotenceKey = cleanText(
        query.idempotence_key || body.idempotence_key || crypto.randomUUID(),
        100
      );

      const metadata = {
        callsign,
        email,
        duration_days: '30',
        product: 'kapterka_pro_30d'
      };
      if (fighterId) metadata.fighter_id = fighterId;

      const payment = await requestYooKassa(
        'POST',
        '/v3/payments',
        {
          amount: {
            value: PAYMENT_AMOUNT_RUB.toFixed(2),
            currency: 'RUB'
          },
          confirmation: {
            type: 'redirect',
            return_url: returnUrl
          },
          capture: true,
          description: 'Каптёрка PRO — 30 дней',
          metadata
        },
        idempotenceKey
      );

      await sendTelegram(
        '💳 <b>Новый платеж Каптёрка ПРО</b>\n' +
        'Позывной: <b>' + escapeTelegramHtml(callsign) + '</b>\n' +
        'Email: <code>' + escapeTelegramHtml(email) + '</code>\n' +
        'Сумма: <b>' + PAYMENT_AMOUNT_RUB + ' ₽</b>\n' +
        'Payment ID: <code>' + escapeTelegramHtml(payment.id || '') + '</code>'
      ).catch(() => false);

      return jsonpOrJson(200, {
        ok: true,
        payment_id: payment.id || '',
        confirmation_url: payment.confirmation?.confirmation_url || ''
      }, callback);
    }

    if (action === 'check') {
      const paymentId = cleanText(query.payment_id || body.payment_id, 100);
      const requestedFighterId = cleanText(query.fighter_id || body.fighter_id, 100);
      if (!paymentId) {
        return jsonpOrJson(400, { ok: false, error: 'MISSING_PAYMENT_ID' }, callback);
      }

      const payment = await requestYooKassa(
        'GET',
        `/v3/payments/${encodeURIComponent(paymentId)}`
      );
      const status = payment.status || 'unknown';
      const paid = status === 'succeeded' && payment.paid === true;

      if (!paid) {
        return jsonpOrJson(200, {
          ok: true,
          paid: false,
          status
        }, callback);
      }

      if (!amountMatchesTariff(payment)) {
        return jsonpOrJson(409, {
          ok: false,
          paid: false,
          status,
          error: 'PAYMENT_AMOUNT_MISMATCH'
        }, callback);
      }

      const paymentFighterId = cleanText(payment.metadata?.fighter_id, 100);
      if (paymentFighterId && requestedFighterId !== paymentFighterId) {
        return jsonpOrJson(403, {
          ok: false,
          paid: false,
          status,
          error: 'FIGHTER_MISMATCH'
        }, callback);
      }

      const licenseKey = keyForPayment(paymentId);
      const activatedAt = paymentTimestampMillis(payment);
      let expiresAt = activatedAt + LICENSE_DURATION_MS;
      const email = cleanEmail(payment.metadata?.email);
      const callsign = cleanText(payment.metadata?.callsign || 'Пользователь', 80);

      // The same succeeded payment always maps to the same license. Once that
      // license has been bound to a fighter, a repeated status check must never
      // move it to another fighter.
      let existingLicense = null;
      try {
        existingLicense = await getFirestoreDocument('licenses', licenseKey);
      } catch (existingReadError) {
        console.error('Existing license read error:', existingReadError?.message || existingReadError);
        return jsonpOrJson(503, {
          ok: false,
          paid: true,
          status,
          error: 'LICENSE_REGISTRY_UNAVAILABLE'
        }, callback);
      }

      const existingFighterId = cleanText(existingLicense?.fighterId, 100);
      if (existingFighterId &&
          requestedFighterId &&
          existingFighterId !== requestedFighterId
      ) {
        return jsonpOrJson(403, {
          ok: false,
          paid: false,
          status,
          error: 'LICENSE_FIGHTER_MISMATCH'
        }, callback);
      }

      const fighterId = existingFighterId || paymentFighterId || requestedFighterId;

      if (existingLicense && existingLicense.expiresAt > 0) {
        // Re-checking the same payment is idempotent: never extend twice.
        expiresAt = existingLicense.expiresAt;
      } else if (fighterId) {
        try {
          const currentFighter = await getFighterById(fighterId);
          if (currentFighter && currentFighter.expiresAt > activatedAt) {
            // Renewal adds 30 days to the remaining paid/admin entitlement instead
            // of discarding the user's unused time.
            expiresAt = currentFighter.expiresAt + LICENSE_DURATION_MS;
          }
        } catch (fighterReadError) {
          console.error('Fighter renewal read error:', fighterReadError?.message || fighterReadError);
          return jsonpOrJson(503, {
            ok: false,
            paid: true,
            status,
            error: 'LICENSE_REGISTRY_UNAVAILABLE'
          }, callback);
        }
      }

      try {
        await registerLicenseInFirestore({
          licenseKey,
          fighterId,
          callsign,
          email,
          paymentId,
          amount: PAYMENT_AMOUNT_RUB,
          activatedAt,
          expiresAt,
          durationDays: 30,
          status: 'ACTIVE',
          source: 'YooKassa server verification'
        });
        if (fighterId) {
          try {
            await patchFirestoreDocument('fighters', fighterId, {
              licenseKey,
              expiresAt,
              isProActive: expiresAt > Date.now(),
              lastSeenAt: Date.now()
            });
          } catch (fighterPatchError) {
            console.error('Fighter license mirror error:', fighterPatchError?.message || fighterPatchError);
          }
        }
      } catch (registryError) {
        console.error('License registry error:', registryError?.message || registryError);
        return jsonpOrJson(503, {
          ok: false,
          paid: true,
          status,
          error: 'LICENSE_REGISTRY_UNAVAILABLE'
        }, callback);
      }

      const responseStatus = expiresAt > Date.now() ? status : 'expired';
      const response = {
        ok: true,
        paid: true,
        status: responseStatus,
        license_key: licenseKey,
        // Legacy website compatibility:
        key: licenseKey,
        expires_at: expiresAt
      };
      return jsonpOrJson(200, response, callback);
    }

    return jsonpOrJson(400, { ok: false, error: 'INVALID_ACTION' }, callback);
  } catch (error) {
    console.error('Kapterka payment backend error:', error?.message || error);
    return jsonpOrJson(502, {
      ok: false,
      error: 'UPSTREAM_ERROR'
    }, callback);
  }
};
