package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class ExcelReportData(
    val sheetName: String,
    val title: String,
    val subtitle: String = "",
    val details: List<Pair<String, String>> = emptyList(),
    val headers: List<String>,
    val colWidthsChars: List<Double>,
    val rows: List<List<String>>,
    val totalRow: List<String>? = null,
    val signers: List<Triple<String, String, String>> = emptyList()
)

object ExcelExportHelper {

    /**
     * Создает полноценный бинарный .xlsx файл (OpenXML Spreadsheet)
     * со стилями, границами ячеек, авто-переносом текста, жирными заголовками и уставным оформлением.
     */
    fun generateXlsxBytes(data: ExcelReportData): ByteArray {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { zip ->
            // 1. [Content_Types].xml
            zip.putNextEntry(ZipEntry("[Content_Types].xml"))
            zip.write(getContentTypesXml().toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()

            // 2. _rels/.rels
            zip.putNextEntry(ZipEntry("_rels/.rels"))
            zip.write(getRootRelsXml().toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()

            // 3. xl/_rels/workbook.xml.rels
            zip.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
            zip.write(getWorkbookRelsXml().toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()

            // 4. xl/workbook.xml
            zip.putNextEntry(ZipEntry("xl/workbook.xml"))
            zip.write(getWorkbookXml(data.sheetName).toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()

            // 5. xl/styles.xml
            zip.putNextEntry(ZipEntry("xl/styles.xml"))
            zip.write(getStylesXml().toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()

            // 6. xl/worksheets/sheet1.xml
            zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            zip.write(getSheetXml(data).toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()
        }
        return bos.toByteArray()
    }

    /**
     * Сохраняет .xlsx файл в системную папку Загрузки (Downloads) устройства
     * и возвращает локальный File для просмотра / отправки.
     */
    fun saveXlsxToDownloads(context: Context, fileName: String, bytes: ByteArray): File {
        val safeName = if (fileName.endsWith(".xlsx", ignoreCase = true)) fileName else "$fileName.xlsx"
        
        // 1. Сохраняем в кэш приложения для мгновенного шаринга через FileProvider
        val cacheFile = File(context.cacheDir, safeName)
        try {
            FileOutputStream(cacheFile).use { it.write(bytes) }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Экспортируем в публичную системную папку «Загрузки» (Downloads)
        var savedPublic = false
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, safeName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(bytes)
                        savedPublic = true
                    }
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (downloadsDir.exists() || downloadsDir.mkdirs()) {
                    val publicFile = File(downloadsDir, safeName)
                    FileOutputStream(publicFile).use { it.write(bytes) }
                    savedPublic = true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val message = if (savedPublic) {
            "Таблица сохранена в «Загрузки»:\n$safeName"
        } else {
            "Файл Excel сформирован:\n$safeName"
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()

        return cacheFile
    }

    /**
     * Запускает системный диалог выбора приложения (Microsoft Excel, МойОфис, Таблицы, Telegram, WhatsApp).
     */
    fun shareOrOpenExcel(context: Context, file: File, title: String) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TITLE, title)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Открыть отчет Excel / Отправить")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Не удалось открыть Excel: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun escapeXml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun colLetter(colIdx: Int): String {
        var n = colIdx
        val result = StringBuilder()
        while (n >= 0) {
            result.insert(0, ('A'.code + (n % 26)).toChar())
            n = (n / 26) - 1
        }
        return result.toString()
    }

    private fun getContentTypesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""

    private fun getRootRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private fun getWorkbookRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

    private fun getWorkbookXml(sheetName: String): String {
        val safeSheetName = escapeXml(sheetName.take(31).ifEmpty { "Ведомость" })
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="$safeSheetName" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>"""
    }

    private fun getStylesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="5">
    <!-- 0: Regular body 10pt -->
    <font><sz val="10"/><name val="Calibri"/><family val="2"/></font>
    <!-- 1: Bold header 10pt -->
    <font><b/><sz val="10"/><name val="Calibri"/><family val="2"/></font>
    <!-- 2: Title bold 12pt -->
    <font><b/><sz val="12"/><name val="Calibri"/><family val="2"/></font>
    <!-- 3: Small 9pt -->
    <font><sz val="9"/><name val="Calibri"/><family val="2"/></font>
    <!-- 4: Small bold 9pt -->
    <font><b/><sz val="9"/><name val="Calibri"/><family val="2"/></font>
  </fonts>
  <fills count="4">
    <!-- 0: none -->
    <fill><patternFill patternType="none"/></fill>
    <!-- 1: gray125 -->
    <fill><patternFill patternType="gray125"/></fill>
    <!-- 2: Tactical Military Sage #D9E1D2 -->
    <fill><patternFill patternType="solid"><fgColor rgb="FFD9E1D2"/></patternFill></fill>
    <!-- 3: Light gray for numbering #EFEFEF -->
    <fill><patternFill patternType="solid"><fgColor rgb="FFEFEFEF"/></patternFill></fill>
  </fills>
  <borders count="3">
    <!-- 0: none -->
    <border><left/><right/><top/><bottom/><diagonal/></border>
    <!-- 1: Thin black all around -->
    <border>
      <left style="thin"><color rgb="FF000000"/></left>
      <right style="thin"><color rgb="FF000000"/></right>
      <top style="thin"><color rgb="FF000000"/></top>
      <bottom style="thin"><color rgb="FF000000"/></bottom>
      <diagonal/>
    </border>
    <!-- 2: Total row (top thin, bottom double) -->
    <border>
      <left style="thin"><color rgb="FF000000"/></left>
      <right style="thin"><color rgb="FF000000"/></right>
      <top style="thin"><color rgb="FF000000"/></top>
      <bottom style="double"><color rgb="FF000000"/></bottom>
      <diagonal/>
    </border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="9">
    <!-- 0: default -->
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <!-- 1: Title (bold 12pt) -->
    <xf numFmtId="0" fontId="2" fillId="0" borderId="0" xfId="0" applyFont="1"><alignment vertical="center"/></xf>
    <!-- 2: Subtitle/Meta (small 9pt) -->
    <xf numFmtId="0" fontId="3" fillId="0" borderId="0" xfId="0" applyFont="1"><alignment vertical="center"/></xf>
    <!-- 3: Table Header (bold, centered, wrapText, sage fill, border) -->
    <xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center" wrapText="1"/>
    </xf>
    <!-- 4: Table Column Numbers (small bold, centered, light gray fill, border) -->
    <xf numFmtId="0" fontId="4" fillId="3" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
    <!-- 5: Cell text left (wrapText, border) -->
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1">
      <alignment horizontal="left" vertical="center" wrapText="1"/>
    </xf>
    <!-- 6: Cell text center (wrapText, border) -->
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center" wrapText="1"/>
    </xf>
    <!-- 7: Cell numeric/right (wrapText, border) -->
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1">
      <alignment horizontal="right" vertical="center" wrapText="1"/>
    </xf>
    <!-- 8: Total row (bold, border2) -->
    <xf numFmtId="0" fontId="1" fillId="0" borderId="2" xfId="0" applyFont="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="right" vertical="center" wrapText="1"/>
    </xf>
  </cellXfs>
</styleSheet>"""

    private fun getSheetXml(data: ExcelReportData): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")

        // Custom column widths
        sb.append("<cols>")
        data.colWidthsChars.forEachIndexed { idx, width ->
            val colNum = idx + 1
            sb.append("""<col min="$colNum" max="$colNum" width="$width" customWidth="1"/>""")
        }
        sb.append("</cols>")

        sb.append("<sheetData>")
        var rowNum = 1

        // Row 1: Title
        sb.append("""<row r="$rowNum">""")
        sb.append("""<c r="A$rowNum" s="1" t="inlineStr"><is><t>${escapeXml(data.title)}</t></is></c>""")
        sb.append("</row>")
        rowNum++

        // Row 2: Subtitle
        if (data.subtitle.isNotBlank()) {
            sb.append("""<row r="$rowNum">""")
            sb.append("""<c r="A$rowNum" s="2" t="inlineStr"><is><t>${escapeXml(data.subtitle)}</t></is></c>""")
            sb.append("</row>")
            rowNum++
        }

        // Details (Основание, Подразделение и т.д.)
        for ((k, v) in data.details) {
            sb.append("""<row r="$rowNum">""")
            sb.append("""<c r="A$rowNum" s="2" t="inlineStr"><is><t>${escapeXml(k)}</t></is></c>""")
            sb.append("""<c r="B$rowNum" s="2" t="inlineStr"><is><t>${escapeXml(v)}</t></is></c>""")
            sb.append("</row>")
            rowNum++
        }

        // Пустая строка перед таблицей
        rowNum++

        // Таблица: Строка заголовков (стиль 3)
        sb.append("""<row r="$rowNum" ht="28" customHeight="1">""")
        data.headers.forEachIndexed { colIdx, header ->
            val cellRef = "${colLetter(colIdx)}$rowNum"
            val cleanHeader = header.replace("\n", " ")
            sb.append("""<c r="$cellRef" s="3" t="inlineStr"><is><t>${escapeXml(cleanHeader)}</t></is></c>""")
        }
        sb.append("</row>")
        rowNum++

        // Таблица: Нумерация граф (Графа 1, 2, 3...) (стиль 4)
        sb.append("""<row r="$rowNum" ht="18" customHeight="1">""")
        data.headers.forEachIndexed { colIdx, _ ->
            val cellRef = "${colLetter(colIdx)}$rowNum"
            sb.append("""<c r="$cellRef" s="4"><v>${colIdx + 1}</v></c>""")
        }
        sb.append("</row>")
        rowNum++

        // Таблица: Строки данных (стили 5 - текст лево, 6 - центр, 7 - число право)
        data.rows.forEach { rowCells ->
            sb.append("""<row r="$rowNum">""")
            rowCells.forEachIndexed { colIdx, value ->
                val cellRef = "${colLetter(colIdx)}$rowNum"
                // Проверяем, является ли значение числом
                val num = value.toDoubleOrNull()
                if (num != null && !value.startsWith("+") && !value.contains("-") && colIdx > 1) {
                    sb.append("""<c r="$cellRef" s="7"><v>$value</v></c>""")
                } else {
                    val style = if (colIdx == 0 || colIdx == 2 || colIdx == 3) 6 else 5
                    sb.append("""<c r="$cellRef" s="$style" t="inlineStr"><is><t>${escapeXml(value)}</t></is></c>""")
                }
            }
            sb.append("</row>")
            rowNum++
        }

        // Строка ИТОГО (если есть)
        data.totalRow?.let { totalCells ->
            sb.append("""<row r="$rowNum" ht="22" customHeight="1">""")
            totalCells.forEachIndexed { colIdx, value ->
                val cellRef = "${colLetter(colIdx)}$rowNum"
                sb.append("""<c r="$cellRef" s="8" t="inlineStr"><is><t>${escapeXml(value)}</t></is></c>""")
            }
            sb.append("</row>")
            rowNum++
        }

        // Подписи ответственных лиц
        if (data.signers.isNotEmpty()) {
            rowNum++ // пустая строка
            sb.append("""<row r="$rowNum">""")
            sb.append("""<c r="A$rowNum" s="1" t="inlineStr"><is><t>Подписи должностных лиц:</t></is></c>""")
            sb.append("</row>")
            rowNum++

            data.signers.forEach { (role, line, name) ->
                sb.append("""<row r="$rowNum" ht="20" customHeight="1">""")
                sb.append("""<c r="A$rowNum" s="2" t="inlineStr"><is><t>${escapeXml(role)}</t></is></c>""")
                sb.append("""<c r="B$rowNum" s="2" t="inlineStr"><is><t>${escapeXml(line)}</t></is></c>""")
                sb.append("""<c r="C$rowNum" s="2" t="inlineStr"><is><t>${escapeXml(name)}</t></is></c>""")
                sb.append("</row>")
                rowNum++
            }
        }

        sb.append("</sheetData>")
        sb.append("</worksheet>")
        return sb.toString()
    }
}
