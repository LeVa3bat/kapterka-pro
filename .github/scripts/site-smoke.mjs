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
  'docs/guide.css',
  'docs/app.js',
  'docs/auth-config.js',
  'docs/auth-v2.js',
  'docs/privacy.html',
  'docs/terms.html',
  'docs/updates.html',
  'docs/help.html',
  'docs/security.html',
  'docs/uchet-ostatkov-na-telefone.html',
  'docs/uchet-imushchestva-offline.html',
  'docs/skladskoy-uchet-android.html',
  'docs/inventarizaciya-na-android.html',
  'docs/guides.html',
  'docs/prihod-rashod-sklad-android.html',
  'docs/uchet-vydachi-imushchestva-android.html',
  'docs/peremeshchenie-mezhdu-skladami-android.html',
  'docs/robots.txt',
  'docs/google3271685078741b10.html',
  'docs/sitemap.xml',
  'docs/site.webmanifest',
  'docs/version.json',
  'docs/release.json',
  'docs/release-meta.js',
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

const guideFiles = [
  'docs/skladskoy-uchet-android.html',
  'docs/uchet-imushchestva-offline.html',
  'docs/uchet-ostatkov-na-telefone.html',
  'docs/guides.html',
  'docs/prihod-rashod-sklad-android.html',
  'docs/inventarizaciya-na-android.html',
  'docs/uchet-vydachi-imushchestva-android.html',
  'docs/peremeshchenie-mezhdu-skladami-android.html',
  'docs/security.html',
  'docs/updates.html',
  'docs/help.html'
];
for (const file of guideFiles) {
  const source = read(file);
  const guideTitle = (source.match(/<title>([^<]+)<\/title>/i) || [])[1] || '';
  const guideH1Count = (source.match(/<h1\b/gi) || []).length;
  if (!guideTitle || guideH1Count !== 1 || !/rel="canonical"/.test(source)) fail(`SEO guide is incomplete: ${file}`);
}
if (!process.exitCode) ok('SEO guide pages are present and structured');

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
if (!index.includes('сборка 31') && !index.includes('Сборка 31') && !index.includes('data-release-code>31</span>')) fail('site build 31 marker is missing');


const htmlFiles = ['docs/index.html', 'docs/privacy.html', 'docs/terms.html', 'docs/security.html', 'docs/updates.html', 'docs/help.html', 'docs/skladskoy-uchet-android.html', 'docs/uchet-imushchestva-offline.html', 'docs/uchet-ostatkov-na-telefone.html', 'docs/guides.html', 'docs/prihod-rashod-sklad-android.html', 'docs/inventarizaciya-na-android.html', 'docs/uchet-vydachi-imushchestva-android.html', 'docs/peremeshchenie-mezhdu-skladami-android.html', 'docs/404.html'];
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


if (!app.includes('function classifyLicenseKey')) fail('legacy license-key classifier is missing');
if (!/KPT\|KAPT|KAPT\|KPT/.test(app) && !app.includes('(?:KAPT|KPT)')) fail('legacy KPT/KAPT compatibility is missing');
if (/недействителен или подделан|ключ не прошел проверку подлинности/i.test(app)) fail('legacy keys must not be mislabeled as counterfeit');
else ok('legacy license-key compatibility is present without counterfeit wording');

if (!app.includes("return 'legacy_unverified'")) fail('legacy keys must be classified as unverified');
if (/applyNewPaidKey\([^\n]+legacy/i.test(app)) fail('legacy-looking keys must never be activated client-side');
if (/Старый ключ Каптёрка PRO принят для совместимости/i.test(app)) fail('unsafe legacy acceptance wording returned');
else ok('legacy-looking keys are never activated client-side');

if (!app.includes('fetchLicenseRegistryMeta') || !app.includes('FIRESTORE_LICENSE_DOC_BASE')) fail('license registry lookup is missing');
if (!app.includes('registryVerified') || !app.includes('expiresAt')) fail('license expiry verification metadata is missing');
if (app.includes("status: 'Активен (30 дн)'")) fail('static 30-day license status returned');
if (!index.includes('id="cabDaysLeft"') || !index.includes('id="cabExpiryProgress"')) fail('license countdown UI is missing');
else ok('license registry countdown is wired');

const versionData = JSON.parse(read('docs/version.json'));
if (!versionData.version || !index.includes('KAPTERKA_WEB_VERSION')) fail('web cache-version handshake is missing');
else ok('web cache-version handshake is present');

const apkTrackerCount = (app.match(/function\s+trackApkDownload\s*\(/g) || []).length;
if (apkTrackerCount !== 1) fail(`expected one trackApkDownload function, found ${apkTrackerCount}`);
if (!app.includes('function trackSiteAction') || !app.includes('installSiteFunnelTracking')) fail('privacy-safe site funnel analytics is missing');
if (/trackSiteAction\([^\n]*(?:email|callsign|licenseKey|unitKey|paymentId)\s*:/i.test(app)) fail('personal/sensitive data must not be sent through site funnel analytics');
else ok('single APK analytics handler and privacy-safe funnel tracking are present');

const googleVerify = read('docs/google3271685078741b10.html').trim();
if (googleVerify !== 'google-site-verification: google3271685078741b10.html') fail('Google verification file changed or is invalid');
else ok('Google verification file is valid');

const robots = read('docs/robots.txt');
if (!/Sitemap:\s*https:\/\/kapterka-pro\.ru\/sitemap\.xml/i.test(robots)) fail('robots.txt must reference the canonical sitemap');
else ok('robots.txt points to the canonical sitemap');

const sitemap = read('docs/sitemap.xml');
const requiredSitemapUrls = [
  'https://kapterka-pro.ru/',
  'https://kapterka-pro.ru/privacy.html',
  'https://kapterka-pro.ru/terms.html',
  'https://kapterka-pro.ru/security.html',
  'https://kapterka-pro.ru/updates.html',
  'https://kapterka-pro.ru/help.html',
  'https://kapterka-pro.ru/skladskoy-uchet-android.html',
  'https://kapterka-pro.ru/uchet-imushchestva-offline.html',
  'https://kapterka-pro.ru/uchet-ostatkov-na-telefone.html',
  'https://kapterka-pro.ru/guides.html',
  'https://kapterka-pro.ru/prihod-rashod-sklad-android.html',
  'https://kapterka-pro.ru/inventarizaciya-na-android.html',
  'https://kapterka-pro.ru/uchet-vydachi-imushchestva-android.html',
  'https://kapterka-pro.ru/peremeshchenie-mezhdu-skladami-android.html'
];
const missingSitemapUrls = requiredSitemapUrls.filter(url => !sitemap.includes('<loc>' + url + '</loc>'));
if (missingSitemapUrls.length) fail('sitemap is missing URLs: ' + missingSitemapUrls.join(', '));
else ok('sitemap contains all public indexable pages');

if (!index.includes('class="neo-trust"') || !index.includes('id="why-kapterka"') || !index.includes('class="neo-resource-strip"')) {
  fail('professional trust/security/update surfaces are missing from homepage');
}
const securityPage = read('docs/security.html');
const updatesPage = read('docs/updates.html');
if ((securityPage.match(/<h1\b/gi) || []).length !== 1 || !/rel="canonical"/.test(securityPage)) fail('security page SEO structure is invalid');
if ((updatesPage.match(/<h1\b/gi) || []).length !== 1 || !/rel="canonical"/.test(updatesPage)) fail('updates page SEO structure is invalid');
if (!securityPage.includes('39ffa4cf13a50398235078a49b7dfaa420fdd095d3258bab8336edb79c410250')) fail('security page APK hash is missing');
if (!securityPage.includes('843a7e883914f3a7a5a7665ff07b2e8c43da87a24ee4dc35e1600758aee73cb9')) fail('security page signer fingerprint is missing');
else ok('professional trust/security/update surfaces are present');

if (!index.includes('id="plans"') || !index.includes('neo-plan-demo') || !index.includes('neo-plan-pro')) fail('professional Demo/PRO comparison section is missing');
if (!index.includes('3 дня') || !index.includes('490 ₽') || !index.includes('30 дней')) fail('Demo/PRO terms are missing from homepage');
else ok('professional demo/PRO section is present');

const helpPage = read('docs/help.html');
if ((helpPage.match(/<h1\b/gi) || []).length !== 1 || !/rel="canonical" href="https:\/\/kapterka-pro\.ru\/help\.html"/.test(helpPage)) fail('help page SEO structure is invalid');
if (!helpPage.includes('https://t.me/kapterka_help_bot') || !helpPage.includes('index.html#tabDownload')) fail('help page support/install routes are incomplete');
if (!index.includes('href="help.html"')) fail('homepage does not link to support center');
else ok('professional support center is present and linked');

const releaseManifest = JSON.parse(read('docs/release.json'));
if (releaseManifest.versionName !== '3.4.9' || Number(releaseManifest.versionCode) !== 31) fail('release.json current release metadata is wrong');
if (releaseManifest.packageName !== 'com.aistudio.kapterka.jmwqve' || Number(releaseManifest.minSdk) !== 24) fail('release.json compatibility metadata is wrong');
if (releaseManifest.apkSha256 !== '39ffa4cf13a50398235078a49b7dfaa420fdd095d3258bab8336edb79c410250') fail('release.json APK hash changed unexpectedly');
if (releaseManifest.signerSha256 !== '843a7e883914f3a7a5a7665ff07b2e8c43da87a24ee4dc35e1600758aee73cb9') fail('release.json signer fingerprint changed unexpectedly');

for (const [name, source] of [['index', index], ['security', securityPage], ['updates', updatesPage], ['help', read('docs/help.html')]]) {
  if (!source.includes(releaseManifest.versionName)) fail(`${name} page does not show current release version`);
}

if (!index.includes('id="previewGalleryTrack"') || !index.includes('id="previewGalleryDots"') || !index.includes('id="modalScreenshotCounter"')) fail('professional screenshot gallery controls are missing');
if (!app.includes('SCREENSHOT_GALLERY') || !app.includes('openGallerySlide') || !app.includes('shiftGallerySlide') || !app.includes('installPreviewGallery')) fail('screenshot gallery logic is incomplete');
if (!app.includes('loadReleaseManifest') || !index.includes('data-release-version') || !index.includes('data-release-code')) fail('release manifest is not wired to visible site metadata');
if (!app.includes("trackSiteAction('pro_view'") || !app.includes("trackSiteAction('scroll_depth'") || !app.includes("support_center_open")) fail('site funnel analytics coverage is incomplete');
else ok('release manifest and professional gallery are consistent');

const publicHtmlForA11y = [
  'docs/index.html','docs/help.html','docs/security.html','docs/updates.html',
  'docs/privacy.html','docs/terms.html','docs/skladskoy-uchet-android.html',
  'docs/uchet-imushchestva-offline.html','docs/uchet-ostatkov-na-telefone.html',
  'docs/guides.html','docs/prihod-rashod-sklad-android.html','docs/inventarizaciya-na-android.html',
  'docs/uchet-vydachi-imushchestva-android.html','docs/peremeshchenie-mezhdu-skladami-android.html'
];
for (const file of publicHtmlForA11y) {
  const source = read(file);
  if (!/lang=["']ru["']/.test(source)) fail(`${file}: html lang=ru is missing`);
  if (!source.includes('class="skip-link"')) fail(`${file}: skip link is missing`);
  const imgs = [...source.matchAll(/<img\b[^>]*>/gi)].map(m => m[0]);
  if (imgs.some(tag => !/\balt=["'][^"']*["']/i.test(tag))) fail(`${file}: image alt is missing`);
  const unsafeBlanks = [...source.matchAll(/<a\b[^>]*target=["']_blank["'][^>]*>/gi)]
    .map(m => m[0]).filter(tag => !/rel=["'][^"']*noopener[^"']*["']/i.test(tag));
  if (unsafeBlanks.length) fail(`${file}: target=_blank link without noopener`);
}
if (index.includes('javascript:void(0)')) fail('homepage still uses javascript:void(0) navigation');
if (!index.includes('role="button" tabindex="0"') || !index.includes('id="userQuickPill"')) fail('keyboard access for user quick pill is missing');

const releaseMeta = read('docs/release-meta.js');
const releaseForSite = JSON.parse(read('docs/release.json'));
if (!Array.isArray(releaseForSite.highlights) || releaseForSite.highlights.length < 3) fail('release.json highlights are incomplete');
if (!releaseMeta.includes("data-release-highlights") || !releaseMeta.includes("data-release-version")) fail('release-meta.js does not hydrate current-release content');
if (!updatesPage.includes('data-release-highlights') || !updatesPage.includes('release-meta.js')) fail('updates page is not wired to release.json');

for (const eventName of ['apk_download','rustore_open','pro_view','license_activated','support_center_open','support_open','faq_open','gallery_open','scroll_depth']) {
  if (!app.includes(eventName)) fail(`analytics event missing: ${eventName}`);
}
if (!app.includes('installProfessionalReveal')) fail('safe professional reveal initializer is missing');
else ok('accessibility and release automation guards are present');

if (index.includes('<script async src="https://www.googletagmanager.com/gtag/js')) fail('Google Analytics returned to render-time loading');
if (!index.includes('scheduleAnalyticsLoad') || !index.includes("setTimeout(start, 8000)")) fail('deferred analytics loader is missing');
if (!index.includes("gtag('config', 'G-RYV6TP63D3')") || !index.includes("ym(112482290, 'init'")) fail('analytics queues/IDs are missing');
if (/aria-label="Открыть экран (Главная|Каталог)"/.test(index)) fail('gallery aria-label overrides visible text');
else ok('analytics is queued and deferred for initial-render performance');

const seoGrowthPages = [
  'docs/guides.html',
  'docs/prihod-rashod-sklad-android.html',
  'docs/inventarizaciya-na-android.html',
  'docs/uchet-vydachi-imushchestva-android.html',
  'docs/peremeshchenie-mezhdu-skladami-android.html'
];
for (const file of seoGrowthPages) {
  if (!fs.existsSync(file)) fail(`${file}: SEO growth page missing`);
  const source = read(file);
  if (!/<h1>[^<]+<\/h1>/.test(source)) fail(`${file}: H1 missing`);
  if (!/name=["']description["']/.test(source)) fail(`${file}: meta description missing`);
  if (!/rel=["']canonical["']/.test(source)) fail(`${file}: canonical missing`);
  if (!/name=["']robots["'][^>]*index/.test(source)) fail(`${file}: robots index missing`);
}
for (const url of ['guides.html','prihod-rashod-sklad-android.html','inventarizaciya-na-android.html','uchet-vydachi-imushchestva-android.html','peremeshchenie-mezhdu-skladami-android.html']) {
  if (!sitemap.includes(url)) fail(`sitemap missing ${url}`);
}
if (!index.includes('href="guides.html">Материалы</a>')) fail('homepage footer does not link to SEO content hub');
else ok('SEO growth pages are indexed and internally linked');

if (!index.includes('class="neo-guides"') ||
    !index.includes('href="guides.html"') ||
    !index.includes('href="prihod-rashod-sklad-android.html"') ||
    !index.includes('href="inventarizaciya-na-android.html"')) {
  fail('homepage SEO content hub is incomplete');
}
if (!index.includes('"@id": "https://kapterka-pro.ru/#website"') ||
    !index.includes('"@id": "https://kapterka-pro.ru/#organization"')) {
  fail('homepage WebSite/Organization structured data is missing');
}
else ok('visible SEO content hub and entity structured data are present');
