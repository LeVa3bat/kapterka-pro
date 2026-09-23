package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.InventoryItem
import com.example.data.model.OperationRecord
import com.example.data.model.OperationType
import com.example.data.model.StockRecord
import com.example.data.model.WarehousePoint
import com.example.universal.WarehouseProfileCatalog
import com.example.util.ExcelExportHelper
import com.example.util.ExcelReportData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ReportInk = Color(0xFF111827)
private val ReportMuted = Color(0xFF667085)
private val ReportPrimary = Color(0xFF5B5CE2)
private val ReportGreen = Color(0xFF107C41)
private val ReportOrange = Color(0xFFE88B22)
private val ReportBg = Color(0xFFF6F7FB)
private val ReportSoft = Color(0xFFEEEEFF)

@Composable
fun UniversalWarehouseReportDialog(
    warehouse: WarehousePoint,
    catalogItems: List<InventoryItem>,
    stockRecords: List<StockRecord>,
    operations: List<OperationRecord>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val profile = WarehouseProfileCatalog.find(warehouse.profileId)
    var tab by remember { mutableIntStateOf(0) }
    var periodDays by remember { mutableStateOf<Int?>(30) }

    val stocks = remember(stockRecords, warehouse.id) {
        stockRecords.filter { it.pointId == warehouse.id }.associateBy { it.itemId }
    }
    val rows = remember(catalogItems, stocks) {
        catalogItems
            .mapNotNull { item ->
                val stock = stocks[item.id] ?: return@mapNotNull null
                if (stock.quantity == 0 && stock.incomeTotal == 0 && stock.expenseTotal == 0) {
                    null
                } else {
                    WarehouseReportRow(item, stock)
                }
            }
            .sortedWith(
                compareBy<WarehouseReportRow> { it.item.serviceCategory }
                    .thenBy { it.item.subType }
                    .thenBy { it.item.name }
            )
    }

    val allWarehouseOperations = remember(operations, warehouse.id, warehouse.name) {
        operations
            .filter { operation ->
                val hasStableIds = operation.fromPointId.isNotBlank() || operation.toPointId.isNotBlank()
                if (hasStableIds) {
                    operation.fromPointId == warehouse.id || operation.toPointId == warehouse.id
                } else {
                    operation.fromPointName == warehouse.name || operation.toPointName == warehouse.name
                }
            }
            .sortedByDescending { it.timestamp }
    }

    val periodOperations = remember(allWarehouseOperations, periodDays) {
        val days = periodDays
        if (days == null) {
            allWarehouseOperations
        } else {
            val cutoff = System.currentTimeMillis() - days * 24L * 60L * 60L * 1000L
            allWarehouseOperations.filter { it.timestamp >= cutoff }
        }
    }

    val categoryCount = rows.map { it.item.serviceCategory }.filter { it.isNotBlank() }.distinct().size
    val groupCount = rows.map { it.item.subType }.filter { it.isNotBlank() }.distinct().size
    val activeCount = rows.count { it.stock.quantity > 0 }
    val formatter = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.94f)
                .padding(horizontal = 10.dp, vertical = 12.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(15.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Отчёты",
                            color = ReportInk,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = warehouse.name + " • " + profile.title,
                            color = ReportMuted,
                            fontSize = 10.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = ReportMuted)
                    }
                }

                Spacer(Modifier.height(9.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    ReportTab(
                        title = "Остатки",
                        selected = tab == 0,
                        onClick = { tab = 0 },
                        modifier = Modifier.weight(1f)
                    )
                    ReportTab(
                        title = "Операции",
                        selected = tab == 1,
                        onClick = { tab = 1 },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    ReportMetric(activeCount.toString(), "с остатком", Modifier.weight(1f))
                    ReportMetric(categoryCount.toString(), "категорий", Modifier.weight(1f))
                    ReportMetric(groupCount.toString(), "групп", Modifier.weight(1f))
                    ReportMetric(allWarehouseOperations.size.toString(), "операций", Modifier.weight(1f))
                }

                if (tab == 1) {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        PeriodChip("7 дней", periodDays == 7) { periodDays = 7 }
                        PeriodChip("30 дней", periodDays == 30) { periodDays = 30 }
                        PeriodChip("Всё", periodDays == null) { periodDays = null }
                    }
                }

                Spacer(Modifier.height(10.dp))

                if (tab == 0) {
                    Text(
                        text = "Текущие остатки и накопленное движение",
                        color = ReportInk,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(ReportBg, RoundedCornerShape(18.dp))
                            .padding(7.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (rows.isEmpty()) {
                            item {
                                ReportEmpty("По этому складу пока нет движения.")
                            }
                        } else {
                            var lastCategory: String? = null
                            rows.forEach { row ->
                                val category = row.item.serviceCategory.ifBlank { "Без категории" }
                                if (category != lastCategory) {
                                    item(key = "cat_" + category) {
                                        Text(
                                            text = category,
                                            color = ReportPrimary,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                                        )
                                    }
                                    lastCategory = category
                                }
                                item(key = row.item.id) {
                                    CompactStockReportRow(row)
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Журнал операций • " + periodTitle(periodDays),
                        color = ReportInk,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(ReportBg, RoundedCornerShape(18.dp))
                            .padding(7.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (periodOperations.isEmpty()) {
                            item {
                                ReportEmpty("За выбранный период операций нет.")
                            }
                        } else {
                            items(periodOperations, key = { it.id }) { op ->
                                CompactOperationReportRow(op, formatter)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (tab == 0) {
                            val data = ExcelReportData(
                                sheetName = warehouse.name.take(31),
                                title = "ВЕДОМОСТЬ ОСТАТКОВ И ДВИЖЕНИЯ",
                                subtitle = warehouse.name + " • " + profile.title,
                                details = listOf(
                                    "Ключ склада" to warehouse.syncKey.ifBlank { "не задан" },
                                    "Сформировано" to formatter.format(Date()),
                                    "Позиций с движением" to rows.size.toString(),
                                    "Категорий" to categoryCount.toString(),
                                    "Групп" to groupCount.toString()
                                ),
                                headers = listOf(
                                    "№", "Категория", "Вид / группа", "Наименование",
                                    "Ед.", "Пришло", "Ушло", "Остаток"
                                ),
                                colWidthsChars = listOf(6.0, 22.0, 24.0, 34.0, 10.0, 12.0, 12.0, 12.0),
                                rows = rows.mapIndexed { index, row ->
                                    listOf(
                                        (index + 1).toString(),
                                        row.item.serviceCategory,
                                        row.item.subType,
                                        row.item.name,
                                        row.item.unit,
                                        row.stock.incomeTotal.toString(),
                                        row.stock.expenseTotal.toString(),
                                        row.stock.quantity.toString()
                                    )
                                }
                            )
                            val bytes = ExcelExportHelper.generateXlsxBytes(data)
                            val fileName = safeFileName("Остатки_" + warehouse.name) + ".xlsx"
                            val file = ExcelExportHelper.saveXlsxToDownloads(context, fileName, bytes)
                            ExcelExportHelper.shareOrOpenExcel(context, file, fileName)
                        } else {
                            val data = ExcelReportData(
                                sheetName = "Операции",
                                title = "ЖУРНАЛ ДВИЖЕНИЯ",
                                subtitle = warehouse.name + " • " + profile.title,
                                details = listOf(
                                    "Ключ склада" to warehouse.syncKey.ifBlank { "не задан" },
                                    "Период" to periodTitle(periodDays),
                                    "Сформировано" to formatter.format(Date()),
                                    "Операций" to periodOperations.size.toString()
                                ),
                                headers = listOf(
                                    "№", "Дата", "Операция", "Откуда", "Куда",
                                    "Позиции", "Документ", "Комментарий"
                                ),
                                colWidthsChars = listOf(6.0, 18.0, 16.0, 22.0, 22.0, 42.0, 18.0, 34.0),
                                rows = periodOperations.mapIndexed { index, op ->
                                    listOf(
                                        (index + 1).toString(),
                                        formatter.format(Date(op.timestamp)),
                                        operationTitle(op.type),
                                        op.fromPointName,
                                        op.toPointName,
                                        op.itemsSummary,
                                        op.docNumber,
                                        op.comment
                                    )
                                }
                            )
                            val bytes = ExcelExportHelper.generateXlsxBytes(data)
                            val fileName = safeFileName("Операции_" + warehouse.name) + ".xlsx"
                            val file = ExcelExportHelper.saveXlsxToDownloads(context, fileName, bytes)
                            ExcelExportHelper.shareOrOpenExcel(context, file, fileName)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (tab == 0) ReportGreen else ReportPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(7.dp))
                    Text(
                        text = if (tab == 0) "Скачать остатки Excel" else "Скачать операции Excel",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private data class WarehouseReportRow(
    val item: InventoryItem,
    val stock: StockRecord
)

@Composable
private fun ReportTab(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) ReportPrimary else ReportBg)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (selected) Color.White else ReportMuted,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ReportMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(ReportBg)
            .padding(horizontal = 8.dp, vertical = 9.dp)
    ) {
        Text(value, color = ReportInk, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            label,
            color = ReportMuted,
            fontSize = 7.8.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PeriodChip(title: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(if (selected) ReportSoft else ReportBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 7.dp)
    ) {
        Text(
            text = title,
            color = if (selected) ReportPrimary else ReportMuted,
            fontSize = 9.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun CompactStockReportRow(row: WarehouseReportRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(Color.White)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.item.name,
                color = ReportInk,
                fontSize = 10.8.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = row.item.subType,
                color = ReportMuted,
                fontSize = 8.2.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = row.stock.quantity.toString() + " " + row.item.unit,
                color = ReportInk,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "+" + row.stock.incomeTotal + " / -" + row.stock.expenseTotal,
                color = ReportMuted,
                fontSize = 8.sp
            )
        }
    }
}

@Composable
private fun CompactOperationReportRow(
    op: OperationRecord,
    formatter: SimpleDateFormat
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(Color.White)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = operationTitle(op.type),
                color = when (op.type) {
                    OperationType.INCOME -> ReportGreen
                    OperationType.TRANSFER -> ReportPrimary
                    OperationType.ISSUE -> ReportOrange
                    OperationType.EXPENDITURE -> Color(0xFFD94C4C)
                },
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = op.itemsSummary.ifBlank { op.fromPointName + " → " + op.toPointName },
                color = ReportInk,
                fontSize = 9.2.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = formatter.format(Date(op.timestamp)),
            color = ReportMuted,
            fontSize = 8.2.sp
        )
    }
}

@Composable
private fun ReportEmpty(text: String) {
    Text(
        text = text,
        color = ReportMuted,
        fontSize = 10.5.sp,
        modifier = Modifier.padding(12.dp)
    )
}

private fun periodTitle(days: Int?): String = when (days) {
    7 -> "последние 7 дней"
    30 -> "последние 30 дней"
    else -> "за всё время"
}

private fun operationTitle(type: OperationType): String = when (type) {
    OperationType.INCOME -> "Поступление"
    OperationType.TRANSFER -> "Перемещение"
    OperationType.ISSUE -> "Выдача"
    OperationType.EXPENDITURE -> "Списание"
}

private fun safeFileName(value: String): String =
    value.replace(Regex("[^A-Za-zА-Яа-я0-9._-]+"), "_").take(80)
