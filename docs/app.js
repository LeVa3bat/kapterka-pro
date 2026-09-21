// ==========================================================================
// KAPTERKA PRO — APP.JS (WITH USER REGISTRATION, EMAIL VERIFICATION & CABINET)
// ==========================================================================

// Global state keys for persistence

// 🌐 ССЫЛКА НА GOOGLE APPS SCRIPT (Для полной автоматизации ЮKassa и Telegram)
window.KAPTERKA_API_URL = 'https://script.google.com/macros/s/AKfycbwuwY74vD9El1R6ZVvO3DDpJ7BkY-wX0ljRphWRSA-jgB33-duUAqEp0g03D_7oFzjqmA/exec';

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

// Default initial state for clean empty inputs
const YM_IDS = [112482290, 112255061];
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
}

function logoutUserSession() {
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

  const adminQuickBadge = document.getElementById('adminQuickBadge');

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
    // Show admin link if developer or admin
    if (adminQuickBadge) {
      const isDev = user.isAdmin === true || localStorage.getItem('kapterka_admin_mode') === 'true';
      adminQuickBadge.style.display = isDev ? 'inline-flex' : 'none';
    }
  } else {
    if (authContainer) authContainer.style.display = 'block';
    if (cabinetContent) cabinetContent.style.display = 'none';
    if (navCallsign) navCallsign.textContent = 'Войти / Регистрация';
    if (adminQuickBadge) adminQuickBadge.style.display = 'none';
  }
}

// 2. Main Tab Switching Controller
function switchMainTab(tabId) {
  // Hide all tabs
  document.querySelectorAll('.app-view-tab').forEach(tab => {
    tab.classList.remove('active');
  });

  // Remove active state from tab navigation buttons
  document.querySelectorAll('.main-tab-btn').forEach(btn => {
    btn.classList.remove('active');
  });

  // Activate selected tab
  const targetTab = document.getElementById(tabId);
  if (targetTab) {
    targetTab.classList.add('active');
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
    if (activeBtn) activeBtn.classList.add('active');
  }

  // Update mobile bottom nav highlight
  document.querySelectorAll('.mobile-bottom-tab-btn').forEach(btn => {
    btn.classList.remove('active');
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
    if (mobileBtn) mobileBtn.classList.add('active');
  }

  // Keep a shareable/restorable URL without reloading the page.
  try {
    if (history && history.replaceState) {
      history.replaceState(null, '', '#' + tabId);
    }
  } catch (e) {}

  // Refresh payment form from the authenticated/local profile when opening payment.
  if (tabId === 'tabPayment') {
    const currentUser = getActiveUserSession();
    const callsign = currentUser?.callsign || localStorage.getItem(STORAGE_USER_CALLSIGN) || '';
    const email = currentUser?.email || localStorage.getItem(STORAGE_USER_EMAIL) || '';
    const payCallsignInput = document.getElementById('payCallsignInput');
    const payEmailInput = document.getElementById('payEmailInput');
    if (payCallsignInput && callsign) payCallsignInput.value = callsign;
    if (payEmailInput && email) payEmailInput.value = email;
  }

  // Scroll smoothly to top of content
  window.scrollTo({ top: 0, behavior: 'smooth' });

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

  // Управление карточками ключа (есть активный ключ vs нет ключа)
  const cabActiveKeyCard = document.getElementById('cabActiveKeyCard');
  const cabNoKeyCard = document.getElementById('cabNoKeyCard');

  if (activeKey && activeKey.startsWith('KAPT-')) {
    if (cabActiveKeyCard) cabActiveKeyCard.style.display = 'block';
    if (cabNoKeyCard) cabNoKeyCard.style.display = 'none';
    if (navBadgeStatus) {
      navBadgeStatus.textContent = 'ПРО';
      navBadgeStatus.style.display = 'inline-block';
    }
  } else {
    if (cabActiveKeyCard) cabActiveKeyCard.style.display = 'none';
    if (cabNoKeyCard) cabNoKeyCard.style.display = 'block';
    if (navBadgeStatus) {
      navBadgeStatus.style.display = 'none';
    }
  }

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

function generateMilitaryLicenseKey() {
  function seg(len) {
    let s = '';
    for (let i = 0; i < len; i++) {
      s += CHECKSUM_CHARS.charAt(Math.floor(Math.random() * CHECKSUM_CHARS.length));
    }
    return s;
  }
  const p1 = seg(4);
  const p2 = seg(4);
  const p3 = computeKeyChecksum(p1, p2);
  return `KAPT-${p1}-${p2}-${p3}`;
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
  tableBody.innerHTML = keys.map((item, index) => {
    const isPrimary = index === 0;
    const rawKey = String(item?.key || '');
    // License keys are limited to their expected character set before being used in an inline handler.
    const copySafeKey = rawKey.toUpperCase().replace(/[^A-Z0-9_-]/g, '');
    const safeKey = escapeHtml(rawKey || '—');
    const safeCallsign = escapeHtml(item?.callsign || 'Пользователь');
    const safeUnit = escapeHtml(item?.unit || 'Подразделение');
    const safeStatus = escapeHtml(item?.status || '—');
    const safeDate = escapeHtml(item?.date || '—');
    return `
      <tr>
        <td>
          <span class="table-key-tag">${safeKey}</span>
        </td>
        <td>
          <strong style="color:var(--text-primary);">${safeCallsign}</strong>
          <div style="font-size:0.75rem; color:var(--text-muted);">${safeUnit}</div>
        </td>
        <td>
          <span class="badge ${isPrimary ? 'badge-gold' : ''}" style="font-size:0.72rem; padding:2px 8px;">
            ${safeStatus}
          </span>
        </td>
        <td style="font-family:var(--font-mono); font-size:0.8rem; color:var(--text-secondary);">
          ${safeDate}
        </td>
        <td>
          <button class="btn btn-primary btn-sm" onclick="copyKeyText('${copySafeKey}')" title="Скопировать">
            Скопировать
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
      showToast(`Ключ ${text} скопирован в буфер обмена!`);
    }).catch(() => {
      fallbackCopy(text);
    });
  } else {
    fallbackCopy(text);
  }
}

// Track APK Download & notify user
function trackApkDownload(source) {
  trackYm('reachGoal', 'apk_download_started', { source: source || 'direct' });
  if (typeof window.gtag === 'function') {
    try {
      window.gtag('event', 'download_apk', { event_category: 'APK', event_label: source || 'direct' });
    } catch (e) {}
  }
  showToast('📥 Скачивание APK-файла «Каптёрка Про v3.4.9» началось...');
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
    showToast(`Ключ ${text} скопирован в буфер обмена!`);
  } catch (err) {
    prompt('Скопируйте ключ вручную:', text);
  }
  document.body.removeChild(textArea);
}

const YOOKASSA_PAYMENT_URL = 'https://yookassa.ru/my/i/apiQMG65ZHIE/l';

// 7. YooKassa Real Payment & Automated Verification Flow
let paymentPollingTimer = null;
let paymentPollingSeconds = 0;

async function processYooKassaPayment() {
  const callsign = document.getElementById('payCallsignInput')?.value.trim() || 'Боец';
  const email = document.getElementById('payEmailInput')?.value.trim() || '';

  if (!email || !email.includes('@')) {
    showToast('⚠️ Укажите ваш Email для получения чека 54-ФЗ и ключа!');
    document.getElementById('payEmailInput')?.focus();
    return;
  }

  // Фиксируем уникальный номер заказа
  const paymentSessionId = 'yk_' + Date.now().toString(36).toUpperCase() + '-' + Math.random().toString(36).substring(2, 6).toUpperCase();
  localStorage.setItem('kapterka_pending_payment_id', paymentSessionId);
  localStorage.setItem('kapterka_pending_callsign', callsign);
  localStorage.setItem('kapterka_pending_email', email);
  localStorage.setItem(STORAGE_USER_CALLSIGN, callsign);
  localStorage.setItem(STORAGE_USER_EMAIL, email);

  // Переключаем интерфейс на шаг ожидания
  const boxInitial = document.getElementById('boxPaymentInitial');
  const boxPending = document.getElementById('boxPaymentPending');
  if (boxInitial) boxInitial.style.display = 'none';
  if (boxPending) boxPending.style.display = 'block';

  // Индикация в правой колонке
  const liveDisplay = document.getElementById('liveGeneratedKeyDisplay');
  const liveStatus = document.getElementById('liveKeyStatusDisplay');
  const btnCopy = document.getElementById('btnCopyPaidKey');

  if (liveDisplay) {
    liveDisplay.textContent = 'ОЖИДАНИЕ ОПЛАТЫ';
    liveDisplay.style.color = 'var(--accent-gold)';
  }
  if (liveStatus) {
    liveStatus.innerHTML = `Счёт 490 ₽ выставлен. Чек 54-ФЗ и ключ направляются на <b>${escapeHtml(email)}</b> после поступления средств.`;
  }
  if (btnCopy) btnCopy.setAttribute('disabled', 'true');

  showToast('Открытие платежного шлюза ЮKassa...');

  // Telegram Alert: боец перешел к оплате
  sendTelegramNotification(
    `💳 <b>Новый переход к оплате (490 ₽)</b>\n\n` +
    `👤 <b>Позывной:</b> ${escapeTelegramHtml(callsign)}\n` +
    `📧 <b>Email:</b> ${escapeTelegramHtml(email)}\n` +
    `🆔 <b>Номер заказа:</b> <code>${paymentSessionId}</code>\n` +
    `🏦 <b>Магазин:</b> ЮKassa ID 1450722\n` +
    `📅 <b>Время:</b> ${new Date().toLocaleString('ru-RU')}`
  );

  // Track Yandex Metrika goal for payment initiation
  // Do not send Email or callsign to analytics.
  trackYm('reachGoal', 'initiate_yookassa_payment', { plan: 'PRO_30', amount: 490 });
  if (typeof window.gtag === 'function') {
    try {
      window.gtag('event', 'begin_checkout', {
        value: 490,
        currency: 'RUB',
        items: [{ item_name: 'Лицензия Каптёрка Про (30 дней)', price: 490, quantity: 1 }]
      });
    } catch (e) {}
  }

  // Полная интеграция ЮKassa по API через Google Apps Script (JSONP CORS Bypass)
  const API_URL = window.KAPTERKA_API_URL || '';
  
  if (API_URL) {
    showToast('Создание уникального защищенного платежа...');
    
    // Генерируем уникальное имя функции для JSONP
    const callbackName = 'jsonp_callback_' + Math.round(100000 * Math.random());
    
    // Создаем функцию глобально
    window[callbackName] = function(data) {
      // Удаляем скрипт
      document.getElementById(callbackName)?.remove();
      delete window[callbackName];
      
      if (data.confirmation_url) {
        window.open(data.confirmation_url, '_blank');
        localStorage.setItem('kapterka_pending_payment_id', data.payment_id);
      } else {
        showToast('Ошибка при соединении с сервером. Перенаправление на резервную ссылку...');
        console.error(data.error);
        setTimeout(() => window.open(YOOKASSA_PAYMENT_URL, '_blank'), 1000);
      }
    };
    
    // Формируем URL с параметрами GET (JSONP)
    const scriptUrl = `${API_URL}?action=pay&email=${encodeURIComponent(email)}&callsign=${encodeURIComponent(callsign)}&callback=${callbackName}`;
    
    // Создаем тег script и добавляем на страницу
    const script = document.createElement('script');
    script.id = callbackName;
    script.src = scriptUrl;
    
    // Обработка ошибки загрузки
    script.onerror = function() {
      document.getElementById(callbackName)?.remove();
      delete window[callbackName];
      showToast('Сбой соединения. Перенаправление на базовую кассу...');
      setTimeout(() => window.open(YOOKASSA_PAYMENT_URL, '_blank'), 1000);
    };
    
    document.body.appendChild(script);
    
  } else {
    // Резервный режим
    window.open(YOOKASSA_PAYMENT_URL, '_blank');
  }
}

// Завершение оплаты и получение ключа (Запрос ключа у администратора)
async function claimPaidLicenseKey() {
  const callsign = localStorage.getItem('kapterka_pending_callsign') || document.getElementById('payCallsignInput')?.value.trim() || 'Боец';
  const email = localStorage.getItem('kapterka_pending_email') || document.getElementById('payEmailInput')?.value.trim() || '';
  const btnClaim = document.getElementById('btnManualVerify');

  if (btnClaim) {
    btnClaim.setAttribute('disabled', 'true');
    btnClaim.innerHTML = '⏳ Отправка запроса...';
  }

  // Отправляем оповещение в Telegram бот
  await sendTelegramNotification(
    `🎖️ <b>ОЖИДАЕТСЯ ПОДТВЕРЖДЕНИЕ ПЛАТЕЖА (490 ₽)</b>\n\n` +
    `👤 <b>Боец:</b> ${escapeTelegramHtml(callsign)}\n` +
    `📧 <b>Email:</b> ${escapeTelegramHtml(email || 'Не указан')}\n\n` +
    `⚠️ <b>ВНИМАНИЕ:</b> Боец нажал кнопку "Я оплатил". Если ЮКасса не прислала уведомление вебхуком, проверьте оплату вручную и передайте ключ.`
  );

  if (btnClaim) {
    btnClaim.style.display = 'none';
  }
  
  // Показываем сообщение бойцу
  const liveDisplay = document.getElementById('liveGeneratedKeyDisplay');
  const liveStatus = document.getElementById('liveKeyStatusDisplay');
  const btnCopy = document.getElementById('btnCopyPaidKey');
  
  if (liveDisplay) {
    liveDisplay.textContent = 'ОЖИДАНИЕ БАНКА';
    liveDisplay.style.color = 'var(--accent-gold)';
    liveDisplay.style.fontSize = '1.2rem';
    liveDisplay.style.letterSpacing = 'normal';
  }
  
  if (liveStatus) {
    liveStatus.innerHTML = `⏳ <b>Система ожидает подтверждения от банка.</b><br>После подтверждения платежа ключ будет направлен на <b>${escapeHtml(email || 'указанную при оплате')}</b>.<br><br>Если письмо не придёт, обратитесь в поддержку.`;
  }

  if (btnCopy) {
    btnCopy.setAttribute('disabled', 'true');
  }

  showToast(`✓ Запрос передан. Ожидайте письмо с ключом на почту!`);
}

// Активация ключа бойцом (из письма на Email, СМС или от администратора)
function verifyWithManualOrderId() {
  const input = document.getElementById('payOrderIdInput');
  const enteredKey = input ? input.value.trim().toUpperCase().replace(/\s+/g, '') : '';
  const callsign = localStorage.getItem('kapterka_pending_callsign') || 'Боец';

  if (!enteredKey) {
    showToast('Введите ключ лицензии (KAPT-XXXX-XXXX-XXXX)');
    return;
  }

  // Проверка криптографической подписи ключа
  if (!verifyKeyChecksum(enteredKey)) {
    showToast('❌ Ошибка: Ключ не прошел проверку подлинности!');
    return;
  }

  applyNewPaidKey(enteredKey, callsign);
  input.value = '';
  showToast(`✓ Ключ ${enteredKey} успешно активирован на 30 дней!`);
}

function applyNewPaidKey(newKey, callsign) {
  // Update Live Display
  const liveDisplay = document.getElementById('liveGeneratedKeyDisplay');
  const liveStatus = document.getElementById('liveKeyStatusDisplay');
  const btnCopy = document.getElementById('btnCopyPaidKey');
  const btnMail = document.getElementById('btnSendKeyToEmail');
  const btnCab = document.getElementById('btnGoToCabinetAfterPay');

  if (liveDisplay) {
    liveDisplay.textContent = newKey;
    liveDisplay.style.color = 'var(--accent-gold)';
  }
  if (liveStatus) liveStatus.textContent = '✓ Лицензия активна • 30 дней доступа';
  if (btnCopy) {
    btnCopy.style.display = 'block';
    btnCopy.removeAttribute('disabled');
  }
  if (btnMail) btnMail.style.display = 'block';
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

  // Add to History
  const history = getKeysHistory();
  const today = new Date().toLocaleDateString('ru-RU');
  const unitName = localStorage.getItem(STORAGE_UNIT_NAME) || '';

  history.unshift({
    key: newKey,
    callsign: callsign,
    unit: unitName,
    status: 'Активен (30 дн)',
    date: today
  });

  localStorage.setItem(STORAGE_KEYS_HISTORY, JSON.stringify(history));

  // Keep authenticated cabinet session aware of the newly linked license.
  try {
    const currentSession = getActiveUserSession();
    if (currentSession) {
      const updatedSession = {
        ...currentSession,
        activeKey: newKey,
        keys: history
      };
      localStorage.setItem(STORAGE_AUTH_USER, JSON.stringify(updatedSession));
    }
  } catch (e) {}

  renderKeysHistory();
  updateAuthUI();

  showToast(`Лицензия активирована! Ключ: ${newKey}`);
}

// Ручная привязка ключа бойцом в Личном кабинете (для синхронизации с приложением на Android)
function linkLicenseKeyInCabinet() {
  const input = document.getElementById('cabManualKeyInput');
  const key = input.value.trim().toUpperCase().replace(/\s+/g, '');

  if (!key) {
    showToast('Введите лицензионный ключ');
    return;
  }

  if (!verifyKeyChecksum(key)) {
    showToast('❌ Ошибка: Ключ недействителен или подделан!');
    return;
  }

  const callsign = localStorage.getItem(STORAGE_USER_CALLSIGN) || 'Боец';
  applyNewPaidKey(key, callsign);
  input.value = '';
  showToast(`✓ Ключ ${key} успешно привязан к вашему личному кабинету!`);
}

// 9. Modals Controller
window.openModal = function(id) {
  const modal = document.getElementById(id);
  if (modal) modal.classList.add('open');
};

window.closeModal = function(id) {
  const modal = document.getElementById(id);
  if (modal) modal.classList.remove('open');
};

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
  // Check user session state and setup auth UI
  updateAuthUI();

  // Load profile data
  loadCabinetProfile();

  // Mobile Toggle
  const mobileToggle = document.getElementById('mobileToggle');
  const mainTabNav = document.getElementById('mainTabNav');
  if (mobileToggle && mainTabNav) {
    mobileToggle.addEventListener('click', () => {
      mainTabNav.classList.toggle('open');
    });
  }

  // Close modals when clicking outside
  document.querySelectorAll('.modal-overlay').forEach(overlay => {
    overlay.addEventListener('click', (e) => {
      if (e.target === overlay) {
        overlay.classList.remove('open');
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

  // Check URL parameters (e.g. returning after payment redirect ?payment=check)
  const urlParams = new URLSearchParams(window.location.search);
  if (urlParams.get('payment') === 'success' || urlParams.get('payment') === 'check') {
    switchMainTab('tabPayment');
    const boxInitial = document.getElementById('boxPaymentInitial');
    const boxPending = document.getElementById('boxPaymentPending');
    if (boxInitial) boxInitial.style.display = 'none';
    if (boxPending) boxPending.style.display = 'block';
    
    // Автоматически нажимаем кнопку выдачи ключа, так как пользователь вернулся после оплаты
    setTimeout(() => {
      claimPaidLicenseKey();
    }, 1000);
  }

  // Tactical Screenshots Carousel Controller
  initScreenshotsCarousel();
});

// Restore tab from URL hash on refresh/back-forward navigation.
document.addEventListener('DOMContentLoaded', () => {
  const allowedTabs = new Set(['tabOverview','tabCabinet','tabPayment','tabSync','tabDownload']);
  const fromHash = (window.location.hash || '').replace('#','');
  if (allowedTabs.has(fromHash)) {
    switchMainTab(fromHash);
  }
});

window.addEventListener('hashchange', () => {
  const tabId = (window.location.hash || '').replace('#','');
  if (['tabOverview','tabCabinet','tabPayment','tabSync','tabDownload'].includes(tabId)) {
    const target = document.getElementById(tabId);
    if (target && !target.classList.contains('active')) switchMainTab(tabId);
  }
});

// =========================================================================
// SCREENSHOT CAROUSEL & LIGHTBOX CONTROLLER
// =========================================================================
let currentCarouselIndex = 0;
const totalCarouselSlides = 7;

function initScreenshotsCarousel() {
  const track = document.getElementById('carouselTrack');
  const btnPrev = document.getElementById('carouselPrev');
  const btnNext = document.getElementById('carouselNext');
  const dots = document.querySelectorAll('.carousel-dot');

  if (!track || !btnPrev || !btnNext) return;

  function updateCarousel() {
    const slide = track.children[0];
    if (!slide) return;
    const slideWidth = slide.offsetWidth + 24; // 24px gap
    track.style.transform = `translateX(-${currentCarouselIndex * slideWidth}px)`;

    dots.forEach((dot, idx) => {
      dot.classList.toggle('active', idx === currentCarouselIndex);
    });

    Array.from(track.children).forEach((card, idx) => {
      card.classList.toggle('active-card', idx === currentCarouselIndex);
    });
  }

  btnPrev.addEventListener('click', () => {
    currentCarouselIndex = (currentCarouselIndex - 1 + totalCarouselSlides) % totalCarouselSlides;
    updateCarousel();
  });

  btnNext.addEventListener('click', () => {
    currentCarouselIndex = (currentCarouselIndex + 1) % totalCarouselSlides;
    updateCarousel();
  });

  dots.forEach((dot) => {
    dot.addEventListener('click', () => {
      const idx = parseInt(dot.getAttribute('data-index'), 10);
      if (!isNaN(idx)) {
        currentCarouselIndex = idx;
        updateCarousel();
      }
    });
  });

  // Touch / Swipe support
  let touchStartX = 0;
  let touchEndX = 0;
  track.addEventListener('touchstart', (e) => {
    touchStartX = e.changedTouches[0].screenX;
  }, { passive: true });

  track.addEventListener('touchend', (e) => {
    touchEndX = e.changedTouches[0].screenX;
    if (touchStartX - touchEndX > 50) {
      // Swipe left -> next
      currentCarouselIndex = (currentCarouselIndex + 1) % totalCarouselSlides;
      updateCarousel();
    } else if (touchEndX - touchStartX > 50) {
      // Swipe right -> prev
      currentCarouselIndex = (currentCarouselIndex - 1 + totalCarouselSlides) % totalCarouselSlides;
      updateCarousel();
    }
  }, { passive: true });

  // Handle window resize
  window.addEventListener('resize', updateCarousel);
}

// Lightbox modal opener
function openLightbox(imgSrc, title) {
  const modal = document.getElementById('modalScreenshot');
  const img = document.getElementById('modalScreenshotImg');
  const titleEl = document.getElementById('modalScreenshotTitle');
  const dl = document.getElementById('modalScreenshotDownload');

  if (!modal || !img) return;

  img.src = imgSrc;
  if (titleEl) titleEl.textContent = title || 'Скриншот приложения';
  if (dl) {
    dl.href = imgSrc;
    dl.setAttribute('download', imgSrc);
  }
  openModal('modalScreenshot');
}

// Track APK Downloads & RuStore clicks
function trackApkDownload(source) {
  try {
    trackYm('reachGoal', 'apk_download', { source: source });
    if (typeof gtag === 'function') {
      gtag('event', 'download_apk', { 'event_category': 'APK', 'event_label': source });
    }
  } catch (e) {
    console.warn('Analytics tracking error:', e);
  }
}

// Tactical FAQ Accordion
function toggleFaq(btn) {
  const item = btn.closest('.faq-item');
  if (!item) return;
  const isActive = item.classList.contains('active');
  
  // Close other open FAQ items for clean accordion effect
  document.querySelectorAll('.faq-item').forEach(el => {
    if (el !== item) el.classList.remove('active');
  });

  if (isActive) {
    item.classList.remove('active');
  } else {
    item.classList.add('active');
  }
}

// =========================================================================
// INTERACTIVE MINI-DEMO CONTROLLER
// =========================================================================
let demoItems = [
  { id: 1, name: 'Рации Baofeng UV-5R', qty: 12, unit: 'шт' },
  { id: 2, name: 'Сухпайки (ИРП-5)', qty: 45, unit: 'компл.' },
  { id: 3, name: 'Аккумуляторы 18650', qty: 80, unit: 'шт' }
];

function renderDemoItems() {
  const container = document.getElementById('demoItemsList');
  const countBadge = document.getElementById('demoTotalItemsCount');
  if (!container) return;

  if (countBadge) {
    countBadge.textContent = `${demoItems.length} позиции`;
  }

  container.innerHTML = demoItems.map(item => `
    <div style="display:flex; justify-content:space-between; align-items:center; background:#121b14; border:1px solid rgba(141,170,89,0.25); border-radius:10px; padding:10px 14px; gap:8px; flex-wrap:wrap;">
      <div style="flex:1; min-width:160px;">
        <div style="color:var(--text-primary); font-weight:600; font-size:0.92rem;">${escapeHtml(item.name)}</div>
        <div style="color:#00e676; font-size:0.85rem; font-family:var(--font-mono); font-weight:700;">
          Остаток: ${item.qty} ${item.unit}
        </div>
      </div>
      <div style="display:flex; gap:6px; align-items:center;">
        <button class="btn btn-sm" onclick="demoChangeQty(${item.id}, 1)" style="background:#1b2d1f; color:#8daa59; border:1px solid rgba(141,170,89,0.4); padding:6px 12px; font-size:0.82rem; font-weight:700;" title="Записать приход">+ Приход</button>
        <button class="btn btn-sm" onclick="demoChangeQty(${item.id}, -1)" style="background:#261717; color:#ff8a80; border:1px solid rgba(255,100,100,0.3); padding:6px 12px; font-size:0.82rem; font-weight:700;" title="Записать выдачу">- Выдать</button>
      </div>
    </div>
  `).join('');
}

function demoChangeQty(id, delta) {
  const item = demoItems.find(i => i.id === id);
  if (!item) return;
  if (delta < 0 && item.qty <= 0) {
    setDemoLog(`⚠️ «${item.name}»: остаток уже 0, выдавать нечего!`);
    return;
  }
  item.qty = Math.max(0, item.qty + delta);
  renderDemoItems();
  const act = delta > 0 ? 'Приход (+1)' : 'Выдача (-1)';
  setDemoLog(`✓ Записано в журнал: ${act} «${item.name}». Новый остаток: ${item.qty} ${item.unit}`);
}

function demoAddItem() {
  const nameInput = document.getElementById('demoNewItemName');
  const qtyInput = document.getElementById('demoNewItemQty');
  if (!nameInput || !qtyInput) return;

  const name = nameInput.value.trim();
  const qty = parseInt(qtyInput.value, 10);
  if (!name) {
    nameInput.focus();
    return;
  }
  const safeQty = isNaN(qty) || qty < 0 ? 1 : qty;

  const newItem = {
    id: Date.now(),
    name: name,
    qty: safeQty,
    unit: 'шт'
  };
  demoItems.unshift(newItem);
  nameInput.value = '';
  qtyInput.value = '10';
  renderDemoItems();
  setDemoLog(`✓ Новая вещь «${name}» добавлена на склад! Остаток: ${safeQty} шт.`);
}

function setDemoLog(msg) {
  const log = document.getElementById('demoLastLogText');
  if (log) {
    log.innerHTML = `<span style="color:#ffd54f;">${msg}</span>`;
  }
}

function demoExportExcel() {
  let csv = 'Название;Остаток;Ед. изм.\r\n';
  demoItems.forEach(i => {
    csv += `"${i.name.replace(/"/g, '""')}";"${i.qty}";"${i.unit}"\r\n`;
  });
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'kapterka_demo_report.csv';
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
  setDemoLog('📊 Демо-отчёт выгружен в CSV/Excel! В приложении отчёт формируется за 1 секунду.');
}

// Initial render
document.addEventListener('DOMContentLoaded', () => {
  renderDemoItems();
});


