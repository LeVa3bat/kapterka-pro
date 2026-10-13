(function (scope) {
  'use strict';
  function quantity(value) {
    const text = String(value).trim().replace(',', '.');
    if (!/^\d{1,10}(?:\.\d{1,3})?$/.test(text)) throw new Error('Введите число от 0 до 1 000 000 000, до трёх знаков после запятой.');
    const result = Number(text);
    if (!Number.isFinite(result) || result > 1e9) throw new Error('Количество слишком велико.');
    return result;
  }
  function reconcile(rows) {
    return rows.map((row, index) => {
      if (!String(row.name).trim()) throw new Error('Строка ' + (index + 1) + ': укажите наименование.');
      if (!String(row.unit).trim()) throw new Error('Строка ' + (index + 1) + ': укажите единицу.');
      const book = quantity(row.book), actual = quantity(row.actual);
      const difference = Math.round((actual - book) * 1000) / 1000;
      return { name: String(row.name).trim(), unit: String(row.unit).trim(), book, actual, difference,
        shortage: Math.max(0, -difference), surplus: Math.max(0, difference) };
    });
  }
  function csvCell(value, protectFormula) {
    let text = String(value);
    // Не даём пользовательскому названию стать формулой при открытии CSV в Excel.
    if (protectFormula && /^[\s]*[=+\-@\t\r\n]/.test(text)) text = "'" + text;
    if (typeof value === 'number') text = text.replace('.', ',');
    return '"' + text.replace(/"/g, '""') + '"';
  }
  function csv(rows) {
    const header = ['Наименование','Единица','По учёту','По факту','Факт минус учёт','Недостача','Излишек'];
    return '\uFEFF' + [header, ...rows.map(row => [row.name,row.unit,row.book,row.actual,row.difference,row.shortage,row.surplus])]
      .map(row => row.map((value, index) => csvCell(value, index < 2)).join(';')).join('\r\n');
  }
  const api = { quantity, reconcile, csv };
  if (typeof module !== 'undefined') module.exports = api;
  if (!scope.document) return;
  const track = name => { if (typeof scope.trackPublicTool === 'function') scope.trackPublicTool(name); };
  const body = document.getElementById('reconcile-inputs');
  if (body) {
    const form = document.getElementById('reconcile-form');
    const message = document.getElementById('reconcile-status');
    const result = document.getElementById('reconcile-results');
    const exportBtn = document.getElementById('reconcile-export');
    let calculated = null;
    function invalidate() { calculated = null; exportBtn.disabled = true; result.replaceChildren(); message.textContent = 'Данные изменились. Рассчитайте заново.'; }
    function addRow() {
      if (body.children.length >= 100) return;
      const row = document.createElement('div'); row.className = 'reconcile-row';
      [['name','Наименование','text'],['unit','Единица','text'],['book','По учёту','text'],['actual','По факту','text']].forEach(([key,title,type]) => {
        const label = document.createElement('label'); label.textContent = title;
        const input = document.createElement('input'); input.type = type; input.dataset.field = key;
        input.maxLength = key === 'name' ? 120 : key === 'unit' ? 20 : 14;
        input.required = true; input.autocomplete = 'off';
        if (key === 'book' || key === 'actual') input.inputMode = 'decimal';
        label.append(input); row.append(label);
      });
      const remove = document.createElement('button'); remove.type = 'button'; remove.className = 'btn secondary'; remove.textContent = 'Убрать';
      remove.addEventListener('click', () => { row.remove(); invalidate(); if (!body.children.length) addRow(); document.getElementById('reconcile-add').disabled = body.children.length >= 100; });
      row.append(remove); body.append(row);
      document.getElementById('reconcile-add').disabled = body.children.length >= 100;
      return row;
    }
    document.getElementById('reconcile-add').addEventListener('click', () => { const row = addRow(); invalidate(); row?.querySelector('input').focus(); });
    body.addEventListener('input', invalidate);
    form.addEventListener('submit', event => {
      event.preventDefault();
      try {
        const rows = [...body.children].map(row => Object.fromEntries([...row.querySelectorAll('input')].map(input => [input.dataset.field,input.value])));
        calculated = reconcile(rows); result.replaceChildren();
        calculated.forEach(row => {
          const item = document.createElement('li');
          const difference = row.difference < 0 ? 'недостача ' + row.shortage : row.difference > 0 ? 'излишек ' + row.surplus : 'совпадает';
          item.textContent = row.name + ': ' + difference + ' ' + row.unit + ' (учёт ' + row.book + ', факт ' + row.actual + ')';
          result.append(item);
        });
        message.textContent = 'Сверка готова. Строк с расхождениями: ' + calculated.filter(row => row.difference !== 0).length + '. Разные единицы не суммируются.';
        exportBtn.disabled = false; track('calculator_use');
      } catch (error) { calculated = null; exportBtn.disabled = true; result.replaceChildren(); message.textContent = error.message; }
    });
    exportBtn.addEventListener('click', () => {
      if (!calculated) return;
      const url = URL.createObjectURL(new Blob([csv(calculated)], {type:'text/csv;charset=utf-8'}));
      const link = document.createElement('a'); link.href = url; link.download = 'sverka-ostatkov.csv'; link.click();
      setTimeout(() => URL.revokeObjectURL(url), 1000); track('calculator_export');
    });
    document.getElementById('reconcile-clear').addEventListener('click', () => { body.replaceChildren(); invalidate(); addRow(); message.textContent = 'Список очищен.'; });
    addRow();
  }
  const shares = {
    template: { title:'Пустой шаблон кладовой', text:'Бесплатный пустой шаблон учёта для небольшой мастерской.', url:'https://kapterka-pro.ru/uchet-instrumenta-android.html?utm_source=referral&utm_medium=share&utm_campaign=workshop_30d&utm_content=blank_template#workshop-kit' },
    app: { title:'Каптёрка PRO', text:'Учёт имущества и остатков на Android.', url:'https://kapterka-pro.ru/?utm_source=referral&utm_medium=share&utm_campaign=workshop_30d&utm_content=app' }
  };
  document.querySelectorAll('[data-public-share]').forEach(button => button.addEventListener('click', async () => {
    const payload = shares[button.dataset.publicShare]; if (!payload) return;
    const status = document.getElementById('public-share-status');
    if (navigator.share) {
      try {
        const pending = navigator.share(payload);
        track('share_menu_requested'); // Запрос меню, никогда не подтверждение открытия или доставки.
        await pending;
        if (status) status.textContent = 'Действие в системном меню завершено. Доставка сообщения не проверяется.';
      } catch (error) { if (status) status.textContent = error.name === 'AbortError' ? 'Действие отменено или нет доступного приложения для отправки.' : 'Меню недоступно. Скопируйте публичную ссылку ниже.'; }
    } else if (status) status.textContent = 'Системное меню недоступно. Скопируйте публичную ссылку ниже.';
    const link = document.getElementById('public-share-link');
    if (link) { link.hidden = false; link.parentElement.hidden = false; link.value = payload.url; link.focus(); link.select(); }
  }));
})(typeof window !== 'undefined' ? window : globalThis);
