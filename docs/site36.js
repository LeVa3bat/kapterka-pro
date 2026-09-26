/* Каптёрка PRO: переключатель «Для кого» и демонстрация «Остаток меняется сам». */
(function () {
  // "Кто вы?": one card, content per audience.
  var personas = [
    { img: 'screens/36-8-form8.jpg', head: 'Имущество роты под контролем',
      list: ['Выдача по взводам и бойцам с распиской в форме 8', 'Книга учёта по форме 18 — сама, из операций', 'Заявки от взводов: видно, что собрано и что выдано'] },
    { img: 'screens/36-3-stock-table.jpg', head: 'Склад без бумажной путаницы',
      list: ['Приход, расход и остаток по каждому складу', 'Перемещение между складами в одно касание', 'Поиск по названию и фильтр по группам'] },
    { img: 'screens/36-4-requests.jpg', head: 'Гуманитарная помощь — прозрачно',
      list: ['Приём грузов и выдача адресатам с журналом', 'Несколько телефонов команды видят одни данные', 'Отчёт для отчётности в Excel за минуту'] },
    { img: 'screens/36-5-journal.jpg', head: 'Инструмент всегда на счету',
      list: ['Кто взял инструмент и когда — в журнале по дням', 'Возврат и списание с причиной', 'Работает в цеху без интернета'] }
  ];
  var tabs = document.querySelectorAll('.lp-persona-tabs button');
  var img = document.getElementById('personaImg');
  var head = document.getElementById('personaHead');
  var list = document.getElementById('personaList');
  tabs.forEach(function (tab) {
    tab.addEventListener('click', function () {
      var p = personas[Number(tab.getAttribute('data-persona')) || 0];
      tabs.forEach(function (t) { t.classList.toggle('on', t === tab); t.setAttribute('aria-selected', t === tab ? 'true' : 'false'); });
      img.src = p.img;
      head.textContent = p.head;
      list.replaceChildren.apply(list, p.list.map(function (text) { var li = document.createElement('li'); li.textContent = text; return li; }));
      if (typeof ym === 'function') { try { ym(112255061, 'reachGoal', 'persona_' + tab.getAttribute('data-persona')); } catch (e) {} }
    });
  });
})();

// «Остаток меняется сам»: демонстрация на вымышленных данных. Ничего не сохраняется и не отправляется.
(function () {
  var root = document.getElementById('lpLedger');
  var journal = document.getElementById('lpJournal');
  var resetBtn = document.getElementById('lpReset');
  if (!root || !journal || !resetBtn) return;
  var STEP = 3;
  var rows = Array.prototype.slice.call(root.querySelectorAll('tr[data-item]'));
  var start = rows.map(function (r) { return Number(r.getAttribute('data-qty')); });
  var qty = start.slice();
  var empty = journal.firstElementChild; // строка «Пока пусто»

  function paint(i, flash) {
    var row = rows[i];
    var cell = row.querySelector('.lp-qty');
    var btn = row.querySelector('.lp-issue');
    var name = row.getAttribute('data-item');
    var can = qty[i] >= STEP;
    cell.textContent = String(qty[i]);
    btn.disabled = !can;
    btn.textContent = can ? 'Выдать ' + STEP : 'Нет остатка';
    btn.setAttribute('aria-label', can ? 'Выдать ' + STEP + ': ' + name : 'Остатка недостаточно: ' + name);
    if (flash) { cell.classList.remove('lp-flash'); void cell.offsetWidth; cell.classList.add('lp-flash'); }
  }
  function stamp() {
    var d = new Date();
    return ('0' + d.getHours()).slice(-2) + ':' + ('0' + d.getMinutes()).slice(-2);
  }

  rows.forEach(function (row, i) {
    row.querySelector('.lp-issue').addEventListener('click', function () {
      if (qty[i] < STEP) return;
      qty[i] -= STEP;
      paint(i, true);
      if (empty && empty.parentNode) empty.parentNode.removeChild(empty);
      var li = document.createElement('li');
      var time = document.createElement('time');
      var text = document.createElement('span');
      time.textContent = stamp();
      text.textContent = 'Выдача −' + STEP + ' · ' + row.getAttribute('data-item') + ' · остаток ' + qty[i];
      li.appendChild(time);
      li.appendChild(text);
      journal.insertBefore(li, journal.firstChild);
      while (journal.children.length > 6) journal.removeChild(journal.lastChild);
    });
    paint(i, false);
  });

  resetBtn.addEventListener('click', function () {
    qty = start.slice();
    rows.forEach(function (_, i) { paint(i, false); });
    journal.replaceChildren(empty);
  });
})();
