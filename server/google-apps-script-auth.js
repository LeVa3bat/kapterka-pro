/**
 * KAPTERKA PRO — Web Auth v2 backend for STANDALONE Google Apps Script
 *
 * Не требует Google Таблиц.
 * Пользователи, одноразовые коды и сессии хранятся в Script Properties.
 * Пароли не используются и не хранятся.
 *
 * Публикация:
 * Deploy -> New deployment -> Web app
 * Execute as: Me
 * Who has access: Anyone
 */

const AUTH_VERSION = "2.1.0";
const CODE_TTL_MS = 10 * 60 * 1000;
const SESSION_TTL_MS = 30 * 24 * 60 * 60 * 1000;
const RESEND_COOLDOWN_SEC = 60;
const MAX_CODE_ATTEMPTS = 5;

function doGet(e) {
  const p = (e && e.parameter) || {};
  const action = String(p.action || "health").trim();

  try {
    ensureAuthPepper_();

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

    cleanupExpiredAuthData_();
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

  const existing = getUser_(email);
  if (mode === "register" && existing) return { ok: false, error: "ACCOUNT_EXISTS" };
  if (mode === "login" && !existing) return { ok: false, error: "USER_NOT_FOUND" };

  const cache = CacheService.getScriptCache();
  const throttleKey = "auth_code_" + sha256Hex_(email).slice(0, 24);
  if (cache.get(throttleKey)) {
    return { ok: false, error: "TOO_SOON", retryAfter: RESEND_COOLDOWN_SEC };
  }
  cache.put(throttleKey, "1", RESEND_COOLDOWN_SEC);

  const code = createSixDigitCode_();
  const now = Date.now();

  const pending = {
    email: email,
    codeHash: hashCode_(email, code),
    expiresAt: now + CODE_TTL_MS,
    attempts: 0,
    used: false,
    mode: mode,
    callsign: callsign,
    rank: rank,
    newsletter: newsletter,
    createdAt: now
  };
  setJsonProperty_(codeKey_(email), pending);

  MailApp.sendEmail({
    to: email,
    subject: "Код входа в Каптёрка PRO",
    name: "Каптёрка PRO",
    htmlBody:
      '<div style="font-family:Arial,sans-serif;max-width:520px;margin:auto;padding:24px;border:1px solid #dfe7e1;border-radius:14px">' +
      '<h2 style="margin:0 0 12px">Каптёрка PRO</h2>' +
      '<p style="color:#4d5b52">Ваш одноразовый код:</p>' +
      '<div style="font-size:32px;font-weight:800;letter-spacing:8px;margin:16px 0">' + code + '</div>' +
      '<p style="color:#68756d;font-size:13px">Код действует 10 минут и используется только один раз.</p>' +
      '<p style="color:#8b9690;font-size:12px">Если вы не запрашивали код, просто проигнорируйте письмо.</p>' +
      '</div>'
  });

  notifyAdmin_("🔐 Запрошен код сайта\nEmail: " + email + "\nРежим: " + mode);

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

  const key = codeKey_(email);
  const pending = getJsonProperty_(key);
  if (!pending) return { ok: false, error: "CODE_NOT_FOUND" };
  if (pending.used) return { ok: false, error: "CODE_USED" };
  if (Date.now() > Number(pending.expiresAt || 0)) {
    deleteProperty_(key);
    return { ok: false, error: "CODE_EXPIRED" };
  }
  if (Number(pending.attempts || 0) >= MAX_CODE_ATTEMPTS) {
    return { ok: false, error: "TOO_MANY_ATTEMPTS" };
  }

  pending.attempts = Number(pending.attempts || 0) + 1;
  setJsonProperty_(key, pending);

  if (hashCode_(email, code) !== String(pending.codeHash || "")) {
    return {
      ok: false,
      error: "WRONG_CODE",
      attemptsLeft: Math.max(0, MAX_CODE_ATTEMPTS - pending.attempts)
    };
  }

  pending.used = true;
  setJsonProperty_(key, pending);

  let user = getUser_(email);

  if (!user) {
    if (pending.mode !== "register") return { ok: false, error: "USER_NOT_FOUND" };

    const now = Date.now();
    user = {
      email: email,
      callsign: cleanText_(pending.callsign, 80) || "Пользователь",
      rank: cleanText_(pending.rank, 100),
      unitName: "",
      unitKey: "kapt_" + randomHex_(6),
      subscribedToNewsletter: !!pending.newsletter,
      status: "ACTIVE",
      createdAt: now,
      updatedAt: now
    };
    saveUser_(user);
  }

  const session = createSession_(email);
  notifyAdmin_("✅ Вход на сайт\nEmail: " + email + "\nПозывной: " + (user.callsign || "-"));

  return {
    ok: true,
    status: "AUTHENTICATED",
    sessionToken: session.token,
    sessionExpiresAt: session.expiresAt,
    user: publicUser_(user)
  };
}

function readSession_(p) {
  const rawToken = String(p.token || "").trim();
  if (!rawToken || rawToken.length < 32) return { ok: false, error: "INVALID_SESSION" };

  const tokenHash = sha256Hex_(rawToken);
  const session = getJsonProperty_(sessionKey_(tokenHash));
  if (!session) return { ok: false, error: "INVALID_SESSION" };

  if (Date.now() > Number(session.expiresAt || 0)) {
    deleteProperty_(sessionKey_(tokenHash));
    return { ok: false, error: "SESSION_EXPIRED" };
  }

  session.lastSeen = Date.now();
  setJsonProperty_(sessionKey_(tokenHash), session);

  const user = getUser_(session.email);
  if (!user) return { ok: false, error: "USER_NOT_FOUND" };

  return {
    ok: true,
    status: "AUTHENTICATED",
    sessionExpiresAt: session.expiresAt,
    user: publicUser_(user)
  };
}

function createSession_(email) {
  const token =
    Utilities.base64EncodeWebSafe(
      Utilities.computeDigest(
        Utilities.DigestAlgorithm.SHA_256,
        Utilities.getUuid() + "|" + Utilities.getUuid() + "|" + Date.now()
      )
    ).replace(/=+$/g, "") +
    "." +
    Utilities.getUuid().replace(/-/g, "");

  const expiresAt = Date.now() + SESSION_TTL_MS;
  const tokenHash = sha256Hex_(token);

  setJsonProperty_(sessionKey_(tokenHash), {
    email: email,
    expiresAt: expiresAt,
    createdAt: Date.now(),
    lastSeen: Date.now()
  });

  return { token: token, expiresAt: expiresAt };
}

function getUser_(email) {
  return getJsonProperty_(userKey_(email));
}

function saveUser_(user) {
  user.updatedAt = Date.now();
  setJsonProperty_(userKey_(user.email), user);
}

function publicUser_(user) {
  return {
    email: normalizeEmail_(user.email),
    callsign: cleanText_(user.callsign, 80),
    rank: cleanText_(user.rank, 100),
    unitName: cleanText_(user.unitName, 120),
    unitKey: cleanText_(user.unitKey, 80),
    subscribedToNewsletter: !!user.subscribedToNewsletter,
    status: cleanText_(user.status, 30) || "ACTIVE"
  };
}

function userKey_(email) {
  return "USR_" + sha256Hex_(normalizeEmail_(email));
}

function codeKey_(email) {
  return "CODE_" + sha256Hex_(normalizeEmail_(email));
}

function sessionKey_(tokenHash) {
  return "SES_" + tokenHash;
}

function setJsonProperty_(key, value) {
  PropertiesService.getScriptProperties().setProperty(key, JSON.stringify(value));
}

function getJsonProperty_(key) {
  const raw = PropertiesService.getScriptProperties().getProperty(key);
  if (!raw) return null;
  try { return JSON.parse(raw); } catch (_) { return null; }
}

function deleteProperty_(key) {
  PropertiesService.getScriptProperties().deleteProperty(key);
}

function cleanupExpiredAuthData_() {
  const props = PropertiesService.getScriptProperties();
  const all = props.getProperties();
  const now = Date.now();
  const toDelete = [];

  Object.keys(all).forEach(function(key) {
    if (key.indexOf("CODE_") === 0 || key.indexOf("SES_") === 0) {
      try {
        const item = JSON.parse(all[key]);
        const expiresAt = Number(item.expiresAt || 0);
        if (expiresAt && now > expiresAt) toDelete.push(key);
      } catch (_) {
        toDelete.push(key);
      }
    }
  });

  if (toDelete.length) {
    toDelete.forEach(function(key) {
      props.deleteProperty(key);
    });
  }
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
  return sha256Hex_(email + "|" + code + "|" + ensureAuthPepper_());
}

function ensureAuthPepper_() {
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
    .map(function(b) {
      return ("0" + ((b < 0 ? b + 256 : b).toString(16))).slice(-2);
    })
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
  return String(value == null ? "" : value)
    .replace(/[\u0000-\u001f<>]/g, "")
    .trim()
    .slice(0, maxLen || 120);
}

function maskEmail_(email) {
  const parts = email.split("@");
  if (parts.length !== 2) return email;
  const name = parts[0];
  const masked = name.length <= 2 ? (name[0] || "") + "*" : name.slice(0, 2) + "***";
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
