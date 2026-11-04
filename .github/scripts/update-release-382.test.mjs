import assert from 'node:assert/strict';
import { readFileSync, readdirSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { main, prepareReleasePages, verifyApkBytes } from './update-release-382.mjs';

// These are packaging-plan and rejection tests. No candidate or release
// provenance is fabricated, and no test writes a public release file.
const read = file => readFileSync(file, 'utf8');
const previousWeb = '3.8.1-web.57';
const pages = new Map([
  ['index.html', '<script>window.KAPTERKA_WEB_VERSION = "3.8.1-web.57";</script><span>Что изменилось в 3.8.1</span><div>Прежнее описание</div>'],
  ['updates.html', '<meta name="description" content="текущая версия 3.8.1 build 37"><article class="release"><span data-release-version>3.8.1</span><span data-release-code>37</span><ul data-release-highlights><li>Прежнее изменение, которое нельзя потерять.</li></ul><span class="release-badge">Текущая версия</span></article><article class="release"><strong>3.8.0</strong><p>Предыдущая история неизменна.</p></article>']
]);
const plan = prepareReleasePages(pages, previousWeb);
const articles = plan.get('updates.html').match(/<article class="release">[\s\S]*?<\/article>/g);
assert.match(articles[0], /data-release-version>3\.8\.2</);
assert.match(articles[0], /data-release-code>38</);
assert.match(articles[0], /автоматически/);
assert.match(articles[0], /оставшемуся сроку/);
assert.match(articles[1], /3\.8\.1/);
assert.match(articles[1], />37</);
assert.match(articles[1], /История<\/span>/);
assert.doesNotMatch(articles[1], /data-release-/);
const oldArticle = pages.get('updates.html').match(/<article class="release">[\s\S]*?<\/article>/)[0];
assert.equal(articles[1], oldArticle.replace(/\sdata-release-[a-z-]+(?:="[^"]*")?/g, '').replace('Текущая версия</span>', 'История</span>'));
assert.equal(articles.slice(2).join('\n'), pages.get('updates.html').match(/<article class="release">[\s\S]*?<\/article>/g).slice(1).join('\n'));
assert.match(plan.get('index.html'), /Что изменилось в 3\.8\.2/);
assert.match(plan.get('index.html'), /KAPTERKA_WEB_VERSION = "3\.8\.2-web\.58"/);
assert.match(plan.get('updates.html'), /текущая версия 3\.8\.2 build 38/);
assert.match(pages.get('index.html'), /Что изменилось в 3\.8\.1/, 'planning does not mutate input pages');
for (const [name, replacement] of [['index.html', 'missing'], ['updates.html', 'missing']]) {
  assert.throws(() => prepareReleasePages(new Map([...pages, [name, replacement]]), previousWeb), /STOP:/);
}

const publicFiles = ['docs/release.json', 'docs/version.json', 'docs/kapterka-pro.apk', 'docs/sitemap.xml', 'README.md', 'PROJECT_MAP.md', ...readdirSync('docs').filter(name => name.endsWith('.html')).map(name => 'docs/' + name)];
const fingerprints = () => publicFiles.map(file => [file, createHash('sha256').update(readFileSync(file)).digest('hex')]);
const before = fingerprints();
const manifest = JSON.parse(read('docs/release.json'));
const args = [String(manifest.apkSize), manifest.apkSha256, manifest.signerSha256, '2026-11-04'];
assert.throws(() => main([]), /expected size/);
assert.throws(() => main([args[0], args[1], '0'.repeat(64), args[3]]), /published signer must be preserved/);
assert.throws(() => verifyApkBytes(readFileSync('docs/kapterka-pro.apk'), manifest.apkSize, '0'.repeat(64)), /provided APK size\/hash differs/);
assert.throws(() => verifyApkBytes(readFileSync('docs/kapterka-pro.apk'), manifest.apkSize + 1, manifest.apkSha256), /provided APK size\/hash differs/);
assert.throws(() => main([args[0], args[1], args[2], '2026-02-30']), /invalid release date/);
// The still-published 3.8.1 APK must be rejected even with its correct bytes,
// size, hash and historical signer; it is not the requested 3.8.2 candidate.
assert.throws(() => main(args), manifest.versionName === '3.8.1'
  ? /actual APK package\/version\/minSdk is not the 3\.8\.2\/38 candidate/
  : /expected the current 3\.8\.1\/37 manifest/);
assert.deepEqual(fingerprints(), before, 'every rejection leaves all public release files byte-for-byte unchanged');
console.log('PASS: 3.8.2 HTML plan preserves prior history; missing markers, wrong signer/hash/size/date and invalid release state are rejected without public-file changes.');
