// Deployment-bound IndexNow notification; no application data, account tokens or new services.
// Protocol and ownership: https://www.indexnow.org/faq
import fs from 'node:fs';
import path from 'node:path';
import { createHash } from 'node:crypto';
import { pathToFileURL } from 'node:url';

export const SITE = 'https://kapterka-pro.ru';
export const KEY = '903fd952854fcb833f54ad87ef4b033b';
export const ENDPOINT = 'https://api.indexnow.org/indexnow';
const VERSION = 1;
const MAX_BYTES = 32 * 1024 * 1024;
const MAX_RESOURCES = 256;
const MAX_URLS = 10000;
const HASH = /^[a-f0-9]{64}$/;
const TEXT = /\.(?:html?|css|[cm]?js|svg|json|webmanifest|txt|vtt|srt)$/i;
const sha = bytes => createHash('sha256').update(bytes).digest('hex');
const timestamp = now => now().toISOString();
const wait = ms => new Promise(resolve => setTimeout(resolve, ms));

function decode(value) {
  return value.replace(/&(?:amp|quot|apos|lt|gt);|&#(?:x[\da-f]+|\d+);/gi, entity => {
    const names = {'&amp;': '&', '&quot;': '"', '&apos;': "'", '&lt;': '<', '&gt;': '>'};
    if (names[entity.toLowerCase()]) return names[entity.toLowerCase()];
    const number = entity.slice(2, -1);
    const point = number[0].toLowerCase() === 'x' ? parseInt(number.slice(1), 16) : parseInt(number, 10);
    return point >= 0 && point <= 0x10ffff ? String.fromCodePoint(point) : entity;
  });
}

function attrs(tag) {
  const result = {};
  for (const match of tag.matchAll(/([\w:-]+)\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s>]+))/g)) {
    result[match[1].toLowerCase()] = decode(match[2] ?? match[3] ?? match[4]);
  }
  return result;
}

function localUrl(ref, base, origin, pageOnly = false) {
  if (!ref || ref.startsWith('#')) return null;
  try {
    const url = new URL(decode(ref), base);
    if (url.origin !== origin || url.protocol !== 'https:' || url.username || url.password) return null;
    if (pageOnly && (url.search || url.hash || !(url.pathname.endsWith('/') || /\.html$/i.test(url.pathname)))) return null;
    url.hash = '';
    return url.href;
  } catch { return null; }
}

function textBytes(url, bytes) {
  const pathname = new URL(url).pathname;
  // Git may check out CRLF on Windows; line endings alone are not a public content change.
  return pathname.endsWith('/') || TEXT.test(pathname) ? Buffer.from(bytes.toString('utf8').replace(/\r\n/g, '\n')) : bytes;
}

function resourceHash(url, bytes) { return sha(textBytes(url, bytes)); }
function excludesIndexing(value) {
  return value.split(/[,;]/).some(part => {
    const directive = part.trim().toLowerCase();
    // 'none' is noindex,nofollow; a parameter such as max-image-preview:none is not.
    if (/^max-(?:image-preview|snippet|video-preview)\s*:/.test(directive)) return false;
    return /(?:^|\s)(?:noindex|none)(?:$|\s)/.test(directive.replace(/^[\w-]+\s*:\s*/, ''));
  });
}
function fingerprint(resources) {
  return sha(JSON.stringify(resources.map(item => [item.url, item.hash]).sort(([a], [b]) => a.localeCompare(b))));
}

function publicHtml(html, url) {
  const links = [...html.matchAll(/<link\b[^>]*>/gi)].map(match => attrs(match[0]));
  const canonicals = links.filter(item => (item.rel || '').toLowerCase().split(/\s+/).includes('canonical'));
  if (canonicals.length !== 1) return false;
  try { if (new URL(canonicals[0].href, url).href !== url) return false; } catch { return false; }
  for (const match of html.matchAll(/<meta\b[^>]*>/gi)) {
    const item = attrs(match[0]);
    if (/^(?:robots|googlebot|bingbot|yandex)$/i.test(item.name || '') && excludesIndexing(item.content || '')) return false;
    if (/^refresh$/i.test(item['http-equiv'] || '')) return false;
  }
  return true;
}

function dependencies(url, bytes, origin, releaseConsumer) {
  const pathname = new URL(url).pathname;
  const content = bytes.toString('utf8');
  const refs = [];
  if (pathname.endsWith('/') || /\.html?$/i.test(pathname)) {
    // Anchors and canonical/alternate links are not rendered dependencies.
    for (const match of content.matchAll(/<(?:script|img|video|audio|source|track|iframe|link|meta)\b[^>]*>/gi)) {
      const item = attrs(match[0]);
      const tag = /^<(\w+)/.exec(match[0])[1].toLowerCase();
      if (tag === 'link') {
        const rel = (item.rel || '').toLowerCase().split(/\s+/);
        if (rel.includes('stylesheet') || (rel.includes('preload') && /^(?:style|script|image|font)$/i.test(item.as || ''))) refs.push(item.href);
      } else if (tag === 'meta') {
        if (/^(?:og:image(?::url)?|twitter:image)$/i.test(item.property || item.name || '')) refs.push(item.content);
      } else {
        refs.push(item.src, item.poster);
        if (item.srcset) refs.push(...item.srcset.split(',').map(part => part.trim().split(/\s+/)[0]));
      }
    }
  } else if (/\.css$/i.test(pathname)) {
    const css = content.replace(/\/\*[\s\S]*?\*\//g, '');
    for (const match of css.matchAll(/url\(\s*(?:"([^"]+)"|'([^']+)'|([^\s)]+))\s*\)/gi)) refs.push(match[1] ?? match[2] ?? match[3]);
    for (const match of css.matchAll(/@import\s+(?:"([^"]+)"|'([^']+)')/gi)) refs.push(match[1] ?? match[2]);
  } else if (/\.[cm]?js$/i.test(pathname)) {
    // Literal local ES module imports only; no execution or application API calls.
    for (const match of content.matchAll(/\b(?:import|export)\s+(?:[^;'"\n]*?\bfrom\s*)?['"]([^'"]+)['"]|\bimport\(\s*['"]([^'"]+)['"]\s*\)/g)) {
      const ref = match[1] ?? match[2];
      if (/^(?:\.{1,2}\/|\/|https:\/\/)/.test(ref)) refs.push(ref);
    }
    // release-meta.js is shared by guides without any rendered release fields. Only
    // pages that actually consume its public text/link fields depend on this manifest.
    // version.json only prompts auto-reload; it is not rendered public content.
    if (releaseConsumer) for (const match of content.matchAll(/\bfetch\(\s*['"]((?:\.\/)?release\.json)(?:\?[^'"]*)?['"]/g)) refs.push(match[1]);
  }
  return [...new Set(refs.map(ref => localUrl(ref, url, origin)).filter(Boolean))];
}

function localReader(docsDir) {
  const root = fs.realpathSync(docsDir);
  const memo = new Map();
  return url => {
    if (memo.has(url)) return memo.get(url);
    const pathname = decodeURIComponent(new URL(url).pathname);
    if (pathname.includes('\\') || pathname.includes('\0')) throw new Error('Invalid local path');
    const file = path.resolve(root, '.' + pathname, pathname.endsWith('/') ? 'index.html' : '');
    const real = fs.realpathSync(file);
    const relative = path.relative(root, real);
    if (relative.startsWith('..') || path.isAbsolute(relative)) throw new Error('Resource outside docs');
    if (fs.statSync(real).size > MAX_BYTES) throw new Error('Local resource exceeds limit');
    const bytes = fs.readFileSync(real);
    memo.set(url, bytes);
    return bytes;
  };
}

function candidateResources(url, read, origin) {
  const resources = new Map();
  const releaseConsumer = /<[^>]+\sdata-release-[\w-]+(?:\s|=|>)/i.test(read(url).toString('utf8'));
  function visit(resource) {
    if (resources.has(resource)) return;
    if (resources.size >= MAX_RESOURCES) throw new Error('Dependency limit exceeded');
    const bytes = read(resource);
    resources.set(resource, {url: resource, hash: resourceHash(resource, bytes)});
    for (const child of dependencies(resource, bytes, origin, releaseConsumer)) visit(child);
  }
  visit(url);
  return [...resources.values()].sort((a, b) => a.url.localeCompare(b.url));
}

function limiter(limit = 4) {
  let active = 0;
  const queue = [];
  return async task => {
    if (active >= limit) await new Promise(resolve => queue.push(resolve));
    active++;
    try { return await task(); }
    finally { active--; queue.shift()?.(); }
  };
}

function remoteReader(fetchImpl, origin, timeoutMs, memoize = true, deadline = Infinity) {
  const memo = new Map();
  const run = limiter();
  return url => {
    if (memoize && memo.has(url)) return memo.get(url);
    const request = run(async () => {
      const remaining = deadline - Date.now();
      if (remaining <= 0) return {status: null, error: 'total_time_limit'};
      try {
        const response = await fetchImpl(url, {
          method: 'GET', redirect: 'error', cache: 'no-store',
          headers: {'Cache-Control': 'no-cache', 'User-Agent': 'KapterkaPro-IndexNow/1.0'},
          signal: AbortSignal.timeout(Math.min(timeoutMs, remaining))
        });
        if (response.url && (new URL(response.url).origin !== origin || new URL(response.url).href !== url)) return {status: response.status, error: 'redirect'};
        if (response.status !== 200) return {status: response.status, error: response.status === 404 ? null : 'http', retryAfter: response.headers.get('retry-after')};
        if (Number(response.headers.get('content-length')) > MAX_BYTES) return {status: 200, error: 'body_limit'};
        const bytes = Buffer.from(await response.arrayBuffer());
        if (bytes.length > MAX_BYTES) return {status: 200, error: 'body_limit'};
        return {status: 200, bytes, hash: resourceHash(url, bytes), noindex: excludesIndexing(response.headers.get('x-robots-tag') || '')};
      } catch (error) { return {status: null, error: error.name === 'TimeoutError' || error.name === 'AbortError' ? 'timeout' : 'network_or_redirect'}; }
    });
    if (memoize) memo.set(url, request);
    return request;
  };
}

export async function prepare({docsDir = 'docs', commit = '', fetchImpl = fetch, now = () => new Date(), origin = SITE, timeoutMs = 10000, totalTimeMs = 180000} = {}) {
  const plan = {version: VERSION, origin, commit, preparedAt: timestamp(now), changed: [], unchanged: [], skipped: [], checks: []};
  const read = localReader(docsDir);
  const sitemap = fs.readFileSync(path.join(docsDir, 'sitemap.xml'), 'utf8');
  const raw = [...sitemap.matchAll(/<loc>\s*([^<]+?)\s*<\/loc>/g)].map(match => decode(match[1]));
  if (raw.length > MAX_URLS) throw new Error('Sitemap URL limit exceeded');
  const urls = new Set();
  for (const ref of raw) {
    const url = localUrl(ref, origin, origin, true);
    if (url) urls.add(url); else plan.skipped.push({url: ref, reason: 'outside_host_or_not_public_html'});
  }
  const remote = remoteReader(fetchImpl, origin, timeoutMs, true, Date.now() + totalTimeMs);
  // Bound page concurrency as well as memoized resource requests.
  const run = limiter();
  await Promise.all([...urls].map(url => run(async () => {
    let resources;
    try {
      if (!publicHtml(read(url).toString('utf8'), url)) {
        plan.skipped.push({url, reason: 'noindex_or_noncanonical'}); return;
      }
      resources = candidateResources(url, read, origin);
    } catch { plan.skipped.push({url, reason: 'local_resource_unavailable_or_invalid'}); return; }
    const page = await remote(url);
    plan.checks.push({url, status: page.status, error: page.error || null});
    if (page.error || ![200, 404].includes(page.status)) {
      plan.skipped.push({url, reason: 'predeployment_page_unknown'}); return;
    }
    const candidate = {url, fingerprint: fingerprint(resources), resources};
    if (page.status === 404) {
      plan.changed.push({...candidate, reason: 'new_page'}); return;
    }
    const live = await Promise.all(resources.map(async item => ({...item, live: await remote(item.url)})));
    for (const item of live.filter(item => item.url !== url)) plan.checks.push({url: item.url, status: item.live.status, error: item.live.error || null});
    if (live.some(item => item.live.error || ![200, 404].includes(item.live.status))) {
      plan.skipped.push({url, reason: 'predeployment_dependency_unknown'}); return;
    }
    const changes = live.filter(item => item.live.status === 404 || item.hash !== item.live.hash).map(item => item.url);
    if (changes.length) plan.changed.push({...candidate, reason: changes.includes(url) ? 'page_changed' : 'dependency_changed', changedResources: changes});
    else plan.unchanged.push(url);
  })));
  plan.changed.sort((a, b) => a.url.localeCompare(b.url));
  plan.unchanged.sort();
  plan.skipped.sort((a, b) => a.url.localeCompare(b.url));
  return plan;
}

function validEntry(entry, origin) {
  if (!entry || localUrl(entry.url, origin, origin, true) !== entry.url || !HASH.test(entry.fingerprint) || !Array.isArray(entry.resources) || !entry.resources.length || entry.resources.length > MAX_RESOURCES) return false;
  const unique = new Set();
  for (const item of entry.resources) {
    if (!item || localUrl(item.url, entry.url, origin) !== item.url || !HASH.test(item.hash) || unique.has(item.url)) return false;
    unique.add(item.url);
  }
  return unique.has(entry.url) && fingerprint(entry.resources) === entry.fingerprint;
}

export function retryDelay(value, attempt, nowMs, ceilingMs = 30000) {
  let delay;
  if (value !== null && value !== undefined && value !== '') {
    if (/^\d+$/.test(value.trim())) delay = Number(value.trim()) * 1000;
    else {
      const date = Date.parse(value);
      if (Number.isFinite(date)) delay = Math.max(0, date - nowMs);
    }
  }
  delay ??= 1000 * 2 ** (attempt - 1);
  // Do not shorten a server's Retry-After to the local ceiling: stop instead.
  return delay > ceilingMs || !Number.isFinite(delay) ? null : delay;
}

export function dedupRecovered({runId, runAttempt, cacheMatchedKey = '', state, previousJournals = [], origin = SITE, commit = ''} = {}) {
  if (!/^\d+$/.test(runId || '') || !Number.isInteger(runAttempt) || runAttempt < 1) return false;
  if (runAttempt === 1) return true;
  const predecessor = runAttempt - 1;
  const exactPredecessorCache = cacheMatchedKey === `indexnow-receipts-v1-${runId}-${predecessor}` && state?.version === VERSION && state.runId === runId && state.runAttempt === predecessor && state.receiptHistoryComplete === true && state.receipts && typeof state.receipts === 'object' && !Array.isArray(state.receipts);
  const exactPredecessorResult = previousJournals.some(item => item?.version === VERSION && item.origin === origin && item.commit === commit && item.runId === runId && item.runAttempt === predecessor && item.receiptHistoryComplete === true && Array.isArray(item.acceptedReceipts));
  return !!(exactPredecessorCache || exactPredecessorResult);
}

export async function notify({plan, state = {version: VERSION, receipts: {}}, previousJournals = [], dedupAvailable = true, runId = '', runAttempt = 1, commit = '', deploymentSuccess = false, fetchImpl = fetch, now = () => new Date(), sleep = wait, origin = SITE, timeoutMs = 10000, maxAttempts = 3, retryCeilingMs = 30000, totalTimeMs = 240000} = {}) {
  const deadline = Date.now() + totalTimeMs;
  const journal = {version: VERSION, startedAt: timestamp(now), runId, runAttempt, receiptHistoryComplete: dedupAvailable, commit, origin, status: 'skipped', submittedUrls: [], acceptedUrls: [], acceptedReceipts: [], skipped: [], checks: [], attempts: [], warnings: [], indexingGuaranteed: false};
  const receipts = state?.version === VERSION && state.receipts && typeof state.receipts === 'object' && !Array.isArray(state.receipts) ? {...state.receipts} : {};
  for (const previous of previousJournals) {
    if (previous?.version !== VERSION || previous.origin !== origin || previous.commit !== commit || previous.runId !== runId || previous.runAttempt >= runAttempt || !Array.isArray(previous.acceptedReceipts)) continue;
    for (const receipt of previous.acceptedReceipts) {
      if (localUrl(receipt?.url, origin, origin, true) !== receipt?.url || !HASH.test(receipt?.fingerprint) || ![200, 202].includes(receipt?.status)) continue;
      const {url, ...record} = receipt;
      if (!receipts[url]?.acceptedAt || record.acceptedAt > receipts[url].acceptedAt) receipts[url] = record;
    }
  }
  const result = () => ({journal: {...journal, finishedAt: timestamp(now)}, state: {version: VERSION, runId, runAttempt, receiptHistoryComplete: dedupAvailable, receipts}});
  if (!deploymentSuccess) { journal.warnings.push('No successful deployment; no POST'); return result(); }
  if (!plan || plan.version !== VERSION || plan.origin !== origin || plan.commit !== commit || !Array.isArray(plan.changed) || plan.changed.length > MAX_URLS) {
    journal.warnings.push('Missing, invalid or different-commit plan; no POST'); return result();
  }
  const entries = [];
  const seen = new Set();
  for (const entry of plan.changed) {
    if (!validEntry(entry, origin)) { journal.skipped.push({url: entry?.url || null, reason: 'invalid_plan_entry'}); continue; }
    if (seen.has(entry.url)) continue;
    seen.add(entry.url);
    if (receipts[entry.url]?.fingerprint === entry.fingerprint && [200, 202].includes(receipts[entry.url]?.status)) {
      journal.skipped.push({url: entry.url, reason: 'accepted_fingerprint'}); continue;
    }
    entries.push(entry);
  }
  if (!entries.length) { journal.status = 'no_changes_or_duplicates'; return result(); }
  if (!dedupAvailable) { journal.warnings.push('Latest predecessor receipts were not restored for this rerun; skip to prevent duplicate POST'); return result(); }
  // Existing public UTF-8 key file is the official domain ownership mechanism.
  const keyUrl = `${origin}/${KEY}.txt`;
  const keyCheck = await remoteReader(fetchImpl, origin, timeoutMs, true, deadline)(keyUrl);
  journal.checks.push({stage: 'ownership', url: keyUrl, status: keyCheck.status, error: keyCheck.error || null});
  if (keyCheck.status !== 200 || keyCheck.error || keyCheck.bytes.toString('utf8').replace(/\r?\n$/, '') !== KEY) {
    journal.warnings.push('Ownership key not exactly available; no POST'); return result();
  }
  const verified = [];
  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    const remote = remoteReader(fetchImpl, origin, timeoutMs, true, deadline);
    const pending = entries.filter(entry => !verified.includes(entry));
    let temporary = false;
    let nextDelay = 1000 * 2 ** (attempt - 1);
    for (const entry of pending) {
      const live = await Promise.all(entry.resources.map(async item => ({...item, live: await remote(item.url)})));
      for (const item of live) journal.checks.push({stage: 'deployment', attempt, page: entry.url, url: item.url, status: item.live.status, error: item.live.error || null});
      const page = live.find(item => item.url === entry.url).live;
      if (!page.error && page.status === 200 && (page.noindex || !publicHtml(page.bytes.toString('utf8'), entry.url))) {
        journal.skipped.push({url: entry.url, reason: 'live_page_not_public'});
        entries.splice(entries.indexOf(entry), 1);
        continue;
      }
      if (live.every(item => !item.live.error && item.live.status === 200 && item.live.hash === item.hash)) {
        verified.push(entry); continue;
      }
      // A CDN propagation mismatch or 404 can resolve. Permanent denial is not retried.
      const permanent = live.some(item => item.live.status && ![200, 404, 408, 429].includes(item.live.status) && item.live.status < 500);
      if (permanent) {
        journal.skipped.push({url: entry.url, reason: 'deployment_permanent_http_error'});
        entries.splice(entries.indexOf(entry), 1);
      } else {
        const delays = live.filter(item => item.live.status !== 200 || item.live.hash !== item.hash)
          .map(item => retryDelay(item.live.retryAfter, attempt, now().getTime(), retryCeilingMs));
        if (delays.some(delay => delay === null)) {
          journal.skipped.push({url: entry.url, reason: 'deployment_retry_after_exceeds_bound'});
          entries.splice(entries.indexOf(entry), 1);
        } else {
          nextDelay = Math.max(nextDelay, ...delays);
          temporary = true;
        }
      }
    }
    if (!temporary || attempt === maxAttempts) break;
    if (Date.now() + nextDelay >= deadline) break;
    await sleep(nextDelay);
  }
  for (const entry of entries.filter(entry => !verified.includes(entry))) journal.skipped.push({url: entry.url, reason: 'candidate_not_confirmed_live'});
  if (!verified.length) { journal.warnings.push('No changed URL confirmed live; no POST'); return result(); }
  const urls = verified.map(entry => entry.url);
  const payload = {host: new URL(origin).hostname, key: KEY, keyLocation: keyUrl, urlList: urls};
  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    const remaining = deadline - Date.now();
    if (remaining <= 0) { journal.warnings.push('Total notification time limit reached'); break; }
    let status = null;
    let error = null;
    let after = null;
    try {
      journal.submittedUrls = [...urls];
      const response = await fetchImpl(ENDPOINT, {
        method: 'POST', redirect: 'error', cache: 'no-store',
        headers: {'Content-Type': 'application/json; charset=utf-8', 'User-Agent': 'KapterkaPro-IndexNow/1.0'},
        body: JSON.stringify(payload), signal: AbortSignal.timeout(Math.min(timeoutMs, remaining))
      });
      status = response.status;
      if (response.url && response.url !== ENDPOINT) { error = 'redirect'; }
      after = response.headers.get('retry-after');
      // No untrusted response body is stored or interpreted.
      await response.body?.cancel();
    } catch (failure) { error = failure.name === 'TimeoutError' || failure.name === 'AbortError' ? 'timeout' : 'network_or_redirect'; }
    const event = {at: timestamp(now), attempt, method: 'POST', url: ENDPOINT, urls: [...journal.submittedUrls], status, error};
    journal.attempts.push(event);
    if (!error && [200, 202].includes(status)) {
      journal.status = 'accepted';
      journal.acceptedUrls = [...journal.submittedUrls];
      for (const entry of verified) {
        const receipt = {fingerprint: entry.fingerprint, status, acceptedAt: event.at, commit};
        receipts[entry.url] = receipt;
        journal.acceptedReceipts.push({url: entry.url, ...receipt});
      }
      journal.warnings.push('HTTP acceptance confirms receipt only, not indexing or traffic');
      return result();
    }
    const transient = error || status === 408 || status === 429 || status >= 500;
    if (!transient || attempt === maxAttempts) break;
    const delay = retryDelay(after, attempt, now().getTime(), retryCeilingMs);
    if (delay === null) { event.retryStopped = 'Retry-After exceeds bounded wait; no early retry'; break; }
    if (Date.now() + delay >= deadline) { event.retryStopped = 'Total notification time limit; no early retry'; break; }
    event.retryDelayMs = delay;
    await sleep(delay);
  }
  journal.status = 'not_accepted';
  journal.warnings.push('Notification failed; the successful site deployment is unaffected');
  return result();
}

function writeJson(file, value) {
  fs.mkdirSync(path.dirname(file), {recursive: true});
  const temporary = file + '.tmp';
  fs.writeFileSync(temporary, JSON.stringify(value, null, 2) + '\n');
  fs.renameSync(temporary, file);
}

function readPreviousJournals(directory) {
  if (!fs.existsSync(directory)) return [];
  const records = [];
  for (const entry of fs.readdirSync(directory, {withFileTypes: true})) {
    const file = path.join(directory, entry.name);
    if (entry.isDirectory()) records.push(...readPreviousJournals(file));
    else if (entry.isFile() && entry.name === 'result.json') {
      try {
        if (fs.statSync(file).size <= MAX_BYTES) records.push(JSON.parse(fs.readFileSync(file, 'utf8')));
      } catch { /* A corrupt journal never permits a submission. */ }
    }
  }
  return records;
}

async function cli() {
  const mode = process.argv[2];
  const planFile = process.argv[3] || '.indexnow/plan.json';
  const stateFile = '.indexnow-state/receipts.json';
  const commit = process.env.GITHUB_SHA || '';
  const journalFile = '.indexnow/result.json';
  try {
    if (mode === 'prepare') {
      const plan = await prepare({commit});
      writeJson(planFile, plan);
      console.log(`IndexNow plan: ${plan.changed.length} changed, ${plan.unchanged.length} unchanged, ${plan.skipped.length} skipped. No notification sent.`);
      for (const item of plan.skipped) console.log(`::warning::IndexNow skipped ${item.url}: ${item.reason}`);
    } else if (mode === 'notify') {
      let state;
      try { state = JSON.parse(fs.readFileSync(stateFile, 'utf8')); } catch { state = undefined; }
      const plan = JSON.parse(fs.readFileSync(planFile, 'utf8'));
      const deploymentSuccess = process.env.INDEXNOW_DEPLOYMENT_SUCCESS === 'true' && process.env.GITHUB_ACTIONS === 'true' && process.env.GITHUB_REF === 'refs/heads/main';
      const previousJournals = readPreviousJournals('.indexnow-prior-results');
      const runId = process.env.GITHUB_RUN_ID || '';
      const runAttempt = Number(process.env.GITHUB_RUN_ATTEMPT || '0');
      const dedupAvailable = dedupRecovered({runId, runAttempt, cacheMatchedKey: process.env.INDEXNOW_CACHE_MATCHED_KEY || '', state, previousJournals, commit});
      const {journal, state: updated} = await notify({plan, state, previousJournals, dedupAvailable, runId, runAttempt, commit, deploymentSuccess});
      writeJson(stateFile, updated);
      writeJson(journalFile, journal);
      console.log(`IndexNow: ${journal.status}; ${journal.acceptedUrls.length} URLs accepted for processing. Indexing and traffic are not guaranteed.`);
      for (const warning of journal.warnings) console.log(`::warning::${warning}`);
      if (process.env.GITHUB_STEP_SUMMARY) fs.appendFileSync(process.env.GITHUB_STEP_SUMMARY,
        `### IndexNow\n\nStatus: ${journal.status}. Accepted for processing: ${journal.acceptedUrls.length}; skipped: ${journal.skipped.length}.\n\nAcceptance does not guarantee indexing or traffic. UTC statuses, URLs and attempts are saved in the indexnow-result artifact.\n`);
    } else throw new Error('Expected prepare or notify');
  } catch (error) {
    // This is optional promotion plumbing: never fail a checked site publication.
    console.log(`::warning::IndexNow ${mode || 'command'} skipped: ${error.name}. Site publication is unaffected.`);
    writeJson(journalFile, {version: VERSION, status: 'internal_error', at: new Date().toISOString(), command: mode || null, error: error.name, indexingGuaranteed: false});
    if (mode === 'prepare') writeJson(planFile, {version: VERSION, origin: SITE, commit, preparedAt: new Date().toISOString(), changed: [], unchanged: [], skipped: [{url: null, reason: 'preparation_failed'}], checks: []});
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(path.resolve(process.argv[1])).href) await cli();
