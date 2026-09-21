const KAPTERKA_WEB_BUILD = window.KAPTERKA_WEB_VERSION || 'unknown';
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
const STORAGE_LICENSE_META = 'kapterka_license_meta_v2';

// Firebase Web API key is an application identifier, not a server secret.
// Reads below request only status/timestamps for one exact license document.
const FIREBASE_LICENSE_API_KEY = 'AIzaSyAYyoG42TuQJFLxN0KnFIePZx-gAtizw0Q';
const FIRESTORE_LICENSE_DOC_BASE = 'https://firestore.googleapis.com/v1/projects/kapterka-pro/databases/(default)/documents/licenses/';

// Default initial state for clean empty inputs
const YM_IDS = [112482290];
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
  const legacyPattern = /^(?:KAPT|KPT)-[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]{4}-[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]{4}-[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]{4}$/;
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

function firestoreFieldValue(field) {
  if (!field || typeof field !== 'object') return null;
  if (field.integerValue != null) return Number(field.integerValue);
  if (field.doubleValue != null) return Number(field.doubleValue);
  if (field.timestampValue) {
    const ms = Date.parse(field.timestampValue);
    return Number.isFinite(ms) ? ms : null;
  }
  if (field.stringValue != null) return String(field.stringValue);
  if (field.booleanValue != null) return !!field.booleanValue;
  return null;
}

async function fetchLicenseRegistryMeta(key) {
  const cleanKey = String(key || '').trim().toUpperCase();
  if (classifyLicenseKey(cleanKey) !== 'signed') {
    return { found: false, verified: false, reason: 'UNSUPPORTED_KEY_FORMAT' };
  }

  const mask = [
    'mask.fieldPaths=licenseKey',
    'mask.fieldPaths=activatedAt',
    'mask.fieldPaths=expiresAt',
    'mask.fieldPaths=status'
  ].join('&');
  const url = FIRESTORE_LICENSE_DOC_BASE + encodeURIComponent(cleanKey) + '?' + mask + '&key=' + encodeURIComponent(FIREBASE_LICENSE_API_KEY);
  const controller = typeof AbortController !== 'undefined' ? new AbortController() : null;
  const timer = controller ? setTimeout(() => controller.abort(), 8000) : null;

  try {
    const response = await fetch(url, {
      method: 'GET',
      cache: 'no-store',
      headers: { 'Accept': 'application/json' },
      signal: controller?.signal
    });

    if (response.status === 404) return { found: false, verified: false, reason: 'NOT_FOUND' };
    if (!response.ok) return { found: false, verified: false, unavailable: true, reason: 'REGISTRY_UNAVAILABLE' };

    const data = await response.json();
    const fields = data?.fields || {};
    const registeredKey = String(firestoreFieldValue(fields.licenseKey) || cleanKey).toUpperCase();
    if (registeredKey !== cleanKey) return { found: false, verified: false, reason: 'KEY_MISMATCH' };

    const activatedAt = Number(firestoreFieldValue(fields.activatedAt) || 0);
    const expiresAt = Number(firestoreFieldValue(fields.expiresAt) || 0);
    const status = String(firestoreFieldValue(fields.status) || 'ACTIVE').toUpperCase();
    const verified = status === 'ACTIVE' && expiresAt > 0;

    return {
      found: true,
      verified,
      registryVerified: verified,
      status,
      activatedAt,
      expiresAt
    };
  } catch (_) {
    return { found: false, verified: false, unavailable: true, reason: 'NETWORK_ERROR' };
  } finally {
    if (timer) clearTimeout(timer);
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
  const activeKey = localStorage.getItem(STORAGE_ACTIVE_KEY) || '';
  if (!activeKey || classifyLicenseKey(activeKey) !== 'signed') {
    updateCabinetLicenseStatus();
    renderKeysHistory();
    return;
  }

  const result = await fetchLicenseRegistryMeta(activeKey);
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
    showToast('Ключ лицензии скопирован в буфер обмена.');
  } catch (err) {
    prompt('Скопируйте ключ вручную:', text);
  }
  document.body.removeChild(textArea);
}

const YOOKASSA_PAYMENT_URL = 'https://yookassa.ru/my/i/apiQMG65ZHIE/l';
let paymentRequestInFlight = false;

// 7. YooKassa Real Payment & Automated Verification Flow
async function processYooKassaPayment() {
  if (paymentRequestInFlight) {
    showToast('Платёж уже создаётся. Подождите несколько секунд.');
    return;
  }

  const callsign = document.getElementById('payCallsignInput')?.value.trim() || 'Боец';
  const email = document.getElementById('payEmailInput')?.value.trim().toLowerCase() || '';

  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) || email.length > 160) {
    showToast('⚠️ Проверьте Email для получения чека и ключа.');
    document.getElementById('payEmailInput')?.focus();
    return;
  }

  const payButton = document.getElementById('btnPayYooKassaMain');
  paymentRequestInFlight = true;
  if (payButton) {
    payButton.disabled = true;
    payButton.dataset.originalText = payButton.textContent || 'Оплатить 490 ₽ через ЮKassa';
    payButton.textContent = 'Создание платежа…';
  }

  // Открываем вкладку в момент нажатия пользователя, чтобы браузер не заблокировал ЮKassa как pop-up.
  let paymentWindow = null;
  try {
    paymentWindow = window.open('about:blank', '_blank');
    if (paymentWindow) paymentWindow.opener = null;
  } catch (_) {}

  const finishPaymentRequest = (url) => {
    paymentRequestInFlight = false;
    if (payButton) {
      payButton.disabled = false;
      payButton.textContent = payButton.dataset.originalText || 'Оплатить 490 ₽ через ЮKassa';
      delete payButton.dataset.originalText;
    }
    if (!url) return;
    try {
      if (paymentWindow && !paymentWindow.closed) {
        paymentWindow.location.replace(url);
      } else {
        window.location.href = url;
      }
    } catch (_) {
      window.location.href = url;
    }
  };

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
    
    const cleanupJsonp = () => {
      document.getElementById(callbackName)?.remove();
      try { delete window[callbackName]; } catch (_) { window[callbackName] = undefined; }
    };

    const requestTimeout = setTimeout(() => {
      cleanupJsonp();
      showToast('Сервер оплаты отвечает слишком долго. Открываю резервную страницу ЮKassa.');
      finishPaymentRequest(YOOKASSA_PAYMENT_URL);
    }, 15000);

    // Создаем функцию глобально
    window[callbackName] = function(data) {
      clearTimeout(requestTimeout);
      cleanupJsonp();

      if (data && data.confirmation_url) {
        if (data.payment_id) localStorage.setItem('kapterka_pending_payment_id', data.payment_id);
        finishPaymentRequest(data.confirmation_url);
      } else {
        showToast('Не удалось создать индивидуальный платёж. Открываю резервную страницу ЮKassa.');
        console.warn('Payment API response without confirmation URL');
        finishPaymentRequest(YOOKASSA_PAYMENT_URL);
      }
    };

    // Формируем URL с параметрами GET (JSONP)
    const scriptUrl = `${API_URL}?action=pay&email=${encodeURIComponent(email)}&callsign=${encodeURIComponent(callsign)}&callback=${callbackName}`;

    // Создаем тег script и добавляем на страницу
    const script = document.createElement('script');
    script.id = callbackName;
    script.src = scriptUrl;

    script.onerror = function() {
      clearTimeout(requestTimeout);
      cleanupJsonp();
      showToast('Сбой соединения. Открываю резервную страницу ЮKassa.');
      finishPaymentRequest(YOOKASSA_PAYMENT_URL);
    };

    document.body.appendChild(script);

  } else {
    // Резервный режим
    finishPaymentRequest(YOOKASSA_PAYMENT_URL);
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
async function verifyWithManualOrderId() {
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
  if (keyKind === 'legacy_unverified') {
    showToast('ℹ️ Это старый формат ключа. Для безопасности сайт не активирует его без подтверждения.');
    return;
  }

  showToast('Проверяю ключ в реестре лицензий…');
  const registry = await fetchLicenseRegistryMeta(enteredKey);
  if (!registry.found || !registry.registryVerified || Number(registry.expiresAt || 0) <= Date.now()) {
    showToast(registry.unavailable
      ? 'Не удалось проверить срок лицензии. Попробуйте ещё раз при стабильном интернете.'
      : 'Ключ не найден среди активных лицензий или срок уже завершён.');
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
  if (keyKind === 'legacy_unverified') {
    showToast('ℹ️ Старый ключ распознан, но для безопасности не активирован. Нужна серверная проверка.');
    return;
  }

  const callsign = localStorage.getItem(STORAGE_USER_CALLSIGN) || 'Боец';
  showToast('Проверяю лицензию в облачном реестре…');
  const registry = await fetchLicenseRegistryMeta(key);

  if (!registry.found || !registry.registryVerified || Number(registry.expiresAt || 0) <= Date.now()) {
    showToast(registry.unavailable
      ? 'Не удалось проверить реестр. Ключ не активирован — попробуйте ещё раз при стабильном интернете.'
      : 'Ключ не найден среди активных лицензий или срок уже завершён.');
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
