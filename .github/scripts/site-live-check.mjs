const targets = [
  { name: 'Главная', url: 'https://kapterka-pro.ru/', own: true },
  { name: 'Помощь', url: 'https://kapterka-pro.ru/help.html', own: true },
  { name: 'Безопасность', url: 'https://kapterka-pro.ru/security.html', own: true },
  { name: 'Обновления', url: 'https://kapterka-pro.ru/updates.html', own: true },
  { name: 'Конфиденциальность', url: 'https://kapterka-pro.ru/privacy.html', own: true },
  { name: 'Оферта', url: 'https://kapterka-pro.ru/terms.html', own: true },
  { name: 'Release manifest', url: 'https://kapterka-pro.ru/release.json', own: true },
  { name: 'RuStore', url: 'https://apps.rustore.ru/app/com.aistudio.kapterka.jmwqve', own: false },
  { name: 'Telegram канал', url: 'https://t.me/kapterka_pro', own: false },
  { name: 'Telegram поддержка', url: 'https://t.me/kapterka_help_bot', own: false }
];
const timeoutMs = 15000;
let failed = false;
async function check(target) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const response = await fetch(target.url, {
      method: 'GET', redirect: 'follow', signal: controller.signal,
      headers: { 'user-agent': 'Kapterka-Pro-Site-Quality/1.0' }
    });
    const status = response.status;
    if (target.own && (status < 200 || status >= 400)) {
      console.error(`FAIL: ${target.name} -> HTTP ${status} ${response.url}`);
      failed = true; return;
    }
    if (!target.own && (status === 404 || status === 410)) {
      console.error(`FAIL: external link ${target.name} -> HTTP ${status}`);
      failed = true; return;
    }
    if (!target.own && (status === 403 || status === 429 || status >= 500)) {
      console.warn(`WARN: ${target.name} returned HTTP ${status}; provider may block CI probes`);
      return;
    }
    console.log(`OK: ${target.name} -> HTTP ${status}`);
  } catch (error) {
    if (target.own) {
      console.error(`FAIL: ${target.name} -> ${error.name || error.message}`);
      failed = true;
    } else console.warn(`WARN: external ${target.name} could not be probed: ${error.name || error.message}`);
  } finally { clearTimeout(timer); }
}
for (const target of targets) await check(target);
if (failed) process.exit(1);
console.log('Live link check PASSED.');
