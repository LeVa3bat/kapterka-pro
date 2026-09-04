/**
 * ИНСТРУКЦИЯ ПО НАСТРОЙКЕ АВТОМАТИЗАЦИИ ЧЕРЕЗ GOOGLE APPS SCRIPT
 */

// 1. ВАШИ ДАННЫЕ ДЛЯ ТЕЛЕГРАМА (УЖЕ ВСТАВЛЕНЫ)
const TELEGRAM_BOT_TOKEN = "8913866950:AAFSMMAOHyULBE4uhsxdEoYG5fUT0-pSSr8"; 
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
    
    // Проверяем, что это сообщение об успешной оплате
    if (data.event === "payment.succeeded") {
      const payment = data.object;
      
      // Ищем email (ЮKassa может присылать его в разных местах)
      let email = "Не указан";
      if (payment.receipt_registration && payment.receipt_registration.customer && payment.receipt_registration.customer.email) {
        email = payment.receipt_registration.customer.email;
      } else if (payment.metadata && payment.metadata.email) {
        email = payment.metadata.email;
      }
      
      const price = payment.amount ? payment.amount.value : "Неизвестно";
      
      if (email !== "Не указан") {
        // 1. Генерируем новый лицензионный ключ
        const newKey = generateMilitaryLicenseKey();
        
        // 2. Отправляем ключ на почту покупателю
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
        
        // 3. Отправляем уведомление ВАМ в Телеграм
        if (TELEGRAM_BOT_TOKEN !== "ВАШ_ТОКЕН_БОТА" && TELEGRAM_CHAT_ID !== "ВАШ_CHAT_ID") {
          const tgUrl = `https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/sendMessage`;
          const message = `💰 <b>Успешная продажа!</b>\n\n📧 <b>Email:</b> ${email}\n💵 <b>Сумма:</b> ${price} руб.\n🔑 <b>Выдан ключ:</b> <code>${newKey}</code>`;
          
          UrlFetchApp.fetch(tgUrl, {
            method: "post",
            contentType: "application/json",
            payload: JSON.stringify({
              chat_id: TELEGRAM_CHAT_ID,
              text: message,
              parse_mode: "HTML"
            })
          });
        }
      }
    }
    
    // Обязательно отвечаем ЮКассе, что всё хорошо
    return ContentService.createTextOutput("OK").setMimeType(ContentService.MimeType.TEXT);
  } catch (error) {
    // В случае ошибки
    return ContentService.createTextOutput("Error: " + error.toString()).setMimeType(ContentService.MimeType.TEXT);
  }
}
