/* Каптёрка PRO 3.6: live phone in the hero and the "Кто вы?" switcher. */
(function () {
  var reduce = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  // Hero phone: crossfade between real screens with a caption.
  var phone = document.querySelector('.s36-live-phone');
  var caption = document.getElementById('s36LiveText');
  if (phone && caption) {
    var shots = phone.querySelectorAll('img');
    var texts = [
      'Остатки по складам — в одной таблице',
      'Заявки: новая → сборка → собрана → выдана',
      'Второй телефон подключается по QR-коду',
      'Форма 8 и 18 — в Excel одной кнопкой'
    ];
    var galleryIndex = [0, 2, 4, 5];
    var i = 0;
    window.s36LiveIndex = 0;
    var show = function (n) {
      shots[i].classList.remove('on');
      i = n % shots.length;
      shots[i].classList.add('on');
      caption.textContent = texts[i];
      window.s36LiveIndex = galleryIndex[i];
    };
    if (!reduce) {
      var timer = setInterval(function () { show(i + 1); }, 3200);
      document.addEventListener('visibilitychange', function () {
        if (document.hidden) { clearInterval(timer); } else { timer = setInterval(function () { show(i + 1); }, 3200); }
      });
    }
  }

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
  var tabs = document.querySelectorAll('.s36-persona-tabs button');
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
      if (typeof ym === 'function') { try { ym(112482290, 'reachGoal', 'persona_' + tab.getAttribute('data-persona')); } catch (e) {} }
    });
  });
})();
