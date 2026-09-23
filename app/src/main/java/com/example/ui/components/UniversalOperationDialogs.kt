package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.InventoryItem
import com.example.data.model.OperationItemEntry
import com.example.data.model.StockRecord
import com.example.data.model.UserProfile
import com.example.data.model.WarehousePoint
import com.example.universal.WarehouseProfileCatalog

private val UOpPrimary = Color(0xFF5B5CE2)
private val UOpInk = Color(0xFF111827)
private val UOpMuted = Color(0xFF667085)
private val UOpBorder = Color(0xFFE3E7EE)
private val UOpSoft = Color(0xFFF5F6FF)
private val UOpBg = Color(0xFFF5F7FB)
private val UOpGreen = Color(0xFF159A72)
private val UOpGreenSoft = Color(0xFFE9F8F3)
private val UOpOrange = Color(0xFFE88B22)
private val UOpOrangeSoft = Color(0xFFFFF4E5)
private val UOpRed = Color(0xFFB42318)
private val UOpRedSoft = Color(0xFFFFEEEE)
private val UOpBlue = Color(0xFF3972D7)
private val UOpBlueSoft = Color(0xFFEEF4FF)

private class UniversalOperationDraft(
    item: InventoryItem? = null,
    quantity: String = "1",
    reason: String = ""
) {
    var item by mutableStateOf(item)
    var quantity by mutableStateOf(quantity)
    var reason by mutableStateOf(reason)
}

@Composable
fun UniversalIncomeOperationDialog(
    profile: UserProfile?,
    points: List<WarehousePoint>,
    catalogItems: List<InventoryItem>,
    stockRecords: List<StockRecord> = emptyList(),
    initialPointId: String,
    warehouseProfileId: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, List<OperationItemEntry>, String) -> Unit
) {
    val warehouseProfile = WarehouseProfileCatalog.find(warehouseProfileId)
    val isMilitary = warehouseProfile.id == "military"
    var selectedPoint by remember(points, initialPointId) {
        mutableStateOf(
            points.firstOrNull { it.id == initialPointId }
                ?: points.firstOrNull()
                ?: WarehousePoint("base", "Основной склад")
        )
    }
    var supplier by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    val drafts = remember { mutableStateListOf(UniversalOperationDraft()) }
    val suggestions = if (isMilitary) {
        listOf("Тыл / служба снабжения", "Центральная база хранения", "Соседнее подразделение", "Волонтёрская помощь")
    } else {
        listOf("Поставщик", "Производитель", "Центральный склад", "Другой склад", "Возврат", "Собственное производство")
    }

    UniversalOperationShell(
        title = warehouseProfile.operations.income,
        subtitle = "Добавьте поступившие позиции на выбранный склад.",
        icon = Icons.Default.Add,
        accent = UOpGreen,
        soft = UOpGreenSoft,
        onDismiss = onDismiss
    ) {
        UniversalPointSelector(
            label = "Склад назначения",
            points = points,
            selected = selectedPoint,
            onSelect = { selectedPoint = it }
        )

        Spacer(Modifier.height(12.dp))

        UniversalSuggestionField(
            value = supplier,
            onValueChange = { supplier = it },
            label = "Откуда / поставщик",
            placeholder = if (isMilitary) "Источник поступления" else "Поставщик, склад или возврат",
            suggestions = suggestions
        )

        Spacer(Modifier.height(14.dp))

        UniversalOperationItems(
            drafts = drafts,
            catalogItems = catalogItems,
            stockMap = null,
            reasonPresets = emptyList()
        )

        Spacer(Modifier.height(12.dp))

        UniversalOperationField(
            value = comment,
            onValueChange = { comment = it },
            label = "Примечание / документ",
            placeholder = if (isMilitary) "Накладная, рейс, ответственное лицо" else "Накладная, заказ или комментарий"
        )

        Spacer(Modifier.height(18.dp))

        UniversalOperationPrimaryButton(
            text = "Сохранить ${warehouseProfile.operations.income.lowercase()}",
            enabled = drafts.any { it.item != null },
            testTag = "submit_income_button",
            accent = UOpGreen
        ) {
            val entries = universalEntries(drafts)
            if (entries.isNotEmpty()) {
                onConfirm(selectedPoint.id, selectedPoint.name, supplier, entries, comment)
                onDismiss()
            }
        }
    }
}

@Composable
fun UniversalTransferOperationDialog(
    points: List<WarehousePoint>,
    catalogItems: List<InventoryItem>,
    stockRecords: List<StockRecord> = emptyList(),
    initialPointId: String,
    warehouseProfileId: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, List<OperationItemEntry>, String) -> Unit
) {
    val profile = WarehouseProfileCatalog.find(warehouseProfileId)
    val isMilitary = profile.id == "military"
    var fromPoint by remember(points, initialPointId) {
        mutableStateOf(
            points.firstOrNull { it.id == initialPointId }
                ?: points.firstOrNull()
                ?: WarehousePoint("base", "Основной склад")
        )
    }
    var toPoint by remember(points, fromPoint.id) {
        mutableStateOf(
            points.firstOrNull { it.id != fromPoint.id }
                ?: points.firstOrNull()
                ?: WarehousePoint("second", "Склад 2")
        )
    }
    var comment by remember { mutableStateOf("") }
    var insufficient by remember { mutableStateOf<List<String>?>(null) }
    val drafts = remember { mutableStateListOf(UniversalOperationDraft()) }

    val stockMap = remember(fromPoint.id, stockRecords) {
        stockRecords.filter { it.pointId == fromPoint.id }.associate { it.itemId to it.quantity }
    }
    val availableIds = remember(stockMap) { stockMap.filterValues { it > 0 }.keys }
    val availableItems = remember(catalogItems, availableIds) { catalogItems.filter { it.id in availableIds } }
    val suggestions = if (isMilitary) {
        listOf("Ответственный за доставку", "Водитель", "Старшина", "Группа обеспечения")
    } else {
        listOf("Сотрудник склада", "Курьер", "Транспортная служба", "Самовывоз")
    }

    UniversalOperationShell(
        title = profile.operations.transfer,
        subtitle = "Переместите позиции между складами без изменения общего остатка.",
        icon = Icons.Default.SwapHoriz,
        accent = UOpBlue,
        soft = UOpBlueSoft,
        onDismiss = onDismiss
    ) {
        UniversalPointSelector(
            label = "Откуда",
            points = points,
            selected = fromPoint,
            onSelect = { newPoint ->
                fromPoint = newPoint
                drafts.forEach { draft ->
                    if (draft.item != null && (stockRecords.firstOrNull { it.pointId == newPoint.id && it.itemId == draft.item!!.id }?.quantity ?: 0) <= 0) {
                        draft.item = null
                    }
                }
                if (toPoint.id == newPoint.id) {
                    points.firstOrNull { it.id != newPoint.id }?.let { toPoint = it }
                }
            }
        )

        Spacer(Modifier.height(10.dp))

        UniversalPointSelector(
            label = "Куда",
            points = points,
            selected = toPoint,
            onSelect = { toPoint = it }
        )

        Spacer(Modifier.height(14.dp))

        if (availableItems.isEmpty()) {
            UniversalOperationNotice("На складе «${fromPoint.name}» нет остатка для перемещения.")
            Spacer(Modifier.height(12.dp))
        }

        UniversalOperationItems(
            drafts = drafts,
            catalogItems = availableItems,
            stockMap = stockMap,
            reasonPresets = emptyList()
        )

        Spacer(Modifier.height(12.dp))

        UniversalSuggestionField(
            value = comment,
            onValueChange = { comment = it },
            label = "Ответственный / примечание",
            placeholder = if (isMilitary) "Ответственный, время перемещения" else "Сотрудник, курьер или комментарий",
            suggestions = suggestions
        )

        Spacer(Modifier.height(18.dp))

        UniversalOperationPrimaryButton(
            text = profile.operations.transfer,
            enabled = drafts.any { it.item != null },
            testTag = "submit_transfer_button",
            accent = UOpBlue
        ) {
            val entries = universalEntries(drafts)
            if (entries.isNotEmpty()) {
                val problems = universalInsufficient(entries, stockMap)
                if (problems.isNotEmpty()) {
                    insufficient = problems
                } else {
                    onConfirm(fromPoint.id, fromPoint.name, toPoint.id, toPoint.name, entries, comment)
                    onDismiss()
                }
            }
        }
    }

    insufficient?.let { problems ->
        UniversalInsufficientStockDialog(
            pointName = fromPoint.name,
            items = problems,
            onDismiss = { insufficient = null },
            onProceed = {
                val entries = universalEntries(drafts)
                if (entries.isNotEmpty()) {
                    onConfirm(fromPoint.id, fromPoint.name, toPoint.id, toPoint.name, entries, comment)
                    insufficient = null
                    onDismiss()
                }
            }
        )
    }
}

@Composable
fun UniversalIssueOperationDialog(
    points: List<WarehousePoint>,
    catalogItems: List<InventoryItem>,
    stockRecords: List<StockRecord> = emptyList(),
    initialPointId: String,
    warehouseProfileId: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, List<OperationItemEntry>, String) -> Unit
) {
    val profile = WarehouseProfileCatalog.find(warehouseProfileId)
    var fromPoint by remember(points, initialPointId) {
        mutableStateOf(
            points.firstOrNull { it.id == initialPointId }
                ?: points.firstOrNull()
                ?: WarehousePoint("base", "Основной склад")
        )
    }
    var targetPoint by remember(points, fromPoint.id) {
        mutableStateOf(
            points.firstOrNull { it.id != fromPoint.id }
                ?: points.firstOrNull()
                ?: WarehousePoint("base", "Основной склад")
        )
    }
    var comment by remember { mutableStateOf("") }
    var insufficient by remember { mutableStateOf<List<String>?>(null) }
    val drafts = remember { mutableStateListOf(UniversalOperationDraft()) }

    val stockMap = remember(fromPoint.id, stockRecords) {
        stockRecords.filter { it.pointId == fromPoint.id }.associate { it.itemId to it.quantity }
    }
    val availableIds = remember(stockMap) { stockMap.filterValues { it > 0 }.keys }
    val availableItems = remember(catalogItems, availableIds) { catalogItems.filter { it.id in availableIds } }

    UniversalOperationShell(
        title = profile.operations.issue,
        subtitle = "Выберите склад, получателя и позиции для выдачи.",
        icon = Icons.Default.Inventory2,
        accent = UOpOrange,
        soft = UOpOrangeSoft,
        onDismiss = onDismiss
    ) {
        UniversalPointSelector(
            label = "Откуда",
            points = points,
            selected = fromPoint,
            onSelect = { newPoint ->
                fromPoint = newPoint
                drafts.forEach { draft ->
                    if (draft.item != null && (stockRecords.firstOrNull { it.pointId == newPoint.id && it.itemId == draft.item!!.id }?.quantity ?: 0) <= 0) {
                        draft.item = null
                    }
                }
            }
        )

        Spacer(Modifier.height(10.dp))

        UniversalPointSelector(
            label = "Куда / получатель",
            points = points,
            selected = targetPoint,
            onSelect = { targetPoint = it }
        )

        Spacer(Modifier.height(14.dp))

        if (availableItems.isEmpty()) {
            UniversalOperationNotice("На складе «${fromPoint.name}» нет остатка для этой операции.")
            Spacer(Modifier.height(12.dp))
        }

        UniversalOperationItems(
            drafts = drafts,
            catalogItems = availableItems,
            stockMap = stockMap,
            reasonPresets = emptyList()
        )

        Spacer(Modifier.height(12.dp))

        UniversalOperationField(
            value = comment,
            onValueChange = { comment = it },
            label = "Примечание",
            placeholder = "Необязательно"
        )

        Spacer(Modifier.height(18.dp))

        UniversalOperationPrimaryButton(
            text = "Сохранить ${profile.operations.issue.lowercase()}",
            enabled = drafts.any { it.item != null },
            testTag = "submit_issue_button",
            accent = UOpOrange
        ) {
            val entries = universalEntries(drafts)
            if (entries.isNotEmpty()) {
                val problems = universalInsufficient(entries, stockMap)
                if (problems.isNotEmpty()) {
                    insufficient = problems
                } else {
                    onConfirm(fromPoint.id, fromPoint.name, targetPoint.id, targetPoint.name, entries, comment)
                    onDismiss()
                }
            }
        }
    }

    insufficient?.let { problems ->
        UniversalInsufficientStockDialog(
            pointName = fromPoint.name,
            items = problems,
            onDismiss = { insufficient = null },
            onProceed = {
                val entries = universalEntries(drafts)
                if (entries.isNotEmpty()) {
                    onConfirm(fromPoint.id, fromPoint.name, targetPoint.id, targetPoint.name, entries, comment)
                    insufficient = null
                    onDismiss()
                }
            }
        )
    }
}

@Composable
fun UniversalExpenditureOperationDialog(
    profile: UserProfile?,
    points: List<WarehousePoint>,
    catalogItems: List<InventoryItem>,
    stockRecords: List<StockRecord> = emptyList(),
    initialPointId: String,
    warehouseProfileId: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, List<OperationItemEntry>, String) -> Unit
) {
    val warehouseProfile = WarehouseProfileCatalog.find(warehouseProfileId)
    val isMilitary = warehouseProfile.id == "military"
    var fromPoint by remember(points, initialPointId) {
        mutableStateOf(
            points.firstOrNull { it.id == initialPointId }
                ?: points.firstOrNull()
                ?: WarehousePoint("base", "Основной склад")
        )
    }
    var docNumber by remember { mutableStateOf("АКТ-${(100..999).random()}") }
    var responsible by remember { mutableStateOf(profile?.callsign.orEmpty()) }
    var comment by remember { mutableStateOf("") }
    var insufficient by remember { mutableStateOf<List<String>?>(null) }
    val drafts = remember { mutableStateListOf(UniversalOperationDraft(reason = "")) }

    val stockMap = remember(fromPoint.id, stockRecords) {
        stockRecords.filter { it.pointId == fromPoint.id }.associate { it.itemId to it.quantity }
    }
    val availableIds = remember(stockMap) { stockMap.filterValues { it > 0 }.keys }
    val availableItems = remember(catalogItems, availableIds) { catalogItems.filter { it.id in availableIds } }

    val reasons = if (isMilitary) {
        listOf("Боевая работа", "Учебная подготовка", "Повреждение", "Естественный износ", "Передача в ремонт", "Утрата / уничтожение")
    } else {
        listOf("Брак", "Естественный износ", "Повреждение", "Просрочка", "Утилизация", "Корректировка остатка")
    }
    val responsibleSuggestions = if (isMilitary) {
        listOf("Ответственный за склад", "Старшина", "Начальник службы", "Командир подразделения")
    } else {
        listOf("Кладовщик", "Материально ответственное лицо", "Руководитель", "Администратор склада")
    }

    UniversalOperationShell(
        title = warehouseProfile.operations.writeOff,
        subtitle = "Зафиксируйте причину и позиции. Операция останется в журнале движения.",
        icon = Icons.Default.Info,
        accent = UOpRed,
        soft = UOpRedSoft,
        onDismiss = onDismiss
    ) {
        UniversalPointSelector(
            label = "Склад / место хранения",
            points = points,
            selected = fromPoint,
            onSelect = { newPoint ->
                fromPoint = newPoint
                drafts.forEach { draft ->
                    if (draft.item != null && (stockRecords.firstOrNull { it.pointId == newPoint.id && it.itemId == draft.item!!.id }?.quantity ?: 0) <= 0) {
                        draft.item = null
                    }
                }
            }
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            UniversalOperationField(
                value = docNumber,
                onValueChange = { docNumber = it },
                label = "Документ / № акта",
                placeholder = "Номер",
                modifier = Modifier.weight(1f)
            )
            UniversalSuggestionField(
                value = responsible,
                onValueChange = { responsible = it },
                label = "Ответственный",
                placeholder = "Имя",
                suggestions = responsibleSuggestions,
                modifier = Modifier.weight(1.25f)
            )
        }

        Spacer(Modifier.height(12.dp))

        if (isMilitary) {
            UniversalOperationInfo(
                "Военные причины списания доступны только в этом профиле. Операция сохраняется в общем журнале."
            )
        } else {
            UniversalOperationInfo(
                "Операция сохраняется в журнале движения и будет доступна для универсальных отчётов."
            )
        }

        Spacer(Modifier.height(12.dp))

        if (availableItems.isEmpty()) {
            UniversalOperationNotice("На складе «${fromPoint.name}» нет остатка для списания.")
            Spacer(Modifier.height(12.dp))
        }

        UniversalOperationItems(
            drafts = drafts,
            catalogItems = availableItems,
            stockMap = stockMap,
            reasonPresets = reasons
        )

        Spacer(Modifier.height(12.dp))

        UniversalOperationField(
            value = comment,
            onValueChange = { comment = it },
            label = "Примечание",
            placeholder = "Необязательно"
        )

        Spacer(Modifier.height(18.dp))

        UniversalOperationPrimaryButton(
            text = "Сохранить ${warehouseProfile.operations.writeOff.lowercase()}",
            enabled = drafts.any { it.item != null },
            testTag = "submit_expenditure_button",
            accent = UOpRed
        ) {
            val entries = universalEntries(drafts, includeReason = true)
            if (entries.isNotEmpty()) {
                val problems = universalInsufficient(entries, stockMap)
                if (problems.isNotEmpty()) {
                    insufficient = problems
                } else {
                    onConfirm(fromPoint.id, fromPoint.name, docNumber, responsible, entries, comment)
                    onDismiss()
                }
            }
        }
    }

    insufficient?.let { problems ->
        UniversalInsufficientStockDialog(
            pointName = fromPoint.name,
            items = problems,
            onDismiss = { insufficient = null },
            onProceed = {
                val entries = universalEntries(drafts, includeReason = true)
                if (entries.isNotEmpty()) {
                    onConfirm(fromPoint.id, fromPoint.name, docNumber, responsible, entries, comment)
                    insufficient = null
                    onDismiss()
                }
            }
        )
    }
}

@Composable
private fun UniversalOperationShell(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    soft: Color,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(soft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(21.dp)
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            color = UOpInk,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = subtitle,
                            color = UOpMuted,
                            fontSize = 10.5.sp,
                            lineHeight = 14.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = Color(0xFF98A2B3),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 545.dp)
                ) {
                    item { content() }
                }
            }
        }
    }
}

@Composable
private fun UniversalPointSelector(
    label: String,
    points: List<WarehousePoint>,
    selected: WarehousePoint,
    onSelect: (WarehousePoint) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = UOpInk, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))

        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(15.dp))
                    .background(UOpBg)
                    .clickable(enabled = points.isNotEmpty()) { expanded = true }
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(UOpSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warehouse,
                        contentDescription = null,
                        tint = UOpPrimary,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Spacer(Modifier.width(9.dp))
                Text(
                    text = selected.name,
                    color = UOpInk,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = UOpPrimary
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color.White)
            ) {
                points.forEach { point ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = point.name,
                                    color = UOpInk,
                                    fontSize = 12.sp,
                                    fontWeight = if (point.id == selected.id) FontWeight.Bold else FontWeight.Medium
                                )
                                if (point.description.isNotBlank()) {
                                    Text(
                                        text = point.description,
                                        color = UOpMuted,
                                        fontSize = 9.5.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        },
                        onClick = {
                            onSelect(point)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun UniversalOperationItems(
    drafts: MutableList<UniversalOperationDraft>,
    catalogItems: List<InventoryItem>,
    stockMap: Map<String, Int>?,
    reasonPresets: List<String>
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Позиции", color = UOpInk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(
                text = if (stockMap == null) "Выберите товар и количество" else "Показываются позиции с доступным остатком",
                color = UOpMuted,
                fontSize = 9.5.sp
            )
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(100.dp))
                .background(UOpSoft)
                .clickable { drafts.add(UniversalOperationDraft()) }
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = UOpPrimary, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(4.dp))
            Text("Добавить", color = UOpPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }

    Spacer(Modifier.height(8.dp))

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        drafts.forEachIndexed { index, draft ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(17.dp),
                colors = CardDefaults.cardColors(containerColor = UOpBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Позиция ${index + 1}",
                            color = UOpMuted,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        if (drafts.size > 1) {
                            IconButton(
                                onClick = { drafts.removeAt(index) },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Удалить позицию",
                                    tint = UOpRed,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    UniversalSearchableItemDropdown(
                        label = "",
                        catalogItems = catalogItems,
                        selectedItem = draft.item,
                        onItemSelected = { draft.item = it },
                        availableStocksMap = stockMap
                    )

                    Spacer(Modifier.height(7.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = draft.quantity,
                            onValueChange = { draft.quantity = it.filter(Char::isDigit) },
                            modifier = Modifier.weight(0.8f),
                            singleLine = true,
                            label = { Text("Количество", fontSize = 10.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(13.dp),
                            colors = universalOperationFieldColors()
                        )

                        Text(
                            text = draft.item?.unit ?: "ед.",
                            color = UOpPrimary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (reasonPresets.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        UniversalReasonField(
                            value = draft.reason,
                            onValueChange = { draft.reason = it },
                            presets = reasonPresets
                        )
                    }

                    val item = draft.item
                    val available = item?.let { stockMap?.get(it.id) }
                    val requested = draft.quantity.toIntOrNull() ?: 0
                    if (available != null && requested > available) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Доступно: $available ${item.unit}. Указано: $requested.",
                            color = UOpRed,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UniversalReasonField(
    value: String,
    onValueChange: (String) -> Unit,
    presets: List<String>
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Причина", fontSize = 10.sp) },
            placeholder = { Text("Выберите или введите", color = Color(0xFF98A2B3), fontSize = 10.5.sp) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Причины",
                    tint = UOpPrimary,
                    modifier = Modifier.clickable { expanded = true }
                )
            },
            shape = RoundedCornerShape(13.dp),
            colors = universalOperationFieldColors()
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            presets.forEach { preset ->
                DropdownMenuItem(
                    text = { Text(preset, color = UOpInk, fontSize = 11.sp) },
                    onClick = {
                        onValueChange(preset)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun UniversalSuggestionField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    suggestions: List<String>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        UniversalOperationField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            placeholder = placeholder
        )

        if (suggestions.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                suggestions.forEach { suggestion ->
                    Text(
                        text = suggestion,
                        color = if (value == suggestion) Color.White else UOpPrimary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(if (value == suggestion) UOpPrimary else UOpSoft)
                            .clickable { onValueChange(suggestion) }
                            .padding(horizontal = 9.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun UniversalOperationField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(label, fontSize = 10.5.sp) },
        placeholder = { Text(placeholder, color = Color(0xFF98A2B3), fontSize = 10.5.sp) },
        shape = RoundedCornerShape(14.dp),
        colors = universalOperationFieldColors()
    )
}

@Composable
private fun UniversalOperationPrimaryButton(
    text: String,
    enabled: Boolean,
    testTag: String,
    accent: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = accent,
            contentColor = Color.White,
            disabledContainerColor = Color(0xFFE2E5EB),
            disabledContentColor = Color(0xFF98A2B3)
        )
    ) {
        Text(text, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun UniversalOperationNotice(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(UOpRedSoft)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = UOpRed,
            modifier = Modifier.size(17.dp)
        )
        Spacer(Modifier.width(7.dp))
        Text(text, color = UOpRed, fontSize = 10.5.sp, lineHeight = 14.sp)
    }
}

@Composable
private fun UniversalOperationInfo(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(UOpSoft)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = UOpPrimary,
            modifier = Modifier.size(17.dp)
        )
        Spacer(Modifier.width(7.dp))
        Text(text, color = UOpMuted, fontSize = 10.5.sp, lineHeight = 14.sp)
    }
}

@Composable
private fun UniversalInsufficientStockDialog(
    pointName: String,
    items: List<String>,
    onDismiss: () -> Unit,
    onProceed: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = {
            Text("Недостаточно остатка", color = UOpInk, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    text = "На складе «$pointName» числится меньше, чем указано в операции.",
                    color = UOpMuted,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp
                )
                Spacer(Modifier.height(8.dp))
                items.forEach { item ->
                    Text(
                        text = "• $item",
                        color = UOpRed,
                        fontSize = 10.5.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onProceed) {
                Text("Продолжить", color = UOpRed, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Исправить", color = UOpPrimary, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

@Composable
private fun universalOperationFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    focusedBorderColor = UOpPrimary,
    unfocusedBorderColor = UOpBorder,
    focusedTextColor = UOpInk,
    unfocusedTextColor = UOpInk,
    focusedLabelColor = UOpPrimary,
    unfocusedLabelColor = UOpMuted
)

private fun universalEntries(
    drafts: List<UniversalOperationDraft>,
    includeReason: Boolean = false
): List<OperationItemEntry> =
    drafts.mapNotNull { draft ->
        val item = draft.item ?: return@mapNotNull null
        val qty = draft.quantity.toIntOrNull() ?: 1
        OperationItemEntry(
            itemId = item.id,
            itemName = item.name,
            unit = item.unit,
            quantity = qty,
            categoryClass = item.categoryClass,
            reason = if (includeReason) draft.reason else ""
        )
    }

private fun universalInsufficient(
    entries: List<OperationItemEntry>,
    stockMap: Map<String, Int>
): List<String> =
    entries.mapNotNull { entry ->
        val available = stockMap[entry.itemId] ?: 0
        if (entry.quantity > available) {
            "${entry.itemName}: в наличии $available ${entry.unit}, указано ${entry.quantity} ${entry.unit}"
        } else {
            null
        }
    }
