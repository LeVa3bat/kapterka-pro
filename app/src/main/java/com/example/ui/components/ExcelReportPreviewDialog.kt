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
    tabTitles.add("Форма № 8 (Акт списания)")
    tabTitles.add("Форма № 18 (Книга учета)")
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
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Button(
                        onClick = {
                            val fileName = when {
                                selectedTab == 0 -> "Сводная_ведомость_${unitName.replace(" ", "_")}.csv"
                                selectedTab in 1..points.size -> "Ведомость_точки_${points[selectedTab - 1].name.replace(" ", "_")}.csv"
                                selectedTab == points.size + 1 -> "Форма_8_Акт_списания_${unitName.replace(" ", "_")}.csv"
                                selectedTab == points.size + 2 -> "Форма_18_Книга_учета_${unitName.replace(" ", "_")}.csv"
                                else -> "Реестр_заявок_${unitName.replace(" ", "_")}.csv"
                            }
                            shareReport(context, exportCsv, fileName)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF107C41),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "📥 СКАЧАТЬ / ЭКСПОРТ EXCEL (CSV/TSV)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
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
            // Header Row 1: Titles
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE2E6E2)) // Official subtle government document tint
                    .border(BorderStroke(1.dp, Color.Black))
            ) {
                table.headers.forEachIndexed { idx, title ->
                    val w = table.columnWidths[idx]
                    Box(
                        modifier = Modifier
                            .width(w)
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
                            lineHeight = 11.sp
                        )
                    }
                }
            }

            // Header Row 2: Standard military column numbering (Графа 1, 2, 3...)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF2F2F2))
                    .border(BorderStroke(1.dp, Color.Black))
            ) {
                table.headers.forEachIndexed { idx, _ ->
                    val w = table.columnWidths[idx]
                    Box(
                        modifier = Modifier
                            .width(w)
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

            // Table Rows
            table.rows.forEachIndexed { rowIdx, rowCells ->
                val rowBg = if (rowIdx % 2 == 1) Color(0xFFFAFAFA) else Color.White
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(rowBg)
                ) {
                    rowCells.forEachIndexed { colIdx, cellValue ->
                        val w = table.columnWidths.getOrElse(colIdx) { 70.dp }
                        val align = table.alignments.getOrElse(colIdx) { TextAlign.Start }
                        val isNumCol = align == TextAlign.End || align == TextAlign.Center

                        Box(
                            modifier = Modifier
                                .width(w)
                                .border(BorderStroke(0.5.dp, Color(0xFF888888)))
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
                                lineHeight = 11.sp
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
                        .background(Color(0xFFEDEDED))
                        .border(BorderStroke(1.dp, Color.Black))
                ) {
                    totalCells.forEachIndexed { colIdx, cellValue ->
                        val w = table.columnWidths.getOrElse(colIdx) { 70.dp }
                        val align = table.alignments.getOrElse(colIdx) { TextAlign.Start }

                        Box(
                            modifier = Modifier
                                .width(w)
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
                                textAlign = align
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
 * ФОРМА № 8 (по ОКУД 6002203 / Приказ МО РФ № 139):
 * «Акт списания (расхода) материальных ценностей»
 */
private fun buildForm8OfficialReport(
    operations: List<OperationRecord>,
    unitName: String,
    parseItems: (String) -> List<OperationItemEntry>
): Pair<List<MilitaryReportBlock>, String> {
    val expOps = operations.filter { it.type == OperationType.EXPENDITURE }
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("ru"))
    val todayStr = dateFormat.format(Date())

    val blocks = mutableListOf<MilitaryReportBlock>()
    val csv = StringBuilder()

    val header = MilitaryReportBlock.Header(
        docFormTitle = "Форма № 8 по ОКУД 6002203\nПриказ МО РФ № 139",
        approvalUnit = unitName,
        docMainTitle = "АКТ СПИСАНИЯ (РАСХОДА) МАТЕРИАЛЬНЫХ ЦЕННОСТЕЙ",
        subTitle = "Акт № 8/${expOps.firstOrNull()?.docNumber ?: "1"} от $todayStr г.",
        details = listOf(
            "Подразделение:" to unitName,
            "Основание составления:" to "Расход боеприпасов и материальных средств при выполнении боевых задач",
            "Служба снабжения:" to "Служба РАВ / Служба МТО",
            "Комиссия:" to "Председатель комиссии и члены комиссии подразделения"
        )
    )
    blocks.add(header)

    // Table columns according to Form 8
    val tableHeaders = listOf(
        "№\nп/п",
        "Наименование материальных\nценностей (номенклатура)",
        "Код / Кат.\nкачества",
        "Ед.\nизм.",
        "Кол-во\n(фактически\nизрасходовано)",
        "Цель расхода\n(боевая задача, приказ,\nобстоятельства)",
        "Дата расхода\nи точка (СП/ВОП)",
        "Первичный\nдокумент"
    )
    val colWidths = listOf(34.dp, 160.dp, 55.dp, 40.dp, 65.dp, 140.dp, 85.dp, 75.dp)
    val alignments = listOf(
        TextAlign.Center,
        TextAlign.Start,
        TextAlign.Center,
        TextAlign.Center,
        TextAlign.End,
        TextAlign.Start,
        TextAlign.Center,
        TextAlign.Center
    )

    val rows = mutableListOf<List<String>>()
    var totalQty = 0
    var itemIndex = 1

    for (op in expOps) {
        val opDate = dateFormat.format(Date(op.timestamp))
        val items = parseItems(op.itemsJson)
        if (items.isNotEmpty()) {
            for (item in items) {
                val reason = if (item.reason.isNotBlank()) item.reason else (if (op.comment.isNotBlank()) op.comment else "Выполнение боевой задачи")
                rows.add(
                    listOf(
                        itemIndex.toString(),
                        item.itemName,
                        item.categoryClass.ifEmpty { "Кат. 1" },
                        item.unit,
                        item.quantity.toString(),
                        reason,
                        "$opDate\n(${op.fromPointName})",
                        op.docNumber
                    )
                )
                totalQty += item.quantity
                itemIndex++
            }
        } else {
            rows.add(
                listOf(
                    itemIndex.toString(),
                    op.itemsSummary.ifEmpty { "Имущество подразделения" },
                    "Кат. 1",
                    "шт.",
                    "-",
                    op.comment.ifEmpty { "Боевой расход" },
                    "$opDate\n(${op.fromPointName})",
                    op.docNumber
                )
            )
            itemIndex++
        }
    }

    if (rows.isEmpty()) {
        rows.add(
            listOf(
                "1",
                "Мина 120-мм ОФ-843Б (образец)",
                "Кат. 1",
                "шт.",
                "18",
                "Подавление опорного пункта противника",
                "$todayStr\n(ОП «Заря»)",
                "АКТ-01"
            )
        )
        totalQty = 18
    }

    val totalRow = listOf(
        "ИТОГО",
        "Всего списано наименований: ${rows.size}",
        "-",
        "-",
        totalQty.toString(),
        "Расход подтвержден",
        "-",
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

    // Statutory signers for Form 8
    blocks.add(
        MilitaryReportBlock.Signatures(
            title = "Материальные ценности списаны по прямому назначению, остатки соответствуют учету:",
            signers = listOf(
                Triple("Председатель комиссии:", "____________________", "Командир подразделения"),
                Triple("Член комиссии (нач. склада):", "____________________", "Старшина подразделения"),
                Triple("Член комиссии (техник/стрелок):", "____________________", "Ответственное лицо")
            )
        )
    )

    // Build standard CSV
    csv.append("ФОРМА № 8 ПО ОКУД 6002203\tПРИКАЗ МО РФ № 139\n")
    csv.append("АКТ СПИСАНИЯ (РАСХОДА) МАТЕРИАЛЬНЫХ ЦЕННОСТЕЙ\n")
    csv.append("Подразделение:\t$unitName\n")
    csv.append("Дата:\t$todayStr\n\n")
    csv.append(tableHeaders.joinToString("\t") { it.replace("\n", " ") } + "\n")
    rows.forEach { r -> csv.append(r.joinToString("\t") { it.replace("\n", " ") } + "\n") }
    csv.append(totalRow.joinToString("\t") + "\n")

    return Pair(blocks, csv.toString())
}

/**
 * ФОРМА № 18 (по ОКУД 6002106 / Приказ МО РФ № 139 / Приказ МО РФ № 300):
 * «Книга учета наличия и движения материальных средств»
 */
private fun buildForm18OfficialReport(
    operations: List<OperationRecord>,
    unitName: String,
    parseItems: (String) -> List<OperationItemEntry>
): Pair<List<MilitaryReportBlock>, String> {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("ru"))
    val todayStr = dateFormat.format(Date())

    val blocks = mutableListOf<MilitaryReportBlock>()
    val csv = StringBuilder()

    val header = MilitaryReportBlock.Header(
        docFormTitle = "Форма № 18 по ОКУД 6002106\nПриказ МО РФ № 139, № 300",
        approvalUnit = unitName,
        docMainTitle = "КНИГА УЧЕТА НАЛИЧИЯ И ДВИЖЕНИЯ МАТЕРИАЛЬНЫХ СРЕДСТВ",
        subTitle = "Лицевой счет материальных средств подразделения за 2026 г.",
        details = listOf(
            "Подразделение:" to unitName,
            "Вид учета:" to "Количественный оперативный учет материальных средств",
            "Период ведения:" to "01.01.2026 — $todayStr"
        )
    )
    blocks.add(header)

    val tableHeaders = listOf(
        "Дата\nзаписи",
        "Наименование\nдокумента и номер",
        "От кого получено\nили кому выдано",
        "Наименование\nимущества",
        "Приход\n(получено)",
        "Расход\n(списано)",
        "Остаток\nналицо",
        "Подпись\nпроводящего"
    )
    val colWidths = listOf(65.dp, 95.dp, 120.dp, 150.dp, 60.dp, 60.dp, 60.dp, 65.dp)
    val alignments = listOf(
        TextAlign.Center,
        TextAlign.Start,
        TextAlign.Start,
        TextAlign.Start,
        TextAlign.End,
        TextAlign.End,
        TextAlign.End,
        TextAlign.Center
    )

    val rows = mutableListOf<List<String>>()
    var rollingStock = 0
    var totalPrihod = 0
    var totalRashod = 0

    for (op in operations) {
        val opDate = dateFormat.format(Date(op.timestamp))
        val items = parseItems(op.itemsJson)
        val sumQty = items.sumOf { it.quantity }
        val itemsText = if (items.isNotEmpty()) {
            items.joinToString(", ") { "${it.itemName} (${it.quantity} ${it.unit})" }
        } else {
            op.itemsSummary.ifEmpty { "Имущество" }
        }

        val docStr = "${op.type.titleRu}\n№ ${op.docNumber}"
        val parties = when (op.type) {
            OperationType.INCOME -> "Служба снабжения ➔ ${op.toPointName}"
            OperationType.TRANSFER -> "${op.fromPointName} ➔ ${op.toPointName}"
            OperationType.ISSUE -> "${op.fromPointName} ➔ ${op.toPointName} (Выдача)"
            OperationType.EXPENDITURE -> "${op.fromPointName} ➔ Расход (ф.8)"
        }

        val prihodStr = if (op.type == OperationType.INCOME) sumQty.toString() else "-"
        val rashodStr = if (op.type == OperationType.EXPENDITURE || op.type == OperationType.ISSUE) sumQty.toString() else "-"

        if (op.type == OperationType.INCOME) {
            rollingStock += sumQty
            totalPrihod += sumQty
        } else if (op.type == OperationType.EXPENDITURE || op.type == OperationType.ISSUE) {
            rollingStock = (rollingStock - sumQty).coerceAtLeast(0)
            totalRashod += sumQty
        }

        rows.add(
            listOf(
                opDate,
                docStr,
                parties,
                itemsText,
                prihodStr,
                rashodStr,
                rollingStock.toString(),
                "Проведено"
            )
        )
    }

    if (rows.isEmpty()) {
        rows.add(
            listOf(
                todayStr,
                "Акт приема № 12",
                "Служба снабжения ➔ Базовый склад",
                "Мина 120-мм ОФ-843Б (24 шт.)",
                "24",
                "-",
                "24",
                "Проведено"
            )
        )
        totalPrihod = 24
        rollingStock = 24
    }

    val totalRow = listOf(
        "ИТОГО",
        "Обороты за период",
        "-",
        "-",
        totalPrihod.toString(),
        totalRashod.toString(),
        rollingStock.toString(),
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
            title = "Правильность записей в книге учета подтверждаю:",
            signers = listOf(
                Triple("Лицо, ведущее учет (каптёр/старшина):", "____________________", "Ответственный за учет"),
                Triple("Проверил командир подразделения:", "____________________", unitName)
            )
        )
    )

    csv.append("ФОРМА № 18 ПО ОКУД 6002106\tПРИКАЗ МО РФ № 139 / № 300\n")
    csv.append("КНИГА УЧЕТА НАЛИЧИЯ И ДВИЖЕНИЯ МАТЕРИАЛЬНЫХ СРЕДСТВ\n")
    csv.append("Подразделение:\t$unitName\n\n")
    csv.append(tableHeaders.joinToString("\t") { it.replace("\n", " ") } + "\n")
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

    if (rows.isEmpty()) {
        rows.add(
            listOf(
                "1",
                "Мина 120-мм ОФ-843Б",
                "Служба РАВ",
                "шт.",
                "18",
                "9",
                "Базовый склад: 5; ОП «Скала»: 2; ОП «Заря»: 2",
                "9"
            )
        )
        totalInc = 18
        totalExp = 9
        totalRem = 9
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

    if (rows.isEmpty()) {
        rows.add(
            listOf(
                "-",
                "Имущество на данной точке отсутствует",
                "-",
                "-",
                "0",
                "0",
                "0",
                "-"
            )
        )
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

    if (rows.isEmpty()) {
        rows.add(
            listOf(
                "-",
                "-",
                "-",
                "-",
                "-",
                "Активных заявок нет",
                "-"
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
