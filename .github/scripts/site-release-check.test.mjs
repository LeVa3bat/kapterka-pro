import fs from 'node:fs';
import path from 'node:path';
import assert from 'node:assert/strict';
import {spawnSync} from 'node:child_process';
const checker=path.resolve('.github/scripts/site-release-check.mjs');
// aapt на Windows требует путь латиницей, как и сама Android-сборка проекта.
const tempRoot=path.resolve('growth/.tmp');
fs.mkdirSync(tempRoot,{recursive:true});
const temp=fs.mkdtempSync(path.join(tempRoot,'release-check-'));
try {
  fs.mkdirSync(path.join(temp,'docs'));
  fs.copyFileSync('docs/kapterka-pro.apk',path.join(temp,'docs/kapterka-pro.apk'));
  const release=JSON.parse(fs.readFileSync('docs/release.json','utf8'));
  function probe(data,html,pattern) {
    fs.writeFileSync(path.join(temp,'docs/release.json'),JSON.stringify(data));
    fs.writeFileSync(path.join(temp,'docs/index.html'),html);
    const result=spawnSync(process.execPath,[checker],{cwd:temp,encoding:'utf8'});
    assert.notEqual(result.status,0); assert.match(result.stdout+result.stderr,pattern);
  }
  probe({...release,apkSha256:'0'.repeat(64)},'',/APK size\/SHA-256 differs/);
  probe({...release,versionCode:999,versionName:'9.9.9'},'',/actual APK package\/version differs/);
  probe(release,'<span data-release-version>9.9.9</span>',/stale generated release metadata/);
  console.log('PASS: wrong hash, wrong actual APK version and stale HTML stop the check.');
} finally {
  if (!path.resolve(temp).startsWith(tempRoot+path.sep)) throw new Error('Invalid temporary target');
  fs.rmSync(temp,{recursive:true,force:true});
}
