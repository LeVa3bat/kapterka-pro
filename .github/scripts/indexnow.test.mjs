import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {fileURLToPath, pathToFileURL} from 'node:url';
import {spawnSync} from 'node:child_process';
import {SITE, KEY, ENDPOINT, prepare, notify, retryDelay, dedupRecovered} from './indexnow.mjs';

const COMMIT = 'a'.repeat(40);
const clock = () => new Date('2026-10-14T09:00:00Z');
const page = (name, body = '', extra = '') => `<!doctype html><html><head><link rel="canonical" href="${SITE}/${name}">${extra}</head><body>${body}</body></html>`;
const rootPage = body => page('', body);

function fixture(t, files = {}, locs = ['']) {
  const temporary = fs.mkdtempSync(path.join(os.tmpdir(), 'kapterka-indexnow-test-'));
  t.after(() => {
    const relative = path.relative(os.tmpdir(), temporary);
    assert(!relative.startsWith('..') && !path.isAbsolute(relative) && relative.startsWith('kapterka-indexnow-test-'));
    fs.rmSync(temporary, {recursive: true, force: true});
  });
  const docsDir = path.join(temporary, 'docs');
  fs.mkdirSync(docsDir);
  const data = new Map();
  function write(name, body) {
    const file = path.join(docsDir, name);
    fs.mkdirSync(path.dirname(file), {recursive: true});
    fs.writeFileSync(file, body);
    const url = `${SITE}/${name === 'index.html' ? '' : name}`;
    data.set(url, Buffer.from(body));
  }
  write('index.html', rootPage('same'));
  write(`${KEY}.txt`, KEY + '\n');
  for (const [name, body] of Object.entries(files)) write(name, body);
  const locations = locs.map(name => /^https?:/.test(name) ? name : `${SITE}/${name}`);
  write('sitemap.xml', `<urlset>${locations.map(url => `<url><loc>${url}</loc><lastmod>2026-10-14</lastmod></url>`).join('')}</urlset>`);
  return {docsDir, data, write, temporary};
}

function response(url, body = '', status = 200, headers = {}, finalUrl = url) {
  const result = new Response(body, {status, headers});
  Object.defineProperty(result, 'url', {value: finalUrl});
  return result;
}

function remote(data, overrides = new Map(), post = [200]) {
  const calls = [];
  const fetchImpl = async (url, options = {}) => {
    calls.push({url, options});
    assert.equal(options.redirect, 'error');
    assert.equal(options.cache, 'no-store');
    assert(options.signal instanceof AbortSignal);
    if (options.method === 'POST') {
      assert.equal(url, ENDPOINT);
      const result = post.length > 1 ? post.shift() : post[0];
      if (result instanceof Error) throw result;
      return typeof result === 'number' ? response(url, '', result) : response(url, '', result.status, result.headers);
    }
    if (overrides.has(url)) {
      const value = overrides.get(url);
      if (value instanceof Error) throw value;
      if (typeof value === 'function') return value(url);
      return response(url, value.body ?? '', value.status ?? 200, value.headers, value.finalUrl);
    }
    return data.has(url) ? response(url, data.get(url)) : response(url, '', 404);
  };
  return {fetchImpl, calls, posts: () => calls.filter(call => call.options.method === 'POST')};
}

async function changedPlan(t, body = 'after') {
  const f = fixture(t, {'index.html': rootPage(body)});
  const live = new Map(f.data);
  live.set(SITE + '/', Buffer.from(rootPage('before')));
  const provider = remote(live);
  const plan = await prepare({docsDir: f.docsDir, commit: COMMIT, fetchImpl: provider.fetchImpl, now: clock});
  assert.equal(plan.changed.length, 1);
  return {f, plan};
}

const options = (plan, provider, extra = {}) => ({plan, commit: COMMIT, runId: '123', runAttempt: 1, deploymentSuccess: true, fetchImpl: provider.fetchImpl, now: clock, sleep: async () => {}, ...extra});

test('unchanged public content and sitemap-only changes generate no notification plan', async t => {
  const f = fixture(t, {'index.html': rootPage('same\nsecond line')});
  const live = new Map(f.data);
  live.set(SITE + '/', Buffer.from(rootPage('same\r\nsecond line')));
  live.set(SITE + '/sitemap.xml', Buffer.from('<old sitemap date>'));
  const provider = remote(live);
  const result = await prepare({docsDir: f.docsDir, commit: COMMIT, fetchImpl: provider.fetchImpl, now: clock});
  assert.deepEqual(result.changed, []);
  assert.deepEqual(result.unchanged, [SITE + '/']);
  assert.equal(provider.calls.filter(call => call.url.endsWith('sitemap.xml')).length, 0);
});

test('only changed HTML is selected; anchor links do not propagate other page changes', async t => {
  const f = fixture(t, {'index.html': rootPage('<a href="guide.html">guide</a>'), 'guide.html': page('guide.html', 'after')}, ['', 'guide.html']);
  const live = new Map(f.data);
  live.set(SITE + '/guide.html', Buffer.from(page('guide.html', 'before')));
  const provider = remote(live);
  const result = await prepare({docsDir: f.docsDir, commit: COMMIT, fetchImpl: provider.fetchImpl});
  assert.deepEqual(result.changed.map(item => item.url), [SITE + '/guide.html']);
  assert.deepEqual(result.unchanged, [SITE + '/']);
  assert.equal(provider.calls.filter(call => call.url === SITE + '/guide.html').length, 1);
});

test('changed CSS, imported CSS/fonts and local ES modules affect only their consuming pages', async t => {
  const f = fixture(t, {
    'index.html': rootPage('<script type="module" src="main.mjs"></script>'),
    'guide.html': page('guide.html', '', '<link rel="stylesheet" href="style.css">'),
    'plain.html': page('plain.html', 'unchanged'),
    'style.css': '@import "nested.css"; body{color:red}',
    'nested.css': '@font-face{src:url("font.woff2")}',
    'font.woff2': 'new-font',
    'main.mjs': 'import {thing} from "./module.mjs"; import("./dynamic.mjs");',
    'module.mjs': 'export const thing = 2;',
    'dynamic.mjs': 'export default true;'
  }, ['', 'guide.html', 'plain.html']);
  const live = new Map(f.data);
  live.set(SITE + '/module.mjs', Buffer.from('export const thing = 1;'));
  live.set(SITE + '/font.woff2', Buffer.from('old-font'));
  const provider = remote(live);
  const result = await prepare({docsDir: f.docsDir, commit: COMMIT, fetchImpl: provider.fetchImpl});
  assert.deepEqual(result.changed.map(item => item.url), [SITE + '/', SITE + '/guide.html']);
  assert(result.changed.every(item => item.reason === 'dependency_changed'));
  assert.deepEqual(result.unchanged, [SITE + '/plain.html']);
  assert(result.changed[0].resources.some(item => item.url.endsWith('dynamic.mjs')));
  assert(result.changed[1].resources.some(item => item.url.endsWith('nested.css')));
});

test('release manifest changes select only pages that render it; reload-only version and application APIs are excluded', async t => {
  const f = fixture(t, {
    'index.html': rootPage('<span data-release-version></span><script src="release-meta.js"></script>'),
    'guide.html': page('guide.html', '<script src="release-meta.js"></script>'),
    'release-meta.js': "fetch('release.json?_=' + Date.now()); fetch('./version.json'); fetch('/api/private');",
    'release.json': '{"versionName":"3.8.1"}',
    'version.json': '{"web":"3.8.1-web.57"}'
  }, ['', 'guide.html']);
  const live = new Map(f.data);
  live.set(SITE + '/release.json', Buffer.from('{"versionName":"3.8.0"}'));
  const provider = remote(live);
  const result = await prepare({docsDir: f.docsDir, commit: COMMIT, fetchImpl: provider.fetchImpl});
  assert.equal(result.changed.length, 1);
  assert.equal(result.changed[0].reason, 'dependency_changed');
  assert.deepEqual(result.changed[0].changedResources, [SITE + '/release.json']);
  assert(!result.changed[0].resources.some(item => item.url === SITE + '/version.json'));
  assert.deepEqual(result.unchanged, [SITE + '/guide.html']);
  assert(provider.calls.every(call => !call.url.includes('/api/')));
});

test('changed og:image, srcset and video poster are tracked; external dependencies are not fetched', async t => {
  const f = fixture(t, {
    'index.html': rootPage('<img src="small.webp" srcset="large.webp 1200w"><video poster="cover.webp"></video><script src="https://third.example/a.js"></script>', ''),
    'small.webp': 'same', 'large.webp': 'new', 'cover.webp': 'new-cover', 'og.webp': 'new-og'
  });
  f.write('index.html', page('', '<img src="small.webp" srcset="large.webp 1200w"><video poster="cover.webp"></video><script src="https://third.example/a.js"></script>', '<meta property="og:image" content="https://kapterka-pro.ru/og.webp">'));
  const live = new Map(f.data);
  live.set(SITE + '/large.webp', Buffer.from('old'));
  live.set(SITE + '/cover.webp', Buffer.from('old-cover'));
  live.set(SITE + '/og.webp', Buffer.from('old-og'));
  const provider = remote(live);
  const result = await prepare({docsDir: f.docsDir, commit: COMMIT, fetchImpl: provider.fetchImpl});
  assert.deepEqual(result.changed[0].changedResources, [SITE + '/cover.webp', SITE + '/large.webp', SITE + '/og.webp']);
  assert(provider.calls.every(call => call.url.startsWith(SITE + '/')));
});

test('a real predeployment 404 is new; unknown errors or redirects fail closed', async t => {
  const f = fixture(t);
  const notFound = remote(new Map(f.data), new Map([[SITE + '/', {status: 404}]]));
  const fresh = await prepare({docsDir: f.docsDir, commit: COMMIT, fetchImpl: notFound.fetchImpl});
  assert.equal(fresh.changed[0].reason, 'new_page');
  for (const value of [{status: 403}, {status: 500}, new TypeError('network'), {status: 200, body: rootPage('same'), finalUrl: 'https://other.example/'}]) {
    const provider = remote(f.data, new Map([[SITE + '/', value]]));
    const result = await prepare({docsDir: f.docsDir, commit: COMMIT, fetchImpl: provider.fetchImpl});
    assert.equal(result.changed.length, 0);
    assert.equal(result.skipped[0].reason, 'predeployment_page_unknown');
  }
});

test('unknown dependency fails closed even when the HTML changed', async t => {
  const f = fixture(t, {'index.html': rootPage('<img src="cover.webp">after'), 'cover.webp': 'new'});
  const live = new Map(f.data);
  live.set(SITE + '/', Buffer.from(rootPage('before')));
  const provider = remote(live, new Map([[SITE + '/cover.webp', {status: 500}]]));
  const result = await prepare({docsDir: f.docsDir, commit: COMMIT, fetchImpl: provider.fetchImpl});
  assert.equal(result.changed.length, 0);
  assert.equal(result.skipped[0].reason, 'predeployment_dependency_unknown');
});

test('noindex, noncanonical, outside-host and non-HTML URLs are excluded without GET', async t => {
  const f = fixture(t, {
    'private.html': page('private.html', 'private', '<meta name="robots" content="noindex,follow">'),
    'alias.html': page('other.html', 'alias')
  }, ['', 'private.html', 'alias.html', 'https://other.example/test.html', 'kapterka-pro.apk', 'index.html?secret=1']);
  const provider = remote(f.data);
  const result = await prepare({docsDir: f.docsDir, commit: COMMIT, fetchImpl: provider.fetchImpl});
  assert.equal(result.skipped.length, 5);
  assert.deepEqual(result.unchanged, [SITE + '/']);
  assert.deepEqual(provider.calls.map(call => call.url), [SITE + '/']);
});

test('missing successful deployment or wrong commit performs no request', async t => {
  const {f, plan} = await changedPlan(t);
  for (const extra of [{deploymentSuccess: false}, {commit: 'b'.repeat(40)}]) {
    const provider = remote(f.data);
    const result = await notify(options(plan, provider, extra));
    assert.equal(provider.calls.length, 0);
    assert.equal(result.journal.status, 'skipped');
  }
});

test('invalid/outside-host plan entries cannot be posted', async t => {
  const {f, plan} = await changedPlan(t);
  const provider = remote(f.data);
  const bad = {...plan, changed: [{...plan.changed[0], url: 'https://other.example/'}]};
  const result = await notify(options(bad, provider));
  assert.equal(provider.calls.length, 0);
  assert.equal(result.journal.skipped[0].reason, 'invalid_plan_entry');
});

test('ownership key must be exactly present on the host before any POST', async t => {
  const {f, plan} = await changedPlan(t);
  for (const value of [{status: 404}, {status: 200, body: 'wrong'}, {status: 200, body: KEY + '\n\n'}, {status: 200, body: KEY, finalUrl: 'https://other.example/key.txt'}]) {
    const provider = remote(f.data, new Map([[`${SITE}/${KEY}.txt`, value]]));
    const result = await notify(options(plan, provider));
    assert.equal(provider.posts().length, 0);
    assert(result.journal.warnings.some(item => item.includes('Ownership')));
  }
});

test('202 records receipt per fingerprint; unchanged rerun and restored cache do not repeat POST', async t => {
  const {f, plan} = await changedPlan(t);
  const provider = remote(f.data, new Map(), [202]);
  const first = await notify(options(plan, provider));
  assert.equal(first.journal.status, 'accepted');
  assert.equal(first.journal.indexingGuaranteed, false);
  assert.equal(first.state.receipts[SITE + '/'].status, 202);
  const payload = JSON.parse(provider.posts()[0].options.body);
  assert.deepEqual(payload, {host: 'kapterka-pro.ru', key: KEY, keyLocation: `${SITE}/${KEY}.txt`, urlList: [SITE + '/']});
  const secondProvider = remote(f.data);
  const second = await notify(options(plan, secondProvider, {state: JSON.parse(JSON.stringify(first.state))}));
  assert.equal(secondProvider.calls.length, 0);
  assert.equal(second.journal.status, 'no_changes_or_duplicates');
  const rerunPlan = await prepare({docsDir: f.docsDir, commit: COMMIT, fetchImpl: secondProvider.fetchImpl});
  assert.equal(rerunPlan.changed.length, 0);
});

test('previous result artifact restores accepted fingerprints after cache eviction; missing rerun receipts fail closed', async t => {
  const {f, plan} = await changedPlan(t);
  const first = await notify(options(plan, remote(f.data)));
  assert.equal(first.journal.acceptedReceipts.length, 1);
  const restoredProvider = remote(f.data);
  const restored = await notify(options(plan, restoredProvider, {state: {version: 1, receipts: {}}, previousJournals: [first.journal], runAttempt: 2}));
  assert.equal(restoredProvider.posts().length, 0);
  assert.equal(restored.journal.status, 'no_changes_or_duplicates');
  assert.equal(restored.state.receipts[SITE + '/'].fingerprint, plan.changed[0].fingerprint);
  const missingProvider = remote(f.data);
  const skipped = await notify(options(plan, missingProvider, {dedupAvailable: false}));
  assert.equal(missingProvider.calls.length, 0);
  assert(skipped.journal.warnings[0].includes('predecessor receipts'));
});

test('rerun requires immediately preceding same-run receipts, not older prefix cache or earlier journal', async t => {
  const {f, plan} = await changedPlan(t);
  const first = await notify(options(plan, remote(f.data)));
  const base = {runId: '123', runAttempt: 2, commit: COMMIT};
  assert.equal(dedupRecovered({...base, state: first.state, cacheMatchedKey: 'indexnow-receipts-v1-123-1'}), true);
  assert.equal(dedupRecovered({...base, state: first.state, cacheMatchedKey: 'indexnow-receipts-v1-122-1'}), false);
  assert.equal(dedupRecovered({...base, previousJournals: [first.journal]}), true);
  assert.equal(dedupRecovered({...base, runAttempt: 3, state: first.state, cacheMatchedKey: 'indexnow-receipts-v1-123-1', previousJournals: [first.journal]}), false);
  const oldCache = {...first.state, runId: '122', receipts: {}};
  const dedupAvailable = dedupRecovered({...base, state: oldCache, cacheMatchedKey: 'indexnow-receipts-v1-122-1'});
  const provider = remote(f.data);
  const skipped = await notify(options(plan, provider, {state: oldCache, runAttempt: 2, dedupAvailable}));
  assert.equal(provider.calls.length, 0);
  assert.equal(skipped.journal.status, 'skipped');
  // A fresh full rerun that sees no differences does not need prior submission history.
  const unchanged = await notify(options({...plan, changed: []}, provider, {dedupAvailable: false, runAttempt: 3}));
  assert.equal(unchanged.journal.status, 'no_changes_or_duplicates');
});

test('CLI with a stale restored cache on attempt2, or attempt1 journal on attempt3, cannot request or POST', async t => {
  const {f, plan} = await changedPlan(t);
  const here = path.dirname(fileURLToPath(import.meta.url));
  const shim = path.join(f.temporary, 'deny-network.mjs');
  fs.writeFileSync(shim, "import fs from 'node:fs'; globalThis.fetch = async () => {fs.appendFileSync('fetch-calls.txt', 'request\\n'); throw new Error('Network is disabled in this integration test');};");
  const planFile = path.join(f.temporary, 'plan.json');
  fs.writeFileSync(planFile, JSON.stringify(plan));
  const stateDir = path.join(f.temporary, '.indexnow-state');
  fs.mkdirSync(stateDir);
  const journalsDir = path.join(f.temporary, '.indexnow-prior-results');
  fs.mkdirSync(journalsDir);
  for (const attempt of [2, 3]) {
    const state = {version: 1, runId: '122', runAttempt: 1, receipts: {}};
    fs.writeFileSync(path.join(stateDir, 'receipts.json'), JSON.stringify(state));
    if (attempt === 3) fs.writeFileSync(path.join(journalsDir, 'result.json'), JSON.stringify({version: 1, origin: SITE, commit: COMMIT, runId: '123', runAttempt: 1, acceptedReceipts: []}));
    const child = spawnSync(process.execPath, ['--import', pathToFileURL(shim).href, path.join(here, 'indexnow.mjs'), 'notify', planFile], {
      cwd: f.temporary, encoding: 'utf8', timeout: 10000,
      env: {...process.env, GITHUB_ACTIONS: 'true', GITHUB_REF: 'refs/heads/main', GITHUB_SHA: COMMIT, GITHUB_RUN_ID: '123', GITHUB_RUN_ATTEMPT: String(attempt), INDEXNOW_DEPLOYMENT_SUCCESS: 'true', INDEXNOW_CACHE_MATCHED_KEY: 'indexnow-receipts-v1-122-1', GITHUB_STEP_SUMMARY: ''}
    });
    assert.equal(child.status, 0, child.stderr);
    assert(!fs.existsSync(path.join(f.temporary, 'fetch-calls.txt')));
    const journal = JSON.parse(fs.readFileSync(path.join(f.temporary, '.indexnow/result.json'), 'utf8'));
    assert.equal(journal.status, 'skipped');
    assert.equal(journal.runAttempt, attempt);
    assert(journal.warnings[0].includes('predecessor receipts'));
  }
});

test('lost history remains incomplete across later reruns; a skipped empty journal cannot authorize another POST', async t => {
  const {f, plan} = await changedPlan(t);
  const second = await notify(options(plan, remote(f.data), {dedupAvailable: false, runAttempt: 2, state: {version: 1, receipts: {}}}));
  assert.equal(second.state.receiptHistoryComplete, false);
  assert.equal(second.journal.receiptHistoryComplete, false);
  const recovered = dedupRecovered({runId: '123', runAttempt: 3, cacheMatchedKey: 'indexnow-receipts-v1-123-2', state: second.state, previousJournals: [second.journal], commit: COMMIT});
  assert.equal(recovered, false);
  const provider = remote(f.data);
  const third = await notify(options(plan, provider, {runAttempt: 3, state: second.state, previousJournals: [second.journal], dedupAvailable: recovered}));
  assert.equal(provider.calls.length, 0);
  assert.equal(third.journal.status, 'skipped');
  assert.equal(third.state.receiptHistoryComplete, false);
});

test('candidate mismatch/404 after deployment prevents notification and exhausts bounded checks', async t => {
  const {f, plan} = await changedPlan(t);
  for (const value of [{status: 200, body: rootPage('before')}, {status: 404}]) {
    const sleeps = [];
    const provider = remote(f.data, new Map([[SITE + '/', value]]));
    const result = await notify(options(plan, provider, {sleep: async ms => sleeps.push(ms)}));
    assert.equal(provider.posts().length, 0);
    assert.deepEqual(sleeps, [1000, 2000]);
    assert.equal(provider.calls.filter(call => call.url === SITE + '/').length, 3);
    assert.equal(result.journal.skipped[0].reason, 'candidate_not_confirmed_live');
  }
});

test('temporary CDN mismatch resolves before POST; live noindex never posts', async t => {
  const {f, plan} = await changedPlan(t);
  let gets = 0;
  const provider = remote(f.data, new Map([[SITE + '/', url => response(url, gets++ === 0 ? rootPage('before') : f.data.get(url))]]));
  const result = await notify(options(plan, provider));
  assert.equal(result.journal.status, 'accepted');
  assert.equal(gets, 2);
  const blocked = remote(f.data, new Map([[SITE + '/', {body: f.data.get(SITE + '/'), headers: {'X-Robots-Tag': 'noindex'}}]]));
  const skipped = await notify(options(plan, blocked));
  assert.equal(blocked.posts().length, 0);
  assert.equal(skipped.journal.skipped[0].reason, 'live_page_not_public');
});

test('standalone robots none excludes indexing but max-image-preview:none is only an image directive', async t => {
  const f = fixture(t, {
    'private.html': page('private.html', '', '<meta name="robots" content="none">')
  }, ['', 'private.html']);
  const prepared = await prepare({docsDir: f.docsDir, commit: COMMIT, fetchImpl: remote(f.data).fetchImpl});
  assert.equal(prepared.skipped[0].reason, 'noindex_or_noncanonical');
  const {f: changed, plan} = await changedPlan(t);
  const denied = remote(changed.data, new Map([[SITE + '/', {body: changed.data.get(SITE + '/'), headers: {'X-Robots-Tag': 'none'}}]]));
  const deniedResult = await notify(options(plan, denied));
  assert.equal(denied.posts().length, 0);
  assert.equal(deniedResult.journal.skipped[0].reason, 'live_page_not_public');
  const imageOnly = remote(changed.data, new Map([[SITE + '/', {body: changed.data.get(SITE + '/'), headers: {'X-Robots-Tag': 'max-image-preview: none'}}]]));
  const allowed = await notify(options(plan, imageOnly));
  assert.equal(allowed.journal.status, 'accepted');
});

test('deployment GET Retry-After is respected; a long server delay stops without early retry', async t => {
  const {f, plan} = await changedPlan(t);
  let gets = 0;
  const sleeps = [];
  const provider = remote(f.data, new Map([[SITE + '/', url => ++gets === 1 ? response(url, '', 429, {'Retry-After': '7'}) : response(url, f.data.get(url))]]));
  const result = await notify(options(plan, provider, {sleep: async ms => sleeps.push(ms)}));
  assert.equal(result.journal.status, 'accepted');
  assert.deepEqual(sleeps, [7000]);
  const long = remote(f.data, new Map([[SITE + '/', {status: 429, headers: {'Retry-After': '600'}}]]));
  const stopped = await notify(options(plan, long, {sleep: async () => assert.fail('Must not retry early')}));
  assert.equal(long.posts().length, 0);
  assert.equal(long.calls.filter(call => call.url === SITE + '/').length, 1);
  assert.equal(stopped.journal.skipped[0].reason, 'deployment_retry_after_exceeds_bound');
});

test('total read time limit fails closed without requests or deployment failure', async t => {
  const {f, plan} = await changedPlan(t);
  const provider = remote(f.data);
  const prepared = await prepare({docsDir: f.docsDir, commit: COMMIT, fetchImpl: provider.fetchImpl, totalTimeMs: 0});
  assert.equal(prepared.changed.length, 0);
  assert.equal(prepared.skipped[0].reason, 'predeployment_page_unknown');
  const skipped = await notify(options(plan, provider, {totalTimeMs: 0}));
  assert.equal(provider.calls.length, 0);
  assert.equal(skipped.journal.status, 'skipped');
});

test('permanent 4xx gets one attempt and no accepted receipt', async t => {
  const {f, plan} = await changedPlan(t);
  for (const status of [400, 403, 422]) {
    const provider = remote(f.data, new Map(), [status]);
    const result = await notify(options(plan, provider));
    assert.equal(provider.posts().length, 1);
    assert.equal(result.journal.status, 'not_accepted');
    assert.deepEqual(result.state.receipts, {});
    assert.equal(result.journal.attempts[0].status, status);
  }
});

test('only temporary API errors retry up to three attempts; every status and URL is journalled', async t => {
  const {f, plan} = await changedPlan(t);
  const sleeps = [];
  const provider = remote(f.data, new Map(), [new TypeError('network'), 503, 200]);
  const result = await notify(options(plan, provider, {sleep: async ms => sleeps.push(ms)}));
  assert.equal(provider.posts().length, 3);
  assert.deepEqual(sleeps, [1000, 2000]);
  assert.deepEqual(result.journal.attempts.map(item => item.status), [null, 503, 200]);
  assert(result.journal.attempts.every(item => item.at === clock().toISOString() && item.urls[0] === SITE + '/'));
  assert.equal(result.journal.status, 'accepted');
  const exhausted = remote(f.data, new Map(), [408]);
  const failure = await notify(options(plan, exhausted));
  assert.equal(exhausted.posts().length, 3);
  assert.equal(failure.journal.status, 'not_accepted');
  assert.deepEqual(failure.state.receipts, {});
});

test('Retry-After is respected and never shortened when it exceeds the ceiling', async t => {
  const {f, plan} = await changedPlan(t);
  const sleeps = [];
  const provider = remote(f.data, new Map(), [{status: 429, headers: {'Retry-After': '7'}}, 200]);
  const result = await notify(options(plan, provider, {sleep: async ms => sleeps.push(ms)}));
  assert.deepEqual(sleeps, [7000]);
  assert.equal(result.journal.status, 'accepted');
  const long = remote(f.data, new Map(), [{status: 429, headers: {'Retry-After': '600'}}, 200]);
  const stopped = await notify(options(plan, long, {sleep: async () => assert.fail('Must not retry early')}));
  assert.equal(long.posts().length, 1);
  assert.equal(stopped.journal.status, 'not_accepted');
  assert(stopped.journal.attempts[0].retryStopped);
  assert.equal(retryDelay('Wed, 14 Oct 2026 09:00:12 GMT', 1, clock().getTime()), 12000);
});

test('workflow has no push POST, no extra token permissions, and notification depends on successful publication', () => {
  const here = path.dirname(fileURLToPath(import.meta.url));
  const testsWorkflow = fs.readFileSync(path.join(here, '../workflows/indexnow.yml'), 'utf8');
  const publication = fs.readFileSync(path.join(here, '../workflows/site-pages.yml'), 'utf8');
  assert(!testsWorkflow.includes('indexnow.mjs notify'));
  assert(!testsWorkflow.includes('api.indexnow.org'));
  assert(publication.includes('needs: [verify, publish]'));
  assert(publication.includes("needs.publish.result == 'success'"));
  assert(publication.indexOf('indexnow.mjs prepare') < publication.indexOf('actions/upload-pages-artifact'));
  assert(publication.includes('ref: ${{ github.sha }}'));
  const notification = publication.slice(publication.indexOf('  indexnow:'), publication.indexOf('  performance:'));
  assert(!notification.includes('permissions:'));
  assert(notification.includes('actions/cache/restore@v4'));
  assert(notification.includes('actions/cache/save@v4'));
  assert(notification.includes('pattern: indexnow-result-${{ github.run_id }}-*'));
  assert(notification.includes('INDEXNOW_CACHE_MATCHED_KEY: ${{ steps.indexnow-cache.outputs.cache-matched-key }}'));
  assert(!notification.includes('INDEXNOW_CACHE_RESTORED'));
  assert(notification.includes('continue-on-error: true'));
  const planNames = [...publication.matchAll(/name: (indexnow-plan-\$\{\{ github.run_id \}\}[^\n]*)/g)].map(match => match[1]);
  assert.equal(planNames.length, 2);
  assert.equal(planNames[0], planNames[1]);
  assert(!planNames[0].includes('run_attempt'), 'Rerun failed publish job must still read the original plan');
});
