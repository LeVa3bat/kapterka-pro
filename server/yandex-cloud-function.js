const https = require('https');
const crypto = require('crypto');

// KAPTERKA PRO — secure payment/notification backend template.
// Secrets are read only from environment variables and must never be committed.
const YOOKASSA_SHOP_ID = process.env.YOOKASSA_SHOP_ID || '1450722';
const YOOKASSA_SECRET_KEY = process.env.YOOKASSA_SECRET_KEY || '';
const TG_BOT_TOKEN = process.env.TG_BOT_TOKEN || '';
const TG_ADMIN_CHAT_ID = process.env.TG_ADMIN_CHAT_ID || '';
const PAYMENT_AMOUNT_RUB = Number(process.env.PAYMENT_AMOUNT_RUB || 490);

const CHECKSUM_CHARS = '23456789ABCDEFGHJKLMNPQRSTUVWXYZ';

function json(statusCode, body, headers = {}) {
  return {
    statusCode,
    headers: {
      'Content-Type': 'application/json; charset=utf-8',
      'Cache-Control': 'no-store',
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Headers': 'Content-Type',
      'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
      ...headers
    },
    body: JSON.stringify(body)
  };
}

function jsonpOrJson(statusCode, body, callback) {
  if (!callback) return json(statusCode, body);
  const safeCallback = String(callback).replace(/[^A-Za-z0-9_$\.]/g, '').slice(0, 80);
  if (!safeCallback) return json(statusCode, body);
  return {
    statusCode,
    headers: {
      'Content-Type': 'application/javascript; charset=utf-8',
      'Cache-Control': 'no-store',
      'Access-Control-Allow-Origin': '*'
    },
    body: `${safeCallback}(${JSON.stringify(body)});`
  };
}

function cleanText(value, max = 160) {
  return String(value || '').replace(/[\u0000-\u001F\u007F]/g, ' ').trim().slice(0, max);
}

function cleanEmail(value) {
  const email = cleanText(value, 160).toLowerCase();
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) ? email : '';
}

function escapeTelegramHtml(value) {
  return String(value || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
}

function computeKeyChecksum(p1, p2) {
  const s = `KAPT-${p1}-${p2}-KAPT3RKA_881_MILITARY`;
  let h1 = 0x811c9dc5 >>> 0;
  let h2 = 0x5a2d1e39 >>> 0;
  for (let i = 0; i < s.length; i++) {
    const code = s.charCodeAt(i);
    h1 = Math.imul(h1 ^ code, 0x01000193) >>> 0;
    h2 = (Math.imul(h2 + code, 31) + 0x45) >>> 0;
  }
  return (
    CHECKSUM_CHARS[(h1 >>> 24) & 0x1F] +
    CHECKSUM_CHARS[(h1 >>> 16) & 0x1F] +
    CHECKSUM_CHARS[(h2 >>> 24) & 0x1F] +
    CHECKSUM_CHARS[(h2 >>> 16) & 0x1F]
  );
}

function keyForPayment(paymentId) {
  const normalized = String(paymentId || '').toUpperCase().replace(/[^A-Z0-9]/g, '');
  const seed = crypto.createHash('sha256').update(normalized).digest('hex').toUpperCase();
  const p1 = seed.slice(0, 4);
  const p2 = seed.slice(4, 8);
  return `KAPT-${p1}-${p2}-${computeKeyChecksum(p1, p2)}`;
}

function sendTelegram(text) {
  return new Promise((resolve) => {
    if (!TG_BOT_TOKEN || !TG_ADMIN_CHAT_ID) return resolve(false);

    const payload = JSON.stringify({
      chat_id: TG_ADMIN_CHAT_ID,
      text,
      parse_mode: 'HTML'
    });

    const req = https.request({
      hostname: 'api.telegram.org',
      port: 443,
      path: `/bot${TG_BOT_TOKEN}/sendMessage`,
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(payload)
      },
      timeout: 8000
    }, (res) => {
      res.resume();
      resolve(res.statusCode >= 200 && res.statusCode < 300);
    });

    req.on('timeout', () => {
      req.destroy();
      resolve(false);
    });
    req.on('error', () => resolve(false));
    req.write(payload);
    req.end();
  });
}

function requestYooKassa(method, apiPath, data = null, idempotenceKey = null) {
  return new Promise((resolve, reject) => {
    if (!YOOKASSA_SECRET_KEY) {
      reject(new Error('YOOKASSA_SECRET_KEY is not configured'));
      return;
    }

    const auth = Buffer.from(`${YOOKASSA_SHOP_ID}:${YOOKASSA_SECRET_KEY}`).toString('base64');
    const body = data ? JSON.stringify(data) : '';
    const options = {
      hostname: 'api.yookassa.ru',
      port: 443,
      path: apiPath,
      method,
      headers: {
        Authorization: `Basic ${auth}`,
        'Idempotence-Key': idempotenceKey || crypto.randomUUID()
      },
      timeout: 12000
    };

    if (body) {
      options.headers['Content-Type'] = 'application/json';
      options.headers['Content-Length'] = Buffer.byteLength(body);
    }

    const req = https.request(options, (res) => {
      let responseBody = '';
      res.on('data', (chunk) => { responseBody += chunk; });
      res.on('end', () => {
        let parsed = {};
        try { parsed = responseBody ? JSON.parse(responseBody) : {}; } catch (_) {}
        if (res.statusCode >= 200 && res.statusCode < 300) {
          resolve(parsed);
        } else {
          const error = new Error(`YooKassa returned HTTP ${res.statusCode}`);
          error.statusCode = res.statusCode;
          error.response = parsed;
          reject(error);
        }
      });
    });

    req.on('timeout', () => req.destroy(new Error('YooKassa timeout')));
    req.on('error', reject);
    if (body) req.write(body);
    req.end();
  });
}

module.exports.handler = async function handler(event) {
  const method = String(event?.httpMethod || 'GET').toUpperCase();
  if (method === 'OPTIONS') return json(200, { ok: true });

  const query = event?.queryStringParameters || {};
  let body = {};
  try {
    const raw = event?.isBase64Encoded
      ? Buffer.from(event.body || '', 'base64').toString('utf8')
      : event?.body;
    body = typeof raw === 'string' && raw ? JSON.parse(raw) : (raw || {});
  } catch (_) {
    body = {};
  }

  const action = cleanText(query.action || body.action, 40).toLowerCase();
  const callback = query.callback || '';

  try {
    if (action === 'health') {
      return jsonpOrJson(200, {
        ok: true,
        service: 'kapterka-payment-api',
        shopId: YOOKASSA_SHOP_ID,
        secretConfigured: Boolean(YOOKASSA_SECRET_KEY)
      }, callback);
    }

    if (action === 'send_telegram') {
      const text = cleanText(body.text || query.text, 3500);
      if (!text) return jsonpOrJson(400, { ok: false, error: 'EMPTY_TEXT' }, callback);
      const delivered = await sendTelegram(text);
      return jsonpOrJson(200, { ok: delivered }, callback);
    }

    if (action === 'create' || action === 'pay') {
      const email = cleanEmail(query.email || body.email);
      const callsign = cleanText(query.callsign || body.callsign || 'Пользователь', 80);
      if (!email) return jsonpOrJson(400, { ok: false, error: 'INVALID_EMAIL' }, callback);

      const returnUrlRaw = cleanText(query.return_url || body.return_url, 300);
      const allowedReturnUrl =
        returnUrlRaw.startsWith('kapterka://payment_success') ||
        returnUrlRaw.startsWith('https://kapterka-pro.ru/');
      const returnUrl = allowedReturnUrl ? returnUrlRaw : 'https://kapterka-pro.ru/?payment=check#tabPayment';

      const idempotenceKey = cleanText(
        query.idempotence_key || body.idempotence_key || crypto.randomUUID(),
        100
      );

      const payment = await requestYooKassa(
        'POST',
        '/v3/payments',
        {
          amount: {
            value: PAYMENT_AMOUNT_RUB.toFixed(2),
            currency: 'RUB'
          },
          confirmation: {
            type: 'redirect',
            return_url: returnUrl
          },
          capture: true,
          description: 'Каптёрка PRO — 30 дней',
          metadata: {
            callsign,
            email,
            duration_days: '30'
          }
        },
        idempotenceKey
      );

      return jsonpOrJson(200, {
        ok: true,
        payment_id: payment.id || '',
        confirmation_url: payment.confirmation?.confirmation_url || ''
      }, callback);
    }

    if (action === 'check') {
      const paymentId = cleanText(query.payment_id || body.payment_id, 100);
      if (!paymentId) return jsonpOrJson(400, { ok: false, error: 'MISSING_PAYMENT_ID' }, callback);

      const payment = await requestYooKassa('GET', `/v3/payments/${encodeURIComponent(paymentId)}`);
      const paid = payment.status === 'succeeded' && payment.paid === true;
      const response = {
        ok: true,
        status: payment.status || 'unknown',
        paid
      };

      if (paid) response.key = keyForPayment(paymentId);
      return jsonpOrJson(200, response, callback);
    }

    return jsonpOrJson(400, { ok: false, error: 'INVALID_ACTION' }, callback);
  } catch (error) {
    console.error('Kapterka payment backend error:', error?.message || error);
    return jsonpOrJson(502, {
      ok: false,
      error: 'UPSTREAM_ERROR'
    }, callback);
  }
};
