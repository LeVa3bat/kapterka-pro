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

if (/DEFAULT_LIVE_KEY|live_[A-Za-z0-9_-]{20,}/.test(read('app/src/main/java/com/example/data/payment/YooKassaPaymentService.kt'))) {
  warn('YooKassa secret material is still present in Android source. Do not treat this as resolved; migrate payment API calls to a server before a future security release.');
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
