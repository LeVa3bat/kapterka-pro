"""Verify native XLSX packages, printable PDFs and decoded QR destinations.

Uses bundled Python/Pillow/pypdf. zxing-cpp is a temporary QA dependency only;
pass its directory as argument 2. It is not needed by the public downloads.
"""
from pathlib import Path
from io import BytesIO
import json
import sys
import zipfile
import xml.etree.ElementTree as ET
from PIL import Image
from pypdf import PdfReader

ROOT = Path(sys.argv[1]).resolve()
if len(sys.argv) > 2:
    sys.path.insert(0, sys.argv[2])
import zxingcpp

OUT = ROOT / 'docs' / 'downloads'
QA = ROOT / 'growth' / 'qa'
URL = 'https://kapterka-pro.ru/uchet-instrumenta-android.html#workshop-kit'
NS = {'m': 'http://schemas.openxmlformats.org/spreadsheetml/2006/main'}
checks = []


def qr_check(image, label):
    found = zxingcpp.read_barcodes(image)
    values = [code.text for code in found]
    assert URL in values, (label, values)
    assert set(values) == {URL}, (label, values)
    checks.append({'kind': 'decoded QR', 'label': label, 'url': URL, 'passed': True})


for name in ['workshop-blank.xlsx', 'workshop-example.xlsx']:
    with zipfile.ZipFile(OUT / name) as archive:
        bad = archive.testzip()
        assert bad is None, (name, bad)
        assert not any(x.endswith('vbaProject.bin') for x in archive.namelist())
        data_validations = 0
        conditional_rules = 0
        errors = []
        for index in range(1, 4):
            sheet = ET.fromstring(archive.read(f'xl/worksheets/sheet{index}.xml'))
            for cell in sheet.findall('m:sheetData/m:row/m:c', NS):
                if cell.get('t') == 'e':
                    errors.append({'cell': cell.get('r'), 'value': cell.findtext('m:v', namespaces=NS)})
            validations = sheet.findall('m:dataValidations/m:dataValidation', NS)
            data_validations += len(validations)
            conditional_rules += len(sheet.findall('m:conditionalFormatting/m:cfRule', NS))
            for validation in validations:
                assert validation.get('errorStyle', 'stop') == 'stop', (name, validation.attrib)
                assert validation.get('showErrorMessage') in ('1', 'true'), (name, validation.attrib)
                assert validation.get('allowBlank') in ('1', 'true'), (name, validation.attrib)
        assert data_validations == 6, (name, data_validations)
        assert conditional_rules == 4, (name, conditional_rules)
        assert not errors, (name, errors)
        media = [x for x in archive.namelist() if x.startswith('xl/media/') and x.endswith('.png')]
        assert len(media) == 1, (name, media)
        qr_check(Image.open(BytesIO(archive.read(media[0]))), f'{name} embedded image')
        relationships = ET.fromstring(archive.read('xl/worksheets/_rels/sheet3.xml.rels'))
        targets = {item.get('Id'): item.get('Target') for item in relationships}
        guide = ET.fromstring(archive.read('xl/worksheets/sheet3.xml'))
        links = guide.findall('m:hyperlinks/m:hyperlink', NS)
        destinations = [targets.get(link.get('{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id'), '') + ('#'+link.get('location') if link.get('location') else '') for link in links]
        assert URL in destinations, (name, 'missing clickable source link', destinations)
        checks.append({'kind': 'XLSX native package', 'file': name, 'validationRules': data_validations, 'conditionalRules': conditional_rules, 'formulaErrors': errors, 'macros': False, 'passed': True})

for name, count in [('workshop-blank.pdf', 2), ('workshop-example.pdf', 2), ('inventory-checklist.pdf', 1)]:
    reader = PdfReader(OUT / name)
    assert len(reader.pages) == count, (name, len(reader.pages))
    for number, page in enumerate(reader.pages, 1):
        width, height = float(page.mediabox.width), float(page.mediabox.height)
        assert abs(width - 841.8898) < 1 and abs(height - 595.2756) < 1, (name, number, width, height)
        assert 'Каптёрка ПРО' in page.extract_text(), (name, number, 'font/text extraction')
        links = [annotation.get_object().get('/A', {}).get('/URI') for annotation in page.get('/Annots', [])]
        assert URL in links, (name, number, links)
        png = QA / f'{Path(name).stem}-{number}.png'
        image = Image.open(png)
        qr_check(image, f'{name} page {number} rendered at 120 dpi')
        qr_check(image.convert('L'), f'{name} page {number} grayscale at 120 dpi')
    checks.append({'kind': 'PDF print geometry', 'file': name, 'pages': count, 'paper': 'A4 landscape', 'sourceLinkOnEveryPage': True, 'passed': True})

for name in ['blank-guide.png', 'example-guide.png']:
    qr_check(Image.open(QA / name), f'XLSX preview {name}')

files = [{ 'file': name, 'bytes': (OUT / name).stat().st_size } for name in ['workshop-blank.xlsx','workshop-example.xlsx','workshop-blank.pdf','workshop-example.pdf','inventory-checklist.pdf']]
report = {'sourceUrl': URL, 'qrDecoder': 'zxing-cpp 3.1.1', 'checks': checks, 'files': files, 'physicalPrinterTest': 'not performed'}
(QA / 'workshop-package-checks.json').write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
print(json.dumps({'checks': len(checks), 'files': files}, ensure_ascii=False))
