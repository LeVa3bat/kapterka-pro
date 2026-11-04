// Real site JavaScript in Chromium. Every nonlocal request is intercepted;
// these checks cannot create a payment, send a message or use a real license.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const http = require('node:http');
const path = require('node:path');
const { pathToFileURL } = require('node:url');
const { chromium } = require('playwright');

const root = path.resolve(__dirname, '../..');
const docs = path.join(root, 'docs');
const api = 'https://kapterka-api.alex-666-881.workers.dev';
const paymentId = '12345678-1234-1234-1234-123456789012';
const confirmationUrl = 'https://yoomoney.ru/checkout/payments/v2/contract?orderId=mock-order';
const requestStorage = 'kapterka_payment_requests_v1';
const expiresAt = Date.now() + 30 * 86400000;
const accountA = 'account-a@example.invalid';
const accountB = 'account-b@example.invalid';
const token = 'ab'.repeat(32);
const idempotenceKey = 'cd'.repeat(32);
const scopes = new Set();

function savedOrder(account = '') {
  return { account, email: account || 'buyer@example.invalid', callsign: 'Тест', webClaimToken: token, idempotenceKey, createdAt: Date.now(), paymentId, confirmationUrl };
}

(async () => {
  const { computeKeyChecksum } = await import(pathToFileURL(path.join(root, 'server/cloudflare/worker.mjs')));
  const key = `KAPT-01AF-B019-${computeKeyChecksum('01AF', 'B019')}`;
  const paid = { ok: true, paid: true, status: 'succeeded', license_key: key, expires_at: expiresAt };
  const created = { ok: true, payment_id: paymentId, confirmation_url: confirmationUrl };
  const pending = { ok: true, paid: false, status: 'pending' };
  const status = { ok: true, active: true, status: 'ACTIVE', expires_at: expiresAt, activated_at: Date.now() };
  const server = http.createServer((req, res) => {
    const relative = decodeURIComponent(new URL(req.url, 'http://localhost').pathname).replace(/^\/+/, '') || 'index.html';
    const file = path.resolve(docs, relative);
    if (!file.startsWith(docs + path.sep) || !fs.existsSync(file) || !fs.statSync(file).isFile()) { res.writeHead(404).end(); return; }
    const types = { '.html': 'text/html', '.js': 'text/javascript', '.css': 'text/css', '.json': 'application/json', '.svg': 'image/svg+xml' };
    res.writeHead(200, { 'Content-Type': types[path.extname(file)] || 'application/octet-stream' });
    fs.createReadStream(file).pipe(res);
  });
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  const base = `http://127.0.0.1:${server.address().port}`;
  const browser = await chromium.launch({ headless: true, executablePath: process.env.CHROME_BIN || (process.platform === 'win32' ? 'C:/Program Files/Google/Chrome/Application/chrome.exe' : '/usr/bin/google-chrome') });
  let passed = 0;
  async function fixture(handler, seed = {}, suffix = '', clock = false) {
    const context = await browser.newContext();
    scopes.add(context);
    const calls = [];
    const external = [];
    const errors = [];
    await context.route('**/*', async route => {
      const request = route.request();
      const url = new URL(request.url());
      if (url.origin === base) return route.continue();
      external.push(url.href);
      if (url.origin === api) {
        const call = { action: url.searchParams.get('action'), payload: request.postDataJSON(), method: request.method(), url: url.href };
        calls.push(call);
        const response = await handler(call, calls);
        if (response === 'abort') return route.abort('failed');
        return route.fulfill({ status: Number.isInteger(response?.status) ? response.status : 200, contentType: 'application/json', body: JSON.stringify(response?.json || response) });
      }
      // No request outside the local server ever reaches the network.
      return route.fulfill({ status: 200, contentType: 'text/plain', body: '' });
    });
    await context.addInitScript(({ seed }) => {
      if (!localStorage.getItem('__payment_test_seeded')) {
        for (const [key, value] of Object.entries(seed)) localStorage.setItem(key, typeof value === 'string' ? value : JSON.stringify(value));
        localStorage.setItem('__payment_test_seeded', '1');
      }
      window.testPaymentRedirects = [];
      window.testPaymentPopups = [];
      window.open = () => {
        const popup = { closed: false, close() { this.closed = true; }, location: { replace(url) {
          window.testPaymentRedirects.push({ url, order: JSON.parse(localStorage.getItem('kapterka_payment_requests_v1') || '{}') });
        } } };
        window.testPaymentPopups.push(popup);
        return popup;
      };
    }, { seed });
    const page = await context.newPage();
    page.on('pageerror', error => errors.push(error.message));
    if (clock) await page.clock.install();
    await page.goto(base + '/' + suffix);
    await page.waitForFunction(() => typeof window.processYooKassaPayment === 'function');
    return { page, calls, external, errors, close: async () => { assert.deepEqual(errors, [], 'no uncaught browser errors'); await context.close(); scopes.delete(context); } };
  }
  async function fillAndCreate(page) {
    await page.evaluate(() => {
      document.getElementById('payEmailInput').value = 'buyer@example.invalid';
      document.getElementById('payCallsignInput').value = 'Тест';
      return window.processYooKassaPayment();
    });
  }
  async function activeKey(page) { return page.evaluate(() => localStorage.getItem('kapterka_active_key')); }
  async function map(page) { return page.evaluate(key => JSON.parse(localStorage.getItem(key) || '{}'), requestStorage); }
  async function check(name, run) { await run(); passed++; console.log('PASS: ' + name); }
  try {
    await check('create uses POST, persists claim/order before redirect and keeps contact API separate', async () => {
      const f = await fixture(() => created);
      await fillAndCreate(f.page);
      assert.equal(f.calls.length, 1);
      const call = f.calls[0];
      assert.equal(call.action, 'create'); assert.equal(call.method, 'POST');
      assert.match(call.payload.web_claim_token, /^[a-f0-9]{64}$/);
      assert.match(call.payload.idempotence_key, /^[a-f0-9]{64}$/);
      assert.equal(call.payload.fighter_id, undefined);
      assert.equal(call.payload.return_url, 'https://kapterka-pro.ru/?payment=check#tabPayment');
      const redirected = await f.page.evaluate(() => window.testPaymentRedirects);
      assert.equal(redirected[0].url, confirmationUrl);
      assert.equal(redirected[0].order[''].paymentId, paymentId);
      assert.equal(redirected[0].order[''].webClaimToken, call.payload.web_claim_token);
      await f.page.evaluate(() => sendTelegramNotification('mocked read-only test'));
      assert.ok(f.external.some(url => url.startsWith('https://script.google.com/macros/s/')));
      await f.close();
    });

    await check('invalid email and forged success URL cannot create or activate anything', async () => {
      const f = await fixture(() => { throw new Error('No payment API expected'); }, {}, '?payment=success&payment_id=forged');
      await f.page.evaluate(() => { document.getElementById('payEmailInput').value = 'bad-email'; return processYooKassaPayment(); });
      await f.page.evaluate(() => claimPaidLicenseKey());
      assert.equal(f.calls.length, 0); assert.equal(await activeKey(f.page), null);
      assert.equal((await f.page.evaluate(() => window.testPaymentRedirects)).length, 0);
      await f.close();
    });

    await check('duplicate clicks create only one in-flight order', async () => {
      let release; const response = new Promise(resolve => { release = resolve; });
      const f = await fixture(() => response);
      await f.page.evaluate(() => {
        document.getElementById('payEmailInput').value = 'buyer@example.invalid';
        window.firstCreate = processYooKassaPayment();
        processYooKassaPayment();
      });
      await f.page.waitForFunction(() => document.getElementById('btnPayYooKassaMain').disabled);
      assert.equal(f.calls.length, 1);
      release(created); await f.page.evaluate(() => window.firstCreate);
      await f.close();
    });

    await check('network failure retains claim/idempotence across reload and permits safe retry', async () => {
      let fail = true;
      const f = await fixture(() => fail ? 'abort' : created);
      await fillAndCreate(f.page);
      const failed = f.calls[0].payload;
      assert.equal((await f.page.evaluate(() => window.testPaymentRedirects)).length, 0);
      assert.equal(await activeKey(f.page), null);
      assert.equal(await f.page.locator('#btnPayYooKassaMain').isDisabled(), false);
      await f.page.reload(); fail = false;
      await fillAndCreate(f.page);
      assert.equal(f.calls[1].payload.web_claim_token, failed.web_claim_token);
      assert.equal(f.calls[1].payload.idempotence_key, failed.idempotence_key);
      await f.close();
    });

    await check('checkout accepts only HTTPS YooKassa/YooMoney hosts and never falls back to a shared link', async () => {
      const f = await fixture(() => ({ ...created, confirmation_url: 'https://yoomoney.ru.evil.invalid/pay' }));
      await fillAndCreate(f.page);
      assert.equal((await f.page.evaluate(() => window.testPaymentRedirects)).length, 0);
      assert.equal((await map(f.page))[''].paymentId, undefined);
      for (const bad of ['javascript:alert(1)', 'http://yoomoney.ru/pay', 'https://user:pass@yoomoney.ru/pay', 'https://yoomoney.ru:8443/pay', 'https://evil.invalid/pay']) {
        assert.equal(await f.page.evaluate(value => safePaymentConfirmationUrl(value), bad), '');
      }
      assert.equal(await f.page.evaluate(() => safePaymentConfirmationUrl('https://checkout.yookassa.ru/pay')), 'https://checkout.yookassa.ru/pay');
      await f.close();
    });

    await check('polling survives pending/network error, then applies only the confirmed server key and expiry', async () => {
      let checks = 0;
      const f = await fixture(call => call.action === 'create' ? created : (++checks === 1 ? pending : checks === 2 ? 'abort' : paid), {}, '', true);
      await fillAndCreate(f.page);
      await f.page.clock.runFor(3501);
      await f.page.waitForFunction(() => !document.getElementById('btnManualVerify').disabled);
      assert.equal(await activeKey(f.page), null);
      await f.page.clock.runFor(5001);
      await f.page.waitForFunction(() => !document.getElementById('btnManualVerify').disabled);
      assert.equal(await activeKey(f.page), null);
      await f.page.clock.runFor(15001);
      await f.page.waitForFunction(() => !!localStorage.getItem('kapterka_active_key'));
      assert.equal(await activeKey(f.page), key);
      const meta = await f.page.evaluate(() => JSON.parse(localStorage.getItem('kapterka_license_meta_v2')));
      assert.equal(meta[key].expiresAt, expiresAt);
      assert.equal(f.calls.filter(call => call.action === 'check').length, 3);
      assert.ok(f.calls.filter(call => call.action === 'check').every(call => call.payload.web_claim_token === f.calls[0].payload.web_claim_token && !call.payload.fighter_id));
      assert.equal(Object.keys(await map(f.page)).length, 0);
      await f.close();
    });

    await check('saved return resumes check with its original claim, never recreates a payment', async () => {
      const f = await fixture(() => pending, { [requestStorage]: { '': savedOrder() } }, '?payment=check&payment_id=attacker', true);
      await f.page.clock.runFor(1);
      await f.page.waitForFunction(() => !document.getElementById('btnManualVerify').disabled);
      assert.equal(f.calls.length, 1); assert.equal(f.calls[0].action, 'check');
      assert.equal(f.calls[0].payload.payment_id, paymentId); assert.equal(f.calls[0].payload.web_claim_token, token);
      assert.equal(await activeKey(f.page), null);
      await f.close();
    });

    await check('malformed paid result and an expired license never activate; manual retry remains usable', async () => {
      let response = { ...paid, license_key: 'KAPT-FAKE-FAKE-FAKE' };
      const f = await fixture(() => response, { [requestStorage]: { '': savedOrder() } }, '', true);
      await f.page.evaluate(() => claimPaidLicenseKey());
      assert.equal(await activeKey(f.page), null); assert.equal(await f.page.locator('#btnManualVerify').isDisabled(), false);
      response = { ...paid, expires_at: Date.now() - 1000 };
      await f.page.evaluate(() => claimPaidLicenseKey()); assert.equal(await activeKey(f.page), null);
      response = paid;
      await f.page.evaluate(() => claimPaidLicenseKey()); assert.equal(await activeKey(f.page), key);
      await f.close();
    });

    await check('cancellation clears only its order and a new purchase gets a fresh identity', async () => {
      const f = await fixture(call => call.action === 'check' ? { ok: true, paid: false, status: 'canceled' } : created,
        { [requestStorage]: { '': savedOrder(), [accountA]: savedOrder(accountA) } }, '', true);
      await f.page.evaluate(() => claimPaidLicenseKey());
      assert.equal(await activeKey(f.page), null); assert.equal((await map(f.page))[''], undefined);
      assert.equal((await map(f.page))[accountA].webClaimToken, token);
      await fillAndCreate(f.page);
      assert.notEqual(f.calls.at(-1).payload.web_claim_token, token);
      await f.close();
    });

    await check('account switch drops an in-flight response and preserves the previous account order', async () => {
      let release; const held = new Promise(resolve => { release = resolve; });
      const f = await fixture(() => held, { kapterka_auth_user: { email: accountA, callsign: 'A' }, [requestStorage]: { [accountA]: savedOrder(accountA) } }, '', true);
      await f.page.evaluate(() => { window.heldCheck = claimPaidLicenseKey(); });
      await f.page.waitForFunction(() => document.getElementById('btnManualVerify').disabled);
      await f.page.evaluate(email => { logoutUserSession(); setUserSession({ email, callsign: 'B', keys: [] }); }, accountB);
      release(paid); await f.page.evaluate(() => window.heldCheck);
      assert.equal(await activeKey(f.page), null);
      assert.equal((await map(f.page))[accountA].webClaimToken, token);
      assert.equal(await f.page.evaluate(() => pendingPaymentRequest()), null);
      assert.notEqual(await f.page.locator('#liveGeneratedKeyDisplay').innerText(), key);
      await f.close();
    });

    await check('logout during create cannot restore the order or navigate another account', async () => {
      let release; const held = new Promise(resolve => { release = resolve; });
      const f = await fixture(() => held, { kapterka_auth_user: { email: accountA, callsign: 'A' } });
      await f.page.evaluate(() => {
        document.getElementById('payEmailInput').value = 'buyer@example.invalid';
        window.heldCreate = processYooKassaPayment();
      });
      await f.page.waitForFunction(() => document.getElementById('btnPayYooKassaMain').disabled);
      await f.page.evaluate(email => { logoutUserSession(); setUserSession({ email, callsign: 'B', keys: [] }); }, accountB);
      release(created); await f.page.evaluate(() => window.heldCreate);
      assert.equal((await f.page.evaluate(() => window.testPaymentRedirects)).length, 0);
      assert.equal((await map(f.page))[accountA].paymentId, undefined);
      assert.equal((await map(f.page))[accountB], undefined);
      assert.equal(await activeKey(f.page), null);
      await f.close();
    });

    await check('logout during both manual verification paths cannot apply a key to another account', async () => {
      for (const cabinet of [false, true]) {
        let release; const held = new Promise(resolve => { release = resolve; });
        let started; const requestStarted = new Promise(resolve => { started = resolve; });
        const f = await fixture(() => { started(); return held; }, { kapterka_auth_user: { email: accountA, callsign: 'A' } });
        await f.page.evaluate(({ key, cabinet }) => {
          document.getElementById(cabinet ? 'cabManualKeyInput' : 'payOrderIdInput').value = key;
          window.heldManual = cabinet ? linkLicenseKeyInCabinet() : verifyWithManualOrderId();
        }, { key, cabinet });
        await requestStarted;
        assert.equal(f.calls.length, 1);
        await f.page.evaluate(email => { logoutUserSession(); setUserSession({ email, callsign: 'B', keys: [] }); }, accountB);
        release(status); await f.page.evaluate(() => window.heldManual);
        assert.equal(await activeKey(f.page), null);
        assert.notEqual(await f.page.locator('#liveGeneratedKeyDisplay').innerText(), key);
        await f.close();
      }
    });

    await check('terminal API error keeps the saved order and retry button; polling limit is resumable', async () => {
      let response = { status: 403, json: { ok: false, error: 'PAYMENT_CLAIM_MISMATCH' } };
      const f = await fixture(() => response, { [requestStorage]: { '': savedOrder() } }, '', true);
      await f.page.evaluate(() => claimPaidLicenseKey());
      assert.equal((await map(f.page))[''].webClaimToken, token);
      assert.equal(await activeKey(f.page), null);
      assert.match(await f.page.locator('#liveKeyStatusDisplay').innerText(), /другом браузере или аккаунте/);
      assert.equal(await f.page.locator('#btnManualVerify').isDisabled(), false);
      response = pending;
      await f.page.evaluate(async () => { for (let attempt = 0; attempt < 60; attempt++) await claimPaidLicenseKey({ automatic: true }); });
      const count = f.calls.length;
      await f.page.clock.runFor(30001);
      assert.equal(f.calls.length, count);
      assert.match(await f.page.locator('#liveKeyStatusDisplay').innerText(), /приостановлена/);
      response = paid;
      await f.page.evaluate(() => claimPaidLicenseKey());
      assert.equal(await activeKey(f.page), key);
      await f.close();
    });

    await check('browser AbortError is treated as a retryable timeout rather than a terminal API error', async () => {
      const f = await fixture(() => { throw new Error('No network expected'); });
      const result = await f.page.evaluate(async () => {
        const original = window.fetch;
        window.fetch = async () => { throw new DOMException('The operation was aborted.', 'AbortError'); };
        try { await requestPaymentApi('check', {}); return null; }
        catch (error) { return { code: error.code, retryable: error.retryable, message: error.message }; }
        finally { window.fetch = original; }
      });
      assert.equal(result.code, 'REQUEST_TIMEOUT'); assert.equal(result.retryable, true);
      assert.match(result.message, /Повторите/);
      await f.close();
    });

    await check('manual key status is read-only, accepts server 0/1 keys and displays the exact expiry', async () => {
      const f = await fixture(call => { assert.equal(call.action, 'license_status'); assert.deepEqual(call.payload, { license_key: key }); return status; });
      await f.page.evaluate(value => { document.getElementById('cabManualKeyInput').value = value; return linkLicenseKeyInCabinet(); }, key);
      assert.equal(await activeKey(f.page), key);
      assert.equal(f.calls.length, 1);
      assert.ok(f.external.every(url => !url.includes('firestore.googleapis.com')));
      assert.equal(await f.page.evaluate(value => JSON.parse(localStorage.getItem('kapterka_license_meta_v2'))[value].expiresAt, key), expiresAt);
      await f.close();
    });

    await check('inactive/manual legacy keys never bypass server status verification', async () => {
      const f = await fixture(() => ({ ...status, active: false, status: 'EXPIRED', expires_at: Date.now() - 1 }));
      await f.page.evaluate(value => { document.getElementById('payOrderIdInput').value = value; return verifyWithManualOrderId(); }, key);
      assert.equal(await activeKey(f.page), null);
      await f.page.evaluate(() => { document.getElementById('payOrderIdInput').value = 'KPT-2222-3333-4444'; return verifyWithManualOrderId(); });
      assert.equal(f.calls.length, 1); assert.equal(await activeKey(f.page), null);
      await f.close();
    });
    console.log(`PASS: ${passed} payment/license browser scenarios; all external traffic mocked.`);
  } finally {
    for (const context of scopes) await context.close();
    await browser.close();
    await new Promise(resolve => server.close(resolve));
  }
})().catch(error => { console.error(error); process.exitCode = 1; });
