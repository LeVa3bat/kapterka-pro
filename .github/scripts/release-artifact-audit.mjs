import fs from 'node:fs';
import crypto from 'node:crypto';

const fail = (msg) => { console.error('FAIL:', msg); process.exitCode = 1; };
const ok = (msg) => console.log('OK:', msg);
const warn = (msg) => console.warn('WARN:', msg);
const read = (p) => fs.readFileSync(p, 'utf8');

const apkPath = 'docs/kapterka-pro.apk';
const gradle = read('app/build.gradle.kts');
const index = read('docs/index.html');
const badging = read(process.argv[2] || '/tmp/apk-badging.txt');
const signer = read(process.argv[3] || '/tmp/apk-signature.txt');
const baseline = JSON.parse(read('.github/release-apk-baseline.json'));
const release = JSON.parse(read('docs/release.json'));

const appId = (gradle.match(/applicationId\s*=\s*"([^"]+)"/) || [])[1] || '';
const versionCode = String((gradle.match(/versionCode\s*=\s*(\d+)/) || [])[1] || '');
const versionName = (gradle.match(/versionName\s*=\s*"([^"]+)"/) || [])[1] || '';
const minSdk = String((gradle.match(/minSdk\s*=\s*(\d+)/) || [])[1] || '');

const pkg = badging.match(/package:\s+name='([^']+)'\s+versionCode='([^']+)'\s+versionName='([^']+)'/);
const apkPackage = pkg?.[1] || '';
const apkVersionCode = pkg?.[2] || '';
const apkVersionName = pkg?.[3] || '';
const apkMinSdk = (badging.match(/sdkVersion:'([^']+)'/) || [])[1] || '';

if (!apkPackage) fail('could not read APK package metadata with aapt');
if (apkPackage !== appId) fail(`APK package ${apkPackage} != Gradle applicationId ${appId}`);
else ok(`APK package matches: ${apkPackage}`);

if (apkVersionCode !== versionCode) fail(`APK versionCode ${apkVersionCode} != Gradle ${versionCode}`);
else ok(`APK versionCode matches: ${versionCode}`);

if (apkVersionName !== versionName) fail(`APK versionName ${apkVersionName} != Gradle ${versionName}`);
else ok(`APK versionName matches: ${versionName}`);

if (apkMinSdk !== minSdk) fail(`APK minSdk ${apkMinSdk} != Gradle ${minSdk}`);
else ok(`APK minSdk matches: ${minSdk}`);

const apk = fs.readFileSync(apkPath);
const sha256 = crypto.createHash('sha256').update(apk).digest('hex');
const size = fs.statSync(apkPath).size;

if (release.packageName !== appId) fail(`release.json packageName ${release.packageName} != Gradle ${appId}`);
if (String(release.versionCode) !== versionCode) fail(`release.json versionCode ${release.versionCode} != Gradle ${versionCode}`);
if (release.versionName !== versionName) fail(`release.json versionName ${release.versionName} != Gradle ${versionName}`);
if (String(release.minSdk) !== minSdk) fail(`release.json minSdk ${release.minSdk} != Gradle ${minSdk}`);
if (release.apkFile !== 'kapterka-pro.apk') fail('release.json APK filename changed');
if (Number(release.apkSize) !== size) fail(`release.json APK size ${release.apkSize} != file size ${size}`);
if (String(release.apkSha256 || '').toLowerCase() !== sha256) fail('release.json APK SHA-256 does not match file');
if (!process.exitCode) ok('release.json matches Gradle and APK artifact');

const siteHash = (index.match(/<strong>SHA-256:<\/strong>\s*<code>([a-f0-9]{64})<\/code>/i) || [])[1] || '';
if (!siteHash) fail('site APK SHA-256 marker is missing');
else if (siteHash.toLowerCase() !== sha256) fail(`site SHA-256 ${siteHash} != APK ${sha256}`);
else ok(`site SHA-256 matches APK: ${sha256}`);

const integrityLine = (index.match(/<strong>Версия:<\/strong>\s*([^<]+)<\/span>/i) || [])[1] || '';
const normalizedLine = integrityLine.replace(/\s+/g, ' ').trim();
const siteSizeDigits = ((normalizedLine.match(/([\d\s]+)\s*байт/) || [])[1] || '').replace(/\s/g, '');
if (siteSizeDigits && Number(siteSizeDigits) !== size) fail(`site APK size ${siteSizeDigits} != file size ${size}`);
else if (!siteSizeDigits) fail('site APK byte-size marker is missing');
else ok(`site APK size matches: ${size} bytes`);

if (!normalizedLine.includes(versionName)) fail('site integrity block does not show current versionName');
if (!normalizedLine.includes(`build ${versionCode}`)) fail('site integrity block does not show current build/versionCode');
if (normalizedLine.includes(versionName) && normalizedLine.includes(`build ${versionCode}`)) ok('site integrity version/build matches Gradle');

if (!index.includes(`"softwareVersion": "${versionName}"`)) fail('SoftwareApplication schema version does not match Gradle');
else ok('SoftwareApplication schema version matches Gradle');

const certSha = (signer.match(/Signer #1 certificate SHA-256 digest:\s*([a-f0-9:]+)/i) || [])[1] || '';
if (!certSha) fail('APK signer SHA-256 fingerprint was not reported');
else {
  const normalized = certSha.replace(/:/g, '').toLowerCase();
  ok(`APK signature verified; signer SHA-256: ${normalized}`);
  console.log(`SIGNER_SHA256=${normalized}`);

  const expectedSigner = String(baseline.signerSha256 || '').replace(/:/g, '').toLowerCase();
  const releaseSigner = String(release.signerSha256 || '').replace(/:/g, '').toLowerCase();
  if (releaseSigner !== normalized) fail(`release.json signer ${releaseSigner} != APK signer ${normalized}`);
  if (!expectedSigner) fail('release signer baseline is missing');
  else if (normalized !== expectedSigner) fail(`APK signer changed: ${normalized} != baseline ${expectedSigner}`);
  else ok('APK signer matches the published-update baseline');

  if (baseline.packageName && apkPackage !== baseline.packageName) {
    fail(`APK package ${apkPackage} != baseline package ${baseline.packageName}`);
  }
}

if (process.exitCode) {
  console.error('\nRelease APK audit FAILED.');
  process.exit(process.exitCode);
}
console.log('\nRelease APK audit PASSED.');
