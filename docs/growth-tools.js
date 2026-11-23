/* Каптёрка PRO 4.0.0 — Интерактивный рост и вовлечение:
 * 1) Бесплатный онлайн-конструктор ведомостей МБ-7 / М-11 / Форма № 8 в .xlsx
 * 2) Подбор профиля 4.0.0 за 3 клика + калькулятор экономии времени
 * 3) Вирусный шеринг «Отправить напарнику / кладовщику»
 */
(function (scope) {
  'use strict';

  var FORM_TEMPLATES = {
    mb7: {
      id: 'mb7',
      tabLabel: 'МБ-7 · Выдача СИЗ и инструмента',
      docTitle: 'ВЕДОМОСТЬ УЧЁТА ВЫДАЧИ СПЕЦОДЕЖДЫ, СПЕЦОБУВИ И ПРЕДОХРАНИТЕЛЬНЫХ ПРИСПОСОБЛЕНИЙ',
      formCode: 'Типовая межотраслевая форма № МБ-7 · ОКУД 0320003',
      fileName: 'vedomost-mb7.xlsx',
      personColHeader: 'ФИО получателя / бригада',
      signColHeader: 'Расписка в получении',
      defaultOrg: 'ООО «РемСтройМастер»',
      defaultWarehouse: 'Кладовая № 1 (Мастерская)',
      defaultRows: [
        { name: 'Перчатки с полимерным покрытием', unit: 'пар', qty: '10', person: 'Бригада № 1 (Соколов А.И.)' },
        { name: 'Перфоратор SDS-Plus (дежурный)', unit: 'шт.', qty: '1', person: 'Егоров Д.В.' },
        { name: 'Очки защитные закрытые', unit: 'шт.', qty: '4', person: 'Участок сборки' }
      ],
      hint: 'Сформировано по форме МБ-7 · В Каптёрке PRO 4.0.0 ведомость МБ-7 собирается из проведённых выдач за 1 секунду.'
    },
    m11: {
      id: 'm11',
      tabLabel: 'М-11 · Накладная перемещения',
      docTitle: 'ТРЕБОВАНИЕ-НАКЛАДНАЯ НА ВНУТРЕННЕЕ ПЕРЕМЕЩЕНИЕ МАТЕРИАЛОВ',
      formCode: 'Типовая межотраслевая форма № М-11 · ОКУД 0315006',
      fileName: 'nakladnaya-m11.xlsx',
      personColHeader: 'Куда / через кого (получатель)',
      signColHeader: 'Отметка о приёмке',
      defaultOrg: 'Сервисный центр № 2',
      defaultWarehouse: 'Основной склад → Ремзона',
      defaultRows: [
        { name: 'Масло моторное синтетическое 5W-40', unit: 'л', qty: '20', person: 'Пост ТО-2 (Волков С.П.)' },
        { name: 'Диски отрезные по металлу 125 мм', unit: 'уп.', qty: '6', person: 'Слесарный цех' },
        { name: 'Комплект фильтров масляных', unit: 'шт.', qty: '8', person: 'Пост ТО-1' }
      ],
      hint: 'Сформировано по форме М-11 · В Каптёрке PRO 4.0.0 накладная М-11 / ТОРГ-13 выгружается в Excel без двойного списания.'
    },
    form8: {
      id: 'form8',
      tabLabel: 'Форма № 8 · Раздаточная ведомость',
      docTitle: 'РАЗДАТОЧНАЯ (СДАТОЧНАЯ) ВЕДОМОСТЬ МАТЕРИАЛЬНЫХ ЦЕННОСТЕЙ',
      formCode: 'Форма № 8 по Приказу МО РФ № 139 · ОКУД 6002203',
      fileName: 'forma-8-vedomost.xlsx',
      personColHeader: 'Получатель (подразделение / ФИО)',
      signColHeader: 'Подпись и дата',
      defaultOrg: 'Подразделение снабжения',
      defaultWarehouse: 'Склад имущества (КЗ)',
      defaultRows: [
        { name: 'Спальный мешок всесезонный', unit: 'шт.', qty: '6', person: '1-е отделение' },
        { name: 'Рукавицы утеплённые', unit: 'пар', qty: '12', person: '2-е отделение' },
        { name: 'Фонарь налобный аккумуляторный', unit: 'шт.', qty: '4', person: 'Дежурная смена' }
      ],
      hint: 'Сформировано по Форме № 8 · В Каптёрке PRO 4.0.0 Форма № 8 (ОКУД 6002203) и Книга № 18 строятся из журнала автоматически.'
    }
  };

  function stripInvalidXmlChars(text) {
    return String(text == null ? '' : text).replace(/[\x00-\x08\x0B\x0C\x0E-\x1F\uFFFE\uFFFF]/g, '');
  }

  function sanitizeExcelText(value) {
    var clean = stripInvalidXmlChars(value).trim();
    if (!clean) return '';
    if (/^[=+\-@\t\r\n]/.test(clean)) return "'" + clean;
    return clean;
  }

  function escapeXml(value) {
    return stripInvalidXmlChars(value)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&apos;');
  }

  function parsePositiveQty(value) {
    var normalized = String(value == null ? '' : value).trim().replace(',', '.');
    if (!/^\d{1,7}(?:\.\d{1,3})?$/.test(normalized)) {
      throw new Error('Количество должно быть положительным числом до 1 000 000 (до 3 знаков после запятой).');
    }
    var num = Number(normalized);
    if (!Number.isFinite(num) || num <= 0 || num > 1000000) {
      throw new Error('Количество должно быть больше 0 и не превышать 1 000 000.');
    }
    return num;
  }

  function validateBuilderRows(rows) {
    if (!Array.isArray(rows) || rows.length === 0) {
      throw new Error('Добавьте хотя бы одну позицию в ведомость.');
    }
    if (rows.length > 15) {
      throw new Error('Бесплатный веб-конструктор формирует до 15 строк за раз. Полный безлимит — в приложении Каптёрка PRO.');
    }
    return rows.map(function (row, idx) {
      var name = sanitizeExcelText(row && row.name);
      var unit = sanitizeExcelText((row && row.unit) || 'шт.');
      var person = sanitizeExcelText((row && row.person) || '');
      if (!name) throw new Error('Строка ' + (idx + 1) + ': укажите наименование имущества.');
      if (!unit) throw new Error('Строка ' + (idx + 1) + ': укажите единицу измерения.');
      var qty = parsePositiveQty(row && row.qty);
      return {
        index: idx + 1,
        name: name.slice(0, 120),
        unit: unit.slice(0, 20),
        qty: qty,
        person: person.slice(0, 80)
      };
    });
  }

  function summarizeUnits(validRows) {
    var order = [];
    var sums = Object.create(null);
    validRows.forEach(function (row) {
      if (!(row.unit in sums)) {
        sums[row.unit] = 0;
        order.push(row.unit);
      }
      sums[row.unit] = Math.round((sums[row.unit] + row.qty) * 1000) / 1000;
    });
    return order.map(function (u) {
      return String(sums[u]).replace('.', ',') + ' ' + u;
    }).join(' · ');
  }

  /* Compact UTF-8 encoder & ZIP Store packager (Zero external deps) */
  var CRC_TABLE = (function () {
    var table = new Uint32Array(256);
    for (var n = 0; n < 256; n++) {
      var c = n;
      for (var k = 0; k < 8; k++) {
        c = (c & 1) ? (0xEDB88320 ^ (c >>> 1)) : (c >>> 1);
      }
      table[n] = c >>> 0;
    }
    return table;
  })();

  function crc32(bytes) {
    var c = 0xFFFFFFFF;
    for (var i = 0; i < bytes.length; i++) {
      c = CRC_TABLE[(c ^ bytes[i]) & 0xFF] ^ (c >>> 8);
    }
    return (c ^ 0xFFFFFFFF) >>> 0;
  }

  function encodeUtf8(str) {
    if (typeof TextEncoder !== 'undefined') {
      return new TextEncoder().encode(str);
    }
    if (typeof Buffer !== 'undefined') {
      return new Uint8Array(Buffer.from(str, 'utf8'));
    }
    var out = [];
    for (var i = 0; i < str.length; i++) {
      var code = str.charCodeAt(i);
      if (code < 0x80) out.push(code);
      else if (code < 0x800) out.push(0xC0 | (code >> 6), 0x80 | (code & 0x3F));
      else if (code < 0xD800 || code >= 0xE000) out.push(0xE0 | (code >> 12), 0x80 | ((code >> 6) & 0x3F), 0x80 | (code & 0x3F));
      else {
        i++;
        code = 0x10000 + (((code & 0x3FF) << 10) | (str.charCodeAt(i) & 0x3FF));
        out.push(0xF0 | (code >> 18), 0x80 | ((code >> 12) & 0x3F), 0x80 | ((code >> 6) & 0x3F), 0x80 | (code & 0x3F));
      }
    }
    return new Uint8Array(out);
  }

  function packZipStore(entries) {
    var parts = [];
    var central = [];
    var offset = 0;

    entries.forEach(function (entry) {
      var nameBytes = encodeUtf8(entry.name);
      var data = typeof entry.data === 'string' ? encodeUtf8(entry.data) : entry.data;
      var crc = crc32(data);

      var lfh = new Uint8Array(30 + nameBytes.length);
      var lv = new DataView(lfh.buffer);
      lv.setUint32(0, 0x04034B50, true);
      lv.setUint16(4, 20, true);
      lv.setUint16(6, 0x0800, true); // UTF-8 filename flag
      lv.setUint16(8, 0, true); // Store
      lv.setUint16(10, 0, true);
      lv.setUint16(12, 0x5421, true);
      lv.setUint32(14, crc, true);
      lv.setUint32(18, data.length, true);
      lv.setUint32(22, data.length, true);
      lv.setUint16(26, nameBytes.length, true);
      lv.setUint16(28, 0, true);
      lfh.set(nameBytes, 30);

      parts.push(lfh, data);

      var cdh = new Uint8Array(46 + nameBytes.length);
      var cv = new DataView(cdh.buffer);
      cv.setUint32(0, 0x02014B50, true);
      cv.setUint16(4, 20, true);
      cv.setUint16(6, 20, true);
      cv.setUint16(8, 0x0800, true);
      cv.setUint16(10, 0, true);
      cv.setUint16(12, 0, true);
      cv.setUint16(14, 0x5421, true);
      cv.setUint32(16, crc, true);
      cv.setUint32(20, data.length, true);
      cv.setUint32(24, data.length, true);
      cv.setUint16(28, nameBytes.length, true);
      cv.setUint16(30, 0, true);
      cv.setUint16(32, 0, true);
      cv.setUint16(34, 0, true);
      cv.setUint16(36, 0, true);
      cv.setUint32(38, 0, true);
      cv.setUint32(42, offset, true);
      cdh.set(nameBytes, 46);
      central.push(cdh);

      offset += lfh.length + data.length;
    });

    var cdSize = central.reduce(function (s, b) { return s + b.length; }, 0);
    var eocd = new Uint8Array(22);
    var ev = new DataView(eocd.buffer);
    ev.setUint32(0, 0x06054B50, true);
    ev.setUint16(4, 0, true);
    ev.setUint16(6, 0, true);
    ev.setUint16(8, entries.length, true);
    ev.setUint16(10, entries.length, true);
    ev.setUint32(12, cdSize, true);
    ev.setUint32(16, offset, true);
    ev.setUint16(20, 0, true);

    var totalLength = offset + cdSize + eocd.length;
    var zip = new Uint8Array(totalLength);
    var cursor = 0;
    parts.forEach(function (chunk) { zip.set(chunk, cursor); cursor += chunk.length; });
    central.forEach(function (chunk) { zip.set(chunk, cursor); cursor += chunk.length; });
    zip.set(eocd, cursor);
    return zip;
  }

  function xmlTextCell(ref, styleIdx, text) {
    return '<c r="' + ref + '" t="inlineStr" s="' + styleIdx + '"><is><t xml:space="preserve">' + escapeXml(text) + '</t></is></c>';
  }

  function xmlNumCell(ref, styleIdx, num) {
    return '<c r="' + ref + '" s="' + styleIdx + '"><v>' + String(num) + '</v></c>';
  }

  function buildFormXlsxBytes(payload) {
    var tpl = FORM_TEMPLATES[(payload && payload.formType) || 'mb7'] || FORM_TEMPLATES.mb7;
    var validRows = validateBuilderRows(payload && payload.rows);
    var orgName = sanitizeExcelText((payload && payload.orgName) || tpl.defaultOrg).slice(0, 100);
    var warehouseName = sanitizeExcelText((payload && payload.warehouseName) || tpl.defaultWarehouse).slice(0, 100);
    var docNumber = sanitizeExcelText((payload && payload.docNumber) || '001').slice(0, 24);
    var today = new Date();
    var fallbackDate = ('0' + today.getDate()).slice(-2) + '.' + ('0' + (today.getMonth() + 1)).slice(-2) + '.' + today.getFullYear();
    var docDate = sanitizeExcelText((payload && payload.docDate) || fallbackDate).slice(0, 24);
    var unitSummary = summarizeUnits(validRows);

    var sheetRows = [];
    sheetRows.push('<row r="1" ht="24" customHeight="1">' + xmlTextCell('A1', 1, tpl.docTitle) + '</row>');
    sheetRows.push('<row r="2" ht="18" customHeight="1">' + xmlTextCell('A2', 6, tpl.formCode + '   |   Документ № ' + docNumber + ' от ' + docDate) + '</row>');
    sheetRows.push('<row r="3" ht="18" customHeight="1">' + xmlTextCell('A3', 0, 'Организация: ' + orgName + '   |   Склад / объект: ' + warehouseName) + '</row>');

    var headerRow = 5;
    sheetRows.push(
      '<row r="' + headerRow + '" ht="26" customHeight="1">' +
      xmlTextCell('A' + headerRow, 2, '№') +
      xmlTextCell('B' + headerRow, 2, 'Наименование имущества / материала') +
      xmlTextCell('C' + headerRow, 2, 'Ед. изм.') +
      xmlTextCell('D' + headerRow, 2, 'Кол-во') +
      xmlTextCell('E' + headerRow, 2, tpl.personColHeader) +
      xmlTextCell('F' + headerRow, 2, tpl.signColHeader) +
      '</row>'
    );

    validRows.forEach(function (item, i) {
      var r = headerRow + 1 + i;
      sheetRows.push(
        '<row r="' + r + '" ht="22" customHeight="1">' +
        xmlNumCell('A' + r, 4, item.index) +
        xmlTextCell('B' + r, 3, item.name) +
        xmlTextCell('C' + r, 3, item.unit) +
        xmlNumCell('D' + r, 4, item.qty) +
        xmlTextCell('E' + r, 3, item.person || '—') +
        xmlTextCell('F' + r, 3, '') +
        '</row>'
      );
    });

    var totalRow = headerRow + 1 + validRows.length;
    sheetRows.push(
      '<row r="' + totalRow + '" ht="22" customHeight="1">' +
      xmlTextCell('A' + totalRow, 5, '') +
      xmlTextCell('B' + totalRow, 5, 'Итого позиций: ' + validRows.length + ' · Раздельный баланс: ' + unitSummary) +
      xmlTextCell('C' + totalRow, 5, '') +
      xmlTextCell('D' + totalRow, 5, '') +
      xmlTextCell('E' + totalRow, 5, '') +
      xmlTextCell('F' + totalRow, 5, '') +
      '</row>'
    );

    var signRow = totalRow + 2;
    sheetRows.push(
      '<row r="' + signRow + '" ht="20" customHeight="1">' +
      xmlTextCell('A' + signRow, 0, 'Выдал / отпустил: ______________   Принял: ______________   Проверил: ______________') +
      '</row>'
    );
    var footRow = signRow + 2;
    sheetRows.push(
      '<row r="' + footRow + '" ht="18" customHeight="1">' +
      xmlTextCell('A' + footRow, 6, 'Сформировано бесплатно на kapterka-pro.ru • Автоматический учёт, QR-синхронизация и выгрузка без лимитов на Android — Каптёрка PRO 4.0') +
      '</row>'
    );

    var contentTypes =
      '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>' +
      '<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">' +
      '<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>' +
      '<Default Extension="xml" ContentType="application/xml"/>' +
      '<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>' +
      '<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>' +
      '<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>' +
      '</Types>';

    var rootRels =
      '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>' +
      '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">' +
      '<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>' +
      '</Relationships>';

    var workbookXml =
      '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>' +
      '<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">' +
      '<sheets><sheet name="Ведомость" sheetId="1" r:id="rId1"/></sheets>' +
      '</workbook>';

    var workbookRels =
      '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>' +
      '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">' +
      '<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>' +
      '<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>' +
      '</Relationships>';

    var stylesXml =
      '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>' +
      '<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">' +
      '<fonts count="5">' +
      '<font><sz val="10"/><color rgb="FF111827"/><name val="Arial"/></font>' +
      '<font><b/><sz val="12"/><color rgb="FF064E3B"/><name val="Arial"/></font>' +
      '<font><b/><sz val="10"/><color rgb="FF064E3B"/><name val="Arial"/></font>' +
      '<font><b/><sz val="10"/><color rgb="FF111827"/><name val="Arial"/></font>' +
      '<font><i/><sz val="9"/><color rgb="FF4B5563"/><name val="Arial"/></font>' +
      '</fonts>' +
      '<fills count="4">' +
      '<fill><patternFill patternType="none"/></fill>' +
      '<fill><patternFill patternType="gray125"/></fill>' +
      '<fill><patternFill patternType="solid"><fgColor rgb="FFE6F4EF"/><bgColor indexed="64"/></patternFill></fill>' +
      '<fill><patternFill patternType="solid"><fgColor rgb="FFF1F5F9"/><bgColor indexed="64"/></patternFill></fill>' +
      '</fills>' +
      '<borders count="2">' +
      '<border><left/><right/><top/><bottom/><diagonal/></border>' +
      '<border><left style="thin"><color rgb="FF94A3B8"/></left><right style="thin"><color rgb="FF94A3B8"/></right><top style="thin"><color rgb="FF94A3B8"/></top><bottom style="thin"><color rgb="FF94A3B8"/></bottom><diagonal/></border>' +
      '</borders>' +
      '<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>' +
      '<cellXfs count="7">' +
      '<xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>' +
      '<xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>' +
      '<xf numFmtId="0" fontId="2" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>' +
      '<xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyFont="1" applyBorder="1" applyAlignment="1"><alignment vertical="center" wrapText="1"/></xf>' +
      '<xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyFont="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>' +
      '<xf numFmtId="0" fontId="3" fillId="3" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment vertical="center"/></xf>' +
      '<xf numFmtId="0" fontId="4" fillId="0" borderId="0" xfId="0" applyFont="1"/>' +
      '</cellXfs>' +
      '<cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>' +
      '</styleSheet>';

    var sheetXml =
      '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>' +
      '<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">' +
      '<sheetViews><sheetView workbookViewId="0"/></sheetViews>' +
      '<sheetFormatPr defaultRowHeight="18"/>' +
      '<cols>' +
      '<col min="1" max="1" width="6" customWidth="1"/>' +
      '<col min="2" max="2" width="38" customWidth="1"/>' +
      '<col min="3" max="3" width="11" customWidth="1"/>' +
      '<col min="4" max="4" width="13" customWidth="1"/>' +
      '<col min="5" max="5" width="28" customWidth="1"/>' +
      '<col min="6" max="6" width="22" customWidth="1"/>' +
      '</cols>' +
      '<sheetData>' + sheetRows.join('') + '</sheetData>' +
      '<pageMargins left="0.5" right="0.5" top="0.5" bottom="0.5" header="0.3" footer="0.3"/>' +
      '<pageSetup orientation="landscape" paperSize="9"/>' +
      '</worksheet>';

    var bytes = packZipStore([
      { name: '[Content_Types].xml', data: contentTypes },
      { name: '_rels/.rels', data: rootRels },
      { name: 'xl/workbook.xml', data: workbookXml },
      { name: 'xl/_rels/workbook.xml.rels', data: workbookRels },
      { name: 'xl/styles.xml', data: stylesXml },
      { name: 'xl/worksheets/sheet1.xml', data: sheetXml }
    ]);

    return {
      bytes: bytes,
      fileName: tpl.fileName,
      unitSummary: unitSummary,
      rowCount: validRows.length
    };
  }

  /* Recommendation Quiz Engine */
  function recommendSetup(input) {
    var sphere = (input && input.sphere) || 'workshop';
    var team = (input && input.team) || 'solo';
    var weeklyOps = Number((input && input.weeklyOps) || 60);
    if (!Number.isFinite(weeklyOps) || weeklyOps < 1) weeklyOps = 25;

    var personaIndex = 0;
    var profileName = 'Гражданский склад / Мастерская (64 позиции в 8 категориях)';
    var forms = 'М-11 / ТОРГ-13, ведомость МБ-7, акт МБ-8 и оборотная ТМЦ в Excel';
    var guideHref = 'uchet-zapchastey-v-avtoservise-android.html';
    var guideLabel = 'Сценарий для мастерской и СТО →';

    if (sphere === 'military') {
      personaIndex = team === 'team' ? 3 : 1;
      profileName = 'Воинский учёт (службы РАВ, БПЛА, Связь, Вещевая, ГСМ, Мед)';
      forms = 'Форма № 8 (ОКУД 6002203, Приказ МО РФ № 139) и Книга учёта Форма № 18';
      guideHref = 'programma-dlya-starshiny-roty.html';
      guideLabel = 'Сценарий для старшины подразделения →';
    } else if (sphere === 'civil') {
      personaIndex = team === 'team' ? 3 : 2;
      profileName = team === 'team' ? 'Командный контур + Гражданский или Пустой склад' : 'Гражданский склад (64 поз.) или Пустой склад с нуля';
      forms = 'Накладная М-11, ведомость СИЗ МБ-7 и акт списания МБ-8 в Excel';
      guideHref = 'nakladnaya-m-11-na-telefone.html';
      guideLabel = 'Как выгружать М-11 за минуту →';
    } else if (team === 'team') {
      personaIndex = 3;
      profileName = 'Гражданский склад (64 поз.) + Подключение второго телефона по QR';
    }

    var hoursSaved = Math.max(2, Math.round((weeklyOps * 4.3 * 2.5) / 60));
    var tierText = (team === 'solo' && weeklyOps <= 25)
      ? '3 дня полного демо, далее хватит бесплатного Базового режима (1 склад, до 15 позиций)'
      : (weeklyOps >= 150 ? '3 дня демо → PRO 90 или 365 дн. (выгода до −41%, безлимит и Excel)' : '3 дня демо → PRO 30 дней за 490 ₽ или Базовый режим навсегда');

    return {
      personaIndex: personaIndex,
      profileName: profileName,
      forms: forms,
      tierText: tierText,
      hoursSaved: hoursSaved,
      syncNote: team === 'team' ? 'Живые заявки Новая → Выдана и QR-связь за 5 секунд' : 'Полная локальная работа в базе Room / SQLite без интернета',
      guideHref: guideHref,
      guideLabel: guideLabel
    };
  }

  var api = {
    FORM_TEMPLATES: FORM_TEMPLATES,
    sanitizeExcelText: sanitizeExcelText,
    escapeXml: escapeXml,
    validateBuilderRows: validateBuilderRows,
    summarizeUnits: summarizeUnits,
    buildFormXlsxBytes: buildFormXlsxBytes,
    recommendSetup: recommendSetup
  };

  if (typeof module !== 'undefined' && module.exports) {
    module.exports = api;
  }

  if (!scope.document) return;

  function safeTrack(name, params) {
    if (typeof scope.trackSiteAction === 'function') {
      try { scope.trackSiteAction(name, params || {}); } catch (_) {}
    }
  }

  function initProfileQuiz() {
    var box = document.getElementById('profile-quiz');
    if (!box) return;
    var state = { sphere: 'workshop', team: 'solo', weeklyOps: 60 };
    var profEl = document.getElementById('quizResProfile');
    var formsEl = document.getElementById('quizResForms');
    var tierEl = document.getElementById('quizResTier');
    var hoursEl = document.getElementById('quizResHours');
    var jumpBtn = document.getElementById('quizJumpPersonaBtn');
    var guideLink = document.getElementById('quizGuideLink');

    function renderQuiz() {
      var res = recommendSetup(state);
      if (profEl) profEl.textContent = res.profileName;
      if (formsEl) formsEl.textContent = res.forms + ' · ' + res.syncNote;
      if (tierEl) tierEl.textContent = res.tierText;
      if (hoursEl) hoursEl.textContent = '≈ ' + res.hoursSaved + ' ч / мес.';
      if (jumpBtn) jumpBtn.setAttribute('data-target-persona', String(res.personaIndex));
      if (guideLink) {
        guideLink.setAttribute('href', res.guideHref);
        guideLink.textContent = res.guideLabel;
      }
    }

    box.querySelectorAll('[data-quiz-group]').forEach(function (btn) {
      btn.addEventListener('click', function () {
        var group = btn.getAttribute('data-quiz-group');
        var val = btn.getAttribute('data-quiz-value');
        if (!group || !val) return;
        state[group] = group === 'weeklyOps' ? Number(val) : val;
        box.querySelectorAll('[data-quiz-group="' + group + '"]').forEach(function (peer) {
          var on = peer === btn;
          peer.classList.toggle('active', on);
          peer.setAttribute('aria-pressed', on ? 'true' : 'false');
        });
        renderQuiz();
        safeTrack('profile_quiz_select', { step: group });
      });
    });

    if (jumpBtn) {
      jumpBtn.addEventListener('click', function () {
        var idx = jumpBtn.getAttribute('data-target-persona') || '0';
        var tab = document.querySelector('.lp-persona-tabs button[data-persona="' + idx + '"]');
        if (tab) {
          tab.click();
          var card = document.querySelector('.lp-persona-card');
          if (card && card.scrollIntoView) card.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }
      });
    }

    renderQuiz();
  }

  function initBlankBuilder() {
    var section = document.getElementById('blank-builder');
    if (!section) return;
    var rowsHost = document.getElementById('builderRowsHost');
    var addRowBtn = document.getElementById('builderAddRowBtn');
    var resetBtn = document.getElementById('builderResetBtn');
    var downloadBtn = document.getElementById('builderDownloadBtn');
    var orgInput = document.getElementById('builderOrgInput');
    var warehouseInput = document.getElementById('builderWarehouseInput');
    var statusEl = document.getElementById('builderStatus');
    var docMetaEl = document.getElementById('builderDocMeta');
    var unitSummaryEl = document.getElementById('builderUnitSummary');
    var typeBtns = section.querySelectorAll('[data-builder-form]');
    var currentForm = 'mb7';

    function createRowDom(data) {
      var row = document.createElement('div');
      row.className = 'k3d-b-row';

      var fields = [
        { key: 'name', label: 'Наименование', placeholder: 'Например: Перчатки', max: 120, val: (data && data.name) || '' },
        { key: 'unit', label: 'Ед. изм.', placeholder: 'шт.', max: 20, val: (data && data.unit) || 'шт.' },
        { key: 'qty', label: 'Кол-во', placeholder: '1', max: 12, val: (data && data.qty) || '1' },
        { key: 'person', label: 'Получатель / куда', placeholder: 'ФИО или участок', max: 80, val: (data && data.person) || '' }
      ];

      fields.forEach(function (f) {
        var wrap = document.createElement('label');
        wrap.className = 'k3d-b-field k3d-b-field-' + f.key;
        var cap = document.createElement('span');
        cap.textContent = f.label;
        var inp = document.createElement('input');
        inp.type = 'text';
        inp.setAttribute('data-builder-field', f.key);
        inp.placeholder = f.placeholder;
        inp.maxLength = f.max;
        inp.autocomplete = 'off';
        if (f.key === 'qty') inp.inputMode = 'decimal';
        inp.value = f.val;
        inp.addEventListener('input', refreshSummary);
        wrap.appendChild(cap);
        wrap.appendChild(inp);
        row.appendChild(wrap);
      });

      var del = document.createElement('button');
      del.type = 'button';
      del.className = 'k3d-b-del';
      del.textContent = '×';
      del.setAttribute('aria-label', 'Удалить строку');
      del.addEventListener('click', function () {
        if (rowsHost.children.length <= 1) return;
        row.remove();
        refreshSummary();
      });
      row.appendChild(del);
      return row;
    }

    function readDomRows() {
      return Array.prototype.map.call(rowsHost.children, function (row) {
        var get = function (k) {
          var el = row.querySelector('[data-builder-field="' + k + '"]');
          return el ? el.value : '';
        };
        return { name: get('name'), unit: get('unit'), qty: get('qty'), person: get('person') };
      });
    }

    function refreshSummary() {
      var tpl = FORM_TEMPLATES[currentForm] || FORM_TEMPLATES.mb7;
      if (docMetaEl) docMetaEl.textContent = tpl.formCode;
      if (addRowBtn) addRowBtn.disabled = rowsHost.children.length >= 15;
      try {
        var valid = validateBuilderRows(readDomRows());
        if (unitSummaryEl) {
          unitSummaryEl.textContent = 'Позиций: ' + valid.length + ' · Итого (раздельно): ' + summarizeUnits(valid);
        }
        if (statusEl && statusEl.dataset.state === 'error') {
          statusEl.textContent = tpl.hint;
          statusEl.dataset.state = '';
        }
      } catch (err) {
        if (unitSummaryEl) unitSummaryEl.textContent = 'Позиций: ' + rowsHost.children.length + ' · Проверьте заполнение';
      }
    }

    function loadTemplate(formKey) {
      var tpl = FORM_TEMPLATES[formKey] || FORM_TEMPLATES.mb7;
      currentForm = tpl.id;
      typeBtns.forEach(function (btn) {
        var on = btn.getAttribute('data-builder-form') === currentForm;
        btn.classList.toggle('active', on);
        btn.setAttribute('aria-pressed', on ? 'true' : 'false');
      });
      if (orgInput) orgInput.value = tpl.defaultOrg;
      if (warehouseInput) warehouseInput.value = tpl.defaultWarehouse;
      rowsHost.replaceChildren();
      tpl.defaultRows.forEach(function (r) {
        rowsHost.appendChild(createRowDom(r));
      });
      if (statusEl) {
        statusEl.textContent = tpl.hint;
        statusEl.dataset.state = '';
      }
      refreshSummary();
    }

    typeBtns.forEach(function (btn) {
      btn.addEventListener('click', function () {
        loadTemplate(btn.getAttribute('data-builder-form'));
      });
    });

    if (addRowBtn) {
      addRowBtn.addEventListener('click', function () {
        if (rowsHost.children.length >= 15) return;
        var node = createRowDom({ name: '', unit: 'шт.', qty: '1', person: '' });
        rowsHost.appendChild(node);
        refreshSummary();
        var first = node.querySelector('input');
        if (first) first.focus();
      });
    }

    if (resetBtn) {
      resetBtn.addEventListener('click', function () {
        loadTemplate(currentForm);
      });
    }

    if (downloadBtn) {
      downloadBtn.addEventListener('click', function () {
        try {
          var out = buildFormXlsxBytes({
            formType: currentForm,
            orgName: orgInput ? orgInput.value : '',
            warehouseName: warehouseInput ? warehouseInput.value : '',
            rows: readDomRows()
          });
          var blob = new Blob([out.bytes], {
            type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
          });
          var url = URL.createObjectURL(blob);
          var link = document.createElement('a');
          link.href = url;
          link.download = out.fileName;
          document.body.appendChild(link);
          link.click();
          document.body.removeChild(link);
          setTimeout(function () { URL.revokeObjectURL(url); }, 1500);
          if (statusEl) {
            statusEl.textContent = 'Файл ' + out.fileName + ' скачан (' + out.rowCount + ' поз., ' + out.unitSummary + ').';
            statusEl.dataset.state = 'ok';
          }
          safeTrack('xlsx_builder_download', { form: currentForm });
        } catch (err) {
          if (statusEl) {
            statusEl.textContent = err && err.message ? err.message : 'Проверьте заполненные строки.';
            statusEl.dataset.state = 'error';
          }
        }
      });
    }

    loadTemplate('mb7');
  }

  function initTeamShare() {
    var shareUrl = 'https://kapterka-pro.ru/?utm_source=referral&utm_medium=share&utm_campaign=team_qr_400';
    var shareTitle = 'Каптёрка PRO 4.0 — общий склад на Android по QR';
    var shareText = 'Удобный складской учёт на Android (офлайн + заявки по QR + Excel М-11, МБ-7, Формы 8 и 18). Ставь приложение — подключу к складу по QR за 5 секунд:';

    document.querySelectorAll('[data-team-share]').forEach(function (btn) {
      btn.addEventListener('click', async function () {
        var box = btn.closest('.k3d-share-bar') || btn.parentElement;
        var note = box ? box.querySelector('[data-team-share-status]') : null;
        if (navigator.share) {
          try {
            safeTrack('team_share_click', { channel: 'web_share' });
            await navigator.share({ title: shareTitle, text: shareText, url: shareUrl });
            if (note) note.textContent = 'Окно отправки открыто — выберите контакт напарника.';
            return;
          } catch (_) {}
        }
        if (navigator.clipboard && navigator.clipboard.writeText) {
          try {
            await navigator.clipboard.writeText(shareUrl);
            safeTrack('team_share_click', { channel: 'clipboard' });
            if (note) note.textContent = 'Ссылка скопирована — отправьте её напарнику в Telegram или WhatsApp.';
            return;
          } catch (_) {}
        }
        if (note) note.textContent = 'Скопируйте ссылку: ' + shareUrl;
      });
    });
  }

  function bootstrapGrowthTools() {
    initProfileQuiz();
    initBlankBuilder();
    initTeamShare();
  }

  if (typeof scope.requestIdleCallback === 'function') {
    scope.requestIdleCallback(bootstrapGrowthTools, { timeout: 1200 });
  } else {
    scope.setTimeout(bootstrapGrowthTools, 60);
  }
})(typeof window !== 'undefined' ? window : globalThis);
