// ==========================================================================
// KAPTERKA PRO — APP.JS (ENHANCED WITH TABS, CABINET, AND YOOKASSA MIRROR)
// ==========================================================================

// Global state keys for persistence
const STORAGE_USER_CALLSIGN = 'kapterka_user_callsign';
const STORAGE_USER_RANK = 'kapterka_user_rank';
const STORAGE_UNIT_NAME = 'kapterka_unit_name';
const STORAGE_UNIT_KEY = 'kapterka_unit_key';
const STORAGE_USER_EMAIL = 'kapterka_user_email';
const STORAGE_USER_PHONE = 'kapterka_user_phone';
const STORAGE_ACTIVE_KEY = 'kapterka_active_key';
const STORAGE_KEYS_HISTORY = 'kapterka_keys_history';

// Default initial state
const defaultProfile = {
  callsign: 'Лева',
  rank: 'Старшина роты',
  unitName: '1-я Мотострелковая рота',
  unitKey: 'kapt_59e13b',
  email: 'alex.666.881@gmail.com',
  phone: '+7 (999) 000-00-00',
  activeKey: 'KAPT-8R4K-7M9X-3V2L',
  keys: [
    { key: 'KAPT-8R4K-7M9X-3V2L', callsign: 'Лева', unit: '1-я МСР', status: 'Активен (30 дн)', date: '02.09.2026' },
    { key: 'KAPT-3F9W-2Y7N-8Q4M', callsign: 'Лева', unit: 'Взвод БПЛА', status: 'Архив', date: '02.08.2026' }
  ]
};

// 1. Toast Notification Helper
function showToast(msg) {
  const toast = document.getElementById('tacticalToast');
  const msgEl = document.getElementById('toastMessage');
  if (!toast || !msgEl) return;

  msgEl.textContent = msg;
  toast.classList.add('show');
  setTimeout(() => {
    toast.classList.remove('show');
  }, 3500);
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
    'tabDownload': 'btnTabDownload'
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
  const payCallsignInput = document.getElementById('payCallsignInput');
  const payEmailInput = document.getElementById('payEmailInput');

  if (callsignInput) callsignInput.value = callsign;
  if (rankInput) rankInput.value = rank;
  if (unitNameInput) unitNameInput.value = unitName;
  if (unitKeyInput) unitKeyInput.value = unitKey;
  if (emailInput) emailInput.value = email;
  if (phoneInput) phoneInput.value = phone;
  if (activeKeyDisp) activeKeyDisp.textContent = activeKey;
  if (navCallsignDisplay) navCallsignDisplay.textContent = callsign;
  if (payCallsignInput) payCallsignInput.value = callsign;
  if (payEmailInput) payEmailInput.value = email;

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

// 4. Military License Key Generation (KAPT-XXXX-XXXX-XXXX)
function generateMilitaryLicenseKey() {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
  function seg(len) {
    let s = '';
    for (let i = 0; i < len; i++) {
      s += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return s;
  }
  return `KAPT-${seg(4)}-${seg(4)}-${seg(4)}`;
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

// 7. YooKassa Payment Simulation & Key Issuance
function processYooKassaPayment() {
  const callsign = document.getElementById('payCallsignInput')?.value.trim() || 'Боец';
  const email = document.getElementById('payEmailInput')?.value.trim() || '';

  if (!callsign) {
    alert('Пожалуйста, укажите позывной бойца для регистрации лицензии!');
    return;
  }

  showToast('Подключение к платежному шлюзу ЮKassa (Shop ID: 1450722)...');

  setTimeout(() => {
    // Generate fresh key
    const newKey = generateMilitaryLicenseKey();
    applyNewPaidKey(newKey, callsign);
  }, 1200);
}

function simulatePaymentSuccessDemo() {
  const callsign = document.getElementById('payCallsignInput')?.value.trim() || 'Боец';
  const newKey = generateMilitaryLicenseKey();
  applyNewPaidKey(newKey, callsign);
}

function applyNewPaidKey(newKey, callsign) {
  // Update Live Display
  const liveDisplay = document.getElementById('liveGeneratedKeyDisplay');
  const liveStatus = document.getElementById('liveKeyStatusDisplay');
  const btnCopy = document.getElementById('btnCopyPaidKey');

  if (liveDisplay) liveDisplay.textContent = newKey;
  if (liveStatus) liveStatus.textContent = '✓ Оплачено 490 ₽ • 30 дней доступа';
  if (btnCopy) btnCopy.removeAttribute('disabled');

  // Update Local Storage active key
  localStorage.setItem(STORAGE_ACTIVE_KEY, newKey);
  const cabKeyDisp = document.getElementById('cabActiveKeyDisp');
  if (cabKeyDisp) cabKeyDisp.textContent = newKey;

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

  showToast(`Лицензия оплачена! Выдан ключ: ${newKey}`);
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

// 10. Initialization on DOMContentLoaded
document.addEventListener('DOMContentLoaded', () => {
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

  // Contact form submission
  const contactForm = document.getElementById('contactForm');
  if (contactForm) {
    contactForm.addEventListener('submit', (e) => {
      e.preventDefault();
      alert('Спасибо за обращение! Разработчик свяжется с вами в ближайшее время.');
      closeModal('modalContact');
    });
  }
});
