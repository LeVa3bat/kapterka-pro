/**
 * KAPTERKA PRO — Google Apps Script Server Handler
 * Обработчик заявок, платежей и выдачи лицензионных ключей через Google Таблицу и Telegram
 */

// Конфигурация
const TG_BOT_TOKEN = PropertiesService.getScriptProperties().getProperty("TG_BOT_TOKEN") || "";
const TG_ADMIN_CHAT_ID = PropertiesService.getScriptProperties().getProperty("TG_ADMIN_CHAT_ID") || "";
const CHECKSUM_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";

function computeKeyChecksum(p1, p2) {
  const s = "KAPT-" + p1 + "-" + p2 + "-KAPT3RKA_881_MILITARY";
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

function generateLicenseKey() {
  function rnd(len) {
    let res = "";
    for (let i = 0; i < len; i++) {
      res += CHECKSUM_CHARS[Math.floor(Math.random() * CHECKSUM_CHARS.length)];
    }
    return res;
  }
  const p1 = rnd(4);
  const p2 = rnd(4);
  const chk = computeKeyChecksum(p1, p2);
  return "KAPT-" + p1 + "-" + p2 + "-" + chk;
}

function doPost(e) {
  try {
    const postData = e.postData && e.postData.contents ? JSON.parse(e.postData.contents) : {};
    const action = postData.action || "payment";
    
    if (action === "create_license" || action === "payment") {
      const email = postData.email || "unknown@kapterka-pro.ru";
      const callsign = postData.callsign || postData.name || "Боец";
      const plan = postData.plan || "PRO";
      const key = generateLicenseKey();
      
      // Логирование в Google Sheet
      const sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
      sheet.appendRow([new Date(), email, callsign, plan, key, "ACTIVE"]);
      
      // Оповещение администратора в Telegram
      sendTgMessage(
        "🎖 <b>Выдана новая лицензия Каптёрка Про!</b>\n\n" +
        "👤 Позывной: " + callsign + "\n" +
        "✉️ Email: " + email + "\n" +
        "🔑 Ключ: <code>" + key + "</code>\n" +
        "📦 Тариф: " + plan
      );
      
      return ContentService.createTextOutput(JSON.stringify({
        status: "success",
        key: key,
        email: email,
        plan: plan
      })).setMimeType(ContentService.MimeType.JSON);
    }
    
    return ContentService.createTextOutput(JSON.stringify({ status: "ok" }))
      .setMimeType(ContentService.MimeType.JSON);
  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({
      status: "error",
      message: err.toString()
    })).setMimeType(ContentService.MimeType.JSON);
  }
}

function doGet(e) {
  return ContentService.createTextOutput(JSON.stringify({
    status: "online",
    service: "Kapterka Pro Apps Script Backend",
    version: "3.4.8"
  })).setMimeType(ContentService.MimeType.JSON);
}

function sendTgMessage(htmlText) {
  if (!TG_BOT_TOKEN || !TG_ADMIN_CHAT_ID) {
    console.warn("Telegram notification skipped: Script Properties are not configured.");
    return;
  }
  const url = "https://api.telegram.org/bot" + TG_BOT_TOKEN + "/sendMessage";
  const payload = {
    chat_id: TG_ADMIN_CHAT_ID,
    text: htmlText,
    parse_mode: "HTML"
  };
  UrlFetchApp.fetch(url, {
    method: "post",
    contentType: "application/json",
    payload: JSON.stringify(payload),
    muteHttpExceptions: true
  });
}
