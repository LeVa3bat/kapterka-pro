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
  'docs/site.webmanifest',
  'docs/404.html',
  'docs/.well-known/security.txt',
  'docs/903fd952854fcb833f54ad87ef4b033b.txt',
  'docs/kapterka-pro.apk'
];

for (const file of requiredFiles) {
  const full = path.join(root, file);
  if (!fs.existsSync(full)) fail(`required file is missing: ${file}`);
}
if (!process.exitCode) ok('required public files are present');

const indexNowKey = read('docs/903fd952854fcb833f54ad87ef4b033b.txt').trim();
if (indexNowKey !== '903fd952854fcb833f54ad87ef4b033b') fail('IndexNow verification key file is invalid');
else ok('IndexNow verification key file is valid');

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

if (!index.includes('rel="manifest"') || !index.includes('site.webmanifest')) fail('manifest link is missing from index.html');
else ok('manifest link is present');

if (!/name=["']viewport["'][^>]*viewport-fit=cover/.test(index)) fail('mobile viewport must include viewport-fit=cover');
else ok('mobile viewport supports display cutouts/safe areas');

if (!index.includes('"softwareRequirements": "Android 7.0 or later"')) fail('site compatibility text no longer matches minSdk 24');
else ok('site Android compatibility matches minSdk 24');

const title = (index.match(/<title>([^<]+)<\/title>/i) || [])[1] || '';
const description = (index.match(/<meta\s+name=["']description["']\s+content=["']([^"']+)["']/i) || [])[1] || '';
const h1Count = (index.match(/<h1\b/gi) || []).length;
if (!/Складской учёт/i.test(title) || !/Android/i.test(title)) fail('SEO title is missing primary search intent');
else ok('SEO title is present');
if (description.length < 110 || !/учёт имущества/i.test(description) || !/офлайн/i.test(description)) fail('meta description is too weak or missing core intent');
else ok('meta description covers core search intent');
if (h1Count !== 1) fail(`homepage must contain exactly one H1 (found ${h1Count})`);
else ok('homepage has one H1');
if (!index.includes('id="for-whom"') || !index.includes('id="how-to-start"')) fail('conversion/SEO explainer sections are missing');
else ok('conversion/SEO explainer sections are present');

const manifest = JSON.parse(read('docs/site.webmanifest'));
if (manifest.name !== 'Каптёрка PRO' || !manifest.start_url || !Array.isArray(manifest.icons) || !manifest.icons.length) {
  fail('site.webmanifest is incomplete');
} else {
  ok('site.webmanifest is valid enough for install metadata');
}

const securityTxt = read('docs/.well-known/security.txt');
if (!/Contact:\s*mailto:/i.test(securityTxt) || !/Canonical:\s*https:\/\/kapterka-pro\.ru\/\.well-known\/security\.txt/i.test(securityTxt)) {
  fail('security.txt is missing contact/canonical');
} else {
  ok('security.txt contains contact and canonical');
}

if (!index.includes('v3.4.9') && !index.includes('3.4.9')) fail('site version 3.4.9 is not visible');
if (!index.includes('сборка 31') && !index.includes('Сборка 31')) fail('site build 31 marker is missing');


const htmlFiles = ['docs/index.html', 'docs/privacy.html', 'docs/terms.html', 'docs/404.html'];
const missingLocalTargets = [];
for (const htmlFile of htmlFiles) {
  const source = read(htmlFile);
  const baseDir = path.dirname(htmlFile);
  const refs = [...source.matchAll(/(?:href|src)=["']([^"']+)["']/g)].map((m) => m[1]);
  for (const ref of refs) {
    if (!ref || ref.startsWith('#') || ref.startsWith('mailto:') || ref.startsWith('tel:') ||
        ref.startsWith('javascript:') || /^https?:\/\//i.test(ref) || ref.startsWith('data:')) continue;
    const clean = ref.split('#')[0].split('?')[0];
    if (!clean) continue;
    const target = clean.startsWith('/')
      ? path.join('docs', clean.replace(/^\/+/, ''))
      : path.normalize(path.join(baseDir, clean));
    if (!fs.existsSync(path.join(root, target))) missingLocalTargets.push(`${htmlFile} -> ${ref}`);
  }
}
if (missingLocalTargets.length) fail(`broken local links/assets: ${missingLocalTargets.join(', ')}`);
else ok('internal links/assets resolve');

const modalIds = [...index.matchAll(/class="modal-overlay" id="([^"]+)"/g)].map((m) => m[1]);
const openModalTargets = [...index.matchAll(/openModal\('([^']+)'\)/g)].map((m) => m[1]);
const missingModals = [...new Set(openModalTargets.filter((id) => !modalIds.includes(id)))];
if (missingModals.length) fail(`openModal targets are missing: ${missingModals.join(', ')}`);
else ok('modal targets exist');

const criticalScripts = app + '\n' + auth;
const inlineCalls = [...index.matchAll(/(?:onclick|onsubmit)=["']([^"']+)["']/g)]
  .flatMap((m) => [...m[1].matchAll(/\b([A-Za-z_$][\w$]*)\s*\(/g)].map((x) => x[1]))
  .filter((name) => !['if'].includes(name));
const definedFunctions = new Set([...criticalScripts.matchAll(/(?:async\s+)?function\s+([A-Za-z_$][\w$]*)\s*\(/g)].map((m) => m[1]));
['openModal','closeModal'].forEach((name) => definedFunctions.add(name));
const missingHandlers = [...new Set(inlineCalls.filter((name) => !definedFunctions.has(name)))];
if (missingHandlers.length) fail(`inline handlers reference missing functions: ${missingHandlers.join(', ')}`);
else ok('inline handlers have matching functions');

if (publicText.includes('112255061')) fail('retired Yandex Metrika counter returned in public web code');
if (index.includes('webvisor:true')) fail('Webvisor must remain disabled on auth/payment pages');
if (!index.includes("ym(112482290, 'init'")) fail('current Yandex Metrika counter is missing');
else ok('analytics configuration matches current privacy settings');

const docsFiles = fs.readdirSync(path.join(root, 'docs'));
const staleArtifacts = docsFiles.filter((name) => /\.(patch|diff)$/i.test(name) || /-v3\.4\.[0-8].*\.apk$/i.test(name));
if (staleArtifacts.length) fail(`stale public artifacts found: ${staleArtifacts.join(', ')}`);
else ok('no stale public patch/old APK artifacts');

if (process.exitCode) {
  console.error('\nSite smoke check FAILED.');
  process.exit(process.exitCode);
}
console.log('\nSite smoke check PASSED.');
