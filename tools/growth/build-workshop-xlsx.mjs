// Build with the Codex bundled artifact-tool. Copy this file to a temporary
// directory whose node_modules junction points to the bundled dependencies.
// Run: node --max-old-space-size=6144 build-workshop-xlsx.mjs <repository>
import fs from 'node:fs/promises';
import path from 'node:path';
import assert from 'node:assert/strict';
import { Workbook, SpreadsheetFile } from '@oai/artifact-tool';

const root = path.resolve(process.argv[2]);
const out = path.join(root, 'docs', 'downloads');
const qa = path.join(root, 'growth', 'qa');
const source = JSON.parse(await fs.readFile(path.join(qa, 'workshop-example-source.json'), 'utf8'));
const qr = await fs.readFile(path.join(qa, 'workshop-source-qr.png'));
const codes = Array.from({ length: 30 }, (_, i) => `W${String(i + 1).padStart(3, '0')}`);
const ink = '#173039';
const muted = '#52646B';
const input = '#FFF4CC';
const units = ['шт', 'пар', 'рул', 'набор', 'л', 'м', 'кг', 'упак'];
const lastItem = 37;
const lastRecord = 307;
const itemCodes = `'Остатки'!$A$8:$A$${lastItem}`;
const itemNames = `'Остатки'!$B$8:$B$${lastItem}`;
const itemOpening = `'Остатки'!$D$8:$D$${lastItem}`;
const records = col => `'Журнал'!$${col}$8:$${col}$${lastRecord}`;
const evidence = { generatedAt: '2026-10-13', sourceUrl: source.url, engine: 'Codex artifact-tool bundled 26.1011.11113', tests: [] };

function val(sheet, cell, value) { sheet.getRange(cell).values = [[value]]; }
function formula(sheet, cell, value) { sheet.getRange(cell).formulas = [[value]]; }
function base(sheet, range) {
  sheet.showGridLines = false;
  sheet.getRange(range).format.font = { name: 'Arial', size: 11, color: ink };
  sheet.getRange(range).format.rowHeight = 24;
  sheet.getRange(range).format.verticalAlignment = 'center';
}
function header(sheet, range) {
  sheet.getRange(range).format = { fill: ink, font: { name: 'Arial', size: 11, color: '#FFFFFF', bold: true }, rowHeight: 36, horizontalAlignment: 'center', verticalAlignment: 'center', wrapText: true };
}
function title(sheet, text, note) {
  val(sheet, 'A2', text);
  sheet.getRange('A2').format.font = { name: 'Arial', size: 17, bold: true, color: ink };
  sheet.getRange('A2:J2').format.rowHeight = 30;
  sheet.getRange('A3:J3').format.borders = { bottom: { style: 'thin', color: '#B9C8CC' } };
  val(sheet, 'A4', note);
  sheet.getRange('A4').format.font = { name: 'Arial', size: 11, color: muted, italic: true };
}
function decimalValidation(sheet, range, min, max, label) {
  sheet.getRange(range).dataValidation = {
    rule: { type: 'decimal', operator: 'between', formula1: min, formula2: max },
    errorAlert: { style: 'stop', title: 'Проверьте количество', message: label },
  };
}
function listValidation(sheet, range, values, message) {
  sheet.getRange(range).dataValidation = { rule: { type: 'list', values }, errorAlert: { style: 'stop', title: 'Выберите из списка', message } };
}
function errorFormatting(sheet, range, test) {
  sheet.getRange(range).conditionalFormats.addCustom(test, { fill: '#FDE6E6', font: { color: '#A71D2A', bold: true } });
}
function readCell(sheet, cell) { return sheet.getRange(cell).values[0][0]; }
function check(sheet, cell, expected, label) {
  const actual = readCell(sheet, cell);
  assert.equal(actual, expected, `${label}: ${sheet.name}!${cell}`);
  evidence.tests.push({ label, cell: `${sheet.name}!${cell}`, expected, actual });
}

async function buildAll() {
for (const example of [false, true]) {
  console.log(`Building ${example?'example':'blank'} workbook`);
  const wb = Workbook.create();
  const stock = wb.worksheets.add('Остатки');
  const journal = wb.worksheets.add('Журнал');
  const guide = wb.worksheets.add('Инструкция');
  stock.tabColor = ink;
  journal.tabColor = '#537981';
  guide.tabColor = '#C5D2D5';
  base(stock, 'A1:J39');
  base(journal, `A1:J${lastRecord}`);
  base(guide, 'A1:D28');
  title(stock, 'Кладовая мастерской: остатки', example ? 'Пример на вымышленных данных. Остаток означает количество на полке.' : 'Заполните название, единицу, начальный остаток и место. Жёлтые ячейки редактируются.');
  title(journal, 'Кладовая мастерской: журнал', 'Записывайте события по порядку. Количество положительное, знак задаёт операция.');
  const headers = ['Код', 'Наименование', 'Ед.', 'На начало', 'Приход', 'Выдача', 'Возврат', 'На полке', 'Проверка', 'Место хранения'];
  stock.getRange('A7:J7').values = [headers];
  journal.getRange('A7:J7').values = [['Дата', 'Код', 'Операция', 'Кол-во', 'Кому / от кого', 'Документ', 'Примечание', 'На полке после', 'Проверка', 'В расчёте']];
  header(stock, 'A7:J7');
  header(journal, 'A7:J7');
  const stockWidths = [75, 245, 60, 92, 85, 85, 85, 92, 235, 155];
  const journalWidths = [105, 75, 110, 90, 195, 115, 240, 115, 220, 85];
  for (let i=0; i<10; i++) {
    const col=String.fromCharCode(65+i);
    stock.getRange(`${col}7:${col}39`).format.columnWidthPx=stockWidths[i];
    journal.getRange(`${col}7:${col}${lastRecord}`).format.columnWidthPx=journalWidths[i];
  }
  stock.getRange('A8:A37').values = codes.map(code=>[code]);
  stock.getRange('B8:D37').format.fill = input;
  stock.getRange('J8:J37').format.fill = input;
  stock.getRange('A8:A37').format.fill = '#EDF3F3';
  stock.getRange('D8:H37').setNumberFormat('General');
  stock.getRange('D8:H37').format.horizontalAlignment='right';
  journal.getRange(`A8:G${lastRecord}`).format.fill=input;
  journal.getRange(`A8:A${lastRecord}`).setNumberFormat('dd.mm.yyyy');
  journal.getRange(`D8:D${lastRecord}`).setNumberFormat('General');
  journal.getRange(`H8:H${lastRecord}`).setNumberFormat('General');
  journal.getRange(`D8:D${lastRecord}`).format.horizontalAlignment='right';
  journal.getRange(`H8:H${lastRecord}`).format.horizontalAlignment='right';
  stock.freezePanes.freezeRows(7);
  journal.freezePanes.freezeRows(7);
  decimalValidation(stock, 'D8:D37', 0, 1e9, 'Введите число от 0 до 1 000 000 000. Ноль допустим.');
  listValidation(stock, 'C8:C37', units, 'Выберите единицу измерения.');
  decimalValidation(journal, `D8:D${lastRecord}`, 0.001, 1e9, 'Введите положительное число от 0,001 до 1 000 000 000.');
  listValidation(journal, `B8:B${lastRecord}`, codes, 'Выберите код W001-W030 из листа Остатки.');
  listValidation(journal, `C8:C${lastRecord}`, ['Приход', 'Выдача', 'Возврат'], 'Выберите Приход, Выдача или Возврат.');
  journal.getRange(`A8:A${lastRecord}`).dataValidation={rule:{type:'date',operator:'between',formula1:'=DATE(2000,1,1)',formula2:'=DATE(2100,12,31)'},errorAlert:{style:'stop',title:'Проверьте дату',message:'Введите дату между 01.01.2000 и 31.12.2100.'}};
  // Ownership of the calculations is visible: stock aggregates the journal,
  // while the journal calculates each running balance from opening stock.
  // Validation flag J checks inputs only and never depends on a balance result.
  for (let r=8;r<=lastItem;r++) {
    for (const [col, operation] of [['E','Приход'],['F','Выдача'],['G','Возврат']]) {
      formula(stock,`${col}${r}`,`=IF(OR(B${r}="",NOT(ISNUMBER(D${r}))),"",SUMIFS(${records('D')},${records('B')},A${r},${records('C')},"${operation}",${records('J')},"Да"))`);
    }
    formula(stock,`H${r}`,`=IF(OR(B${r}="",NOT(ISNUMBER(D${r}))),"",D${r}+E${r}-F${r}+G${r})`);
    formula(stock,`I${r}`,`=IF(B${r}="","",IF(A${r}="","Укажите код",IF(COUNTIFS($A$8:$A$37,A${r})>1,"Повтор кода",IF(C${r}="","Укажите единицу",IF(NOT(ISNUMBER(D${r})),"Укажите начальный остаток",IF(H${r}<0,"Отрицательный остаток",IF(G${r}>F${r},"Проверьте возврат","")))))))`);
  }
  for (let r=8;r<=lastRecord;r++) {
    formula(journal,`J${r}`,`=IF(COUNTA(A${r}:G${r})=0,"",IF(COUNTIFS(${itemCodes},B${r})<>1,"Нет",IF(AND(ISNUMBER(A${r}),A${r}>=DATE(2000,1,1),A${r}<=DATE(2100,12,31),OR(C${r}="Приход",C${r}="Выдача",C${r}="Возврат"),ISNUMBER(D${r}),D${r}>=0.001,D${r}<=1000000000,INDEX(${itemNames},MATCH(B${r},${itemCodes},0))<>"",ISNUMBER(INDEX(${itemOpening},MATCH(B${r},${itemCodes},0))),NOT(ISBLANK(INDEX(${itemOpening},MATCH(B${r},${itemCodes},0))))),"Да","Нет")))`);
    const roll=(operation)=>`SUMIFS($D$8:D${r},$B$8:B${r},B${r},$C$8:C${r},"${operation}",$J$8:J${r},"Да")`;
    formula(journal,`H${r}`,`=IF(J${r}<>"Да","",SUMIFS(${itemOpening},${itemCodes},B${r})+${roll('Приход')}-${roll('Выдача')}+${roll('Возврат')})`);
    formula(journal,`I${r}`,`=IF(COUNTA(A${r}:G${r})=0,"",IF(J${r}<>"Да","Проверьте поля и код",IF(H${r}<0,"Отрицательный остаток","")))`);
  }
  errorFormatting(stock,'H8:H37','AND(ISNUMBER(H8),H8<0)');
  errorFormatting(stock,'I8:I37','I8<>""');
  errorFormatting(journal,`H8:H${lastRecord}`,'AND(ISNUMBER(H8),H8<0)');
  errorFormatting(journal,`I8:J${lastRecord}`,'AND(COUNTA($A8:$G8)>0,OR($J8="Нет",$H8<0))');
  val(stock,'A39','30 позиций. Сначала изучите лист Инструкция.');
  stock.getRange('A39').format.font={name:'Arial',size:11,color:muted,italic:true};
  // This tab has a distinct workflow: getting started, capacity and definitions.
  guide.getRange('A1:A28').format.columnWidthPx=45;
  guide.getRange('B1:B28').format.columnWidthPx=760;
  guide.getRange('C1:C28').format.columnWidthPx=25;
  guide.getRange('D1:D28').format.columnWidthPx=180;
  guide.getRange('B2').values=[['Кладовая мастерской: как пользоваться']];
  guide.getRange('B2').format.font={name:'Arial',size:17,bold:true,color:ink};
  const instructions=[
    [4,'Назначение: 30 позиций и 300 движений. Учёт расходников и возвратного инструмента на одной полке или в одной кладовой.'],
    [6,'1. На листе Остатки заполните жёлтые поля: название, единицу, количество на полке на дату начала и место хранения. Если ничего нет, введите 0.'],
    [8,'2. Коды W001-W030 уже заданы. Не меняйте их. В журнале выбирайте код из списка. Одному коду соответствует одна позиция и одна единица измерения.'],
    [10,'3. На листе Журнал добавляйте события по порядку в первую свободную строку. Дата, код, операция и положительное количество обязательны.'],
    [12,'Приход добавляет количество на полку. Выдача уменьшает. Возврат добавляет ранее выданное и физически возвращённое. Расходник после использования не возвращается.'],
    [14,'4. На полке = на начало + приход - выдача + возврат. В журнале показан остаток после каждой строки. Не сортируйте строки: их порядок задаёт последовательность событий.'],
    [16,'5. Красный остаток означает ошибку или выдачу сверх записанного наличия. Проверьте дату, количество и пропущенные движения. Возврат больше выдачи тоже требует проверки.'],
    [18,'6. Строка с В расчёте = Нет исключена из итогов. Проверьте обязательные поля, код и начальный остаток. Вставка из буфера может обходить проверку ввода.'],
    [20,'7. Не редактируйте расчётные столбцы. Для нового периода сохраните отдельную копию: в начальные остатки перенесите сверенное количество на полке. Вернувшееся из прошлого периода внесите как приход с примечанием.'],
    [22,'Количество допускает дробь (например 2,5 л). Данные храните в своих файлах. Шаблон не связан с приложением автоматически.'],
    [24,example?'Это учебный пример. Все названия, получатели и документы вымышлены. Начальный период: 01.10.2026.':'Это пустой шаблон. Начните с листа Остатки, затем внесите первую запись в Журнал.'],
    [26,'Источник: оригинальный шаблон Каптёрки ПРО. На странице комплекта есть печатные PDF и пример.'],
  ];
  for (const [r,text] of instructions) {
    val(guide,`B${r}`,text);
    guide.getRange(`B${r}`).format.wrapText=true;
    guide.getRange(`A${r}:D${r}`).format.rowHeight=r===20?66:48;
  }
  val(guide,'B28',source.url);
  guide.getRange('B28').format.font={name:'Arial',size:11,color:'#146C81',underline:'single'};
  guide.images.add({dataUrl:`data:image/png;base64,${qr.toString('base64')}`,anchor:{from:{row:3,col:3},extent:{widthPx:150,heightPx:150}}});
  val(guide,'D10','Страница комплекта');
  if (example) {
    stock.getRange('B8:D13').values=source.items.map(item=>[item[1],item[2],item[3]]);
    stock.getRange('J8:J13').values=source.items.map(item=>[item[8]]);
    journal.getRange('A8:G20').values=source.records.map(row=>{
      const [d,m,y]=row[0].split('.').map(Number);
      const excelDate=(Date.UTC(y,m-1,d)-Date.UTC(1899,11,30))/86400000;
      return [excelDate,...row.slice(1)];
    });
  }
  console.log('Data and formulas authored; recalculating');
  wb.recalculate();
  if (example) {
    for (let i=0;i<source.items.length;i++) check(stock,`H${8+i}`,source.items[i][7],`Остаток ${source.items[i][0]}`);
    check(journal,'H13',7,'Возврат сверла увеличивает полку');
    check(journal,'H16',2,'Возврат набора увеличивает полку');
    check(journal,'H17',5.5,'Дробная выдача');
    check(journal,'J20','Да','Последняя примерная строка включена');
    // Exercise input-driven recalculation and invalid/negative boundaries, then restore.
    val(journal,'D8',41);
    wb.recalculate();
    check(stock,'H8',53,'Изменение прихода пересчитывает остаток');
    val(journal,'D8',40);
    val(journal,'D9',20);
    wb.recalculate();
    check(journal,'H9',-10,'Выдача больше наличия');
    check(stock,'H9',-9,'Отрицательный итог');
    check(journal,'I9','Отрицательный остаток','Предупреждение об отрицательном остатке');
    val(journal,'D9',4);
    val(journal,'D8',0);
    wb.recalculate();
    check(journal,'J8','Нет','Нулевая операция исключена');
    check(stock,'H8',12,'Нулевая операция не попадает в итог');
    val(journal,'D8',40);
    val(journal,'B8','UNKNOWN');
    wb.recalculate();
    check(journal,'J8','Нет','Неизвестный код исключён');
    val(journal,'B8','W001');
    val(stock,'D8',null);
    wb.recalculate();
    check(stock,'I8','Укажите начальный остаток','Пустой начальный остаток');
    check(journal,'J8','Нет','Незаполненная позиция не учитывается');
    val(stock,'D8',0);
    wb.recalculate();
    check(stock,'H8',32,'Ноль как начальный остаток допустим');
    val(stock,'D8',20);
    journal.getRange(`A${lastRecord}:G${lastRecord}`).values=[[(Date.UTC(2026,9,6)-Date.UTC(1899,11,30))/86400000,'W001','Возврат',1,'Мастер Б','УЧ-014','Проверка ёмкости']];
    wb.recalculate();
    check(stock,'H8',53,'Строка 300 включена в диапазоны');
    journal.getRange(`A${lastRecord}:G${lastRecord}`).clear({applyTo:'contents'});
    val(journal,'D13',5);
    wb.recalculate();
    check(stock,'I9','Проверьте возврат','Возврат больше выдачи');
    val(journal,'D13',1);
    wb.recalculate();
    for (let i=0;i<source.items.length;i++) check(stock,`H${8+i}`,source.items[i][7],`Итог восстановлен ${source.items[i][0]}`);
  } else {
    check(stock,'H8','','Пустая позиция не показывает фиктивный ноль');
    check(journal,'H8','','Пустая строка журнала');
    check(journal,'J307','','Пустая последняя строка');
  }
  console.log('Checking formulas and rendering views');
  // Block writes infer numeric types, so reapply the date format after writes.
  journal.getRange(`A8:A${lastRecord}`).setNumberFormat('dd.mm.yyyy');
  const formulaErrors=await wb.inspect({kind:'match',searchTerm:'#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A|#NUM!|#NULL!|#SPILL!|#CALC!',options:{useRegex:true,maxResults:30},summary:'formula error scan',maxChars:3000});
  evidence[example?'exampleErrorScan':'blankErrorScan']=formulaErrors.ndjson;
  if (/#REF!|#DIV\/0!|#VALUE!|#NAME\?|#N\/A|#NUM!|#NULL!|#SPILL!|#CALC!/.test(formulaErrors.ndjson.replace(/"searchTerm"[^\n]+/g,''))) throw new Error('Formula error scan requires review: '+formulaErrors.ndjson);
  evidence[example?'exampleValues':'blankValues']=(await wb.inspect({kind:'table',range:'Остатки!A7:J13',include:'values,formulas',tableMaxRows:7,tableMaxCols:10,maxChars:8000})).ndjson;
  console.log('Exporting XLSX');
  const file=await SpreadsheetFile.exportXlsx(wb);
  const name=example?'workshop-example.xlsx':'workshop-blank.xlsx';
  await file.save(path.join(out,name));
  try { await fs.rename(path.join(out,`${name}.inspect.ndjson`),path.join(qa,`${name}.inspect.ndjson`)); } catch(error) { if(error.code!=='ENOENT') throw error; }
  for (const [sheet,range] of [['Остатки','A1:J15'],['Журнал','A1:J22'],['Инструкция','A1:D28']]) {
    console.log(`Rendering ${sheet}`);
    const png=await wb.render({sheetName:sheet,range,scale:1,format:'png'});
    await fs.writeFile(path.join(qa,`${example?'example':'blank'}-${sheet==='Остатки'?'stock':sheet==='Журнал'?'journal':'guide'}.png`),new Uint8Array(await png.arrayBuffer()));
  }
}
await fs.writeFile(path.join(qa,'workshop-xlsx-checks.json'),JSON.stringify(evidence,null,2));
console.log(JSON.stringify({xlsx:[path.join(out,'workshop-blank.xlsx'),path.join(out,'workshop-example.xlsx')],checks:evidence.tests.length}));
}
try { await buildAll(); }
catch (error) { console.error(error.message); console.error(error.stack); process.exitCode=1; }
