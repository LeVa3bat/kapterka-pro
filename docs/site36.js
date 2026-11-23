/* Каптёрка PRO: переключатель стартовых профилей (Путь А) и интерактивная демонстрация учёта. */
(function () {
  var personas = [
    {
      img: 'screens/36-2-dashboard-preview.webp',
      tag: 'Стартовый профиль № 2 • Гражданский учёт (64 позиции)',
      head: 'Мастерская, бригада и гражданский склад',
      desc: 'Готовый начальный справочник на 64 позиции в 8 категориях (электроинструмент, ручной инструмент, расходники, крепёж, СИЗ, электрика, ГСМ, оборудование) и бланки М-11, МБ-7, МБ-8.',
      list: [
        'Накладная перемещения М-11 (ОКУД 0315006) / ТОРГ-13, ведомость выдачи на руки МБ-7 (ОКУД 0320003) и акт списания МБ-8',
        'Честный учёт остатков: внутренние перемещения не раздувают расход, разные единицы измерения не смешиваются',
        'Бесплатный базовый режим офлайн (1 склад, 15 ключевых позиций) — навсегда после 3 дней демо'
      ]
    },
    {
      img: 'screens/36-8-form8-preview.webp',
      tag: 'Стартовый профиль № 1 • Военный учёт (Ф-8 / Ф-18)',
      head: 'Склад подразделения и снабжение',
      desc: 'Готовый справочник по службам (РАВ, БПЛА, Связь, Вещевая, ГСМ, Мед), КЗ и передовые точки, официальные печатные формы с грифом «УТВЕРЖДАЮ» и кодами ОКУД.',
      list: [
        'Точная Раздаточная ведомость Форма № 8 (ОКУД 6002203, Приказ МО РФ № 139) и Книга учёта Форма № 18 (ОКУД 0504042)',
        'Заявки от подразделений по этапам: Новая → Сборка → Собрана → Выдана со склада',
        'При обновлении старой версии все ваши склады, остатки и история сохраняются без изменений'
      ]
    },
    {
      img: 'screens/36-3-stock-table-preview.webp',
      tag: 'Стартовый профиль № 3 • Чистый лист',
      head: 'Пустой склад — всё на ваше усмотрение',
      desc: 'Один «Основной склад» и 0 навязанных карточек или категорий. Заведите только свою структуру с нуля — ничего лишнего перед глазами.',
      list: [
        'Полная свобода структуры: свои названия складов, стеллажей, цехов, категорий и карточек',
        'В любой момент в настройках можно переключить бланки (гражданские / военные) или догрузить шаблон каталога',
        'Локальная резервная копия в файл — данные всегда под вашим контролем'
      ]
    },
    {
      img: 'screens/36-6-unit-qr-preview.webp',
      tag: 'Командная работа • Синхронизация по QR',
      head: 'Волонтёрские штабы и выездные бригады',
      desc: 'Несколько телефонов работают с одной базой. Наведите камеру на QR-код первого телефона — и вся команда видит актуальные остатки.',
      list: [
        'Приём грузов и выдача адресатам с точной датой, временем и ответственным',
        'Учёт работает без интернета в дороге и на складе; обмен запускается при появлении сети',
        'Синхронизацию можно поставить на паузу или удалить облачную копию одной кнопкой'
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
})();

// «Остаток меняется сам»: интерактивная демонстрация на вымышленных данных (Мастерская / Подразделение).
(function () {
  var root = document.getElementById('lpLedger');
  var journal = document.getElementById('lpJournal');
  var resetBtn = document.getElementById('lpReset');
  if (!root || !journal || !resetBtn) return;

  var PRESETS = {
    workshop: {
      caption: 'Склад «Мастерская № 1» (пример)',
      items: [
        { name: 'Перфоратор SDS-Plus', unit: 'шт.', qty: 6 },
        { name: 'Диски отрезные 125 мм', unit: 'уп.', qty: 35 },
        { name: 'Краги сварочные', unit: 'пар', qty: 24 }
      ]
    },
    unit: {
      caption: 'Склад подразделения (КЗ, пример)',
      items: [
        { name: 'Рукавицы утеплённые', unit: 'пар', qty: 42 },
        { name: 'Спальный мешок', unit: 'шт.', qty: 18 },
        { name: 'Тушёнка говяжья', unit: 'банок', qty: 240 }
      ]
    }
  };

  var ISSUE_STEP = 3;
  var INCOME_STEP = 5;
  var currentPreset = 'workshop';
  var rows = Array.prototype.slice.call(root.querySelectorAll('tr[data-item]'));
  var captionEl = root.querySelector('table caption');
  var qty = rows.map(function (r) { return Number(r.getAttribute('data-qty')); });
  var empty = journal.firstElementChild;
  var modeBtns = document.querySelectorAll('[data-ledger-preset]');

  function stamp() {
    var d = new Date();
    return ('0' + d.getHours()).slice(-2) + ':' + ('0' + d.getMinutes()).slice(-2);
  }

  function paintRow(i, flashClass) {
    var row = rows[i];
    if (!row) return;
    var cell = row.querySelector('.lp-qty');
    var issueBtn = row.querySelector('.lp-issue');
    var incomeBtn = row.querySelector('.lp-income');
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
  }

  function appendLog(kind, deltaText, itemName, newQty) {
    if (empty && empty.parentNode) empty.parentNode.removeChild(empty);
    var li = document.createElement('li');
    li.className = kind === 'in' ? 'lp-log-in' : 'lp-log-out';
    var time = document.createElement('time');
    var text = document.createElement('span');
    time.textContent = stamp();
    text.textContent = deltaText + ' · ' + itemName + ' · остаток ' + newQty;
    li.appendChild(time);
    li.appendChild(text);
    journal.insertBefore(li, journal.firstChild);
    while (journal.children.length > 6) journal.removeChild(journal.lastChild);
  }

  function loadPreset(presetKey) {
    var preset = PRESETS[presetKey] || PRESETS.workshop;
    currentPreset = presetKey in PRESETS ? presetKey : 'workshop';
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
      paintRow(i, '');
    });
    if (empty) journal.replaceChildren(empty);
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
})();
