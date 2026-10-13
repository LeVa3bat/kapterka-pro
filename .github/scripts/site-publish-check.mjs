// Только GET публичного манифеста; предотвращает случайный откат APK при публикации сайта.
import fs from 'node:fs';
const local = JSON.parse(fs.readFileSync('docs/release.json','utf8'));
const response = await fetch('https://kapterka-pro.ru/release.json', {cache:'no-store',signal:AbortSignal.timeout(15000)});
if (!response.ok) throw new Error('STOP: cannot verify published release, HTTP '+response.status);
const live = await response.json();
if (!Number.isInteger(live.versionCode) || !live.apkSha256) throw new Error('STOP: invalid published release');
if (local.versionCode < live.versionCode) throw new Error('STOP: local APK '+local.versionName+'/'+local.versionCode+' is older than published '+live.versionName+'/'+live.versionCode);
if (local.versionCode === live.versionCode && local.apkSha256 !== live.apkSha256) throw new Error('STOP: same build has a different APK hash');
console.log('PASS: publication does not downgrade published APK '+live.versionName+'/'+live.versionCode+'.');
