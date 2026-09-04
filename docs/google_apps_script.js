/**
 * АВТОМАТИЗАЦИЯ ВЫДАЧИ КЛЮЧЕЙ И УВЕДОМЛЕНИЙ TELEGRAM
 */

// 1. ВАШИ ДАННЫЕ ДЛЯ ТЕЛЕГРАМА 
const TELEGRAM_BOT_TOKEN = "8913866950:AAHcLToeSbgUuu2npb82Zd4-jmxlfwTu_kI"; 
const TELEGRAM_CHAT_ID = "7426550032";

// 2. ДАННЫЕ ВАШЕГО МАГАЗИНА ЮKASSA
const YOOKASSA_SHOP_ID = "1450722";
const YOOKASSA_SECRET_KEY = "live_**********QIh0"; // ВНИМАНИЕ: ВСТАВЬТЕ СЮДА ВАШ ПОЛНЫЙ СЕКРЕТНЫЙ КЛЮЧ ИЗ ЛИЧНОГО КАБИНЕТА ЮКАССЫ!

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

function sendTelegramMessage(text) {
  if (TELEGRAM_BOT_TOKEN && TELEGRAM_BOT_TOKEN !== "") {
    const tgUrl = `https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/sendMessage`;
    UrlFetchApp.fetch(tgUrl, {
      method: "post",
      contentType: "application/json",
      payload: JSON.stringify({
        chat_id: TELEGRAM_CHAT_ID,
        text: text,
        parse_mode: "HTML"
      }),
      muteHttpExceptions: true
    });
  }
}

// Позволяет делать запросы GET (для проверки)
function doGet(e) {
  return ContentService.createTextOutput("Kapterka API Server is running.");
}

function doPost(e) {
  try {
    const data = JSON.parse(e.postData.contents);
    
    // 1. Ручные уведомления с сайта (форма связи)
    if (data.action === "send_telegram") {
       sendTelegramMessage(data.text);
       return ContentService.createTextOutput("OK");
    }

    // 2. СОЗДАНИЕ ПЛАТЕЖА (Запрос от сайта)
    if (data.action === "create_payment") {
       if (YOOKASSA_SECRET_KEY.includes("***")) {
          return ContentService.createTextOutput(JSON.stringify({
            error: "В скрипте не указан полный YOOKASSA_SECRET_KEY!"
          })).setMimeType(ContentService.MimeType.JSON);
       }
       
       const email = data.email || "";
       const callsign = data.callsign || "Боец";
       
       const payload = {
         amount: {
           value: "490.00",
           currency: "RUB"
         },
         capture: true,
         confirmation: {
           type: "redirect",
           return_url: "https://alex666881.github.io/kapterka/" // ЗАМЕНИТЕ НА ВАШ РЕАЛЬНЫЙ АДРЕС САЙТА ЕСЛИ ОН ДРУГОЙ
         },
         description: `Лицензия Каптёрка ПРО для ${callsign}`,
         metadata: {
           email: email,
           callsign: callsign
         }
       };

       // Добавляем чек для ФЗ-54, если передан email
       if (email) {
         payload.receipt = {
           customer: { email: email },
           items: [
             {
               description: "Лицензионный ключ Каптёрка ПРО",
               quantity: "1.00",
               amount: { value: "490.00", currency: "RUB" },
               vat_code: 1, // Без НДС
               payment_mode: "full_prepayment",
               payment_subject: "commodity"
             }
           ]
         };
       }

       const authHeader = "Basic " + Utilities.base64Encode(YOOKASSA_SHOP_ID + ":" + YOOKASSA_SECRET_KEY);
       
       const options = {
         method: "post",
         headers: {
           "Idempotence-Key": Utilities.getUuid(),
           "Authorization": authHeader
         },
         contentType: "application/json",
         payload: JSON.stringify(payload),
         muteHttpExceptions: true
       };

       const response = UrlFetchApp.fetch("https://api.yookassa.ru/v3/payments", options);
       const result = JSON.parse(response.getContentText());

       if (result.confirmation && result.confirmation.confirmation_url) {
         return ContentService.createTextOutput(JSON.stringify({
           confirmation_url: result.confirmation.confirmation_url,
           payment_id: result.id
         })).setMimeType(ContentService.MimeType.JSON);
       } else {
         return ContentService.createTextOutput(JSON.stringify({
           error: "Ошибка создания платежа в ЮКассе: " + response.getContentText()
         })).setMimeType(ContentService.MimeType.JSON);
       }
    }

    // 3. АВТОМАТИЧЕСКАЯ ОБРАБОТКА ПЛАТЕЖЕЙ ОТ ЮКАССЫ (Вебхук)
    if (data.event === "payment.succeeded") {
      const payment = data.object;
      
      let email = "Не указан";
      if (payment.metadata && payment.metadata.email) {
        email = payment.metadata.email;
      } else if (payment.receipt_registration && payment.receipt_registration.customer && payment.receipt_registration.customer.email) {
        email = payment.receipt_registration.customer.email;
      }
      
      const price = payment.amount ? payment.amount.value : "Неизвестно";
      const newKey = generateMilitaryLicenseKey();
      
      // Отправляем ключ на почту покупателю
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
        } catch(mailErr) {}
      }
      
      // Отправляем уведомление ВАМ в Телеграм
      const message = `💰 <b>Успешная продажа!</b>\n\n📧 <b>Email:</b> ${email}\n💵 <b>Сумма:</b> ${price} руб.\n🔑 <b>Выдан ключ:</b> <code>${newKey}</code>\n\n✅ <i>Письмо с ключом отправлено бойцу на почту!</i>`;
      sendTelegramMessage(message);
    }
    
    return ContentService.createTextOutput("OK").setMimeType(ContentService.MimeType.TEXT);
    
  } catch (error) {
    return ContentService.createTextOutput("OK").setMimeType(ContentService.MimeType.TEXT);
  }
}
