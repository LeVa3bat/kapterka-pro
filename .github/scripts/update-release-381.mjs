import { readFileSync, writeFileSync, readdirSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { execFileSync } from 'node:child_process';

// Called only after aapt/apksigner have verified the signed 3.8.1 APK.
// Usage: node update-release-381.mjs <apkSize> <apkSha256> <signerSha256> <releaseDate>
const [sizeArg, sha, signer, date] = process.argv.slice(2);
const expectedSigner = '843a7e883914f3a7a5a7665ff07b2e8c43da87a24ee4dc35e1600758aee73cb9';
const old = { name: '3.8.0', code: 36 };
const next = { name: '3.8.1', code: 37, web: '3.8.1-web.57' };
const releaseType = 'рекомендации и добровольный отзыв';
const highlights = [
  'две кнопки в разделе «Ещё»: поделиться публичной ссылкой на приложение или бесплатный шаблон учёта',
  'предложение оставить честный отзыв после сохранения Excel и возвращения из меню отправки; его можно отложить или отключить',
  'при ошибке сохранения Excel неполный файл удаляется и успешное сохранение не показывается'
];
const read = (file) => readFileSync(file, 'utf8');
const write = (file, value) => writeFileSync(file, value);
const fail = (message) => { throw new Error('STOP: ' + message); };

const size = Number(sizeArg);
if (!/^\d+$/.test(sizeArg || '') || !Number.isSafeInteger(size) || size <= 0) fail('invalid APK size');
if (!/^[a-f0-9]{64}$/.test(sha || '')) fail('invalid APK SHA-256');
if (signer !== expectedSigner) fail('published signer must be preserved');
if (!/^\d{4}-\d{2}-\d{2}$/.test(date || '') || Number.isNaN(Date.parse(date)) || new Date(date).toISOString().slice(0, 10) !== date) fail('invalid release date');

const manifest = JSON.parse(read('docs/release.json'));
if (manifest.versionName !== old.name || manifest.versionCode !== old.code) fail('expected the current 3.8.0/36 manifest');
if (manifest.packageName !== 'com.aistudio.kapterka.jmwqve' || manifest.minSdk !== 24 || manifest.roomDatabaseVersion !== 3 || manifest.signerSha256 !== expectedSigner) fail('release compatibility baseline changed');
const apk = readFileSync('docs/kapterka-pro.apk');
if (apk.length !== size || createHash('sha256').update(apk).digest('hex') !== sha) fail('provided APK size/hash differs from the actual file');
const gradle = read('app/build.gradle.kts');
if (!/versionCode\s*=.*\?\:\s*37\s*$/.test(gradle.split('\n').find(line => line.includes('versionCode =')) || '') ||
    !/versionName\s*=.*\?\:\s*"3\.8\.1"\s*$/.test(gradle.split('\n').find(line => line.includes('versionName =')) || '')) fail('source defaults must be 3.8.1/37');

// Find every required marker before changing any files.
const html = new Map(readdirSync('docs').filter(name => name.endsWith('.html')).map(name => [name, read('docs/' + name)]));
const originals = new Map(html);
const index = html.get('index.html');
const updates = html.get('updates.html');
if (!index.includes('Что изменилось в 3.8.0')) fail('homepage release summary marker missing');
const currentMatch = /<article class="release">[\s\S]*?<\/article>/.exec(updates);
if (!currentMatch || !currentMatch[0].includes('data-release-version>3.8.0<')) fail('current release article missing');
const readme = read('README.md');
const projectMap = read('PROJECT_MAP.md');
if (!/^- Версия: \*\*\d+\.\d+\.\d+\*\*/m.test(readme) || !/^- Build \/ versionCode: \*\*\d+\*\*/m.test(readme)) fail('README release markers missing');
if (!/\*\*\d+\.\d+\.\d+\*\*, build \*\*\d+\*\*/.test(projectMap)) fail('PROJECT_MAP release marker missing');

Object.assign(manifest, { versionName: next.name, versionCode: next.code, apkSize: size, apkSha256: sha, signerSha256: signer, releaseDate: date, releaseType, highlights });
write('docs/release.json', JSON.stringify(manifest, null, 2) + '\n');
const version = JSON.parse(read('docs/version.json'));
const previousWeb = version.version;
write('docs/version.json', JSON.stringify({ ...version, version: next.web, updatedAt: new Date().toISOString() }, null, 2) + '\n');

// Freeze the previous article before the central marker renderer runs.
const history = currentMatch[0]
  .replace(/\sdata-release-[a-z-]+(?:="[^"]*")?/g, '')
  .replace('Текущая версия</span>', 'История</span>');
const current = `<article class="release">
        <div class="release-head"><div><strong>Каптёрка PRO <span data-release-version>${next.name}</span></strong><small>build <span data-release-code>${next.code}</span> • <span data-release-type>${releaseType}</span></small></div><span class="release-badge">Текущая версия</span></div>
        <ul data-release-highlights>
${highlights.map((text, i) => `          <li>${text}${i === highlights.length - 1 ? '.' : ';'}</li>`).join('\n')}
        </ul>
        <div class="spec">
          <div><span>Package</span><strong class="mono" data-release-package>com.aistudio.kapterka.jmwqve</strong></div>
          <div><span>versionCode</span><strong data-release-code>${next.code}</strong></div>
          <div><span>Room DB</span><strong>3 • схема базы сохранена</strong></div>
          <div><span>Проверка обновления</span><strong>прежняя подпись APK; обновление устанавливается поверх без удаления приложения</strong></div>
        </div>
      </article>`;
html.set('updates.html', updates.replace(currentMatch[0], current + '\n\n      ' + history)
  .replace('текущая версия 3.8.0 build 36', 'текущая версия 3.8.1 build 37'));
html.set('index.html', index
  .replace('Что изменилось в 3.8.0', 'Что изменилось в 3.8.1')
  .replace(/(<span>Что изменилось в 3\.8\.1<\/span>\s*<div>)[^<]*(<\/div>)/, '$1Публичные рекомендации, добровольный отзыв, надёжное сохранение Excel$2'));
for (const [name, source] of html) {
  const changed = source.split(previousWeb).join(next.web);
  if (changed !== read('docs/' + name)) write('docs/' + name, changed);
}
write('README.md', readme.replace(/^- Версия: \*\*\d+\.\d+\.\d+\*\*/m, '- Версия: **3.8.1**')
  .replace(/^- Build \/ versionCode: \*\*\d+\*\*/m, '- Build / versionCode: **37**'));
write('PROJECT_MAP.md', projectMap.replace(/\*\*\d+\.\d+\.\d+\*\*, build \*\*\d+\*\*/, '**3.8.1**, build **37**'));

// Verifies actual APK package/version using Android aapt, then renders all derived fields.
execFileSync(process.execPath, ['.github/scripts/site-release-check.mjs', '--write'], { stdio: 'inherit' });

// Only changed pages receive new modification dates; sitemap and schema stay aligned.
const modified = new Set();
for (const [name] of html) {
  const file = 'docs/' + name;
  let source = read(file);
  if (source !== originals.get(name)) {
    modified.add(name);
    source = source.replace(/("dateModified"\s*:\s*")[^"]+("\s*)/g, (_, start, end) => start + date + end);
    write(file, source);
  }
}
const sitemap = read('docs/sitemap.xml').replace(/(<url>\s*<loc>https:\/\/kapterka-pro\.ru\/([^<]*)<\/loc>\s*<lastmod>)[^<]*(<\/lastmod>)/g,
  (match, start, urlPath, end) => modified.has(urlPath || 'index.html') ? start + date + end : match);
write('docs/sitemap.xml', sitemap);
console.log('Site release metadata updated for 3.8.1 (37); published APK signature is checked by the release workflow.');
