// Firestore rules tests (run inside the emulator):
//   npx firebase emulators:exec --only firestore --config server/firestore/firebase.json \
//     --project demo-kapterka "node server/firestore/rules.test.mjs"
import fs from 'node:fs';
import assert from 'node:assert/strict';
import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { collection, doc, getDoc, getDocs, setDoc, deleteDoc, query, where } from 'firebase/firestore';

const dir = new URL('.', import.meta.url).pathname;
let passed = 0;

async function withRules(file, fn) {
  const env = await initializeTestEnvironment({
    projectId: 'demo-kapterka-' + file.replace(/\W/g, ''),
    firestore: { rules: fs.readFileSync(dir + file, 'utf8') }
  });
  try {
    await env.withSecurityRulesDisabled(async (ctx) => {
      const db = ctx.firestore();
      await setDoc(doc(db, 'units/kapt_unit'), { unitKey: 'kapt_unit', createdBy: 'owner-uid', createdAt: 1 });
      await setDoc(doc(db, 'units/kapt_unit/members/member-uid'), { uid: 'member-uid' });
      await setDoc(doc(db, 'units/kapt_unit/stock_records/base___item'), { quantity: 5 });
      await setDoc(doc(db, 'fighters/F1'), { email: 'a@b.c', unitKey: 'kapt_unit' });
      await setDoc(doc(db, 'licenses/KAPT-AAAA-BBBB-CCCC'), { email: 'a@b.c', status: 'ACTIVE' });
      await setDoc(doc(db, 'srv_licenses/KAPT-AAAA-BBBB-CCCC'), { status: 'ACTIVE' });
    });
    await fn({
      anon: env.unauthenticatedContext().firestore(),
      stranger: env.authenticatedContext('stranger-uid').firestore(),
      member: env.authenticatedContext('member-uid').firestore()
    });
  } finally {
    await env.cleanup();
  }
}

async function check(name, fn) {
  await fn();
  passed++;
  console.log('  ok  ' + name);
}

await withRules('transition.rules', async ({ anon, stranger, member }) => {
  await check('A: server registries are closed', async () => {
    for (const db of [anon, stranger, member]) {
      await assertFails(getDoc(doc(db, 'srv_licenses/KAPT-AAAA-BBBB-CCCC')));
      await assertFails(setDoc(doc(db, 'srv_fighters/X'), { expiresAt: 1 }));
    }
  });
  await check('A: fighter registry can no longer be read or dumped', async () => {
    await assertFails(getDocs(collection(anon, 'fighters')));
    await assertFails(getDoc(doc(anon, 'fighters/F1')));
    await assertSucceeds(setDoc(doc(anon, 'fighters/F2'), { callsign: 'legacy registration' }, { merge: true }));
  });
  await check('A: license list is closed, single legacy lookup works', async () => {
    await assertFails(getDocs(collection(anon, 'licenses')));
    await assertFails(getDocs(query(collection(anon, 'licenses'), where('email', '==', 'a@b.c'))));
    await assertSucceeds(getDoc(doc(anon, 'licenses/KAPT-AAAA-BBBB-CCCC')));
  });
  await check('A: legacy unit sync keeps working', async () => {
    await assertSucceeds(getDoc(doc(anon, 'units/kapt_unit/stock_records/base___item')));
    await assertSucceeds(setDoc(doc(anon, 'units/kapt_unit/stock_records/x___y'), { quantity: 1 }));
    await assertFails(getDocs(collection(anon, 'units')));
  });
  await check('A: members are written only by the server', async () => {
    await assertFails(setDoc(doc(stranger, 'units/kapt_unit/members/stranger-uid'), { uid: 'stranger-uid' }));
    await assertFails(getDocs(collection(stranger, 'units/kapt_unit/members')));
    await assertSucceeds(getDocs(collection(member, 'units/kapt_unit/members')));
  });
  await check('A: unknown collections are closed', async () => {
    await assertFails(setDoc(doc(anon, 'anything/x'), { a: 1 }));
  });
});

await withRules('strict.rules', async ({ anon, stranger, member }) => {
  await check('B: non-members cannot read or write unit data', async () => {
    for (const db of [anon, stranger]) {
      await assertFails(getDoc(doc(db, 'units/kapt_unit/stock_records/base___item')));
      await assertFails(getDocs(collection(db, 'units/kapt_unit/stock_records')));
      await assertFails(setDoc(doc(db, 'units/kapt_unit/stock_records/x___y'), { quantity: 999 }));
      await assertFails(setDoc(doc(db, 'units/kapt_unit/members/stranger-uid'), { uid: 'x' }));
    }
  });
  await check('B: members sync normally', async () => {
    await assertSucceeds(getDocs(collection(member, 'units/kapt_unit/stock_records')));
    await assertSucceeds(setDoc(doc(member, 'units/kapt_unit/stock_records/x___y'), { quantity: 1 }));
    await assertSucceeds(deleteDoc(doc(member, 'units/kapt_unit/stock_records/x___y')));
    await assertSucceeds(setDoc(doc(member, 'units/kapt_unit/devices/dev_1'), { lastSeen: 1 }));
    await assertSucceeds(getDoc(doc(member, 'units/kapt_unit')));
    await assertSucceeds(setDoc(doc(member, 'units/kapt_unit'), { unitName: 'Взвод', lastActivity: 2 }, { merge: true }));
    await assertFails(setDoc(doc(member, 'units/kapt_unit'), { createdBy: 'member-uid' }, { merge: true }));
    await assertFails(setDoc(doc(stranger, 'units/kapt_unit'), { unitName: 'x' }, { merge: true }));
  });
  await check('B: membership of one unit does not open another', async () => {
    await assertFails(getDoc(doc(member, 'units/kapt_other/stock_records/a')));
  });
  await check('B: legacy registries fully closed', async () => {
    await assertFails(getDoc(doc(anon, 'licenses/KAPT-AAAA-BBBB-CCCC')));
    await assertFails(setDoc(doc(anon, 'fighters/F3'), { a: 1 }));
    await assertFails(setDoc(doc(member, 'licenses/KAPT-ZZZZ-ZZZZ-ZZZZ'), { status: 'ACTIVE' }));
  });
});

assert.ok(passed > 0);
console.log(`\n${passed} rules checks passed`);
