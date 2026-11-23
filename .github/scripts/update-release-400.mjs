import { readFileSync, writeFileSync, readdirSync, existsSync, unlinkSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import path from 'node:path';
import { pathToFileURL } from 'node:url';

// Run only with the verified GitHub candidate already copied to docs/kapterka-pro.apk.
// Usage: node .github/scripts/update-release-400.mjs <apkSize> <apkSha256> <signerSha256> <releaseDate> [candidate/source.json]
const expectedSigner = '843a7e883914f3a7a5a7665ff07b2e8c43da87a24ee4dc35e1600758aee73cb9';
const expectedPackage = 'com.aistudio.kapterka.jmwqve';
const old = { name: '3.9.0', code: 39 };
const next = { name: '4.0.0', code: 40, web: '4.0.0-web.1' };
const releaseType = 'глобальное 3D-обновление, 3 профиля складов и синхронизация заявок';
const highlights = [
  'полностью новый объёмный 3D-интерфейс (тёмная и светлая темы), живая 3D-заставка и тактильные кнопки без вылетов при нажатии',
  '3 готовых профиля складов (Гражданский 64 позиции, Воинский учёт и Пустой склад) с чистым сбросом старых операций при смене складов профиля',
  'мгновенная синхронизация заявок, статусов, карточек и категорий между телефонами со звуковым уведомлением, авто-восстановлением связи и защитой офлайн-правок',
  'честный баланс без смешивания разных единиц измерения (шт., л., пары, уп.), отдельная операция «Возврат» инструмента и точные бланки Excel (М-11 / ТОРГ-13, МБ-7, МБ-8, Формы № 8 и № 18)',
  'обновлённое окно оплаты PRO (30/90/365 дней и ввод Email сразу на первом экране), плавный возврат из банка и вход в личный кабинет сайта по коду на Email'
];
const read = (file) => readFileSync(file, 'utf8');
const fail = (message) => { throw new Error('STOP: ' + message); };

export function verifyApkBytes(apk, size, sha) {
  if (apk.length !== size || createHash('sha256').update(apk).digest('hex') !== sha) fail('provided APK size/hash differs from the actual file');
}

export function prepareReleasePages(pages, previousWeb) {
  const html = new Map(pages);
  const index = html.get('index.html') || '';
  const updates = html.get('updates.html') || '';
  if (!index.includes('Что изменилось в 3.9.0')) fail('homepage release summary marker missing');
  if (!index.includes(`KAPTERKA_WEB_VERSION = "${previousWeb}"`)) fail('website version handshake is inconsistent');
  const currentMatch = /<article class="release">[\s\S]*?<\/article>/.exec(updates);
  if (!currentMatch || !currentMatch[0].includes('data-release-version>3.9.0<')) fail('current release article missing');

  // History must stop hydrating from the current release.json.
  const history = currentMatch[0]
    .replace(/\sdata-release-[a-z-]+(?:="[^"]*")?/g, '')
    .replace('Текущая версия</span>', 'История</span>');
  const current = `<article class="release">
        <div class="release-head"><div><strong>Каптёрка PRO <span data-release-version>${next.name}</span></strong><small>build <span data-release-code>${next.code}</span> • <span data-release-type>${releaseType}</span></small></div><span class="release-badge">Текущая версия</span></div>
        <ul data-release-highlights>
${highlights.map((text, i) => `          <li>${text}${i === highlights.length - 1 ? '.' : ';'}</li>`).join('\n')}
        </ul>
        <div class="spec">
          <div><span>Package</span><strong class="mono" data-release-package>${expectedPackage}</strong></div>
          <div><span>versionCode</span><strong data-release-code>${next.code}</strong></div>
          <div><span>Room DB</span><strong>3 • схема базы сохранена</strong></div>
          <div><span>Обновление</span><strong>устанавливайте поверх приложения с прежней подписью; перед обновлением сохраните резервную копию данных</strong></div>
        </div>
      </article>`;
  if (!updates.includes('текущая версия 3.9.0 build 39')) fail('updates description marker missing');
  html.set('updates.html', updates.replace(currentMatch[0], current + '\n\n      ' + history)
    .replace('текущая версия 3.9.0 build 39', 'текущая версия 4.0.0 build 40'));
  html.set('index.html', index
    .replace('Что изменилось в 3.9.0', 'Что изменилось в 4.0.0')
    .replace(/(<span>Что изменилось в 4\.0\.0<\/span>\s*<div>)[^<]*(<\/div>)/, '$1Глобальное 3D-обновление: новый 3D-интерфейс и заставка, 3 профиля складов (64 поз. гражданский / военный / пустой), мгновенная синхронизация заявок между телефонами, возврат инструмента, раздельный баланс по единицам и точные Excel М-11 / МБ-7 / МБ-8 / Ф-8 / Ф-18$2'));
  for (const [name, source] of html) html.set(name, source.split(previousWeb).join(next.web));
  return html;
}

function verifiedSource(sourceFile, size, sha, signer) {
  if (!sourceFile) return null;
  const source = JSON.parse(read(sourceFile));
  if (source.versionName !== next.name || source.versionCode !== next.code || source.packageName !== expectedPackage ||
      source.minSdk !== 24 || source.apkSize !== size || source.apkSha256 !== sha || source.signerSha256 !== signer) fail('candidate source.json differs from the verified APK');
  if (!/^[a-f0-9]{40}$/.test(source.sourceCommit || '') || !String(source.sourceRef || '').trim() ||
      !/^\d+$/.test(String(source.workflowRunId || '')) || !Number.isFinite(Date.parse(source.builtAt))) fail('candidate provenance fields are missing or invalid');
  const commit = execFileSync('git', ['rev-parse', '--verify', source.sourceCommit + '^{commit}'], { encoding: 'utf8' }).trim();
  if (commit !== source.sourceCommit) fail('candidate source commit is unavailable');
  // Only documentation/release packaging may differ from the compiled source.
  execFileSync('git', ['diff', '--quiet', source.sourceCommit, '--', 'app', 'server', 'gradle', 'build.gradle.kts', 'settings.gradle.kts', 'gradle.properties']);
  return {
    ...source,
    verification: { ...source.verification, aapt: true, apksigner: true, sha256: true, sourceCommit: true },
    releaseWorkflowUrl: `https://github.com/LeVa3bat/kapterka-pro/actions/runs/${source.workflowRunId}`,
    physicalDeviceUpgradeTested: source.physicalDeviceUpgradeTested === true
  };
}

export function main(args = process.argv.slice(2)) {
  const [sizeArg, sha, signer, date, sourceFile] = args;
  const size = Number(sizeArg);
  if (args.length < 4 || args.length > 5) fail('expected size, APK SHA-256, signer SHA-256, release date and optional candidate source.json');
  if (!/^\d+$/.test(sizeArg || '') || !Number.isSafeInteger(size) || size <= 0) fail('invalid APK size');
  if (!/^[a-f0-9]{64}$/.test(sha || '')) fail('invalid APK SHA-256');
  if (signer !== expectedSigner) fail('published signer must be preserved');
  if (!/^\d{4}-\d{2}-\d{2}$/.test(date || '') || Number.isNaN(Date.parse(date)) || new Date(date).toISOString().slice(0, 10) !== date) fail('invalid release date');

  const manifest = JSON.parse(read('docs/release.json'));
  const baseline = JSON.parse(read('.github/release-apk-baseline.json'));
  if (manifest.versionName !== old.name || manifest.versionCode !== old.code) fail('expected the current 3.9.0/39 manifest');
  if (manifest.packageName !== expectedPackage || manifest.minSdk !== 24 || manifest.roomDatabaseVersion !== 3 || manifest.signerSha256 !== expectedSigner ||
      baseline.packageName !== expectedPackage || baseline.signerSha256 !== expectedSigner) fail('release compatibility baseline changed');
  const apkPath = path.resolve('docs/kapterka-pro.apk');
  const apk = readFileSync(apkPath);
  verifyApkBytes(apk, size, sha);
  const gradle = read('app/build.gradle.kts');
  if (!/versionCode\s*=.*\?:\s*40\s*$/m.test(gradle) || !/versionName\s*=.*\?:\s*"4\.0\.0"\s*$/m.test(gradle)) fail('source defaults must be 4.0.0/40');
  if (!/@Database\([\s\S]*?\bversion\s*=\s*3\b/.test(read('app/src/main/java/com/example/data/local/KapterkaDatabase.kt'))) fail('Room schema must remain at version 3');

  const sdk = process.env.ANDROID_SDK_ROOT || process.env.ANDROID_HOME || (process.platform === 'win32' ? 'C:/Android/Sdk' : '/usr/local/lib/android/sdk');
  const toolsRoot = path.join(sdk, 'build-tools');
  const aaptName = process.platform === 'win32' ? 'aapt.exe' : 'aapt';
  const tools = existsSync(toolsRoot) ? readdirSync(toolsRoot).sort((a, b) => b.localeCompare(a, undefined, { numeric: true }))
    .map(name => path.join(toolsRoot, name)).find(dir => existsSync(path.join(dir, aaptName)) && existsSync(path.join(dir, 'lib/apksigner.jar'))) : null;
  if (!tools) fail('Android SDK aapt and apksigner.jar are required');
  const badging = execFileSync(path.join(tools, aaptName), ['dump', 'badging', apkPath], { encoding: 'utf8' });
  const actual = /package:\s+name='([^']+)'\s+versionCode='([^']+)'\s+versionName='([^']+)'/.exec(badging);
  if (!actual || actual[1] !== expectedPackage || Number(actual[2]) !== next.code || actual[3] !== next.name || !/^sdkVersion:'24'\s*$/m.test(badging)) fail('actual APK package/version/minSdk is not the 4.0.0/40 candidate');
  const java = process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME, 'bin', process.platform === 'win32' ? 'java.exe' : 'java') : 'java';
  const certs = execFileSync(java, ['-jar', path.join(tools, 'lib/apksigner.jar'), 'verify', '--verbose', '--print-certs', apkPath], { encoding: 'utf8' });
  const actualSigner = /^Signer #1 certificate SHA-256 digest:\s*([a-f0-9:]+)\s*$/im.exec(certs)?.[1]?.replace(/:/g, '').toLowerCase();
  if (actualSigner !== expectedSigner || /^Signer #2 certificate SHA-256 digest:/im.test(certs)) fail('actual APK signer differs from the published signer');
  const source = verifiedSource(sourceFile, size, sha, signer);

  const version = JSON.parse(read('docs/version.json'));
  if (!/^3\.9\.0-web\.\d+$/.test(version.version || '')) fail('unexpected current website version');
  const originals = new Map(readdirSync('docs').filter(name => name.endsWith('.html')).map(name => [name, read('docs/' + name)]));
  const html = prepareReleasePages(originals, version.version);
  const readme = read('README.md');
  const projectMap = read('PROJECT_MAP.md');
  if (!readme.includes('- Версия: **3.9.0**') || !readme.includes('- Build / versionCode: **39**')) fail('README release markers missing');
  if (!projectMap.includes('**3.9.0**, build **39**')) fail('PROJECT_MAP release marker missing');
  const changes = new Map([
    ['docs/release.json', JSON.stringify({ ...manifest, versionName: next.name, versionCode: next.code, apkSize: size, apkSha256: sha, signerSha256: signer, releaseDate: date, releaseType, highlights }, null, 2) + '\n'],
    ['docs/version.json', JSON.stringify({ ...version, version: next.web, updatedAt: new Date().toISOString() }, null, 2) + '\n'],
    ['README.md', readme.replace('- Версия: **3.9.0**', '- Версия: **4.0.0**').replace('- Build / versionCode: **39**', '- Build / versionCode: **40**')],
    ['PROJECT_MAP.md', projectMap.replace('**3.9.0**, build **39**', '**4.0.0**, build **40**')],
    ['docs/sitemap.xml', read('docs/sitemap.xml')],
    ...Array.from(html, ([name, value]) => ['docs/' + name, value])
  ]);
  if (source) changes.set('growth/releases/4.0.0-source.json', JSON.stringify(source, null, 2) + '\n');
  const rollback = new Map(Array.from(changes.keys(), file => [file, existsSync(file) ? read(file) : null]));
  try {
    for (const [file, value] of changes) writeFileSync(file, value);
    execFileSync(process.execPath, ['.github/scripts/site-release-check.mjs', '--write'], { stdio: 'inherit' });
    const modified = new Set();
    for (const [name, original] of originals) {
      const file = 'docs/' + name;
      let text = read(file);
      if (text !== original || ['rukovodstvo-polzovatelya.html', 'help.html', 'terms.html'].includes(name)) {
        modified.add(name);
        text = text.replace(/("dateModified"\s*:\s*")[^"]+("\s*)/g, (_, start, end) => start + date + end);
        writeFileSync(file, text);
      }
    }
    const sitemap = read('docs/sitemap.xml').replace(/(<url>\s*<loc>https:\/\/kapterka-pro\.ru\/([^<]*)<\/loc>\s*<lastmod>)[^<]*(<\/lastmod>)/g,
      (match, start, urlPath, end) => modified.has(urlPath || 'index.html') ? start + date + end : match);
    writeFileSync('docs/sitemap.xml', sitemap);
    execFileSync(process.execPath, ['.github/scripts/site-release-check.mjs'], { stdio: 'inherit' });
  } catch (error) {
    for (const [file, original] of rollback) {
      if (original === null) { if (existsSync(file)) unlinkSync(file); }
      else writeFileSync(file, original);
    }
    throw error;
  }
  console.log('PASS: 4.0.0 (40) release package prepared from the actual APK; signer, package, Room schema and prior history preserved. No publication performed.');
  if (!source) console.log('No source.json supplied: growth release provenance was not created.');
}

if (process.argv[1] && pathToFileURL(path.resolve(process.argv[1])).href === import.meta.url) main();
