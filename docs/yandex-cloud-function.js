const https = require('https');

// ==========================================================
// 🛠️ НАСТРОЙКИ (Вставьте свои данные ЮKassa)
// ==========================================================
const YOOKASSA_SHOP_ID = '1450722'; // Ваш Shop ID ЮKassa
const YOOKASSA_SECRET_KEY = 'test_...'; // Ваш Secret Key ЮKassa (начинается на test_ или live_)
const TG_BOT_TOKEN = '8913866950:AAFSMMAOHyULBE4uhsxdEoYG5fUT0-pSSr8';
const TG_ADMIN_CHAT_ID = '7426550032';

// Криптографическая подпись ключа
const CHECKSUM_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
function computeKeyChecksum(p1, p2) {
  const s = `KAPT-${p1}-${p2}-KAPT3RKA_881_MILITARY`;
  let h1 = 0x811c9dc5 >>> 0; let h2 = 0x5a2d1e39 >>> 0;
  for (let i = 0; i < s.length; i++) {
    const code = s.charCodeAt(i);
    h1 = Math.imul(h1 ^ code, 0x01000193) >>> 0;
    h2 = (Math.imul(h2 + code, 31) + 0x45) >>> 0;
  }
  return `${CHECKSUM_CHARS[(h1>>>24)&0x1F]}${CHECKSUM_CHARS[(h1>>>16)&0x1F]}${CHECKSUM_CHARS[(h2>>>24)&0x1F]}${CHECKSUM_CHARS[(h2>>>16)&0x1F]}`;
}

// Отправка в Телеграм
function sendTelegram(text) {
  return new Promise((resolve) => {
    const payload = JSON.stringify({ chat_id: TG_ADMIN_CHAT_ID, text: text, parse_mode: 'HTML' });
    const req = https.request({
      hostname: 'api.telegram.org', port: 443, path: `/bot${TG_BOT_TOKEN}/sendMessage`, method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(payload) }
    }, (res) => resolve());
    req.on('error', () => resolve());
    req.write(payload); req.end();
  });
}

// Запрос к API ЮKassa
function requestYooKassa(method, path, data = null) {
  return new Promise((resolve, reject) => {
    const auth = Buffer.from(`${YOOKASSA_SHOP_ID}:${YOOKASSA_SECRET_KEY}`).toString('base64');
    const options = {
      hostname: 'api.yookassa.ru',
      port: 443,
      path: path,
      method: method,
      headers: {
        'Authorization': `Basic ${auth}`,
        'Idempotence-Key': Math.random().toString(36).substring(7)
      }
    };
    if (data) {
      options.headers['Content-Type'] = 'application/json';
      options.headers['Content-Length'] = Buffer.byteLength(data);
    }
    const req = https.request(options, (res) => {
      let body = '';
      res.on('data', d => body += d);
      res.on('end', () => resolve(JSON.parse(body)));
    });
    req.on('error', reject);
    if (data) req.write(data);
    req.end();
  });
}

module.exports.handler = async function (event, context) {
  const headers = {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': 'Content-Type',
    'Access-Control-Allow-Methods': 'GET, POST, OPTIONS'
  };

  if (event.httpMethod === 'OPTIONS') {
    return { statusCode: 200, headers, body: '' };
  }

  const query = event.queryStringParameters || {};
  let body = {};
  try { body = event.isBase64Encoded ? JSON.parse(Buffer.from(event.body, 'base64').toString('utf8')) : (typeof event.body === 'string' ? JSON.parse(event.body) : event.body || {}); } catch(e){}

  if (query.action === 'create' || body.action === 'create') {
    const payload = JSON.stringify({
      amount: { value: '490.00', currency: 'RUB' },
      confirmation: { type: 'redirect', return_url: 'https://kapterka-pro.ru/' },
      capture: true,
      description: 'Лицензия ПРО (30 суток)',
      metadata: { callsign: body.callsign || 'Боец', email: body.email || '' }
    });
    const yooRes = await requestYooKassa('POST', '/v3/payments', payload);
    return {
      statusCode: 200, headers,
      body: JSON.stringify({
        payment_id: yooRes.id,
        confirmation_url: yooRes.confirmation ? yooRes.confirmation.confirmation_url : null
      })
    };
  }

  if (query.action === 'check') {
    const paymentId = query.payment_id;
    if (!paymentId) return { statusCode: 400, headers, body: 'Missing payment_id' };
    
    const yooRes = await requestYooKassa('GET', `/v3/payments/${paymentId}`);
    
    if (yooRes.status === 'succeeded') {
      // Генерируем ключ на основе ID платежа, чтобы он был постоянным для этого платежа
      const p1 = paymentId.substring(0, 4).toUpperCase();
      const p2 = paymentId.substring(4, 8).toUpperCase();
      const key = `KAPT-${p1}-${p2}-${computeKeyChecksum(p1, p2)}`;
      
      // Отправляем уведомление (один раз)
      if (!yooRes.metadata.notified) {
        await sendTelegram(
          `⚡ <b>ОПЛАТА ПОДТВЕРЖДЕНА (490 ₽)</b>\n\n` +
          `👤 <b>Боец:</b> ${yooRes.metadata.callsign || 'Боец'}\n` +
          `📧 <b>Email:</b> ${yooRes.metadata.email || 'Нет'}\n` +
          `🔑 <b>АВТО-КЛЮЧ:</b> <code>${key}</code>\n\n` +
          `✅ <i>Ключ выдан на экран бойца.</i>`
        );
        // Не обновляем metadata в ЮKassa, так как это требует отдельного запроса
      }
      
      return {
        statusCode: 200, headers,
        body: JSON.stringify({ status: 'succeeded', key: key })
      };
    }
    
    return { statusCode: 200, headers, body: JSON.stringify({ status: yooRes.status }) };
  }

  return { statusCode: 400, headers, body: 'Invalid action' };
};
