/**
 * KAPTERKA PRO — Web Auth v3 client (Email-only OTP, no Google Sign-In)
 * Primary transport: POST JSON to Cloudflare Worker (KAPTERKA_PAYMENT_API_URL).
 * Seamless fallback: legacy GAS endpoint ONLY if Worker has not yet deployed web_auth_* actions.
 * Existing local profile/license values are preserved during migration.
 */
(function () {
  const TOKEN_KEY = "kapterka_web_auth_token_v2";
  const TOKEN_EXP_KEY = "kapterka_web_auth_token_exp_v2";
  const PROVIDER_KEY = "kapterka_web_auth_provider_v2";
  const ACCOUNT_CACHE_PREFIX = "kapterka_account_cache_v2_";
  let pendingMode = null;
  let pendingEmail = null;
  let pendingRegister = null;
  let pendingTransport = "worker";
  let verifyInFlight = false;

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

  function workerBaseUrl() {
    const url = typeof window.KAPTERKA_PAYMENT_API_URL === "string" ? window.KAPTERKA_PAYMENT_API_URL.trim().replace(/\/+$/, "") : "";
    return /^https:\/\//.test(url) ? url : "";
  }

  function legacyGasEnabled() {
    return typeof window.KAPTERKA_AUTH_API_URL === "string" &&
      /^https:\/\/script\.google\.com\/macros\/s\/.+\/exec/.test(window.KAPTERKA_AUTH_API_URL);
  }

  function enabled() {
    return Boolean(workerBaseUrl() || legacyGasEnabled());
  }

  async function postWorker(action, payload, timeoutMs) {
    const base = workerBaseUrl();
    if (!base) return { ok: false, error: "AUTH_BACKEND_NOT_CONFIGURED" };
    const controller = typeof AbortController !== "undefined" ? new AbortController() : null;
    const timer = controller ? setTimeout(() => controller.abort(), timeoutMs || 35000) : null;
    try {
      const response = await fetch(base + "?action=" + encodeURIComponent(action), {
        method: "POST",
        cache: "no-store",
        credentials: "omit",
        headers: { "Content-Type": "application/json", "Accept": "application/json" },
        body: JSON.stringify(payload || {}),
        signal: controller?.signal
      });
      const data = await response.json().catch(() => null);
      if (!data || typeof data !== "object") {
        return { ok: false, error: "SERVER_ERROR", httpStatus: response.status };
      }
      return { ...data, httpStatus: response.status };
    } catch (err) {
      throw new Error(err?.name === "AbortError" ? "AUTH_TIMEOUT" : "AUTH_NETWORK_ERROR");
    } finally {
      if (timer) clearTimeout(timer);
    }
  }

  function jsonpLegacy(params, timeoutMs) {
    return new Promise((resolve, reject) => {
      if (!legacyGasEnabled()) return reject(new Error("AUTH_BACKEND_NOT_CONFIGURED"));

      const cb = "__kaptAuthCb_" + Date.now() + "_" + Math.random().toString(36).slice(2);
      const script = document.createElement("script");
      const timer = setTimeout(() => {
        cleanup();
        reject(new Error("AUTH_TIMEOUT"));
      }, timeoutMs || 18000);

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

  async function requestAuthApi(workerAction, workerPayload, legacyParams, timeoutMs) {
    if (workerBaseUrl()) {
      const data = await postWorker(workerAction, workerPayload, timeoutMs);
      if (data && data.error === "INVALID_ACTION" && legacyParams && legacyGasEnabled()) {
        const legacy = await jsonpLegacy(legacyParams, timeoutMs);
        return { data: legacy, transport: "legacy" };
      }
      return { data, transport: "worker" };
    }
    const legacy = await jsonpLegacy(legacyParams, timeoutMs);
    return { data: legacy, transport: "legacy" };
  }

  function setAuthAlert(message, type) {
    const box = document.getElementById("authInlineAlert");
    if (!box) return;
    if (!message) {
      box.style.display = "none";
      box.textContent = "";
      box.className = "auth-inline-alert";
      return;
    }
    box.style.display = "flex";
    box.className = "auth-inline-alert " + (type === "success" ? "is-success" : type === "info" ? "is-info" : "is-error");
    box.textContent = message;
  }

  function updateStepIndicator(step) {
    const s1 = document.getElementById("authStep1Badge");
    const s2 = document.getElementById("authStep2Badge");
    if (!s1 || !s2) return;
    s1.classList.toggle("active", step === 1);
    s1.classList.toggle("done", step === 2);
    s2.classList.toggle("active", step === 2);
  }

  function setBusy(button, busy, busyText) {
    if (!button) return;
    if (busy) {
      if (!button.dataset.originalText) button.dataset.originalText = button.textContent || "";
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
      ACCOUNT_EXISTS: "Этот Email уже зарегистрирован. Переключитесь на вкладку «Войти».",
      USER_NOT_FOUND: "Аккаунт с таким Email не найден. Переключитесь на «Создать аккаунт».",
      TOO_SOON: "Код уже отправлен. Подождите минуту и попробуйте снова.",
      EMAIL_CODE_TOO_SOON: "Код уже отправлен. Подождите минуту и попробуйте снова.",
      EMAIL_CODE_HOURLY_LIMIT: "Слишком много запросов за час. Попробуйте позже.",
      RATE_LIMITED: "Слишком частые запросы. Подождите минуту.",
      EMAIL_PROVIDER_UNAVAILABLE: "Почтовый сервис временно недоступен. Попробуйте через минуту.",
      INVALID_CODE: "Введите все 6 цифр кода.",
      CODE_NOT_FOUND: "Код не найден. Запросите новый код.",
      CODE_USED: "Этот код уже использован. Запросите новый.",
      CODE_EXPIRED: "Срок действия кода истёк. Запросите новый.",
      TOO_MANY_ATTEMPTS: "Слишком много неверных попыток. Запросите новый код.",
      WRONG_CODE: "Код не подходит. Проверьте письмо и попробуйте ещё раз.",
      SERVER_ERROR: "Сервер авторизации временно недоступен.",
      AUTH_TIMEOUT: "Сервер отвечает слишком долго. Попробуйте ещё раз.",
      AUTH_NETWORK_ERROR: "Не удалось связаться с сервером. Проверьте интернет.",
      UNKNOWN_ACTION: "Сервис авторизации требует обновления."
    };
    if (code === "WRONG_CODE" && data && Number.isFinite(data.attemptsLeft)) {
      return map[code] + " Осталось попыток: " + data.attemptsLeft + ".";
    }
    if ((code === "TOO_SOON" || code === "EMAIL_CODE_TOO_SOON") && data && Number.isFinite(data.retry_after_seconds)) {
      return `Код уже отправлен. Повтор через ${data.retry_after_seconds} сек.`;
    }
    return map[code] || "Не удалось выполнить вход. Попробуйте ещё раз.";
  }

  function notify(message, type) {
    if (type) setAuthAlert(message, type);
    if (typeof window.showToast === "function") window.showToast(message);
  }

  function wireSixPins(pins, onSubmit) {
    pins.forEach((el, idx) => {
      el.inputMode = "numeric";
      el.autocomplete = idx === 0 ? "one-time-code" : "off";
      el.oninput = () => {
        const digits = el.value.replace(/\D/g, "");
        if (digits.length > 1) {
          digits.slice(0, 6).split("").forEach((d, i) => { if (pins[i]) pins[i].value = d; });
          const full = pins.map((p) => p.value).join("");
          pins[Math.min(digits.length, 6) - 1]?.focus();
          if (full.length === 6) onSubmit();
          return;
        }
        el.value = digits.slice(-1);
        if (el.value && pins[idx + 1]) {
          pins[idx + 1].focus();
        } else if (el.value && idx === pins.length - 1) {
          const full = pins.map((p) => p.value).join("");
          if (full.length === 6) onSubmit();
        }
      };
      el.onkeydown = (ev) => {
        if (ev.key === "Backspace" && !el.value && pins[idx - 1]) pins[idx - 1].focus();
        if (ev.key === "ArrowLeft" && pins[idx - 1]) pins[idx - 1].focus();
        if (ev.key === "ArrowRight" && pins[idx + 1]) pins[idx + 1].focus();
        if (ev.key === "Enter") {
          ev.preventDefault();
          onSubmit();
        }
      };
      el.onpaste = (ev) => {
        const digits = (ev.clipboardData?.getData("text") || "").replace(/\D/g, "").slice(0, 6);
        if (digits.length > 1) {
          ev.preventDefault();
          digits.split("").forEach((d, i) => { if (pins[i]) pins[i].value = d; });
          pins[Math.min(digits.length, 6) - 1]?.focus();
          if (digits.length === 6) onSubmit();
        }
      };
    });
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
      input.autocomplete = "off";
      input.maxLength = 1;
      input.className = "pin-digit";
      input.id = "pin" + n;
      pinWrap.appendChild(input);
    }
    const pins = [...pinWrap.querySelectorAll(".pin-digit")];
    wireSixPins(pins, verifyRegistrationCode);
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

  function migrateAndSetSession(serverUser, token, expiresAt, provider) {
    if (typeof window.pausePaymentChecks === "function") window.pausePaymentChecks();
    const targetEmail = normalizeAccountEmail(serverUser?.email || pendingEmail || "");
    const currentSession = typeof window.getActiveUserSession === "function" ? window.getActiveUserSession() : null;
    const currentEmail = normalizeAccountEmail(currentSession?.email || localStorage.getItem("kapterka_user_email") || "");

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
      callsign: serverUser?.callsign || cached?.callsign || (sameGenericOwner ? localStorage.getItem("kapterka_user_callsign") || "" : "") || "Пользователь",
      rank: serverUser?.rank ?? cached?.rank ?? (sameGenericOwner ? localStorage.getItem("kapterka_user_rank") || "" : ""),
      unitName: serverUser?.unitName || cached?.unitName || (sameGenericOwner ? localStorage.getItem("kapterka_unit_name") || "" : "") || "",
      unitKey: serverUser?.unitKey || cached?.unitKey || (sameGenericOwner ? localStorage.getItem("kapterka_unit_key") || "" : "") || "",
      phone: serverUser?.phone || cached?.phone || (sameGenericOwner ? localStorage.getItem("kapterka_user_phone") || "" : "") || "",
      activeKey: activeKey,
      keys: keys,
      emailVerified: true,
      authProvider: provider === "legacy" ? "email_otp_legacy" : "email_otp_worker",
      webAuthV2: true
    };

    localStorage.setItem(TOKEN_KEY, token || "");
    localStorage.setItem(TOKEN_EXP_KEY, String(expiresAt || 0));
    localStorage.setItem(PROVIDER_KEY, provider || "worker");

    if (typeof window.setUserSession === "function") {
      window.setUserSession(user);
    } else {
      localStorage.setItem("kapterka_auth_user", JSON.stringify(user));
      localStorage.setItem("kapterka_user_email", user.email || "");
      if (typeof window.updateAuthUI === "function") window.updateAuthUI();
      if (typeof window.loadCabinetProfile === "function") window.loadCabinetProfile();
    }

    snapshotAccount(user.email);
    setAuthAlert("", "");
    updateStepIndicator(1);
    if (typeof window.resumePendingPaymentChecks === "function") window.resumePendingPaymentChecks();
  }

  function resetToEmailStep() {
    setAuthAlert("", "");
    updateStepIndicator(1);
    const stepInputs = document.getElementById("regStepInputs");
    const stepVerif = document.getElementById("regStepVerification");
    if (stepInputs) stepInputs.style.display = "block";
    if (stepVerif) stepVerif.style.display = "none";
    const loginInputs = document.getElementById("loginStepInputs");
    const loginBox = document.getElementById("loginOtpVerifyBox");
    if (loginInputs) loginInputs.style.display = "block";
    if (loginBox) loginBox.style.display = "none";
  }

  function showRegistrationVerify() {
    const stepInputs = document.getElementById("regStepInputs");
    const stepVerif = document.getElementById("regStepVerification");
    if (stepInputs) stepInputs.style.display = "none";
    if (stepVerif) {
      stepVerif.style.display = "block";
      ensureSixPins(stepVerif);
      const title = stepVerif.querySelector("h4");
      if (title) title.textContent = "Введите 6-значный код из письма";
      const hint = document.getElementById("verificationHint");
      if (hint) hint.textContent = "Код действует 10 минут и используется один раз. Проверьте «Спам», если письмо задержалось.";
      const disp = document.getElementById("verifyEmailDisplay");
      if (disp) disp.textContent = pendingEmail || "";
      clearSixPins(stepVerif);
      document.getElementById("pin1")?.focus();
    }
    updateStepIndicator(2);
  }

  function ensureLoginVerifyBox() {
    const panel = document.getElementById("panelLogin");
    if (!panel) return null;
    let box = document.getElementById("loginOtpVerifyBox");
    if (box) {
      const pins = [...box.querySelectorAll(".pin-digit")];
      if (pins.length === 6) wireSixPins(pins, verifyLoginCode);
      const verifyBtn = document.getElementById("loginVerifyCodeBtn");
      const resendBtn = document.getElementById("loginResendCodeBtn");
      const backBtn = document.getElementById("loginBackToEmailBtn");
      if (verifyBtn) verifyBtn.onclick = verifyLoginCode;
      if (resendBtn) resendBtn.onclick = requestLoginCode;
      if (backBtn) backBtn.onclick = resetToEmailStep;
      return box;
    }

    box = document.createElement("div");
    box.id = "loginOtpVerifyBox";
    box.className = "pin-verify-box auth-v2-login-verify";
    box.style.display = "none";
    box.innerHTML = [
      '<div class="auth-v2-code-title">Введите 6-значный код из письма</div>',
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
      '<button type="button" class="btn btn-primary btn-lg" id="loginVerifyCodeBtn">Войти в кабинет</button>',
      '<button type="button" class="btn btn-outline btn-sm" id="loginResendCodeBtn">Отправить код ещё раз</button>',
      '<button type="button" class="btn btn-outline btn-sm" id="loginBackToEmailBtn">Изменить Email</button>',
      '</div>'
    ].join("");

    panel.appendChild(box);
    wireSixPins([...box.querySelectorAll(".pin-digit")], verifyLoginCode);
    document.getElementById("loginVerifyCodeBtn").onclick = verifyLoginCode;
    document.getElementById("loginResendCodeBtn").onclick = requestLoginCode;
    document.getElementById("loginBackToEmailBtn").onclick = resetToEmailStep;
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

    ensureSixPins(document.getElementById("regStepVerification"));
    ensureLoginVerifyBox();

    const regBack = document.getElementById("regBackToEmailBtn");
    if (regBack) regBack.onclick = resetToEmailStep;

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
    const unitName = document.getElementById("regUnitName")?.value.trim() || "";
    const newsletter = document.getElementById("regNewsletterCheck")?.checked !== false;

    setAuthAlert("", "");
    if (!callsign) return notify("Укажите имя или позывной.", "error");
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) || email.length > 160) return notify("Проверьте адрес электронной почты.", "error");

    pendingMode = "register";
    pendingEmail = email;
    pendingRegister = { callsign, rank, unitName, newsletter };

    try {
      setBusy(actionBtn, true, "Отправляю код…");
      const { data, transport } = await requestAuthApi(
        "web_auth_request_code",
        { mode: "register", email, callsign, rank, unit_name: unitName, newsletter },
        { action: "auth_request_code", mode: "register", email, callsign, rank, newsletter: newsletter ? "1" : "0" }
      );
      pendingTransport = transport;
      if (!data || !data.ok) {
        const msg = authErrorMessage(data?.error, data);
        if (data?.error === "ACCOUNT_EXISTS" && typeof window.switchAuthMode === "function") {
          const loginInput = document.getElementById("loginEmail");
          if (loginInput) loginInput.value = email;
        }
        return notify(msg, "error");
      }
      showRegistrationVerify();
      startResendCooldown(document.getElementById("regResendCodeBtn"), 60);
      notify("Код отправлен на " + (data.maskedEmail || email), "success");
    } catch (err) {
      console.warn(err);
      notify(authErrorMessage(err?.message || "AUTH_NETWORK_ERROR"), "error");
    } finally {
      setBusy(actionBtn, false);
    }
  }

  async function requestLoginCode() {
    const actionBtn = document.querySelector("#panelLogin button[onclick*='processUserLogin']");
    const email = document.getElementById("loginEmail")?.value.trim().toLowerCase() || pendingEmail || "";
    setAuthAlert("", "");
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) || email.length > 160) return notify("Проверьте адрес электронной почты.", "error");

    pendingMode = "login";
    pendingEmail = email;
    pendingRegister = null;

    try {
      setBusy(actionBtn, true, "Отправляю код…");
      const { data, transport } = await requestAuthApi(
        "web_auth_request_code",
        { mode: "login", email },
        { action: "auth_request_code", mode: "login", email }
      );
      pendingTransport = transport;
      if (!data || !data.ok) return notify(authErrorMessage(data?.error, data), "error");

      const loginInputs = document.getElementById("loginStepInputs");
      if (loginInputs) loginInputs.style.display = "none";
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
      updateStepIndicator(2);
      startResendCooldown(document.getElementById("loginResendCodeBtn"), 60);
      notify("Одноразовый код отправлен на " + (data.maskedEmail || email), "success");
    } catch (err) {
      console.warn(err);
      notify(authErrorMessage(err?.message || "AUTH_NETWORK_ERROR"), "error");
    } finally {
      setBusy(actionBtn, false);
    }
  }

  async function verifyCode(code, mode) {
    if (verifyInFlight) return;
    if (!pendingEmail) return notify("Сначала запросите код на почту.", "error");
    if (!code || code.length !== 6) return notify("Введите все 6 цифр кода.", "error");

    const verifyBtn = document.getElementById(mode === "login" ? "loginVerifyCodeBtn" : "regVerifyCodeBtn");
    verifyInFlight = true;
    setAuthAlert("", "");
    try {
      setBusy(verifyBtn, true, "Проверяю…");
      let data;
      let transport = pendingTransport;
      if (transport === "legacy") {
        data = await jsonpLegacy({ action: "auth_verify_code", email: pendingEmail, code });
      } else {
        const res = await requestAuthApi(
          "web_auth_verify_code",
          { email: pendingEmail, code },
          { action: "auth_verify_code", email: pendingEmail, code }
        );
        data = res.data;
        transport = res.transport;
      }

      if (!data || !data.ok) return notify(authErrorMessage(data?.error, data), "error");

      migrateAndSetSession(data.user || {}, data.sessionToken || "", data.sessionExpiresAt || 0, transport);
      pendingMode = null;
      pendingEmail = null;
      pendingRegister = null;
      resetToEmailStep();

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
      notify(authErrorMessage(err?.message || "AUTH_NETWORK_ERROR"), "error");
    } finally {
      verifyInFlight = false;
      setBusy(verifyBtn, false);
    }
  }

  async function verifyRegistrationCode() {
    const code = readSixPins(document.getElementById("regStepVerification"));
    return verifyCode(code, "register");
  }

  async function verifyLoginCode() {
    return verifyCode(readLoginCode(), "login");
  }

  async function saveProfileToServer(patch) {
    const token = localStorage.getItem(TOKEN_KEY) || "";
    const provider = localStorage.getItem(PROVIDER_KEY) || "worker";
    if (!token || provider !== "worker" || !workerBaseUrl()) return { ok: false, skipped: true };
    try {
      const data = await postWorker("web_auth_update_profile", {
        token,
        callsign: patch.callsign || "",
        rank: patch.rank || "",
        unit_name: patch.unitName || "",
        unit_key: patch.unitKey || "",
        phone: patch.phone || ""
      }, 15000);
      return data || { ok: false };
    } catch (_) {
      return { ok: false, error: "AUTH_NETWORK_ERROR" };
    }
  }

  async function refreshExistingSession() {
    const token = localStorage.getItem(TOKEN_KEY) || "";
    const expiresAt = Number(localStorage.getItem(TOKEN_EXP_KEY) || 0);
    const provider = localStorage.getItem(PROVIDER_KEY) || (token.includes(".") ? "legacy" : "worker");
    if (!token) return;

    if (expiresAt && Date.now() > expiresAt) {
      logoutV2();
      notify("Сессия завершена. Войдите снова по коду из Email.");
      return;
    }

    try {
      let data;
      let resolvedProvider = provider;
      if (provider === "legacy" && legacyGasEnabled()) {
        data = await jsonpLegacy({ action: "auth_session", token }, 12000);
      } else if (workerBaseUrl()) {
        const res = await requestAuthApi(
          "web_auth_session",
          { token },
          { action: "auth_session", token },
          12000
        );
        data = res.data;
        resolvedProvider = res.transport;
      }

      if (data && data.ok && data.user) {
        migrateAndSetSession(data.user, token, data.sessionExpiresAt || expiresAt, resolvedProvider);
        return;
      }

      if (data && ["INVALID_SESSION", "SESSION_EXPIRED", "USER_NOT_FOUND"].includes(data.error)) {
        logoutV2();
        notify("Сессия завершена. Войдите снова по коду из Email.");
      }
    } catch (err) {
      console.warn("Web Auth session refresh skipped:", err);
    }
  }

  function logoutV2() {
    const token = localStorage.getItem(TOKEN_KEY) || "";
    const provider = localStorage.getItem(PROVIDER_KEY) || "worker";
    if (token && provider === "worker" && workerBaseUrl()) {
      postWorker("web_auth_logout", { token }, 6000).catch(() => {});
    }

    if (typeof window.pausePaymentChecks === "function") window.pausePaymentChecks();
    const current = typeof window.getActiveUserSession === "function" ? window.getActiveUserSession() : null;
    snapshotAccount(current?.email || localStorage.getItem("kapterka_user_email"));

    localStorage.removeItem("kapterka_auth_user");
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(TOKEN_EXP_KEY);
    localStorage.removeItem(PROVIDER_KEY);
    localStorage.removeItem("kapterka_pending_callsign");
    localStorage.removeItem("kapterka_pending_key");
    localStorage.removeItem("kapterka_pending_payment_id");
    localStorage.removeItem("kapterka_verified_key");
    clearGenericProfileStorage();

    resetToEmailStep();
    if (typeof window.updateAuthUI === "function") window.updateAuthUI();
    if (typeof window.loadCabinetProfile === "function") window.loadCabinetProfile();
    notify("Вы вышли из личного кабинета.");
  }

  function activate() {
    if (!enabled()) return;
    adaptUi();

    const origSwitch = window.switchAuthMode;
    window.switchAuthMode = function (mode) {
      resetToEmailStep();
      if (typeof origSwitch === "function") origSwitch(mode);
    };

    window.startRegistrationProcess = requestRegistrationCode;
    window.processUserLogin = requestLoginCode;
    window.verifyEmailPinCode = verifyRegistrationCode;
    window.resendPinCode = function () {
      return pendingMode === "login" ? requestLoginCode() : requestRegistrationCode();
    };
    window.logoutUserSession = logoutV2;
    window.saveWebAuthProfileToServer = saveProfileToServer;

    document.documentElement.classList.add("auth-v2-enabled");
    refreshExistingSession();
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", activate);
  } else {
    activate();
  }
})();
