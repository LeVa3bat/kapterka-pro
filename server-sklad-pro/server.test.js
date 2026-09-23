const test = require('node:test');
const assert = require('node:assert/strict');

const {
  effectiveEntitlement,
  normalizeMoney,
  requireVerifiedEmail,
  VALID_PROFILE_IDS
} = require('./server');

test('active paid subscription wins over trial state', () => {
  const now = 1_700_000_000_000;
  const entitlement = effectiveEntitlement({
    status: 'pro',
    planId: 'pro_month',
    demoEndsAt: now - 1,
    paidUntil: now + 86_400_000
  }, now);

  assert.equal(entitlement.status, 'pro');
  assert.equal(entitlement.isProActive, true);
  assert.equal(entitlement.isTrialActive, false);
  assert.equal(entitlement.serverTime, now);
});

test('expired paid subscription falls back to still-active trial', () => {
  const now = 1_700_000_000_000;
  const entitlement = effectiveEntitlement({
    status: 'pro',
    demoStartedAt: now - 10_000,
    demoEndsAt: now + 86_400_000,
    paidUntil: now - 1
  }, now);

  assert.equal(entitlement.status, 'trial');
  assert.equal(entitlement.isProActive, false);
  assert.equal(entitlement.isTrialActive, true);
});

test('expired entitlement cannot stay pro because of stale stored status', () => {
  const now = 1_700_000_000_000;
  const entitlement = effectiveEntitlement({
    status: 'pro',
    demoEndsAt: now - 100,
    paidUntil: now - 1
  }, now);

  assert.equal(entitlement.status, 'expired');
  assert.equal(entitlement.storedStatus, 'pro');
  assert.equal(entitlement.isProActive, false);
  assert.equal(entitlement.isTrialActive, false);
});

test('money normalization rejects invalid prices and fixes precision', () => {
  assert.equal(normalizeMoney('490'), '490.00');
  assert.equal(normalizeMoney('490.5'), '490.50');
  assert.equal(normalizeMoney('0'), '');
  assert.equal(normalizeMoney('not-a-number'), '');
});

test('protected backend requires a verified email token', () => {
  assert.throws(
    () => requireVerifiedEmail({ uid: 'u1', email: 'user@example.com', email_verified: false }),
    (error) => error.message === 'EMAIL_VERIFICATION_REQUIRED' && error.statusCode === 403
  );

  const decoded = { uid: 'u1', email: 'user@example.com', email_verified: true };
  assert.equal(requireVerifiedEmail(decoded), decoded);
});

test('workspace profile ids stay aligned with Android presets', () => {
  assert.equal(VALID_PROFILE_IDS.size, 14);
  for (const id of ['universal', 'retail', 'auto', 'medical', 'office_it', 'military']) {
    assert.equal(VALID_PROFILE_IDS.has(id), true);
  }
  assert.equal(VALID_PROFILE_IDS.has('unknown-profile'), false);
});
