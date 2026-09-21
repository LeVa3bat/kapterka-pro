import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const read = (p) => fs.readFileSync(path.join(root, p), 'utf8');
const fail = (message) => {
  console.error('FAIL:', message);
  process.exitCode = 1;
};
const ok = (message) => console.log('OK:', message);

const index = read('docs/index.html');
const app = read('docs/app.js');
const auth = read('docs/auth-v2.js');
const authConfig = read('docs/auth-config.js');

const requiredFiles = [
  'docs/CNAME',
  'docs/index.html',
  'docs/style.css',
  'docs/app.js',
  'docs/auth-config.js',
  'docs/auth-v2.js',
  'docs/privacy.html',
  'docs/terms.html',
  'docs/robots.txt',
  'docs/sitemap.xml',
  'docs/kapterka-pro.apk'
];

for (const file of requiredFiles) {
  const full = path.join(root, file);
  if (!fs.existsSync(full)) fail(`required file is missing: ${file}`);
}
if (!process.exitCode) ok('required public files are present');

const apkPath = path.join(root, 'docs/kapterka-pro.apk');
if (fs.existsSync(apkPath)) {
  const size = fs.statSync(apkPath).size;
  if (size < 1_000_000) fail(`APK looks too small: ${size} bytes`);
  else ok(`APK present (${Math.round(size / 1024 / 1024)} MB)`);
}

const ids = [...index.matchAll(/id="([^"]+)"/g)].map((m) => m[1]);
const dupIds = [...new Set(ids.filter((id, i) => ids.indexOf(id) !== i))];
if (dupIds.length) fail(`duplicate HTML ids: ${dupIds.join(', ')}`);
else ok('no duplicate HTML ids');

const requiredIds = [
  'tabOverview','tabCabinet','tabPayment','tabSync','tabDownload',
  'authContainer','cabinetContent','regEmail','loginEmail',
  'btnPayYooKassaMain','payEmailInput','liveKeyStatusDisplay',
  'mobileTabOverview','mobileTabCabinet','mobileTabPayment','mobileTabSync','mobileTabDownload'
];
const missingIds = requiredIds.filter((id) => !ids.includes(id));
if (missingIds.length) fail(`required UI ids are missing: ${missingIds.join(', ')}`);
else ok('critical UI ids are present');

const bannedPublicPatterns = [
  ['legacy OTP bypass', /entered\s*!==\s*['"]1111['"]/],
  ['legacy demo password', /password\s*:\s*['"]demo['"]/],
  ['public admin tab', /id=['"]tabAdmin['"]/],
  ['public live YooKassa secret', /live_[A-Za-z0-9_-]{20,}/],
  ['Telegram bot token shape', /\b\d{8,12}:[A-Za-z0-9_-]{25,}\b/]
];
const publicText = [index, app, auth, authConfig].join('\n');
for (const [label, pattern] of bannedPublicPatterns) {
  if (pattern.test(publicText)) fail(label);
}
if (!process.exitCode) ok('legacy bypasses/secrets are absent from public web code');

const forbiddenPublicFiles = [
  'docs/google-apps-script.js',
  'docs/yandex-cloud-function.js',
  'docs/yookassa-webhook.js',
  'docs/app.js.patch',
  'docs/app.js.diff',
  'docs/kapterka-app-src.zip'
];
for (const file of forbiddenPublicFiles) {
  if (fs.existsSync(path.join(root, file))) fail(`server/source artifact must not be public: ${file}`);
}
if (!process.exitCode) ok('server/source artifacts are outside docs');

if (!/KAPTERKA_AUTH_API_URL\s*=\s*["']https:\/\/script\.google\.com\/macros\/s\/[^"']+\/exec["']/.test(authConfig)) {
  fail('Web Auth endpoint is not configured to a Google Apps Script /exec URL');
} else {
  ok('Web Auth endpoint is configured');
}

if (!index.includes('kapterka-pro.apk')) fail('APK download link is missing from index.html');
else ok('APK download link is present');

if (!index.includes('v3.4.9') && !index.includes('3.4.9')) fail('site version 3.4.9 is not visible');
if (!index.includes('сборка 31') && !index.includes('Сборка 31')) fail('site build 31 marker is missing');

const docsFiles = fs.readdirSync(path.join(root, 'docs'));
const staleArtifacts = docsFiles.filter((name) => /\.(patch|diff)$/i.test(name) || /-v3\.4\.[0-8].*\.apk$/i.test(name));
if (staleArtifacts.length) fail(`stale public artifacts found: ${staleArtifacts.join(', ')}`);
else ok('no stale public patch/old APK artifacts');

if (process.exitCode) {
  console.error('\nSite smoke check FAILED.');
  process.exit(process.exitCode);
}
console.log('\nSite smoke check PASSED.');
