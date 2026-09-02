package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

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
    val tabTitles = mutableListOf("Полная ведомость (Все позиции)")
    points.forEach { pt ->
        tabTitles.add("Точка: ${pt.name}")
    }
    tabTitles.add("Форма № 8 (Акт расхода)")
    tabTitles.add("Форма № 18 (Книга учета)")
    tabTitles.add("Заявки (Реестр заявок)")

    var selectedTab by remember {
        mutableIntStateOf(initialFormIndex.coerceIn(0, (tabTitles.size - 1).coerceAtLeast(0)))
    }
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.97f)
                .fillMaxHeight(0.9f)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalSurface),
            border = BorderStroke(1.dp, TacticalBorder)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ГЕНЕРАТОР ВЕДОМОСТЕЙ EXCEL",
                            color = SageGreenBright,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Строгий белый бланк • Черный шрифт • Печатная форма",
                            color = TacticalTextMuted,
                            fontSize = 11.sp
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
                Spacer(modifier = Modifier.height(10.dp))

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
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == index) SageGreenBright else TacticalTextMuted
                                )
                            }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.White)
                        .padding(8.dp)
                ) {
                    val reportText = when {
                        selectedTab == 0 -> buildFullConsolidatedText(catalogItems, stockRecords, points, operations, unitName, parseItems)
                        selectedTab in 1..points.size -> buildSinglePointText(points[selectedTab - 1], catalogItems, stockRecords.filter { it.pointId == points[selectedTab - 1].id }, unitName)
                        selectedTab == points.size + 1 -> buildForm8Text(operations, unitName, parseItems)
                        selectedTab == points.size + 2 -> buildForm18Text(operations, unitName, parseItems)
                        else -> buildRequisitionsText(requisitions, unitName)
                    }
                    
                    val lines = reportText.trim().split("\n")
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 2.dp)) {
                        items(lines.size) { index ->
                            val line = lines[index]
                            if (line.isBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                            } else if (!line.contains(";")) {
                                Text(
                                    text = line,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            } else {
                                val cols = line.split(";")
                                val isHeader = line.startsWith("№;") || line.startsWith("Дата;")
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color.LightGray)
                                        .background(if (isHeader) Color(0xFFEEEEEE) else Color.Transparent)
                                        .padding(4.dp)
                                ) {
                                    cols.forEach { col ->
                                        Text(
                                            text = col,
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(horizontal = 2.dp),
                                            fontSize = 9.sp,
                                            color = Color.Black,
                                            maxLines = 4,
                                            lineHeight = 11.sp,
                                            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                Box(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = {
                            val fileName = when {
                                selectedTab == 0 -> "Полная_ведомость.csv"
                                selectedTab in 1..points.size -> "Ведомость_точки_${points[selectedTab - 1].name.replace(" ", "_")}.csv"
                                selectedTab == points.size + 1 -> "Форма_8_Акт_расхода.csv"
                                selectedTab == points.size + 2 -> "Форма_18_Книга_учета.csv"
                                else -> "Реестр_заявок.csv"
                            }
                            val reportText = when {
                                selectedTab == 0 -> buildFullConsolidatedText(catalogItems, stockRecords, points, operations, unitName, parseItems)
                                selectedTab in 1..points.size -> buildSinglePointText(points[selectedTab - 1], catalogItems, stockRecords.filter { it.pointId == points[selectedTab - 1].id }, unitName)
                                selectedTab == points.size + 1 -> buildForm8Text(operations, unitName, parseItems)
                                selectedTab == points.size + 2 -> buildForm18Text(operations, unitName, parseItems)
                                else -> buildRequisitionsText(requisitions, unitName)
                            }
                            shareReport(context, reportText, fileName)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF107C41),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "СКАЧАТЬ / ПОДЕЛИТЬСЯ EXCEL (.TSV)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

private fun buildFullConsolidatedText(
    catalogItems: List<InventoryItem>,
    stockRecords: List<StockRecord>,
    points: List<WarehousePoint>,
    operations: List<OperationRecord>,
    unitName: String,
    parseItems: (String) -> List<OperationItemEntry>
): String {
    val sb = java.lang.StringBuilder()
    sb.append("ПОЛНАЯ СВОДНАЯ ВЕДОМОСТЬ НАЛИЧИЯ ИМУЩЕСТВА\n")
    sb.append("Подразделение:;$unitName\n\n")
    sb.append("№;Наименование имущества;Категория;Ед.изм;Приход;Расход;Остаток на точках;Всего (Сводка)\n")
    
    val itemsMap = catalogItems.associateBy { it.id }
    val grouped = stockRecords.groupBy { it.itemId }
    
    var index = 1
    for ((itemId, records) in grouped) {
        val item = itemsMap[itemId]
        val itemName = item?.name ?: itemId
        val cat = item?.serviceCategory ?: "Прочее"
        val unit = item?.unit ?: "шт."
        val totalIncome = records.sumOf { it.incomeTotal }
        val totalExpense = records.sumOf { it.expenseTotal }
        val totalStock = records.sumOf { it.quantity }
        val pointsStr = records.filter { it.quantity > 0 }.joinToString("; ") { r ->
            val pName = points.find { it.id == r.pointId }?.name ?: "Неизвестно"
            "$pName: ${r.quantity}"
        }
        sb.append("$index;$itemName;$cat;$unit;$totalIncome;$totalExpense;$pointsStr;$totalStock\n")
        index++
    }
    
    if (grouped.isEmpty()) {
        sb.append("-;Нет данных об имуществе.;-;-;0;0;-;0\n")
    }
    return sb.toString()
}

private fun buildSinglePointText(
    point: WarehousePoint,
    catalogItems: List<InventoryItem>,
    stockRecords: List<StockRecord>,
    unitName: String
): String {
    val sb = java.lang.StringBuilder()
    sb.append("ВЕДОМОСТЬ НАЛИЧИЯ ИМУЩЕСТВА НА ТОЧКЕ\n")
    sb.append("Точка:;${point.name}\n")
    sb.append("Подразделение:;$unitName\n\n")
    sb.append("№;Наименование имущества;Категория;Ед.изм;Приход;Расход;Текущий остаток;Итого по категории\n")
    
    val itemsMap = catalogItems.associateBy { it.id }
    val catTotals = mutableMapOf<String, Int>()
    
    for (r in stockRecords) {
        val it = itemsMap[r.itemId]
        val cat = it?.serviceCategory ?: "Прочее"
        catTotals[cat] = (catTotals[cat] ?: 0) + r.quantity
    }
    
    var i = 1
    for (r in stockRecords) {
        val item = itemsMap[r.itemId]
        val cat = item?.serviceCategory ?: "Прочее"
        val totalCat = catTotals[cat] ?: r.quantity
        val unit = item?.unit ?: "шт."
        val name = item?.name ?: r.itemId
        sb.append("$i;$name;$cat;$unit;${r.incomeTotal};${r.expenseTotal};${r.quantity};$totalCat $unit\n")
        i++
    }
    if (i == 1) {
        sb.append("-;На точке «${point.name}» нет имущества и операций.;-;-;0;0;0;-\n")
    }
    return sb.toString()
}

private fun buildForm8Text(
    operations: List<OperationRecord>,
    unitName: String,
    parseItems: (String) -> List<OperationItemEntry>
): String {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru"))
    val sb = java.lang.StringBuilder()
    sb.append("УТВЕРЖДАЮ\nКомандир $unitName\nАКТ СПИСАНИЯ (РАСХОДА) МАТЕРИАЛЬНЫХ ЦЕННОСТЕЙ (ФОРМА № 8)\n\n")
    sb.append("№;Наименование;Кат.;Ед.;Кол-во (Расход);Причина расхода / Боевая задача;Позиция / Документ / Дата\n")
    var index = 1
    val expOps = operations.filter { it.type == OperationType.EXPENDITURE }
    for (op in expOps) {
        val items = parseItems(op.itemsJson)
        val dateStr = dateFormat.format(Date(op.timestamp))
        for (item in items) {
            val reason = if (item.reason.isNotEmpty()) item.reason else (if (op.comment.isNotEmpty()) op.comment else "Боевая работа")
            sb.append("$index;${item.itemName};${item.categoryClass.ifEmpty { "Кат. 1" }};${item.unit};${item.quantity};$reason;${op.fromPointName} / ${op.docNumber} ($dateStr)\n")
            index++
        }
    }
    if (expOps.isEmpty()) {
        sb.append("-;Акты списания (расхода по ф.8) отсутствуют.;-;-;0;-;-\n")
    }
    return sb.toString()
}

private fun buildForm18Text(
    operations: List<OperationRecord>,
    unitName: String,
    parseItems: (String) -> List<OperationItemEntry>
): String {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru"))
    val sb = java.lang.StringBuilder()
    sb.append("КНИГА УЧЕТА НАЛИЧИЯ И ДВИЖЕНИЯ МАТЕРИАЛЬНЫХ СРЕДСТВ (ФОРМА № 18)\nПодразделение: $unitName\n\n")
    sb.append("Дата;Вид операции и документ;От кого / Кому;Имущество;Приход;Расход;Остаток\n")
    var rolling = 0
    var i = 1
    for (op in operations) {
        val dateStr = dateFormat.format(Date(op.timestamp))
        val items = parseItems(op.itemsJson)
        val total = items.sumOf { it.quantity }
        val itemsSummary = items.joinToString(", ") { "${it.itemName}: ${it.quantity} ${it.unit}" }
        val prihod = if (op.type == OperationType.INCOME) "$total" else "-"
        val rashod = if (op.type == OperationType.EXPENDITURE || op.type == OperationType.ISSUE) "$total" else "-"
        if (op.type == OperationType.INCOME) {
            rolling += total
        } else if (op.type == OperationType.EXPENDITURE || op.type == OperationType.ISSUE) {
            rolling = (rolling - total).coerceAtLeast(0)
        }
        val route = when (op.type) {
            OperationType.INCOME -> "Поставщик -> ${op.toPointName}"
            OperationType.TRANSFER -> "${op.fromPointName} -> ${op.toPointName}"
            OperationType.ISSUE -> "${op.fromPointName} -> ${op.toPointName} (Выдача)"
            OperationType.EXPENDITURE -> "${op.fromPointName} (Расход)"
        }
        sb.append("$dateStr;${op.type.titleRu} № ${op.docNumber};$route;$itemsSummary;$prihod;$rashod;$rolling\n")
        i++
    }
    if (operations.isEmpty()) {
        sb.append("-;Книга учета пуста. Операции движения не зарегистрированы.;-;-;-;-;0\n")
    }
    return sb.toString()
}

private fun buildRequisitionsText(
    requisitions: List<RequisitionRequest>,
    unitName: String
): String {
    val sb = java.lang.StringBuilder()
    sb.append("РЕЕСТР ЗАЯВОК ПОДРАЗДЕЛЕНИЯ\n")
    sb.append("Подразделение:;$unitName\n\n")
    sb.append("№;Дата/Время;От кого (Позывной);Точка назначения;Статус;Примечание;Имущество (кол-во)\n")

    val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    requisitions.sortedByDescending { it.timestamp }.forEachIndexed { index, req ->
        val statusStr = req.status.titleRu
        val dateStr = formatter.format(Date(req.timestamp))
        val commentStr = if (req.comment.isNotBlank()) req.comment else "-"
        sb.append("${index + 1};$dateStr;${req.applicantName};${req.pointName};$statusStr;$commentStr;${req.itemsSummary}\n")
    }
    return sb.toString()
}

private fun shareReport(context: Context, content: String, title: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, content)
        putExtra(Intent.EXTRA_TITLE, title)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Отправить ведомость Excel")
    context.startActivity(shareIntent)
}
