import fs from 'node:fs';

const targets = [
  { name: 'Главная', url: 'https://kapterka-pro.ru/', own: true },
  { name: 'Помощь', url: 'https://kapterka-pro.ru/help.html', own: true },
  { name: 'Безопасность', url: 'https://kapterka-pro.ru/security.html', own: true },
  { name: 'Обновления', url: 'https://kapterka-pro.ru/updates.html', own: true },
  { name: 'Конфиденциальность', url: 'https://kapterka-pro.ru/privacy.html', own: true },
  { name: 'Оферта', url: 'https://kapterka-pro.ru/terms.html', own: true },
  { name: 'Release manifest', url: 'https://kapterka-pro.ru/release.json', own: true },
  { name: 'Материалы', url: 'https://kapterka-pro.ru/guides.html', own: true },
  { name: 'Приход и расход', url: 'https://kapterka-pro.ru/prihod-rashod-sklad-android.html', own: true },
  { name: 'Инвентаризация', url: 'https://kapterka-pro.ru/inventarizaciya-na-android.html', own: true },
  { name: 'Выдача имущества', url: 'https://kapterka-pro.ru/uchet-vydachi-imushchestva-android.html', own: true },
  { name: 'Перемещение между складами', url: 'https://kapterka-pro.ru/peremeshchenie-mezhdu-skladami-android.html', own: true },
  { name: 'Учёт ТМЦ', url: 'https://kapterka-pro.ru/uchet-tmc-android.html', own: true },
  { name: 'Учёт инструмента', url: 'https://kapterka-pro.ru/uchet-instrumenta-android.html', own: true },
  { name: 'Учёт оборудования', url: 'https://kapterka-pro.ru/uchet-oborudovaniya-android.html', own: true },
  { name: 'Учёт инвентаря', url: 'https://kapterka-pro.ru/uchet-inventarya-android.html', own: true },
  { name: 'Небольшой склад', url: 'https://kapterka-pro.ru/skladskoy-uchet-dlya-nebolshogo-sklada.html', own: true },
  { name: 'Склад без 1С', url: 'https://kapterka-pro.ru/sklad-bez-1c-na-telefone.html', own: true },
  { name: 'Журнал движения', url: 'https://kapterka-pro.ru/zhurnal-dvizheniya-imushchestva-android.html', own: true },
  { name: 'Несколько складов', url: 'https://kapterka-pro.ru/uchet-neskolkih-skladov-android.html', own: true },
  { name: 'Безопасное обновление', url: 'https://kapterka-pro.ru/bezopasnoe-obnovlenie-kapterka-pro.html', own: true },
  { name: 'RuStore', url: 'https://apps.rustore.ru/app/com.aistudio.kapterka.jmwqve', own: false },
  { name: 'Telegram канал', url: 'https://t.me/kapterka_pro', own: false },
  { name: 'Telegram поддержка', url: 'https://t.me/kapterka_help_bot', own: false }
];
const timeoutMs = 15000;
const expectedVersion = JSON.parse(fs.readFileSync('docs/version.json', 'utf8')).version;

async function waitForPublishedVersion() {
  for (let attempt = 1; attempt <= 15; attempt++) {
    try {
      const response = await fetch('https://kapterka-pro.ru/version.json?_=' + Date.now(), {
        cache: 'no-store',
        headers: { 'user-agent': 'Kapterka-Pro-Site-Quality/1.0' }
      });
      if (response.ok) {
        const live = await response.json();
        if (live?.version === expectedVersion) {
          console.log(`OK: live web version is ${live.version}`);
          return;
        }
        console.log(`WAIT: live=${live?.version || 'unknown'}, expected=${expectedVersion} (attempt ${attempt}/15)`);
      }
    } catch (error) {
      console.log(`WAIT: version probe failed: ${error.name || error.message} (attempt ${attempt}/15)`);
    }
    await new Promise(resolve => setTimeout(resolve, 12000));
  }
  throw new Error(`Live site did not reach expected version ${expectedVersion}`);
}

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
await waitForPublishedVersion();
for (const target of targets) await check(target);
if (failed) process.exit(1);
console.log('Live link check PASSED.');
