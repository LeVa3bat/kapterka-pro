const crypto = require('crypto');
const assert = require('assert');

process.env.YOOKASSA_SHOP_ID = 'test-shop';
process.env.YOOKASSA_SECRET_KEY = 'unit-test-yookassa-secret';
process.env.FIREBASE_SERVICE_ACCOUNT_JSON = JSON.stringify({
  client_email: 'service-account@example.invalid',
  private_key: '-----BEGIN PRIVATE KEY-----\nunit-test-only\n-----END PRIVATE KEY-----'
});
process.env.ADMIN_API_SECRET_SHA256 = crypto
  .createHash('sha256')
  .update('correct-admin-secret')
  .digest('hex');
process.env.ADMIN_SESSION_SECRET = 'unit-test-admin-session-secret';
process.env.BREVO_API_KEY = 'unit-test-brevo-key';
process.env.EMAIL_SENDER_EMAIL = 'sender@example.invalid';

const { handler } = require('./yandex-cloud-function.js');

async function call(action, body = {}, method = 'POST', sourceIp = '127.0.0.1') {
  const response = await handler({
    httpMethod: method,
    queryStringParameters: method === 'GET' ? { action } : {},
    body: method === 'GET' ? '' : JSON.stringify({ action, ...body }),
    isBase64Encoded: false,
    requestContext: { identity: { sourceIp } }
  });
  let parsed = {};
  try { parsed = JSON.parse(response.body || '{}'); } catch (_) {}
  return { ...response, parsed };
}

(async () => {
  const health = await call('health', {}, 'GET');
  assert.strictEqual(health.statusCode, 200);
  assert.strictEqual(health.parsed.ok, true);
  assert.strictEqual(health.parsed.service, 'kapterka-payment-api');
  for (const field of [
    'secretConfigured',
    'licenseRegistryConfigured',
    'adminAuthConfigured',
    'adminSessionConfigured',
    'emailConfigured'
  ]) {
    assert.strictEqual(health.parsed[field], true, 'health flag must be true: ' + field);
  }
  const healthRaw = JSON.stringify(health.parsed);
  for (const secret of [
    process.env.YOOKASSA_SECRET_KEY,
    process.env.ADMIN_SESSION_SECRET,
    process.env.BREVO_API_KEY,
    'unit-test-only'
  ]) {
    assert.ok(!healthRaw.includes(secret), 'health must not leak secret value');
  }

  const invalidAdmin = await call('admin_list_fighters', {
    admin_token: 'invalid.token'
  });
  assert.strictEqual(invalidAdmin.statusCode, 403);
  assert.strictEqual(invalidAdmin.parsed.error, 'ADMIN_SESSION_INVALID');

  // admin rate limit: five failures are rejected normally; the sixth is throttled.
  for (let i = 0; i < 5; i++) {
    const bad = await call('admin_auth', { secret: 'wrong-secret-' + i }, 'POST', '198.51.100.10');
    assert.strictEqual(bad.statusCode, 403);
    assert.strictEqual(bad.parsed.error, 'ADMIN_AUTH_FAILED');
  }
  const throttled = await call('admin_auth', { secret: 'still-wrong' }, 'POST', '198.51.100.10');
  assert.strictEqual(throttled.statusCode, 429);
  assert.strictEqual(throttled.parsed.error, 'ADMIN_AUTH_RATE_LIMITED');

  // A different source with the correct secret must still be able to authenticate.
  const validAdmin = await call('admin_auth', { secret: 'correct-admin-secret' }, 'POST', '198.51.100.11');
  assert.strictEqual(validAdmin.statusCode, 200);
  assert.strictEqual(validAdmin.parsed.ok, true);
  assert.ok(validAdmin.parsed.admin_token);

  const invalidPayment = await call('create', {
    email: 'not-an-email',
    callsign: 'Test'
  });
  assert.strictEqual(invalidPayment.statusCode, 400);
  assert.strictEqual(invalidPayment.parsed.error, 'INVALID_EMAIL');

  const invalidLicense = await call('license_verify', {
    license_key: 'INVALID',
    fighter_id: 'fighter-test'
  });
  assert.strictEqual(invalidLicense.statusCode, 400);
  assert.strictEqual(invalidLicense.parsed.error, 'INVALID_LICENSE_KEY');

  const missingFighter = await call('fighter_lookup', {});
  assert.strictEqual(missingFighter.statusCode, 400);
  assert.strictEqual(missingFighter.parsed.error, 'MISSING_FIGHTER_ID');

  const options = await handler({ httpMethod: 'OPTIONS' });
  assert.strictEqual(options.statusCode, 200);

  console.log('Backend self-test PASSED.');
})().catch((error) => {
  console.error(error);
  process.exit(1);
});
