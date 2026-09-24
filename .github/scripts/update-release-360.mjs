import { readFileSync, writeFileSync, readdirSync } from 'node:fs';

// Usage: node update-release-360.mjs <apkSize> <apkSha256> <signerSha256> <releaseDate>
// Moves the site from 3.5.0 (32) to 3.6.0 (33). Fails loudly if an expected marker is missing.
const [size, sha, signer, date] = process.argv.slice(2);
if (!size || !sha || !signer || !date) {
  console.error('missing args');
  process.exit(1);
}

const OLD = { name: '3.5.0', code: 32, web: '3.5.0-web.52', sha: '49e8dfb2566545258f51207e1bf7de85fdbed3fb4fedbd3713d00021bfa11a1f' };
const NEW = { name: '3.6.0', code: 33, web: '3.6.0-web.53' };
const HIGHLIGHTS = [
  'новый современный интерфейс: плавающая нижняя панель, компактный главный экран, анимации и вибрация при сохранении',
  'подключение второго телефона по QR-коду подразделения',
  'заявки по этапам «новая → сборка → собрана → выдана»; выдача по заявке проводится со склада',
  'журнал операций по дням с фильтром «сегодня / 7 / 30 дней»',
  'форма № 8 — раздаточная ведомость по приказу МО РФ № 139, форма № 18 — книга учёта по наименованиям',
  'регистрация с кодом на почту, письмо с ключом подразделения',
  'защищённая синхронизация и лицензии, проверяемые сервером; новое окно оплаты PRO'
];
const SHORT = 'Новый интерфейс • QR-подключение • заявки по этапам • формы 8 и 18 • защищённая синхронизация';

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
  releaseType: 'новый интерфейс, QR-подключение и заявки',
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
    .replace(new RegExp(`<strong>Версия:<\\/strong>\\s*${OLD.name.replace(/\./g, '\\.')} • build ${OLD.code} • [\\d\\s ]+ байт`),
      `<strong>Версия:</strong> ${NEW.name} • build ${NEW.code} • ${sizeText} байт`)
    .split(OLD.sha).join(sha)
    .replace(`Что изменилось в ${OLD.name}`, `Что изменилось в ${NEW.name}`)
    .replace(/(<span>Что изменилось в 3\.6\.0<\/span>\s*<div>)[^<]*(<\/div>)/, `$1${SHORT}$2`);
  must(p, s, t);
  if (!t.includes(sha)) throw new Error('SHA-256 marker not updated');
  if (!t.includes(`${NEW.name} • build ${NEW.code}`)) throw new Error('integrity line not updated');
}

// updates.html: new current release on top, 3.5.0 becomes history.
{
  const p = 'docs/updates.html';
  const s = readFileSync(p, 'utf8');
  const oldStart = s.indexOf('<article class="release">');
  const oldEnd = s.indexOf('</article>', oldStart) + '</article>'.length;
  if (oldStart < 0) throw new Error('release list not found');
  const history = `<article class="release">
        <div class="release-head"><div><strong>Каптёрка PRO ${OLD.name}</strong><small>build ${OLD.code} • предыдущий релиз</small></div><span class="release-badge">История</span></div>
        <ul>
          <li>оплата переведена на защищённый сервер — исправлена ошибка «ошибка шлюза»;</li>
          <li>секретные ключи ЮKassa удалены из приложения;</li>
          <li>лицензия активируется автоматически после подтверждения оплаты сервером.</li>
        </ul>
      </article>`;
  const current = `<article class="release">
        <div class="release-head"><div><strong>Каптёрка PRO <span data-release-version>${NEW.name}</span></strong><small>build <span data-release-code>${NEW.code}</span> • <span data-release-type>новый интерфейс, QR-подключение и заявки</span></small></div><span class="release-badge">Текущая версия</span></div>
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
  let t = s.slice(0, oldStart) + current + s.slice(oldEnd);
  t = t.replace('<span>Room DB v2</span>', '<span>Room DB v3</span>');
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

console.log(`site metadata updated for ${NEW.name} (${NEW.code})`);
