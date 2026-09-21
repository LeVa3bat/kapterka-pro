/**
 * KAPTERKA PRO — Web Auth backend (Google Apps Script)
 *
 * Отдельный web-app только для регистрации/входа по одноразовому коду Email.
 * Пароли не хранятся и не передаются.
 *
 * Script Properties:
 *   AUTH_PEPPER        — создаётся автоматически при первом запуске
 *   TG_BOT_TOKEN       — необязательно, только для служебных уведомлений
 *   TG_ADMIN_CHAT_ID   — необязательно
 *
 * Script должен быть привязан к Google Spreadsheet.
 */

const AUTH_VERSION = "1.0.0";
const USERS_SHEET = "WebUsers";
const CODES_SHEET = "WebAuthCodes";
const SESSIONS_SHEET = "WebSessions";
const CODE_TTL_MS = 10 * 60 * 1000;
const SESSION_TTL_MS = 30 * 24 * 60 * 60 * 1000;
const RESEND_COOLDOWN_SEC = 60;
const MAX_CODE_ATTEMPTS = 5;

function doGet(e) {
  const p = (e && e.parameter) || {};
  const action = String(p.action || "health").trim();

  try {
    ensureAuthSheets_();

    let result;
    if (action === "health") {
      result = { ok: true, service: "kapterka-web-auth", version: AUTH_VERSION };
    } else if (action === "auth_request_code") {
      result = requestAuthCode_(p);
    } else if (action === "auth_verify_code") {
      result = verifyAuthCode_(p);
    } else if (action === "auth_session") {
      result = readSession_(p);
    } else {
      result = { ok: false, error: "UNKNOWN_ACTION" };
    }
    return jsonResponse_(result, p.callback);
  } catch (err) {
    console.error(err && err.stack ? err.stack : err);
    return jsonResponse_({ ok: false, error: "SERVER_ERROR" }, p.callback);
  }
}

function requestAuthCode_(p) {
  const mode = String(p.mode || "login").toLowerCase() === "register" ? "register" : "login";
  const email = normalizeEmail_(p.email);
  const callsign = cleanText_(p.callsign, 80);
  const rank = cleanText_(p.rank, 100);
  const newsletter = String(p.newsletter || "1") !== "0";

  if (!isValidEmail_(email)) return { ok: false, error: "INVALID_EMAIL" };
  if (mode === "register" && !callsign) return { ok: false, error: "CALLSIGN_REQUIRED" };

  const users = getUsersSheet_();
  const existing = findUserByEmail_(users, email);

  if (mode === "register" && existing) {
    return { ok: false, error: "ACCOUNT_EXISTS" };
  }
  if (mode === "login" && !existing) {
    return { ok: false, error: "USER_NOT_FOUND" };
  }

  const cache = CacheService.getScriptCache();
  const throttleKey = "auth_code_" + sha256Hex_(email).slice(0, 24);
  if (cache.get(throttleKey)) {
    return { ok: false, error: "TOO_SOON", retryAfter: RESEND_COOLDOWN_SEC };
  }
  cache.put(throttleKey, "1", RESEND_COOLDOWN_SEC);

  const code = createSixDigitCode_();
  const codeHash = hashCode_(email, code);
  const now = Date.now();

  const codes = getCodesSheet_();
  codes.appendRow([
    email,
    codeHash,
    now + CODE_TTL_MS,
    0,
    false,
    mode,
    callsign,
    rank,
    newsletter ? "1" : "0",
    new Date(now)
  ]);

  MailApp.sendEmail({
    to: email,
    subject: "Код входа в Каптёрка PRO",
    name: "Каптёрка PRO",
    htmlBody:
      '<div style="font-family:Arial,sans-serif;max-width:520px;margin:auto;padding:24px;border:1px solid #dfe7e1;border-radius:14px">' +
      '<h2 style="margin:0 0 12px">Каптёрка PRO</h2>' +
      '<p style="color:#4d5b52">Код подтверждения:</p>' +
      '<div style="font-size:32px;font-weight:800;letter-spacing:8px;margin:16px 0">' + code + '</div>' +
      '<p style="color:#68756d;font-size:13px">Код действует 10 минут и используется только один раз.</p>' +
      '<p style="color:#8b9690;font-size:12px">Если вы не запрашивали код, просто проигнорируйте письмо.</p>' +
      '</div>'
  });

  notifyAdmin_("🔐 Запрошен код сайта\\nEmail: " + email + "\\nРежим: " + mode);

  return {
    ok: true,
    status: "CODE_SENT",
    maskedEmail: maskEmail_(email),
    expiresIn: Math.floor(CODE_TTL_MS / 1000)
  };
}

function verifyAuthCode_(p) {
  const email = normalizeEmail_(p.email);
  const code = String(p.code || "").replace(/\D/g, "").slice(0, 6);

  if (!isValidEmail_(email)) return { ok: false, error: "INVALID_EMAIL" };
  if (code.length !== 6) return { ok: false, error: "INVALID_CODE" };

  const codes = getCodesSheet_();
  const row = findLatestCodeRow_(codes, email);
  if (!row) return { ok: false, error: "CODE_NOT_FOUND" };

  const [rowIndex, values] = row;
  const expiresAt = Number(values[2] || 0);
  const attempts = Number(values[3] || 0);
  const used = String(values[4]).toLowerCase() === "true";

  if (used) return { ok: false, error: "CODE_USED" };
  if (Date.now() > expiresAt) return { ok: false, error: "CODE_EXPIRED" };
  if (attempts >= MAX_CODE_ATTEMPTS) return { ok: false, error: "TOO_MANY_ATTEMPTS" };

  codes.getRange(rowIndex, 4).setValue(attempts + 1);

  if (hashCode_(email, code) !== String(values[1] || "")) {
    return { ok: false, error: "WRONG_CODE", attemptsLeft: MAX_CODE_ATTEMPTS - attempts - 1 };
  }

  codes.getRange(rowIndex, 5).setValue(true);

  const mode = String(values[5] || "login");
  const callsign = cleanText_(values[6], 80);
  const rank = cleanText_(values[7], 100);
  const newsletter = String(values[8] || "1") === "1";

  const users = getUsersSheet_();
  let user = findUserByEmail_(users, email);

  if (!user) {
    if (mode !== "register") return { ok: false, error: "USER_NOT_FOUND" };
    const now = new Date();
    const unitKey = "kapt_" + randomHex_(6);
    users.appendRow([
      email,
      callsign || "Пользователь",
      rank || "",
      "",
      unitKey,
      newsletter ? "1" : "0",
      "ACTIVE",
      now,
      now
    ]);
    user = findUserByEmail_(users, email);
  }

  const session = createSession_(email);
  notifyAdmin_("✅ Вход на сайт\\nEmail: " + email + "\\nПозывной: " + (user.callsign || "-"));

  return {
    ok: true,
    status: "AUTHENTICATED",
    sessionToken: session.token,
    sessionExpiresAt: session.expiresAt,
    user: user
  };
}

function readSession_(p) {
  const rawToken = String(p.token || "").trim();
  if (!rawToken || rawToken.length < 32) return { ok: false, error: "INVALID_SESSION" };

  const tokenHash = sha256Hex_(rawToken);
  const sessions = getSessionsSheet_();
  const data = sessions.getDataRange().getValues();

  for (let i = data.length - 1; i >= 1; i--) {
    if (String(data[i][1] || "") !== tokenHash) continue;
    const expiresAt = Number(data[i][2] || 0);
    if (Date.now() > expiresAt) return { ok: false, error: "SESSION_EXPIRED" };

    const email = normalizeEmail_(data[i][0]);
    sessions.getRange(i + 1, 5).setValue(new Date());

    const user = findUserByEmail_(getUsersSheet_(), email);
    if (!user) return { ok: false, error: "USER_NOT_FOUND" };

    return {
      ok: true,
      status: "AUTHENTICATED",
      sessionExpiresAt: expiresAt,
      user: user
    };
  }

  return { ok: false, error: "INVALID_SESSION" };
}

function createSession_(email) {
  const token = Utilities.base64EncodeWebSafe(
    Utilities.computeDigest(
      Utilities.DigestAlgorithm.SHA_256,
      Utilities.getUuid() + "|" + Utilities.getUuid() + "|" + Date.now()
    )
  ).replace(/=+$/g, "") + "." + Utilities.getUuid().replace(/-/g, "");

  const expiresAt = Date.now() + SESSION_TTL_MS;
  getSessionsSheet_().appendRow([
    email,
    sha256Hex_(token),
    expiresAt,
    new Date(),
    new Date()
  ]);

  return { token: token, expiresAt: expiresAt };
}

function ensureAuthSheets_() {
  getOrCreateSheet_(USERS_SHEET, [
    "email","callsign","rank","unitName","unitKey","newsletter","status","createdAt","updatedAt"
  ]);
  getOrCreateSheet_(CODES_SHEET, [
    "email","codeHash","expiresAt","attempts","used","mode","callsign","rank","newsletter","createdAt"
  ]);
  getOrCreateSheet_(SESSIONS_SHEET, [
    "email","tokenHash","expiresAt","createdAt","lastSeen"
  ]);
  getPepper_();
}

function getUsersSheet_() { return getOrCreateSheet_(USERS_SHEET, []); }
function getCodesSheet_() { return getOrCreateSheet_(CODES_SHEET, []); }
function getSessionsSheet_() { return getOrCreateSheet_(SESSIONS_SHEET, []); }

function getOrCreateSheet_(name, headers) {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  if (!ss) throw new Error("Script must be bound to a Google Spreadsheet.");
  let sh = ss.getSheetByName(name);
  if (!sh) {
    sh = ss.insertSheet(name);
    if (headers && headers.length) sh.appendRow(headers);
  } else if (headers && headers.length && sh.getLastRow() === 0) {
    sh.appendRow(headers);
  }
  return sh;
}

function findUserByEmail_(sheet, email) {
  const data = sheet.getDataRange().getValues();
  for (let i = data.length - 1; i >= 1; i--) {
    if (normalizeEmail_(data[i][0]) !== email) continue;
    return {
      email: email,
      callsign: cleanText_(data[i][1], 80),
      rank: cleanText_(data[i][2], 100),
      unitName: cleanText_(data[i][3], 120),
      unitKey: cleanText_(data[i][4], 80),
      subscribedToNewsletter: String(data[i][5] || "0") === "1",
      status: cleanText_(data[i][6], 30) || "ACTIVE"
    };
  }
  return null;
}

function findLatestCodeRow_(sheet, email) {
  const data = sheet.getDataRange().getValues();
  for (let i = data.length - 1; i >= 1; i--) {
    if (normalizeEmail_(data[i][0]) === email) return [i + 1, data[i]];
  }
  return null;
}

function createSixDigitCode_() {
  const bytes = Utilities.computeDigest(
    Utilities.DigestAlgorithm.SHA_256,
    Utilities.getUuid() + "|" + Date.now() + "|" + Math.random()
  );
  let n = 0;
  for (let i = 0; i < 4; i++) n = ((n << 8) | (bytes[i] & 0xff)) >>> 0;
  return String(100000 + (n % 900000));
}

function hashCode_(email, code) {
  return sha256Hex_(email + "|" + code + "|" + getPepper_());
}

function getPepper_() {
  const props = PropertiesService.getScriptProperties();
  let value = props.getProperty("AUTH_PEPPER");
  if (!value) {
    value = Utilities.getUuid() + Utilities.getUuid();
    props.setProperty("AUTH_PEPPER", value);
  }
  return value;
}

function sha256Hex_(value) {
  return Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256, String(value))
    .map(function(b) { return ("0" + ((b < 0 ? b + 256 : b).toString(16))).slice(-2); })
    .join("");
}

function randomHex_(len) {
  return sha256Hex_(Utilities.getUuid() + "|" + Date.now()).slice(0, len);
}

function normalizeEmail_(value) {
  return String(value || "").trim().toLowerCase();
}

function isValidEmail_(email) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) && email.length <= 160;
}

function cleanText_(value, maxLen) {
  return String(value == null ? "" : value).replace(/[\u0000-\u001f<>]/g, "").trim().slice(0, maxLen || 120);
}

function maskEmail_(email) {
  const parts = email.split("@");
  if (parts.length !== 2) return email;
  const name = parts[0];
  const masked = name.length <= 2 ? name[0] + "*" : name.slice(0, 2) + "***";
  return masked + "@" + parts[1];
}

function jsonResponse_(obj, callback) {
  const json = JSON.stringify(obj);
  if (callback && /^[A-Za-z_$][0-9A-Za-z_$]{0,64}$/.test(callback)) {
    return ContentService.createTextOutput(callback + "(" + json + ");")
      .setMimeType(ContentService.MimeType.JAVASCRIPT);
  }
  return ContentService.createTextOutput(json)
    .setMimeType(ContentService.MimeType.JSON);
}

function notifyAdmin_(text) {
  const props = PropertiesService.getScriptProperties();
  const token = props.getProperty("TG_BOT_TOKEN") || "";
  const chatId = props.getProperty("TG_ADMIN_CHAT_ID") || "";
  if (!token || !chatId) return;

  try {
    UrlFetchApp.fetch("https://api.telegram.org/bot" + token + "/sendMessage", {
      method: "post",
      contentType: "application/json",
      payload: JSON.stringify({ chat_id: chatId, text: text }),
      muteHttpExceptions: true
    });
  } catch (err) {
    console.warn("Telegram notify failed: " + err);
  }
}
