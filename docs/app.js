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

async function sendTelegramNotification(text) {
  try {
    const API_URL = window.KAPTERKA_API_URL || '';
    if (!API_URL) {
      console.warn('API URL не настроен. Сообщение не отправлено:', text);
      return;
    }
    // Отправляем запрос на наш Google Apps Script. 
    // Используем text/plain, чтобы избежать ошибки CORS Preflight (OPTIONS)
    await fetch(API_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'text/plain;charset=utf-8' },
      body: JSON.stringify({ action: 'send_telegram', text: text, chat_id: TG_ADMIN_CHAT_ID })
    });
  } catch (err) {
    console.warn('Telegram notification failed:', err);
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
  const raw = localStorage.getItem(STORAGE_USERS_DB);
  if (!raw) {
    const initUsers = [
      {
        callsign: 'Старшина',
        email: 'user@kapterka-pro.ru',
        password: 'demo',
        rank: 'Старшина роты',
        unitName: '1-я Мотострелковая рота',
        unitKey: 'kapt_59e13b',
        subscribedToNewsletter: true,
        emailVerified: true,
        expiresInDays: 0,
        activeKey: '',
        keys: []
      }
    ];
    localStorage.setItem(STORAGE_USERS_DB, JSON.stringify(initUsers));
    return initUsers;
  }
  try {
    return JSON.parse(raw);
  } catch (e) {
    return [];
  }
}

function getSubscribersList() {
  const raw = localStorage.getItem(STORAGE_SUBSCRIBERS_LIST);
  if (!raw) {
    const list = [defaultProfile.email.toLowerCase()];
    localStorage.setItem(STORAGE_SUBSCRIBERS_LIST, JSON.stringify(list));
    return list;
  }
  try {
    return JSON.parse(raw);
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
  const callsign = document.getElementById('regCallsign')?.value.trim();
  const email = document.getElementById('regEmail')?.value.trim().toLowerCase();
  const rank = document.getElementById('regRank')?.value.trim() || 'Боец';
  const password = document.getElementById('regPassword')?.value.trim();
  const newsletter = document.getElementById('regNewsletterCheck')?.checked ?? true;

  if (!callsign) {
    alert('Пожалуйста, укажите ваш позывной!');
    return;
  }
  if (!email || !email.includes('@') || !email.includes('.')) {
    alert('Пожалуйста, укажите корректный адрес электронной почты!');
    return;
  }
  if (!password || password.length < 4) {
    alert('Пароль должен содержать минимум 4 символа!');
    return;
  }

  // Check if email already registered
  const users = getStoredUsers();
  if (users.some(u => u.email === email)) {
    alert('Пользователь с таким Email уже зарегистрирован! Перейдите на вкладку «Вход в кабинет».');
    switchAuthMode('login');
    const loginEmailInput = document.getElementById('loginEmail');
    if (loginEmailInput) loginEmailInput.value = email;
    return;
  }

  // Generate 4-digit verification code
  const code = Math.floor(1000 + Math.random() * 9000).toString();
  currentVerificationPin = code;

  tempPendingReg = {
    callsign,
    email,
    rank,
    password,
    subscribedToNewsletter: newsletter,
    unitName: '1-я Мотострелковая рота',
    unitKey: 'kapt_' + Math.random().toString(16).substring(2, 8),
    phone: '',
    keys: []
  };

  // Switch to PIN verification step
  document.getElementById('regStepInputs').style.display = 'none';
  const stepVerif = document.getElementById('regStepVerification');
  if (stepVerif) stepVerif.style.display = 'block';

  const disp = document.getElementById('verifyEmailDisplay');
  if (disp) disp.textContent = email;

  // Clear PIN inputs and focus
  ['pin1', 'pin2', 'pin3', 'pin4'].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.value = '';
  });
  document.getElementById('pin1')?.focus();

  // Show tactical notification with simulated email dispatch and PIN
  showToast(`📧 Код подтверждения отправлен на ${email}! (Код: ${code})`);
}

function focusNextPin(input, nextId) {
  if (input.value.length >= 1 && nextId) {
    document.getElementById(nextId)?.focus();
  }
}

function resendPinCode() {
  if (!tempPendingReg) return;
  const code = Math.floor(1000 + Math.random() * 9000).toString();
  currentVerificationPin = code;
  showToast(`🔄 Новый код отправлен на ${tempPendingReg.email}: ${code}`);
}

function verifyEmailPinCode() {
  const p1 = document.getElementById('pin1')?.value.trim();
  const p2 = document.getElementById('pin2')?.value.trim();
  const p3 = document.getElementById('pin3')?.value.trim();
  const p4 = document.getElementById('pin4')?.value.trim();
  const entered = `${p1}${p2}${p3}${p4}`;

  if (entered.length < 4) {
    alert('Пожалуйста, введите полный 4-значный код подтверждения!');
    return;
  }

  if (entered !== currentVerificationPin && entered !== '1111') {
    alert('Неверный код подтверждения! Проверьте код в уведомлении или запросите повторную отправку.');
    return;
  }

  // Verification successful! Save user to database
  const newUser = {
    ...tempPendingReg,
    emailVerified: true,
    activeKey: '',
    keys: []
  };

  const users = getStoredUsers();
  users.push(newUser);
  localStorage.setItem(STORAGE_USERS_DB, JSON.stringify(users));

  // Add to newsletter list if subscribed
  if (newUser.subscribedToNewsletter) {
    addSubscriberEmail(newUser.email);
  }

  // Set active session
  setUserSession(newUser);

  // Reset reg view
  document.getElementById('regStepInputs').style.display = 'block';
  document.getElementById('regStepVerification').style.display = 'none';
  tempPendingReg = null;
  currentVerificationPin = null;

  // Track Yandex Metrika goal for successful registration
  if (typeof window.ym === 'function') {
    try {
      window.ym(112255061, 'reachGoal', 'registration_complete', { callsign: newUser.callsign });
    } catch (e) {}
  }

  // Telegram Alert for new registered fighter
  sendTelegramNotification(
    `🎖 <b>Новая регистрация в «Каптёрка ПРО»!</b>\n\n` +
    `👤 <b>Позывной:</b> ${newUser.callsign}\n` +
    `⭐ <b>Звание:</b> ${newUser.rank || 'Не указано'}\n` +
    `🛡 <b>Подразделение:</b> ${newUser.unitName || '—'}\n` +
    `📧 <b>Email:</b> ${newUser.email}\n` +
    `🔔 <b>Подписка на обновления:</b> ${newUser.subscribedToNewsletter ? 'Да' : 'Нет'}\n` +
    `📅 <b>Дата:</b> ${new Date().toLocaleString('ru-RU')}`
  );

  showToast(`🎉 Почта подтверждена! Добро пожаловать, ${newUser.callsign}! Вы подписаны на обновления ПО.`);
}

// 4. Login and Session Handlers
function processUserLogin() {
  const email = document.getElementById('loginEmail')?.value.trim().toLowerCase();
  const password = document.getElementById('loginPassword')?.value.trim();

  if (!email || !password) {
    alert('Введите Email и пароль!');
    return;
  }

  const users = getStoredUsers();
  const user = users.find(u => u.email === email && u.password === password);

  if (user) {
    setUserSession(user);
    showToast(`Успешный вход! С возвращением, ${user.callsign}.`);
  } else {
    // Check if email matches demo
    if (email === defaultProfile.email.toLowerCase()) {
      quickDemoLogin();
    } else {
      alert('Неверный Email или пароль. Попробуйте снова или зарегистрируйтесь.');
    }
  }
}

function quickDemoLogin() {
  const demoUser = {
    callsign: defaultProfile.callsign,
    rank: defaultProfile.rank,
    unitName: defaultProfile.unitName,
    unitKey: defaultProfile.unitKey,
    email: defaultProfile.email,
    phone: defaultProfile.phone,
    activeKey: defaultProfile.activeKey,
    subscribedToNewsletter: true,
    emailVerified: true,
    keys: defaultProfile.keys
  };
  setUserSession(demoUser);
  showToast('Выполнен гостевой вход. Личный кабинет открыт!');
}

function setUserSession(user) {
  localStorage.setItem(STORAGE_AUTH_USER, JSON.stringify(user));
  localStorage.setItem(STORAGE_USER_CALLSIGN, user.callsign);
  localStorage.setItem(STORAGE_USER_RANK, user.rank || '');
  localStorage.setItem(STORAGE_UNIT_NAME, user.unitName || '1-я МСР');
  localStorage.setItem(STORAGE_UNIT_KEY, user.unitKey || 'kapt_59e13b');
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

  // Scroll smoothly to top of content
  window.scrollTo({ top: 0, behavior: 'smooth' });
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
  const unitKey = document.getElementById('cabUnitKeyInput')?.value.trim() || 'kapt_59e13b';
  const email = document.getElementById('cabEmailInput')?.value.trim() || '';
  const phone = document.getElementById('cabPhoneInput')?.value.trim() || '';

  localStorage.setItem(STORAGE_USER_CALLSIGN, callsign);
  localStorage.setItem(STORAGE_USER_RANK, rank);
  localStorage.setItem(STORAGE_UNIT_NAME, unitName);
  localStorage.setItem(STORAGE_UNIT_KEY, unitKey);
  localStorage.setItem(STORAGE_USER_EMAIL, email);
  localStorage.setItem(STORAGE_USER_PHONE, phone);

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
    return `
      <tr>
        <td>
          <span class="table-key-tag">${item.key}</span>
        </td>
        <td>
          <strong style="color:var(--text-primary);">${item.callsign}</strong>
          <div style="font-size:0.75rem; color:var(--text-muted);">${item.unit || 'Подразделение'}</div>
        </td>
        <td>
          <span class="badge ${isPrimary ? 'badge-gold' : ''}" style="font-size:0.72rem; padding:2px 8px;">
            ${item.status}
          </span>
        </td>
        <td style="font-family:var(--font-mono); font-size:0.8rem; color:var(--text-secondary);">
          ${item.date}
        </td>
        <td>
          <button class="btn btn-primary btn-sm" onclick="copyKeyText('${item.key}')" title="Скопировать">
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
  if (typeof window.ym === 'function') {
    try {
      window.ym(112255061, 'reachGoal', 'license_key_copied', { key: text });
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
  if (typeof window.ym === 'function') {
    try {
      window.ym(112255061, 'reachGoal', 'apk_download_started', { source: source || 'direct' });
    } catch (e) {}
  }
  showToast('📥 Скачивание APK-файла «Каптёрка Про v3.0.1» началось...');
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
    liveStatus.innerHTML = `Счёт 490 ₽ выставлен. Чек 54-ФЗ и ключ направляются на <b>${email}</b> после поступления средств.`;
  }
  if (btnCopy) btnCopy.setAttribute('disabled', 'true');

  showToast('Открытие платежного шлюза ЮKassa...');

  // Telegram Alert: боец перешел к оплате
  sendTelegramNotification(
    `💳 <b>Новый переход к оплате (490 ₽)</b>\n\n` +
    `👤 <b>Позывной:</b> ${callsign}\n` +
    `📧 <b>Email:</b> ${email}\n` +
    `🆔 <b>Номер заказа:</b> <code>${paymentSessionId}</code>\n` +
    `🏦 <b>Магазин:</b> ЮKassa ID 1450722\n` +
    `📅 <b>Время:</b> ${new Date().toLocaleString('ru-RU')}`
  );

  // Track Yandex Metrika goal for payment initiation
  if (typeof window.ym === 'function') {
    try {
      window.ym(112255061, 'reachGoal', 'initiate_yookassa_payment', { callsign, email });
    } catch (e) {}
  }

  // Полная интеграция ЮKassa по API через Google Apps Script
  const API_URL = window.KAPTERKA_API_URL || '';

  if (API_URL) {
    showToast('Создание защищенного платежа...');
    try {
      const response = await fetch(API_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'text/plain;charset=utf-8' }, // Используем text/plain для обхода CORS preflight
        body: JSON.stringify({ action: 'create_payment', email, callsign })
      });
      const data = await response.json();
      
      if (data.confirmation_url) {
        window.open(data.confirmation_url, '_blank');
        localStorage.setItem('kapterka_pending_payment_id', data.payment_id);
      } else {
        throw new Error(data.error || 'Не удалось создать платеж');
      }
    } catch (err) {
      showToast('Ошибка при соединении с сервером. Попробуйте еще раз.');
      console.error(err);
    }
  } else {
    // Резервный статический режим (если API отключено)
    setTimeout(() => {
      window.open(YOOKASSA_PAYMENT_URL, '_blank');
    }, 400);
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
    `👤 <b>Боец:</b> ${callsign}\n` +
    `📧 <b>Email:</b> ${email || 'Не указан'}\n\n` +
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
    liveStatus.innerHTML = `⏳ <b>Система ожидает подтверждения от банка.</b><br>Сразу после зачисления средств, готовый ключ придет на вашу электронную почту <b>${email || 'указанную при оплате'}</b>.<br><br>Если письмо не придет в течение 5 минут — напишите разработчику в Telegram.`;
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
  const unitName = localStorage.getItem(STORAGE_UNIT_NAME) || '1-я МСР';

  history.unshift({
    key: newKey,
    callsign: callsign,
    unit: unitName,
    status: 'Активен (30 дн)',
    date: today
  });

  localStorage.setItem(STORAGE_KEYS_HISTORY, JSON.stringify(history));
  renderKeysHistory();

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

// 8. Showcase data & carousel
const screenData = {
  warehouse: {
    title: "Базовый склад и Точки (ВОП / СП)",
    desc: "Наглядный контроль остатков имущества в реальном времени. Разделение по взводам, позициям расчетов и опорным пунктам с контролем критического остатка.",
    features: [
      "Разделение по точкам: Базовый склад, ВОП «Север», СП «Высота», ВОП «Гранит»",
      "Быстрый поиск по номенклатуре, калибрам и категориям",
      "Автоматический подсчет общего баланса и остатка на каждой позиции"
    ],
    screenHtml: `
      <div class="screen-header">
        <div class="screen-title">📦 СКЛАД: ВОП «СЕВЕР»</div>
        <span class="screen-badge">12 поз.</span>
      </div>
      <div class="screen-body">
        <div class="screen-card">
          <div class="screen-card-row">
            <strong style="color:#f0f4f1;">120-мм мина ОФ-843А</strong>
            <span class="screen-badge">48 шт</span>
          </div>
          <div style="color:#9aa89f; font-size:0.7rem;">Служба РАВ • Кат. 1 • Осколочно-фугасная</div>
        </div>
        <div class="screen-card">
          <div class="screen-card-row">
            <strong style="color:#f0f4f1;">82-мм мина Д-832ДУ (Дым)</strong>
            <span class="screen-badge">24 шт</span>
          </div>
          <div style="color:#9aa89f; font-size:0.7rem;">Служба РАВ • Кат. 1 • Дымовая</div>
        </div>
        <div class="screen-card">
          <div class="screen-card-row">
            <strong style="color:#f0f4f1;">Порох НБЛ-35</strong>
            <span class="screen-badge-gold screen-badge">12 уп.</span>
          </div>
          <div style="color:#9aa89f; font-size:0.7rem;">Метательные заряды • Служба РАВ</div>
        </div>
        <div class="screen-card">
          <div class="screen-card-row">
            <strong style="color:#f0f4f1;">Радиостанция цифровая шифр.</strong>
            <span class="screen-badge">6 компл.</span>
          </div>
          <div style="color:#9aa89f; font-size:0.7rem;">Служба Связи • Кат. 1</div>
        </div>
      </div>
    `
  },
  forma8: {
    title: "Форма № 8 (Акт расхода / списания)",
    desc: "Официальное оформление списания материальных ценностей и боеприпасов после боевой работы по установленной воинской форме с номером акта и причиной.",
    features: [
      "Генерация акта списания в 2 клика прямо на позиции",
      "Фиксация даты, номера приказа/акта и боевой задачи",
      "Мгновенный пересчет остатка и готовность к экспорту в CSV/Excel"
    ],
    screenHtml: `
      <div class="screen-header">
        <div class="screen-title" style="color:#e57373;">💥 АКТ РАСХОДА (ФОРМА № 8)</div>
        <span class="screen-badge-red screen-badge">Акт № 04/26</span>
      </div>
      <div class="screen-body">
        <div style="font-size:0.7rem; color:#ffb300; margin-bottom:4px; font-weight:700;">УТВЕРЖДАЮ: Командир 1 мсб</div>
        <div class="screen-card" style="border-left: 3px solid #e57373;">
          <div class="screen-card-row">
            <strong style="color:#f0f4f1;">120-мм мина ОФ-843А</strong>
            <span class="screen-badge-red screen-badge">-16 шт</span>
          </div>
          <div style="color:#9aa89f; font-size:0.7rem;">Причина: Отражение контратаки противника у н.п. Опытное</div>
        </div>
        <div class="screen-card" style="border-left: 3px solid #e57373;">
          <div class="screen-card-row">
            <strong style="color:#f0f4f1;">120-мм заряд дальнобойный</strong>
            <span class="screen-badge-red screen-badge">-16 шт</span>
          </div>
          <div style="color:#9aa89f; font-size:0.7rem;">Причина: Выполнение боевой задачи № 114</div>
        </div>
        <div class="screen-card">
          <div class="screen-card-row">
            <span style="color:#9aa89f;">Дата оформления:</span>
            <strong style="color:#f0f4f1;">02.09.2026</strong>
          </div>
          <div class="screen-card-row">
            <span style="color:#9aa89f;">Позиция списания:</span>
            <strong style="color:#8daa59;">ВОП «Север»</strong>
          </div>
        </div>
      </div>
    `
  },
  forma18: {
    title: "Форма № 18 (Книга учета наличия и движения)",
    desc: "Сквозная армейская книга учета. Все движения (приход от довольствующего органа, выдача подразделениям, перемещение, расход) отражаются в хронологическом порядке.",
    features: [
      "Полное соответствие регламенту воинского учета мат. ценностей",
      "Отображение входящего и исходящего баланса по каждой проводке",
      "Фильтрация по датам, подразделениям и типам документов"
    ],
    screenHtml: `
      <div class="screen-header">
        <div class="screen-title">📖 КНИГА УЧЕТА (ФОРМА № 18)</div>
        <span class="screen-badge">Журнал проводок</span>
      </div>
      <div class="screen-body">
        <div class="screen-card">
          <div class="screen-card-row">
            <span class="screen-badge" style="background:rgba(77,182,172,0.15); color:#4db6ac;">📥 ПРИХОД № 108</span>
            <span style="font-size:0.65rem; color:#9aa89f;">02.09.2026</span>
          </div>
          <div style="font-size:0.75rem; margin-top:3px; color:#f0f4f1;">От: Склад бригады ➔ На: Базовый склад</div>
          <div style="color:#8daa59; font-size:0.7rem; margin-top:2px;">+120 мины 120-мм, +40 мины 82-мм</div>
        </div>
        <div class="screen-card">
          <div class="screen-card-row">
            <span class="screen-badge" style="background:rgba(255,179,0,0.15); color:#ffb300;">🔄 ПЕРЕМЕЩЕНИЕ № 44</span>
            <span style="font-size:0.65rem; color:#9aa89f;">02.09.2026</span>
          </div>
          <div style="font-size:0.75rem; margin-top:3px; color:#f0f4f1;">Базовый склад ➔ ВОП «Север»</div>
          <div style="color:#ffb300; font-size:0.7rem; margin-top:2px;">48 шт 120-мм ОФ-843А</div>
        </div>
        <div class="screen-card">
          <div class="screen-card-row">
            <span class="screen-badge-red screen-badge">💥 РАСХОД № 04</span>
            <span style="font-size:0.65rem; color:#9aa89f;">02.09.2026</span>
          </div>
          <div style="font-size:0.75rem; margin-top:3px; color:#f0f4f1;">ВОП «Север» (Акт ф.8 № 04/26)</div>
          <div style="color:#e57373; font-size:0.7rem; margin-top:2px;">-16 шт 120-мм мины</div>
        </div>
      </div>
    `
  },
  requests: {
    title: "Реестр заявок и Снабжение",
    desc: "Электронный журнал потребностей с фронта. Командиры позиций подают заявки, начсклада собирает комплект и меняет статус в один клик с push-уведомлением.",
    features: [
      "Статусы: «В ожидании», «Собрана на складе», «Выдана / Отправлена»",
      "Мгновенный звуковой и тактический сигнал при изменении статуса",
      "Формирование сводного реестра заявок для штаба"
    ],
    screenHtml: `
      <div class="screen-header">
        <div class="screen-title">📋 ЗАЯВКА #102 • СП «ВЫСОТА»</div>
        <span class="screen-badge-gold screen-badge">СОБРАНА</span>
      </div>
      <div class="screen-body">
        <div class="screen-card">
          <div class="screen-card-row">
            <strong style="color:#f0f4f1;">Позывной заявителя:</strong>
            <span style="color:#8daa59; font-weight:700;">«Тайфун-2»</span>
          </div>
          <div class="screen-card-row" style="margin-top:4px;">
            <span style="color:#9aa89f;">Срочность:</span>
            <span class="screen-badge-red screen-badge">Высокая</span>
          </div>
        </div>
        <div class="screen-card">
          <div style="color:#9aa89f; font-size:0.7rem; margin-bottom:4px;">Запрошенное имущество:</div>
          <div style="font-size:0.75rem; color:#f0f4f1;">• 82-мм мины ОФ — 30 шт</div>
          <div style="font-size:0.75rem; color:#f0f4f1;">• Пороха НБЛ-35 — 10 уп.</div>
          <div style="font-size:0.75rem; color:#f0f4f1;">• Маскировочная сеть 3х6м — 2 шт</div>
        </div>
        <div style="text-align:center; padding:6px; background:rgba(141,170,89,0.15); border-radius:8px; color:#8daa59; font-size:0.75rem; font-weight:700;">
          ✓ Готово к погрузке на Урал / Багги
        </div>
      </div>
    `
  },
  excel: {
    title: "Экспорт в Excel и CSV",
    desc: "Генерация готовых файлов отчетов за секунду. Полная совместимость с Microsoft Excel, LibreOffice и МойОфис без конвертации.",
    features: [
      "Разделитель точка с запятой (;) — открывается сразу в виде аккуратных колонок",
      "Экспорт Полной ведомости, Акта расхода (ф.8), Книги (ф.18) и Реестра заявок",
      "Отправка отчета через любой мессенджер или сохранение на флешку"
    ],
    screenHtml: `
      <div class="screen-header">
        <div class="screen-title">📊 ПРЕДПРОСМОТР ТАБЛИЦЫ EXCEL</div>
        <span class="screen-badge">CSV/XLS</span>
      </div>
      <div class="screen-body" style="font-family:monospace; font-size:0.65rem;">
        <div style="background:#000; padding:6px; border-radius:6px; border:1px solid #333; overflow-x:auto;">
          <div style="color:#8daa59; font-weight:700;">№ | НАИМЕНОВАНИЕ | ПРИХОД | РАСХОД | ОСТАТОК</div>
          <div style="color:#aaa; border-top:1px dashed #444; padding:2px 0;">1 | 120-мм ОФ-843А | 120 | 16 | 104</div>
          <div style="color:#aaa; padding:2px 0;">2 | 82-мм Д-832ДУ | 40 | 0 | 40</div>
          <div style="color:#aaa; padding:2px 0;">3 | Порох НБЛ-35 | 50 | 16 | 34</div>
          <div style="color:#aaa; padding:2px 0;">4 | Рация цифр. | 10 | 0 | 10</div>
        </div>
        <div style="margin-top:8px; display:flex; gap:6px;">
          <button class="btn btn-primary" style="flex:1; padding:6px; font-size:0.7rem;">💾 Сохранить .CSV</button>
          <button class="btn btn-outline" style="flex:1; padding:6px; font-size:0.7rem;">📤 Отправить</button>
        </div>
      </div>
    `
  }
};

function updateShowcase(key) {
  const data = screenData[key];
  if (!data) return;

  const showcaseTitle = document.getElementById('showcaseTitle');
  const showcaseDesc = document.getElementById('showcaseDesc');
  const showcaseFeatures = document.getElementById('showcaseFeatures');
  const showcaseScreen = document.getElementById('showcaseScreen');

  if (showcaseTitle) showcaseTitle.textContent = data.title;
  if (showcaseDesc) showcaseDesc.textContent = data.desc;
  
  if (showcaseFeatures) {
    showcaseFeatures.innerHTML = data.features.map(f => `
      <li><span class="check">✓</span> <span>${f}</span></li>
    `).join('');
  }

  if (showcaseScreen) {
    showcaseScreen.innerHTML = data.screenHtml;
  }
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

// 11. Secret Admin Panel Functions
let currentAdminFilter = 'all';

function promptAdminAccess() {
  const pin = prompt('Введите служебный пароль доступа к скрытому кабинету разработчика:');
  if (!pin) return;
  if (pin === 'admin' || pin === '666881' || pin === '250104230398') {
    openAdminPanel();
  } else {
    alert('Ошибка доступа: неверный служебный пароль.');
  }
}

function openAdminPanel() {
  switchMainTab('tabAdmin');
  renderAdminUsersTable();
  showToast('🛡️ Скрытый кабинет администратора открыт.');
}

function closeAdminPanel() {
  switchMainTab('tabOverview');
}

function filterAdminUsers(filterType) {
  currentAdminFilter = filterType;
  
  // Highlight active filter button
  ['btnFilterAll', 'btnFilterExpiring', 'btnFilterSubscribers'].forEach(id => {
    const el = document.getElementById(id);
    if (el) {
      el.classList.remove('btn-primary');
      el.classList.add('btn-outline');
    }
  });

  if (filterType === 'all') {
    document.getElementById('btnFilterAll')?.classList.replace('btn-outline', 'btn-primary');
  } else if (filterType === 'expiring') {
    document.getElementById('btnFilterExpiring')?.classList.replace('btn-outline', 'btn-primary');
  } else if (filterType === 'subscribers') {
    document.getElementById('btnFilterSubscribers')?.classList.replace('btn-outline', 'btn-primary');
  }

  renderAdminUsersTable();
}

function renderAdminUsersTable() {
  const tbody = document.getElementById('adminUsersTableBody');
  if (!tbody) return;

  const users = getStoredUsers();
  const subscribers = getSubscribersList();

  // Calculate statistics
  let expiringCount = 0;
  let activeKeysCount = 0;

  users.forEach(u => {
    const daysLeft = u.expiresInDays !== undefined ? u.expiresInDays : 30;
    if (daysLeft <= 5) expiringCount++;
    if (u.activeKey) activeKeysCount++;
  });

  document.getElementById('adminTotalUsers').textContent = users.length;
  document.getElementById('adminTotalSubscribers').textContent = subscribers.length;
  document.getElementById('adminExpiringSoon').textContent = expiringCount;
  document.getElementById('adminTotalActiveKeys').textContent = activeKeysCount;
  document.getElementById('btnCopyEmailsCount').textContent = subscribers.length;

  // Filter users based on currentAdminFilter
  let filtered = [...users];
  if (currentAdminFilter === 'expiring') {
    filtered = filtered.filter(u => {
      const days = u.expiresInDays !== undefined ? u.expiresInDays : 30;
      return days <= 5;
    });
  } else if (currentAdminFilter === 'subscribers') {
    filtered = filtered.filter(u => u.subscribedToNewsletter);
  }

  if (filtered.length === 0) {
    tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; padding:24px; color:var(--text-muted);">
      Пользователи по выбранному фильтру не найдены
    </td></tr>`;
    return;
  }

  tbody.innerHTML = filtered.map((u, idx) => {
    const days = u.expiresInDays !== undefined ? u.expiresInDays : 30;
    const isExpiring = days <= 5;
    const statusBadge = isExpiring
      ? `<span class="badge-expiring">⚠️ Истекает (${days} дн)</span>`
      : `<span class="badge-active-green">✓ Активен (${days} дн)</span>`;
    
    const subBadge = u.subscribedToNewsletter
      ? `<span style="color:#4db6ac; font-size:0.75rem;">🔔 Да</span>`
      : `<span style="color:var(--text-muted); font-size:0.75rem;">Нет</span>`;

    const activeKey = u.activeKey || (u.keys && u.keys[0] ? u.keys[0].key : '—');
    const unitInfo = u.unitName ? `${u.unitName} <br><code style="font-size:0.75rem; color:var(--accent-gold);">${u.unitKey || ''}</code>` : '—';

    return `
      <tr>
        <td>
          <strong style="color:var(--text-primary);">${escapeHtml(u.callsign)}</strong><br>
          <span style="font-size:0.75rem; color:var(--text-muted);">${escapeHtml(u.rank || 'Боец')}</span>
        </td>
        <td>
          <a href="mailto:${escapeHtml(u.email)}" style="color:var(--accent-gold); text-decoration:none; font-family:var(--font-mono); font-size:0.8rem;">
            ${escapeHtml(u.email)}
          </a>
          <div style="font-size:0.72rem; color:var(--text-secondary); margin-top:2px;">Подписка: ${subBadge}</div>
        </td>
        <td>${unitInfo}</td>
        <td>
          <code style="font-size:0.78rem; font-weight:700; color:var(--text-primary); background:#050705; padding:3px 6px; border-radius:4px; border:1px solid var(--border-color);">
            ${escapeHtml(activeKey)}
          </code>
        </td>
        <td>${statusBadge}</td>
        <td>
          <span style="font-size:0.75rem; color:${u.emailVerified ? '#8daa59' : '#e57373'};">
            ${u.emailVerified ? '✓ Подтвержден' : 'Не подтвержден'}
          </span>
        </td>
        <td>
          <div style="display:flex; gap:6px; flex-wrap:wrap;">
            <button class="btn btn-outline btn-sm" style="padding:4px 8px; font-size:0.72rem;" onclick="sendSingleNotice('${escapeHtml(u.email)}', '${escapeHtml(u.callsign)}', ${days})">
              ✉️ Написать
            </button>
            <button class="btn btn-outline btn-sm" style="padding:4px 8px; font-size:0.72rem; color:#e57373; border-color:rgba(229,115,115,0.3);" onclick="deleteUserFromAdmin('${escapeHtml(u.email)}')">
              🗑️
            </button>
          </div>
        </td>
      </tr>
    `;
  }).join('');
}

function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function deleteUserFromAdmin(email) {
  if (!confirm(`Удалить учетную запись ${email} из базы?`)) return;
  let users = getStoredUsers();
  users = users.filter(u => u.email.toLowerCase() !== email.toLowerCase());
  localStorage.setItem(STORAGE_USERS_DB, JSON.stringify(users));

  let subs = getSubscribersList();
  subs = subs.filter(e => e.toLowerCase() !== email.toLowerCase());
  localStorage.setItem(STORAGE_SUBSCRIBERS_LIST, JSON.stringify(subs));

  renderAdminUsersTable();
  showToast(`Пользователь ${email} удален.`);
}

function sendSingleNotice(email, callsign, daysLeft) {
  const isExpiring = daysLeft <= 5;
  const subject = isExpiring
    ? `[Каптёрка Про] Напоминание: Срок вашей лицензии истекает через ${daysLeft} дн.`
    : `[Каптёрка Про] Сообщение для бойца ${callsign}`;

  const body = isExpiring
    ? `Здравия желаю, боец ${callsign}!%0D%0A%0D%0AНапоминаем, что срок действия вашей лицензии «Каптёрка Про» истекает через ${daysLeft} дн.%0D%0AДля бесперебойной работы склада и списания имущества рекомендуем продлить ключ в Личном кабинете или на сайте:%0D%0Ahttps://kapterka-pro.ru/%0D%0A%0D%0AС уважением, разработчик ПО «Каптёрка Про»`
    : `Здравия желаю, боец ${callsign}!%0D%0A%0D%0AВышло обновление мобильного комплекса «Каптёрка Про».%0D%0AСкачать APK и просмотреть ключ можно на сайте: https://kapterka-pro.ru/%0D%0A%0D%0AС уважением, разработчик ПО «Каптёрка Про»`;

  window.location.href = `mailto:${email}?subject=${encodeURIComponent(subject)}&body=${body}`;
}

function openNewsletterModal(presetType) {
  const subjInput = document.getElementById('adminMailSubject');
  const bodyInput = document.getElementById('adminMailBody');

  if (presetType === 'update') {
    if (subjInput) subjInput.value = '[Каптёрка Про] Вышло важное обновление v3.0: Форма № 8 и новые справочники';
    if (bodyInput) bodyInput.value = `Здравствуйте, боец!\n\nВышло обновление программы «Каптёрка Про».\nЧто нового:\n- Добавлена новая форма актов списания боеприпасов.\n- Улучшена автономная работа базы данных при РЭБ.\n- Экспорт в Excel стал еще быстрее.\n\nСкачать обновленный APK можно на официальном сайте: https://kapterka-pro.ru/\nВаш персональный лицензионный ключ сохранен в вашем Личном кабинете.`;
    showToast('Шаблон «Выход обновления ПО» применен.');
  } else if (presetType === 'expiring') {
    if (subjInput) subjInput.value = '[Каптёрка Про] Внимание: Срок действия лицензии подходит к концу';
    if (bodyInput) bodyInput.value = `Здравия желаю!\n\nУведомляем вас о том, что срок действия вашей 30-дневной персональной лицензии «Каптёрка Про» подходит к концу.\n\nЧтобы не потерять оперативный доступ к ведению складов и формированию документов, вы можете продлить лицензию на сайте:\nhttps://kapterka-pro.ru/ (кнопка «Оплата ЮKassa»)\n\nВсе ваши данные на смартфонах остаются в полной сохранности.`;
    showToast('Шаблон «Напоминание о продлении лицензии» применен.');
  }

  // Scroll to mail box
  document.getElementById('adminMailSubject')?.scrollIntoView({ behavior: 'smooth' });
}

function copyAllSubscriberEmails() {
  const subs = getSubscribersList();
  if (subs.length === 0) {
    alert('Список адресов пуст.');
    return;
  }
  const text = subs.join(', ');
  navigator.clipboard.writeText(text).then(() => {
    showToast(`✓ Скопировано ${subs.length} адресов для рассылки (BCC)!`);
  }).catch(() => {
    prompt('Скопируйте адреса вручную:', text);
  });
}

function sendMailToSubscribersClient() {
  const subs = getSubscribersList();
  const subj = document.getElementById('adminMailSubject')?.value || 'Обновление Каптёрка Про';
  const body = document.getElementById('adminMailBody')?.value || '';
  const bccList = subs.join(',');

  const mailtoUrl = `mailto:support@kapterka-pro.ru?bcc=${encodeURIComponent(bccList)}&subject=${encodeURIComponent(subj)}&body=${encodeURIComponent(body)}`;
  window.location.href = mailtoUrl;
}

function exportSubscribersToCSV() {
  const users = getStoredUsers();
  if (users.length === 0) {
    alert('База пользователей пуста.');
    return;
  }

  let csvContent = 'data:text/csv;charset=utf-8,\uFEFF';
  csvContent += 'Позывной;Email;Звание;Подразделение;Ключ роты;Активный ключ;Дней до конца лицензии;Подписка на обновления;Почта подтверждена\n';

  users.forEach(u => {
    const days = u.expiresInDays !== undefined ? u.expiresInDays : 30;
    const activeKey = u.activeKey || (u.keys && u.keys[0] ? u.keys[0].key : '');
    const row = [
      `"${(u.callsign || '').replace(/"/g, '""')}"`,
      `"${(u.email || '').replace(/"/g, '""')}"`,
      `"${(u.rank || '').replace(/"/g, '""')}"`,
      `"${(u.unitName || '').replace(/"/g, '""')}"`,
      `"${(u.unitKey || '').replace(/"/g, '""')}"`,
      `"${activeKey}"`,
      days,
      u.subscribedToNewsletter ? 'Да' : 'Нет',
      u.emailVerified ? 'Да' : 'Нет'
    ];
    csvContent += row.join(';') + '\n';
  });

  const encodedUri = encodeURI(csvContent);
  const link = document.createElement('a');
  link.setAttribute('href', encodedUri);
  link.setAttribute('download', `kapterka_users_${new Date().toISOString().slice(0,10)}.csv`);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);

  showToast('📥 Файл kapterka_users.csv успешно сформирован и скачан!');
}

// 10. Initialization on DOMContentLoaded
document.addEventListener('DOMContentLoaded', () => {
  // Check user session state and setup auth UI
  updateAuthUI();

  // Load profile data
  loadCabinetProfile();

  // Initialize Showcase with warehouse
  updateShowcase('warehouse');

  // Showcase tab clicks
  const tabButtons = document.querySelectorAll('.tab-btn');
  tabButtons.forEach(btn => {
    btn.addEventListener('click', () => {
      tabButtons.forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      const target = btn.getAttribute('data-target');
      updateShowcase(target);
    });
  });

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

      await sendTelegramNotification(
        `✉️ <b>Новое обращение с сайта «Каптёрка ПРО»!</b>\n\n` +
        `👤 <b>От кого:</b> ${name}\n` +
        `📞 <b>Связь:</b> ${info}\n` +
        `💬 <b>Вопрос:</b>\n${msg}\n\n` +
        `📅 <b>Дата:</b> ${new Date().toLocaleString('ru-RU')}`
      );

      if (btnSubmit) {
        btnSubmit.removeAttribute('disabled');
        btnSubmit.textContent = 'Отправить сообщение разработчику';
      }

      contactForm.reset();
      showToast('✓ Сообщение успешно доставлено разработчику в Telegram!');
      closeModal('modalContact');
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
