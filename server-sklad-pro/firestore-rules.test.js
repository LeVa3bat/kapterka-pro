const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');

const rules = fs.readFileSync(require('node:path').join(__dirname, 'firestore.rules'), 'utf8');

function blockAfter(marker, length = 500) {
  const start = rules.indexOf(marker);
  assert.notEqual(start, -1, 'Missing rules block: ' + marker);
  return rules.slice(start, start + length);
}

test('billing and entitlement records stay server-only', () => {
  const entitlements = blockAfter('match /entitlements/{uid}');
  assert.match(entitlements, /allow write:\s*if false/);

  const payments = blockAfter('match /payments/{paymentId}');
  assert.match(payments, /allow write:\s*if false/);
});

test('workspace roles cannot be changed directly by Android clients', () => {
  const members = blockAfter('match /members/{uid}');
  assert.match(members, /allow write:\s*if false/);
});

test('accounting operations are append-only', () => {
  const operations = blockAfter('match /operations/{operationId}');
  assert.match(operations, /allow create:/);
  assert.match(operations, /allow update, delete:\s*if false/);
});

test('firestore rules keep a default-deny fallback', () => {
  const fallback = blockAfter('match /{document=**}', 180);
  assert.match(fallback, /allow read, write:\s*if false/);
});

test('all client access requires a verified email identity', () => {
  const verified = blockAfter('function verifiedUser()', 240);
  assert.match(verified, /request\.auth\s*!=\s*null/);
  assert.match(verified, /request\.auth\.token\.email_verified\s*==\s*true/);

  assert.doesNotMatch(rules, /function signedIn\(\)/);
});
