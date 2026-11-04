const KAPTERKA_WEB_BUILD = window.KAPTERKA_WEB_VERSION || 'unknown';
// ==========================================================================
// KAPTERKA PRO — APP.JS (WITH USER REGISTRATION, EMAIL VERIFICATION & CABINET)
// ==========================================================================

// Global state keys for persistence

// Contacts and legacy notifications keep their existing Apps Script endpoint.
window.KAPTERKA_API_URL = 'https://script.google.com/macros/s/AKfycbwuwY74vD9El1R6ZVvO3DDpJ7BkY-wX0ljRphWRSA-jgB33-duUAqEp0g03D_7oFzjqmA/exec';
// Payments and license status are served only by the authoritative backend.
window.KAPTERKA_PAYMENT_API_URL = 'https://kapterka-api.alex-666-881.workers.dev';

const STORAGE_AUTH_USER = 'kapterka_auth_user'; // JSON of currently logged in user
const STORAGE_USERS_DB = 'kapterka_users_db'; // Array of registered users
const STORAGE_SUBSCRIBERS_LIST = 'kapterka_newsletter_subscribers'; // Newsletter emails list
const STORAGE_USER_CALLSIGN = 'kapterka_user_callsign';
const STORAGE_USER_RANK = 'kapterka_user_rank';
const STORAGE_UNIT_NAME = 'kapterka_unit_name';
const STORAGE_UNIT_KEY = 'kapterka_unit_key';
const STORAGE_USER_EMAIL = 'kapterka_user_email';
const STORAGE_USER_PHONE = 'kapterka_user_phone';
const STORAGE_ACTIVE_KEY = 'kapterka_active_key';
const STORAGE_KEYS_HISTORY = 'kapterka_keys_history';
const STORAGE_LICENSE_META = 'kapterka_license_meta_v2';

const STORAGE_PAYMENT_REQUEST = 'kapterka_payment_requests_v1';

// Default initial state for clean empty inputs
const YM_IDS = [112255061];
function trackYm(action, ...args) {
  if (typeof window.ym === 'function') {
    YM_IDS.forEach(id => {
      try { window.ym(id, action, ...args); } catch(e) {}
    });
  }
}

const defaultProfile = {
  callsign: '',
  rank: '',
  unitName: '',
  unitKey: '',
  email: '',
  phone: '',
  activeKey: '',
  subscribedToNewsletter: true,
  emailVerified: false,
  keys: []
};

// State for registration flow
let tempPendingReg = null;
let currentVerificationPin = null;

// Telegram Notification Bot Configuration
// ВНИМАНИЕ: Токены нельзя хранить в открытом коде на GitHub!
// Все запросы теперь должны идти через ваш Google Apps Script (KAPTERKA_API_URL)
const TG_ADMIN_CHAT_ID = '7426550032';

function escapeTelegramHtml(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
}

async function sendTelegramNotification(text) {
  try {
    const API_URL = window.KAPTERKA_API_URL || '';
    if (!API_URL) {
      console.warn('API URL не настроен. Сообщение не отправлено.');
      return false;
    }
    // Используем text/plain, чтобы избежать CORS preflight.
    const response = await fetch(API_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'text/plain;charset=utf-8' },
      body: JSON.stringify({ action: 'send_telegram', text: text, chat_id: TG_ADMIN_CHAT_ID })
    });
    return response.ok;
  } catch (err) {
    console.warn('Telegram notification failed:', err);
    return false;
  }
}

// 1. Toast Notification Helper
function showToast(msg) {
  const toast = document.getElementById('tacticalToast');
  const msgEl = document.getElementById('toastMessage');
  if (!toast || !msgEl) return;

  msgEl.textContent = msg;
  toast.classList.add('show');
  setTimeout(() => {
    toast.classList.remove('show');
  }, 4000);
}

// 2. Auth Modes & Session Management
function switchAuthMode(mode) {
  const btnReg = document.getElementById('btnSwitchRegister');
  const btnLogin = document.getElementById('btnSwitchLogin');
  const panelReg = document.getElementById('panelRegister');
  const panelLogin = document.getElementById('panelLogin');

  if (mode === 'register') {
    if (btnReg) btnReg.classList.add('active');
    if (btnLogin) btnLogin.classList.remove('active');
    if (panelReg) panelReg.classList.add('active');
    if (panelLogin) panelLogin.classList.remove('active');
  } else {
    if (btnLogin) btnLogin.classList.add('active');
    if (btnReg) btnReg.classList.remove('active');
    if (panelLogin) panelLogin.classList.add('active');
    if (panelReg) panelReg.classList.remove('active');
  }
}

function getStoredUsers() {
  // Legacy local user database is retained only for compatibility with old browser data.
  const raw = localStorage.getItem(STORAGE_USERS_DB);
  if (!raw) return [];
  try {
    const users = JSON.parse(raw);
    return Array.isArray(users) ? users : [];
  } catch (e) {
    return [];
  }
}

function getSubscribersList() {
  const raw = localStorage.getItem(STORAGE_SUBSCRIBERS_LIST);
  if (!raw) return [];
  try {
    const list = JSON.parse(raw);
    return Array.isArray(list) ? list : [];
  } catch (e) {
    return [];
  }
}

function addSubscriberEmail(email) {
  if (!email) return;
  const list = getSubscribersList();
  const clean = email.trim().toLowerCase();
  if (!list.includes(clean)) {
    list.push(clean);
    localStorage.setItem(STORAGE_SUBSCRIBERS_LIST, JSON.stringify(list));
  }
}

// 3. User Registration Flow with Email Pin Verification
function startRegistrationProcess() {
  showToast('Защищённая регистрация загружается. Обновите страницу и попробуйте ещё раз.');
}

function focusNextPin(input, nextId) {
  if (input.value.length >= 1 && nextId) {
    document.getElementById(nextId)?.focus();
  }
}

function resendPinCode() {
  showToast('Повторная отправка доступна только через защищённую серверную авторизацию.');
}

function verifyEmailPinCode() {
  showToast('Проверка Email доступна только через защищённую серверную авторизацию.');
}

// 4. Login and Session Handlers
function processUserLogin() {
  showToast('Защищённый вход загружается. Обновите страницу и попробуйте ещё раз.');
}

function quickDemoLogin() {
  showToast('Демо-режим доступен после обычной регистрации по Email.');
}

function setUserSession(user) {
  if (paymentAccountScope() !== String(user.email || '').trim().toLowerCase()) pausePaymentChecks();
  localStorage.setItem(STORAGE_AUTH_USER, JSON.stringify(user));
  localStorage.setItem(STORAGE_USER_CALLSIGN, user.callsign);
  localStorage.setItem(STORAGE_USER_RANK, user.rank || '');
  localStorage.setItem(STORAGE_UNIT_NAME, user.unitName || '');
  localStorage.setItem(STORAGE_UNIT_KEY, user.unitKey || '');
  localStorage.setItem(STORAGE_USER_EMAIL, user.email);
  localStorage.setItem(STORAGE_USER_PHONE, user.phone || '');

  if (user.keys && user.keys.length > 0) {
    localStorage.setItem(STORAGE_ACTIVE_KEY, user.keys[0].key);
    localStorage.setItem(STORAGE_KEYS_HISTORY, JSON.stringify(user.keys));
  }

  updateAuthUI();
  loadCabinetProfile();
  resumePendingPaymentChecks();
}

function logoutUserSession() {
  pausePaymentChecks();
  localStorage.removeItem(STORAGE_AUTH_USER);
  localStorage.removeItem(STORAGE_USER_CALLSIGN);
  localStorage.removeItem(STORAGE_USER_RANK);
  localStorage.removeItem(STORAGE_UNIT_NAME);
  localStorage.removeItem(STORAGE_UNIT_KEY);
  localStorage.removeItem(STORAGE_USER_EMAIL);
  localStorage.removeItem(STORAGE_USER_PHONE);
  localStorage.removeItem(STORAGE_ACTIVE_KEY);
  localStorage.removeItem(STORAGE_KEYS_HISTORY);
  localStorage.removeItem('kapterka_pending_callsign');
  localStorage.removeItem('kapterka_pending_key');
  localStorage.removeItem('kapterka_pending_payment_id');
  localStorage.removeItem('kapterka_verified_key');
  
  updateAuthUI();
  loadCabinetProfile();
  showToast('Вы вышли из учетной записи. Данные сессии очищены.');
}

function getActiveUserSession() {
  const raw = localStorage.getItem(STORAGE_AUTH_USER);
  if (!raw) return null;
  try {
    return JSON.parse(raw);
  } catch (e) {
    return null;
  }
}


function updateAuthUI() {
  const user = getActiveUserSession();
  const authContainer = document.getElementById('authContainer');
  const cabinetContent = document.getElementById('cabinetContent');
  const navCallsign = document.getElementById('navCallsignDisplay');
  const sessionCallsign = document.getElementById('sessionCallsignText');
  const sessionEmail = document.getElementById('sessionEmailText');
  const sessionChip = document.getElementById('sessionNewsletterChip');

  if (user) {
    if (authContainer) authContainer.style.display = 'none';
    if (cabinetContent) cabinetContent.style.display = 'block';
    if (navCallsign) navCallsign.textContent = user.callsign;
    if (sessionCallsign) sessionCallsign.textContent = user.callsign;
    if (sessionEmail) sessionEmail.textContent = user.email;
    if (sessionChip) {
      sessionChip.textContent = user.subscribedToNewsletter
        ? '✓ Подписка на обновления ПО активна'
        : 'Рассылка отключена';
    }
  } else {
    if (authContainer) authContainer.style.display = 'block';
    if (cabinetContent) cabinetContent.style.display = 'none';
    if (navCallsign) navCallsign.textContent = 'Войти / Регистрация';
  }
}

// 2. Main Tab Switching Controller
function switchMainTab(tabId) {
  // Hide all tabs
  document.querySelectorAll('.app-view-tab').forEach(tab => {
    tab.classList.remove('active');
    tab.setAttribute('aria-hidden', 'true');
  });

  // Remove active state from tab navigation buttons
  document.querySelectorAll('.main-tab-btn').forEach(btn => {
    btn.classList.remove('active');
    btn.setAttribute('aria-selected', 'false');
  });

  // Activate selected tab
  const targetTab = document.getElementById(tabId);
  if (targetTab) {
    targetTab.classList.add('active');
    targetTab.setAttribute('aria-hidden', 'false');
  }

  // Update navbar button highlight
  const btnMap = {
    'tabOverview': 'btnTabOverview',
    'tabCabinet': 'btnTabCabinet',
    'tabPayment': 'btnTabPayment',
    'tabSync': 'btnTabSync',
    'tabDownload': 'btnTabDownload',
    'tabAdmin': null
  };

  const activeBtnId = btnMap[tabId];
  if (activeBtnId) {
    const activeBtn = document.getElementById(activeBtnId);
    if (activeBtn) {
      activeBtn.classList.add('active');
      activeBtn.setAttribute('aria-selected', 'true');
    }
  }

  // Update mobile bottom nav highlight
  document.querySelectorAll('.mobile-bottom-tab-btn').forEach(btn => {
    btn.classList.remove('active');
    btn.setAttribute('aria-selected', 'false');
  });
  const mobileBtnMap = {
    'tabOverview': 'mobileTabOverview',
    'tabCabinet': 'mobileTabCabinet',
    'tabPayment': 'mobileTabPayment',
    'tabSync': 'mobileTabSync',
    'tabDownload': 'mobileTabDownload'
  };
  const activeMobileBtnId = mobileBtnMap[tabId];
  if (activeMobileBtnId) {
    const mobileBtn = document.getElementById(activeMobileBtnId);
    if (mobileBtn) {
      mobileBtn.classList.add('active');
      mobileBtn.setAttribute('aria-selected', 'true');
    }
  }

  // Close the compact mobile menu after a selection.
  const mobileNav = document.getElementById('mainTabNav');
  const mobileToggle = document.getElementById('mobileToggle');
  if (mobileNav) mobileNav.classList.remove('open');
  if (mobileToggle) mobileToggle.setAttribute('aria-expanded', 'false');

  // Keep a shareable/restorable URL. New selections go into browser history,
  // while restoring the already-selected hash does not create duplicate entries.
  try {
    const nextHash = '#' + tabId;
    if (history && history.pushState && window.location.hash !== nextHash) {
      history.pushState({ tabId }, '', nextHash);
    } else if (history && history.replaceState) {
      history.replaceState({ tabId }, '', nextHash);
    }
  } catch (e) {}

  // Refresh payment form from the authenticated/local profile when opening payment.
  if (tabId === 'tabCabinet') {
    refreshStoredLicenseStatus();
  }

  if (tabId === 'tabPayment') {
    if (!window.__kapterkaProViewTracked) {
      window.__kapterkaProViewTracked = true;
      if (typeof trackSiteAction === 'function') trackSiteAction('pro_view', { source: 'tab' });
    }
    const currentUser = getActiveUserSession();
    const callsign = currentUser?.callsign || localStorage.getItem(STORAGE_USER_CALLSIGN) || '';
    const email = currentUser?.email || localStorage.getItem(STORAGE_USER_EMAIL) || '';
    const payCallsignInput = document.getElementById('payCallsignInput');
    const payEmailInput = document.getElementById('payEmailInput');
    if (payCallsignInput && callsign) payCallsignInput.value = callsign;
    if (payEmailInput && email) payEmailInput.value = email;
  }

  // Scroll smoothly to top of content
  const reduceMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)')?.matches;
  window.scrollTo({ top: 0, behavior: reduceMotion ? 'auto' : 'smooth' });

  // Track virtual pageview in Google Analytics and Yandex.Metrika
  try {
    if (typeof window.gtag === 'function') {
      window.gtag('event', 'page_view', {
        page_title: document.title,
        page_location: window.location.origin + window.location.pathname + '#' + tabId,
        page_path: '/#' + tabId
      });
    }
    trackYm('hit', '/#' + tabId);
  } catch (e) {}
}

// 3. Cabinet Profile Management
function loadCabinetProfile() {
  const callsign = localStorage.getItem(STORAGE_USER_CALLSIGN) || defaultProfile.callsign;
  const rank = localStorage.getItem(STORAGE_USER_RANK) || defaultProfile.rank;
  const unitName = localStorage.getItem(STORAGE_UNIT_NAME) || defaultProfile.unitName;
  const unitKey = localStorage.getItem(STORAGE_UNIT_KEY) || defaultProfile.unitKey;
  const email = localStorage.getItem(STORAGE_USER_EMAIL) || defaultProfile.email;
  const phone = localStorage.getItem(STORAGE_USER_PHONE) || defaultProfile.phone;
  const activeKey = localStorage.getItem(STORAGE_ACTIVE_KEY) || defaultProfile.activeKey;

  // Set inputs
  const callsignInput = document.getElementById('cabCallsignInput');
  const rankInput = document.getElementById('cabRankInput');
  const unitNameInput = document.getElementById('cabUnitNameInput');
  const unitKeyInput = document.getElementById('cabUnitKeyInput');
  const emailInput = document.getElementById('cabEmailInput');
  const phoneInput = document.getElementById('cabPhoneInput');
  const activeKeyDisp = document.getElementById('cabActiveKeyDisp');
  const navCallsignDisplay = document.getElementById('navCallsignDisplay');
  const navBadgeStatus = document.getElementById('navBadgeStatus');
  const payCallsignInput = document.getElementById('payCallsignInput');
  const payEmailInput = document.getElementById('payEmailInput');

  if (callsignInput) callsignInput.value = callsign;
  if (rankInput) rankInput.value = rank;
  if (unitNameInput) unitNameInput.value = unitName;
  if (unitKeyInput) unitKeyInput.value = unitKey;
  if (emailInput) emailInput.value = email;
  if (phoneInput) phoneInput.value = phone;
  if (activeKeyDisp) activeKeyDisp.textContent = activeKey || '—';
  if (navCallsignDisplay) navCallsignDisplay.textContent = callsign || 'Личный кабинет';
  if (payCallsignInput && callsign) payCallsignInput.value = callsign;
  if (payEmailInput && email) payEmailInput.value = email;

  updateCabinetLicenseStatus();
  renderKeysHistory();
}

function saveCabinetProfile() {
  const callsign = document.getElementById('cabCallsignInput')?.value.trim() || 'Боец';
  const rank = document.getElementById('cabRankInput')?.value.trim() || '';
  const unitName = document.getElementById('cabUnitNameInput')?.value.trim() || '';
  const unitKey = document.getElementById('cabUnitKeyInput')?.value.trim() || localStorage.getItem(STORAGE_UNIT_KEY) || '';
  const currentSession = getActiveUserSession();
  // Email является идентификатором серверного аккаунта и не меняется локальным редактированием профиля.
  const email = (currentSession?.email || document.getElementById('cabEmailInput')?.value || '').trim().toLowerCase();
  const phone = document.getElementById('cabPhoneInput')?.value.trim() || '';

  localStorage.setItem(STORAGE_USER_CALLSIGN, callsign);
  localStorage.setItem(STORAGE_USER_RANK, rank);
  localStorage.setItem(STORAGE_UNIT_NAME, unitName);
  localStorage.setItem(STORAGE_UNIT_KEY, unitKey);
  localStorage.setItem(STORAGE_USER_EMAIL, email);
  localStorage.setItem(STORAGE_USER_PHONE, phone);

  // Keep current authenticated session profile in sync with edited cabinet fields.
  try {
    if (currentSession) {
      const updatedSession = {
        ...currentSession,
        callsign,
        rank,
        unitName,
        unitKey,
        email,
        phone
      };
      localStorage.setItem(STORAGE_AUTH_USER, JSON.stringify(updatedSession));
    }
  } catch (e) {}

  const navCallsignDisplay = document.getElementById('navCallsignDisplay');
  if (navCallsignDisplay) navCallsignDisplay.textContent = callsign;

  const payCallsignInput = document.getElementById('payCallsignInput');
  if (payCallsignInput) payCallsignInput.value = callsign;

  showToast('Данные профиля и подразделения сохранены!');
}

function generateNewUnitKey() {
  const chars = '0123456789abcdef';
  let rand = '';
  for (let i = 0; i < 6; i++) {
    rand += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  const newKey = `kapt_${rand}`;
  const unitKeyInput = document.getElementById('cabUnitKeyInput');
  if (unitKeyInput) {
    unitKeyInput.value = newKey;
    localStorage.setItem(STORAGE_UNIT_KEY, newKey);
    showToast(`Сформирован новый ключ роты: ${newKey}`);
  }
}

// 4. Cryptographic Military License Key Checksum & Generation
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

function verifyKeyChecksum(key) {
  if (!key || typeof key !== 'string') return false;
  const clean = key.trim().toUpperCase().replace(/\s+/g, '');
  const parts = clean.split('-');
  if (parts.length !== 4 || parts[0] !== 'KAPT' || parts[1].length !== 4 || parts[2].length !== 4 || parts[3].length !== 4) {
    return false;
  }
  const expected = computeKeyChecksum(parts[1], parts[2]);
  return parts[3] === expected;
}

// До 04.09.2026 приложение выпускало KAPT-ключи без контрольной суммы:
// все три 4-символьных блока генерировались случайно. Такие ключи нельзя
// отличить от поддельных только по математике, поэтому сайт принимает их
// как совместимый старый формат, но не называет криптографически проверенными.
// KPT-XXXX-XXXX-XXXX также поддерживается Android-приложением как legacy-формат.
function classifyLicenseKey(key) {
  if (!key || typeof key !== 'string') return 'invalid';
  const clean = key.trim().toUpperCase().replace(/\s+/g, '');
  // Server payment keys contain hexadecimal 0/1; old keys used a smaller alphabet.
  const legacyPattern = /^(?:KAPT|KPT)-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}$/;
  if (!legacyPattern.test(clean)) return 'invalid';
  if (clean.startsWith('KAPT-') && verifyKeyChecksum(clean)) return 'signed';
  return 'legacy_unverified';
}



function getLicenseMetaMap() {
  try {
    const raw = JSON.parse(localStorage.getItem(STORAGE_LICENSE_META) || '{}');
    return raw && typeof raw === 'object' ? raw : {};
  } catch (_) {
    return {};
  }
}

function saveLicenseMeta(key, patch) {
  const cleanKey = String(key || '').trim().toUpperCase();
  if (!cleanKey) return;
  const map = getLicenseMetaMap();
  map[cleanKey] = {
    ...(map[cleanKey] || {}),
    ...(patch || {}),
    updatedAt: Date.now()
  };
  localStorage.setItem(STORAGE_LICENSE_META, JSON.stringify(map));
}

function getLicenseMeta(key) {
  const cleanKey = String(key || '').trim().toUpperCase();
  return getLicenseMetaMap()[cleanKey] || null;
}

function paymentErrorMessage(code) {
  return {
    INVALID_EMAIL: 'Проверьте Email для чека и ключа.',
    RATE_LIMITED: 'Слишком частые запросы. Подождите немного и повторите проверку.',
    PAYMENT_GATEWAY_AUTH: 'Сервис оплаты временно недоступен. Попробуйте позже.',
    UPSTREAM_ERROR: 'Сервис оплаты временно недоступен. Повторите запрос.',
    LICENSE_REGISTRY_UNAVAILABLE: 'Сервер лицензий временно недоступен. Повторите проверку.',
    FIGHTER_MISMATCH: 'Лицензия или платёж привязаны к другому устройству. Обратитесь в поддержку.',
    LICENSE_FIGHTER_MISMATCH: 'Лицензия привязана к другому устройству. Обратитесь в поддержку.',
    WEB_CLAIM_REQUIRED: 'Не найдено подтверждение этого заказа в браузере. Обратитесь в поддержку.',
    WEB_CLAIM_MISMATCH: 'Этот заказ создан в другом браузере или аккаунте. Откройте его там или обратитесь в поддержку.',
    PAYMENT_CLAIM_MISMATCH: 'Этот заказ создан в другом браузере или аккаунте. Откройте его там или обратитесь в поддержку.',
    INVALID_WEB_CLAIM: 'Не удалось подтвердить заказ в этом браузере. Обратитесь в поддержку.',
    PAYMENT_AMOUNT_MISMATCH: 'Сумма платежа не соответствует тарифу. Обратитесь в поддержку.',
    PAYMENT_PRODUCT_MISMATCH: 'Платёж относится к другому товару. Обратитесь в поддержку.',
    LICENSE_NOT_ACTIVE: 'Ключ не найден среди активных лицензий или срок уже завершён.',
    LICENSE_NOT_FOUND: 'Ключ не найден в реестре лицензий. Проверьте символы или обратитесь в поддержку.',
    INVALID_LICENSE_KEY: 'Проверьте символы и дефисы в ключе.',
    INVALID_PAYMENT_RESPONSE: 'Сервер вернул неполные данные. Повторите проверку.',
    REQUEST_TIMEOUT: 'Сервер отвечает слишком долго. Повторите запрос: сохранённый заказ не потерян.',
    NETWORK_ERROR: 'Не удалось связаться с сервером. Проверьте интернет и повторите запрос.'
  }[code] || 'Не удалось подтвердить данные на сервере. Повторите запрос или обратитесь в поддержку.';
}

async function requestPaymentApi(action, payload) {
  const controller = typeof AbortController !== 'undefined' ? new AbortController() : null;
  const timer = controller ? setTimeout(() => controller.abort(), 45000) : null;
  try {
    const response = await fetch(window.KAPTERKA_PAYMENT_API_URL + '?action=' + encodeURIComponent(action), {
      method: 'POST',
      cache: 'no-store',
      credentials: 'omit',
      headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
      body: JSON.stringify(payload),
      signal: controller?.signal
    });
    const data = await response.json().catch(() => null);
    if (!response.ok || !data || data.ok !== true) {
      const error = new Error(paymentErrorMessage(data?.error));
      error.code = data?.error || 'INVALID_PAYMENT_RESPONSE';
      error.paymentApi = true;
      error.retryable = response.status === 429 || response.status >= 500 || !data;
      throw error;
    }
    return data;
  } catch (error) {
    if (error.paymentApi) throw error;
    const failure = new Error(paymentErrorMessage(error.name === 'AbortError' ? 'REQUEST_TIMEOUT' : 'NETWORK_ERROR'));
    failure.code = error.name === 'AbortError' ? 'REQUEST_TIMEOUT' : 'NETWORK_ERROR';
    failure.retryable = true;
    throw failure;
  } finally {
    if (timer) clearTimeout(timer);
  }
}

async function fetchLicenseRegistryMeta(key) {
  const cleanKey = String(key || '').trim().toUpperCase();
  if (!/^KAPT-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}$/.test(cleanKey)) {
    return { found: false, verified: false, reason: 'UNSUPPORTED_KEY_FORMAT' };
  }
  try {
    // Read-only status never binds a browser to a key intended for Android.
    const data = await requestPaymentApi('license_status', { license_key: cleanKey });
    const expiresAt = Number(data.expires_at || 0);
    const status = String(data.status || (data.active ? 'ACTIVE' : 'INACTIVE')).toUpperCase();
    if (!Number.isFinite(expiresAt) || expiresAt <= 0) {
      return { found: false, verified: false, reason: 'INVALID_PAYMENT_RESPONSE', errorMessage: paymentErrorMessage('INVALID_PAYMENT_RESPONSE') };
    }
    const verified = data.active === true && status === 'ACTIVE' && expiresAt > Date.now();
    return { found: true, verified, registryVerified: true, status, expiresAt, activatedAt: Number(data.activated_at || 0) };
  } catch (error) {
    return { found: false, verified: false, unavailable: !!error.retryable, reason: error.code, errorMessage: error.message };
  }
}

function licenseDaysRemaining(expiresAt) {
  const exp = Number(expiresAt || 0);
  if (!exp) return null;
  return Math.max(0, Math.ceil((exp - Date.now()) / 86400000));
}

function formatLicenseDate(value) {
  const ts = Number(value || 0);
  if (!ts) return '';
  try {
    return new Date(ts).toLocaleDateString('ru-RU');
  } catch (_) {
    return '';
  }
}

function resolveLicenseMeta(itemOrKey) {
  const item = typeof itemOrKey === 'object' && itemOrKey ? itemOrKey : {};
  const key = typeof itemOrKey === 'string' ? itemOrKey : String(item.key || '');
  const stored = getLicenseMeta(key) || {};
  return {
    ...item,
    ...stored,
    key
  };
}

function updateCabinetLicenseStatus() {
  const activeKey = localStorage.getItem(STORAGE_ACTIVE_KEY) || '';
  const card = document.getElementById('cabActiveKeyCard');
  const noKeyCard = document.getElementById('cabNoKeyCard');
  const badge = document.getElementById('cabStatusBadge');
  const expiryText = document.getElementById('cabExpiryText');
  const daysEl = document.getElementById('cabDaysLeft');
  const expiryDateEl = document.getElementById('cabExpiryDate');
  const progressEl = document.getElementById('cabExpiryProgress');
  const navBadgeStatus = document.getElementById('navBadgeStatus');

  if (!activeKey) {
    if (card) card.style.display = 'none';
    if (noKeyCard) noKeyCard.style.display = 'block';
    if (navBadgeStatus) navBadgeStatus.style.display = 'none';
    return;
  }

  if (card) card.style.display = 'block';
  if (noKeyCard) noKeyCard.style.display = 'none';

  const meta = resolveLicenseMeta(activeKey);
  const days = licenseDaysRemaining(meta.expiresAt);
  const expiryDate = formatLicenseDate(meta.expiresAt);
  const isVerifiedActive = !!meta.registryVerified && Number(meta.expiresAt || 0) > Date.now() && String(meta.status || 'ACTIVE').toUpperCase() === 'ACTIVE';
  const isVerifiedExpired = !!meta.registryVerified && Number(meta.expiresAt || 0) > 0 && Number(meta.expiresAt) <= Date.now();

  if (isVerifiedActive) {
    if (badge) {
      badge.textContent = 'PRO АКТИВЕН';
      badge.classList.add('badge-gold');
    }
    if (expiryText) expiryText.textContent = 'Срок подтверждён реестром';
    if (daysEl) daysEl.textContent = String(days ?? '—');
    if (expiryDateEl) expiryDateEl.textContent = expiryDate ? 'до ' + expiryDate : '';
    if (progressEl) progressEl.style.width = Math.max(0, Math.min(100, ((days || 0) / 30) * 100)) + '%';
    if (navBadgeStatus) {
      navBadgeStatus.textContent = 'ПРО';
      navBadgeStatus.style.display = 'inline-block';
    }
  } else if (isVerifiedExpired) {
    if (badge) {
      badge.textContent = 'СРОК ИСТЁК';
      badge.classList.remove('badge-gold');
    }
    if (expiryText) expiryText.textContent = expiryDate ? 'Завершилась ' + expiryDate : 'Лицензия завершилась';
    if (daysEl) daysEl.textContent = '0';
    if (expiryDateEl) expiryDateEl.textContent = 'нужно продлить';
    if (progressEl) progressEl.style.width = '0%';
    if (navBadgeStatus) navBadgeStatus.style.display = 'none';
  } else {
    if (badge) {
      badge.textContent = 'КЛЮЧ СОХРАНЁН';
      badge.classList.remove('badge-gold');
    }
    if (expiryText) expiryText.textContent = 'Проверяем срок в реестре';
    if (daysEl) daysEl.textContent = '—';
    if (expiryDateEl) expiryDateEl.textContent = 'срок не подтверждён';
    if (progressEl) progressEl.style.width = '0%';
    if (navBadgeStatus) navBadgeStatus.style.display = 'none';
  }
}

async function refreshStoredLicenseStatus() {
  const generation = paymentSessionGeneration;
  const account = paymentAccountScope();
  const activeKey = localStorage.getItem(STORAGE_ACTIVE_KEY) || '';
  if (!activeKey || classifyLicenseKey(activeKey) === 'invalid') {
    updateCabinetLicenseStatus();
    renderKeysHistory();
    return;
  }

  const result = await fetchLicenseRegistryMeta(activeKey);
  if (generation !== paymentSessionGeneration || account !== paymentAccountScope() || activeKey !== localStorage.getItem(STORAGE_ACTIVE_KEY)) return;
  if (result.found) {
    saveLicenseMeta(activeKey, result);
    const history = getKeysHistory();
    const next = history.map(item => {
      if (String(item?.key || '').toUpperCase() !== activeKey.toUpperCase()) return item;
      return {
        ...item,
        registryVerified: !!result.registryVerified,
        activatedAt: result.activatedAt || item.activatedAt || 0,
        expiresAt: result.expiresAt || item.expiresAt || 0,
        status: result.status || item.status || ''
      };
    });
    localStorage.setItem(STORAGE_KEYS_HISTORY, JSON.stringify(next));
  } else if (!result.unavailable) {
    saveLicenseMeta(activeKey, { registryVerified: false, status: 'NOT_FOUND' });
  }

  updateCabinetLicenseStatus();
  renderKeysHistory();
}

// 5. Render Keys History Table
function getKeysHistory() {
  const raw = localStorage.getItem(STORAGE_KEYS_HISTORY);
  if (!raw) {
    localStorage.setItem(STORAGE_KEYS_HISTORY, JSON.stringify(defaultProfile.keys));
    return defaultProfile.keys;
  }
  try {
    return JSON.parse(raw);
  } catch (e) {
    return defaultProfile.keys;
  }
}

function renderKeysHistory() {
  const tableBody = document.getElementById('keysHistoryTableBody');
  if (!tableBody) return;

  const keys = getKeysHistory();
  if (!keys.length) {
    tableBody.innerHTML = '<tr class="license-empty-row"><td colspan="5">Лицензионных ключей пока нет.</td></tr>';
    return;
  }

  tableBody.innerHTML = keys.map((item, index) => {
    const meta = resolveLicenseMeta(item);
    const rawKey = String(item?.key || '');
    const copySafeKey = rawKey.toUpperCase().replace(/[^A-Z0-9_-]/g, '');
    const safeKey = escapeHtml(rawKey || '—');
    const safeCallsign = escapeHtml(item?.callsign || 'Пользователь');
    const safeUnit = escapeHtml(item?.unit || '');
    const days = licenseDaysRemaining(meta.expiresAt);
    const expiryDate = formatLicenseDate(meta.expiresAt);
    const activatedDate = formatLicenseDate(meta.activatedAt) || escapeHtml(item?.date || '');
    const active = !!meta.registryVerified && Number(meta.expiresAt || 0) > Date.now() && String(meta.status || 'ACTIVE').toUpperCase() === 'ACTIVE';
    const expired = !!meta.registryVerified && Number(meta.expiresAt || 0) > 0 && Number(meta.expiresAt) <= Date.now();

    let statusText = 'Срок не подтверждён';
    let statusClass = 'license-state-pending';
    if (active) {
      statusText = 'Осталось ' + (days ?? '—') + ' дн.';
      statusClass = 'license-state-active';
    } else if (expired) {
      statusText = 'Истекла';
      statusClass = 'license-state-expired';
    }

    const dateText = active && expiryDate
      ? 'до ' + expiryDate
      : (expired && expiryDate ? 'до ' + expiryDate : (activatedDate ? 'добавлен ' + activatedDate : '—'));

    return `
      <tr class="license-history-row ${index === 0 ? 'is-current' : ''}">
        <td data-label="Ключ">
          <span class="table-key-tag">${safeKey}</span>
        </td>
        <td data-label="Пользователь">
          <strong class="license-user">${safeCallsign}</strong>
          ${safeUnit ? `<div class="license-unit">${safeUnit}</div>` : ''}
        </td>
        <td data-label="Статус">
          <span class="license-state ${statusClass}">${escapeHtml(statusText)}</span>
        </td>
        <td data-label="Срок">
          <span class="license-history-date">${escapeHtml(dateText)}</span>
        </td>
        <td class="license-copy-cell">
          <button class="btn btn-outline btn-sm license-copy-btn" onclick="copyKeyText('${copySafeKey}')" title="Скопировать ключ">
            Копировать
          </button>
        </td>
      </tr>
    `;
  }).join('');
}

// 6. Copy Key Helpers
function copyCabinetKey() {
  const keyDisp = document.getElementById('cabActiveKeyDisp');
  if (!keyDisp) return;
  copyKeyText(keyDisp.textContent.trim());
}

function copyPaidKeyAction() {
  const keyDisp = document.getElementById('liveGeneratedKeyDisplay');
  if (!keyDisp) return;
  copyKeyText(keyDisp.textContent.trim());
}

function copyKeyText(text) {
  // Track Yandex Metrika goal for copying license key
  // Never send the license key itself to analytics.
  trackYm('reachGoal', 'license_key_copied');
  if (typeof window.gtag === 'function') {
    try {
      window.gtag('event', 'copy_license_key');
    } catch (e) {}
  }

  if (navigator.clipboard && window.isSecureContext) {
    navigator.clipboard.writeText(text).then(() => {
      showToast('Ключ лицензии скопирован в буфер обмена.');
    }).catch(() => {
      fallbackCopy(text);
    });
  } else {
    fallbackCopy(text);
  }
}

function fallbackCopy(text) {
  const textArea = document.createElement('textarea');
  textArea.value = text;
  textArea.style.position = 'fixed';
  textArea.style.left = '-999999px';
  textArea.style.top = '-999999px';
  document.body.appendChild(textArea);
  textArea.focus();
  textArea.select();
  try {
    document.execCommand('copy');
    showToast('Ключ лицензии скопирован в буфер обмена.');
  } catch (err) {
    prompt('Скопируйте ключ вручную:', text);
  }
  document.body.removeChild(textArea);
}

let paymentRequestInFlight = false;
let paymentCheckInFlight = false;
let paymentPollTimer = null;
let paymentPollAttempts = 0;
let paymentSessionGeneration = 0;

function paymentAccountScope() {
  return String(getActiveUserSession()?.email || '').trim().toLowerCase();
}

function paymentRandomToken() {
  if (!window.crypto?.getRandomValues) throw new Error('Браузер не поддерживает защищённую оплату. Обновите браузер и повторите.');
  const bytes = new Uint8Array(32);
  window.crypto.getRandomValues(bytes);
  return Array.from(bytes, value => value.toString(16).padStart(2, '0')).join('');
}

function readPaymentRequest() {
  try {
    const requests = JSON.parse(localStorage.getItem(STORAGE_PAYMENT_REQUEST) || '{}');
    const request = requests[paymentAccountScope()];
    if (!request || request.account !== paymentAccountScope() || !/^[a-f0-9]{64}$/.test(request.webClaimToken) || !/^[a-f0-9]{64}$/.test(request.idempotenceKey)) return null;
    return request;
  } catch (_) { return null; }
}

function paymentRequestMap() {
  try {
    const requests = JSON.parse(localStorage.getItem(STORAGE_PAYMENT_REQUEST) || '{}');
    return requests && typeof requests === 'object' && !Array.isArray(requests) ? requests : {};
  } catch (_) { return {}; }
}

function savePaymentRequest(request) {
  const requests = paymentRequestMap();
  requests[request.account] = request;
  localStorage.setItem(STORAGE_PAYMENT_REQUEST, JSON.stringify(requests));
}

function currentPaymentRequest(request) {
  return readPaymentRequest()?.idempotenceKey === request.idempotenceKey;
}

function pendingPaymentRequest() {
  const request = readPaymentRequest();
  return request && /^[A-Za-z0-9-]{8,100}$/.test(request.paymentId || '') ? request : null;
}

function safePaymentConfirmationUrl(value) {
  try {
    const url = new URL(value);
    const allowed = ['yoomoney.ru', 'yookassa.ru'].some(host => url.hostname === host || url.hostname.endsWith('.' + host));
    return url.protocol === 'https:' && !url.username && !url.password && (!url.port || url.port === '443') && allowed ? url.href : '';
  } catch (_) { return ''; }
}

function stopPaymentPolling() {
  if (paymentPollTimer) clearTimeout(paymentPollTimer);
  paymentPollTimer = null;
}

function setPaymentStatus(title, message) {
  const display = document.getElementById('liveGeneratedKeyDisplay');
  const status = document.getElementById('liveKeyStatusDisplay');
  if (display) { display.textContent = title; display.style.color = 'var(--accent-gold)'; }
  if (status) status.textContent = message;
  const copy = document.getElementById('btnCopyPaidKey');
  if (copy) { copy.setAttribute('disabled', 'true'); copy.style.display = 'none'; }
}

function showPendingPayment() {
  const initial = document.getElementById('boxPaymentInitial');
  const pending = document.getElementById('boxPaymentPending');
  if (initial) initial.style.display = 'none';
  if (pending) pending.style.display = 'block';
  const verify = document.getElementById('btnManualVerify');
  if (verify) verify.style.display = '';
}

function schedulePaymentCheck(delay = 5000) {
  stopPaymentPolling();
  if (!pendingPaymentRequest()) return;
  if (paymentPollAttempts >= 60) {
    const status = document.getElementById('liveKeyStatusDisplay');
    if (status) status.textContent = 'Автоматическая проверка приостановлена. Нажмите «Я оплатил — проверить», чтобы продолжить.';
    return;
  }
  paymentPollTimer = setTimeout(() => {
    paymentPollTimer = null;
    claimPaidLicenseKey({ automatic: true });
  }, delay);
}

function forgetPendingPayment() {
  stopPaymentPolling();
  localStorage.removeItem('kapterka_pending_payment_id');
  localStorage.removeItem('kapterka_pending_email');
  localStorage.removeItem('kapterka_pending_callsign');
  const requests = paymentRequestMap();
  delete requests[paymentAccountScope()];
  localStorage.setItem(STORAGE_PAYMENT_REQUEST, JSON.stringify(requests));
}

function pausePaymentChecks() {
  stopPaymentPolling();
  paymentSessionGeneration++;
  paymentRequestInFlight = false;
  paymentCheckInFlight = false;
  const initial = document.getElementById('boxPaymentInitial');
  const pending = document.getElementById('boxPaymentPending');
  if (initial) initial.style.display = 'block';
  if (pending) pending.style.display = 'none';
  const button = document.getElementById('btnPayYooKassaMain');
  if (button) { button.disabled = false; button.textContent = 'Оплатить 490 ₽ через ЮKassa'; }
  const verify = document.getElementById('btnManualVerify');
  if (verify) { verify.disabled = false; verify.textContent = 'Я оплатил — проверить'; }
  const cabinet = document.getElementById('btnGoToCabinetAfterPay');
  if (cabinet) cabinet.style.display = 'none';
  setPaymentStatus('ОЖИДАНИЕ ОПЛАТЫ', 'После подтверждения оплаты ключ появится здесь.');
}

function resumePendingPaymentChecks() {
  if (!pendingPaymentRequest()) return false;
  showPendingPayment();
  setPaymentStatus('ПРОВЕРКА ОПЛАТЫ', 'Проверяем сохранённый платёж на сервере.');
  paymentPollAttempts = 0;
  schedulePaymentCheck(0);
  return true;
}

async function processYooKassaPayment() {
  if (paymentRequestInFlight) return;
  const generation = paymentSessionGeneration;
  const callsign = document.getElementById('payCallsignInput')?.value.trim() || 'Боец';
  const email = document.getElementById('payEmailInput')?.value.trim().toLowerCase() || '';
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) || email.length > 160) {
    showToast('Проверьте Email для получения чека и ключа.');
    document.getElementById('payEmailInput')?.focus();
    return;
  }
  const button = document.getElementById('btnPayYooKassaMain');
  paymentRequestInFlight = true;
  if (button) { button.disabled = true; button.textContent = 'Создание платежа…'; }
  let paymentWindow = null;
  try { paymentWindow = window.open('about:blank', '_blank'); if (paymentWindow) paymentWindow.opener = null; } catch (_) {}
  try {
    let request = readPaymentRequest();
    // Retain the same order after a timeout: a retry must not create a second charge.
    if (!request || request.email !== email || request.callsign !== callsign) {
      request = { account: paymentAccountScope(), email, callsign, webClaimToken: paymentRandomToken(), idempotenceKey: paymentRandomToken(), createdAt: Date.now() };
      savePaymentRequest(request);
      localStorage.removeItem('kapterka_pending_payment_id');
    }
    const existingUrl = request.paymentId && safePaymentConfirmationUrl(request.confirmationUrl);
    const data = existingUrl ? { payment_id: request.paymentId, confirmation_url: existingUrl } : await requestPaymentApi('create', {
      email, callsign, web_claim_token: request.webClaimToken,
      idempotence_key: request.idempotenceKey,
      return_url: 'https://kapterka-pro.ru/?payment=check#tabPayment'
    });
    if (generation !== paymentSessionGeneration || !currentPaymentRequest(request)) {
      if (paymentWindow && !paymentWindow.closed) paymentWindow.close();
      return;
    }
    const confirmationUrl = safePaymentConfirmationUrl(data.confirmation_url);
    if (!/^[A-Za-z0-9-]{8,100}$/.test(data.payment_id || '') || !confirmationUrl) throw new Error(paymentErrorMessage('INVALID_PAYMENT_RESPONSE'));
    request = { ...request, paymentId: data.payment_id, confirmationUrl };
    // Persist identity and order before leaving the page for the bank.
    savePaymentRequest(request);
    localStorage.setItem('kapterka_pending_payment_id', request.paymentId);
    localStorage.setItem('kapterka_pending_callsign', callsign);
    localStorage.setItem('kapterka_pending_email', email);
    localStorage.setItem(STORAGE_USER_CALLSIGN, callsign);
    localStorage.setItem(STORAGE_USER_EMAIL, email);
    showPendingPayment();
    setPaymentStatus('ОЖИДАНИЕ ОПЛАТЫ', 'Завершите оплату в ЮKassa. Ключ появится здесь после подтверждения платежа.');
    paymentPollAttempts = 0;
    schedulePaymentCheck(3500);
    trackYm('reachGoal', 'initiate_yookassa_payment', { plan: 'PRO_30', amount: 490 });
    if (typeof window.gtag === 'function') {
      try { window.gtag('event', 'begin_checkout', { value: 490, currency: 'RUB', items: [{ item_name: 'Лицензия Каптёрка Про (30 дней)', price: 490, quantity: 1 }] }); } catch (_) {}
    }
    if (paymentWindow && !paymentWindow.closed) paymentWindow.location.replace(confirmationUrl);
    else window.location.href = confirmationUrl;
  } catch (error) {
    if (paymentWindow && !paymentWindow.closed) paymentWindow.close();
    if (generation !== paymentSessionGeneration) return;
    setPaymentStatus('ПЛАТЁЖ НЕ ОТКРЫТ', error.message || paymentErrorMessage('NETWORK_ERROR'));
    showToast(error.message || paymentErrorMessage('NETWORK_ERROR'));
  } finally {
    if (generation === paymentSessionGeneration) {
      paymentRequestInFlight = false;
      if (button) { button.disabled = false; button.textContent = 'Оплатить 490 ₽ через ЮKassa'; }
    }
  }
}

async function claimPaidLicenseKey({ automatic = false } = {}) {
  if (paymentCheckInFlight) return;
  const generation = paymentSessionGeneration;
  if (!automatic) { stopPaymentPolling(); paymentPollAttempts = 0; }
  const request = pendingPaymentRequest();
  if (!request) {
    if (!automatic) showToast('В этом браузере нет сохранённого счёта. Вернитесь в браузер, где начали оплату, или обратитесь в поддержку.');
    return;
  }
  paymentCheckInFlight = true;
  paymentPollAttempts++;
  const button = document.getElementById('btnManualVerify');
  if (button) { button.disabled = true; button.textContent = 'Проверка оплаты…'; }
  let retry = false;
  let retryDelay = 5000;
  try {
    const data = await requestPaymentApi('check', { payment_id: request.paymentId, web_claim_token: request.webClaimToken });
    // Logout or switching accounts while a request is running cannot restore its key.
    if (generation !== paymentSessionGeneration || !currentPaymentRequest(request) || pendingPaymentRequest()?.paymentId !== request.paymentId) return;
    if (data.paid === true) {
      const key = String(data.license_key || '').toUpperCase();
      const expiresAt = Number(data.expires_at || 0);
      if (data.status !== 'succeeded' || classifyLicenseKey(key) !== 'signed' || !Number.isFinite(expiresAt) || expiresAt <= Date.now()) {
        if (data.status === 'expired') {
          setPaymentStatus('СРОК ЗАВЕРШЁН', 'Срок лицензии по этому платежу уже завершён. Для продления создайте новый платёж.');
          forgetPendingPayment();
          const initial = document.getElementById('boxPaymentInitial');
          if (initial) initial.style.display = 'block';
          return;
        }
        const error = new Error(paymentErrorMessage('INVALID_PAYMENT_RESPONSE'));
        error.retryable = true;
        throw error;
      }
      forgetPendingPayment();
      applyNewPaidKey(key, request.callsign, { registryVerified: true, verified: true, status: 'ACTIVE', expiresAt, activatedAt: Number(data.activated_at || 0) });
      const pending = document.getElementById('boxPaymentPending');
      if (pending) pending.style.display = 'none';
      return;
    }
    if (data.status === 'canceled') {
      forgetPendingPayment();
      setPaymentStatus('ОПЛАТА ОТМЕНЕНА', 'ЮKassa не подтвердила оплату. Можно создать новый платёж.');
      const initial = document.getElementById('boxPaymentInitial');
      const pending = document.getElementById('boxPaymentPending');
      if (initial) initial.style.display = 'block';
      if (pending) pending.style.display = 'none';
      return;
    }
    retry = data.status === 'pending' || data.status === 'waiting_for_capture';
    setPaymentStatus('ОЖИДАНИЕ БАНКА', retry
      ? 'Оплата пока не завершена. Проверяем автоматически; можно повторить проверку этой кнопкой.'
      : 'Платёж пока не найден или не подтверждён. Повторите проверку или обратитесь в поддержку.');
  } catch (error) {
    if (generation !== paymentSessionGeneration || !currentPaymentRequest(request) || pendingPaymentRequest()?.paymentId !== request.paymentId) return;
    retry = !!error.retryable;
    retryDelay = 15000;
    setPaymentStatus('ОПЛАТА НЕ ПОДТВЕРЖДЕНА', error.message || paymentErrorMessage('NETWORK_ERROR'));
    if (!automatic) showToast(error.message || paymentErrorMessage('NETWORK_ERROR'));
  } finally {
    if (generation === paymentSessionGeneration) {
      paymentCheckInFlight = false;
      if (button) { button.disabled = false; button.textContent = 'Я оплатил — проверить'; }
      if (retry) schedulePaymentCheck(retryDelay);
    }
  }
}

// Активация ключа бойцом (из письма на Email, СМС или от администратора)
async function verifyWithManualOrderId() {
  const generation = paymentSessionGeneration;
  const input = document.getElementById('payOrderIdInput');
  const enteredKey = input ? input.value.trim().toUpperCase().replace(/\s+/g, '') : '';
  const callsign = localStorage.getItem('kapterka_pending_callsign') || 'Боец';

  if (!enteredKey) {
    showToast('Введите ключ лицензии (KAPT-XXXX-XXXX-XXXX)');
    return;
  }

  const keyKind = classifyLicenseKey(enteredKey);
  if (keyKind === 'invalid') {
    showToast('❌ Неверный формат ключа. Проверьте символы и дефисы.');
    return;
  }
  if (enteredKey.startsWith('KPT-')) {
    showToast('Этот старый формат ключа требует проверки поддержкой.');
    return;
  }

  showToast('Проверяю ключ в реестре лицензий…');
  const registry = await fetchLicenseRegistryMeta(enteredKey);
  if (generation !== paymentSessionGeneration) return;
  if (!registry.found || !registry.verified || Number(registry.expiresAt || 0) <= Date.now()) {
    showToast(registry.errorMessage || (registry.unavailable
      ? 'Не удалось проверить срок лицензии. Попробуйте ещё раз при стабильном интернете.'
      : 'Ключ не найден среди активных лицензий или срок уже завершён.'));
    return;
  }

  applyNewPaidKey(enteredKey, callsign, registry);
  input.value = '';
  showToast('✓ Лицензия подтверждена. Срок загружен из реестра.');
}

function applyNewPaidKey(newKey, callsign, meta = {}) {
  // Update Live Display
  const liveDisplay = document.getElementById('liveGeneratedKeyDisplay');
  const liveStatus = document.getElementById('liveKeyStatusDisplay');
  const btnCopy = document.getElementById('btnCopyPaidKey');
  const btnCab = document.getElementById('btnGoToCabinetAfterPay');

  if (liveDisplay) {
    liveDisplay.textContent = newKey;
    liveDisplay.style.color = 'var(--accent-gold)';
  }
  const daysLeft = licenseDaysRemaining(meta.expiresAt);
  if (liveStatus) liveStatus.textContent = meta.registryVerified
    ? '✓ Лицензия подтверждена • осталось ' + (daysLeft ?? '—') + ' дн.'
    : 'Ключ сохранён • срок не подтверждён';
  if (btnCopy) {
    btnCopy.style.display = 'block';
    btnCopy.removeAttribute('disabled');
  }
  if (btnCab) btnCab.style.display = 'block';

  // Update Local Storage active key
  localStorage.setItem(STORAGE_ACTIVE_KEY, newKey);
  const cabKeyDisp = document.getElementById('cabActiveKeyDisp');
  if (cabKeyDisp) cabKeyDisp.textContent = newKey;

  const cabActiveKeyCard = document.getElementById('cabActiveKeyCard');
  const cabNoKeyCard = document.getElementById('cabNoKeyCard');
  const navBadgeStatus = document.getElementById('navBadgeStatus');
  if (cabActiveKeyCard) cabActiveKeyCard.style.display = 'block';
  if (cabNoKeyCard) cabNoKeyCard.style.display = 'none';
  if (navBadgeStatus) {
    navBadgeStatus.textContent = 'ПРО';
    navBadgeStatus.style.display = 'inline-block';
  }

  // Add/update history without duplicating the same key.
  const history = getKeysHistory();
  const today = new Date().toLocaleDateString('ru-RU');
  const unitName = localStorage.getItem(STORAGE_UNIT_NAME) || '';
  const filteredHistory = history.filter(item => String(item?.key || '').toUpperCase() !== String(newKey || '').toUpperCase());
  const historyItem = {
    key: newKey,
    callsign: callsign,
    unit: unitName,
    status: meta.status || 'ACTIVE',
    date: meta.activatedAt ? formatLicenseDate(meta.activatedAt) : today,
    activatedAt: Number(meta.activatedAt || 0),
    expiresAt: Number(meta.expiresAt || 0),
    registryVerified: !!meta.registryVerified
  };
  const nextHistory = [historyItem, ...filteredHistory];

  localStorage.setItem(STORAGE_KEYS_HISTORY, JSON.stringify(nextHistory));
  saveLicenseMeta(newKey, historyItem);

  // Keep authenticated cabinet session aware of the newly linked license.
  try {
    const currentSession = getActiveUserSession();
    if (currentSession) {
      const updatedSession = {
        ...currentSession,
        activeKey: newKey,
        keys: nextHistory
      };
      localStorage.setItem(STORAGE_AUTH_USER, JSON.stringify(updatedSession));
    }
  } catch (e) {}

  updateCabinetLicenseStatus();
  renderKeysHistory();
  updateAuthUI();

  // Conversion tracking without sending the license key or personal data.
  trackYm('reachGoal', 'license_activated', { plan: 'PRO_30' });
  if (typeof window.gtag === 'function') {
    try {
      window.gtag('event', 'license_activated', { plan: 'PRO_30' });
    } catch (e) {}
  }

  showToast('Лицензия активирована. Ключ сохранён в личном кабинете.');
}

// Ручная привязка ключа бойцом в Личном кабинете (для синхронизации с приложением на Android)
async function linkLicenseKeyInCabinet() {
  const generation = paymentSessionGeneration;
  const input = document.getElementById('cabManualKeyInput');
  const key = input.value.trim().toUpperCase().replace(/\s+/g, '');

  if (!key) {
    showToast('Введите лицензионный ключ');
    return;
  }

  const keyKind = classifyLicenseKey(key);
  if (keyKind === 'invalid') {
    showToast('❌ Неверный формат ключа. Проверьте символы и дефисы.');
    return;
  }
  if (key.startsWith('KPT-')) {
    showToast('Этот старый формат ключа требует проверки поддержкой.');
    return;
  }

  const callsign = localStorage.getItem(STORAGE_USER_CALLSIGN) || 'Боец';
  showToast('Проверяю лицензию в облачном реестре…');
  const registry = await fetchLicenseRegistryMeta(key);
  if (generation !== paymentSessionGeneration) return;

  if (!registry.found || !registry.verified || Number(registry.expiresAt || 0) <= Date.now()) {
    showToast(registry.errorMessage || (registry.unavailable
      ? 'Не удалось проверить реестр. Ключ не активирован — попробуйте ещё раз при стабильном интернете.'
      : 'Ключ не найден среди активных лицензий или срок уже завершён.'));
    return;
  }

  applyNewPaidKey(key, callsign, registry);
  input.value = '';
  showToast('✓ Лицензия подтверждена. Счётчик срока обновлён.');
}

// 9. Modals Controller
let activeModalId = null;
let lastModalTrigger = null;

function getModalFocusable(modal) {
  if (!modal) return [];
  return Array.from(modal.querySelectorAll(
    'button:not([disabled]), a[href], input:not([disabled]), textarea:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"])'
  )).filter(el => el.offsetParent !== null);
}

window.openModal = function(id) {
  const modal = document.getElementById(id);
  if (!modal) return;

  lastModalTrigger = document.activeElement instanceof HTMLElement ? document.activeElement : null;
  activeModalId = id;
  modal.classList.add('open');
  modal.setAttribute('aria-hidden', 'false');
  document.body.classList.add('modal-open');

  const content = modal.querySelector('.modal-content');
  const title = modal.querySelector('h3');
  if (content) content.setAttribute('tabindex', '-1');
  if (title && !modal.hasAttribute('aria-label') && !modal.hasAttribute('aria-labelledby')) {
    modal.setAttribute('aria-label', title.textContent.trim());
  }

  requestAnimationFrame(() => {
    const focusable = getModalFocusable(modal);
    (focusable[0] || content || modal).focus?.();
  });
};

window.closeModal = function(id) {
  const modal = document.getElementById(id);
  if (!modal) return;

  modal.classList.remove('open');
  modal.setAttribute('aria-hidden', 'true');
  if (activeModalId === id) activeModalId = null;
  if (!document.querySelector('.modal-overlay.open')) document.body.classList.remove('modal-open');

  const trigger = lastModalTrigger;
  lastModalTrigger = null;
  if (trigger && document.contains(trigger)) requestAnimationFrame(() => trigger.focus?.());
};

document.addEventListener('keydown', (event) => {
  if (!activeModalId) return;
  const modal = document.getElementById(activeModalId);
  if (!modal || !modal.classList.contains('open')) return;

  if (event.key === 'Escape') {
    event.preventDefault();
    closeModal(activeModalId);
    return;
  }

  if (event.key === 'Tab') {
    const focusable = getModalFocusable(modal);
    if (!focusable.length) {
      event.preventDefault();
      modal.querySelector('.modal-content')?.focus();
      return;
    }
    const first = focusable[0];
    const last = focusable[focusable.length - 1];
    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault();
      first.focus();
    }
  }
});

// Shared HTML escaping helper
function escapeHtml(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

// 10. Initialization on DOMContentLoaded
document.addEventListener('DOMContentLoaded', () => {
  // Anonymous funnel analytics; no personal data is included.
  installSiteFunnelTracking();
  loadReleaseManifest();
  installPreviewGallery();
  installProfessionalReveal();

  // Check user session state and setup auth UI
  updateAuthUI();

  // Load profile data
  loadCabinetProfile();
  refreshStoredLicenseStatus();

  function enableTablistKeyboardNavigation(tablist) {
    if (!tablist) return;
    const tabs = Array.from(tablist.querySelectorAll('[role="tab"]'));
    tabs.forEach((tab, index) => {
      tab.addEventListener('keydown', (event) => {
        let targetIndex = null;
        if (event.key === 'ArrowRight') targetIndex = (index + 1) % tabs.length;
        if (event.key === 'ArrowLeft') targetIndex = (index - 1 + tabs.length) % tabs.length;
        if (event.key === 'Home') targetIndex = 0;
        if (event.key === 'End') targetIndex = tabs.length - 1;
        if (targetIndex === null) return;

        event.preventDefault();
        const target = tabs[targetIndex];
        target.focus();
        const panelId = target.getAttribute('aria-controls');
        if (panelId) switchMainTab(panelId);
      });
    });
  }

  document.querySelectorAll('[role="tablist"]').forEach(enableTablistKeyboardNavigation);

  // Mobile Toggle
  const mobileToggle = document.getElementById('mobileToggle');
  const mainTabNav = document.getElementById('mainTabNav');
  if (mobileToggle && mainTabNav) {
    mobileToggle.setAttribute('aria-expanded', 'false');
    mobileToggle.setAttribute('aria-controls', 'mainTabNav');
    mobileToggle.addEventListener('click', () => {
      const opened = mainTabNav.classList.toggle('open');
      mobileToggle.setAttribute('aria-expanded', opened ? 'true' : 'false');
    });
  }

  // Initialize modal accessibility state.
  document.querySelectorAll('.modal-overlay').forEach(modal => modal.setAttribute('aria-hidden', 'true'));

  // Close modals when clicking outside
  document.querySelectorAll('.modal-overlay').forEach(overlay => {
    overlay.addEventListener('click', (e) => {
      if (e.target === overlay) {
        closeModal(overlay.id);
      }
    });
  });

  // Contact form submission -> Direct to Telegram Bot
  const contactForm = document.getElementById('contactForm');
  if (contactForm) {
    contactForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const name = document.getElementById('contactNameInput')?.value.trim() || 'Без имени';
      const info = document.getElementById('contactInfoInput')?.value.trim() || 'Не указан';
      const msg = document.getElementById('contactMessageInput')?.value.trim() || '';

      const btnSubmit = document.getElementById('btnSubmitContact');
      if (btnSubmit) {
        btnSubmit.setAttribute('disabled', 'true');
        btnSubmit.textContent = 'Отправка...';
      }

      const delivered = await sendTelegramNotification(
        `✉️ <b>Новое обращение с сайта «Каптёрка PRO»</b>\n\n` +
        `👤 <b>От кого:</b> ${escapeTelegramHtml(name)}\n` +
        `📞 <b>Связь:</b> ${escapeTelegramHtml(info)}\n` +
        `💬 <b>Вопрос:</b>\n${escapeTelegramHtml(msg)}\n\n` +
        `📅 <b>Дата:</b> ${escapeTelegramHtml(new Date().toLocaleString('ru-RU'))}`
      );

      if (btnSubmit) {
        btnSubmit.removeAttribute('disabled');
        btnSubmit.textContent = 'Отправить сообщение';
      }

      if (delivered) {
        contactForm.reset();
        showToast('✓ Сообщение отправлено в поддержку.');
        closeModal('modalContact');
      } else {
        showToast('Не удалось отправить сообщение. Используйте Telegram или Email.');
      }
    });
  }

  // A return URL only opens the status screen. Only the saved order and server
  // confirmation can supply a key; never trust a URL's success/payment_id flags.
  const urlParams = new URLSearchParams(window.location.search);
  if (urlParams.get('payment') === 'success' || urlParams.get('payment') === 'check') {
    switchMainTab('tabPayment');
    if (!resumePendingPaymentChecks()) {
      setPaymentStatus('НЕТ СОХРАНЁННОГО СЧЁТА', 'Откройте браузер и аккаунт, где начали оплату, или обратитесь в поддержку.');
    }
  } else {
    resumePendingPaymentChecks();
  }

});

// Restore tab from URL hash on refresh/back-forward navigation.
document.addEventListener('DOMContentLoaded', () => {
  const allowedTabs = new Set(['tabOverview','tabCabinet','tabPayment','tabSync','tabDownload']);
  const fromHash = (window.location.hash || '').replace('#','');
  if (allowedTabs.has(fromHash)) {
    switchMainTab(fromHash);
  }
});

function restoreTabFromLocation() {
  const allowed = ['tabOverview','tabCabinet','tabPayment','tabSync','tabDownload'];
  const tabId = (window.location.hash || '').replace('#','');
  const resolved = allowed.includes(tabId) ? tabId : 'tabOverview';
  const target = document.getElementById(resolved);
  if (target && !target.classList.contains('active')) {
    // Temporarily normalize the URL before calling switchMainTab so it does not push a duplicate entry.
    if (!tabId && history?.replaceState) history.replaceState({ tabId: resolved }, '', '#' + resolved);
    switchMainTab(resolved);
  }
}

window.addEventListener('hashchange', restoreTabFromLocation);
window.addEventListener('popstate', restoreTabFromLocation);

// SCREENSHOT LIGHTBOX
// Lightbox modal opener
const SCREENSHOT_GALLERY = [
  { src: 'screens/36-2-dashboard.jpg', title: 'Главная — Склады, остатки и быстрые операции', alt: 'Каптёрка PRO 3.6 — Главная' },
  { src: 'screens/36-3-stock-table.jpg', title: 'Остатки — Приход, расход и остаток по складу', alt: 'Каптёрка PRO 3.6 — Остатки' },
  { src: 'screens/36-4-requests.jpg', title: 'Заявки — Новая → Сборка → Собрана → Выдана', alt: 'Каптёрка PRO 3.6 — Заявки' },
  { src: 'screens/36-5-journal.jpg', title: 'Журнал — Операции по дням и за период', alt: 'Каптёрка PRO 3.6 — Журнал' },
  { src: 'screens/36-6-unit-qr.jpg', title: 'Подключение по QR — Второй телефон за секунды', alt: 'Каптёрка PRO 3.6 — Подключение по QR' },
  { src: 'screens/36-8-form8.jpg', title: 'Форма № 8 — Раздаточная ведомость в Excel', alt: 'Каптёрка PRO 3.6 — Форма № 8' },
  { src: 'screens/36-7-pro.jpg', title: 'PRO — Понятная оплата и ключ на почту', alt: 'Каптёрка PRO 3.6 — PRO' }
];
let screenshotGalleryIndex = 0;
let previewGalleryIndex = 0;

function normalizeGalleryIndex(index) {
  const length = SCREENSHOT_GALLERY.length || 1;
  return ((Number(index) || 0) % length + length) % length;
}

function updatePreviewGalleryState(index) {
  previewGalleryIndex = normalizeGalleryIndex(index);
  const cards = Array.from(document.querySelectorAll('#previewGalleryTrack [data-gallery-index]'));
  cards.forEach((card, i) => card.classList.toggle('active', i === previewGalleryIndex));

  const dots = Array.from(document.querySelectorAll('#previewGalleryDots button'));
  dots.forEach((dot, i) => {
    dot.classList.toggle('active', i === previewGalleryIndex);
    if (i === previewGalleryIndex) dot.setAttribute('aria-current', 'true');
    else dot.removeAttribute('aria-current');
  });

  const count = document.getElementById('previewGalleryCount');
  if (count) count.textContent = (previewGalleryIndex + 1) + ' / ' + SCREENSHOT_GALLERY.length;
}

function movePreviewGalleryTo(index, smooth = true) {
  updatePreviewGalleryState(index);
  const card = document.querySelector('#previewGalleryTrack [data-gallery-index="' + previewGalleryIndex + '"]');
  card?.scrollIntoView({
    behavior: smooth && !window.matchMedia?.('(prefers-reduced-motion: reduce)')?.matches ? 'smooth' : 'auto',
    block: 'nearest',
    inline: 'center'
  });
}

function movePreviewGallery(delta) {
  movePreviewGalleryTo(previewGalleryIndex + Number(delta || 0));
}

function renderGalleryModalSlide(index) {
  screenshotGalleryIndex = normalizeGalleryIndex(index);
  const item = SCREENSHOT_GALLERY[screenshotGalleryIndex];
  const img = document.getElementById('modalScreenshotImg');
  const titleEl = document.getElementById('modalScreenshotTitle');
  const dl = document.getElementById('modalScreenshotDownload');
  const counter = document.getElementById('modalScreenshotCounter');
  if (!item || !img) return;

  img.src = item.src;
  img.alt = item.alt || item.title;
  if (titleEl) titleEl.textContent = item.title;
  if (dl) {
    dl.href = item.src;
    dl.setAttribute('download', item.src);
  }
  if (counter) counter.textContent = (screenshotGalleryIndex + 1) + ' / ' + SCREENSHOT_GALLERY.length;
}

function openGallerySlide(index) {
  renderGalleryModalSlide(index);
  trackSiteAction('gallery_open', { slide: screenshotGalleryIndex + 1 });
  openModal('modalScreenshot');
}

function shiftGallerySlide(delta) {
  renderGalleryModalSlide(screenshotGalleryIndex + Number(delta || 0));
  trackSiteAction('gallery_navigate', { slide: screenshotGalleryIndex + 1 });
}

function openLightbox(imgSrc, title) {
  const knownIndex = SCREENSHOT_GALLERY.findIndex(item => item.src === imgSrc);
  if (knownIndex >= 0) {
    openGallerySlide(knownIndex);
    return;
  }

  const modal = document.getElementById('modalScreenshot');
  const img = document.getElementById('modalScreenshotImg');
  const titleEl = document.getElementById('modalScreenshotTitle');
  const dl = document.getElementById('modalScreenshotDownload');
  const counter = document.getElementById('modalScreenshotCounter');
  if (!modal || !img) return;

  img.src = imgSrc;
  img.alt = title || 'Скриншот приложения';
  if (titleEl) titleEl.textContent = title || 'Скриншот приложения';
  if (dl) {
    dl.href = imgSrc;
    dl.setAttribute('download', imgSrc);
  }
  if (counter) counter.textContent = '1 / 1';
  openModal('modalScreenshot');
}

async function loadReleaseManifest() {
  try {
    const response = await fetch('release.json?_=' + Date.now(), { cache: 'no-store' });
    if (!response.ok) return;
    const release = await response.json();
    if (!release || !release.versionName || !release.versionCode) return;
    window.KAPTERKA_RELEASE = release;

    document.querySelectorAll('[data-release-version]').forEach(el => { el.textContent = release.versionName; });
    document.querySelectorAll('[data-release-code]').forEach(el => { el.textContent = String(release.versionCode); });
    document.querySelectorAll('[data-release-status]').forEach(el => { el.textContent = release.statusLabel || 'Стабильный релиз'; });
    document.querySelectorAll('[data-release-sha]').forEach(el => { el.textContent = release.apkSha256; });
    document.querySelectorAll('[data-release-size]').forEach(el => { el.textContent = (release.apkSize / 1024 / 1024).toFixed(2).replace('.', ',') + ' МиБ (' + release.apkSize + ' байт)'; });
    document.querySelectorAll('[data-release-integrity]').forEach(el => { el.textContent = release.versionName + ' • build ' + release.versionCode + ' • ' + release.apkSize + ' байт'; });
  } catch (_) {}
}

function installProfessionalReveal() {
  if (!('IntersectionObserver' in window) || window.matchMedia?.('(prefers-reduced-motion: reduce)')?.matches) return;
  const sections = Array.from(document.querySelectorAll('.neo-home > section')).slice(1);
  const observer = new IntersectionObserver(entries => {
    entries.forEach(entry => {
      if (!entry.isIntersecting) return;
      entry.target.classList.add('is-revealed');
      observer.unobserve(entry.target);
    });
  }, { rootMargin: '0px 0px -8% 0px', threshold: 0.08 });
  sections.forEach(section => {
    section.classList.add('pro-reveal');
    observer.observe(section);
  });
}

function installPreviewGallery() {
  updatePreviewGalleryState(0);
  const track = document.getElementById('previewGalleryTrack');
  if (track) {
    let rafId = 0;
    track.addEventListener('scroll', () => {
      if (rafId) cancelAnimationFrame(rafId);
      rafId = requestAnimationFrame(() => {
        const cards = Array.from(track.querySelectorAll('[data-gallery-index]'));
        if (!cards.length) return;
        const center = track.getBoundingClientRect().left + track.clientWidth / 2;
        let nearestIndex = 0;
        let nearestDistance = Infinity;
        cards.forEach((card, index) => {
          const rect = card.getBoundingClientRect();
          const distance = Math.abs((rect.left + rect.width / 2) - center);
          if (distance < nearestDistance) {
            nearestDistance = distance;
            nearestIndex = index;
          }
        });
        updatePreviewGalleryState(nearestIndex);
      });
    }, { passive: true });
  }

  const frame = document.getElementById('modalScreenshotFrame');
  if (frame) {
    let touchStartX = null;
    frame.addEventListener('touchstart', event => {
      touchStartX = event.changedTouches?.[0]?.clientX ?? null;
    }, { passive: true });
    frame.addEventListener('touchend', event => {
      if (touchStartX === null) return;
      const endX = event.changedTouches?.[0]?.clientX ?? touchStartX;
      const delta = endX - touchStartX;
      touchStartX = null;
      if (Math.abs(delta) >= 42) shiftGallerySlide(delta < 0 ? 1 : -1);
    }, { passive: true });
  }
}

document.addEventListener('keydown', event => {
  if (activeModalId !== 'modalScreenshot') return;
  if (event.key === 'ArrowLeft') {
    event.preventDefault();
    shiftGallerySlide(-1);
  } else if (event.key === 'ArrowRight') {
    event.preventDefault();
    shiftGallerySlide(1);
  }
});

// Privacy-safe site funnel analytics.
// Never send Email, callsign, license keys, unit keys or payment identifiers.
function trackSiteAction(eventName, params = {}) {
  const safeParams = { ...params, web_version: window.KAPTERKA_WEB_VERSION || 'unknown' };
  try {
    trackYm('reachGoal', eventName, safeParams);
  } catch (_) {}
  if (typeof window.gtag === 'function') {
    try {
      window.gtag('event', eventName, safeParams);
    } catch (_) {}
  }
}

function trackApkDownload(source) {
  const placement = String(source || 'direct').slice(0, 40);
  trackSiteAction('apk_download', { source: placement });
  const version = window.KAPTERKA_RELEASE?.versionName;
  showToast('📥 Открыта ссылка на APK «Каптёрка PRO' + (version ? ' ' + version : '') + '».');
}

function installSiteFunnelTracking() {
  document.addEventListener('click', (event) => {
    const link = event.target?.closest?.('a[href]');
    if (!link) return;
    const href = String(link.getAttribute('href') || '');

    if (href === 'help.html' || href.endsWith('/help.html')) {
      trackSiteAction('support_center_open', { source: 'site' });
    } else if (href.includes('apps.rustore.ru/app/com.aistudio.kapterka.jmwqve')) {
      trackSiteAction('rustore_open', { source: 'site' });
    } else if (href.includes('t.me/kapterka_help_bot')) {
      trackSiteAction('support_open', { channel: 'telegram_bot' });
    } else if (href.includes('t.me/kapterka_pro')) {
      trackSiteAction('telegram_channel_open', { source: 'site' });
    } else if (href.startsWith('mailto:')) {
      trackSiteAction('support_open', { channel: 'email' });
    }
  }, { passive: true });

  document.querySelectorAll('.lp-faq-list details').forEach((details, index) => {
    details.addEventListener('toggle', () => {
      if (details.open) trackSiteAction('faq_open', { faq_index: index + 1 });
    });
  });

  const depthSent = new Set();
  window.addEventListener('scroll', () => {
    const doc = document.documentElement;
    const scrollable = Math.max(1, doc.scrollHeight - window.innerHeight);
    const percent = Math.round((window.scrollY / scrollable) * 100);
    [50, 90].forEach(mark => {
      if (percent >= mark && !depthSent.has(mark)) {
        depthSent.add(mark);
        trackSiteAction('scroll_depth', { percent: mark });
      }
    });
  }, { passive: true });
}
