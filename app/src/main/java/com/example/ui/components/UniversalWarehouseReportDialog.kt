package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.TableChart
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.InventoryItem
import com.example.data.model.OperationItemEntry
import com.example.data.model.OperationRecord
import com.example.data.model.OperationType
import com.example.data.model.StockRecord
import com.example.data.model.WarehousePoint
import com.example.universal.WarehouseProfileCatalog
import com.example.util.ExcelExportHelper
import com.example.util.ExcelReportData
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ReportInk = Color(0xFF101828)
private val ReportMuted = Color(0xFF667085)
private val ReportPrimary = Color(0xFF4F46E5)
private val ReportGreen = Color(0xFF08783E)
private val ReportRed = Color(0xFFC4320A)
private val ReportBg = Color(0xFFF6F7FB)
private val ReportBorder = Color(0xFFE4E7EC)
private val ReportHeader = Color(0xFF172554)

private data class ReportDefinition(
    val profileId: String,
    val title: String,
    val subtitle: String,
    val stockTitle: String,
    val movementTitle: String,
    val stockReference: String,
    val movementReference: String
)

private data class ReportTable(
    val headers: List<String>,
    val widths: List<Dp>,
    val rows: List<List<String>>
)

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
    val definition = remember(profile.id) { reportDefinition(profile.id) }
    var tab by remember { mutableIntStateOf(0) }

    val stockByItem = remember(stockRecords, warehouse.id) {
        stockRecords
            .filter { it.pointId == warehouse.id }
            .associateBy { it.itemId }
    }
    val allowedItems = remember(catalogItems) { catalogItems.associateBy { it.id } }

    val stockTable = remember(profile.id, catalogItems, stockByItem) {
        buildStockTable(profile.id, catalogItems, stockByItem)
    }
    val movementTable = remember(
        profile.id,
        warehouse.id,
        warehouse.name,
        operations,
        stockByItem,
        allowedItems
    ) {
        buildMovementTable(
            profileId = profile.id,
            warehouse = warehouse,
            operations = operations,
            stockByItem = stockByItem,
            allowedItems = allowedItems
        )
    }

    val activeTable = if (tab == 0) stockTable else movementTable
    val activeTitle = if (tab == 0) definition.stockTitle else definition.movementTitle
    val activeReference = if (tab == 0) definition.stockReference else definition.movementReference
    val formatter = remember { SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.96f)
                .padding(horizontal = 7.dp, vertical = 8.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFFEEF2FF), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = null,
                            tint = ReportPrimary
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = definition.title,
                            color = ReportInk,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = warehouse.name + " • " + profile.title,
                            color = ReportMuted,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = ReportMuted)
                    }
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = definition.subtitle,
                    color = ReportMuted,
                    fontSize = 9.5.sp,
                    lineHeight = 13.sp
                )

                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    ReportTabButton(
                        title = "Остатки",
                        selected = tab == 0,
                        modifier = Modifier.weight(1f)
                    ) { tab = 0 }
                    ReportTabButton(
                        title = "Движение",
                        selected = tab == 1,
                        modifier = Modifier.weight(1f)
                    ) { tab = 1 }
                }

                Spacer(Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ReportBg, RoundedCornerShape(14.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = activeTitle,
                        color = ReportInk,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = activeReference,
                        color = ReportPrimary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Предварительный просмотр таблицы",
                    color = ReportInk,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (activeTable.rows.isEmpty())
                        "По выбранному складу пока нет данных."
                    else
                        "Показаны только данные выбранного склада. Чужие профили сюда не попадают.",
                    color = ReportMuted,
                    fontSize = 8.8.sp
                )

                Spacer(Modifier.height(6.dp))

                ReportTablePreview(
                    table = activeTable,
                    modifier = Modifier.weight(1f)
                )

                Spacer(Modifier.height(10.dp))

                Button(
                    onClick = {
                        val data = ExcelReportData(
                            sheetName = safeSheetName(activeTitle),
                            title = activeTitle.uppercase(),
                            subtitle = warehouse.name + " • " + profile.title,
                            details = listOf(
                                "Тип склада" to profile.title,
                                "Склад" to warehouse.name,
                                "Ключ синхронизации" to warehouse.syncKey.ifBlank { "—" },
                                "Основа формы" to activeReference,
                                "Сформировано" to formatter.format(Date())
                            ),
                            headers = activeTable.headers,
                            colWidthsChars = activeTable.widths.map { width ->
                                (width.value / 6.4).coerceIn(9.0, 44.0)
                            },
                            rows = activeTable.rows
                        )
                        val bytes = ExcelExportHelper.generateXlsxBytes(data)
                        val fileName = safeFileName(
                            profile.title + "_" + warehouse.name + "_" +
                                if (tab == 0) "остатки" else "движение"
                        ) + ".xlsx"
                        val file = ExcelExportHelper.saveXlsxToDownloads(
                            context,
                            fileName,
                            bytes
                        )
                        ExcelExportHelper.shareOrOpenExcel(context, file, fileName)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ReportPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = "Скачать эту таблицу Excel",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportTabButton(
    title: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(
                if (selected) ReportPrimary else ReportBg,
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
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
private fun ReportTablePreview(
    table: ReportTable,
    modifier: Modifier = Modifier
) {
    val horizontal = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ReportBg, RoundedCornerShape(16.dp))
            .padding(6.dp)
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(horizontal)
                .background(ReportHeader, RoundedCornerShape(10.dp))
                .padding(vertical = 8.dp)
        ) {
            table.headers.forEachIndexed { index, header ->
                ReportCell(
                    value = header,
                    width = table.widths.getOrElse(index) { 100.dp },
                    color = Color.White,
                    bold = true
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        if (table.rows.isEmpty()) {
            Text(
                text = "Нет данных для этой таблицы.",
                color = ReportMuted,
                fontSize = 10.sp,
                modifier = Modifier.padding(12.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                itemsIndexed(table.rows, key = { index, row ->
                    index.toString() + row.joinToString("|").take(80)
                }) { index, row ->
                    Row(
                        modifier = Modifier
                            .horizontalScroll(horizontal)
                            .background(
                                if (index % 2 == 0) Color.White else Color(0xFFFAFBFC),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(vertical = 7.dp)
                    ) {
                        table.headers.indices.forEach { column ->
                            ReportCell(
                                value = row.getOrElse(column) { "" },
                                width = table.widths.getOrElse(column) { 100.dp },
                                color = when {
                                    table.headers[column].contains("Остат", ignoreCase = true) ->
                                        ReportInk
                                    table.headers[column].contains("Приход", ignoreCase = true) ->
                                        ReportGreen
                                    table.headers[column].contains("Расход", ignoreCase = true) ||
                                        table.headers[column].contains("Продаж", ignoreCase = true) ->
                                        ReportRed
                                    else -> ReportInk
                                },
                                bold = table.headers[column].contains("Остат", ignoreCase = true)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportCell(
    value: String,
    width: Dp,
    color: Color = ReportInk,
    bold: Boolean = false
) {
    Text(
        text = value.ifBlank { "—" },
        modifier = Modifier
            .width(width)
            .padding(horizontal = 6.dp),
        color = color,
        fontSize = 8.8.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
    )
}

private fun reportDefinition(profileId: String): ReportDefinition = when (profileId) {
    "retail" -> ReportDefinition(
        profileId = profileId,
        title = "Отчёты магазина",
        subtitle = "Товарные остатки и движение выбранного магазина. Военные и универсальные позиции сюда не включаются.",
        stockTitle = "Ведомость товарных остатков",
        movementTitle = "Журнал движения товаров",
        stockReference = "Торговый склад • количественный остаток",
        movementReference = "На основе ТОРГ-18 • ОКУД 0330218"
    )
    "military" -> ReportDefinition(
        profileId = profileId,
        title = "Воинский учёт",
        subtitle = "Отдельный отчёт выбранного военного склада: службы, группы, имущество и движение.",
        stockTitle = "Ведомость наличия по службам",
        movementTitle = "Книга наличия и движения",
        stockReference = "Военный склад • количественный учёт по службам",
        movementReference = "Структура книги учёта наличия и движения • форма 18"
    )
    else -> ReportDefinition(
        profileId = "universal",
        title = "Отчёты склада",
        subtitle = "Материалы и имущество только выбранного универсального склада.",
        stockTitle = "Ведомость остатков ТМЦ",
        movementTitle = "Отчёт о движении ТМЦ",
        stockReference = "Количественный складской учёт",
        movementReference = "На основе МХ-20 • ОКУД 0335020"
    )
}

private fun buildStockTable(
    profileId: String,
    items: List<InventoryItem>,
    stockByItem: Map<String, StockRecord>
): ReportTable {
    val rows = items
        .mapNotNull { item ->
            val stock = stockByItem[item.id] ?: return@mapNotNull null
            if (stock.quantity == 0 && stock.incomeTotal == 0 && stock.expenseTotal == 0) {
                return@mapNotNull null
            }

            when (profileId) {
                "military" -> listOf(
                    item.serviceCategory,
                    item.subType,
                    item.name,
                    item.unit,
                    stock.incomeTotal.toString(),
                    stock.expenseTotal.toString(),
                    stock.quantity.toString()
                )
                "retail" -> listOf(
                    item.serviceCategory,
                    item.subType,
                    item.name,
                    item.unit,
                    stock.incomeTotal.toString(),
                    stock.expenseTotal.toString(),
                    stock.quantity.toString()
                )
                else -> listOf(
                    item.serviceCategory,
                    item.subType,
                    item.name,
                    item.unit,
                    openingBalance(stock).toString(),
                    stock.incomeTotal.toString(),
                    stock.expenseTotal.toString(),
                    stock.quantity.toString()
                )
            }
        }
        .sortedBy { row -> row.joinToString("|") }

    return when (profileId) {
        "military" -> ReportTable(
            headers = listOf(
                "Служба",
                "Вид / группа",
                "Наименование имущества",
                "Ед.",
                "Приход",
                "Расход",
                "Остаток"
            ),
            widths = listOf(150.dp, 145.dp, 220.dp, 55.dp, 70.dp, 70.dp, 78.dp),
            rows = rows
        )
        "retail" -> ReportTable(
            headers = listOf(
                "Раздел",
                "Группа",
                "Товар",
                "Ед.",
                "Приход",
                "Продажа / расход",
                "Остаток"
            ),
            widths = listOf(135.dp, 130.dp, 210.dp, 55.dp, 70.dp, 95.dp, 78.dp),
            rows = rows
        )
        else -> ReportTable(
            headers = listOf(
                "Категория",
                "Группа",
                "Наименование",
                "Ед.",
                "Остаток на начало",
                "Приход",
                "Расход",
                "Остаток"
            ),
            widths = listOf(135.dp, 130.dp, 210.dp, 55.dp, 100.dp, 70.dp, 70.dp, 78.dp),
            rows = rows
        )
    }
}

private fun buildMovementTable(
    profileId: String,
    warehouse: WarehousePoint,
    operations: List<OperationRecord>,
    stockByItem: Map<String, StockRecord>,
    allowedItems: Map<String, InventoryItem>
): ReportTable {
    val formatter = SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault())
    val balanceByItem = mutableMapOf<String, Int>()

    allowedItems.values.forEach { item ->
        val stock = stockByItem[item.id]
        if (stock != null) {
            balanceByItem[item.id] = openingBalance(stock)
        }
    }

    val rows = mutableListOf<List<String>>()

    operations
        .sortedBy { it.timestamp }
        .forEach { operation ->
            val entries = parseOperationItems(operation)
                .filter { it.itemId in allowedItems.keys }

            entries.forEach { entry ->
                val current = balanceByItem[entry.itemId] ?: 0
                val movement = movementForWarehouse(
                    operation = operation,
                    warehouse = warehouse,
                    currentBalance = current,
                    entry = entry
                )
                balanceByItem[entry.itemId] = movement.balance

                val item = allowedItems[entry.itemId]
                val itemName = item?.name ?: entry.itemName
                val unit = item?.unit ?: entry.unit
                val document = operation.docNumber.ifBlank {
                    operation.type.titleRu
                }
                val note = operation.comment.ifBlank { entry.reason }

                when (profileId) {
                    "retail" -> rows += listOf(
                        formatter.format(Date(operation.timestamp)),
                        document,
                        itemName + " (" + unit + ")",
                        movement.income.ifZeroDash(),
                        movement.expense.ifZeroDash(),
                        movement.balance.toString(),
                        note
                    )
                    "military" -> rows += listOf(
                        formatter.format(Date(operation.timestamp)),
                        document,
                        counterparty(operation, warehouse),
                        itemName + " (" + unit + ")",
                        movement.income.ifZeroDash(),
                        movement.expense.ifZeroDash(),
                        movement.balance.toString()
                    )
                    else -> rows += listOf(
                        formatter.format(Date(operation.timestamp)),
                        operation.type.titleRu,
                        operation.fromPointName,
                        operation.toPointName,
                        itemName + " (" + unit + ")",
                        movement.income.ifZeroDash(),
                        movement.expense.ifZeroDash(),
                        movement.balance.toString()
                    )
                }
            }
        }

    return when (profileId) {
        "retail" -> ReportTable(
            headers = listOf(
                "Дата",
                "Документ",
                "Товар",
                "Приход",
                "Расход",
                "Остаток",
                "Примечание"
            ),
            widths = listOf(110.dp, 110.dp, 220.dp, 70.dp, 70.dp, 78.dp, 190.dp),
            rows = rows
        )
        "military" -> ReportTable(
            headers = listOf(
                "Дата",
                "Документ",
                "От кого / кому",
                "Имущество",
                "Приход",
                "Расход",
                "Остаток"
            ),
            widths = listOf(110.dp, 110.dp, 170.dp, 220.dp, 70.dp, 70.dp, 78.dp),
            rows = rows
        )
        else -> ReportTable(
            headers = listOf(
                "Дата / время",
                "Операция",
                "Откуда",
                "Куда",
                "Позиция",
                "Приход",
                "Расход",
                "Остаток"
            ),
            widths = listOf(115.dp, 105.dp, 145.dp, 145.dp, 220.dp, 70.dp, 70.dp, 78.dp),
            rows = rows
        )
    }
}

private data class WarehouseMovement(
    val income: Int,
    val expense: Int,
    val balance: Int
)

private fun movementForWarehouse(
    operation: OperationRecord,
    warehouse: WarehousePoint,
    currentBalance: Int,
    entry: OperationItemEntry
): WarehouseMovement {
    if (operation.type == OperationType.CORRECTION) {
        val corrected = correctionBalance(operation, currentBalance)
        return if (corrected >= currentBalance) {
            WarehouseMovement(
                income = corrected - currentBalance,
                expense = 0,
                balance = corrected
            )
        } else {
            WarehouseMovement(
                income = 0,
                expense = currentBalance - corrected,
                balance = corrected
            )
        }
    }

    val isFrom = operation.fromPointId == warehouse.id ||
        (operation.fromPointId.isBlank() && operation.fromPointName == warehouse.name)
    val isTo = operation.toPointId == warehouse.id ||
        (operation.toPointId.isBlank() && operation.toPointName == warehouse.name)

    val income = when (operation.type) {
        OperationType.INCOME -> if (isTo) entry.quantity else 0
        OperationType.TRANSFER -> if (isTo) entry.quantity else 0
        else -> 0
    }
    val expense = when (operation.type) {
        OperationType.TRANSFER -> if (isFrom) entry.quantity else 0
        OperationType.ISSUE,
        OperationType.EXPENDITURE -> if (isFrom) entry.quantity else 0
        else -> 0
    }

    return WarehouseMovement(
        income = income,
        expense = expense,
        balance = currentBalance + income - expense
    )
}

private fun correctionBalance(
    operation: OperationRecord,
    fallback: Int
): Int {
    val regex = Regex("→\\s*(\\d+)")
    val source = operation.itemsSummary + " " + operation.comment
    return regex.find(source)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: fallback
}

private fun counterparty(
    operation: OperationRecord,
    warehouse: WarehousePoint
): String {
    val isFrom = operation.fromPointId == warehouse.id ||
        (operation.fromPointId.isBlank() && operation.fromPointName == warehouse.name)

    return if (isFrom) {
        operation.toPointName.ifBlank { "Получатель" }
    } else {
        operation.fromPointName.ifBlank { "Поставщик" }
    }
}

private fun parseOperationItems(
    operation: OperationRecord
): List<OperationItemEntry> {
    if (operation.itemsJson.isBlank()) return emptyList()

    return try {
        val array = JSONArray(operation.itemsJson)
        buildList {
            for (index in 0 until array.length()) {
                val obj = array.getJSONObject(index)
                add(
                    OperationItemEntry(
                        itemId = obj.optString("itemId", ""),
                        itemName = obj.optString("itemName", ""),
                        unit = obj.optString("unit", ""),
                        quantity = obj.optInt("quantity", 0),
                        categoryClass = obj.optString("categoryClass", ""),
                        reason = obj.optString("reason", "")
                    )
                )
            }
        }
    } catch (_: Throwable) {
        emptyList()
    }
}

private fun openingBalance(stock: StockRecord): Int =
    stock.quantity - stock.incomeTotal + stock.expenseTotal

private fun Int.ifZeroDash(): String =
    if (this == 0) "—" else toString()

private fun safeSheetName(value: String): String =
    value.replace(Regex("[\\/:?*\\[\\]]"), " ").take(31)

private fun safeFileName(value: String): String =
    value.replace(Regex("[^A-Za-zА-Яа-я0-9._-]+"), "_").take(80)
