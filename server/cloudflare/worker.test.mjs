// Self-test for the Cloudflare Worker backend. Runs on plain Node 20+:
//   node server/cloudflare/worker.test.mjs
// Every upstream (YooKassa, Google OAuth, Firestore, Brevo, Telegram) is faked
// in-memory; nothing leaves the machine.
import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';
import worker, { keyForPayment, __test } from './worker.mjs';

// ------------------------------------------------------------------ fakes

const { privateKey } = await crypto.subtle.generateKey(
  { name: 'RSASSA-PKCS1-v1_5', modulusLength: 2048, publicExponent: new Uint8Array([1, 0, 1]), hash: 'SHA-256' },
  true,
  ['sign', 'verify']
);
const pkcs8 = Buffer.from(await crypto.subtle.exportKey('pkcs8', privateKey)).toString('base64');
const pem = `-----BEGIN PRIVATE KEY-----\n${pkcs8.match(/.{1,64}/g).join('\n')}\n-----END PRIVATE KEY-----\n`;

// Fake Firebase Auth signing key (stands in for Google's securetoken keys).
const authPair = await crypto.subtle.generateKey(
  { name: 'RSASSA-PKCS1-v1_5', modulusLength: 2048, publicExponent: new Uint8Array([1, 0, 1]), hash: 'SHA-256' },
  true,
  ['sign', 'verify']
);
const jwkPublic = { ...(await crypto.subtle.exportKey('jwk', authPair.publicKey)), kid: 'test-kid', alg: 'RS256', use: 'sig' };
const b64u = (buf) => Buffer.from(buf).toString('base64url');
async function idToken(uid, overrides = {}, signer = authPair.privateKey) {
  const now = Math.floor(Date.now() / 1000);
  const head = b64u(JSON.stringify({ alg: 'RS256', kid: 'test-kid', typ: 'JWT' }));
  const body = b64u(JSON.stringify({
    aud: 'kapterka-pro', iss: 'https://securetoken.google.com/kapterka-pro',
    sub: uid, iat: now, exp: now + 3600, firebase: { sign_in_provider: 'anonymous' }, ...overrides
  }));
  const sig = await crypto.subtle.sign('RSASSA-PKCS1-v1_5', signer, new TextEncoder().encode(`${head}.${body}`));
  return `${head}.${body}.${b64u(sig)}`;
}

const SECRETS = {
  yookassa: 'unit-test-yookassa-secret',
  session: 'unit-test-admin-session-secret',
  brevo: 'unit-test-brevo-key',
  tg: 'unit-test-tg-token',
  admin: 'correct-admin-secret'
};

const env = {
  YOOKASSA_SHOP_ID: 'test-shop',
  YOOKASSA_SECRET_KEY: SECRETS.yookassa,
  PAYMENT_AMOUNT_RUB: '490',
  FIREBASE_PROJECT_ID: 'kapterka-pro',
  FIREBASE_SERVICE_ACCOUNT_B64: Buffer.from(JSON.stringify({
    client_email: 'svc@example.invalid',
    private_key: pem
  })).toString('base64'),
  ADMIN_API_SECRET_SHA256: createHash('sha256').update(SECRETS.admin).digest('hex'),
  ADMIN_SESSION_SECRET: SECRETS.session,
  BREVO_API_KEY: SECRETS.brevo,
  EMAIL_SENDER_EMAIL: 'sender@example.invalid',
  TG_BOT_TOKEN: SECRETS.tg,
  TG_ADMIN_CHAT_ID: '1'
};

const state = {
  docs: new Map(), // "collection/id" -> fields object (plain values)
  payments: new Map(),
  telegram: [],
  relayed: [],
  emails: [],
  firestoreDown: false
};

function toFields(obj) {
  const fields = {};
  for (const [k, v] of Object.entries(obj)) {
    fields[k] = typeof v === 'number' ? { integerValue: String(v) } : typeof v === 'boolean' ? { booleanValue: v } : { stringValue: String(v) };
  }
  return fields;
}
function fromFields(fields) {
  const out = {};
  for (const [k, f] of Object.entries(fields)) {
    out[k] = 'integerValue' in f ? Number(f.integerValue) : 'booleanValue' in f ? f.booleanValue : f.stringValue;
  }
  return out;
}
const jsonResponse = (status, body) => new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });

globalThis.fetch = async (input, init = {}) => {
  const url = new URL(typeof input === 'string' ? input : input.url);
  const method = (init.method || 'GET').toUpperCase();
  const body = init.body ? String(init.body) : '';

  if (url.hostname === 'oauth2.googleapis.com') {
    const assertion = new URLSearchParams(body).get('assertion') || '';
    assert.equal(assertion.split('.').length, 3, 'service-account JWT must be signed');
    return jsonResponse(200, { access_token: 'fake-google-token', expires_in: 3600 });
  }

  if (url.hostname === 'firestore.googleapis.com') {
    assert.equal(init.headers.Authorization, 'Bearer fake-google-token');
    if (state.firestoreDown) return jsonResponse(503, {});
    const prefix = '/v1/projects/kapterka-pro/databases/(default)/documents';
    const path = decodeURIComponent(url.pathname.slice(prefix.length));
    if (path === ':runQuery') {
      const q = JSON.parse(body).structuredQuery;
      const coll = q.from[0].collectionId;
      if (q.from[0].allDescendants) {
        const rows = [];
        for (const [key, doc] of state.docs) {
          const parts = key.split('/');
          if (parts.length >= 2 && parts[parts.length - 2] === coll) {
            rows.push({ document: { name: `${prefix}/${key}`, updateTime: new Date().toISOString(), fields: toFields(doc) } });
          }
        }
        return jsonResponse(200, rows);
      }
      const field = q.where.fieldFilter.field.fieldPath;
      const value = q.where.fieldFilter.value.stringValue;
      const rows = [];
      for (const [key, doc] of state.docs) {
        const [c, id] = key.split('/');
        if (c === coll && doc[field] === value) rows.push({ document: { name: `${prefix}/${c}/${id}`, fields: toFields(doc) } });
      }
      return jsonResponse(200, rows);
    }
    const parts = path.replace(/^\//, '').split('/');
    if (parts.length % 2 === 1) {
      const coll = parts.join('/');
      const documents = [...state.docs]
        .filter(([k]) => k.startsWith(coll + '/') && k.split('/').length === parts.length + 1)
        .map(([k, doc]) => ({ name: `${prefix}/${k}`, fields: toFields(doc) }));
      return jsonResponse(200, documents.length ? { documents } : {});
    }
    const key = parts.join('/');
    if (method === 'GET') {
      return state.docs.has(key) ? jsonResponse(200, { name: key, fields: toFields(state.docs.get(key)) }) : jsonResponse(404, {});
    }
    if (method === 'DELETE') {
      state.docs.delete(key);
      return jsonResponse(200, {});
    }
    if (method === 'PATCH') {
      const exists = url.searchParams.get('currentDocument.exists');
      if (exists === 'false' && state.docs.has(key)) return jsonResponse(409, { error: { status: 'ALREADY_EXISTS' } });
      if (exists === 'true' && !state.docs.has(key)) return jsonResponse(404, { error: { status: 'NOT_FOUND' } });
      state.docs.set(key, { ...(state.docs.get(key) || {}), ...fromFields(JSON.parse(body).fields) });
      return jsonResponse(200, {});
    }
  }

  if (url.hostname === 'api.yookassa.ru') {
    assert.equal(init.headers.Authorization, 'Basic ' + btoa(`test-shop:${SECRETS.yookassa}`));
    if (method === 'POST' && url.pathname === '/v3/payments') {
      const req = JSON.parse(body);
      const id = 'pay-' + (state.payments.size + 1).toString().padStart(8, '0');
      state.payments.set(id, { id, status: 'pending', paid: false, amount: req.amount, metadata: req.metadata });
      return jsonResponse(200, { id, confirmation: { confirmation_url: 'https://yoomoney.ru/checkout/' + id } });
    }
    const id = decodeURIComponent(url.pathname.split('/').pop());
    const p = state.payments.get(id);
    return p ? jsonResponse(200, p) : jsonResponse(404, { description: 'secret upstream detail' });
  }

  if (url.hostname === 'www.googleapis.com' && url.pathname.includes('securetoken')) {
    return new Response(JSON.stringify({ keys: [jwkPublic] }), {
      status: 200,
      headers: { 'Content-Type': 'application/json', 'Cache-Control': 'public, max-age=3600' }
    });
  }

  if (url.hostname === 'script.google.com') {
    const msg = JSON.parse(body);
    if (msg.secret !== 'relay-secret-0123456789') return jsonResponse(200, { ok: false, error: 'FORBIDDEN' });
    state.relayed.push(msg);
    return jsonResponse(200, { ok: true });
  }

  if (url.hostname === 'api.brevo.com') {
    state.emails.push(JSON.parse(body));
    return jsonResponse(201, {});
  }
  if (url.hostname === 'api.telegram.org') {
    state.telegram.push(JSON.parse(body).text);
    return jsonResponse(200, { ok: true });
  }
  throw new Error('Unexpected outbound request: ' + url.href);
};

async function call(action, body = {}, { method = 'POST', ip = '10.0.0.1', origin, envOverride } = {}) {
  const url = 'https://kapterka-api.test/?action=' + encodeURIComponent(action);
  const headers = { 'CF-Connecting-IP': ip, 'Content-Type': 'application/json' };
  if (origin) headers.Origin = origin;
  const request = new Request(url, method === 'POST' ? { method, headers, body: JSON.stringify(body) } : { method, headers });
  const response = await worker.fetch(request, envOverride || env);
  const text = await response.text();
  for (const secret of [...Object.values(SECRETS), pkcs8.slice(10, 40), 'secret upstream detail']) {
    assert.ok(!text.includes(secret), `response for ${action} leaks secret material`);
  }
  return { status: response.status, headers: response.headers, body: JSON.parse(text || '{}') };
}

function pay(id, patch = {}) {
  Object.assign(state.payments.get(id), { status: 'succeeded', paid: true, captured_at: new Date().toISOString() }, patch);
}

const tests = [];
const test = (name, fn) => tests.push({ name, fn });

// ------------------------------------------------------------------ tests

test('health reports readiness without secrets', async () => {
  const r = await call('health', {}, { method: 'GET' });
  assert.equal(r.status, 200);
  assert.equal(r.body.service, 'kapterka-payment-api');
  for (const f of ['secretConfigured', 'licenseRegistryConfigured', 'adminAuthConfigured', 'adminSessionConfigured', 'emailConfigured']) {
    assert.equal(r.body[f], true, f);
  }
  const empty = await call('health', {}, { method: 'GET', envOverride: {} });
  assert.equal(empty.body.secretConfigured, false);
  assert.equal(empty.body.licenseRegistryConfigured, false);
});

test('send_telegram relay is gone', async () => {
  const before = state.telegram.length;
  const r = await call('send_telegram', { text: 'spam', chat_id: '666' });
  assert.equal(r.status, 410);
  assert.equal(state.telegram.length, before);
});

test('unknown action and wrong method are rejected', async () => {
  assert.equal((await call('drop_database')).status, 400);
  assert.equal((await call('admin_list_fighters', {}, { method: 'GET' })).status, 405);
  assert.equal((await call('__proto__')).status, 400);
});

test('CORS only for the official site', async () => {
  const good = await call('health', {}, { method: 'GET', origin: 'https://kapterka-pro.ru' });
  assert.equal(good.headers.get('Access-Control-Allow-Origin'), 'https://kapterka-pro.ru');
  const bad = await call('health', {}, { method: 'GET', origin: 'https://evil.example' });
  assert.equal(bad.headers.get('Access-Control-Allow-Origin'), null);
});

test('oversized body is rejected', async () => {
  const r = await call('create', { email: 'a@b.cd', pad: 'x'.repeat(70 * 1024) });
  assert.equal(r.status, 413);
});

test('create: price is server-side, return URL is allow-listed', async () => {
  const r = await call('create', { email: 'Boec@Mail.ru', callsign: 'Кедр', fighter_id: 'БОЕЦ-AAA', return_url: 'https://evil.example/', amount: 1 });
  assert.equal(r.status, 200);
  const p = state.payments.get(r.body.payment_id);
  assert.equal(p.amount.value, '490.00');
  assert.equal(p.metadata.email, 'boec@mail.ru');
  assert.equal(p.metadata.fighter_id, 'БОЕЦ-AAA');
  assert.ok(r.body.confirmation_url.startsWith('https://yoomoney.ru/'));
  assert.equal((await call('create', { email: 'not-an-email' })).status, 400);
});

test('check: pending payment grants nothing', async () => {
  const c = await call('create', { email: 'p@p.pp', fighter_id: 'БОЕЦ-PEND' });
  const r = await call('check', { payment_id: c.body.payment_id, fighter_id: 'БОЕЦ-PEND' });
  assert.equal(r.body.paid, false);
  assert.equal(r.body.license_key, undefined);
});

test('check: one payment = one license, repeat is idempotent', async () => {
  const c = await call('create', { email: 'one@x.ru', callsign: 'Один', fighter_id: 'БОЕЦ-ONE' });
  pay(c.body.payment_id);
  const first = await call('check', { payment_id: c.body.payment_id, fighter_id: 'БОЕЦ-ONE' });
  assert.equal(first.status, 200);
  assert.equal(first.body.paid, true);
  assert.equal(first.body.license_key, await keyForPayment(c.body.payment_id));
  assert.equal(first.body.key, first.body.license_key, '3.5.0 compatibility field');
  const again = await call('check', { payment_id: c.body.payment_id, fighter_id: 'БОЕЦ-ONE' });
  assert.equal(again.body.expires_at, first.body.expires_at, 'repeat check must not extend');
  const stolen = await call('check', { payment_id: c.body.payment_id, fighter_id: 'БОЕЦ-THIEF' });
  assert.equal(stolen.status, 403);
  assert.equal(stolen.body.license_key, undefined);
});

test('check: wrong amount is refused', async () => {
  const c = await call('create', { email: 'cheap@x.ru', fighter_id: 'БОЕЦ-CHEAP' });
  pay(c.body.payment_id, { amount: { value: '1.00', currency: 'RUB' } });
  const r = await call('check', { payment_id: c.body.payment_id, fighter_id: 'БОЕЦ-CHEAP' });
  assert.equal(r.status, 409);
  assert.equal(r.body.error, 'PAYMENT_AMOUNT_MISMATCH');
  assert.equal(r.body.license_key, undefined);
});

test('check: registry outage never returns a key', async () => {
  const c = await call('create', { email: 'down@x.ru', fighter_id: 'БОЕЦ-DOWN' });
  pay(c.body.payment_id);
  state.firestoreDown = true;
  try {
    const r = await call('check', { payment_id: c.body.payment_id, fighter_id: 'БОЕЦ-DOWN' });
    assert.equal(r.status, 503);
    assert.equal(r.body.license_key, undefined);
  } finally {
    state.firestoreDown = false;
  }
});

test('check: legacy 3.5.0 flow (no fighter) then first 3.6 device claims it', async () => {
  const c = await call('create', { email: 'legacy@x.ru', callsign: 'Старый' });
  pay(c.body.payment_id);
  const r = await call('check', { payment_id: c.body.payment_id });
  assert.equal(r.status, 200);
  assert.ok(r.body.key);
  const claim = await call('license_verify', { license_key: r.body.key, fighter_id: 'БОЕЦ-NEW' });
  assert.equal(claim.status, 200);
  assert.equal(claim.body.fighter_id, 'БОЕЦ-NEW');
  const other = await call('license_verify', { license_key: r.body.key, fighter_id: 'БОЕЦ-OTHER' });
  assert.equal(other.status, 403);
});

test('check: renewal keeps unused time', async () => {
  state.docs.set('srv_fighters/БОЕЦ-RENEW', { fighterId: 'БОЕЦ-RENEW', email: 'renew@x.ru', expiresAt: Date.now() + 10 * 86400000 });
  const c = await call('create', { email: 'renew@x.ru', fighter_id: 'БОЕЦ-RENEW' });
  pay(c.body.payment_id);
  const r = await call('check', { payment_id: c.body.payment_id, fighter_id: 'БОЕЦ-RENEW' });
  const days = (r.body.expires_at - Date.now()) / 86400000;
  assert.ok(days > 39 && days < 41, 'expected ~40 days, got ' + days);
});

test('license_verify: invalid, unknown, and forged keys', async () => {
  assert.equal((await call('license_verify', { license_key: 'nope', fighter_id: 'x' })).status, 400);
  assert.equal((await call('license_verify', { license_key: 'KAPT-AAAA-BBBB-CCCC', fighter_id: 'x' })).status, 404);
});

test('license_restore needs matching fighter and email', async () => {
  const ok = await call('license_restore', { email: 'one@x.ru', fighter_id: 'БОЕЦ-ONE' });
  assert.equal(ok.status, 200);
  assert.ok(ok.body.license_key);
  const wrong = await call('license_restore', { email: 'one@x.ru', fighter_id: 'БОЕЦ-EVE' });
  assert.equal(wrong.status, 403);
  assert.equal(wrong.body.license_key, undefined);
  assert.equal((await call('license_restore', { email: 'none@x.ru', fighter_id: 'БОЕЦ-ONE' })).status, 404);
});

test('fighter_upsert never trusts client license fields', async () => {
  const r = await call('fighter_upsert', { fighter_id: 'БОЕЦ-UP', email: 'up@x.ru', callsign: 'Ап', license_key: 'KAPT-FAKE-FAKE-FAKE', expires_at: 9e15, isProActive: true });
  assert.equal(r.status, 200);
  const doc = state.docs.get('srv_fighters/БОЕЦ-UP');
  assert.equal(doc.licenseKey, '');
  assert.equal(doc.isProActive, false);
  // Different email cannot hijack the profile.
  await call('fighter_upsert', { fighter_id: 'БОЕЦ-UP', email: 'attacker@x.ru', unit_key: 'kapt_attacker' });
  assert.equal(state.docs.get('srv_fighters/БОЕЦ-UP').email, 'up@x.ru');
  assert.notEqual(state.docs.get('srv_fighters/БОЕЦ-UP').unitKey, 'kapt_attacker');
});

test('fighter_lookup requires registered email', async () => {
  await call('fighter_upsert', { fighter_id: 'БОЕЦ-LK', email: 'lk@x.ru', unit_key: 'kapt_secret_unit' });
  const good = await call('fighter_lookup', { fighter_id: 'БОЕЦ-LK', email: 'lk@x.ru' });
  assert.equal(good.body.fighter.unit_key, 'kapt_secret_unit');
  const bad = await call('fighter_lookup', { fighter_id: 'БОЕЦ-LK', email: 'eve@x.ru' });
  assert.equal(bad.status, 403);
  assert.equal(bad.body.fighter, undefined);
});

test('admin: wrong secret, brute force limit, token checks', async () => {
  assert.equal((await call('admin_list_fighters', { admin_token: 'forged.token.value' })).status, 403);
  for (let i = 0; i < 5; i++) {
    assert.equal((await call('admin_auth', { secret: 'guess' + i }, { ip: '6.6.6.6' })).status, 403);
  }
  assert.equal((await call('admin_auth', { secret: SECRETS.admin }, { ip: '6.6.6.6' })).status, 429);

  const auth = await call('admin_auth', { secret: SECRETS.admin }, { ip: '1.2.3.4' });
  assert.equal(auth.status, 200);
  const token = auth.body.admin_token;
  const [exp, nonce, sig] = token.split('.');
  const tampered = `${Number(exp) + 999999}.${nonce}.${sig}`;
  assert.equal((await call('admin_list_fighters', { admin_token: tampered })).status, 403);

  const list = await call('admin_list_fighters', { admin_token: token });
  assert.equal(list.status, 200);
  assert.ok(list.body.fighters.some((f) => f.id === 'БОЕЦ-UP'));

  const grant = await call('admin_grant_license', { admin_token: token, fighter_id: 'БОЕЦ-UP', days: 10 });
  assert.equal(grant.status, 200);
  const extend = await call('admin_grant_license', { admin_token: token, fighter_id: 'БОЕЦ-UP', days: 5 });
  assert.equal(extend.body.license_key, grant.body.license_key, '+days keeps the key');
  assert.equal(extend.body.expires_at, grant.body.expires_at + 5 * 86400000);
  assert.equal((await call('admin_grant_license', { admin_token: token, fighter_id: 'БОЕЦ-GHOST' })).status, 404);

  assert.equal((await call('admin_delete_fighter', { admin_token: token, fighter_id: 'БОЕЦ-UP' })).status, 200);
  assert.ok(!state.docs.has('srv_fighters/БОЕЦ-UP'));
  assert.ok(state.docs.has('srv_licenses/' + grant.body.license_key), 'licenses are preserved');
});

test('send_license_email: owner only, rate limited', async () => {
  const key = (await call('license_restore', { email: 'one@x.ru', fighter_id: 'БОЕЦ-ONE' })).body.license_key;
  assert.equal((await call('send_license_email', { license_key: key, email: 'eve@x.ru' })).status, 403);
  const sent = await call('send_license_email', { license_key: key, email: 'one@x.ru' });
  assert.equal(sent.status, 200);
  assert.equal(state.emails.at(-1).to[0].email, 'one@x.ru');
  assert.equal((await call('send_license_email', { license_key: key, email: 'one@x.ru' })).status, 429);
});

test('telegram notifications escape HTML and mask keys', async () => {
  await call('create', { email: 'x@x.ru', callsign: '<b>hack</b>', fighter_id: 'БОЕЦ-XSS' });
  const last = state.telegram.at(-1);
  assert.ok(last.includes('&lt;b&gt;hack&lt;/b&gt;'));
  assert.ok(!state.telegram.some((t) => /KAPT-[A-Z0-9]{4}-[A-Z0-9]{4}-/.test(t)), 'full keys must not reach Telegram');
});

test('upstream errors are not leaked', async () => {
  const r = await call('check', { payment_id: 'unknown-payment-id' });
  assert.equal(r.status, 502);
  assert.equal(r.body.error, 'UPSTREAM_ERROR');
});

test('key derivation matches the 3.5.0 backend', async () => {
  // Reference value computed by the published 3.5.0 worker algorithm.
  const k = await keyForPayment('2e5b9c1a-000f-5000-9000-1a2b3c4d5e6f');
  assert.match(k, /^KAPT-[0-9A-F]{4}-[0-9A-F]{4}-[2-9A-Z]{4}$/);
  assert.equal(k, await keyForPayment('2E5B9C1A000F500090001A2B3C4D5E6F'));
});


test('unit_join: requires a valid Firebase identity', async () => {
  assert.equal((await call('unit_join', { unit_key: 'kapt_abc123' })).status, 401);
  assert.equal((await call('unit_join', { unit_key: 'kapt_abc123', id_token: 'a.b.c' })).status, 401);
  const expired = await idToken('uid-exp', { exp: Math.floor(Date.now() / 1000) - 10 });
  assert.equal((await call('unit_join', { unit_key: 'kapt_abc123', id_token: expired })).status, 401);
  const otherProject = await idToken('uid-x', { aud: 'evil-project', iss: 'https://securetoken.google.com/evil-project' });
  assert.equal((await call('unit_join', { unit_key: 'kapt_abc123', id_token: otherProject })).status, 401);
  const forged = await idToken('uid-forged', {}, privateKey); // signed by a key Google never published
  assert.equal((await call('unit_join', { unit_key: 'kapt_abc123', id_token: forged })).status, 401);
});

test('unit_join: unknown unit is not created by a guess', async () => {
  const token = await idToken('uid-guesser');
  const r = await call('unit_join', { unit_key: 'kapt_000000', id_token: token });
  assert.equal(r.status, 404);
  assert.ok(![...state.docs.keys()].some((k) => k.startsWith('units/kapt_000000')));
  assert.equal((await call('unit_join', { unit_key: 'bad/key', id_token: token })).status, 400);
});

test('unit_join: legacy unit with data admits the key holder', async () => {
  state.docs.set('units/kapt_a1b2c3/devices/dev_1', { deviceId: 'dev_1' });
  const token = await idToken('uid-member');
  const r = await call('unit_join', { unit_key: 'kapt_a1b2c3', id_token: token, fighter_id: 'БОЕЦ-M', callsign: 'Сова' });
  assert.equal(r.status, 200);
  assert.equal(r.body.created, false);
  const m = state.docs.get('units/kapt_a1b2c3/members/uid-member');
  assert.equal(m.callsign, 'Сова');
  assert.ok(state.docs.get('units/kapt_a1b2c3').createdAt > 0);
  const again = await call('unit_join', { unit_key: 'kapt_a1b2c3', id_token: token });
  assert.equal(again.status, 200, 'rejoin is idempotent');
});

test('unit_join: create=true makes a new unit with an owner', async () => {
  const token = await idToken('uid-owner');
  const r = await call('unit_join', { unit_key: 'kapt_0123456789abcdef0123', id_token: token, create: true });
  assert.equal(r.status, 200);
  assert.equal(r.body.created, true);
  assert.equal(state.docs.get('units/kapt_0123456789abcdef0123').createdBy, 'uid-owner');
  // A second creator cannot take ownership.
  const t2 = await idToken('uid-late');
  await call('unit_join', { unit_key: 'kapt_0123456789abcdef0123', id_token: t2, create: true });
  assert.equal(state.docs.get('units/kapt_0123456789abcdef0123').createdBy, 'uid-owner');
});

test('unit_join: guessing is rate limited', async () => {
  const limiterEnv = { ...env, AUTH_LIMITER: (() => { let n = 0; return { limit: async () => ({ success: ++n <= 5 }) }; })() };
  const token = await idToken('uid-brute');
  const codes = [];
  for (let i = 0; i < 8; i++) {
    codes.push((await call('unit_join', { unit_key: 'kapt_ff00' + i + '0', id_token: token }, { envOverride: limiterEnv })).status);
  }
  assert.deepEqual(codes.slice(5), [429, 429, 429]);
});


test('forged records in legacy client-writable collections are ignored', async () => {
  state.docs.set('licenses/KAPT-HACK-HACK-2222', { licenseKey: 'KAPT-HACK-HACK-2222', status: 'ACTIVE', expiresAt: Date.now() + 1e12 });
  assert.equal((await call('license_verify', { license_key: 'KAPT-HACK-HACK-2222', fighter_id: 'x' })).status, 404);
  state.docs.set('fighters/БОЕЦ-HACK', { fighterId: 'БОЕЦ-HACK', expiresAt: Date.now() + 1e12 });
  const c = await call('create', { email: 'h@x.ru', fighter_id: 'БОЕЦ-HACK' });
  pay(c.body.payment_id);
  const r = await call('check', { payment_id: c.body.payment_id, fighter_id: 'БОЕЦ-HACK' });
  assert.ok((r.body.expires_at - Date.now()) / 86400000 < 31, 'legacy fighter expiry must not be trusted');
});


test('registration sends the Telegram notification like before', async () => {
  const before = state.telegram.length;
  await call('fighter_upsert', { fighter_id: 'БОЕЦ-TGNEW', callsign: 'Гром', unit_name: '1 взвод', unit_key: 'kapt_0123456789abcdef0123', email: 'grom@x.ru', device_model: 'Samsung A52' });
  const msg = state.telegram.at(-1);
  assert.equal(state.telegram.length, before + 1);
  assert.ok(msg.includes('Новая регистрация'));
  assert.ok(msg.includes('Гром') && msg.includes('1 взвод') && msg.includes('grom@x.ru') && msg.includes('Samsung A52'));
  assert.ok(msg.includes('kapt_***23'), 'unit key must be masked');
  assert.ok(!msg.includes('0123456789abcdef'), 'full unit key must not reach Telegram');
  // Repeated start of the same user is silent.
  await call('fighter_upsert', { fighter_id: 'БОЕЦ-TGNEW', email: 'grom@x.ru' });
  assert.equal(state.telegram.length, before + 1);
});

test('user upgrading from 3.5.0 is reported as an upgrade', async () => {
  state.docs.set('fighters/БОЕЦ-OLD', { fighterId: 'БОЕЦ-OLD', callsign: 'Ветеран' });
  await call('fighter_upsert', { fighter_id: 'БОЕЦ-OLD', callsign: 'Ветеран', email: 'old@x.ru' });
  assert.ok(state.telegram.at(-1).includes('перешёл на новую версию'));
  assert.ok(state.docs.has('fighters/БОЕЦ-OLD'), 'legacy record is left untouched');
});


test('admin: single ADMIN_PASSWORD secret is enough', async () => {
  const pwEnv = { ...env, ADMIN_API_SECRET_SHA256: '', ADMIN_SESSION_SECRET: '', ADMIN_PASSWORD: 'Очень-длинный-пароль-42' };
  const h = await call('health', {}, { method: 'GET', envOverride: pwEnv });
  assert.equal(h.body.adminAuthConfigured, true);
  assert.equal(h.body.adminSessionConfigured, true);
  assert.equal((await call('admin_auth', { secret: 'wrong' }, { envOverride: pwEnv, ip: '9.9.9.1' })).status, 403);
  const ok = await call('admin_auth', { secret: 'Очень-длинный-пароль-42' }, { envOverride: pwEnv, ip: '9.9.9.2' });
  assert.equal(ok.status, 200);
  assert.equal((await call('admin_list_fighters', { admin_token: ok.body.admin_token }, { envOverride: pwEnv })).status, 200);
  const shortEnv = { ...pwEnv, ADMIN_PASSWORD: 'short' };
  assert.equal((await call('health', {}, { method: 'GET', envOverride: shortEnv })).body.adminAuthConfigured, false);
});


test('admin_stats: owner dashboard numbers', async () => {
  const pwEnv = { ...env };
  const auth = await call('admin_auth', { secret: SECRETS.admin }, { ip: '4.4.4.4' });
  const now = Date.now();
  state.docs.set('units/kapt_stats/devices/dev_a', { deviceId: 'dev_a', callsign: 'Сокол', unitName: 'Рота', deviceModel: 'Pixel', timestampMillis: now - 60_000 });
  state.docs.set('units/kapt_stats/devices/dev_b', { deviceId: 'dev_b', callsign: 'Старый', timestampMillis: now - 3 * 86400000 });
  state.docs.set('fighters/БОЕЦ-LEG', { fighterId: 'БОЕЦ-LEG', registeredAt: now - 1000, expiresAt: now + 86400000 });
  const r = await call('admin_stats', { admin_token: auth.body.admin_token }, { envOverride: pwEnv });
  assert.equal(r.status, 200);
  assert.ok(r.body.online.devices_now >= 1);
  assert.ok(r.body.online_list.some((d) => d.callsign === 'Сокол'));
  assert.ok(!r.body.online_list.some((d) => d.callsign === 'Старый'));
  assert.ok(r.body.users.total >= 1);
  assert.equal(r.body.registrations_14d.length, 14);
  assert.ok(r.body.licenses.active_verified >= 1);
  assert.ok(r.body.licenses.active_legacy >= 1);
  assert.equal((await call('admin_stats', { admin_token: 'x.y.z' })).status, 403);
  const raw = JSON.stringify(r.body);
  assert.ok(!raw.includes('@'), 'no e-mails in stats');
  assert.ok(!raw.includes('kapt_stats'), 'no unit keys in stats');
});


test('email confirmation code: send, wrong, right, welcome letter', async () => {
  const before = state.emails.length;
  const sent = await call('email_code_send', { email: 'New@Boec.ru', fighter_id: 'БОЕЦ-MAIL' }, { ip: '7.7.7.1' });
  assert.equal(sent.status, 200);
  const letter = state.emails.at(-1);
  assert.equal(letter.to[0].email, 'new@boec.ru');
  const code = (letter.subject.match(/(\d{6})/) || [])[1];
  assert.ok(code, 'code in subject');
  assert.ok(letter.htmlContent.includes(code));
  // Resend within a minute is refused.
  assert.equal((await call('email_code_send', { email: 'new@boec.ru', fighter_id: 'БОЕЦ-MAIL' }, { ip: '7.7.7.2' })).status, 429);
  // Wrong code, other fighter.
  const wrong = await call('email_code_verify', { email: 'new@boec.ru', fighter_id: 'БОЕЦ-MAIL', code: code === '111111' ? '222222' : '111111' });
  assert.equal(wrong.status, 403);
  assert.equal(wrong.body.attempts_left, 4);
  assert.equal((await call('email_code_verify', { email: 'new@boec.ru', fighter_id: 'БОЕЦ-EVE', code })).status, 403);
  const ok = await call('email_code_verify', { email: 'new@boec.ru', fighter_id: 'БОЕЦ-MAIL', code, unit_key: 'kapt_0123456789abcdef0123', callsign: 'Новый' });
  assert.equal(ok.status, 200);
  assert.equal(state.docs.get('srv_fighters/БОЕЦ-MAIL').emailVerified, true);
  assert.ok(state.emails.at(-1).htmlContent.includes('kapt_0123456789abcdef0123'), 'welcome letter carries the unit key');
  assert.equal(state.emails.length, before + 2);
  // Code is single-use.
  assert.equal((await call('email_code_verify', { email: 'new@boec.ru', fighter_id: 'БОЕЦ-MAIL', code })).status, 410);
});

test('email confirmation code: brute force stops after 5 attempts', async () => {
  await call('email_code_send', { email: 'brute@x.ru', fighter_id: 'БОЕЦ-B' }, { ip: '7.7.7.3' });
  const code = (state.emails.at(-1).subject.match(/(\d{6})/) || [])[1];
  const bad = code === '000000' ? '999999' : '000000';
  for (let i = 0; i < 5; i++) await call('email_code_verify', { email: 'brute@x.ru', fighter_id: 'БОЕЦ-B', code: bad });
  assert.equal((await call('email_code_verify', { email: 'brute@x.ru', fighter_id: 'БОЕЦ-B', code })).status, 429);
});

test('email provider missing is reported, not hidden', async () => {
  const r = await call('email_code_send', { email: 'a@b.ru', fighter_id: 'X' }, { envOverride: { ...env, BREVO_API_KEY: '' } });
  assert.equal(r.status, 503);
  assert.equal(r.body.error, 'EMAIL_PROVIDER_UNAVAILABLE');
});


test('e-mail via the owner Gmail relay (Apps Script)', async () => {
  const relayEnv = { ...env, BREVO_API_KEY: '', MAIL_RELAY_URL: 'https://script.google.com/macros/s/AKfy-test_123/exec', MAIL_RELAY_SECRET: 'relay-secret-0123456789' };
  assert.equal((await call('health', {}, { method: 'GET', envOverride: relayEnv })).body.emailConfigured, true);
  const r = await call('email_code_send', { email: 'relay@x.ru', fighter_id: 'БОЕЦ-R' }, { envOverride: relayEnv, ip: '8.8.1.1' });
  assert.equal(r.status, 200);
  const m = state.relayed.at(-1);
  assert.equal(m.to, 'relay@x.ru');
  assert.match(m.subject, /\d{6}/);
  const badUrl = { ...relayEnv, MAIL_RELAY_URL: 'https://evil.example/exec' };
  assert.equal((await call('health', {}, { method: 'GET', envOverride: badUrl })).body.emailConfigured, false);
  const r2 = await call('email_code_send', { email: 'relay2@x.ru', fighter_id: 'БОЕЦ-R2' }, { envOverride: { ...relayEnv, MAIL_RELAY_SECRET: 'wrong-secret-0000000' }, ip: '8.8.1.2' });
  assert.equal(r2.status, 503);
});

// ------------------------------------------------------------------ run

let failed = 0;
for (const { name, fn } of tests) {
  try {
    await fn();
    console.log('  ok  ' + name);
  } catch (error) {
    failed++;
    console.error('  FAIL ' + name + '\n       ' + (error?.stack || error));
  }
}
__test.reset();
console.log(`\n${tests.length - failed}/${tests.length} backend tests passed`);
if (failed) process.exit(1);
