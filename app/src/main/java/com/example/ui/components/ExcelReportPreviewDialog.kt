package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.util.ExcelExportHelper
import com.example.util.ExcelReportData
import java.text.SimpleDateFormat
import java.util.*

// Data structures for standard military document layout
sealed interface MilitaryReportBlock {
    data class Header(
        val docFormTitle: String, // e.g. "Форма № 8 по ОКУД 6002203"
        val approvalUnit: String, // e.g. "Командир 3 минбат"
        val docMainTitle: String, // e.g. "АКТ СПИСАНИЯ (РАСХОДА) МАТЕРИАЛЬНЫХ ЦЕННОСТЕЙ"
        val subTitle: String, // e.g. "№ 104 от 03.09.2026 г."
        val details: List<Pair<String, String>> = emptyList() // "Основание:", "Комиссия:" etc.
    ) : MilitaryReportBlock

    data class Table(
        val headers: List<String>,
        val columnWidths: List<Dp>,
        val alignments: List<TextAlign>,
        val rows: List<List<String>>,
        val totalRow: List<String>? = null
    ) : MilitaryReportBlock

    data class Signatures(
        val title: String = "Подписи ответственных лиц:",
        val signers: List<Triple<String, String, String>> // (Должность/роль, Подпись, И.О. Фамилия)
    ) : MilitaryReportBlock

    data class SimpleText(val text: String, val isBold: Boolean = false) : MilitaryReportBlock
}

@Composable
fun ExcelReportPreviewDialog(

    operations: List<OperationRecord>,
    stockRecords: List<StockRecord>,
    points: List<WarehousePoint> = emptyList(),
    catalogItems: List<InventoryItem> = emptyList(),
    requisitions: List<RequisitionRequest> = emptyList(),
    unitName: String,
    initialFormIndex: Int = 0,
    parseItems: (String) -> List<OperationItemEntry>,
    onDismiss: () -> Unit
) {
    val tabTitles = mutableListOf("Сводная ведомость (МТО)")
    points.forEach { pt ->
        tabTitles.add("Точка: ${pt.name}")
    }
    tabTitles.add("Форма № 8 (Раздаточная ведомость)")
    tabTitles.add("Форма № 18 (Книга учёта)")
    tabTitles.add("Заявки (Реестр)")

    var selectedTab by remember {
        mutableIntStateOf(initialFormIndex.coerceIn(0, (tabTitles.size - 1).coerceAtLeast(0)))
    }
    val context = LocalContext.current

    // Build structured military blocks and exportable text
    val (blocks, exportCsv) = remember(selectedTab, operations, stockRecords, points, catalogItems, requisitions, unitName) {
        when {
            selectedTab == 0 -> buildConsolidatedReport(catalogItems, stockRecords, points, unitName)
            selectedTab in 1..points.size -> buildSinglePointReport(points[selectedTab - 1], catalogItems, stockRecords.filter { it.pointId == points[selectedTab - 1].id }, unitName)
            selectedTab == points.size + 1 -> buildForm8OfficialReport(operations, unitName, parseItems)
            selectedTab == points.size + 2 -> buildForm18OfficialReport(operations, unitName, parseItems)
            else -> buildRequisitionsReport(requisitions, unitName)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.98f)
                .fillMaxHeight(0.94f)
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalSurface),
            border = BorderStroke(1.dp, TacticalBorder)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "УСТАВНЫЕ ВОИНСКИЕ ФОРМЫ (МО РФ)",
                            color = SageGreenBright,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Утвержденный бланк • Четкие графы • Готово к печати А4 / Excel",
                            color = TacticalTextMuted,
                            fontSize = 10.sp
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = TacticalTextMuted
                        )
                    }
                }

                // Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = TacticalBg,
                    contentColor = SageGreenBright,
                    edgePadding = 4.dp,
                    indicator = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = SageGreenPrimary
                            )
                        }
                    },
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (selectedTab == index) SageGreenBright else TacticalTextMuted
                                )
                            }
                        )
                    }
                }

                // Document Paper Sheet (Strict white military paper)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFFE8EAE6)) // Soft neutral frame around white sheet
                        .padding(6.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFCCCCCC))
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp)
                        ) {
                            items(blocks) { block ->
                                when (block) {
                                    is MilitaryReportBlock.Header -> {
                                        MilitaryHeaderView(block)
                                        Spacer(modifier = Modifier.height(10.dp))
                                    }
                                    is MilitaryReportBlock.Table -> {
                                        MilitaryTableView(block)
                                        Spacer(modifier = Modifier.height(14.dp))
                                    }
                                    is MilitaryReportBlock.Signatures -> {
                                        MilitarySignaturesView(block)
                                        Spacer(modifier = Modifier.height(10.dp))
                                    }
                                    is MilitaryReportBlock.SimpleText -> {
                                        Text(
                                            text = block.text,
                                            fontSize = 11.sp,
                                            fontWeight = if (block.isBold) FontWeight.Bold else FontWeight.Normal,
                                            color = Color.Black,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Export Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val baseFileName = when {
                        selectedTab == 0 -> "Сводная_ведомость_${unitName.replace(" ", "_")}"
                        selectedTab in 1..points.size -> "Ведомость_точки_${points[selectedTab - 1].name.replace(" ", "_")}"
                        selectedTab == points.size + 1 -> "Форма_8_Раздаточная_ведомость_${unitName.replace(" ", "_")}"
                        selectedTab == points.size + 2 -> "Форма_18_Книга_учета_${unitName.replace(" ", "_")}"
                        else -> "Реестр_заявок_${unitName.replace(" ", "_")}"
                    }
                    val sheetName = when {
                        selectedTab == 0 -> "Сводная МТО"
                        selectedTab in 1..points.size -> points[selectedTab - 1].name.take(28)
                        selectedTab == points.size + 1 -> "Форма 8 (Ведомость)"
                        selectedTab == points.size + 2 -> "Форма 18 (Книга)"
                        else -> "Заявки"
                    }

                    // 1. Главная кнопка: Реальный файл Excel (.xlsx)
                    Button(
                        onClick = {
                            val excelData = extractExcelReportData(sheetName, blocks)
                            val bytes = ExcelExportHelper.generateXlsxBytes(excelData)
                            val file = ExcelExportHelper.saveXlsxToDownloads(context, "$baseFileName.xlsx", bytes)
                            ExcelExportHelper.shareOrOpenExcel(context, file, "$baseFileName.xlsx")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF107C41), // Microsoft Excel Brand Green
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "СКАЧАТЬ EXCEL (.XLSX)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    // 2. Вторая кнопка: Текст / CSV для быстрой вставки
                    OutlinedButton(
                        onClick = {
                            shareReport(context, exportCsv, "$baseFileName.csv")
                        },
                        modifier = Modifier.height(46.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = SageGreenBright
                        ),
                        border = BorderStroke(1.dp, SageGreenPrimary.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Текст/CSV",
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// COMPOSABLE VIEWS FOR MILITARY ACCURACY
// -------------------------------------------------------------

@Composable
private fun MilitaryHeaderView(header: MilitaryReportBlock.Header) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Form standard badge top-right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "Воинская часть / Подразделение: ${header.approvalUnit}",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = header.docFormTitle,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF444444),
                textAlign = TextAlign.End,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // "УТВЕРЖДАЮ" Stamp Box (Top-Right standard military format)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Column(
                modifier = Modifier
                    .width(210.dp)
                    .border(1.dp, Color(0xFF555555))
                    .padding(6.dp)
            ) {
                Text(
                    text = "УТВЕРЖДАЮ",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Командир подразделения",
                    fontSize = 9.sp,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = header.approvalUnit,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "_________ / _______________ /",
                    fontSize = 8.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "« ___ » ____________ 2026 г.",
                    fontSize = 8.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Document Title Centered
        Text(
            text = header.docMainTitle,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        if (header.subTitle.isNotBlank()) {
            Text(
                text = header.subTitle,
                fontSize = 10.sp,
                color = Color(0xFF444444),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Additional Meta Lines
        if (header.details.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            header.details.forEach { (label, value) ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                    Text(
                        text = "$label ",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = value,
                        fontSize = 9.sp,
                        color = Color(0xFF222222)
                    )
                }
            }
        }
    }
}

@Composable
private fun MilitaryTableView(table: MilitaryReportBlock.Table) {
    val totalTableWidth = table.columnWidths.fold(0.dp) { acc, w -> acc + w }
    val hScroll = rememberScrollState()

    // Scrollable container with fixed column widths for perfectly aligned grid
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(hScroll)
            .border(1.dp, Color.Black)
    ) {
        Column(modifier = Modifier.width(totalTableWidth)) {
            // Header Row 1: Titles (All cells match the tallest cell height)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .background(Color(0xFFE2E6E2)) // Official subtle government document tint
                    .border(BorderStroke(1.dp, Color.Black))
            ) {
                table.headers.forEachIndexed { idx, title ->
                    val w = table.columnWidths[idx]
                    Box(
                        modifier = Modifier
                            .width(w)
                            .fillMaxHeight()
                            .border(BorderStroke(0.5.dp, Color.Black))
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            textAlign = TextAlign.Center,
                            lineHeight = 11.sp,
                            softWrap = true
                        )
                    }
                }
            }

            // Header Row 2: Standard military column numbering (Графа 1, 2, 3...)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .background(Color(0xFFF2F2F2))
                    .border(BorderStroke(1.dp, Color.Black))
            ) {
                table.headers.forEachIndexed { idx, _ ->
                    val w = table.columnWidths[idx]
                    Box(
                        modifier = Modifier
                            .width(w)
                            .fillMaxHeight()
                            .border(BorderStroke(0.5.dp, Color.Black))
                            .padding(vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${idx + 1}",
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF555555),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Table Rows (Even, uniform cell heights across each row with crisp borders)
            table.rows.forEachIndexed { rowIdx, rowCells ->
                val rowBg = if (rowIdx % 2 == 1) Color(0xFFFBFBFB) else Color.White
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .background(rowBg)
                ) {
                    rowCells.forEachIndexed { colIdx, cellValue ->
                        val w = table.columnWidths.getOrElse(colIdx) { 70.dp }
                        val align = table.alignments.getOrElse(colIdx) { TextAlign.Start }
                        val isNumCol = align == TextAlign.End || align == TextAlign.Center

                        Box(
                            modifier = Modifier
                                .width(w)
                                .fillMaxHeight()
                                .border(BorderStroke(0.5.dp, Color.Black))
                                .padding(horizontal = 4.dp, vertical = 5.dp),
                            contentAlignment = when (align) {
                                TextAlign.End -> Alignment.CenterEnd
                                TextAlign.Center -> Alignment.Center
                                else -> Alignment.CenterStart
                            }
                        ) {
                            Text(
                                text = cellValue,
                                fontSize = 8.5.sp,
                                fontFamily = if (isNumCol) FontFamily.Monospace else FontFamily.Default,
                                fontWeight = if (isNumCol && cellValue != "-" && cellValue != "0") FontWeight.Bold else FontWeight.Normal,
                                color = Color.Black,
                                textAlign = align,
                                lineHeight = 11.5.sp,
                                softWrap = true
                            )
                        }
                    }
                }
            }

            // Total / Summary Row if present
            table.totalRow?.let { totalCells ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .background(Color(0xFFEAEAEA))
                        .border(BorderStroke(1.dp, Color.Black))
                ) {
                    totalCells.forEachIndexed { colIdx, cellValue ->
                        val w = table.columnWidths.getOrElse(colIdx) { 70.dp }
                        val align = table.alignments.getOrElse(colIdx) { TextAlign.Start }

                        Box(
                            modifier = Modifier
                                .width(w)
                                .fillMaxHeight()
                                .border(BorderStroke(0.5.dp, Color.Black))
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            contentAlignment = when (align) {
                                TextAlign.End -> Alignment.CenterEnd
                                TextAlign.Center -> Alignment.Center
                                else -> Alignment.CenterStart
                            }
                        ) {
                            Text(
                                text = cellValue,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                textAlign = align,
                                lineHeight = 11.sp,
                                softWrap = true
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MilitarySignaturesView(sigs: MilitaryReportBlock.Signatures) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text(
            text = sigs.title,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(4.dp))
        sigs.signers.forEach { (role, signaturePlaceholder, person) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = role,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    modifier = Modifier.weight(1.3f)
                )
                Text(
                    text = signaturePlaceholder,
                    fontSize = 8.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = person,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1.2f)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// REPORT BUILDERS (PRECISE STATUTORY COMPLIANCE)
// -------------------------------------------------------------

/**
 * ФОРМА № 8 по ОКУД 6002203 (приказ МО РФ от 28.03.2008 № 139):
 * «Раздаточная (сдаточная) ведомость материальных ценностей».
 * Строки — получатели, графы — наименования; по каждому получателю дата
 * получения и место для расписки. Внизу «Итого» по каждому наименованию и
 * подписи «Выдал», «Принял», «Правильность выдачи проверил», «Бухгалтер».
 */
private fun buildForm8OfficialReport(
    operations: List<OperationRecord>,
    unitName: String,
    parseItems: (String) -> List<OperationItemEntry>
): Pair<List<MilitaryReportBlock>, String> {
    val issues = operations.filter { it.type == OperationType.ISSUE }.sortedBy { it.timestamp }
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("ru"))
    val todayStr = dateFormat.format(Date())
    val periodFrom = issues.firstOrNull()?.let { dateFormat.format(Date(it.timestamp)) } ?: todayStr
    val periodTo = issues.lastOrNull()?.let { dateFormat.format(Date(it.timestamp)) } ?: todayStr

    // Materials become columns (in order of first issue), recipients become rows.
    data class Line(val recipient: String, val date: String, val qty: Map<String, Int>)
    val materials = linkedMapOf<String, OperationItemEntry>()
    val lines = mutableListOf<Line>()
    for (op in issues) {
        val items = parseItems(op.itemsJson)
        items.forEach { materials.putIfAbsent(it.itemId.ifBlank { it.itemName }, it) }
        val qty = items.groupBy { it.itemId.ifBlank { it.itemName } }.mapValues { (_, v) -> v.sumOf { it.quantity } }
        lines.add(Line(op.toPointName.ifBlank { "—" }, dateFormat.format(Date(op.timestamp)), qty))
    }
    val materialKeys = materials.keys.toList()

    val blocks = mutableListOf<MilitaryReportBlock>()
    blocks.add(
        MilitaryReportBlock.Header(
            docFormTitle = "Форма № 8 по ОКУД 6002203\nКоды: дата ${todayStr}, по ОКПО ________",
            approvalUnit = unitName,
            docMainTitle = "РАЗДАТОЧНАЯ (СДАТОЧНАЯ) ВЕДОМОСТЬ № ___ МАТЕРИАЛЬНЫХ ЦЕННОСТЕЙ",
            subTitle = "за период с $periodFrom по $periodTo",
            details = listOf(
                "Воинская часть:" to "________________ (условное наименование)",
                "Структурное подразделение:" to unitName,
                "Материально ответственное лицо:" to "________________________",
                "Ведомость действительна по:" to "«__» ____________ 20__ г."
            )
        )
    )

    val headers = buildList {
        add("№\nп/п")
        add("Получатель\n(сдатчик)")
        add("Код\nполучателя")
        materialKeys.forEach { key ->
            val m = materials.getValue(key)
            add("${m.itemName}\nкод: ${m.itemId.take(10)}\n${m.unit} • ${m.categoryClass.ifBlank { "Кат. 1" }}")
        }
        add("Дата\nполучения\n(сдачи)")
        add("Расписка\nполучателя\n(сдатчика)")
    }
    val widths = buildList {
        add(34.dp); add(130.dp); add(60.dp)
        repeat(materialKeys.size) { add(96.dp) }
        add(78.dp); add(90.dp)
    }
    val aligns = buildList {
        add(TextAlign.Center); add(TextAlign.Start); add(TextAlign.Center)
        repeat(materialKeys.size) { add(TextAlign.Center) }
        add(TextAlign.Center); add(TextAlign.Center)
    }
    // Official column numbering row: 1, 2, 3 …
    val numbering = headers.indices.map { (it + 1).toString() }
    val rows = mutableListOf(numbering)
    lines.forEachIndexed { i, line ->
        rows.add(
            buildList {
                add((i + 1).toString())
                add(line.recipient)
                add("")
                materialKeys.forEach { key -> add(line.qty[key]?.toString() ?: "") }
                add(line.date)
                add("")
            }
        )
    }
    if (lines.isEmpty()) {
        rows.add(List(headers.size) { if (it == 1) "Выдач за период нет" else "" })
    }
    val totalRow = buildList {
        add("")
        add("Итого")
        add("")
        materialKeys.forEach { key -> add(lines.sumOf { it.qty[key] ?: 0 }.toString()) }
        add("")
        add("")
    }
    blocks.add(MilitaryReportBlock.Table(headers, widths, aligns, rows, totalRow))
    blocks.add(
        MilitaryReportBlock.Signatures(
            title = "Цена за единицу и сумма (руб. коп.) заполняются бухгалтерией.",
            signers = listOf(
                Triple("Выдал:", "____________________", "(должность, воинское звание, подпись, инициал имени, фамилия)"),
                Triple("Принял:", "____________________", "(должность, воинское звание, подпись, инициал имени, фамилия)"),
                Triple("Правильность выдачи (приема) проверил:", "____________________", "«__» ________ 20__ г."),
                Triple("Бухгалтер:", "____________________", "(подпись, инициал имени, фамилия) «__» ________ 20__ г.")
            )
        )
    )

    val csv = StringBuilder()
    csv.append("\t\t\tУТВЕРЖДАЮ\n\t\t\t________________________\n\t\t\t«__» __________ 20__ г.\n")
    csv.append("РАЗДАТОЧНАЯ (СДАТОЧНАЯ) ВЕДОМОСТЬ № ___ МАТЕРИАЛЬНЫХ ЦЕННОСТЕЙ\tФорма № 8 по ОКУД 6002203\n")
    csv.append("за период с $periodFrom по $periodTo\n")
    csv.append("Структурное подразделение:\t$unitName\n")
    csv.append("Материально ответственное лицо:\t\n\n")
    csv.append(headers.joinToString("\t") { it.replace("\n", " ") } + "\n")
    rows.forEach { r -> csv.append(r.joinToString("\t") { it.replace("\n", " ") } + "\n") }
    csv.append(totalRow.joinToString("\t") + "\n\n")
    csv.append("Выдал:\t____________\tПринял:\t____________\n")
    csv.append("Правильность выдачи (приема) проверил:\t____________\tБухгалтер:\t____________\n")
    return Pair(blocks, csv.toString())
}

/**
 * Книга учёта наличия и движения материальных ценностей подразделения
 * («форма 18» в приложении). Как в бумажной книге: на каждое наименование
 * отдельный раздел, по нему строки движения с графами «дата», «документ»,
 * «от кого получено / кому отпущено», «приход», «расход», «состоит».
 * Остаток считается отдельно по каждому наименованию.
 */
private fun buildForm18OfficialReport(
    operations: List<OperationRecord>,
    unitName: String,
    parseItems: (String) -> List<OperationItemEntry>
): Pair<List<MilitaryReportBlock>, String> {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("ru"))
    val todayStr = dateFormat.format(Date())
    val ordered = operations.sortedBy { it.timestamp }
    val periodFrom = ordered.firstOrNull()?.let { dateFormat.format(Date(it.timestamp)) } ?: todayStr

    data class Move(val op: OperationRecord, val entry: OperationItemEntry)
    val byItem = linkedMapOf<String, MutableList<Move>>()
    for (op in ordered) {
        for (entry in parseItems(op.itemsJson)) {
            byItem.getOrPut(entry.itemId.ifBlank { entry.itemName }) { mutableListOf() }.add(Move(op, entry))
        }
    }

    val blocks = mutableListOf<MilitaryReportBlock>()
    blocks.add(
        MilitaryReportBlock.Header(
            docFormTitle = "Книга учёта (ф. 18)\nведётся бессрочно",
            approvalUnit = unitName,
            docMainTitle = "КНИГА УЧЁТА НАЛИЧИЯ И ДВИЖЕНИЯ МАТЕРИАЛЬНЫХ ЦЕННОСТЕЙ",
            subTitle = "период с $periodFrom по $todayStr",
            details = listOf(
                "Подразделение:" to unitName,
                "Материально ответственное лицо:" to "________________________",
                "Разделов (наименований):" to byItem.size.toString()
            )
        )
    )

    val headers = listOf(
        "№\nзаписи",
        "Дата",
        "Наименование и\n№ документа",
        "От кого получено /\nкому отпущено",
        "Приход",
        "Расход",
        "Состоит\n(остаток)",
        "Подпись"
    )
    val widths = listOf(44.dp, 70.dp, 110.dp, 150.dp, 60.dp, 60.dp, 70.dp, 70.dp)
    val aligns = listOf(
        TextAlign.Center, TextAlign.Center, TextAlign.Start, TextAlign.Start,
        TextAlign.End, TextAlign.End, TextAlign.End, TextAlign.Center
    )
    val rows = mutableListOf(headers.indices.map { (it + 1).toString() })
    var totalIn = 0
    var totalOut = 0
    for ((_, moves) in byItem) {
        val first = moves.first().entry
        rows.add(listOf("", "", "▌ ${first.itemName}", "ед. изм.: ${first.unit} • ${first.categoryClass.ifBlank { "Кат. 1" }}", "", "", "", ""))
        var balance = 0
        moves.forEachIndexed { i, (op, entry) ->
            val qty = entry.quantity
            val (incoming, outgoing, party) = when (op.type) {
                OperationType.INCOME -> Triple(qty, 0, "Получено: ${op.fromPointName.ifBlank { "служба снабжения" }} → ${op.toPointName}")
                OperationType.ISSUE -> Triple(0, qty, "Выдано: ${op.toPointName}")
                OperationType.EXPENDITURE -> Triple(0, qty, "Списано (${entry.reason.ifBlank { op.comment.ifBlank { "расход" } }})")
                OperationType.TRANSFER -> Triple(0, 0, "Перемещено: ${op.fromPointName} → ${op.toPointName}")
            }
            balance = (balance + incoming - outgoing).coerceAtLeast(0)
            totalIn += incoming
            totalOut += outgoing
            rows.add(
                listOf(
                    (i + 1).toString(),
                    dateFormat.format(Date(op.timestamp)),
                    "${op.type.titleRu}${if (op.docNumber.isNotBlank()) " № ${op.docNumber.removePrefix("№").trim()}" else ""}",
                    party,
                    if (incoming > 0) incoming.toString() else "",
                    if (outgoing > 0) outgoing.toString() else "",
                    balance.toString(),
                    ""
                )
            )
        }
    }
    if (byItem.isEmpty()) rows.add(listOf("", "", "Записей нет", "", "", "", "", ""))
    val totalRow = listOf("", "", "Итого оборотов", "", totalIn.toString(), totalOut.toString(), "", "")
    blocks.add(MilitaryReportBlock.Table(headers, widths, aligns, rows, totalRow))
    blocks.add(
        MilitaryReportBlock.Signatures(
            title = "Сверено с данными учёта службы воинской части:",
            signers = listOf(
                Triple("Материально ответственное лицо:", "____________________", "(подпись, инициал имени, фамилия)"),
                Triple("Командир подразделения:", "____________________", "(подпись, инициал имени, фамилия)"),
                Triple("Начальник службы:", "____________________", "«__» ________ 20__ г.")
            )
        )
    )

    val csv = StringBuilder()
    csv.append("КНИГА УЧЁТА НАЛИЧИЯ И ДВИЖЕНИЯ МАТЕРИАЛЬНЫХ ЦЕННОСТЕЙ\n")
    csv.append("Подразделение:\t$unitName\nПериод:\tс $periodFrom по $todayStr\n\n")
    csv.append(headers.joinToString("\t") { it.replace("\n", " ") } + "\n")
    rows.forEach { r -> csv.append(r.joinToString("\t") { it.replace("\n", " ") } + "\n") }
    csv.append(totalRow.joinToString("\t") + "\n")
    return Pair(blocks, csv.toString())
}

/**
 * ПОЛНАЯ СВОДНАЯ ВЕДОМОСТЬ НАЛИЧИЯ И ОСТАТКОВ
 */
private fun buildConsolidatedReport(
    catalogItems: List<InventoryItem>,
    stockRecords: List<StockRecord>,
    points: List<WarehousePoint>,
    unitName: String
): Pair<List<MilitaryReportBlock>, String> {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("ru"))
    val todayStr = dateFormat.format(Date())

    val blocks = mutableListOf<MilitaryReportBlock>()
    val csv = StringBuilder()

    val header = MilitaryReportBlock.Header(
        docFormTitle = "Служебный документ МТО\nВоинский материальный учет",
        approvalUnit = unitName,
        docMainTitle = "СВОДНАЯ ОБОРОТНАЯ ВЕДОМОСТЬ НАЛИЧИЯ ИМУЩЕСТВА",
        subTitle = "По состоянию на $todayStr г.",
        details = listOf(
            "Подразделение:" to unitName,
            "Количество точек учета:" to "${points.size} (Базовый склад, СП, ВОП)",
            "Службы:" to "Служба РАВ, БПЛА, Вещевая, Инженерная"
        )
    )
    blocks.add(header)

    val tableHeaders = listOf(
        "№\nп/п",
        "Наименование имущества\n(номенклатура)",
        "Служба /\nКатегория",
        "Ед.\nизм.",
        "Всего\nприход",
        "Всего\nрасход",
        "Остатки по складам\nи огневым позициям (СП/ВОП)",
        "Итоговый\nостаток"
    )
    val colWidths = listOf(34.dp, 160.dp, 80.dp, 40.dp, 55.dp, 55.dp, 180.dp, 65.dp)
    val alignments = listOf(
        TextAlign.Center,
        TextAlign.Start,
        TextAlign.Start,
        TextAlign.Center,
        TextAlign.End,
        TextAlign.End,
        TextAlign.Start,
        TextAlign.End
    )

    val itemsMap = catalogItems.associateBy { it.id }
    val grouped = stockRecords.groupBy { it.itemId }
    val rows = mutableListOf<List<String>>()

    var totalInc = 0
    var totalExp = 0
    var totalRem = 0
    var idx = 1

    for ((itemId, records) in grouped) {
        val item = itemsMap[itemId]
        val itemName = item?.name ?: itemId
        val cat = item?.serviceCategory ?: "РАВ"
        val unit = item?.unit ?: "шт."
        val inc = records.sumOf { it.incomeTotal }
        val exp = records.sumOf { it.expenseTotal }
        val rem = records.sumOf { it.quantity }

        val pointsBreakdown = records.filter { it.quantity > 0 }.joinToString("; ") { r ->
            val pName = points.find { it.id == r.pointId }?.name ?: "Склад"
            "$pName: ${r.quantity}"
        }.ifEmpty { "Нет в наличии" }

        rows.add(
            listOf(
                idx.toString(),
                itemName,
                cat,
                unit,
                inc.toString(),
                exp.toString(),
                pointsBreakdown,
                rem.toString()
            )
        )
        totalInc += inc
        totalExp += exp
        totalRem += rem
        idx++
    }

    val totalRow = listOf(
        "ИТОГО",
        "Всего позиций в ведомости: ${rows.size}",
        "-",
        "-",
        totalInc.toString(),
        totalExp.toString(),
        "Все точки подразделения",
        totalRem.toString()
    )

    blocks.add(
        MilitaryReportBlock.Table(
            headers = tableHeaders,
            columnWidths = colWidths,
            alignments = alignments,
            rows = rows,
            totalRow = totalRow
        )
    )

    blocks.add(
        MilitaryReportBlock.Signatures(
            title = "Сводную ведомость составил:",
            signers = listOf(
                Triple("Начальник вещевого/тех. снабжения:", "____________________", "Старшина"),
                Triple("Сверил командир подразделения:", "____________________", unitName)
            )
        )
    )

    csv.append("СВОДНАЯ ВЕДОМОСТЬ НАЛИЧИЯ И ДВИЖЕНИЯ ИМУЩЕСТВА\n")
    csv.append("Подразделение:\t$unitName\n")
    csv.append("Дата:\t$todayStr\n\n")
    csv.append(tableHeaders.joinToString("\t") { it.replace("\n", " ") } + "\n")
    rows.forEach { r -> csv.append(r.joinToString("\t") { it.replace("\n", " ") } + "\n") }
    csv.append(totalRow.joinToString("\t") + "\n")

    return Pair(blocks, csv.toString())
}

/**
 * ВЕДОМОСТЬ ПО КОНКРЕТНОЙ ТОЧКЕ УЧЕТА
 */
private fun buildSinglePointReport(
    point: WarehousePoint,
    catalogItems: List<InventoryItem>,
    stockRecords: List<StockRecord>,
    unitName: String
): Pair<List<MilitaryReportBlock>, String> {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("ru"))
    val todayStr = dateFormat.format(Date())

    val blocks = mutableListOf<MilitaryReportBlock>()
    val csv = StringBuilder()

    val header = MilitaryReportBlock.Header(
        docFormTitle = "Раздаточная / Учетная ведомость\nТочка хранения",
        approvalUnit = unitName,
        docMainTitle = "ВЕДОМОСТЬ НАЛИЧИЯ ИМУЩЕСТВА НА СКЛАДЕ / ТОЧКЕ",
        subTitle = "Точка учета: «${point.name}»${if (point.isBase) " (Базовый склад)" else ""} на $todayStr г.",
        details = listOf(
            "Подразделение:" to unitName,
            "Точка хранения:" to point.name,
            "Описание точки:" to point.description.ifEmpty { "Склад / Огневая позиция" }
        )
    )
    blocks.add(header)

    val tableHeaders = listOf(
        "№\nп/п",
        "Наименование имущества",
        "Служба /\nКатегория",
        "Ед.\nизм.",
        "Поступило\nна точку",
        "Списано /\nИзрасходовано",
        "Фактический\nостаток",
        "Отметка о\nсверке"
    )
    val colWidths = listOf(34.dp, 160.dp, 80.dp, 40.dp, 65.dp, 65.dp, 65.dp, 60.dp)
    val alignments = listOf(
        TextAlign.Center,
        TextAlign.Start,
        TextAlign.Start,
        TextAlign.Center,
        TextAlign.End,
        TextAlign.End,
        TextAlign.End,
        TextAlign.Center
    )

    val itemsMap = catalogItems.associateBy { it.id }
    val rows = mutableListOf<List<String>>()

    var sumInc = 0
    var sumExp = 0
    var sumRem = 0
    var idx = 1

    for (r in stockRecords) {
        val item = itemsMap[r.itemId]
        val itemName = item?.name ?: r.itemId
        val cat = item?.serviceCategory ?: "РАВ"
        val unit = item?.unit ?: "шт."

        rows.add(
            listOf(
                idx.toString(),
                itemName,
                cat,
                unit,
                r.incomeTotal.toString(),
                r.expenseTotal.toString(),
                r.quantity.toString(),
                "В наличии"
            )
        )
        sumInc += r.incomeTotal
        sumExp += r.expenseTotal
        sumRem += r.quantity
        idx++
    }

    val totalRow = listOf(
        "ИТОГО",
        "Итого на точке «${point.name}»",
        "-",
        "-",
        sumInc.toString(),
        sumExp.toString(),
        sumRem.toString(),
        "-"
    )

    blocks.add(
        MilitaryReportBlock.Table(
            headers = tableHeaders,
            columnWidths = colWidths,
            alignments = alignments,
            rows = rows,
            totalRow = totalRow
        )
    )

    blocks.add(
        MilitaryReportBlock.Signatures(
            title = "Имущество на ответственное хранение принял:",
            signers = listOf(
                Triple("Материально ответственное лицо точки:", "____________________", "Ответственный (${point.name})"),
                Triple("Начальник материальной службы:", "____________________", "Старшина")
            )
        )
    )

    csv.append("ВЕДОМОСТЬ НАЛИЧИЯ ИМУЩЕСТВА: ${point.name}\n")
    csv.append("Подразделение:\t$unitName\n\n")
    csv.append(tableHeaders.joinToString("\t") { it.replace("\n", " ") } + "\n")
    rows.forEach { r -> csv.append(r.joinToString("\t") { it.replace("\n", " ") } + "\n") }
    csv.append(totalRow.joinToString("\t") + "\n")

    return Pair(blocks, csv.toString())
}

/**
 * РЕЕСТР ПОЛЕВЫХ ЗАЯВОК
 */
private fun buildRequisitionsReport(
    requisitions: List<RequisitionRequest>,
    unitName: String
): Pair<List<MilitaryReportBlock>, String> {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru"))
    val todayStr = dateFormat.format(Date())

    val blocks = mutableListOf<MilitaryReportBlock>()
    val csv = StringBuilder()

    val header = MilitaryReportBlock.Header(
        docFormTitle = "Журнал заявок МТО\nСнабжение передовой",
        approvalUnit = unitName,
        docMainTitle = "РЕЕСТР ЗАЯВОК НА МАТЕРИАЛЬНЫЕ СРЕДСТВА И БК",
        subTitle = "Журнал оперативных потребностей на $todayStr",
        details = listOf(
            "Подразделение:" to unitName,
            "Всего зарегистрировано заявок:" to "${requisitions.size} шт."
        )
    )
    blocks.add(header)

    val tableHeaders = listOf(
        "№\nп/п",
        "Дата и время\nподачи",
        "Позывной заявителя\n(кто подал)",
        "Точка / Позиция\nназначения",
        "Статус заявки",
        "Запрашиваемое имущество\nи количество",
        "Примечание / Оперативная\nсрочность"
    )
    val colWidths = listOf(34.dp, 85.dp, 95.dp, 95.dp, 80.dp, 150.dp, 110.dp)
    val alignments = listOf(
        TextAlign.Center,
        TextAlign.Center,
        TextAlign.Start,
        TextAlign.Start,
        TextAlign.Center,
        TextAlign.Start,
        TextAlign.Start
    )

    val rows = mutableListOf<List<String>>()
    requisitions.sortedByDescending { it.timestamp }.forEachIndexed { idx, req ->
        val dateStr = dateFormat.format(Date(req.timestamp))
        rows.add(
            listOf(
                (idx + 1).toString(),
                dateStr,
                req.applicantName,
                req.pointName,
                req.status.titleRu,
                req.itemsSummary,
                req.comment.ifEmpty { "Плановая заявка" }
            )
        )
    }

    blocks.add(
        MilitaryReportBlock.Table(
            headers = tableHeaders,
            columnWidths = colWidths,
            alignments = alignments,
            rows = rows,
            totalRow = null
        )
    )

    blocks.add(
        MilitaryReportBlock.Signatures(
            title = "Сводку заявок сверил:",
            signers = listOf(
                Triple("Начальник снабжения подразделения:", "____________________", "Ответственный"),
                Triple("Командир подразделения:", "____________________", unitName)
            )
        )
    )

    csv.append("РЕЕСТР ЗАЯВОК ПОДРАЗДЕЛЕНИЯ\n")
    csv.append("Подразделение:\t$unitName\n\n")
    csv.append(tableHeaders.joinToString("\t") { it.replace("\n", " ") } + "\n")
    rows.forEach { r -> csv.append(r.joinToString("\t") { it.replace("\n", " ") } + "\n") }

    return Pair(blocks, csv.toString())
}

private fun shareReport(context: Context, content: String, title: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, content)
        putExtra(Intent.EXTRA_TITLE, title)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Отправить ведомость Excel / Штаб")
    context.startActivity(shareIntent)
}

/**
 * Преобразует уставные военные блоки отчета в структуру реального Excel документа (.xlsx)
 */
fun extractExcelReportData(sheetName: String, blocks: List<MilitaryReportBlock>): ExcelReportData {
    var title = "ВОИНСКАЯ ВЕДОМОСТЬ"
    var subtitle = ""
    var details = emptyList<Pair<String, String>>()
    var headers = emptyList<String>()
    var colWidths = emptyList<Double>()
    var rows = emptyList<List<String>>()
    var totalRow: List<String>? = null
    var signers = emptyList<Triple<String, String, String>>()

    for (b in blocks) {
        when (b) {
            is MilitaryReportBlock.Header -> {
                title = b.docMainTitle
                subtitle = "${b.docFormTitle} • ${b.subTitle}".trim().trim('•').trim()
                details = b.details
            }
            is MilitaryReportBlock.Table -> {
                headers = b.headers.map { it.replace("\n", " ") }
                colWidths = b.columnWidths.map { (it.value / 5.2).coerceIn(8.0, 48.0) }
                rows = b.rows.map { row -> row.map { cell -> cell.replace("\n", " ") } }
                totalRow = b.totalRow?.map { it.replace("\n", " ") }
            }
            is MilitaryReportBlock.Signatures -> {
                signers = b.signers
            }
            is MilitaryReportBlock.SimpleText -> {}
        }
    }

    val cleanSheetName = sheetName
        .replace("[/\\\\?*\\]\\[]".toRegex(), " ")
        .take(31)
        .trim()
        .ifBlank { "Ведомость" }

    return ExcelReportData(
        sheetName = cleanSheetName,
        title = title,
        subtitle = subtitle,
        details = details,
        headers = headers,
        colWidthsChars = colWidths,
        rows = rows,
        totalRow = totalRow,
        signers = signers
    )
}

