import fs from 'node:fs';
import {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds
} from '@firebase/rules-unit-testing';
import { collection, doc, getDoc, getDocs, setDoc, deleteDoc } from 'firebase/firestore';

const projectId = 'kapterka-next-safe-rules-test';
const rules = fs.readFileSync('server/firestore.next-safe.rules', 'utf8');

const env = await initializeTestEnvironment({
  projectId,
  firestore: { rules }
});

try {
  const anon = env.unauthenticatedContext().firestore();
  const auth = env.authenticatedContext('ordinary-user').firestore();

  // Privileged global collections must never be reachable from Android/Web SDK clients.
  for (const db of [anon, auth]) {
    await assertFails(getDoc(doc(db, 'licenses', 'KAPT-TEST-TEST-TEST')));
    await assertFails(setDoc(doc(db, 'licenses', 'KAPT-TEST-TEST-TEST'), { status: 'ACTIVE' }));
    await assertFails(getDoc(doc(db, 'fighters', 'fighter-1')));
    await assertFails(setDoc(doc(db, 'fighters', 'fighter-1'), { isProActive: true }));
  }

  // Current compatibility stage intentionally keeps direct unit sync open.
  // Root unit enumeration is denied so high-entropy unit keys are not discoverable
  // through a simple unauthenticated list query.
  const unitRoot = doc(anon, 'units', 'unit-test');
  await assertSucceeds(setDoc(unitRoot, { unitKey: 'unit-test' }));
  await assertSucceeds(getDoc(unitRoot));
  await assertFails(getDocs(collection(anon, 'units')));

  const unitStock = doc(anon, 'units', 'unit-test', 'stock_records', 'base___item');
  await assertSucceeds(setDoc(unitStock, { pointId: 'base', itemId: 'item', quantity: 1 }));
  await assertSucceeds(getDoc(unitStock));
  await assertSucceeds(deleteDoc(unitStock));

  // Anything outside the explicit compatibility surface is denied.
  await assertFails(getDoc(doc(anon, 'unknown', 'doc')));
  await assertFails(setDoc(doc(anon, 'unknown', 'doc'), { value: 1 }));

  console.log('Firestore NEXT-SAFE rules self-test PASSED.');
} finally {
  await env.cleanup();
}
