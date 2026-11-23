const assert = require('node:assert/strict');
const zlib = require('node:zlib');
const { spawnSync } = require('node:child_process');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const {
  FORM_TEMPLATES,
  sanitizeExcelText,
  validateBuilderRows,
  summarizeUnits,
  buildFormXlsxBytes,
  recommendSetup
} = require('../../docs/growth-tools.js');

// Independent pure-JS ZIP Central Directory + Local Header + zlib.crc32 validator
function parseAndVerifyZip(uint8) {
  const buf = Buffer.from(uint8);
  assert.equal(buf.readUInt32LE(0), 0x04034B50, 'ZIP must start with local file header');
  // Find EOCD
  let eocdOffset = -1;
  for (let i = buf.length - 22; i >= Math.max(0, buf.length - 1024); i--) {
    if (buf.readUInt32LE(i) === 0x06054B50) {
      eocdOffset = i;
      break;
    }
  }
  assert.ok(eocdOffset > 0, 'ZIP EOCD signature not found');
  const cdCount = buf.readUInt16LE(eocdOffset + 10);
  const cdSize = buf.readUInt32LE(eocdOffset + 12);
  const cdOffset = buf.readUInt32LE(eocdOffset + 16);
  assert.equal(cdOffset + cdSize, eocdOffset, 'Central Directory size mismatch');

  const files = {};
  let ptr = cdOffset;
  for (let i = 0; i < cdCount; i++) {
    assert.equal(buf.readUInt32LE(ptr), 0x02014B50, 'CDH signature mismatch');
    const crc = buf.readUInt32LE(ptr + 16);
    const compSize = buf.readUInt32LE(ptr + 20);
    const uncompSize = buf.readUInt32LE(ptr + 24);
    const nameLen = buf.readUInt16LE(ptr + 28);
    const extraLen = buf.readUInt16LE(ptr + 30);
    const commentLen = buf.readUInt16LE(ptr + 32);
    const lfhOffset = buf.readUInt32LE(ptr + 42);
    const name = buf.toString('utf8', ptr + 46, ptr + 46 + nameLen);
    ptr += 46 + nameLen + extraLen + commentLen;

    assert.equal(buf.readUInt32LE(lfhOffset), 0x04034B50, `LFH signature mismatch for ${name}`);
    const lfhNameLen = buf.readUInt16LE(lfhOffset + 26);
    const lfhExtraLen = buf.readUInt16LE(lfhOffset + 28);
    const dataStart = lfhOffset + 30 + lfhNameLen + lfhExtraLen;
    assert.equal(compSize, uncompSize, `Expected uncompressed store entry for ${name}`);
    const payload = buf.subarray(dataStart, dataStart + uncompSize);
    assert.equal(zlib.crc32(payload) >>> 0, crc, `CRC-32 mismatch for ${name}`);
    files[name] = payload.toString('utf8');
  }
  return files;
}

// 1. Formula injection protection & validation
assert.equal(sanitizeExcelText('=CMD|"/c calc"!A0'), "'=CMD|\"/c calc\"!A0");
assert.equal(sanitizeExcelText('+1+1'), "'+1+1");
assert.equal(sanitizeExcelText('-2+3'), "'-2+3");
assert.equal(sanitizeExcelText('@SUM(1,1)'), "'@SUM(1,1)");
assert.equal(sanitizeExcelText('  \x07Молоток\x00  '), 'Молоток');

const validated = validateBuilderRows([
  { name: '=HYPERLINK("https://evil.example")', unit: 'шт.', qty: '2,5', person: 'Иванов' },
  { name: 'Перчатки', unit: 'пар', qty: '10', person: 'Петров' },
  { name: 'Свёрла', unit: 'шт.', qty: '1.5', person: 'Сидоров' }
]);
assert.equal(validated.length, 3);
assert.equal(validated[0].name, "'=HYPERLINK(\"https://evil.example\")");
assert.equal(validated[0].qty, 2.5);

// 2. Unit-safe summary never mixes шт. and пар
assert.equal(summarizeUnits(validated), '4 шт. · 10 пар');

assert.throws(() => validateBuilderRows([]), /хотя бы одну/);
assert.throws(() => validateBuilderRows(new Array(16).fill({ name: 'X', unit: 'шт.', qty: '1' })), /до 15/);
assert.throws(() => validateBuilderRows([{ name: '', unit: 'шт.', qty: '1' }]), /наименование/);
assert.throws(() => validateBuilderRows([{ name: 'Болт', unit: 'шт.', qty: '-5' }]), /положительным/);

// 3. Quiz recommendation logic
const workshopSolo = recommendSetup({ sphere: 'workshop', team: 'solo', weeklyOps: 25 });
assert.equal(workshopSolo.personaIndex, 0);
assert.match(workshopSolo.tierText, /Базового режима/);
assert.ok(workshopSolo.hoursSaved >= 2);

const militaryTeam = recommendSetup({ sphere: 'military', team: 'team', weeklyOps: 150 });
assert.equal(militaryTeam.personaIndex, 3);
assert.match(militaryTeam.forms, /Форма № 8/);
assert.match(militaryTeam.tierText, /PRO/);

// 4. Generate valid OOXML .xlsx for all 3 templates and verify ZIP + XML
const requiredEntries = [
  '[Content_Types].xml',
  '_rels/.rels',
  'xl/workbook.xml',
  'xl/_rels/workbook.xml.rels',
  'xl/styles.xml',
  'xl/worksheets/sheet1.xml'
];

for (const formType of Object.keys(FORM_TEMPLATES)) {
  const result = buildFormXlsxBytes({
    formType,
    orgName: 'ООО Тест & Партнёры',
    warehouseName: 'Склад № 1',
    rows: [
      { name: '=SUM(A1)', unit: 'шт.', qty: '3', person: 'Мастер <А>' },
      { name: 'Перчатки', unit: 'пар', qty: '5', person: 'Бригада 1' }
    ]
  });
  assert.ok(result.bytes instanceof Uint8Array);
  assert.equal(result.unitSummary, '3 шт. · 5 пар');
  assert.equal(result.rowCount, 2);

  const entries = parseAndVerifyZip(result.bytes);
  for (const name of requiredEntries) {
    assert.ok(name in entries, `Missing OOXML entry ${name}`);
    assert.match(entries[name], /^<\?xml version="1\.0" encoding="UTF-8" standalone="yes"\?>/);
  }
  const sheet = entries['xl/worksheets/sheet1.xml'];
  assert.ok(sheet.includes('&apos;=SUM(A1)'), `Formula guard missing in ${formType}`);
  assert.ok(sheet.includes('ООО Тест &amp; Партнёры'), `XML ampersand escape missing in ${formType}`);
  assert.ok(sheet.includes('Мастер &lt;А&gt;'), `XML angle-bracket escape missing in ${formType}`);
  assert.ok(sheet.includes('kapterka-pro.ru'), `Site attribution missing in ${formType}`);

  // Cross-check with OS native Zip + XML parser (.NET on Windows, Python on POSIX)
  if (process.platform === 'win32') {
    const tmpFile = path.join(os.tmpdir(), `kapterka-xlsx-test-${formType}-${process.pid}.xlsx`);
    fs.writeFileSync(tmpFile, result.bytes);
    try {
      const psScript = [
        'Add-Type -AssemblyName System.IO.Compression,System.IO.Compression.FileSystem;',
        `$z=[System.IO.Compression.ZipFile]::OpenRead('${tmpFile.replace(/'/g, "''")}');`,
        'if($z.Entries.Count -ne 6){throw "Bad count"};',
        'foreach($e in $z.Entries){$sr=New-Object System.IO.StreamReader($e.Open(),[Text.Encoding]::UTF8);$x=[xml]$sr.ReadToEnd();$sr.Dispose();};',
        '$z.Dispose(); Write-Output "DOTNET_OK"'
      ].join(' ');
      const ps = spawnSync('powershell', ['-NoProfile', '-Command', psScript], { encoding: 'utf8' });
      assert.equal(ps.status, 0, `.NET Zip/XML parse failed for ${formType}: ${ps.stderr || ps.stdout}`);
      assert.match(ps.stdout, /DOTNET_OK/);
    } finally {
      try { fs.unlinkSync(tmpFile); } catch {}
    }
  } else {
    const pyCheck = spawnSync('python3', ['-c', 'import io,sys,zipfile,xml.etree.ElementTree as ET; zf=zipfile.ZipFile(io.BytesIO(sys.stdin.buffer.read())); assert zf.testzip() is None; [ET.fromstring(zf.read(n)) for n in zf.namelist()]'], { input: Buffer.from(result.bytes), encoding: 'utf8' });
    assert.equal(pyCheck.status, 0, `Python Zip/XML parse failed for ${formType}: ${pyCheck.stderr}`);
  }
}

console.log('PASS: growth-tools XLSX OOXML builder (node zlib + OS ZIP/XML parser), formula guard, unit-safe totals and 3-click quiz.');
