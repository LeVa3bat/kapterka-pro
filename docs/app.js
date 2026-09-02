// Application Interactive Controller

document.addEventListener('DOMContentLoaded', () => {
  // Mobile Navigation Toggle
  const mobileToggle = document.getElementById('mobileToggle');
  const navLinks = document.getElementById('navLinks');
  if (mobileToggle && navLinks) {
    mobileToggle.addEventListener('click', () => {
      navLinks.classList.toggle('open');
      mobileToggle.textContent = navLinks.classList.contains('open') ? '✕' : '☰';
    });

    // Close menu when a navigation item is clicked
    navLinks.querySelectorAll('a').forEach(link => {
      link.addEventListener('click', () => {
        navLinks.classList.remove('open');
        mobileToggle.textContent = '☰';
      });
    });
  }

  // Showcase / Carousel Data
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

  // Switcher function for Showcase
  const tabButtons = document.querySelectorAll('.tab-btn');
  const showcaseTitle = document.getElementById('showcaseTitle');
  const showcaseDesc = document.getElementById('showcaseDesc');
  const showcaseFeatures = document.getElementById('showcaseFeatures');
  const showcaseScreen = document.getElementById('showcaseScreen');

  function updateShowcase(key) {
    const data = screenData[key];
    if (!data) return;

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

  tabButtons.forEach(btn => {
    btn.addEventListener('click', () => {
      tabButtons.forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      const target = btn.getAttribute('data-target');
      updateShowcase(target);
    });
  });

  // Cabinet Tabs
  const cabinetTabs = document.querySelectorAll('.cabinet-tab');
  const tabCheck = document.getElementById('tabContentCheck');
  const tabAuth = document.getElementById('tabContentAuth');

  cabinetTabs.forEach(tab => {
    tab.addEventListener('click', () => {
      cabinetTabs.forEach(t => t.classList.remove('active'));
      tab.classList.add('active');
      const target = tab.getAttribute('data-tab');

      if (target === 'check') {
        if (tabCheck) tabCheck.style.display = 'block';
        if (tabAuth) tabAuth.style.display = 'none';
      } else {
        if (tabCheck) tabCheck.style.display = 'none';
        if (tabAuth) tabAuth.style.display = 'block';
      }
    });
  });

  // Device ID Check Simulator / Checker
  const btnCheckDevice = document.getElementById('btnCheckDevice');
  const inputDeviceId = document.getElementById('inputDeviceId');
  const statusBox = document.getElementById('statusBox');
  const statusResultText = document.getElementById('statusResultText');
  const statusDaysLeft = document.getElementById('statusDaysLeft');
  const statusDevIdDisp = document.getElementById('statusDevIdDisp');

  if (btnCheckDevice && inputDeviceId && statusBox) {
    btnCheckDevice.addEventListener('click', () => {
      const val = inputDeviceId.value.trim();
      if (!val) {
        alert('Пожалуйста, укажите Ключ (Device ID) из приложения «Каптёрка Про» (раздел Меню ➔ О программе)');
        return;
      }

      statusBox.classList.add('active');
      statusDevIdDisp.textContent = val;
      
      // Simulate status
      if (val.toLowerCase().includes('trial') || val.length < 8) {
        statusResultText.innerHTML = '<span style="color:#ffb300; font-weight:700;">ПРОБНЫЙ ПЕРИОД (3 ДНЯ)</span>';
        statusDaysLeft.textContent = '2 дня осталось';
      } else {
        statusResultText.innerHTML = '<span style="color:#8daa59; font-weight:700;">PRO-ЛИЦЕНЗИЯ АКТИВНА</span>';
        statusDaysLeft.textContent = '30 дней (до 02.10.2026)';
      }
    });
  }

  // Modals Controller
  const modalOffer = document.getElementById('modalOffer');
  const modalPrivacy = document.getElementById('modalPrivacy');
  const modalContact = document.getElementById('modalContact');
  const modalAuth = document.getElementById('modalAuth');

  window.openModal = function(id) {
    const modal = document.getElementById(id);
    if (modal) modal.classList.add('open');
  };

  window.closeModal = function(id) {
    const modal = document.getElementById(id);
    if (modal) modal.classList.remove('open');
  };

  // Close on outside click
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

  // Registration form submission
  const regForm = document.getElementById('regForm');
  if (regForm) {
    regForm.addEventListener('submit', (e) => {
      e.preventDefault();
      alert('Регистрация прошла успешно! Вы можете привязать Device ID в личном кабинете.');
      closeModal('modalAuth');
    });
  }
});
