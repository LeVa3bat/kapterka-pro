import { readFileSync, writeFileSync } from 'node:fs';

// Usage: node update-release-350.mjs <apkSize> <apkSha256> <signerSha256> <releaseDate>
const [size, sha, signer, date] = process.argv.slice(2);
if (!size || !sha || !signer || !date) {
  console.error('missing args');
  process.exit(1);
}

const rjPath = 'docs/release.json';
const rj = JSON.parse(readFileSync(rjPath, 'utf8'));
rj.versionName = '3.5.0';
rj.versionCode = 32;
rj.apkSize = Number(size);
rj.apkSha256 = sha;
rj.signerSha256 = signer;
rj.releaseDate = date;
rj.releaseType = 'обновление безопасности оплаты';
rj.highlights = [
  'оплата переведена на защищённый сервер — исправлена ошибка «ошибка шлюза»',
  'секретные ключи ЮKassa удалены из приложения',
  'лицензия активируется автоматически после подтверждения оплаты сервером'
];
writeFileSync(rjPath, JSON.stringify(rj, null, 2) + '\n');

const vjPath = 'docs/version.json';
const vj = JSON.parse(readFileSync(vjPath, 'utf8'));
vj.version = '3.5.0-web.52';
vj.updatedAt = new Date().toISOString();
writeFileSync(vjPath, JSON.stringify(vj, null, 2) + '\n');

for (const p of ['docs/index.html', 'docs/updates.html']) {
  let s = readFileSync(p, 'utf8');
  s = s.split('data-release-version>3.4.9<').join('data-release-version>3.5.0<');
  s = s.split('data-release-code>31<').join('data-release-code>32<');
  s = s.split('текущая версия 3.4.9 build 31').join('текущая версия 3.5.0 build 32');
  s = s.split('KAPTERKA_WEB_VERSION = "3.4.9-web.51"').join('KAPTERKA_WEB_VERSION = "3.5.0-web.52"');
  writeFileSync(p, s);
}

console.log('release metadata updated for 3.5.0 (32)');
