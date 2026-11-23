/* Каптёрка PRO 4.0.0 — 3D-интерактив витрины: 3D-смартфон, профили, симулятор склада и 2-телефонных заявок. */
(function () {
  /* 1. 3D Hero Smartphone Switcher & Pointer Perspective Tilt */
  var HERO_SCREENS = [
    { src: 'screens/36-2-dashboard-preview.webp', alt: 'Каптёрка PRO 4.0.0 — Главный экран в тёмной 3D-теме' },
    { src: 'screens/40-military-light-preview.webp', alt: 'Каптёрка PRO 4.0.0 — Главный экран в светлой 3D-теме (Воинский учёт)' },
    { src: 'screens/40-splash-preview.webp', alt: 'Каптёрка PRO 4.0.0 — Живая 3D-заставка при запуске' },
    { src: 'screens/36-4-requests-preview.webp', alt: 'Каптёрка PRO 4.0.0 — Заявки в реальном времени между телефонами' },
    { src: 'screens/40-m11-preview.webp', alt: 'Каптёрка PRO 4.0.0 — Готовые отчёты Excel: М-11, МБ-7, МБ-8 и Формы № 8 / № 18' }
  ];

  var heroImg = document.getElementById('heroPhoneScreen');
  var heroBtns = document.querySelectorAll('[data-hero-screen]');
  if (heroImg && heroBtns.length) {
    heroBtns.forEach(function (btn) {
      btn.addEventListener('click', function () {
        var idx = Number(btn.getAttribute('data-hero-screen')) || 0;
        var item = HERO_SCREENS[idx] || HERO_SCREENS[0];
        heroImg.src = item.src;
        heroImg.alt = item.alt;
        heroBtns.forEach(function (b) {
          var on = Number(b.getAttribute('data-hero-screen')) === idx;
          b.classList.toggle('active', on);
          b.setAttribute('aria-pressed', on ? 'true' : 'false');
        });
      });
    });
  }

  var stage = document.getElementById('hero3dStage');
  var phoneShell = document.getElementById('heroPhoneShell');
  var reduceMotion = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  if (stage && phoneShell && !reduceMotion && window.matchMedia && window.matchMedia('(pointer: fine)').matches) {
    stage.addEventListener('pointermove', function (e) {
      var rect = stage.getBoundingClientRect();
      var x = (e.clientX - rect.left) / rect.width - 0.5;
      var y = (e.clientY - rect.top) / rect.height - 0.5;
      var ry = (-x * 16).toFixed(2);
      var rx = (y * 12).toFixed(2);
      phoneShell.style.transform = 'rotateY(' + ry + 'deg) rotateX(' + rx + 'deg) translateY(-3px)';
    });
    stage.addEventListener('pointerleave', function () {
      phoneShell.style.transform = '';
    });
  }

  /* 2. Starting Profiles Switcher (#profiles) */
  var personas = [
    {
      img: 'screens/36-2-dashboard-preview.webp',
      tag: 'Профиль № 1 • Гражданский склад / Мастерская (64 позиции)',
      head: 'Мастерская, бригада и гражданский склад',
      desc: 'Готовый справочник на 64 позиции в 8 категориях (электроинструмент, ручной инструмент, расходники, крепёж, СИЗ, электрика, ГСМ, оборудование), операция «Возврат» и бланки М-11, МБ-7, МБ-8.',
      list: [
        'Накладная М-11 (ОКУД 0315006) / ТОРГ-13, ведомость выдачи на руки МБ-7 (ОКУД 0320003) и акт списания МБ-8 в Excel',
        'Раздельный баланс без смешивания единиц измерения (шт., л., пары, уп.) и отдельная кнопка «Возврат» инструмента',
        'Бесплатный базовый режим офлайн (1 склад, 15 позиций) остаётся навсегда после 3 дней полного демо'
      ]
    },
    {
      img: 'screens/40-military-light-preview.webp',
      tag: 'Профиль № 2 • Воинский учёт (Ф-8 / Ф-18 · светлая и тёмная 3D-темы)',
      head: 'Склад подразделения и снабжение',
      desc: 'Готовый справочник по службам (РАВ, БПЛА, Связь, Вещевая, ГСМ, Мед), КЗ и передовые точки, официальные печатные формы с грифом «УТВЕРЖДАЮ» и кодами ОКУД.',
      list: [
        'Точная Раздаточная ведомость Форма № 8 (ОКУД 6002203, Приказ МО РФ № 139) и Книга учёта Форма № 18 (ОКУД 0504042)',
        'Мгновенная синхронизация заявок между телефонами со звуковым сигналом: Новая → Сборка → Собрана → Выдана',
        'При смене складов профиля старые операции очищаются чисто; при обновлении поверх старой версии база сохраняется'
      ]
    },
    {
      img: 'screens/40-profiles-preview.webp',
      tag: 'Профиль № 3 • Чистый лист с нуля',
      head: 'Пустой склад — всё на ваше усмотрение',
      desc: 'Один «Основной склад» и 0 навязанных карточек или категорий. Соберите свою структуру с нуля — ничего лишнего перед глазами.',
      list: [
        'Полная свобода структуры: свои названия складов, цехов, стеллажей, машин и категорий имущества',
        'В любой момент в профиле можно переключить бланки (гражданские / военные) или загрузить готовый справочник',
        'Локальная резервная копия в файл — база Room/SQLite всегда хранится на вашем телефоне'
      ]
    },
    {
      img: 'screens/36-6-unit-qr-preview.webp',
      tag: 'Командная работа • QR-подключение за 5 секунд',
      head: 'Волонтёрские штабы и выездные бригады',
      desc: 'Несколько телефонов работают с единой базой. Наведите камеру на QR-код первого телефона — и вся команда в реальном времени видит остатки и заявки.',
      list: [
        'Приём грузов, выдача и возврат по фамилии получателя с точным временем и ответственным',
        'Офлайн-работа без сети в дороге и на складе; авто-восстановление связи и защита локальных правок',
        'Синхронизацию можно поставить на паузу или удалить облачную копию в 1 клик'
      ]
    }
  ];

  var tabs = document.querySelectorAll('.lp-persona-tabs button');
  var img = document.getElementById('personaImg');
  var tag = document.getElementById('personaTag');
  var head = document.getElementById('personaHead');
  var desc = document.getElementById('personaDesc');
  var list = document.getElementById('personaList');

  function applyPersona(index) {
    var p = personas[index] || personas[0];
    tabs.forEach(function (t) {
      var active = Number(t.getAttribute('data-persona')) === index;
      t.classList.toggle('on', active);
      t.setAttribute('aria-selected', active ? 'true' : 'false');
    });
    if (img) img.src = p.img;
    if (tag) tag.textContent = p.tag;
    if (head) head.textContent = p.head;
    if (desc) desc.textContent = p.desc;
    if (list) {
      list.replaceChildren.apply(list, p.list.map(function (text) {
        var li = document.createElement('li');
        li.textContent = text;
        return li;
      }));
    }
  }

  tabs.forEach(function (tab) {
    tab.addEventListener('click', function () {
      var idx = Number(tab.getAttribute('data-persona')) || 0;
      applyPersona(idx);
      if (typeof ym === 'function') {
        try { ym(112255061, 'reachGoal', 'persona_' + idx); } catch (e) {}
      }
    });
  });

  document.querySelectorAll('[data-jump-persona]').forEach(function (link) {
    link.addEventListener('click', function () {
      var idx = Number(link.getAttribute('data-jump-persona')) || 0;
      applyPersona(idx);
    });
  });

  /* 3. Interactive 3D Warehouse Ledger + Unit-Safe Balance + Return + 2-Phone Requisition Simulator */
  var root = document.getElementById('lpLedger');
  var journal = document.getElementById('lpJournal');
  var resetBtn = document.getElementById('lpReset');
  if (!root || !journal || !resetBtn) return;

  var PRESETS = {
    workshop: {
      caption: 'Склад «Мастерская № 1» · Гражданский профиль (пример)',
      reqItem: 'Перфоратор SDS-Plus (1 шт.)',
      reqFrom: 'Телефон № 1 · Бригада № 2',
      reqTo: 'Телефон № 2 · Кладовщик склада',
      items: [
        { name: 'Перфоратор SDS-Plus', unit: 'шт.', qty: 6 },
        { name: 'Диски отрезные 125 мм', unit: 'уп.', qty: 35 },
        { name: 'Краги сварочные', unit: 'пар', qty: 24 }
      ]
    },
    unit: {
      caption: 'Склад подразделения (КЗ) · Военный профиль (пример)',
      reqItem: 'Спальный мешок (2 шт.)',
      reqFrom: 'Телефон № 1 · Взвод / точка',
      reqTo: 'Телефон № 2 · Каптёрка КЗ',
      items: [
        { name: 'Рукавицы утеплённые', unit: 'пар', qty: 42 },
        { name: 'Спальный мешок', unit: 'шт.', qty: 18 },
        { name: 'Тушёнка говяжья', unit: 'банок', qty: 240 }
      ]
    }
  };

  var ISSUE_STEP = 3;
  var INCOME_STEP = 5;
  var RETURN_STEP = 1;
  var currentPreset = 'workshop';
  var opsCount = 0;
  var rows = Array.prototype.slice.call(root.querySelectorAll('tr[data-item]'));
  var captionEl = root.querySelector('table caption');
  var qty = rows.map(function (r) { return Number(r.getAttribute('data-qty')); });
  var units = rows.map(function (r) {
    var sm = r.querySelector('.lp-name small');
    return sm ? sm.textContent.trim() : 'шт.';
  });
  var empty = journal.firstElementChild;
  var modeBtns = document.querySelectorAll('[data-ledger-preset]');
  var unitSummaryEl = document.getElementById('simUnitSummary');
  var opsCountEl = document.getElementById('simOpsCount');
  var activePosEl = document.getElementById('simActiveCount');

  function stamp() {
    var d = new Date();
    return ('0' + d.getHours()).slice(-2) + ':' + ('0' + d.getMinutes()).slice(-2);
  }

  function updateUnitSummary() {
    var byUnit = {};
    var activePos = 0;
    for (var i = 0; i < qty.length; i++) {
      if (qty[i] > 0) activePos++;
      var u = units[i] || 'шт.';
      byUnit[u] = (byUnit[u] || 0) + qty[i];
    }
    var parts = Object.keys(byUnit).map(function (u) { return byUnit[u] + ' ' + u; });
    if (unitSummaryEl) unitSummaryEl.textContent = parts.join(' · ');
    if (opsCountEl) opsCountEl.textContent = String(opsCount);
    if (activePosEl) activePosEl.textContent = String(activePos);
  }

  function paintRow(i, flashClass) {
    var row = rows[i];
    if (!row) return;
    var cell = row.querySelector('.lp-qty');
    var issueBtn = row.querySelector('.lp-issue');
    var incomeBtn = row.querySelector('.lp-income');
    var returnBtn = row.querySelector('.lp-return');
    var name = row.getAttribute('data-item');
    var canIssue = qty[i] >= ISSUE_STEP;
    if (cell) {
      cell.textContent = String(qty[i]);
      if (flashClass) {
        cell.classList.remove('lp-flash', 'lp-flash-in');
        void cell.offsetWidth;
        cell.classList.add(flashClass);
      }
    }
    if (issueBtn) {
      issueBtn.disabled = !canIssue;
      issueBtn.textContent = canIssue ? '−' + ISSUE_STEP + ' Выдать' : 'Нет остатка';
      issueBtn.setAttribute('aria-label', canIssue ? 'Выдать ' + ISSUE_STEP + ': ' + name : 'Остатка недостаточно: ' + name);
    }
    if (incomeBtn) {
      incomeBtn.setAttribute('aria-label', 'Приход +' + INCOME_STEP + ': ' + name);
    }
    if (returnBtn) {
      returnBtn.setAttribute('aria-label', 'Возврат +' + RETURN_STEP + ': ' + name);
    }
    updateUnitSummary();
  }

  function appendLog(kind, deltaText, itemName, newQty) {
    opsCount++;
    if (empty && empty.parentNode) empty.parentNode.removeChild(empty);
    var li = document.createElement('li');
    li.className = kind === 'in' ? 'lp-log-in' : (kind === 'ret' ? 'lp-log-ret' : 'lp-log-out');
    var time = document.createElement('time');
    var text = document.createElement('span');
    time.textContent = stamp();
    text.textContent = deltaText + ' · ' + itemName + ' · остаток ' + newQty;
    li.appendChild(time);
    li.appendChild(text);
    journal.insertBefore(li, journal.firstChild);
    while (journal.children.length > 6) journal.removeChild(journal.lastChild);
    updateUnitSummary();
  }

  /* Live 2-Phone Requisition Pipeline Simulator */
  var reqStage = 0;
  var reqSteps = document.querySelectorAll('[data-req-stage]');
  var reqBtn = document.getElementById('reqSimAdvanceBtn');
  var reqP1Title = document.getElementById('reqPhone1Title');
  var reqP1Sub = document.getElementById('reqPhone1Sub');
  var reqP2Title = document.getElementById('reqPhone2Title');
  var reqP2Sub = document.getElementById('reqPhone2Sub');
  var reqArrow = document.getElementById('reqSimArrow');
  var reqCountEl = document.getElementById('simReqCount');

  function renderReqStage() {
    var preset = PRESETS[currentPreset] || PRESETS.workshop;
    var stageInfo = [
      {
        btn: 'Перевести в «Сборка» →',
        p1t: 'Отправлена: ' + preset.reqItem,
        p1s: 'Статус: Новая · синхронизировано за 0,4 с',
        p2t: '🔔 Новая заявка со звуком!',
        p2s: preset.reqItem + ' · ожидает сборки',
        arrow: 'QR-контур ⚡ В сети'
      },
      {
        btn: 'Отметить «Собрана» →',
        p1t: 'Собирается на складе',
        p1s: 'Статус: Сборка · видно в реальном времени',
        p2t: 'Кладовщик собирает заказ',
        p2s: preset.reqItem + ' · этап Сборка',
        arrow: 'Синхронизация ✓'
      },
      {
        btn: 'Выдать со склада (−1) →',
        p1t: 'Готово к получению!',
        p1s: 'Статус: Собрана · можно забирать',
        p2t: 'Собрано на выдаче',
        p2s: 'Нажмите «Выдать» — остаток спишется сам',
        arrow: 'Готово к выдаче ✓'
      },
      {
        btn: 'Создать новую заявку ↻',
        p1t: 'Получено: ' + preset.reqItem,
        p1s: 'Статус: Выдана · запись в журнале',
        p2t: '✓ Выдано и списано с остатка',
        p2s: 'Баланс обновлён на обоих телефонах',
        arrow: 'Проведено в учёт ✓'
      }
    ][reqStage];

    reqSteps.forEach(function (el) {
      var s = Number(el.getAttribute('data-req-stage'));
      el.classList.toggle('done', s < reqStage);
      el.classList.toggle('active', s === reqStage);
    });
    if (reqBtn && stageInfo) reqBtn.textContent = stageInfo.btn;
    if (reqP1Title && stageInfo) reqP1Title.textContent = stageInfo.p1t;
    if (reqP1Sub && stageInfo) reqP1Sub.textContent = stageInfo.p1s;
    if (reqP2Title && stageInfo) reqP2Title.textContent = stageInfo.p2t;
    if (reqP2Sub && stageInfo) reqP2Sub.textContent = stageInfo.p2s;
    if (reqArrow && stageInfo) reqArrow.textContent = stageInfo.arrow;
    if (reqCountEl) reqCountEl.textContent = reqStage === 3 ? '0' : '1';
    var p1Head = document.getElementById('reqPhone1Head');
    var p2Head = document.getElementById('reqPhone2Head');
    if (p1Head) p1Head.textContent = preset.reqFrom;
    if (p2Head) p2Head.textContent = preset.reqTo;
  }

  if (reqBtn) {
    reqBtn.addEventListener('click', function () {
      if (reqStage < 3) {
        reqStage++;
        if (reqStage === 3 && qty[0] >= 1) {
          qty[0] -= 1;
          paintRow(0, 'lp-flash');
          appendLog('out', 'Заявка выдана −1', rows[0].getAttribute('data-item'), qty[0]);
        }
      } else {
        reqStage = 0;
      }
      renderReqStage();
    });
  }

  function loadPreset(presetKey) {
    var preset = PRESETS[presetKey] || PRESETS.workshop;
    currentPreset = presetKey in PRESETS ? presetKey : 'workshop';
    opsCount = 0;
    reqStage = 0;
    modeBtns.forEach(function (btn) {
      var active = btn.getAttribute('data-ledger-preset') === currentPreset;
      btn.classList.toggle('active', active);
      btn.setAttribute('aria-pressed', active ? 'true' : 'false');
    });
    if (captionEl) captionEl.textContent = preset.caption;
    rows.forEach(function (row, i) {
      var item = preset.items[i];
      if (!item) return;
      row.setAttribute('data-item', item.name);
      row.setAttribute('data-qty', String(item.qty));
      var nameCell = row.querySelector('.lp-name');
      if (nameCell) {
        nameCell.textContent = '';
        nameCell.appendChild(document.createTextNode(item.name));
        var small = document.createElement('small');
        small.textContent = item.unit;
        nameCell.appendChild(small);
      }
      qty[i] = item.qty;
      units[i] = item.unit;
      paintRow(i, '');
    });
    if (empty) journal.replaceChildren(empty);
    renderReqStage();
    updateUnitSummary();
  }

  rows.forEach(function (row, i) {
    var issueBtn = row.querySelector('.lp-issue');
    if (issueBtn) {
      issueBtn.addEventListener('click', function () {
        if (qty[i] < ISSUE_STEP) return;
        qty[i] -= ISSUE_STEP;
        paintRow(i, 'lp-flash');
        appendLog('out', 'Выдача −' + ISSUE_STEP, row.getAttribute('data-item'), qty[i]);
      });
    }
    var incomeBtn = row.querySelector('.lp-income');
    if (incomeBtn) {
      incomeBtn.addEventListener('click', function () {
        qty[i] += INCOME_STEP;
        paintRow(i, 'lp-flash-in');
        appendLog('in', 'Приход +' + INCOME_STEP, row.getAttribute('data-item'), qty[i]);
      });
    }
    var returnBtn = row.querySelector('.lp-return');
    if (returnBtn) {
      returnBtn.addEventListener('click', function () {
        qty[i] += RETURN_STEP;
        paintRow(i, 'lp-flash-in');
        appendLog('ret', 'Возврат +' + RETURN_STEP, row.getAttribute('data-item'), qty[i]);
      });
    }
    paintRow(i, '');
  });

  modeBtns.forEach(function (btn) {
    btn.addEventListener('click', function () {
      loadPreset(btn.getAttribute('data-ledger-preset'));
    });
  });

  resetBtn.addEventListener('click', function () {
    loadPreset(currentPreset);
  });

  renderReqStage();
  updateUnitSummary();
})();
