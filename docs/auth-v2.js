/**
 * KAPTERKA PRO — Web Auth v2 client
 * Passwordless email OTP. Activates only when KAPTERKA_AUTH_API_URL is configured.
 * Existing local profile/license values are preserved during migration.
 */
(function () {
  const TOKEN_KEY = "kapterka_web_auth_token_v2";
  const TOKEN_EXP_KEY = "kapterka_web_auth_token_exp_v2";
  const ACCOUNT_CACHE_PREFIX = "kapterka_account_cache_v2_";
  let pendingMode = null;
  let pendingEmail = null;
  let pendingRegister = null;

  function normalizeAccountEmail(value) {
    return String(value || "").trim().toLowerCase();
  }

  function accountCacheKey(email) {
    return ACCOUNT_CACHE_PREFIX + encodeURIComponent(normalizeAccountEmail(email));
  }

  function readAccountCache(email) {
    const normalized = normalizeAccountEmail(email);
    if (!normalized) return null;
    try {
      return JSON.parse(localStorage.getItem(accountCacheKey(normalized)) || "null");
    } catch (_) {
      return null;
    }
  }

  function snapshotAccount(email) {
    const normalized = normalizeAccountEmail(email || localStorage.getItem("kapterka_user_email"));
    if (!normalized) return;
    let keys = [];
    try { keys = JSON.parse(localStorage.getItem("kapterka_keys_history") || "[]"); } catch (_) {}
    const snapshot = {
      email: normalized,
      callsign: localStorage.getItem("kapterka_user_callsign") || "",
      rank: localStorage.getItem("kapterka_user_rank") || "",
      unitName: localStorage.getItem("kapterka_unit_name") || "",
      unitKey: localStorage.getItem("kapterka_unit_key") || "",
      phone: localStorage.getItem("kapterka_user_phone") || "",
      activeKey: localStorage.getItem("kapterka_active_key") || "",
      keys: Array.isArray(keys) ? keys : []
    };
    localStorage.setItem(accountCacheKey(normalized), JSON.stringify(snapshot));
  }

  function clearGenericProfileStorage() {
    [
      "kapterka_user_callsign",
      "kapterka_user_rank",
      "kapterka_unit_name",
      "kapterka_unit_key",
      "kapterka_user_email",
      "kapterka_user_phone",
      "kapterka_active_key",
      "kapterka_keys_history"
    ].forEach((key) => localStorage.removeItem(key));
  }

  function enabled() {
    return typeof window.KAPTERKA_AUTH_API_URL === "string" &&
      /^https:\/\/script\.google\.com\/macros\/s\/.+\/exec/.test(window.KAPTERKA_AUTH_API_URL);
  }

  function jsonp(params, timeoutMs) {
    return new Promise((resolve, reject) => {
      if (!enabled()) return reject(new Error("AUTH_BACKEND_NOT_CONFIGURED"));

      const cb = "__kaptAuthCb_" + Date.now() + "_" + Math.random().toString(36).slice(2);
      const script = document.createElement("script");
      const timer = setTimeout(() => {
        cleanup();
        reject(new Error("AUTH_TIMEOUT"));
      }, timeoutMs || 15000);

      function cleanup() {
        clearTimeout(timer);
        try { delete window[cb]; } catch (_) { window[cb] = undefined; }
        script.remove();
      }

      window[cb] = (data) => {
        cleanup();
        resolve(data || {});
      };

      const q = new URLSearchParams({ ...params, callback: cb, _ts: String(Date.now()) });
      script.src = window.KAPTERKA_AUTH_API_URL + "?" + q.toString();
      script.async = true;
      script.onerror = () => {
        cleanup();
        reject(new Error("AUTH_NETWORK_ERROR"));
      };
      document.head.appendChild(script);
    });
  }

  function setBusy(button, busy, busyText) {
    if (!button) return;
    if (busy) {
      button.dataset.originalText = button.textContent || "";
      button.disabled = true;
      if (busyText) button.textContent = busyText;
    } else {
      button.disabled = false;
      if (button.dataset.originalText) {
        button.textContent = button.dataset.originalText;
        delete button.dataset.originalText;
      }
    }
  }

  function startResendCooldown(button, seconds) {
    if (!button) return;
    const total = Number(seconds || 60);
    if (button._kapterkaCooldownTimer) clearInterval(button._kapterkaCooldownTimer);

    let left = total;
    const original = button.dataset.cooldownOriginalText || button.textContent || "Отправить ещё раз";
    button.dataset.cooldownOriginalText = original;
    button.disabled = true;
    button.textContent = `Повтор через ${left} сек`;

    button._kapterkaCooldownTimer = setInterval(() => {
      left -= 1;
      if (left <= 0) {
        clearInterval(button._kapterkaCooldownTimer);
        button._kapterkaCooldownTimer = null;
        button.disabled = false;
        button.textContent = original;
        return;
      }
      button.textContent = `Повтор через ${left} сек`;
    }, 1000);
  }

  function authErrorMessage(code, data) {
    const map = {
      INVALID_EMAIL: "Проверьте адрес электронной почты.",
      CALLSIGN_REQUIRED: "Укажите имя или позывной.",
      ACCOUNT_EXISTS: "Этот Email уже зарегистрирован. Переключитесь на «Вход».",
      USER_NOT_FOUND: "Аккаунт с таким Email не найден. Сначала зарегистрируйтесь.",
      TOO_SOON: "Код уже отправлен. Подождите около минуты и попробуйте снова.",
      INVALID_CODE: "Введите все 6 цифр кода.",
      CODE_NOT_FOUND: "Сначала запросите новый код.",
      CODE_USED: "Этот код уже использован. Запросите новый.",
      CODE_EXPIRED: "Срок действия кода истёк. Запросите новый.",
      TOO_MANY_ATTEMPTS: "Слишком много попыток. Запросите новый код.",
      WRONG_CODE: "Код не подходит. Проверьте письмо и попробуйте ещё раз.",
      SERVER_ERROR: "Сервер авторизации временно недоступен.",
      UNKNOWN_ACTION: "Сервис авторизации требует обновления."
    };
    if (code === "WRONG_CODE" && data && Number.isFinite(data.attemptsLeft)) {
      return map[code] + " Осталось попыток: " + data.attemptsLeft + ".";
    }
    return map[code] || "Не удалось выполнить вход. Попробуйте ещё раз.";
  }

  function notify(message) {
    if (typeof window.showToast === "function") window.showToast(message);
    else alert(message);
  }

  function ensureSixPins(container) {
    if (!container) return;
    const pinWrap = container.querySelector(".pin-inputs");
    if (!pinWrap) return;
    for (let n = 5; n <= 6; n++) {
      if (document.getElementById("pin" + n)) continue;
      const input = document.createElement("input");
      input.type = "text";
      input.inputMode = "numeric";
      input.autocomplete = "one-time-code";
      input.maxLength = 1;
      input.className = "pin-digit";
      input.id = "pin" + n;
      pinWrap.appendChild(input);
    }

    const pins = [...pinWrap.querySelectorAll(".pin-digit")];
    pins.forEach((el, idx) => {
      el.inputMode = "numeric";
      el.autocomplete = "one-time-code";
      el.oninput = () => {
        el.value = el.value.replace(/\D/g, "").slice(-1);
        if (el.value && pins[idx + 1]) pins[idx + 1].focus();
      };
      el.onkeydown = (ev) => {
        if (ev.key === "Backspace" && !el.value && pins[idx - 1]) pins[idx - 1].focus();
        if (ev.key === "Enter") {
          ev.preventDefault();
          verifyRegistrationCode();
        }
      };
      el.onpaste = (ev) => {
        const digits = (ev.clipboardData?.getData("text") || "").replace(/\D/g, "").slice(0, 6);
        if (digits.length > 1) {
          ev.preventDefault();
          digits.split("").forEach((d, i) => { if (pins[i]) pins[i].value = d; });
          pins[Math.min(digits.length, 6) - 1]?.focus();
          if (digits.length === 6) document.getElementById("regVerifyCodeBtn")?.focus();
        }
      };
    });
  }

  function readSixPins(root) {
    const scope = root || document;
    let code = "";
    for (let i = 1; i <= 6; i++) {
      const el = scope.querySelector("#pin" + i) || document.getElementById("pin" + i);
      code += (el?.value || "").replace(/\D/g, "");
    }
    return code.slice(0, 6);
  }

  function clearSixPins(root) {
    const scope = root || document;
    for (let i = 1; i <= 6; i++) {
      const el = scope.querySelector("#pin" + i) || document.getElementById("pin" + i);
      if (el) el.value = "";
    }
  }

  function migrateAndSetSession(serverUser, token, expiresAt) {
    const targetEmail = normalizeAccountEmail(serverUser?.email || pendingEmail || "");
    const currentSession = typeof window.getActiveUserSession === "function" ? window.getActiveUserSession() : null;
    const currentEmail = normalizeAccountEmail(currentSession?.email || localStorage.getItem("kapterka_user_email") || "");

    // Persist the current account before switching to another one.
    if (currentEmail && targetEmail && currentEmail !== targetEmail) {
      snapshotAccount(currentEmail);
      clearGenericProfileStorage();
    }

    const cached = targetEmail ? readAccountCache(targetEmail) : null;
    const sameGenericOwner = !!targetEmail && normalizeAccountEmail(localStorage.getItem("kapterka_user_email")) === targetEmail;

    let legacyKeys = [];
    if (sameGenericOwner) {
      try { legacyKeys = JSON.parse(localStorage.getItem("kapterka_keys_history") || "[]"); } catch (_) {}
    }

    const cachedKeys = Array.isArray(cached?.keys) ? cached.keys : [];
    const keys = cachedKeys.length ? cachedKeys : (Array.isArray(legacyKeys) ? legacyKeys : []);
    const activeKey =
      cached?.activeKey ||
      (sameGenericOwner ? localStorage.getItem("kapterka_active_key") || "" : "") ||
      "";

    if (!keys.length && activeKey) {
      keys.push({ key: activeKey, date: new Date().toLocaleDateString("ru-RU"), plan: "PRO" });
    }

    const user = {
      ...(serverUser || {}),
      email: targetEmail || normalizeAccountEmail(currentSession?.email) || "",
      callsign: cached?.callsign || (sameGenericOwner ? localStorage.getItem("kapterka_user_callsign") || "" : "") || serverUser?.callsign || "Пользователь",
      rank: cached?.rank || (sameGenericOwner ? localStorage.getItem("kapterka_user_rank") || "" : "") || serverUser?.rank || "",
      unitName: cached?.unitName || (sameGenericOwner ? localStorage.getItem("kapterka_unit_name") || "" : "") || serverUser?.unitName || "",
      unitKey: cached?.unitKey || (sameGenericOwner ? localStorage.getItem("kapterka_unit_key") || "" : "") || serverUser?.unitKey || "",
      phone: cached?.phone || (sameGenericOwner ? localStorage.getItem("kapterka_user_phone") || "" : "") || "",
      activeKey: activeKey,
      keys: keys,
      emailVerified: true,
      authProvider: "email_otp_v2",
      webAuthV2: true
    };

    localStorage.setItem(TOKEN_KEY, token || "");
    localStorage.setItem(TOKEN_EXP_KEY, String(expiresAt || 0));

    if (typeof window.setUserSession === "function") {
      window.setUserSession(user);
    } else {
      localStorage.setItem("kapterka_auth_user", JSON.stringify(user));
      localStorage.setItem("kapterka_user_email", user.email || "");
      if (typeof window.updateAuthUI === "function") window.updateAuthUI();
      if (typeof window.loadCabinetProfile === "function") window.loadCabinetProfile();
    }

    snapshotAccount(user.email);
  }

  function showRegistrationVerify() {
    const stepInputs = document.getElementById("regStepInputs");
    const stepVerif = document.getElementById("regStepVerification");
    if (stepInputs) stepInputs.style.display = "none";
    if (stepVerif) {
      stepVerif.style.display = "block";
      ensureSixPins(stepVerif);
      const title = stepVerif.querySelector("h4");
      if (title) title.textContent = "Введите код из Email";
      const hint = document.getElementById("verificationHint");
      if (hint) hint.textContent = "Код действует 10 минут и используется только один раз.";
      const disp = document.getElementById("verifyEmailDisplay");
      if (disp) disp.textContent = pendingEmail || "";
      clearSixPins(stepVerif);
      document.getElementById("pin1")?.focus();
    }
  }

  function ensureLoginVerifyBox() {
    const panel = document.getElementById("panelLogin");
    if (!panel) return null;
    let box = document.getElementById("loginOtpVerifyBox");
    if (box) return box;

    box = document.createElement("div");
    box.id = "loginOtpVerifyBox";
    box.className = "pin-verify-box auth-v2-login-verify";
    box.style.display = "none";
    box.innerHTML = [
      '<div class="auth-v2-code-title">Введите код из письма</div>',
      '<div class="auth-v2-code-email" id="loginVerifyEmail"></div>',
      '<div class="pin-inputs">',
      '<input type="text" maxlength="1" class="pin-digit" id="loginPin1">',
      '<input type="text" maxlength="1" class="pin-digit" id="loginPin2">',
      '<input type="text" maxlength="1" class="pin-digit" id="loginPin3">',
      '<input type="text" maxlength="1" class="pin-digit" id="loginPin4">',
      '<input type="text" maxlength="1" class="pin-digit" id="loginPin5">',
      '<input type="text" maxlength="1" class="pin-digit" id="loginPin6">',
      '</div>',
      '<div class="auth-v2-login-actions">',
      '<button class="btn btn-primary btn-lg" id="loginVerifyCodeBtn">Войти</button>',
      '<button class="btn btn-outline btn-sm" id="loginResendCodeBtn">Отправить код ещё раз</button>',
      '</div>'
    ].join("");

    panel.appendChild(box);

    const pins = [...box.querySelectorAll(".pin-digit")];
    pins.forEach((el, idx) => {
      el.inputMode = "numeric";
      el.autocomplete = "one-time-code";
      el.oninput = () => {
        el.value = el.value.replace(/\D/g, "").slice(-1);
        if (el.value && pins[idx + 1]) pins[idx + 1].focus();
      };
      el.onkeydown = ev => {
        if (ev.key === "Backspace" && !el.value && pins[idx - 1]) pins[idx - 1].focus();
        if (ev.key === "Enter") {
          ev.preventDefault();
          verifyLoginCode();
        }
      };
      el.onpaste = ev => {
        const digits = (ev.clipboardData?.getData("text") || "").replace(/\D/g, "").slice(0, 6);
        if (digits.length > 1) {
          ev.preventDefault();
          digits.split("").forEach((d, i) => { if (pins[i]) pins[i].value = d; });
          if (digits.length === 6) document.getElementById("loginVerifyCodeBtn")?.focus();
        }
      };
    });

    document.getElementById("loginVerifyCodeBtn").onclick = verifyLoginCode;
    document.getElementById("loginResendCodeBtn").onclick = requestLoginCode;
    return box;
  }

  function readLoginCode() {
    let code = "";
    for (let i = 1; i <= 6; i++) code += document.getElementById("loginPin" + i)?.value || "";
    return code.replace(/\D/g, "").slice(0, 6);
  }

  function adaptUi() {
    if (!enabled()) return;

    const regPassword = document.getElementById("regPassword");
    if (regPassword) {
      const group = regPassword.closest(".form-group");
      if (group) group.style.display = "none";
    }
    const loginPassword = document.getElementById("loginPassword");
    if (loginPassword) {
      const group = loginPassword.closest(".form-group");
      if (group) group.style.display = "none";
    }

    const regBtn = document.querySelector("#regStepInputs button[onclick*='startRegistrationProcess']");
    if (regBtn) regBtn.textContent = "Получить код на Email";

    const loginBtn = document.querySelector("#panelLogin button[onclick*='processUserLogin']");
    if (loginBtn) loginBtn.textContent = "Получить код для входа";

    const demoLink = document.querySelector("#panelLogin a[onclick*='quickDemoLogin']");
    if (demoLink) demoLink.style.display = "none";

    const regText = document.querySelector("#panelRegister > div:first-child p");
    if (regText) regText.textContent = "Без пароля: подтвердите Email одноразовым кодом и войдите в кабинет.";

    const loginText = document.querySelector("#panelLogin > div:first-child p");
    if (loginText) loginText.textContent = "Введите Email — мы отправим одноразовый 6-значный код.";

    ensureSixPins(document.getElementById("regStepVerification"));
    ensureLoginVerifyBox();

    const regEmail = document.getElementById("regEmail");
    if (regEmail && !regEmail.dataset.kapterkaEnterBound) {
      regEmail.dataset.kapterkaEnterBound = "1";
      regEmail.addEventListener("keydown", (ev) => {
        if (ev.key === "Enter") {
          ev.preventDefault();
          requestRegistrationCode();
        }
      });
    }

    const loginEmail = document.getElementById("loginEmail");
    if (loginEmail && !loginEmail.dataset.kapterkaEnterBound) {
      loginEmail.dataset.kapterkaEnterBound = "1";
      loginEmail.addEventListener("keydown", (ev) => {
        if (ev.key === "Enter") {
          ev.preventDefault();
          requestLoginCode();
        }
      });
    }
  }

  async function requestRegistrationCode() {
    const actionBtn = document.querySelector("#regStepInputs button[onclick*='startRegistrationProcess']");
    const callsign = document.getElementById("regCallsign")?.value.trim() || "";
    const email = document.getElementById("regEmail")?.value.trim().toLowerCase() || "";
    const rank = document.getElementById("regRank")?.value.trim() || "";
    const newsletter = document.getElementById("regNewsletterCheck")?.checked !== false;

    if (!callsign) return notify("Укажите имя или позывной.");
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) || email.length > 160) return notify("Проверьте Email.");

    pendingMode = "register";
    pendingEmail = email;
    pendingRegister = { callsign, rank, newsletter };

    try {
      setBusy(actionBtn, true, "Отправляю код…");
      notify("Отправляю код подтверждения...");
      const data = await jsonp({
        action: "auth_request_code",
        mode: "register",
        email,
        callsign,
        rank,
        newsletter: newsletter ? "1" : "0"
      });
      if (!data.ok) return notify(authErrorMessage(data.error, data));
      showRegistrationVerify();
      startResendCooldown(document.getElementById("regResendCodeBtn"), 60);
      notify("Код отправлен на " + (data.maskedEmail || email));
    } catch (err) {
      console.warn(err);
      notify("Не удалось связаться с сервером авторизации.");
    } finally {
      setBusy(actionBtn, false);
    }
  }

  async function requestLoginCode() {
    const actionBtn = document.querySelector("#panelLogin button[onclick*='processUserLogin']");
    const email = document.getElementById("loginEmail")?.value.trim().toLowerCase() || pendingEmail || "";
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) || email.length > 160) return notify("Проверьте Email.");

    pendingMode = "login";
    pendingEmail = email;
    pendingRegister = null;

    try {
      setBusy(actionBtn, true, "Отправляю код…");
      notify("Отправляю код для входа...");
      const data = await jsonp({ action: "auth_request_code", mode: "login", email });
      if (!data.ok) return notify(authErrorMessage(data.error, data));

      const box = ensureLoginVerifyBox();
      if (box) {
        box.style.display = "block";
        const label = document.getElementById("loginVerifyEmail");
        if (label) label.textContent = "Код отправлен на " + (data.maskedEmail || email);
        for (let i = 1; i <= 6; i++) {
          const el = document.getElementById("loginPin" + i);
          if (el) el.value = "";
        }
        document.getElementById("loginPin1")?.focus();
      }
      startResendCooldown(document.getElementById("loginResendCodeBtn"), 60);
      notify("Проверьте почту.");
    } catch (err) {
      console.warn(err);
      notify("Не удалось связаться с сервером авторизации.");
    } finally {
      setBusy(actionBtn, false);
    }
  }

  async function verifyCode(code, mode) {
    if (!pendingEmail) return notify("Сначала запросите код.");
    if (!code || code.length !== 6) return notify("Введите все 6 цифр.");

    try {
      notify("Проверяю код...");
      const data = await jsonp({
        action: "auth_verify_code",
        email: pendingEmail,
        code
      });
      if (!data.ok) return notify(authErrorMessage(data.error, data));

      migrateAndSetSession(data.user || {}, data.sessionToken || "", data.sessionExpiresAt || 0);
      pendingMode = null;
      pendingEmail = null;
      pendingRegister = null;

      document.getElementById("regStepInputs")?.style && (document.getElementById("regStepInputs").style.display = "block");
      document.getElementById("regStepVerification")?.style && (document.getElementById("regStepVerification").style.display = "none");
      const loginBox = document.getElementById("loginOtpVerifyBox");
      if (loginBox) loginBox.style.display = "none";

      if (typeof window.trackYm === "function") {
        try {
          window.trackYm("reachGoal", mode === "register" ? "registration_complete" : "login_complete");
        } catch (_) {}
      }
      if (typeof window.gtag === "function") {
        try {
          window.gtag("event", mode === "register" ? "sign_up" : "login", { method: "email_otp" });
        } catch (_) {}
      }

      notify(mode === "register" ? "Аккаунт создан. Добро пожаловать!" : "Вход выполнен.");
    } catch (err) {
      console.warn(err);
      notify("Не удалось проверить код. Попробуйте ещё раз.");
    }
  }

  async function verifyRegistrationCode() {
    const code = readSixPins(document.getElementById("regStepVerification"));
    return verifyCode(code, "register");
  }

  async function verifyLoginCode() {
    return verifyCode(readLoginCode(), "login");
  }

  async function refreshExistingSession() {
    const token = localStorage.getItem(TOKEN_KEY) || "";
    const expiresAt = Number(localStorage.getItem(TOKEN_EXP_KEY) || 0);
    if (!token) return;

    if (expiresAt && Date.now() > expiresAt) {
      logoutV2();
      notify("Сессия завершена. Войдите снова по коду из Email.");
      return;
    }

    try {
      const data = await jsonp({ action: "auth_session", token: token }, 12000);

      if (data && data.ok && data.user) {
        migrateAndSetSession(data.user, token, data.sessionExpiresAt || expiresAt);
        return;
      }

      if (data && ["INVALID_SESSION", "SESSION_EXPIRED", "USER_NOT_FOUND"].includes(data.error)) {
        logoutV2();
        notify("Сессия завершена. Войдите снова по коду из Email.");
      }
    } catch (err) {
      console.warn("Web Auth session refresh skipped:", err);
      // При временной ошибке сети локальную сессию не сбрасываем.
    }
  }

  function logoutV2() {
    const current = typeof window.getActiveUserSession === "function" ? window.getActiveUserSession() : null;
    snapshotAccount(current?.email || localStorage.getItem("kapterka_user_email"));

    localStorage.removeItem("kapterka_auth_user");
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(TOKEN_EXP_KEY);
    // Pending payment state must never leak into another account.
    localStorage.removeItem("kapterka_pending_callsign");
    localStorage.removeItem("kapterka_pending_key");
    localStorage.removeItem("kapterka_pending_payment_id");
    localStorage.removeItem("kapterka_verified_key");
    clearGenericProfileStorage();

    if (typeof window.updateAuthUI === "function") window.updateAuthUI();
    if (typeof window.loadCabinetProfile === "function") window.loadCabinetProfile();
    notify("Вы вышли из личного кабинета.");
  }

  function activate() {
    if (!enabled()) return;
    adaptUi();

    window.startRegistrationProcess = requestRegistrationCode;
    window.processUserLogin = requestLoginCode;
    window.verifyEmailPinCode = verifyRegistrationCode;
    window.resendPinCode = function () {
      return pendingMode === "login" ? requestLoginCode() : requestRegistrationCode();
    };
    window.logoutUserSession = logoutV2;

    document.documentElement.classList.add("auth-v2-enabled");
    refreshExistingSession();
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", activate);
  } else {
    activate();
  }
})();
