import { readFileSync, writeFileSync, readdirSync } from 'node:fs';

// Usage: node update-release-370.mjs <apkSize> <apkSha256> <signerSha256> <releaseDate>
// Moves the site from 3.6.1 (34) to 3.7.0 (35). Fails loudly if an expected marker is missing.
const [size, sha, signer, date] = process.argv.slice(2);
if (!size || !sha || !signer || !date) {
  console.error('missing args');
  process.exit(1);
}

const OLD = { name: '3.6.1', code: 34, web: '3.6.1-web.54', sha: 'f1bddc1f57d88154af27be3e1f091551c61e4bebdcbcdca06daff9ef349705ba' };
const NEW = { name: '3.7.0', code: 35, web: '3.7.0-web.55' };
const HIGHLIGHTS = [
  'резервная копия в файл (можно сохранить на Google Диск) и восстановление из неё; автокопия раз в неделю',
  'облачная копия склада подразделения: обновляется раз в сутки, кнопки «В облако» и «Из облака»',
  'статус синхронизации на кнопке «Синхр.»: только что, N мин назад, нет сети; при возвращении интернета синхронизация запускается сама',
  'локальный отчёт о сбое без личных данных с кнопкой «Отправить»'
];
const SHORT = 'Резервная копия в файл и в облако, статус синхронизации на главном экране';

const escRe = (s) => s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
const sizeText = Number(size).toLocaleString('ru-RU').replace(/\s/g, ' ');
const must = (p, before, after) => {
  if (after === before) throw new Error('nothing changed in ' + p);
  writeFileSync(p, after);
};

// release.json / version.json
const rj = JSON.parse(readFileSync('docs/release.json', 'utf8'));
if (rj.versionName !== OLD.name) throw new Error('release.json is not at ' + OLD.name);
Object.assign(rj, {
  versionName: NEW.name,
  versionCode: NEW.code,
  apkSize: Number(size),
  apkSha256: sha,
  signerSha256: signer,
  releaseDate: date,
  releaseType: 'надёжность данных',
  highlights: HIGHLIGHTS,
  roomDatabaseVersion: 3
});
writeFileSync('docs/release.json', JSON.stringify(rj, null, 2) + '\n');
writeFileSync('docs/version.json', JSON.stringify({ version: NEW.web, updatedAt: new Date().toISOString() }, null, 2) + '\n');

// Generic markers on every page.
for (const f of readdirSync('docs').filter((n) => n.endsWith('.html'))) {
  const p = 'docs/' + f;
  const s = readFileSync(p, 'utf8');
  const t = s
    .split(`data-release-version>${OLD.name}<`).join(`data-release-version>${NEW.name}<`)
    .split(`data-release-code>${OLD.code}<`).join(`data-release-code>${NEW.code}<`)
    .split(`data-release-version>${OLD.name}</b>`).join(`data-release-version>${NEW.name}</b>`)
    .split(`Официальная версия ${OLD.name}`).join(`Официальная версия ${NEW.name}`)
    .split(`текущая версия ${OLD.name} build ${OLD.code}`).join(`текущая версия ${NEW.name} build ${NEW.code}`)
    .split(`KAPTERKA_WEB_VERSION = "${OLD.web}"`).join(`KAPTERKA_WEB_VERSION = "${NEW.web}"`);
  if (t !== s) writeFileSync(p, t);
}

// index.html: schema version, integrity line, SHA, "what changed" strip.
{
  const p = 'docs/index.html';
  const s = readFileSync(p, 'utf8');
  const t = s
    .replace(`"softwareVersion": "${OLD.name}"`, `"softwareVersion": "${NEW.name}"`)
    .replace(new RegExp(`<strong>Версия:<\\/strong>\\s*${escRe(OLD.name)} • build ${OLD.code} • [\\d\\s ]+ байт`),
      `<strong>Версия:</strong> ${NEW.name} • build ${NEW.code} • ${sizeText} байт`)
    .split(OLD.sha).join(sha)
    .replace(`Что изменилось в ${OLD.name}`, `Что изменилось в ${NEW.name}`)
    .replace(new RegExp(`(<span>Что изменилось в ${escRe(NEW.name)}<\\/span>\\s*<div>)[^<]*(<\\/div>)`), `$1${SHORT}$2`);
  must(p, s, t);
  if (!t.includes(sha)) throw new Error('SHA-256 marker not updated');
  if (!t.includes(`${NEW.name} • build ${NEW.code}`)) throw new Error('integrity line not updated');
}

// updates.html: new current release on top, 3.6.0 becomes history.
{
  const p = 'docs/updates.html';
  const s = readFileSync(p, 'utf8');
  const oldStart = s.indexOf('<article class="release">');
  const oldEnd = s.indexOf('</article>', oldStart) + '</article>'.length;
  if (oldStart < 0) throw new Error('release list not found');
  const history = `<article class="release">
        <div class="release-head"><div><strong>Каптёрка PRO ${OLD.name}</strong><small>build ${OLD.code} • предыдущий релиз</small></div><span class="release-badge">История</span></div>
        <ul>
          <li>исправлена корректировка остатка на складе: она сразу создаёт операцию прихода или расхода и видна в истории;</li>
          <li>приход/расход и отчёты по форме № 8/18 совпадают с фактическим остатком после корректировки;</li>
          <li>в диалоге корректировки остатка появилось поле «причина».</li>
        </ul>
      </article>`;
  const current = `<article class="release">
        <div class="release-head"><div><strong>Каптёрка PRO <span data-release-version>${NEW.name}</span></strong><small>build <span data-release-code>${NEW.code}</span> • <span data-release-type>надёжность данных</span></small></div><span class="release-badge">Текущая версия</span></div>
        <ul data-release-highlights>
${HIGHLIGHTS.map((h, i) => `          <li>${h}${i === HIGHLIGHTS.length - 1 ? '.' : ';'}</li>`).join('\n')}
        </ul>
        <div class="spec">
          <div><span>Package</span><strong class="mono" data-release-package>com.aistudio.kapterka.jmwqve</strong></div>
          <div><span>versionCode</span><strong data-release-code>${NEW.code}</strong></div>
          <div><span>Room DB</span><strong>3 • явная миграция, данные сохраняются</strong></div>
          <div><span>Проверка обновления</span><strong>подпись APK совпадает с 3.4.9 и 3.5.0 — устанавливается поверх без удаления, данные и лицензия сохраняются</strong></div>
        </div>
      </article>

      ${history}`;
  const t = s.slice(0, oldStart) + current + s.slice(oldEnd);
  must(p, s, t);
}

// Site smoke test expectations.
{
  const p = '.github/scripts/site-smoke.mjs';
  const s = readFileSync(p, 'utf8');
  const t = s
    .replace(`index.includes('v${OLD.name}') && !index.includes('${OLD.name}')) fail('site version ${OLD.name}`,
      `index.includes('v${NEW.name}') && !index.includes('${NEW.name}')) fail('site version ${NEW.name}`)
    .replace(`!index.includes('сборка ${OLD.code}') && !index.includes('Сборка ${OLD.code}') && !index.includes('data-release-code>${OLD.code}</span>')) fail('site build ${OLD.code}`,
      `!index.includes('сборка ${NEW.code}') && !index.includes('Сборка ${NEW.code}') && !index.includes('data-release-code>${NEW.code}</span>')) fail('site build ${NEW.code}`)
    .replace(`releaseManifest.versionName !== '${OLD.name}' || Number(releaseManifest.versionCode) !== ${OLD.code}`,
      `releaseManifest.versionName !== '${NEW.name}' || Number(releaseManifest.versionCode) !== ${NEW.code}`)
    .split(`'${OLD.sha}'`).join(`'${sha}'`);
  must(p, s, t);
}

// README.md and PROJECT_MAP.md: current version lines.
for (const [p, from, to] of [
  ['README.md', '- Версия: **' + OLD.name + '**', '- Версия: **' + NEW.name + '**'],
  ['PROJECT_MAP.md', '**' + OLD.name + '**, build **' + OLD.code + '**', '**' + NEW.name + '**, build **' + NEW.code + '**']
]) {
  const s = readFileSync(p, 'utf8');
  must(p, s, s.replace(from, to));
}

console.log(`site metadata updated for ${NEW.name} (${NEW.code})`);
