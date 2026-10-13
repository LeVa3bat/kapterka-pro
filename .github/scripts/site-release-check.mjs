// Публикация читает один release.json. --write обновляет только производный HTML.
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import {execFileSync} from 'node:child_process';
const root = process.cwd(), dir = path.join(root, 'docs');
const release = JSON.parse(fs.readFileSync(path.join(dir, 'release.json'), 'utf8'));
if (release.apkFile !== 'kapterka-pro.apk' || !/^\d+\.\d+\.\d+$/.test(release.versionName) || !Number.isInteger(release.versionCode)) throw new Error('Invalid release manifest');
const apk = fs.readFileSync(path.join(dir, release.apkFile));
if (apk.length !== release.apkSize || crypto.createHash('sha256').update(apk).digest('hex') !== release.apkSha256) throw new Error('STOP: APK size/SHA-256 differs from release.json');
const sdk = process.env.ANDROID_SDK_ROOT || process.env.ANDROID_HOME || (process.platform === 'win32' ? 'C:/Android/Sdk' : '/usr/local/lib/android/sdk');
const buildTools = path.join(sdk,'build-tools');
const aaptName = process.platform === 'win32' ? 'aapt.exe' : 'aapt';
const aapt = fs.existsSync(buildTools) ? fs.readdirSync(buildTools).sort((a,b)=>b.localeCompare(a,undefined,{numeric:true})).map(name=>path.join(buildTools,name,aaptName)).find(file=>fs.existsSync(file)) : null;
if (!aapt) throw new Error('STOP: Android SDK aapt required to verify actual APK version');
const badging = execFileSync(aapt,['dump','badging',path.join(dir,release.apkFile)],{encoding:'utf8'});
const actual = /package:\s+name='([^']+)'\s+versionCode='([^']+)'\s+versionName='([^']+)'/.exec(badging);
if (!actual || actual[1] !== release.packageName || Number(actual[2]) !== release.versionCode || actual[3] !== release.versionName) throw new Error('STOP: actual APK package/version differs from release.json');
const fields = {version:release.versionName,code:release.versionCode,android:release.minAndroid,status:release.statusLabel,type:release.releaseType,package:release.packageName,sha:release.apkSha256,signer:release.signerSha256,size:(release.apkSize/1024/1024).toFixed(2).replace('.', ',')+' МиБ ('+release.apkSize+' байт)',integrity:release.versionName+' • build '+release.versionCode+' • '+release.apkSize+' байт'};
let failed = false;
for (const name of fs.readdirSync(dir).filter(name=>name.endsWith('.html'))) {
  const file = path.join(dir,name), original = fs.readFileSync(file,'utf8');
  let generated = original.replace(/(<([a-z][a-z0-9]*)\b[^>]*\bdata-release-(version|code|android|status|type|package|sha|signer|size|integrity)(?:="[^"]*")?[^>]*>)[^<]*(<\/\2>)/gi,
    (_,open,tag,key,close)=>open+fields[key]+close);
  if (name === 'index.html') generated = generated.replace(/("softwareVersion":\s*")[^"]*(")/, (_,a,b)=>a+release.versionName+b);
  if (generated !== original) {
    if (process.argv.includes('--write')) fs.writeFileSync(file, generated);
    else { console.error('STOP: stale generated release metadata in '+name+'; run node .github/scripts/site-release-check.mjs --write'); failed = true; }
  }
}
if (failed) process.exit(1);
console.log('PASS: APK bytes/hash and generated release metadata agree ('+release.versionName+'/'+release.versionCode+').');
