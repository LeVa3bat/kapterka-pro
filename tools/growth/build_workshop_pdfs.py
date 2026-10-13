"""Build printable workshop kit. Uses fictitious examples and bundled ReportLab.

Run with the bundled Python: build_workshop_pdfs.py <repository>.
The XLSX builder uses the QR PNG created here.
"""
from pathlib import Path
from datetime import date
import json
import sys

from reportlab.pdfgen import canvas
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4, landscape
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import Table, TableStyle, Paragraph
from reportlab.lib.styles import ParagraphStyle
from reportlab.graphics.barcode.qr import QrCodeWidget
from reportlab.graphics import renderPDF
from reportlab.graphics.shapes import Drawing
from PIL import Image, ImageDraw

ROOT = Path(sys.argv[1]).resolve()
OUT = ROOT / 'docs' / 'downloads'
QA = ROOT / 'growth' / 'qa'
OUT.mkdir(parents=True, exist_ok=True)
QA.mkdir(parents=True, exist_ok=True)
URL = 'https://kapterka-pro.ru/uchet-instrumenta-android.html#workshop-kit'
FONT_DIR = Path('C:/Windows/Fonts')
pdfmetrics.registerFont(TTFont('KitArial', str(FONT_DIR / 'arial.ttf')))
pdfmetrics.registerFont(TTFont('KitArialBold', str(FONT_DIR / 'arialbd.ttf')))
INK = colors.HexColor('#173039')
MUTED = colors.HexColor('#52646b')
RULE = colors.HexColor('#b9c8cc')
PAPER = colors.HexColor('#edf3f3')
STYLE = ParagraphStyle('cell', fontName='KitArial', fontSize=9, leading=11, textColor=INK)
HEAD = ParagraphStyle('head', parent=STYLE, fontName='KitArialBold', fontSize=9)
SMALL = ParagraphStyle('small', parent=STYLE, fontSize=8.5, leading=11)
ITEMS = [
    ['W001', 'Перчатки нитриловые', 'пар', 20, 40, 8, 0, 52, 'Стеллаж А'],
    ['W002', 'Сверло по металлу 6 мм', 'шт', 10, 0, 4, 1, 7, 'Стеллаж А'],
    ['W003', 'Лента малярная', 'рул', 6, 4, 3, 0, 7, 'Стеллаж Б'],
    ['W004', 'Набор шестигранников', 'набор', 2, 0, 1, 1, 2, 'Стеллаж Б'],
    ['W005', 'Очиститель', 'л', 5, 3, 2.5, 0, 5.5, 'Шкаф В'],
    ['W006', 'Круг шлифовальный', 'шт', 30, 20, 12, 2, 40, 'Шкаф В'],
]
RECORDS = [
    ['01.10.2026', 'W001', 'Приход', 40, 'Учебный поставщик', 'УЧ-001', 'Новая упаковка'],
    ['01.10.2026', 'W002', 'Выдача', 4, 'Мастер А', 'УЧ-002', 'Для рабочего места'],
    ['02.10.2026', 'W001', 'Выдача', 8, 'Мастер Б', 'УЧ-003', 'Для смены'],
    ['02.10.2026', 'W003', 'Приход', 4, 'Учебный поставщик', 'УЧ-004', 'Пополнение'],
    ['02.10.2026', 'W004', 'Выдача', 1, 'Мастер А', 'УЧ-005', 'Для работы'],
    ['03.10.2026', 'W002', 'Возврат', 1, 'Мастер А', 'УЧ-006', 'Не использовано'],
    ['03.10.2026', 'W003', 'Выдача', 3, 'Мастер Б', 'УЧ-007', 'Для работ'],
    ['03.10.2026', 'W005', 'Приход', 3, 'Учебный поставщик', 'УЧ-008', 'Пополнение'],
    ['04.10.2026', 'W004', 'Возврат', 1, 'Мастер А', 'УЧ-009', 'Вернули набор'],
    ['04.10.2026', 'W005', 'Выдача', 2.5, 'Мастер Б', 'УЧ-010', 'Для очистки'],
    ['04.10.2026', 'W006', 'Приход', 20, 'Учебный поставщик', 'УЧ-011', 'Пополнение'],
    ['05.10.2026', 'W006', 'Выдача', 12, 'Мастер А', 'УЧ-012', 'Для работ'],
    ['05.10.2026', 'W006', 'Возврат', 2, 'Мастер А', 'УЧ-013', 'Не использовано'],
]


def number(value):
    if isinstance(value, (int, float)):
        return ('%g' % value).replace('.', ',')
    return str(value)


def qr_drawing(size=25 * mm):
    widget = QrCodeWidget(URL, barLevel='M')
    x0, y0, x1, y1 = widget.getBounds()
    drawing = Drawing(size, size, transform=[size / (x1-x0), 0, 0, size / (y1-y0), 0, 0])
    drawing.add(widget)
    return drawing


def export_qr():
    # ReportLab supplies the encoded modules. Draw those modules at integer pixels
    # so the XLSX image has a crisp four-module quiet zone without a raster backend.
    widget = QrCodeWidget(URL, barLevel='M')
    widget.qr.make()
    modules = widget.qr.modules
    scale = 12
    count = len(modules)
    image = Image.new('RGB', ((count+8)*scale, (count+8)*scale), 'white')
    draw = ImageDraw.Draw(image)
    for row in range(count):
        for col in range(count):
            if modules[row][col]:
                left, top = (col+4)*scale, (row+4)*scale
                draw.rectangle((left, top, left+scale-1, top+scale-1), fill='black')
    image.save(QA / 'workshop-source-qr.png')


def paragraph(text, style=STYLE):
    return Paragraph(str(text), style)


def table(c, headers, rows, widths, top, row_height):
    data = [[paragraph(x, HEAD) for x in headers]]
    data += [[paragraph(number(x)) for x in row] for row in rows]
    heights = [12*mm] + [row_height] * len(rows)
    tbl = Table(data, colWidths=widths, rowHeights=heights)
    tbl.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), PAPER),
        ('VALIGN', (0,0), (-1,-1), 'MIDDLE'),
        ('LEFTPADDING', (0,0), (-1,-1), 5),
        ('RIGHTPADDING', (0,0), (-1,-1), 5),
        ('TOPPADDING', (0,0), (-1,-1), 3),
        ('BOTTOMPADDING', (0,0), (-1,-1), 3),
        ('GRID', (0,0), (-1,-1), 0.45, RULE),
    ]))
    _, height = tbl.wrap(1000, 1000)
    tbl.drawOn(c, 14*mm, top-height)
    return top-height


def header(c, title, subtitle, page, pages):
    width, height = landscape(A4)
    c.setFillColor(INK)
    c.setFont('KitArialBold', 19)
    c.drawString(14*mm, height-19*mm, title)
    c.setFillColor(MUTED)
    c.setFont('KitArial', 10)
    c.drawString(14*mm, height-26*mm, subtitle)
    c.setStrokeColor(RULE)
    c.line(14*mm, height-30*mm, width-14*mm, height-30*mm)
    c.setFont('KitArial', 8)
    c.drawRightString(width-14*mm, 9*mm, f'{page} / {pages}')


def footer(c, lines):
    y = 39*mm
    for line in lines:
        p = paragraph(line, SMALL)
        _, h = p.wrap(231*mm, 30*mm)
        p.drawOn(c, 14*mm, y-h)
        y -= h + 2*mm
    c.setFont('KitArial', 8)
    c.setFillColor(MUTED)
    c.drawString(14*mm, 13*mm, 'Источник: Каптёрка ПРО. Шаблон для кладовой мастерской.')
    c.drawString(14*mm, 9*mm, 'kapterka-pro.ru/uchet-instrumenta-android.html#workshop-kit')
    c.linkURL(URL, (14*mm, 7*mm, 220*mm, 15*mm), relative=0)
    renderPDF.draw(qr_drawing(), c, 257*mm, 16*mm)


def build_workshop(example):
    filename = 'workshop-example.pdf' if example else 'workshop-blank.pdf'
    c = canvas.Canvas(str(OUT / filename), pagesize=landscape(A4))
    c.setTitle('Кладовая мастерской - пример' if example else 'Кладовая мастерской - пустой шаблон')
    c.setAuthor('Каптёрка ПРО')
    subtitle = 'Пример: все названия, люди и документы вымышлены. Период 01-05.10.2026.' if example else 'Пустой шаблон. Мастерская ____________________  Начало учёта ______________'
    header(c, 'Кладовая мастерской: остатки', subtitle, 1, 2)
    rows = ITEMS if example else [['']*9 for _ in range(10)]
    widths = [17, 64, 16, 22, 22, 22, 22, 25, 59]
    table(c, ['Код', 'Наименование', 'Ед.', 'На начало', 'Приход', 'Выдача', 'Возврат', 'На полке', 'Место хранения'], rows,
          [x*mm for x in widths], 171*mm, 11*mm if not example else 14*mm)
    footer(c, [
        '<b>Как считать:</b> на полке = на начало + приход - выдача + возврат. Единицу измерения не меняйте в пределах одного кода.',
        'Укажите остаток на полке на дату начала. Каждое последующее движение заносите в журнал. Отрицательный остаток означает, что записи нужно проверить.',
    ])
    c.showPage()
    header(c, 'Кладовая мастерской: журнал', subtitle, 2, 2)
    rows = RECORDS if example else [['']*7 for _ in range(12)]
    widths = [27, 20, 25, 21, 62, 35, 79]
    table(c, ['Дата', 'Код', 'Операция', 'Кол-во', 'Кому / от кого', 'Документ', 'Примечание'], rows,
          [x*mm for x in widths], 171*mm, 8.5*mm)
    footer(c, [
        '<b>Операция:</b> приход добавляет на полку, выдача уменьшает, возврат добавляет ранее выданное. Количество всегда положительное.',
        'Заполняйте по порядку событий. Возврат расходника отмечайте только если он физически вернулся. Затем перенесите итоги по коду в лист остатков.',
    ])
    c.save()


def build_reconciliation():
    c = canvas.Canvas(str(OUT / 'inventory-checklist.pdf'), pagesize=landscape(A4))
    c.setTitle('Лист сверки остатков без интернета')
    c.setAuthor('Каптёрка ПРО')
    header(c, 'Сверка остатков без интернета', 'Место ____________________  Дата / время ____________________  Проверил ____________________', 1, 1)
    table(c, ['Код', 'Наименование', 'Ед.', 'Место', 'По учёту', 'Фактически', 'Разница', 'Причина / действие'],
          [['']*8 for _ in range(10)], [17*mm,59*mm,15*mm,37*mm,25*mm,26*mm,24*mm,66*mm], 171*mm, 11*mm)
    footer(c, [
        '<b>Порядок:</b> зафиксируйте время и приостановите движения на участке. Считайте то, что физически находится в указанном месте. Выданное отдельно уточните у получателя.',
        '<b>Разница = фактически - по учёту.</b> Минус означает недостачу, плюс - излишек. Пересчитайте расхождения и проверьте журнал до исправления остатков.',
    ])
    c.save()


export_qr()
build_workshop(False)
build_workshop(True)
build_reconciliation()
(QA / 'workshop-example-source.json').write_text(json.dumps({'url': URL, 'items': ITEMS, 'records': RECORDS}, ensure_ascii=False, indent=2), encoding='utf-8')
print(json.dumps({'pdfs': [str(OUT / x) for x in ['workshop-blank.pdf', 'workshop-example.pdf', 'inventory-checklist.pdf']], 'qr': str(QA / 'workshop-source-qr.png')}, ensure_ascii=False))
