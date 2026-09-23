package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
private val ReportMuted = Color(0xFF6B7280)
private val ReportPrimary = Color(0xFF5B5CE2)
private val ReportGreen = Color(0xFF107C41)
private val ReportBg = Color(0xFFF5F7FB)

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
    val warehouseOperations = remember(operations, warehouse.id, warehouse.name) {
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
    val formatter = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 12.dp, vertical = 16.dp),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Отчёт склада",
                            color = ReportInk,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = warehouse.name + " • " + profile.title,
                            color = ReportMuted,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = ReportMuted)
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportMetric(
                        value = rows.size.toString(),
                        label = "позиций с движением",
                        modifier = Modifier.weight(1f)
                    )
                    ReportMetric(
                        value = warehouseOperations.size.toString(),
                        label = "операций",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Позиции: пришло / ушло / остаток",
                    color = ReportInk,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(ReportBg, RoundedCornerShape(18.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (rows.isEmpty()) {
                        item {
                            Text(
                                text = "По этому складу пока нет движения.",
                                color = ReportMuted,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    } else {
                        items(rows, key = { it.item.id }) { row ->
                            ReportStockRow(row)
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val data = ExcelReportData(
                                sheetName = warehouse.name.take(31),
                                title = "ВЕДОМОСТЬ ОСТАТКОВ И ДВИЖЕНИЯ",
                                subtitle = warehouse.name + " • " + profile.title,
                                details = listOf(
                                    "Код склада" to warehouse.syncKey.ifBlank { "не задан" },
                                    "Сформировано" to formatter.format(Date())
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
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ReportGreen,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(Modifier.padding(horizontal = 3.dp))
                        Text("Остатки Excel", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val data = ExcelReportData(
                                sheetName = "Операции",
                                title = "ЖУРНАЛ ДВИЖЕНИЯ",
                                subtitle = warehouse.name + " • " + profile.title,
                                details = listOf(
                                    "Код склада" to warehouse.syncKey.ifBlank { "не задан" },
                                    "Сформировано" to formatter.format(Date())
                                ),
                                headers = listOf(
                                    "№", "Дата", "Операция", "Откуда", "Куда",
                                    "Позиции", "Документ", "Комментарий"
                                ),
                                colWidthsChars = listOf(6.0, 18.0, 16.0, 22.0, 22.0, 42.0, 18.0, 34.0),
                                rows = warehouseOperations.mapIndexed { index, op ->
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
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ReportPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(Modifier.padding(horizontal = 3.dp))
                        Text("Операции Excel", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
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
private fun ReportMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(ReportBg, RoundedCornerShape(15.dp))
            .padding(11.dp)
    ) {
        Text(value, color = ReportInk, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = ReportMuted, fontSize = 9.5.sp)
    }
}

@Composable
private fun ReportStockRow(row: WarehouseReportRow) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp)) {
            Text(
                text = row.item.name,
                color = ReportInk,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = row.item.serviceCategory + " • " + row.item.subType,
                color = ReportMuted,
                fontSize = 9.sp
            )
            Spacer(Modifier.height(5.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Пришло ${row.stock.incomeTotal} ${row.item.unit}",
                    color = Color(0xFF159A72),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Ушло ${row.stock.expenseTotal} ${row.item.unit}",
                    color = Color(0xFFE88B22),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Остаток ${row.stock.quantity} ${row.item.unit}",
                    color = ReportInk,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun operationTitle(type: OperationType): String = when (type) {
    OperationType.INCOME -> "Поступление"
    OperationType.TRANSFER -> "Перемещение"
    OperationType.ISSUE -> "Выдача"
    OperationType.EXPENDITURE -> "Списание"
}

private fun safeFileName(value: String): String =
    value.replace(Regex("[^A-Za-zА-Яа-я0-9._-]+"), "_").take(80)
