/**
 * KAPTERKA PRO — почтовый ретранслятор через Gmail владельца.
 *
 * Сервер kapterka-api присылает сюда готовое письмо (код подтверждения,
 * приветствие с ключом подразделения, лицензионный ключ), а этот скрипт
 * отправляет его с вашего Gmail. Бесплатно, до 100 писем в день.
 *
 * Установка:
 * 1. https://script.google.com → «Создать проект» → вставить этот код целиком.
 * 2. Слева «Настройки проекта» (шестерёнка) → «Свойства скрипта» → «Добавить свойство»:
 *      Свойство: RELAY_SECRET   Значение: ваша секретная фраза (не короче 16 символов)
 * 3. Вверху «Начать развертывание» → «Новое развертывание» → тип «Веб-приложение»:
 *      Выполнять как: «Я»;  У кого есть доступ: «Все»  → «Начать развертывание»
 *    Разрешите доступ к Gmail (Дополнительно → Перейти к проекту → Разрешить).
 * 4. Скопируйте «URL веб-приложения» (заканчивается на /exec).
 */

const SENDER_NAME = 'Каптёрка ПРО';

function doPost(e) {
  try {
    const msg = JSON.parse((e && e.postData && e.postData.contents) || '{}');
    const secret = String(PropertiesService.getScriptProperties().getProperty('RELAY_SECRET') || '').trim();
    if (secret.length < 16 || String(msg.secret || '').trim() !== secret) return json_({ ok: false, error: 'FORBIDDEN' });

    const to = String(msg.to || '').trim();
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(to)) return json_({ ok: false, error: 'INVALID_EMAIL' });
    if (MailApp.getRemainingDailyQuota() < 1) return json_({ ok: false, error: 'QUOTA_EXCEEDED' });

    MailApp.sendEmail({
      to: to,
      subject: String(msg.subject || 'Каптёрка ПРО').slice(0, 200),
      body: String(msg.text || '').slice(0, 20000),
      htmlBody: String(msg.html || '').slice(0, 60000),
      name: SENDER_NAME
    });
    return json_({ ok: true, quota_left: MailApp.getRemainingDailyQuota() });
  } catch (err) {
    return json_({ ok: false, error: 'SEND_FAILED' });
  }
}

function doGet() {
  return json_({ ok: true, service: 'kapterka-mail-relay' });
}

function json_(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj)).setMimeType(ContentService.MimeType.JSON);
}
