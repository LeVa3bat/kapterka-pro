package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.InventoryItem
import com.example.data.model.OperationItemEntry
import com.example.data.model.StockRecord
import com.example.data.model.WarehousePoint
import com.example.data.model.UserProfile
import com.example.ui.theme.SageGreenBright
import com.example.ui.theme.SageGreenContainer
import com.example.ui.theme.SageGreenDark
import com.example.ui.theme.SageGreenPrimary
import com.example.ui.theme.TacticalBg
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalBorderSubtle
import com.example.ui.theme.TacticalGold
import com.example.ui.theme.TacticalGoldDark
import com.example.ui.theme.TacticalGoldText
import com.example.ui.theme.TacticalRed
import com.example.ui.theme.TacticalRedDark
import com.example.ui.theme.TacticalRedText
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalSurfaceLight
import com.example.ui.theme.TacticalTeal
import com.example.ui.theme.TacticalTealDark
import com.example.ui.theme.TacticalTealText
import com.example.ui.theme.TacticalTextDim
import com.example.ui.theme.TacticalTextMuted
import com.example.ui.theme.TacticalTextPrimary
import com.example.ui.theme.TacticalTextSecondary

class OperationDraftItem(
    selectedItem: InventoryItem? = null,
    quantityString: String = "1",
    reasonString: String = ""
) {
    var selectedItem by mutableStateOf(selectedItem)
    var quantityString by mutableStateOf(quantityString)
    var reasonString by mutableStateOf(reasonString)
}

// DIALOG: «Привезли» (Income)
@Composable
fun IncomeOperationDialog(
    profile: UserProfile?,
    points: List<WarehousePoint>,
    catalogItems: List<InventoryItem>,
    stockRecords: List<StockRecord> = emptyList(),
    initialPointId: String,
    onDismiss: () -> Unit,
    onConfirm: (toPointId: String, toPointName: String, supplier: String, items: List<OperationItemEntry>, comment: String) -> Unit
) {
    var selectedPoint by remember {
        mutableStateOf(points.firstOrNull { it.id == initialPointId } ?: points.firstOrNull() ?: WarehousePoint("base", "Базовый склад"))
    }
    var supplier by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    val draftItems = remember {
        mutableStateListOf(OperationDraftItem(selectedItem = null, quantityString = "1"))
    }

    val supplierSuggestions = listOf(
        "Служба РАВ / Тыл",
        "Волонтёрская помощь",
        "Командование бригады",
        "Центральная база хранения",
        "Соседнее подразделение",
        "Трофейное имущество"
    )

    TacticalOperationModalLayout(
        title = "Операция «Привезли»",
        titleColor = SageGreenBright,
        badgeColor = SageGreenDark,
        onDismiss = onDismiss
    ) {
        // Destination point searchable selector with live stock summary
        TacticalSearchablePointDropdown(
            label = "Куда (Точка / Склад назначения)",
            points = points,
            selectedPoint = selectedPoint,
            stockRecords = stockRecords,
            catalogItems = catalogItems,
            onPointSelected = { selectedPoint = it }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Supplier searchable text field
        TacticalSearchableTextDropdown(
            label = "Откуда / Поставщик",
            value = supplier,
            onValueChange = { supplier = it },
            suggestions = supplierSuggestions,
            placeholder = "Служба РАВ, Волонтеры, База..."
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Items List with live search in row
        ItemsDraftListSection(
            draftItems = draftItems,
            catalogItems = catalogItems,
            showReasonField = false
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Comment
        TacticalInputField(
            label = "Примечание / Документ поставки",
            value = comment,
            onValueChange = { comment = it },
            placeholder = "Накладная №..., рейс, позывной"
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Save Button with guaranteed fit
        TacticalFitButton(
            text = "СОХРАНИТЬ ПРИХОД НА СКЛАД",
            icon = Icons.Default.Add,
            containerColor = SageGreenPrimary,
            contentColor = Color.White,
            onClick = {
                val validItems = draftItems.mapNotNull { draft ->
                    val item = draft.selectedItem ?: return@mapNotNull null
                    val qty = draft.quantityString.toIntOrNull() ?: 1
                    OperationItemEntry(
                        itemId = item.id,
                        itemName = item.name,
                        unit = item.unit,
                        quantity = qty,
                        categoryClass = item.categoryClass
                    )
                }
                if (validItems.isNotEmpty()) {
                    onConfirm(selectedPoint.id, selectedPoint.name, supplier, validItems, comment)
                    onDismiss()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("submit_income_button")
        )
    }
}

// DIALOG: «Перемещение» (Transfer)
@Composable
fun TransferOperationDialog(
    points: List<WarehousePoint>,
    catalogItems: List<InventoryItem>,
    stockRecords: List<StockRecord> = emptyList(),
    initialPointId: String,
    onDismiss: () -> Unit,
    onConfirm: (fromPointId: String, fromPointName: String, toPointId: String, toPointName: String, items: List<OperationItemEntry>, comment: String) -> Unit
) {
    var fromPoint by remember {
        mutableStateOf(points.firstOrNull { it.id == initialPointId } ?: points.firstOrNull() ?: WarehousePoint("base", "Базовый склад"))
    }
    var toPoint by remember {
        mutableStateOf(points.firstOrNull { it.id != fromPoint.id } ?: points.firstOrNull() ?: WarehousePoint("op_skala", "ОП «Скала»"))
    }
    var comment by remember { mutableStateOf("") }
    val draftItems = remember {
        mutableStateListOf(OperationDraftItem(selectedItem = null, quantityString = "1"))
    }

    var pendingInsufficientItems by remember { mutableStateOf<List<String>?>(null) }

    // Filter items to ONLY those present on the source point
    val fromPointStocks = remember(fromPoint, stockRecords) {
        stockRecords.filter { it.pointId == fromPoint.id && it.quantity > 0 }
    }
    val fromPointItemIds = remember(fromPointStocks) {
        fromPointStocks.map { it.itemId }.toSet()
    }
    val availableItemsOnPoint = remember(catalogItems, fromPointItemIds) {
        catalogItems.filter { it.id in fromPointItemIds }
    }
    val stocksMapForFromPoint = remember(fromPoint, stockRecords) {
        stockRecords.filter { it.pointId == fromPoint.id }.associate { it.itemId to it.quantity }
    }

    val driverSuggestions = listOf(
        "Водитель дежурного УРАЛа",
        "Личный состав взвода обеспечения",
        "Старшина роты",
        "Пешая группа переноски"
    )

    if (pendingInsufficientItems != null) {
        InsufficientStockAlertDialog(
            pointName = fromPoint.name,
            insufficientItems = pendingInsufficientItems!!,
            onDismiss = { pendingInsufficientItems = null },
            onProceedAnyway = {
                val validItems = draftItems.mapNotNull { draft ->
                    val item = draft.selectedItem ?: return@mapNotNull null
                    val qty = draft.quantityString.toIntOrNull() ?: 1
                    OperationItemEntry(
                        itemId = item.id,
                        itemName = item.name,
                        unit = item.unit,
                        quantity = qty,
                        categoryClass = item.categoryClass
                    )
                }
                if (validItems.isNotEmpty()) {
                    onConfirm(fromPoint.id, fromPoint.name, toPoint.id, toPoint.name, validItems, comment)
                    pendingInsufficientItems = null
                    onDismiss()
                }
            }
        )
    }

    TacticalOperationModalLayout(
        title = "Операция «Перемещение»",
        titleColor = TacticalTealText,
        badgeColor = TacticalTealDark,
        onDismiss = onDismiss
    ) {
        // From Point
        TacticalSearchablePointDropdown(
            label = "Откуда (Точка списания)",
            points = points,
            selectedPoint = fromPoint,
            stockRecords = stockRecords,
            catalogItems = catalogItems,
            onPointSelected = { 
                fromPoint = it
                // Clear any drafts if they are no longer on new point
                draftItems.forEach { draft ->
                    if (draft.selectedItem != null && draft.selectedItem!!.id !in stockRecords.filter { s -> s.pointId == it.id && s.quantity > 0 }.map { s -> s.itemId }) {
                        draft.selectedItem = null
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // To Point
        TacticalSearchablePointDropdown(
            label = "Куда (Точка зачисления)",
            points = points,
            selectedPoint = toPoint,
            stockRecords = stockRecords,
            catalogItems = catalogItems,
            onPointSelected = { toPoint = it }
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (availableItemsOnPoint.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(TacticalRedDark.copy(alpha = 0.4f))
                    .border(1.dp, TacticalRed.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "⚠️ На точке «${fromPoint.name}» нет имущества на остатке для перемещения (0 ед.). Выберите другую точку списания.",
                    color = TacticalRedText,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Items Draft List filtered strictly to items available on fromPoint
        ItemsDraftListSection(
            draftItems = draftItems,
            catalogItems = availableItemsOnPoint,
            availableStocksMap = stocksMapForFromPoint,
            showReasonField = false
        )

        Spacer(modifier = Modifier.height(12.dp))

        TacticalSearchableTextDropdown(
            label = "Ответственный за доставку / Примечание",
            value = comment,
            onValueChange = { comment = it },
            suggestions = driverSuggestions,
            placeholder = "Позывной водителя, время перемещения"
        )

        Spacer(modifier = Modifier.height(18.dp))

        TacticalFitButton(
            text = "ВЫПОЛНИТЬ ПЕРЕМЕЩЕНИЕ",
            icon = Icons.Default.ArrowDropDown,
            containerColor = TacticalTeal,
            contentColor = Color.White,
            onClick = {
                val validItems = draftItems.mapNotNull { draft ->
                    val item = draft.selectedItem ?: return@mapNotNull null
                    val qty = draft.quantityString.toIntOrNull() ?: 1
                    OperationItemEntry(
                        itemId = item.id,
                        itemName = item.name,
                        unit = item.unit,
                        quantity = qty,
                        categoryClass = item.categoryClass
                    )
                }
                if (validItems.isNotEmpty()) {
                    // Check for insufficient items
                    val insufficient = validItems.mapNotNull { entry ->
                        val available = stocksMapForFromPoint[entry.itemId] ?: 0
                        if (entry.quantity > available) {
                            "${entry.itemName}: в наличии $available ${entry.unit}, указано: ${entry.quantity} ${entry.unit} (нехватка ${entry.quantity - available} ${entry.unit})"
                        } else null
                    }
                    if (insufficient.isNotEmpty()) {
                        pendingInsufficientItems = insufficient
                    } else {
                        onConfirm(fromPoint.id, fromPoint.name, toPoint.id, toPoint.name, validItems, comment)
                        onDismiss()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("submit_transfer_button")
        )
    }
}

// DIALOG: «Подняли» (Issue)
@Composable
fun IssueOperationDialog(
    points: List<WarehousePoint>,
    catalogItems: List<InventoryItem>,
    stockRecords: List<StockRecord> = emptyList(),
    initialPointId: String,
    onDismiss: () -> Unit,
    onConfirm: (fromPointId: String, fromPointName: String, toPointId: String, toPointName: String, items: List<OperationItemEntry>, comment: String) -> Unit
) {
    var fromPoint by remember {
        mutableStateOf(points.firstOrNull { it.id == initialPointId } ?: points.firstOrNull() ?: WarehousePoint("base", "Базовый склад"))
    }
    var targetPoint by remember {
        mutableStateOf(points.firstOrNull { it.id != initialPointId } ?: points.firstOrNull() ?: WarehousePoint("base", "Базовый склад"))
    }
    var comment by remember { mutableStateOf("") }
    val draftItems = remember {
        mutableStateListOf(OperationDraftItem(selectedItem = null, quantityString = "1"))
    }

    var pendingInsufficientItems by remember { mutableStateOf<List<String>?>(null) }

    // Filter items to ONLY those present on the source point
    val fromPointStocks = remember(fromPoint, stockRecords) {
        stockRecords.filter { it.pointId == fromPoint.id && it.quantity > 0 }
    }
    val fromPointItemIds = remember(fromPointStocks) {
        fromPointStocks.map { it.itemId }.toSet()
    }
    val availableItemsOnPoint = remember(catalogItems, fromPointItemIds) {
        catalogItems.filter { it.id in fromPointItemIds }
    }
    val stocksMapForFromPoint = remember(fromPoint, stockRecords) {
        stockRecords.filter { it.pointId == fromPoint.id }.associate { it.itemId to it.quantity }
    }

    if (pendingInsufficientItems != null) {
        InsufficientStockAlertDialog(
            pointName = fromPoint.name,
            insufficientItems = pendingInsufficientItems!!,
            onDismiss = { pendingInsufficientItems = null },
            onProceedAnyway = {
                val validItems = draftItems.mapNotNull { draft ->
                    val item = draft.selectedItem ?: return@mapNotNull null
                    val qty = draft.quantityString.toIntOrNull() ?: 1
                    OperationItemEntry(
                        itemId = item.id,
                        itemName = item.name,
                        unit = item.unit,
                        quantity = qty,
                        categoryClass = item.categoryClass
                    )
                }
                if (validItems.isNotEmpty()) {
                    onConfirm(fromPoint.id, fromPoint.name, targetPoint.id, targetPoint.name, validItems, comment)
                    pendingInsufficientItems = null
                    onDismiss()
                }
            }
        )
    }

    TacticalOperationModalLayout(
        title = "Операция «Подняли» (Выдача)",
        titleColor = TacticalGoldText,
        badgeColor = TacticalGoldDark,
        onDismiss = onDismiss
    ) {
        TacticalSearchablePointDropdown(
            label = "Откуда выдано (Точка списания)",
            points = points,
            selectedPoint = fromPoint,
            stockRecords = stockRecords,
            catalogItems = catalogItems,
            onPointSelected = { 
                fromPoint = it
                draftItems.forEach { draft ->
                    if (draft.selectedItem != null && draft.selectedItem!!.id !in stockRecords.filter { s -> s.pointId == it.id && s.quantity > 0 }.map { s -> s.itemId }) {
                        draft.selectedItem = null
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        TacticalSearchablePointDropdown(
            label = "Кому / Куда подняли (Точка получения)",
            points = points,
            selectedPoint = targetPoint,
            stockRecords = stockRecords,
            catalogItems = catalogItems,
            onPointSelected = { targetPoint = it }
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (availableItemsOnPoint.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(TacticalRedDark.copy(alpha = 0.4f))
                    .border(1.dp, TacticalRed.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "⚠️ На точке «${fromPoint.name}» нет имущества на остатке для выдачи (0 ед.). Выберите другую точку списания.",
                    color = TacticalRedText,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        ItemsDraftListSection(
            draftItems = draftItems,
            catalogItems = availableItemsOnPoint,
            availableStocksMap = stocksMapForFromPoint,
            showReasonField = false
        )

        Spacer(modifier = Modifier.height(12.dp))

        TacticalInputField(
            label = "Примечание",
            value = comment,
            onValueChange = { comment = it },
            placeholder = "Введите название (примечание)"
        )

        Spacer(modifier = Modifier.height(18.dp))

        TacticalFitButton(
            text = "ЗАФИКСИРОВАТЬ ВЫДАЧУ",
            containerColor = TacticalGold,
            contentColor = Color.White,
            onClick = {
                val validItems = draftItems.mapNotNull { draft ->
                    val item = draft.selectedItem ?: return@mapNotNull null
                    val qty = draft.quantityString.toIntOrNull() ?: 1
                    OperationItemEntry(
                        itemId = item.id,
                        itemName = item.name,
                        unit = item.unit,
                        quantity = qty,
                        categoryClass = item.categoryClass
                    )
                }
                if (validItems.isNotEmpty()) {
                    val insufficient = validItems.mapNotNull { entry ->
                        val available = stocksMapForFromPoint[entry.itemId] ?: 0
                        if (entry.quantity > available) {
                            "${entry.itemName}: в наличии $available ${entry.unit}, указано: ${entry.quantity} ${entry.unit} (нехватка ${entry.quantity - available} ${entry.unit})"
                        } else null
                    }
                    if (insufficient.isNotEmpty()) {
                        pendingInsufficientItems = insufficient
                    } else {
                        onConfirm(fromPoint.id, fromPoint.name, targetPoint.id, targetPoint.name, validItems, comment)
                        onDismiss()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("submit_issue_button")
        )
    }
}

// DIALOG: «Отстрел» (Expenditure Form 8)
@Composable
fun ExpenditureOperationDialog(
    profile: UserProfile?,
    points: List<WarehousePoint>,
    catalogItems: List<InventoryItem>,
    stockRecords: List<StockRecord> = emptyList(),
    initialPointId: String,
    onDismiss: () -> Unit,
    onConfirm: (fromPointId: String, pointName: String, docNumber: String, responsiblePerson: String, items: List<OperationItemEntry>, comment: String) -> Unit
) {
    var fromPoint by remember {
        mutableStateOf(points.firstOrNull { it.id == initialPointId } ?: points.firstOrNull() ?: WarehousePoint("base", "Базовый склад"))
    }
    var docNumber by remember { mutableStateOf("АКТ-${(100..999).random()}") }
    var responsiblePerson by remember { mutableStateOf(profile?.callsign ?: "") }
    var comment by remember { mutableStateOf("") }
    val draftItems = remember {
        mutableStateListOf(OperationDraftItem(selectedItem = null, quantityString = "1", reasonString = ""))
    }

    var pendingInsufficientItems by remember { mutableStateOf<List<String>?>(null) }

    // Filter items to ONLY those present on the source point
    val fromPointStocks = remember(fromPoint, stockRecords) {
        stockRecords.filter { it.pointId == fromPoint.id && it.quantity > 0 }
    }
    val fromPointItemIds = remember(fromPointStocks) {
        fromPointStocks.map { it.itemId }.toSet()
    }
    val availableItemsOnPoint = remember(catalogItems, fromPointItemIds) {
        catalogItems.filter { it.id in fromPointItemIds }
    }
    val stocksMapForFromPoint = remember(fromPoint, stockRecords) {
        stockRecords.filter { it.pointId == fromPoint.id }.associate { it.itemId to it.quantity }
    }

    val responsibleSuggestions = listOf(
        "Командир огневого расчета",
        "Командир взвода",
        "Старшина роты",
        "Начальник службы РАВ",
        "Командир группы БПЛА"
    )

    if (pendingInsufficientItems != null) {
        InsufficientStockAlertDialog(
            pointName = fromPoint.name,
            insufficientItems = pendingInsufficientItems!!,
            onDismiss = { pendingInsufficientItems = null },
            onProceedAnyway = {
                val validItems = draftItems.mapNotNull { draft ->
                    val item = draft.selectedItem ?: return@mapNotNull null
                    val qty = draft.quantityString.toIntOrNull() ?: 1
                    OperationItemEntry(
                        itemId = item.id,
                        itemName = item.name,
                        unit = item.unit,
                        quantity = qty,
                        categoryClass = item.categoryClass,
                        reason = draft.reasonString
                    )
                }
                if (validItems.isNotEmpty()) {
                    onConfirm(fromPoint.id, fromPoint.name, docNumber, responsiblePerson, validItems, comment)
                    pendingInsufficientItems = null
                    onDismiss()
                }
            }
        )
    }

    TacticalOperationModalLayout(
        title = "Операция «Расход» (Форма 8)",
        titleColor = TacticalRedText,
        badgeColor = TacticalRedDark,
        onDismiss = onDismiss
    ) {
        TacticalSearchablePointDropdown(
            label = "Позиция расхода / ОП",
            points = points,
            selectedPoint = fromPoint,
            stockRecords = stockRecords,
            catalogItems = catalogItems,
            onPointSelected = { 
                fromPoint = it
                draftItems.forEach { draft ->
                    if (draft.selectedItem != null && draft.selectedItem!!.id !in stockRecords.filter { s -> s.pointId == it.id && s.quantity > 0 }.map { s -> s.itemId }) {
                        draft.selectedItem = null
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TacticalInputField(
                label = "№ Акта списания",
                value = docNumber,
                onValueChange = { docNumber = it },
                modifier = Modifier.weight(1f)
            )
            TacticalSearchableTextDropdown(
                label = "Ответственное лицо",
                value = responsiblePerson,
                onValueChange = { responsiblePerson = it },
                suggestions = responsibleSuggestions,
                placeholder = "Позывной командира",
                modifier = Modifier.weight(1.3f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Warning banner for military write-off
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(TacticalRedDark.copy(alpha = 0.5f))
                .border(1.dp, TacticalRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Внимание",
                tint = TacticalRedText,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Списанное имущество автоматически сформирует Акт Формы № 8 для службы РАВ и архива журнала.",
                color = TacticalRedText,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (availableItemsOnPoint.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(TacticalRedDark.copy(alpha = 0.4f))
                    .border(1.dp, TacticalRed.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "⚠️ На позиции «${fromPoint.name}» нет имущества на остатке для списания (0 ед.). Выберите другую позицию.",
                    color = TacticalRedText,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        ItemsDraftListSection(
            draftItems = draftItems,
            catalogItems = availableItemsOnPoint,
            availableStocksMap = stocksMapForFromPoint,
            showReasonField = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        TacticalInputField(
            label = "Примечание",
            value = comment,
            onValueChange = { comment = it },
            placeholder = "Введите название (примечание)"
        )

        Spacer(modifier = Modifier.height(18.dp))

        TacticalFitButton(
            text = "СПИСАТЬ И ПРОВЕСТИ АКТ Ф.8",
            containerColor = TacticalRed,
            contentColor = Color.White,
            onClick = {
                val validItems = draftItems.mapNotNull { draft ->
                    val item = draft.selectedItem ?: return@mapNotNull null
                    val qty = draft.quantityString.toIntOrNull() ?: 1
                    OperationItemEntry(
                        itemId = item.id,
                        itemName = item.name,
                        unit = item.unit,
                        quantity = qty,
                        categoryClass = item.categoryClass,
                        reason = draft.reasonString
                    )
                }
                if (validItems.isNotEmpty()) {
                    val insufficient = validItems.mapNotNull { entry ->
                        val available = stocksMapForFromPoint[entry.itemId] ?: 0
                        if (entry.quantity > available) {
                            "${entry.itemName}: в наличии $available ${entry.unit}, указано: ${entry.quantity} ${entry.unit} (нехватка ${entry.quantity - available} ${entry.unit})"
                        } else null
                    }
                    if (insufficient.isNotEmpty()) {
                        pendingInsufficientItems = insufficient
                    } else {
                        onConfirm(fromPoint.id, fromPoint.name, docNumber, responsiblePerson, validItems, comment)
                        onDismiss()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("submit_expenditure_button")
        )
    }
}

@Composable
private fun InsufficientStockAlertDialog(
    pointName: String,
    insufficientItems: List<String>,
    onDismiss: () -> Unit,
    onProceedAnyway: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, TacticalRed)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = TacticalRedText,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "НЕДОСТАТОЧНО ИМУЩЕСТВА",
                        color = TacticalRedText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "На точке/складе «$pointName» официально числится меньше имущества, чем указано в операции:",
                    color = TacticalTextPrimary,
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(TacticalRedDark.copy(alpha = 0.4f))
                        .padding(8.dp)
                ) {
                    insufficientItems.forEach { itemDesc ->
                        Text(
                            text = "• $itemDesc",
                            color = TacticalRedText,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TacticalFitButton(
                        text = "ИСПРАВИТЬ",
                        containerColor = TacticalSurfaceLight,
                        contentColor = TacticalTextPrimary,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                    TacticalFitButton(
                        text = "ВСЁ РАВНО ПРОВЕСТИ",
                        containerColor = TacticalRed,
                        contentColor = Color.White,
                        onClick = onProceedAnyway,
                        modifier = Modifier.weight(1.3f)
                    )
                }
            }
        }
    }
}

// SHARED SUBCOMPONENTS FOR DIALOGS
@Composable
private fun TacticalOperationModalLayout(
    title: String,
    titleColor: Color,
    badgeColor: Color,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 20.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, TacticalBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(badgeColor)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = title,
                                color = titleColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
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

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp)
                ) {
                    item {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
fun PointDropdownSelector(
    label: String,
    points: List<WarehousePoint>,
    selectedPoint: WarehousePoint,
    onPointSelected: (WarehousePoint) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, color = TacticalTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(TacticalSurfaceLight)
                .border(1.dp, TacticalBorder, RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 11.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedPoint.name,
                    color = TacticalTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Выбрать точку",
                    tint = SageGreenPrimary
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(TacticalSurfaceLight)
            ) {
                points.forEach { pt ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = pt.name,
                                color = if (pt.id == selectedPoint.id) SageGreenBright else TacticalTextPrimary,
                                fontWeight = if (pt.id == selectedPoint.id) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            onPointSelected(pt)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TacticalInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column(modifier = modifier) {
        Text(text = label, color = TacticalTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = TacticalTextDim, fontSize = 13.sp) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = TacticalSurfaceLight,
                unfocusedContainerColor = TacticalSurfaceLight,
                focusedBorderColor = SageGreenPrimary,
                unfocusedBorderColor = TacticalBorder,
                focusedTextColor = TacticalTextPrimary,
                unfocusedTextColor = TacticalTextPrimary
            )
        )
    }
}

@Composable
private fun ItemsDraftListSection(
    draftItems: MutableList<OperationDraftItem>,
    catalogItems: List<InventoryItem>,
    availableStocksMap: Map<String, Int>? = null,
    showReasonField: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ИМУЩЕСТВО И КОЛИЧЕСТВО",
                color = TacticalTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(SageGreenDark)
                    .clickable {
                        draftItems.add(OperationDraftItem(selectedItem = null, quantityString = "1"))
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Добавить",
                    tint = SageGreenBright,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "+ строка",
                    color = SageGreenBright,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        draftItems.forEachIndexed { index, draft ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = TacticalBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, TacticalBorderSubtle)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Позиция #${index + 1}",
                            color = TacticalTextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (draftItems.size > 1) {
                            IconButton(
                                onClick = { draftItems.removeAt(index) },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Удалить строку",
                                    tint = TacticalRed,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Item Searchable Autocomplete Dropdown
                    TacticalSearchableItemDropdown(
                        label = "",
                        catalogItems = catalogItems,
                        selectedItem = draft.selectedItem,
                        availableStocksMap = availableStocksMap,
                        onItemSelected = { selected ->
                            draft.selectedItem = selected
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val reasonPresets = listOf(
                        "Боевая работа (поражение целей)",
                        "Пристрелка и выверка боя",
                        "Учебные стрельбы / подготовка",
                        "Повреждение при артобстреле",
                        "Естественный износ ствола/узлов",
                        "Передача в рембат / на ТО"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = draft.quantityString,
                            onValueChange = { draft.quantityString = it },
                            label = { Text("Кол-во", color = TacticalTextSecondary, fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = TacticalSurfaceLight,
                                unfocusedContainerColor = TacticalSurfaceLight,
                                focusedBorderColor = SageGreenPrimary,
                                unfocusedBorderColor = TacticalBorder,
                                focusedTextColor = TacticalTextPrimary,
                                unfocusedTextColor = TacticalTextPrimary
                            )
                        )

                        Text(
                            text = draft.selectedItem?.unit ?: "ед.",
                            color = SageGreenBright,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        if (showReasonField) {
                            TacticalSearchableTextDropdown(
                                label = "",
                                value = draft.reasonString,
                                onValueChange = { draft.reasonString = it },
                                suggestions = reasonPresets,
                                placeholder = "Причина списания",
                                modifier = Modifier.weight(2f)
                            )
                        }
                    }

                    // Warning if quantity exceeds available stock on this point
                    val selected = draft.selectedItem
                    val available = if (selected != null) availableStocksMap?.get(selected.id) else null
                    val requestedQty = draft.quantityString.toIntOrNull() ?: 0
                    if (available != null && requestedQty > available) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(TacticalRedDark.copy(alpha = 0.45f))
                                .border(0.5.dp, TacticalRed.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = TacticalRedText,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "⚠️ На данной точке нет столько (официально)! В наличии: $available ${selected?.unit ?: "ед."}",
                                color = TacticalRedText,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
