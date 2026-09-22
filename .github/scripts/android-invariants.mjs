import fs from 'node:fs';

const fail = (msg) => { console.error('FAIL:', msg); process.exitCode = 1; };
const ok = (msg) => console.log('OK:', msg);
const warn = (msg) => console.warn('WARN:', msg);
const read = (p) => fs.readFileSync(p, 'utf8');

const gradle = read('app/build.gradle.kts');
const db = read('app/src/main/java/com/example/data/local/KapterkaDatabase.kt');
const manifest = read('app/src/main/AndroidManifest.xml');
const applicationSource = read('app/src/main/java/com/example/KapterkaApplication.kt');
const mainActivitySource = read('app/src/main/java/com/example/MainActivity.kt');

const appId = (gradle.match(/applicationId\s*=\s*"([^"]+)"/) || [])[1];
if (appId !== 'com.aistudio.kapterka.jmwqve') fail(`applicationId changed: ${appId || 'missing'}`);
else ok('applicationId preserved');

const directVersionCode = (gradle.match(/versionCode\s*=\s*(\d+)/) || [])[1] || '';
const fallbackVersionCode = (gradle.match(/versionCode\s*=\s*System\.getenv\([^\n]+?\?\:\s*(\d+)/) || [])[1] || '';
const versionCode = Number(directVersionCode || fallbackVersionCode || 0);
if (versionCode < 31) fail(`versionCode must never go backwards (found ${versionCode})`);
else ok(`versionCode is monotonic-safe: ${versionCode}`);

const directVersionName = (gradle.match(/versionName\s*=\s*"([^"]+)"/) || [])[1] || '';
const fallbackVersionName = (gradle.match(/versionName\s*=\s*System\.getenv\([^\n]+?\?\:\s*"([^"]+)"/) || [])[1] || '';
const versionName = directVersionName || fallbackVersionName;
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

if (!applicationSource.includes('!BuildConfig.IS_NEXT_SAFE_TEST') ||
    !mainActivitySource.includes('!BuildConfig.IS_NEXT_SAFE_TEST')) {
  fail('NEXT-SAFE test build can initialize production Firebase');
} else {
  ok('NEXT-SAFE test build is isolated from production Firebase initialization');
}

if (/implementation\(libs\.firebase\.appcheck\.debug\)/.test(gradle) &&
    !/debugImplementation\(libs\.firebase\.appcheck\.debug\)/.test(gradle)) {
  fail('Firebase App Check debug provider is packaged in release implementation');
} else {
  ok('Firebase App Check debug provider is excluded from release implementation');
}

if (!/android:usesCleartextTraffic="false"/.test(manifest) ||
    !manifest.includes('android:networkSecurityConfig="@xml/network_security_config"')) {
  fail('Android network security must explicitly disable cleartext HTTP');
} else {
  const networkSecurity = read('app/src/main/res/xml/network_security_config.xml');
  if (!networkSecurity.includes('cleartextTrafficPermitted="false"')) {
    fail('network_security_config does not deny cleartext traffic');
  } else {
    ok('cleartext HTTP is disabled for Android');
  }
}

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

if (licenseSource.includes('FirebaseFirestore') ||
    licenseSource.includes('collection("licenses")') ||
    !licenseSource.includes('LicenseBackendService')) {
  fail('LicenseManager must not read Firestore licenses directly');
} else {
  ok('LicenseManager verifies/restores licenses through backend');
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

const backendSource = read('server/yandex-cloud-function.js');
const restoreStart = backendSource.indexOf("if (action === 'license_restore')");
const restoreEnd = backendSource.indexOf("if (action === 'send_license_email')", restoreStart);
const restoreSource = backendSource.slice(restoreStart, restoreEnd);
if (!restoreSource.includes("MISSING_FIGHTER_ID") ||
    !restoreSource.includes("LICENSE_RESTORE_IDENTITY_MISMATCH") ||
    !restoreSource.includes("license.fighterId !== fighterId")) {
  fail('license restore is not bound to the existing fighter identity');
} else {
  ok('license restore is identity-bound and does not disclose keys by email alone');
}

const viewModelSource = read('app/src/main/java/com/example/ui/viewmodel/KapterkaViewModel.kt');
const legacyActivateStart = viewModelSource.indexOf('fun activateProSubscription()');
const legacyActivateEnd = viewModelSource.indexOf('\n    fun ', legacyActivateStart + 5);
const legacyActivateSource = viewModelSource.slice(
  legacyActivateStart,
  legacyActivateEnd > legacyActivateStart ? legacyActivateEnd : legacyActivateStart + 1200
);
if (legacyActivateSource.includes('copy(isProActive = true') ||
    legacyActivateSource.includes('proDaysLeft = 30')) {
  fail('legacy local PRO activation path can still grant entitlement');
} else {
  ok('local test PRO activation path is blocked');
}

const repositorySafetySource = read('app/src/main/java/com/example/data/repository/KapterkaRepository.kt');
const clearStart = repositorySafetySource.indexOf('suspend fun clearAllData()');
const clearEnd = repositorySafetySource.indexOf('suspend fun clearLocalUnitData()', clearStart);
const clearSource = repositorySafetySource.slice(clearStart, clearEnd);
if (!clearSource.includes('"stock_record"') ||
    !clearSource.includes('"operation"') ||
    !clearSource.includes('"requisition"') ||
    !clearSource.includes('prepareDeletionTombstone')) {
  fail('full unit reset does not create explicit tombstones before deletion');
} else {
  ok('full unit reset creates explicit tombstones before deletion');
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

if (!syncSource.includes('shouldAcceptStockRecord') ||
    !syncSource.includes('isSupersededBy(stock.lastUpdated)') ||
    !syncSource.includes('deleteSyncTombstoneById') ||
    !syncSource.includes('publishTombstoneIfNewer') ||
    !syncSource.includes('cloudDeletedAt')) {
  fail('tombstone conflict resolution can regress or block legitimate stock recreation');
} else {
  ok('tombstone conflict ordering is monotonic and permits newer stock recreation');
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

const fighterRegistrySource = read('app/src/main/java/com/example/data/admin/FighterRegistryManager.kt');
if (fighterRegistrySource.includes('FirebaseFirestore') ||
    fighterRegistrySource.includes('collection("fighters")')) {
  fail('FighterRegistryManager must not access Firestore directly');
} else if (!fighterRegistrySource.includes('FighterBackendService')) {
  fail('ordinary fighter registry operations are not routed through backend');
} else {
  ok('ordinary fighter registry access is backend-routed');
}

const adminBackendSource = read('app/src/main/java/com/example/data/admin/AdminBackendService.kt');
if (!adminBackendSource.includes('admin_list_fighters')) {
  fail('global fighter list is not admin-backend protected');
} else {
  ok('global fighter list requires admin backend');
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

const syncIdentitySource = read('app/src/main/java/com/example/data/sync/SyncIdentityGenerator.kt');
if (!syncIdentitySource.includes('fun newUnitKey(): String = "kapt_" + randomHex(20)')) {
  fail('new unit keys must keep at least 80 bits of random hex entropy');
} else {
  ok('new sync unit keys are high-entropy while legacy keys remain compatible');
}
if (!syncIdentitySource.includes('fun newDeviceId(): String = "dev_" + randomHex(16)')) {
  fail('new device IDs must keep at least 64 bits of random hex entropy');
} else {
  ok('new sync device IDs are collision-resistant');
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

const telegramNotifierSource = read('app/src/main/java/com/example/data/notification/TelegramNotifier.kt');
if (telegramNotifierSource.includes('script.google.com') ||
    telegramNotifierSource.includes('send_telegram') ||
    telegramNotifierSource.includes('HttpURLConnection') ||
    telegramNotifierSource.includes('api.telegram.org')) {
  fail('future Android must not call a public/direct Telegram relay');
} else {
  ok('future Android Telegram notifications are backend-owned');
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
