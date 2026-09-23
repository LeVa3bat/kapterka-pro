const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const rules = fs.readFileSync(path.join(__dirname, 'firestore.rules'), 'utf8');

function blockAfter(marker, length = 700) {
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

test('personal workspace bootstrap is restricted to verified owner uid', () => {
  const helper = blockAfter('function isPersonalOwner(workspaceId)', 240);
  assert.match(helper, /workspaceId\s*==\s*request\.auth\.uid/);

  const workspace = blockAfter('match /workspaces/{workspaceId}', 700);
  assert.match(workspace, /allow create:\s*if isPersonalOwner\(workspaceId\)/);
  assert.match(workspace, /request\.resource\.data\.ownerUid\s*==\s*request\.auth\.uid/);
  assert.match(workspace, /allow delete:\s*if false/);
});

test('workspace roles cannot be promoted or deleted by Android clients', () => {
  const members = blockAfter('match /members/{uid}', 650);
  assert.match(members, /allow create:\s*if isPersonalOwner\(workspaceId\)/);
  assert.match(members, /uid\s*==\s*request\.auth\.uid/);
  assert.match(members, /request\.resource\.data\.role\s*==\s*'owner'/);
  assert.match(members, /allow update, delete:\s*if false/);
});

test('warehouse data has explicit no-delete rules', () => {
  for (const marker of [
    'match /warehouses/{warehouseId}',
    'match /items/{itemId}',
    'match /stocks/{stockId}',
    'match /tombstones/{tombstoneId}'
  ]) {
    const block = blockAfter(marker, 350);
    assert.match(block, /allow delete:\s*if false/);
  }
});

test('accounting operations are append-only', () => {
  const operations = blockAfter('match /operations/{operationId}', 500);
  assert.match(operations, /allow create:/);
  assert.match(operations, /request\.resource\.data\.createdBy\s*==\s*request\.auth\.uid/);
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
