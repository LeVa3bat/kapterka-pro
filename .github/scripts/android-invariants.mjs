import fs from 'node:fs';

const fail = (msg) => { console.error('FAIL:', msg); process.exitCode = 1; };
const ok = (msg) => console.log('OK:', msg);
const warn = (msg) => console.warn('WARN:', msg);
const read = (p) => fs.readFileSync(p, 'utf8');

const gradle = read('app/build.gradle.kts');
const db = read('app/src/main/java/com/example/data/local/KapterkaDatabase.kt');
const manifest = read('app/src/main/AndroidManifest.xml');

const appId = (gradle.match(/applicationId\s*=\s*"([^"]+)"/) || [])[1];
if (appId !== 'com.aistudio.kapterka.jmwqve') fail(`applicationId changed: ${appId || 'missing'}`);
else ok('applicationId preserved');

const versionCode = Number((gradle.match(/versionCode\s*=\s*(\d+)/) || [])[1] || 0);
if (versionCode < 31) fail(`versionCode must never go backwards (found ${versionCode})`);
else ok(`versionCode is monotonic-safe: ${versionCode}`);

const versionName = (gradle.match(/versionName\s*=\s*"([^"]+)"/) || [])[1] || '';
if (!/^\d+\.\d+\.\d+(?:[-+][A-Za-z0-9.-]+)?$/.test(versionName)) fail(`invalid versionName: ${versionName || 'missing'}`);
else ok(`versionName format is valid: ${versionName}`);

const minSdk = Number((gradle.match(/minSdk\s*=\s*(\d+)/) || [])[1] || 0);
if (!minSdk) fail('minSdk is missing');
else if (minSdk > 24) fail(`minSdk increased to ${minSdk}; this would drop currently supported Android 7 devices`);
else ok(`minSdk compatibility preserved: ${minSdk}`);

const targetSdk = Number((gradle.match(/targetSdk\s*=\s*(\d+)/) || [])[1] || 0);
if (targetSdk < 34) fail(`targetSdk unexpectedly decreased: ${targetSdk}`);
else ok(`targetSdk is not below release baseline: ${targetSdk}`);

if (!/release\s*\{[\s\S]*?signingConfig\s*=\s*signingConfigs\.getByName\("debugConfig"\)/.test(gradle)) {
  fail('release signing configuration changed; published signature continuity must be reviewed before release');
} else {
  ok('release signing configuration still matches published-signature baseline');
}

const roomVersion = Number((db.match(/version\s*=\s*(\d+)/) || [])[1] || 0);
if (roomVersion < 2) fail(`Room database version regressed: ${roomVersion}`);
else ok(`Room database version: ${roomVersion}`);

if (!db.includes('"kapterka_database"')) fail('Room database filename changed');
else ok('Room database filename preserved');

if (/fallbackToDestructiveMigration|deleteDatabase\s*\(/.test(db)) {
  fail('destructive database fallback/delete detected');
} else {
  ok('no destructive database fallback detected');
}

for (let from = 1; from < roomVersion; from++) {
  const to = from + 1;
  const migrationPattern = new RegExp(`Migration\\s*\\(\\s*${from}\\s*,\\s*${to}\\s*\\)`);
  if (!migrationPattern.test(db)) fail(`missing explicit Room migration ${from} -> ${to}`);
}
if (!process.exitCode) ok('Room migration chain is explicit');

if (!/android:allowBackup="true"/.test(manifest)) warn('android:allowBackup is no longer true; verify backup/restore impact deliberately');

const paymentSource = read('app/src/main/java/com/example/data/payment/YooKassaPaymentService.kt');
if (/DEFAULT_LIVE_KEY|live_[A-Za-z0-9_-]{20,}/.test(paymentSource)) {
  fail('YooKassa secret material must never be present in Android source');
} else {
  ok('YooKassa secret material is absent from Android source');
}
if (paymentSource.includes('api.yookassa.ru') || paymentSource.includes('Authorization", "Basic')) {
  fail('Android must not call authenticated YooKassa API directly');
} else if (!paymentSource.includes('BuildConfig.PAYMENT_API_URL')) {
  fail('Android payment client is not routed through PAYMENT_API_URL');
} else {
  ok('Android payment client uses server-only payment API');
}

const licenseSource = read('app/src/main/java/com/example/data/license/LicenseManager.kt');
const legacyPaymentIssuer = licenseSource.slice(
  licenseSource.indexOf('suspend fun activateLicenseAfterPayment'),
  licenseSource.indexOf('private suspend fun updateRoomProfilePro')
);
if (legacyPaymentIssuer.includes('generateLicenseKey()') || legacyPaymentIssuer.includes('30L * 24L')) {
  fail('legacy post-payment path can still mint a client-side license');
} else if (!licenseSource.includes('activateServerVerifiedLicense')) {
  fail('server-verified license activation path is missing');
} else {
  ok('new paid licenses are persisted only from server-issued data');
}

const manualActivation = licenseSource.slice(
  licenseSource.indexOf('suspend fun activateKeyManually'),
  licenseSource.lastIndexOf('\n}')
);
if (manualActivation.includes('Ключ подтвержден цифровой подписью в оффлайн-режиме') ||
    manualActivation.includes('Активация проверенного военного ключа')) {
  fail('manual activation still contains offline license minting fallback');
} else {
  ok('manual activation does not mint a fresh offline license');
}

const daoSource = read('app/src/main/java/com/example/data/local/KapterkaDao.kt');
const repositorySource = read('app/src/main/java/com/example/data/repository/KapterkaRepository.kt');
if (!daoSource.includes('@Transaction') || !daoSource.includes('commitOperationAndStocks')) {
  fail('atomic Room operation+stock transaction is missing');
} else if ((repositorySource.match(/dao\.commitOperationAndStocks\(/g) || []).length < 4) {
  fail('not all core stock-changing operations use atomic Room transaction');
} else {
  ok('core operation history and stock mutations are committed atomically');
}

const syncSource = read('app/src/main/java/com/example/data/sync/FirebaseSyncManager.kt');
const reconcileStart = syncSource.indexOf('suspend fun syncAndReconcileAll');
const reconcileEnd = syncSource.indexOf('fun pushOperationAsync', reconcileStart);
const reconcileSource = syncSource.slice(reconcileStart, reconcileEnd);
if (reconcileSource.includes('dao.clearAllStockRecords()') ||
    /if \(!cloudOpIds\.contains\([^)]+\)\)\s*\{\s*dao\.deleteOperation/.test(reconcileSource) ||
    /if \(!cloudReqIds\.contains\([^)]+\)\)\s*\{\s*dao\.deleteRequisition/.test(reconcileSource)) {
  fail('sync reconcile still contains delete-by-absence behavior');
} else {
  ok('sync reconcile preserves local data when cloud data is absent');
}

const tombstoneModel = read('app/src/main/java/com/example/data/model/SyncTombstone.kt');
if (roomVersion < 3) {
  fail('Room v3 is required for persistent sync tombstones');
} else if (!db.includes('SyncTombstone::class') || !db.includes('MIGRATION_2_3')) {
  fail('Room v2 -> v3 tombstone migration is missing');
} else if (!tombstoneModel.includes('tableName = "sync_tombstones"')) {
  fail('sync_tombstones entity is missing');
} else {
  ok('Room v3 adds persistent sync tombstones with explicit migration');
}

if (!syncSource.includes('collection("sync_tombstones")') ||
    !syncSource.includes('prepareDeletionTombstone') ||
    !syncSource.includes('applyTombstone')) {
  fail('explicit tombstone sync protocol is incomplete');
} else {
  ok('explicit tombstone sync protocol is present');
}

const destructiveRemovedPatterns = [
  /DocumentChange\.Type\.REMOVED[\s\S]{0,180}dao\.deletePoint/,
  /DocumentChange\.Type\.REMOVED[\s\S]{0,180}dao\.deleteItem/,
  /DocumentChange\.Type\.REMOVED[\s\S]{0,180}dao\.deleteOperation/,
  /DocumentChange\.Type\.REMOVED[\s\S]{0,180}dao\.deleteRequisition/
];
if (destructiveRemovedPatterns.some((pattern) => pattern.test(syncSource))) {
  fail('raw Firestore REMOVED event can still delete local user data without tombstone');
} else {
  ok('raw Firestore REMOVED events are non-destructive');
}

const backupRules = read('app/src/main/res/xml/backup_rules.xml');
const extractionRules = read('app/src/main/res/xml/data_extraction_rules.xml');
if (!backupRules.includes('kapterka_sync_prefs.xml') ||
    !extractionRules.includes('kapterka_sync_prefs.xml')) {
  fail('device UUID backup exclusion is missing');
} else {
  ok('device UUID is excluded from cloud/device restore');
}

const emailSource = read('app/src/main/java/com/example/data/notification/EmailDeliveryService.kt');
if (emailSource.includes('api.brevo.com') ||
    emailSource.includes('api.resend.com') ||
    emailSource.includes('"brevo_api_key"') && emailSource.includes('.putString("brevo_api_key"') ||
    emailSource.includes('.putString("smtp_pass"')) {
  fail('email provider credentials/API must not be handled directly by Android');
} else if (!emailSource.includes('BuildConfig.PAYMENT_API_URL') ||
           !emailSource.includes('send_license_email')) {
  fail('license email delivery is not routed through the backend');
} else {
  ok('license email delivery is server-authoritative');
}

const sourceFiles = [];
const walk = (dir) => {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = `${dir}/${entry.name}`;
    if (entry.isDirectory()) walk(full);
    else sourceFiles.push(full);
  }
};
walk('app/src/main');
const historicalUnitKeyHits = sourceFiles.filter((file) => {
  try { return read(file).includes('kapt_59e13b'); } catch (_) { return false; }
});
if (historicalUnitKeyHits.length) {
  fail(`historical shared unit key must not be used as a fallback: ${historicalUnitKeyHits.join(', ')}`);
} else {
  ok('historical shared unit key is absent from production source');
}

const outdatedReferenceHits = sourceFiles.filter((file) => {
  try {
    const text = read(file);
    return text.includes('https://kapterka-pro.ru/#cabinet') || text.includes('@Levaminbat');
  } catch (_) {
    return false;
  }
});
if (outdatedReferenceHits.length) {
  fail(`outdated cabinet/support references remain: ${outdatedReferenceHits.join(', ')}`);
} else {
  ok('cabinet/support references use current destinations');
}

const directTelegramCredentialHits = sourceFiles.filter((file) => {
  try {
    const text = read(file);
    return /\b\d{8,12}:[A-Za-z0-9_-]{25,}\b/.test(text) ||
      text.includes('api.telegram.org/bot') ||
      text.includes('TOKEN_PARTS');
  } catch (_) {
    return false;
  }
});
if (directTelegramCredentialHits.length) {
  fail(`direct Telegram bot credentials/API use detected in Android source: ${directTelegramCredentialHits.join(', ')}`);
} else {
  ok('Android notifications do not embed Telegram bot credentials');
}

const genericUnitFallbackHits = sourceFiles.filter((file) => {
  try {
    const text = read(file);
    return text.includes('kapt_default');
  } catch (_) {
    return false;
  }
});
if (genericUnitFallbackHits.length) {
  fail(`generic shared unit-key fallbacks detected: ${genericUnitFallbackHits.join(', ')}`);
} else {
  ok('generic shared unit-key fallbacks are absent');
}

const sensitiveTracked = [
  'app/google-services.json',
  'google-services.json',
  'my-upload-key.jks',
  'app/my-upload-key.jks',
  'debug.keystore',
  'app/debug.keystore',
  '.env',
  'app/.env'
].filter((p) => fs.existsSync(p));
if (sensitiveTracked.length) fail(`sensitive local files are tracked/present in checkout: ${sensitiveTracked.join(', ')}`);
else ok('no common keystore/env/google-services files are tracked');

if (process.exitCode) {
  console.error('\nAndroid invariants FAILED.');
  process.exit(process.exitCode);
}
console.log('\nAndroid invariants PASSED.');
