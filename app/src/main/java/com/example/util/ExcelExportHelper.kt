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
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Структура данных для экспорта в официальный Excel (.xlsx)
 */
data class ExcelReportData(
    val sheetName: String,
    val title: String,
    val subtitle: String = "",
    val details: List<Pair<String, String>> = emptyList(),
    val headers: List<String>,
    val colWidthsChars: List<Double> = emptyList(),
    val rows: List<List<String>>,
    val totalRow: List<String>? = null,
    val signers: List<Triple<String, String, String>> = emptyList()
)

object ExcelExportHelper {

    /**
     * Создает стандартный файл OpenXML Spreadsheet (.xlsx) без сторонних тяжелых библиотек.
     * Открывается во всех версиях Microsoft Excel, LibreOffice, WPS Office, МойОфис и Таблицах Google.
     */
    fun generateXlsxBytes(data: ExcelReportData): ByteArray {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { zip ->
            // 1. [Content_Types].xml
            zip.putNextEntry(ZipEntry("[Content_Types].xml"))
            zip.write(contentTypesXml.toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()

            // 2. _rels/.rels
            zip.putNextEntry(ZipEntry("_rels/.rels"))
            zip.write(rootRelsXml.toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()

            // 3. xl/workbook.xml
            zip.putNextEntry(ZipEntry("xl/workbook.xml"))
            zip.write(workbookXml(data.sheetName).toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()

            // 4. xl/_rels/workbook.xml.rels
            zip.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
            zip.write(workbookRelsXml.toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()

            // 5. xl/styles.xml
            zip.putNextEntry(ZipEntry("xl/styles.xml"))
            zip.write(stylesXml.toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()

            // 6. xl/worksheets/sheet1.xml
            zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            zip.write(sheetXml(data).toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()
        }
        return bos.toByteArray()
    }

    private fun escapeXml(s: String): String {
        return s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun colLetter(colIdx: Int): String {
        var col = colIdx
        val sb = StringBuilder()
        while (col >= 0) {
            sb.insert(0, ('A'.code + (col % 26)).toChar())
            col = (col / 26) - 1
        }
        return sb.toString()
    }

    private val contentTypesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>""".trimIndent()

    private val rootRelsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>""".trimIndent()

    private fun workbookXml(sheetName: String): String {
        val safeName = escapeXml(sheetName.take(31))
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="$safeName" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>""".trimIndent()
    }

    private val workbookRelsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>""".trimIndent()

    /**
     * Styles:
     * 0: Normal
     * 1: Title (Bold 14pt, Centered)
     * 2: Subtitle (Bold 10pt, Centered)
     * 3: Header cell (Bold 9pt, Centered, Gray fill #E2E6E2, thin border)
     * 4: Subheader / Col Numbers (8pt, Centered, #F2F2F2 fill, thin border)
     * 5: Data Text Left (9pt, Left align, thin border)
     * 6: Data Text Center (9pt, Center align, thin border)
     * 7: Data Text Right (9pt, Right align, thin border)
     * 8: Total Row Left (Bold 9pt, Left, #EDEDED fill, thin border)
     * 9: Total Row Right (Bold 9pt, Right, #EDEDED fill, thin border)
     * 10: Details label (Bold 9pt, Left)
     * 11: Details value (9pt, Left)
     * 12: Signature label (Bold 9pt, Left)
     * 13: Signature underline (9pt, Center, bottom border)
     */
    private val stylesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="4">
    <font><sz val="9"/><name val="Calibri"/></font>
    <font><b/><sz val="14"/><name val="Calibri"/></font>
    <font><b/><sz val="10"/><name val="Calibri"/></font>
    <font><b/><sz val="9"/><name val="Calibri"/></font>
  </fonts>
  <fills count="5">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFE2E6E2"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFF2F2F2"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFEDEDED"/></patternFill></fill>
  </fills>
  <borders count="3">
    <border><left/><right/><top/><bottom/></border>
    <border>
      <left style="thin"><color rgb="FF000000"/></left>
      <right style="thin"><color rgb="FF000000"/></right>
      <top style="thin"><color rgb="FF000000"/></top>
      <bottom style="thin"><color rgb="FF000000"/></bottom>
    </border>
    <border>
      <left/><right/><top/>
      <bottom style="thin"><color rgb="FF000000"/></bottom>
    </border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="14">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0"><alignment horizontal="center" vertical="center"/></xf>
    <xf numFmtId="0" fontId="2" fillId="0" borderId="0" xfId="0"><alignment horizontal="center" vertical="center"/></xf>
    <xf numFmtId="0" fontId="3" fillId="2" borderId="1" xfId="0"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>
    <xf numFmtId="0" fontId="0" fillId="3" borderId="1" xfId="0"><alignment horizontal="center" vertical="center"/></xf>
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0"><alignment horizontal="left" vertical="center" wrapText="1"/></xf>
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0"><alignment horizontal="center" vertical="center"/></xf>
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0"><alignment horizontal="right" vertical="center"/></xf>
    <xf numFmtId="0" fontId="3" fillId="4" borderId="1" xfId="0"><alignment horizontal="left" vertical="center"/></xf>
    <xf numFmtId="0" fontId="3" fillId="4" borderId="1" xfId="0"><alignment horizontal="right" vertical="center"/></xf>
    <xf numFmtId="0" fontId="3" fillId="0" borderId="0" xfId="0"><alignment horizontal="left" vertical="center"/></xf>
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"><alignment horizontal="left" vertical="center"/></xf>
    <xf numFmtId="0" fontId="3" fillId="0" borderId="0" xfId="0"><alignment horizontal="left" vertical="center"/></xf>
    <xf numFmtId="0" fontId="0" fillId="0" borderId="2" xfId="0"><alignment horizontal="center" vertical="center"/></xf>
  </cellXfs>
</styleSheet>""".trimIndent()

    private fun sheetXml(data: ExcelReportData): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")

        val colCount = maxOf(data.headers.size, 1)

        // Column widths
        sb.append("<cols>")
        for (i in 0 until colCount) {
            val width = if (i < data.colWidthsChars.size) data.colWidthsChars[i] else 16.0
            val clampedWidth = width.coerceIn(8.0, 50.0)
            sb.append("""<col min="${i + 1}" max="${i + 1}" width="$clampedWidth" customWidth="1"/>""")
        }
        sb.append("</cols>")

        sb.append("<sheetData>")
        var rowNum = 1

        // 1. Title Row
        sb.append("""<row r="$rowNum" ht="26" customHeight="1">""")
        sb.append("""<c r="A$rowNum" s="1" t="inlineStr"><is><t>${escapeXml(data.title)}</t></is></c>""")
        sb.append("</row>")
        rowNum++

        // 2. Subtitle Row
        if (data.subtitle.isNotBlank()) {
            sb.append("""<row r="$rowNum" ht="18" customHeight="1">""")
            sb.append("""<c r="A$rowNum" s="2" t="inlineStr"><is><t>${escapeXml(data.subtitle)}</t></is></c>""")
            sb.append("</row>")
            rowNum++
        }

        // 3. Details (Основание, Комиссия, Период)
        if (data.details.isNotEmpty()) {
            rowNum++ // Blank line
            for (d in data.details) {
                sb.append("""<row r="$rowNum">""")
                sb.append("""<c r="A$rowNum" s="10" t="inlineStr"><is><t>${escapeXml(d.first)}</t></is></c>""")
                sb.append("""<c r="B$rowNum" s="11" t="inlineStr"><is><t>${escapeXml(d.second)}</t></is></c>""")
                sb.append("</row>")
                rowNum++
            }
        }

        // Empty space before table
        rowNum++

        // 4. Headers
        sb.append("""<row r="$rowNum" ht="28" customHeight="1">""")
        for ((cIdx, h) in data.headers.withIndex()) {
            val ref = "${colLetter(cIdx)}$rowNum"
            sb.append("""<c r="$ref" s="3" t="inlineStr"><is><t>${escapeXml(h)}</t></is></c>""")
        }
        sb.append("</row>")
        rowNum++

        // 5. Header column numbers (1, 2, 3...)
        sb.append("""<row r="$rowNum" ht="16" customHeight="1">""")
        for (cIdx in data.headers.indices) {
            val ref = "${colLetter(cIdx)}$rowNum"
            sb.append("""<c r="$ref" s="4" t="inlineStr"><is><t>${cIdx + 1}</t></is></c>""")
        }
        sb.append("</row>")
        rowNum++

        // 6. Data rows
        for (r in data.rows) {
            sb.append("""<row r="$rowNum" ht="20" customHeight="1">""")
            for (cIdx in 0 until colCount) {
                val cellVal = if (cIdx < r.size) r[cIdx] else ""
                val ref = "${colLetter(cIdx)}$rowNum"

                // Alignment: numbers on right, units/num on center, names on left
                val styleId = when {
                    cIdx == 0 -> 6 // №
                    cIdx == 1 -> 5 // Наименование
                    cIdx == 2 -> 6 // Ед. изм / Категория
                    cellVal.toDoubleOrNull() != null -> 7 // Number
                    cellVal == "-" -> 6
                    else -> 5
                }

                // If numeric, write as number
                val numVal = cellVal.replace(" ", "").replace(",", ".").toDoubleOrNull()
                if (numVal != null && cIdx > 1) {
                    sb.append("""<c r="$ref" s="$styleId"><v>$numVal</v></c>""")
                } else {
                    sb.append("""<c r="$ref" s="$styleId" t="inlineStr"><is><t>${escapeXml(cellVal)}</t></is></c>""")
                }
            }
            sb.append("</row>")
            rowNum++
        }

        // 7. Total row
        if (data.totalRow != null) {
            sb.append("""<row r="$rowNum" ht="22" customHeight="1">""")
            for (cIdx in 0 until colCount) {
                val cellVal = if (cIdx < data.totalRow.size) data.totalRow[cIdx] else ""
                val ref = "${colLetter(cIdx)}$rowNum"
                val styleId = if (cIdx <= 1) 8 else 9

                val numVal = cellVal.replace(" ", "").replace(",", ".").toDoubleOrNull()
                if (numVal != null && cIdx > 1) {
                    sb.append("""<c r="$ref" s="$styleId"><v>$numVal</v></c>""")
                } else {
                    sb.append("""<c r="$ref" s="$styleId" t="inlineStr"><is><t>${escapeXml(cellVal)}</t></is></c>""")
                }
            }
            sb.append("</row>")
            rowNum++
        }

        // 8. Signatures
        if (data.signers.isNotEmpty()) {
            rowNum++ // Blank line
            sb.append("""<row r="$rowNum">""")
            sb.append("""<c r="A$rowNum" s="12" t="inlineStr"><is><t>Подписи должностных лиц:</t></is></c>""")
            sb.append("</row>")
            rowNum++

            for (s in data.signers) {
                sb.append("""<row r="$rowNum" ht="20" customHeight="1">""")
                sb.append("""<c r="A$rowNum" s="10" t="inlineStr"><is><t>${escapeXml(s.first)}</t></is></c>""")
                sb.append("""<c r="B$rowNum" s="13" t="inlineStr"><is><t>${escapeXml(s.second.ifBlank { "________" })}</t></is></c>""")
                val thirdCol = if (colCount >= 3) colLetter(2) else "C"
                sb.append("""<c r="$thirdCol$rowNum" s="11" t="inlineStr"><is><t>${escapeXml(s.third)}</t></is></c>""")
                sb.append("</row>")
                rowNum++
            }
        }

        sb.append("</sheetData>")

        // Merges for titles
        val lastColLetter = colLetter(maxOf(colCount - 1, 1))
        sb.append("<mergeCells count=\"2\">")
        sb.append("""<mergeCell ref="A1:${lastColLetter}1"/>""")
        if (data.subtitle.isNotBlank()) {
            sb.append("""<mergeCell ref="A2:${lastColLetter}2"/>""")
        }
        sb.append("</mergeCells>")

        sb.append("</worksheet>")
        return sb.toString()
    }

    /**
     * Сохраняет файл в папку «Загрузки» устройства (Downloads) и возвращает File.
     */
    fun saveXlsxToDownloads(context: Context, fileName: String, bytes: ByteArray): File? {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Каптерка")
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { os ->
                        os.write(bytes)
                        os.flush()
                    }
                }
            }

            // Also save locally in app cache / files directory so FileProvider can immediately open/share it
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val localFile = File(exportDir, fileName)
            FileOutputStream(localFile).use { fos ->
                fos.write(bytes)
                fos.flush()
            }

            Toast.makeText(context, "Файл сохранен: $fileName", Toast.LENGTH_LONG).show()
            return localFile
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Ошибка сохранения: ${e.message}", Toast.LENGTH_LONG).show()
            return null
        }
    }

    /**
     * Открывает диалог "Поделиться / Открыть в Excel" через системный шлюз Android
     */
    fun shareOrOpenExcel(context: Context, file: File?, fileName: String) {
        if (file == null || !file.exists()) {
            Toast.makeText(context, "Не удалось подготовить файл для отправки", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, fileName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Открыть или отправить ведомость Excel")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Не удалось открыть Excel: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
