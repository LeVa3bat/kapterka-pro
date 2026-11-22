// Real Chromium end-to-end verification of redesigned site, Email-only OTP Web Auth,
// Path A interactive demos, legal pages, and responsive layouts (360px / 412px / 1280px).
const assert = require('node:assert/strict');
const fs = require('node:fs');
const http = require('node:http');
const path = require('node:path');
const { chromium } = require('playwright');

const root = path.resolve(__dirname, '../..');
const docs = path.join(root, 'docs');
const api = 'https://kapterka-api.alex-666-881.workers.dev';
const previewDir = process.env.SITE_PREVIEW_DIR || path.join(root, 'design/site-preview');

function startStaticServer() {
  const types = {
    '.html': 'text/html; charset=utf-8',
    '.js': 'text/javascript; charset=utf-8',
    '.css': 'text/css; charset=utf-8',
    '.json': 'application/json; charset=utf-8',
    '.svg': 'image/svg+xml',
    '.png': 'image/png',
    '.jpg': 'image/jpeg',
    '.webp': 'image/webp',
    '.woff2': 'font/woff2',
    '.pdf': 'application/pdf',
    '.xlsx': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    '.apk': 'application/vnd.android.package-archive'
  };
  const server = http.createServer((req, res) => {
    const pathname = decodeURIComponent(new URL(req.url, 'http://localhost').pathname);
    const relative = pathname.replace(/^\/+/, '') || 'index.html';
    const file = path.resolve(docs, relative);
    if (!file.startsWith(docs + path.sep) || !fs.existsSync(file) || !fs.statSync(file).isFile()) {
      res.writeHead(404).end();
      return;
    }
    res.writeHead(200, { 'Content-Type': types[path.extname(file).toLowerCase()] || 'application/octet-stream' });
    fs.createReadStream(file).pipe(res);
  });
  return new Promise((resolve) => server.listen(0, '127.0.0.1', () => resolve(server)));
}

(async () => {
  fs.mkdirSync(previewDir, { recursive: true });
  const server = await startStaticServer();
  const base = `http://127.0.0.1:${server.address().port}`;
  const browser = await chromium.launch({
    headless: true,
    executablePath: process.env.CHROME_BIN || (process.platform === 'win32' ? 'C:/Program Files/Google/Chrome/Application/chrome.exe' : '/usr/bin/google-chrome')
  });

  try {
    // 1. Visual + Layout overflow + Interactive Path A checks across 3 viewports
    for (const [label, viewport] of [
      ['desktop-1280', { width: 1280, height: 800 }],
      ['mobile-412', { width: 412, height: 915 }],
      ['mobile-360', { width: 360, height: 800 }]
    ]) {
      const ctx = await browser.newContext({ viewport });
      await ctx.route('**/*', (route) => {
        const url = new URL(route.request().url());
        if (url.origin === base) return route.continue();
        return route.fulfill({ status: 200, contentType: 'text/plain', body: '' });
      });
      const page = await ctx.newPage();
      const errors = [];
      page.on('pageerror', (err) => errors.push(err.message));

      // Check homepage & legal/info pages for overflow, zero external Google Fonts, and zero JS errors
      for (const file of ['', 'terms.html', 'privacy.html', 'security.html', 'help.html', 'updates.html', 'skladskoy-uchet-android.html', 'uchet-imushchestva-offline.html', 'uchet-ostatkov-na-telefone.html']) {
        await page.goto(`${base}/${file}`, { waitUntil: 'networkidle' });
        await page.locator('h1').waitFor();
        const check = await page.evaluate(() => ({
          scrollW: document.documentElement.scrollWidth,
          innerW: window.innerWidth,
          googleFonts: [...document.querySelectorAll('link[href*="fonts.googleapis.com"]')].length
        }));
        assert.ok(
          check.scrollW <= check.innerW + 1,
          `${label} overflow on /${file}: scrollWidth=${check.scrollW} > innerWidth=${check.innerW}`
        );
        assert.equal(check.googleFonts, 0, `${label}: external Google Fonts link found on /${file}`);
      }

      // Homepage interactive features
      await page.goto(`${base}/`, { waitUntil: 'networkidle' });
      // Path A persona switcher visible even on mobile!
      const personaVisible = await page.locator('#profiles').isVisible();
      assert.equal(personaVisible, true, `${label}: Path A profile switcher must be visible`);
      await page.locator('.lp-persona-tabs button[data-persona="1"]').click();
      assert.match(await page.locator('#personaHead').innerText(), /подразделения/i);
      await page.locator('.lp-persona-tabs button[data-persona="2"]').click();
      assert.match(await page.locator('#personaHead').innerText(), /Пустой склад/i);
      await page.locator('.lp-persona-tabs button[data-persona="0"]').click();

      // Interactive ledger demo: +5 income, -3 issue, preset switch, reset
      const firstQty = page.locator('#lpLedger tbody tr').first().locator('.lp-qty');
      assert.equal(await firstQty.innerText(), '6');
      await page.locator('#lpLedger tbody tr').first().locator('.lp-income').click();
      assert.equal(await firstQty.innerText(), '11');
      await page.locator('#lpLedger tbody tr').first().locator('.lp-issue').click();
      assert.equal(await firstQty.innerText(), '8');
      assert.match(await page.locator('#lpJournal').innerText(), /Выдача −3/);
      await page.locator('[data-ledger-preset="unit"]').click();
      assert.equal(await firstQty.innerText(), '42');
      await page.locator('[data-ledger-preset="workshop"]').click();
      assert.equal(await firstQty.innerText(), '6');

      if (label === 'desktop-1280' || label === 'mobile-412') {
        await page.evaluate(() => window.scrollTo(0, 0));
        await page.waitForTimeout(1300);
        await page.screenshot({ path: path.join(previewDir, `${label}-home.png`), fullPage: false });
        await page.screenshot({ path: path.join(previewDir, `${label}-home-full.png`), fullPage: true });
        await page.evaluate(() => { window.switchMainTab('tabCabinet'); window.scrollTo(0, 0); });
        await page.locator('#tabCabinet.active').waitFor({ state: 'visible' });
        await page.waitForTimeout(250);
        await page.evaluate(() => window.scrollTo(0, 0));
        await page.screenshot({ path: path.join(previewDir, `${label}-cabinet-auth.png`), fullPage: false });
        await page.evaluate(() => { window.switchMainTab('tabPayment'); window.scrollTo(0, 0); });
        await page.locator('#tabPayment.active').waitFor({ state: 'visible' });
        await page.waitForTimeout(250);
        await page.evaluate(() => window.scrollTo(0, 0));
        await page.screenshot({ path: path.join(previewDir, `${label}-payment.png`), fullPage: false });
      }

      // Check all 5 tabs for horizontal overflow and unclipped mobile trust badges
      for (const tab of ['tabOverview', 'tabCabinet', 'tabPayment', 'tabSync', 'tabDownload']) {
        await page.evaluate((id) => window.switchMainTab(id), tab);
        await page.locator(`#${tab}.active`).waitFor({ state: 'visible' });
        const ov = await page.evaluate((id) => {
          const out = { s: document.documentElement.scrollWidth, w: window.innerWidth, clipped: [] };
          if (id === 'tabPayment') {
            document.querySelectorAll('#tabPayment .v2-trust-list span').forEach((el) => {
              const r = el.getBoundingClientRect();
              if (r.right > window.innerWidth + 1) out.clipped.push(el.textContent.trim());
            });
          }
          return out;
        }, tab);
        assert.ok(ov.s <= ov.w + 1, `${label} overflow on ${tab}: ${ov.s} > ${ov.w}`);
        assert.deepEqual(ov.clipped, [], `${label} clipped trust badges on ${tab}: ${ov.clipped.join(', ')}`);
      }

      assert.deepEqual(errors, [], `${label}: browser page errors`);
      await ctx.close();
      console.log(`PASS: layout, legal pages, Path A switcher, ledger demo & 5 tabs (${label})`);
    }

    // 2. Email-only OTP Web Auth end-to-end against mocked Cloudflare Worker API
    {
      const ctx = await browser.newContext({ viewport: { width: 1280, height: 800 } });
      const apiCalls = [];
      const token = 'a'.repeat(64);
      const user = {
        email: 'master@workshop.ru',
        callsign: 'Алексей',
        rank: 'Мастер',
        unitName: 'Цех №1',
        unitKey: 'kapt_0123456789abcdef0123',
        phone: '',
        subscribedToNewsletter: true,
        status: 'ACTIVE'
      };
      await ctx.route('**/*', async (route) => {
        const req = route.request();
        const url = new URL(req.url());
        if (url.origin === base) return route.continue();
        if (url.origin === api) {
          const action = url.searchParams.get('action');
          const payload = req.postDataJSON();
          apiCalls.push({ action, payload });
          if (action === 'web_auth_request_code') {
            return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ ok: true, status: 'CODE_SENT', maskedEmail: 'ma***@workshop.ru', expiresIn: 600, existingAccount: payload.mode === 'login' }) });
          }
          if (action === 'web_auth_verify_code') {
            if (payload.code !== '482910') {
              return route.fulfill({ status: 403, contentType: 'application/json', body: JSON.stringify({ ok: false, error: 'WRONG_CODE', attemptsLeft: 4 }) });
            }
            return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ ok: true, status: 'AUTHENTICATED', sessionToken: token, sessionExpiresAt: Date.now() + 86400000, user }) });
          }
          if (action === 'web_auth_update_profile') {
            return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ ok: true, user: { ...user, callsign: payload.callsign, unitName: payload.unit_name } }) });
          }
          if (action === 'web_auth_logout') {
            return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ ok: true }) });
          }
          if (action === 'web_auth_session') {
            return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ ok: true, status: 'AUTHENTICATED', sessionExpiresAt: Date.now() + 86400000, user }) });
          }
        }
        return route.fulfill({ status: 200, contentType: 'text/plain', body: '' });
      });

      const page = await ctx.newPage();
      const errors = [];
      page.on('pageerror', (err) => errors.push(err.message));

      await page.goto(`${base}/#tabCabinet`, { waitUntil: 'networkidle' });
      await page.locator('#regCallsign').fill('Алексей');
      await page.locator('#regEmail').fill('master@workshop.ru');
      await page.locator('#regUnitName').fill('Цех №1');
      await page.locator('#regRank').fill('Мастер');
      await page.getByRole('button', { name: 'Получить код на Email' }).click();

      await page.locator('#regStepVerification').waitFor({ state: 'visible' });
      assert.equal(await page.locator('#authStep2Badge').evaluate((el) => el.classList.contains('active')), true);

      // Back button returns cleanly to step 1
      await page.locator('#regBackToEmailBtn').click();
      await page.locator('#regStepInputs').waitFor({ state: 'visible' });
      assert.equal(await page.locator('#authStep1Badge').evaluate((el) => el.classList.contains('active')), true);

      // Request code again and test wrong code inline error, then auto-submit on 6th digit
      await page.getByRole('button', { name: 'Получить код на Email' }).click();
      await page.locator('#regStepVerification').waitFor({ state: 'visible' });
      for (let i = 1; i <= 6; i++) await page.locator(`#pin${i}`).fill('1');
      await page.waitForFunction(() => document.getElementById('authInlineAlert')?.textContent?.includes('Осталось попыток: 4'));

      for (const [idx, digit] of '482910'.split('').entries()) {
        await page.locator(`#pin${idx + 1}`).fill(digit);
      }
      await page.locator('#cabinetContent').waitFor({ state: 'visible' });
      assert.equal(await page.locator('#sessionCallsignText').innerText(), 'Алексей');
      assert.equal(await page.locator('#cabUnitKeyInput').inputValue(), 'kapt_0123456789abcdef0123');

      // Invalid unit key is rejected client-side without corrupting storage or calling server
      const validKeyBefore = await page.locator('#cabUnitKeyInput').inputValue();
      const profCallsBefore = apiCalls.filter((c) => c.action === 'web_auth_update_profile').length;
      await page.locator('#cabUnitKeyInput').fill('bad key!');
      await page.getByRole('button', { name: 'Сохранить' }).click();
      await page.waitForTimeout(100);
      assert.equal(apiCalls.filter((c) => c.action === 'web_auth_update_profile').length, profCallsBefore, 'invalid unitKey must not call server');
      assert.equal(await page.evaluate(() => localStorage.getItem('kapterka_unit_key')), validKeyBefore, 'invalid unitKey must not overwrite localStorage');

      // Generate new unit key uses 80-bit hex (20 hex chars)
      await page.getByRole('button', { name: 'Новый' }).click();
      assert.match(await page.locator('#cabUnitKeyInput').inputValue(), /^kapt_[a-f0-9]{20}$/);

      // Save profile syncs to Worker
      await page.locator('#cabCallsignInput').fill('Алексей Мастер');
      await page.getByRole('button', { name: 'Сохранить' }).click();
      await page.waitForFunction(() => true);
      await page.waitForTimeout(150);
      assert.ok(apiCalls.some((c) => c.action === 'web_auth_update_profile' && c.payload.callsign === 'Алексей Мастер'));

      // Logout revokes session on Worker
      await page.getByRole('button', { name: 'Выйти' }).click();
      await page.locator('#authContainer').waitFor({ state: 'visible' });
      await page.waitForTimeout(150);
      assert.ok(apiCalls.some((c) => c.action === 'web_auth_logout' && c.payload.token === token));
      assert.deepEqual(errors, [], 'zero browser errors during Email OTP auth flow');
      await ctx.close();
      console.log('PASS: Email-only OTP Web Auth (register, back button, wrong code inline error, 6-digit auto-verify, 80-bit keygen, server profile sync, server logout)');
    }
  } finally {
    await browser.close();
    await new Promise((resolve) => server.close(resolve));
  }
})().catch((err) => {
  console.error(err);
  process.exitCode = 1;
});
