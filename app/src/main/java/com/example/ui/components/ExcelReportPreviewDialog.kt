package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.util.ExcelExportHelper
import com.example.util.ReportBuilders
import com.example.util.ReportColAlign
import com.example.util.ReviewPromptPreferences
import com.example.util.StructuredReport

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
    profileMode: WorkspaceProfileMode = WorkspaceProfileMode.MILITARY,
    responsiblePerson: String = "",
    onDismiss: () -> Unit
) {
    val activeMode = remember(profileMode) {
        if (profileMode.isMilitary) WorkspaceProfileMode.MILITARY
        else WorkspaceProfileMode.CIVILIAN
    }

    val tabTitles = remember(activeMode, points) {
        buildList {
            if (activeMode.isMilitary) {
                add("Сводная (МТО)")
                points.forEach { add("Точка: ${it.name}") }
                add("Форма № 8 (ОКУД 6002203)")
                add("Форма № 18 (Книга учёта)")
                add("Акт списания")
                add("Реестр заявок")
            } else {
                add("Оборотная ТМЦ")
                points.forEach { add("Склад: ${it.name}") }
                add("Накладная М-11 / ТОРГ-13")
                add("Выдача на руки (МБ-7)")
                add("Акт списания (МБ-8)")
                add("Реестр заявок")
            }
        }
    }

    var selectedTab by remember(activeMode, points.size) {
        mutableIntStateOf(initialFormIndex.coerceIn(0, (tabTitles.size - 1).coerceAtLeast(0)))
    }
    val context = LocalContext.current
    val reviewPreferences = remember(context) {
        ReviewPromptPreferences(context.getSharedPreferences("kapterka_app_prefs", Context.MODE_PRIVATE))
    }
    var showReviewRequest by remember { mutableStateOf(false) }
    var reviewPendingAfterChooser by remember { mutableStateOf(false) }
    var chooserWasPaused by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, reviewPreferences) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE && reviewPendingAfterChooser) {
                chooserWasPaused = true
            }
            if (event == Lifecycle.Event.ON_RESUME && reviewPendingAfterChooser && chooserWasPaused) {
                reviewPendingAfterChooser = false
                chooserWasPaused = false
                showReviewRequest = reviewPreferences.recordPromptAfterSavedExport()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val report: StructuredReport = remember(
        activeMode, selectedTab, operations, stockRecords, points, catalogItems, requisitions, unitName, responsiblePerson
    ) {
        val ptCount = points.size
        when {
            selectedTab == 0 -> ReportBuilders.buildConsolidatedTurnoverReport(
                profileMode = activeMode,
                catalogItems = catalogItems,
                stockRecords = stockRecords,
                operations = operations,
                points = points,
                unitName = unitName,
                responsiblePerson = responsiblePerson,
                parseItems = parseItems
            )
            selectedTab in 1..ptCount -> ReportBuilders.buildSinglePointReport(
                profileMode = activeMode,
                point = points[selectedTab - 1],
                catalogItems = catalogItems,
                stockRecords = stockRecords.filter { it.pointId == points[selectedTab - 1].id },
                operations = operations,
                unitName = unitName,
                responsiblePerson = responsiblePerson,
                parseItems = parseItems
            )
            selectedTab == ptCount + 1 -> if (activeMode.isMilitary) {
                ReportBuilders.buildMilitaryForm8(
                    operations = operations,
                    catalogItems = catalogItems,
                    unitName = unitName,
                    responsiblePerson = responsiblePerson,
                    parseItems = parseItems
                )
            } else {
                ReportBuilders.buildCivilianTransferInvoiceM11(
                    operations = operations,
                    points = points,
                    catalogItems = catalogItems,
                    unitName = unitName,
                    responsiblePerson = responsiblePerson,
                    parseItems = parseItems
                )
            }
            selectedTab == ptCount + 2 -> if (activeMode.isMilitary) {
                ReportBuilders.buildMilitaryForm18(
                    operations = operations,
                    stockRecords = stockRecords,
                    points = points,
                    catalogItems = catalogItems,
                    unitName = unitName,
                    responsiblePerson = responsiblePerson,
                    parseItems = parseItems
                )
            } else {
                ReportBuilders.buildCivilianIssueRegisterMB7(
                    operations = operations,
                    catalogItems = catalogItems,
                    unitName = unitName,
                    responsiblePerson = responsiblePerson,
                    parseItems = parseItems
                )
            }
            selectedTab == ptCount + 3 -> if (activeMode.isMilitary) {
                ReportBuilders.buildMilitaryExpenditureAct(
                    operations = operations,
                    catalogItems = catalogItems,
                    unitName = unitName,
                    responsiblePerson = responsiblePerson,
                    parseItems = parseItems
                )
            } else {
                ReportBuilders.buildCivilianWriteOffActMB8(
                    operations = operations,
                    catalogItems = catalogItems,
                    unitName = unitName,
                    responsiblePerson = responsiblePerson,
                    parseItems = parseItems
                )
            }
            else -> ReportBuilders.buildRequisitionsReport(
                profileMode = activeMode,
                requisitions = requisitions,
                unitName = unitName
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Animated3DDialogContainer(
            modifier = Modifier
                .fillMaxWidth(0.97f)
                .fillMaxHeight(0.94f)
                .padding(vertical = 6.dp),
            cornerRadius = 24.dp,
            accentBorder = SageGreenPrimary
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 3D Dialog Header (strictly locked to active workspace profile — zero military forms in Civilian/Empty)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SageGreenDark.copy(alpha = 0.45f))
                        .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (activeMode.isMilitary) "УСТАВНЫЕ ФОРМЫ (МО РФ)" else "СКЛАДСКИЕ БЛАНКИ (ОКУД)",
                            color = SageGreenBright,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.4.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (activeMode.isMilitary) {
                                "Форма № 8 • Форма № 18 • Акт списания • Excel А4"
                            } else {
                                "Оборотная ТМЦ • М-11 / ТОРГ-13 • МБ-7 • МБ-8 • Excel А4"
                            },
                            color = TacticalTextMuted,
                            fontSize = 10.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconOrb3DButton(
                        icon = Icons.Default.Close,
                        contentDescription = "Закрыть",
                        tint = TacticalTextMuted,
                        onClick = onDismiss,
                        size = 34.dp
                    )
                }

                // Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedTab.coerceIn(0, (tabTitles.size - 1).coerceAtLeast(0)),
                    containerColor = TacticalSurface,
                    contentColor = SageGreenBright,
                    edgePadding = 8.dp,
                    indicator = { tabPositions ->
                        val safeIdx = selectedTab.coerceIn(0, (tabPositions.size - 1).coerceAtLeast(0))
                        if (safeIdx < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[safeIdx]),
                                color = SageGreenPrimary
                            )
                        }
                    }
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 12.5.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == index) SageGreenBright else TacticalTextMuted,
                                    maxLines = 1
                                )
                            }
                        )
                    }
                }

                // Strict white printable paper sheet
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFFE2E8F0))
                        .padding(8.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(6.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            item {
                                PrintableHeaderView(report)
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                            item {
                                PrintableTableView(report)
                                Spacer(modifier = Modifier.height(14.dp))
                            }
                            if (report.signers.isNotEmpty()) {
                                item {
                                    PrintableSignaturesView(report.signaturesTitle, report.signers)
                                    Spacer(modifier = Modifier.height(10.dp))
                                }
                            }
                        }
                    }
                }

                // Export Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Volumetric3DButton(
                        text = "Скачать Excel",
                        icon = Icons.Default.FileDownload,
                        onClick = {
                            val excelData = report.toExcelReportData()
                            val bytes = ExcelExportHelper.generateXlsxBytes(excelData)
                            val fileName = "${report.fileBaseName}.xlsx"
                            val file = ExcelExportHelper.saveXlsxToDownloads(context, fileName, bytes)
                            if (file != null) {
                                reviewPendingAfterChooser = !showReviewRequest
                                chooserWasPaused = false
                                if (!ExcelExportHelper.shareOrOpenExcel(context, file, fileName)) {
                                    reviewPendingAfterChooser = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        height = 50.dp,
                        cornerRadius = 15.dp
                    )

                    OutlinedButton(
                        onClick = {
                            shareReportText(context, report.toTsvText(), "${report.fileBaseName}.csv")
                        },
                        modifier = Modifier.height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SageGreenBright),
                        border = BorderStroke(1.2.dp, SageGreenPrimary.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Текст/CSV",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }

                if (showReviewRequest) {
                    ReviewRequestCard(
                        onOpenReview = {
                            reviewPreferences.stop()
                            showReviewRequest = false
                        },
                        onLater = {
                            reviewPreferences.later()
                            showReviewRequest = false
                        },
                        onStopAsking = {
                            reviewPreferences.stop()
                            showReviewRequest = false
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FormModeChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) SageGreenPrimary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else TacticalTextSecondary,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PrintableHeaderView(report: StructuredReport) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Top line: Form standard on left/top + Code box (OKUD / OKPO) & Approval Stamp on right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = report.docFormTitle,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF1E293B),
                    lineHeight = 11.sp
                )
                if (report.approvalLines.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(0.8.dp, Color(0xFF475569))
                            .padding(6.dp)
                    ) {
                        report.approvalLines.forEachIndexed { i, line ->
                            Text(
                                text = line,
                                fontSize = if (i == 0) 9.sp else 8.sp,
                                fontWeight = if (i == 0) FontWeight.Bold else FontWeight.Normal,
                                color = Color.Black,
                                lineHeight = 10.5.sp
                            )
                        }
                    }
                }
            }

            // Official OKUD / OKPO Code Table
            if (report.formCodeRows.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .width(142.dp)
                        .border(1.dp, Color.Black)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFE2E8F0))
                            .border(0.5.dp, Color.Black)
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("КОДЫ", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                    report.formCodeRows.forEach { (k, v) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min)
                                .border(0.5.dp, Color.Black)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1.1f)
                                    .fillMaxHeight()
                                    .border(0.5.dp, Color.Black)
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(k, fontSize = 7.sp, color = Color(0xFF334155), lineHeight = 9.sp)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(0.9f)
                                    .fillMaxHeight()
                                    .border(0.5.dp, Color.Black)
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    v,
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Main Document Title Centered
        Text(
            text = report.docMainTitle,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.Black,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp,
            modifier = Modifier.fillMaxWidth()
        )
        if (report.subTitle.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = report.subTitle,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF334155),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Document Meta Details
        if (report.details.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            report.details.forEach { (label, value) ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.5.dp)) {
                    Text(
                        text = "$label ",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = value,
                        fontSize = 8.5.sp,
                        color = Color(0xFF1E293B)
                    )
                }
            }
        }
    }
}

@Composable
private fun PrintableTableView(report: StructuredReport) {
    val totalTableWidth = report.colWidthsDp.fold(0.dp) { acc, w -> acc + w.dp }
    val hScroll = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(hScroll)
            .border(1.dp, Color.Black)
    ) {
        Column(modifier = Modifier.width(totalTableWidth)) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .background(Color(0xFFE3EFEA))
                    .border(BorderStroke(1.dp, Color.Black))
            ) {
                report.headers.forEachIndexed { idx, title ->
                    val w = report.colWidthsDp.getOrElse(idx) { 72 }.dp
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
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            textAlign = TextAlign.Center,
                            lineHeight = 10.5.sp
                        )
                    }
                }
            }

            // Column Numbering Row (Графа 1, 2, 3...)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .background(Color(0xFFF1F5F9))
                    .border(BorderStroke(1.dp, Color.Black))
            ) {
                report.headers.forEachIndexed { idx, _ ->
                    val w = report.colWidthsDp.getOrElse(idx) { 72 }.dp
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
                            color = Color(0xFF475569),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Data Rows
            report.rows.forEachIndexed { rowIdx, rowCells ->
                val isSection = rowIdx in report.sectionRowIndices
                val rowBg = when {
                    isSection -> Color(0xFFECF7F2)
                    rowIdx % 2 == 1 -> Color(0xFFF7FAF8)
                    else -> Color.White
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .background(rowBg)
                ) {
                    rowCells.forEachIndexed { colIdx, cellValue ->
                        val w = report.colWidthsDp.getOrElse(colIdx) { 72 }.dp
                        val align = when (report.alignments.getOrElse(colIdx) { ReportColAlign.START }) {
                            ReportColAlign.START -> TextAlign.Start
                            ReportColAlign.CENTER -> TextAlign.Center
                            ReportColAlign.END -> TextAlign.End
                        }
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
                                fontSize = if (isSection) 8.5.sp else 8.sp,
                                fontFamily = if (!isSection && isNumCol) FontFamily.Monospace else FontFamily.Default,
                                fontWeight = if (isSection || (isNumCol && cellValue.isNotBlank() && cellValue != "-" && cellValue != "—" && cellValue != "0")) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSection) Color(0xFF0B5D47) else Color.Black,
                                textAlign = if (isSection) TextAlign.Start else align,
                                lineHeight = 11.sp
                            )
                        }
                    }
                }
            }

            // Total Row
            report.totalRow?.let { totalCells ->
                PrintableSummaryRow(
                    cells = totalCells,
                    widths = report.colWidthsDp,
                    alignments = report.alignments,
                    bgColor = Color(0xFFE2E8F0),
                    bold = true
                )
            }

            // Extra Accounting Rows (Цена за единицу / Сумма in Form 8)
            report.extraFooterRows.forEach { extraCells ->
                PrintableSummaryRow(
                    cells = extraCells,
                    widths = report.colWidthsDp,
                    alignments = report.alignments,
                    bgColor = Color(0xFFF8FAFC),
                    bold = false
                )
            }
        }
    }
}

@Composable
private fun PrintableSummaryRow(
    cells: List<String>,
    widths: List<Int>,
    alignments: List<ReportColAlign>,
    bgColor: Color,
    bold: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(bgColor)
            .border(BorderStroke(1.dp, Color.Black))
    ) {
        cells.forEachIndexed { colIdx, cellValue ->
            val w = widths.getOrElse(colIdx) { 72 }.dp
            val align = when (alignments.getOrElse(colIdx) { ReportColAlign.START }) {
                ReportColAlign.START -> TextAlign.Start
                ReportColAlign.CENTER -> TextAlign.Center
                ReportColAlign.END -> TextAlign.End
            }
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
                    fontSize = 8.sp,
                    fontWeight = if (bold || colIdx <= 1) FontWeight.Bold else FontWeight.Normal,
                    color = Color.Black,
                    textAlign = align,
                    lineHeight = 10.5.sp
                )
            }
        }
    }
}

@Composable
private fun PrintableSignaturesView(
    title: String,
    signers: List<Triple<String, String, String>>
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Text(
            text = title,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(6.dp))
        signers.forEach { (role, sigLine, person) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = role,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    modifier = Modifier.weight(1.3f)
                )
                Text(
                    text = sigLine,
                    fontSize = 7.5.sp,
                    color = Color(0xFF475569),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(0.9f)
                )
                Text(
                    text = person,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1E293B),
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1.3f)
                )
            }
        }
    }
}

private fun shareReportText(context: Context, content: String, title: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, content)
        putExtra(Intent.EXTRA_TITLE, title)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Отправить ведомость / таблицу")
    context.startActivity(shareIntent)
}
