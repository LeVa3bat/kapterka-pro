/**
 * ИНСТРУКЦИЯ ПО НАСТРОЙКЕ АВТОМАТИЗАЦИИ ЧЕРЕЗ GOOGLE APPS SCRIPT
 */

// 1. ВАШИ ДАННЫЕ ДЛЯ ТЕЛЕГРАМА (НУЖЕН НОВЫЙ ТОКЕН!)
// ВАЖНО: Получите новый токен в @BotFather, так как старый заблокирован Telegram из-за утечки на GitHub.
const TELEGRAM_BOT_TOKEN = "ВАШ_НОВЫЙ_ТОКЕН_ОТ_BOTFATHER"; 
const TELEGRAM_CHAT_ID = "7426550032";

// 2. СЕКРЕТНЫЙ КЛЮЧ ЮКАССЫ (необязателен для вебхука)
const YOOKASSA_SECRET_KEY = ""; 

const CHECKSUM_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";

function computeKeyChecksum(p1, p2) {
  const s = `KAPT-${p1}-${p2}-KAPT3RKA_881_MILITARY`;
  let h1 = 0x811c9dc5 >>> 0;
  let h2 = 0x5a2d1e39 >>> 0;
  for (let i = 0; i < s.length; i++) {
    const code = s.charCodeAt(i);
    h1 = Math.imul(h1 ^ code, 0x01000193) >>> 0;
    h2 = (Math.imul(h2 + code, 31) + 0x45) >>> 0;
  }
  const c0 = CHECKSUM_CHARS[(h1 >>> 24) & 0x1F];
  const c1 = CHECKSUM_CHARS[(h1 >>> 16) & 0x1F];
  const c2 = CHECKSUM_CHARS[(h2 >>> 24) & 0x1F];
  const c3 = CHECKSUM_CHARS[(h2 >>> 16) & 0x1F];
  return `${c0}${c1}${c2}${c3}`;
}

function generateMilitaryLicenseKey() {
  function seg(len) {
    let str = '';
    for (let i = 0; i < len; i++) {
      str += CHECKSUM_CHARS.charAt(Math.floor(Math.random() * CHECKSUM_CHARS.length));
    }
    return str;
  }
  const p1 = seg(4);
  const p2 = seg(4);
  const checksum = computeKeyChecksum(p1, p2);
  return `KAPT-${p1}-${p2}-${checksum}`;
}

function doPost(e) {
  try {
    const data = JSON.parse(e.postData.contents);
    
    // Поддержка ручных уведомлений с сайта (например, форма обратной связи или клики кнопок)
    if (data.action === "send_telegram") {
       if (TELEGRAM_BOT_TOKEN !== "ВАШ_НОВЫЙ_ТОКЕН_ОТ_BOTFATHER" && TELEGRAM_BOT_TOKEN !== "") {
          const tgUrl = `https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/sendMessage`;
          UrlFetchApp.fetch(tgUrl, {
            method: "post",
            contentType: "application/json",
            payload: JSON.stringify({
              chat_id: data.chat_id || TELEGRAM_CHAT_ID,
              text: data.text,
              parse_mode: "HTML"
            }),
            muteHttpExceptions: true
          });
       }
       return ContentService.createTextOutput("OK");
    }

    // Проверяем, что это сообщение об успешной оплате
    if (data.event === "payment.succeeded") {
      const payment = data.object;
      
      // Ищем email (ЮKassa может присылать его в разных местах)
      let email = "Не указан";
      if (payment.receipt_registration && payment.receipt_registration.customer && payment.receipt_registration.customer.email) {
        email = payment.receipt_registration.customer.email;
      } else if (payment.metadata && payment.metadata.email) {
        email = payment.metadata.email;
      } else if (payment.authorization_details && payment.authorization_details.customer_email) {
        email = payment.authorization_details.customer_email;
      }
      
      const price = payment.amount ? payment.amount.value : "Неизвестно";
      
      // 1. Генерируем новый лицензионный ключ всегда
      const newKey = generateMilitaryLicenseKey();
      
      // 2. Отправляем ключ на почту покупателю, если email найден
      if (email !== "Не указан") {
        try {
          MailApp.sendEmail({
            to: email,
            subject: "Ваш лицензионный ключ для Каптёрка Про",
            htmlBody: `
              <h3>Здравия желаю!</h3>
              <p>Оплата успешно получена. Спасибо за поддержку проекта.</p>
              <p>Ваш персональный лицензионный ключ:</p>
              <h2 style="background: #eee; padding: 10px; display: inline-block;">${newKey}</h2>
              <p>Скопируйте его и вставьте на сайте или в Личном кабинете приложения.</p>
              <br>
              <p>С уважением, разработчик ПО «Каптёрка Про»</p>
            `
          });
        } catch(mailErr) {
          // Игнорируем ошибку отправки почты, чтобы не прерывать телеграм
        }
      }
      
      // 3. ВСЕГДА Отправляем уведомление ВАМ в Телеграм (даже если email нет)
      if (TELEGRAM_BOT_TOKEN !== "ВАШ_НОВЫЙ_ТОКЕН_ОТ_BOTFATHER" && TELEGRAM_BOT_TOKEN !== "") {
        const tgUrl = `https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/sendMessage`;
        const message = `💰 <b>Успешная продажа!</b>\n\n📧 <b>Email:</b> ${email}\n💵 <b>Сумма:</b> ${price} руб.\n🔑 <b>Выдан ключ:</b> <code>${newKey}</code>\n\n⚠️ <i>Если Email не указан, передайте ключ бойцу вручную.</i>`;
        
        UrlFetchApp.fetch(tgUrl, {
          method: "post",
          contentType: "application/json",
          payload: JSON.stringify({
            chat_id: TELEGRAM_CHAT_ID,
            text: message,
            parse_mode: "HTML"
          }),
          muteHttpExceptions: true
        });
      }
    }
    
    // Обязательно отвечаем ЮКассе, что всё хорошо
    return ContentService.createTextOutput("OK").setMimeType(ContentService.MimeType.TEXT);
    
  } catch (error) {
    // В случае сбоя логируем и отвечаем OK, чтобы Юкасса не спамила
    return ContentService.createTextOutput("OK").setMimeType(ContentService.MimeType.TEXT);
  }
}
