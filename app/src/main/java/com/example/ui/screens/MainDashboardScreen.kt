@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.AssignmentInd
import androidx.compose.material.icons.filled.MoveToInbox
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import com.example.data.model.InventoryItem
import com.example.data.model.OperationRecord
import com.example.data.model.StockRecord
import com.example.data.model.UserProfile
import com.example.data.model.WarehousePoint
import com.example.ui.components.PointActionsSheet
import com.example.ui.components.AdjustStockDialog
import com.example.ui.components.DemoBanner
import com.example.ui.components.ReorderPointsDialog
import com.example.ui.components.TacticalHeader
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
import com.example.ui.theme.TacticalSurfaceElevated
import com.example.ui.theme.TacticalSurfaceLight
import com.example.ui.theme.TacticalTeal
import com.example.ui.theme.TacticalTealDark
import com.example.ui.theme.TacticalTealText
import com.example.ui.theme.TacticalTextDim
import com.example.ui.theme.TacticalTextMuted
import com.example.ui.theme.TacticalTextPrimary
import com.example.ui.theme.TacticalTextSecondary

private data class PendingAdjustStock(
    val pointId: String,
    val pointName: String,
    val itemId: String,
    val itemName: String,
    val quantity: Int,
    val unit: String
)

data class TableInventoryRow(
    val pointId: String,
    val pointName: String,
    val isBasePoint: Boolean,
    val item: InventoryItem,
    val quantity: Int,
    val incomeTotal: Int,
    val expenseTotal: Int
)

@Composable
fun MainDashboardScreen(
    profile: UserProfile?,
    points: List<WarehousePoint>,
    catalogItems: List<InventoryItem>,
    stockRecords: List<StockRecord>,
    operations: List<OperationRecord> = emptyList(),
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    availableCategories: List<String>,
    onIncomeClick: () -> Unit,
    onTransferClick: () -> Unit,
    onIssueClick: () -> Unit,
    onExpenditureClick: () -> Unit,
    onAddPointClick: () -> Unit,
    onEditPointClick: (WarehousePoint) -> Unit,
    onAddCustomItemClick: () -> Unit = {},
    onAdjustStock: (pointId: String, pointName: String, itemId: String, itemName: String, unit: String, newQuantity: Int, comment: String) -> Unit,
    onSyncClick: () -> Unit,
    onSecondPhoneClick: () -> Unit = {},
    onExportClick: () -> Unit,
    onBannerClick: () -> Unit,
    onProfileClick: () -> Unit,
    onHelpClick: () -> Unit = {},
    isDarkTheme: Boolean = true,
    onToggleTheme: () -> Unit = {},
    syncState: com.example.data.sync.SyncState = com.example.data.sync.SyncState(),
    profileMode: com.example.data.model.WorkspaceProfileMode = com.example.data.model.WorkspaceProfileMode.MILITARY,
    onReorderPoints: (List<WarehousePoint>) -> Unit = {},
    onDeletePoint: (String) -> Unit = {},
    onReturnClick: () -> Unit = {},
    pendingRequisitionsCount: Int = 0
) {
    var selectedPointFilterId by remember { mutableStateOf<String?>(null) } // null = Все склады
    var adjustingStock by remember { mutableStateOf<PendingAdjustStock?>(null) }
    var showReorderPointsDialog by remember { mutableStateOf(false) }
    val expandedPointIds = remember { mutableStateMapOf<String, Boolean>() }
    // Point whose long-press action sheet is open.
    var pointMenuFor by remember { mutableStateOf<WarehousePoint?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val categories = remember(availableCategories, catalogItems, profileMode) {
        val baseCats = if (availableCategories.isNotEmpty()) {
            availableCategories
        } else {
            com.example.data.local.InitialData.getCategoriesFor(profileMode)
        }
        val syncedCustomCats = catalogItems
            .filter { it.isCustom && it.serviceCategory.isNotBlank() && it.serviceCategory != "Все виды" }
            .map { it.serviceCategory.trim() }
        val merged = (baseCats + syncedCustomCats).distinct()
        if (merged.isEmpty()) listOf("Все виды") else listOf("Все виды") + merged
    }

    // Helper map of stock per point
    val stockMap = remember(stockRecords) {
        stockRecords.groupBy { it.pointId }
    }

    // Helper map to recover item names, units, and categories from operations history
    val operationItemsMap = remember(operations) {
        val map = mutableMapOf<String, com.example.data.model.OperationItemEntry>()
        for (op in operations) {
            if (op.itemsJson.isNotBlank()) {
                try {
                    val arr = org.json.JSONArray(op.itemsJson)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val id = obj.optString("itemId", "")
                        val name = obj.optString("itemName", "")
                        if (id.isNotBlank() && name.isNotBlank() && !id.startsWith("null")) {
                            map[id] = com.example.data.model.OperationItemEntry(
                                itemId = id,
                                itemName = name,
                                unit = obj.optString("unit", "шт."),
                                quantity = obj.optInt("quantity", 1),
                                categoryClass = obj.optString("categoryClass", "Кат. 1")
                            )
                        }
                    }
                } catch (_: Exception) {}
            }
        }
        map
    }

    // Prepare table items for a specific point or all points
    fun getItemsForPoint(pointId: String): List<TableInventoryRow> {
        val cleanQuery = searchQuery.trim().lowercase()
        val stocksForPoint = (stockMap[pointId] ?: emptyList()).associateBy { it.itemId }
        val pt = points.firstOrNull { it.id == pointId } ?: return emptyList()

        // Build a complete catalog map ensuring every item with stock or history is represented
        val catalogMap = catalogItems.associateBy { it.id }.toMutableMap()
        for (st in stocksForPoint.values) {
            val existing = catalogMap[st.itemId]
            // If item is missing or is just a placeholder "Имущество (id)"
            if (existing == null || existing.name.startsWith("Имущество (") || existing.name.isBlank()) {
                val defaultItem = (com.example.data.local.InitialData.getCivilianItems() +
                    com.example.data.local.InitialData.getDefaultItems()).find { it.id == st.itemId }
                if (defaultItem != null) {
                    catalogMap[st.itemId] = defaultItem
                } else {
                    val fromOp = operationItemsMap[st.itemId]
                    val friendlyName = when {
                        fromOp != null && fromOp.itemName.isNotBlank() -> fromOp.itemName
                        st.itemId == "auto_01" -> "Комплект фильтров УАЗ Патриот Пикап"
                        st.itemId == "auto_02" -> "Масло моторное 10W-40 (Канистра 5л)"
                        st.itemId == "auto_03" -> "Антифриз G12 (Канистра 5л)"
                        st.itemId == "rav_27" -> "Мина 120-мм дымовая Д-843А"
                        st.itemId == "rav_28" -> "Мина 120-мм осветительная С-843"
                        st.itemId == "rav_29" -> "Мина 82-мм дымовая Д-832ДУ"
                        st.itemId == "rav_30" -> "Мина 82-мм осветительная С-832С"
                        else -> "Имущество (${st.itemId})"
                    }
                    val resolvedCat = when {
                        st.itemId.startsWith("rav_") -> "Служба РАВ"
                        st.itemId.startsWith("auto_") -> "Автомобильная и БТ служба"
                        st.itemId.startsWith("med_") -> "Медицинская служба"
                        st.itemId.startsWith("vesh_") -> "Вещевая служба и СИБЗ"
                        st.itemId.startsWith("ing_") -> "Инженерная служба"
                        st.itemId.startsWith("prod_") -> "Продовольственная служба"
                        st.itemId.startsWith("gsm_") -> "Служба ГСМ"
                        st.itemId.startsWith("rhbz_") -> "Служба РХБЗ"
                        st.itemId.startsWith("svyaz_") || st.itemId.startsWith("bpla_") -> "Служба связи и РЭБ"
                        friendlyName.contains("Мина", ignoreCase = true) || friendlyName.contains("Снаряд", ignoreCase = true) || friendlyName.contains("Патрон", ignoreCase = true) -> "Служба РАВ"
                        friendlyName.contains("Дизель", ignoreCase = true) || friendlyName.contains("Бензин", ignoreCase = true) || friendlyName.contains("Масло", ignoreCase = true) -> "Служба ГСМ"
                        friendlyName.contains("Аптечка", ignoreCase = true) || friendlyName.contains("Бинт", ignoreCase = true) || friendlyName.contains("Жгут", ignoreCase = true) -> "Медицинская служба"
                        else -> if (profileMode.isMilitary) "Служба РАВ" else "Прочее"
                    }
                    val unit = fromOp?.unit?.takeIf { it.isNotBlank() }
                        ?: existing?.unit?.takeIf { it.isNotBlank() }
                        ?: if (st.itemId.startsWith("vesh_") || st.itemId.startsWith("auto_")) "компл." else if (st.itemId.startsWith("gsm_")) "л." else if (st.itemId.startsWith("prod_")) "кг." else "шт."
                    catalogMap[st.itemId] = InventoryItem(
                        id = st.itemId,
                        name = friendlyName,
                        serviceCategory = resolvedCat,
                        subType = "Снабжение",
                        unit = unit,
                        categoryClass = fromOp?.categoryClass?.takeIf { it.isNotBlank() } ?: existing?.categoryClass ?: "Кат. 1",
                        isCustom = true
                    )
                }
            }
        }

        // Distinct list of all known catalog items plus items currently on this point (using updated catalogMap)
        val allRelevantItems = (catalogItems.map { catalogMap[it.id] ?: it } + stocksForPoint.values.mapNotNull { catalogMap[it.itemId] }).distinctBy { it.id }

        return allRelevantItems.mapNotNull { item ->
            val st = stocksForPoint[item.id]
            val qty = st?.quantity ?: 0
            val inc = st?.incomeTotal ?: 0
            val exp = st?.expenseTotal ?: 0

            // Category filter
            val matchesCategory = (selectedCategory == "Все виды") || (item.serviceCategory == selectedCategory)
            if (!matchesCategory) return@mapNotNull null

            // Search filter
            val matchesQuery = if (cleanQuery.isEmpty()) true else {
                pt.name.lowercase().contains(cleanQuery) ||
                item.name.lowercase().contains(cleanQuery) ||
                item.subType.lowercase().contains(cleanQuery) ||
                item.standardCode.lowercase().contains(cleanQuery) ||
                item.serviceCategory.lowercase().contains(cleanQuery)
            }
            if (!matchesQuery) return@mapNotNull null

            // Show non-zero stock or active movement if no search query
            if (cleanQuery.isEmpty() && qty <= 0 && inc <= 0 && exp <= 0) {
                return@mapNotNull null
            }

            TableInventoryRow(
                pointId = pt.id,
                pointName = pt.name,
                isBasePoint = pt.isBase,
                item = item,
                quantity = qty,
                incomeTotal = inc,
                expenseTotal = exp
            )
        }
    }

    // Total overall statistics across all active points
    // Computed once per data change, not on every frame: recomputing this
    // inside list items made scrolling and expanding stutter.
    val rowsByPoint = remember(points, catalogItems, stockRecords, searchQuery, selectedCategory, operations) {
        points.associate { it.id to getItemsForPoint(it.id) }
    }
    val allStockRows = remember(rowsByPoint) { points.flatMap { rowsByPoint[it.id].orEmpty() } }
    val catalogUnitById = remember(catalogItems, operationItemsMap) {
        val map = mutableMapOf<String, String>()
        operationItemsMap.forEach { (id, entry) ->
            if (entry.unit.isNotBlank()) map[id] = entry.unit
        }
        catalogItems.forEach { item ->
            if (item.unit.isNotBlank()) map[item.id] = item.unit
        }
        map
    }
    val overallPositionsCount = remember(stockRecords) {
        stockRecords.filter { it.quantity > 0 }.map { it.itemId }.distinct().size
    }
    val overallUnitBreakdown = remember(stockRecords, catalogUnitById) {
        com.example.ui.components.formatUnitBreakdown(
            unitQuantities = stockRecords
                .filter { it.quantity > 0 }
                .map { (catalogUnitById[it.itemId] ?: "шт.") to it.quantity },
            maxUnits = 3,
            emptyLabel = "Остатков пока нет"
        )
    }
    val todayOpsCount = remember(operations) {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis
        operations.count { it.timestamp >= startOfDay }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(TacticalBg)
    ) {
        // TOP HEADER
        item {
            TacticalHeader(
                profile = profile,
                onSyncClick = onSyncClick,
                onSecondPhoneClick = onSecondPhoneClick,
                onExportClick = onExportClick,
                onProfileClick = onProfileClick,
                onHelpClick = onHelpClick,
                onBannerClick = onBannerClick,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                syncState = syncState,
                profileMode = profileMode
            )
        }

        // DEMO OR PRO STATUS BANNER
        item {
            DemoBanner(
                profile = profile,
                onBannerClick = onBannerClick
            )
        }

        // 3D VOLUMETRIC HERO STAGE CARD (Unit-safe: positions count + separated units, NEVER mixed sum!)
        item {
            com.example.ui.components.Hero3DStageCard(
                eyebrow = if (profileMode.isMilitary) "Сводка наличия по точкам" else "Состояние склада",
                positionsCount = overallPositionsCount,
                unitBreakdown = overallUnitBreakdown,
                warehousesCount = points.size,
                todayOpsCount = todayOpsCount,
                pendingRequestsCount = pendingRequisitionsCount,
                warehousesLabel = if (profileMode.isMilitary) "Точек" else "Складов"
            )
        }

        // PRIMARY ACTIONS — Extruded 3D Tactile 4-Tile Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                com.example.ui.components.Volumetric3DActionTile(
                    title = "Приход",
                    icon = Icons.Default.MoveToInbox,
                    gradientStart = Color(0xFF10B981),
                    gradientEnd = Color(0xFF059669),
                    extrusionColor = Color(0xFF065F46),
                    onClick = onIncomeClick,
                    modifier = Modifier.weight(1f).testTag("op_income_button")
                )
                com.example.ui.components.Volumetric3DActionTile(
                    title = "Выдача",
                    icon = Icons.Default.AssignmentInd,
                    gradientStart = Color(0xFFF59E0B),
                    gradientEnd = Color(0xFFD97706),
                    extrusionColor = Color(0xFF92400E),
                    onClick = onIssueClick,
                    modifier = Modifier.weight(1f).testTag("op_issue_button")
                )
                if (profileMode.isMilitary) {
                    com.example.ui.components.Volumetric3DActionTile(
                        title = "Перенос",
                        icon = Icons.Default.LocalShipping,
                        gradientStart = Color(0xFF06B6D4),
                        gradientEnd = Color(0xFF0891B2),
                        extrusionColor = Color(0xFF155E75),
                        onClick = onTransferClick,
                        modifier = Modifier.weight(1f).testTag("op_transfer_button")
                    )
                } else {
                    com.example.ui.components.Volumetric3DActionTile(
                        title = "Возврат",
                        icon = Icons.Default.Undo,
                        gradientStart = Color(0xFF06B6D4),
                        gradientEnd = Color(0xFF0891B2),
                        extrusionColor = Color(0xFF155E75),
                        onClick = onReturnClick,
                        modifier = Modifier.weight(1f).testTag("op_return_button")
                    )
                }
                com.example.ui.components.Volumetric3DActionTile(
                    title = "Списание",
                    icon = Icons.Default.DeleteSweep,
                    gradientStart = Color(0xFFEF4444),
                    gradientEnd = Color(0xFFDC2626),
                    extrusionColor = Color(0xFF991B1B),
                    onClick = onExpenditureClick,
                    modifier = Modifier.weight(1f).testTag("op_expenditure_button")
                )
            }
        }

        // SEARCH BAR WITH MANDATORY "Введите название" PLACEHOLDER
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = {
                    Text(
                        "Введите название",
                        color = TacticalTextDim,
                        fontSize = 13.sp
                    )
                },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Поиск",
                        tint = SageGreenPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .testTag("dashboard_search_input"),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = TacticalSurface,
                    unfocusedContainerColor = TacticalSurface,
                    focusedBorderColor = SageGreenPrimary,
                    unfocusedBorderColor = TacticalBorderSubtle,
                    focusedTextColor = TacticalTextPrimary,
                    unfocusedTextColor = TacticalTextPrimary
                )
            )
        }

        // CATEGORY FILTER CHIPS (3D Pills)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                categories.forEach { cat ->
                    com.example.ui.components.Filter3DPill(
                        text = cat,
                        selected = cat == selectedCategory,
                        onClick = { onSelectCategory(cat) }
                    )
                }
            }
        }

        // POINTS SECTION HEADER: title + compact actions
        item {
            val allExpanded = points.isNotEmpty() && points.all { expandedPointIds[it.id] == true }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 14.dp, top = 10.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (profileMode.isMilitary) "Склады и точки" else "Склады и участки",
                    color = TacticalTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = points.size.toString(),
                    color = TacticalTextMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.weight(1f))
                if (points.size > 1) {
                    SectionIconButton(
                        icon = Icons.Default.SwapVert,
                        description = "Порядок складов",
                        onClick = { showReorderPointsDialog = true }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                SectionIconButton(
                    icon = if (allExpanded) Icons.Default.UnfoldLess else Icons.Default.UnfoldMore,
                    description = if (allExpanded) "Свернуть все" else "Развернуть все",
                    onClick = { points.forEach { pt -> expandedPointIds[pt.id] = !allExpanded } }
                )
                Spacer(modifier = Modifier.width(6.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SageGreenDark)
                        .clickable { onAddPointClick() }
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Добавить склад",
                        tint = SageGreenBright,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Склад",
                        color = SageGreenBright,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // CONTENT SECTION:
        // Case A: Specific Point is selected -> Concise Stock & Remaining Balance for that Point
        // Case B: All Points -> Collapsible point cards (information hidden in lists) + search results
        if (selectedPointFilterId != null) {
            val currentPoint = points.firstOrNull { it.id == selectedPointFilterId }
            if (currentPoint != null) {
                val pointRows = rowsByPoint[currentPoint.id].orEmpty()
                val activePointPositions = pointRows.count { it.quantity > 0 }
                val pointUnitBreakdown = com.example.ui.components.formatUnitBreakdown(
                    unitQuantities = pointRows.filter { it.quantity > 0 }.map { it.item.unit to it.quantity },
                    maxUnits = 2,
                    emptyLabel = "Пусто"
                )

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        // Point Summary 3D Card
                        com.example.ui.components.Card3DSurface(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 20.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = currentPoint.name,
                                            color = TacticalTextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (currentPoint.isBase) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(TacticalGoldDark)
                                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = if (profileMode.isMilitary) "Базовый склад" else "Основной склад",
                                                    color = TacticalGoldText,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    if (currentPoint.description.isNotBlank()) {
                                        Text(
                                            text = currentPoint.description,
                                            color = TacticalTextMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "$activePointPositions поз.",
                                        color = SageGreenBright,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = pointUnitBreakdown,
                                        color = TacticalTextMuted,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Concise Stock Table for this point with visible borders
                        PointStockTableView(
                            rows = pointRows,
                            searchQuery = searchQuery,
                            onAdjustClick = { row ->
                                adjustingStock = PendingAdjustStock(
                                    pointId = row.pointId,
                                    pointName = row.pointName,
                                    itemId = row.item.id,
                                    itemName = row.item.name,
                                    quantity = row.quantity,
                                    unit = row.item.unit
                                )
                            }
                        )
                    }
                }
            }
        } else {
            // "Все склады" selected:
            // If search is active, show matching items table
            // Otherwise show Clean Collapsible Point Cards (hidden in lists)
            if (searchQuery.isNotBlank()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Результаты поиска: ${allStockRows.size}",
                            color = SageGreenBright,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        PointStockTableView(
                            rows = allStockRows,
                            searchQuery = searchQuery,
                            showPointColumn = true,
                            onAdjustClick = { row ->
                                adjustingStock = PendingAdjustStock(
                                    pointId = row.pointId,
                                    pointName = row.pointName,
                                    itemId = row.item.id,
                                    itemName = row.item.name,
                                    quantity = row.quantity,
                                    unit = row.item.unit
                                )
                            }
                        )
                    }
                }
            } else {
                // Collapsible 3D Point Cards (unit-safe metrics: positions count + separated units)
                itemsIndexed(points, key = { _, pt -> pt.id }) { _, point ->
                    val pointRows = rowsByPoint[point.id].orEmpty()
                    val activePointPositions = pointRows.count { it.quantity > 0 }
                    val pointUnitBreakdown = com.example.ui.components.formatUnitBreakdown(
                        unitQuantities = pointRows.filter { it.quantity > 0 }.map { it.item.unit to it.quantity },
                        maxUnits = 2,
                        emptyLabel = "Пусто"
                    )
                    // Default to collapsed (false) so that on entrance all lists are hidden
                    val isExpanded = expandedPointIds[point.id] ?: false

                    val lifted = pointMenuFor?.id == point.id
                    val lift by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (lifted) 1f else 0f,
                        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.55f, stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow),
                        label = "lift"
                    )
                    com.example.ui.components.Card3DSurface(
                        modifier = Modifier
                            .animateItem()
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 5.dp)
                            .graphicsLayer {
                                scaleX = 1f + 0.03f * lift
                                scaleY = 1f + 0.03f * lift
                                translationY = -4f * lift
                            },
                        cornerRadius = 20.dp,
                        accentBorder = if (lifted) SageGreenBright else if (isExpanded) SageGreenPrimary.copy(alpha = 0.55f) else null,
                        elevated = isExpanded || lifted
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Point Header Row (Tappable to expand / collapse)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = { expandedPointIds[point.id] = !isExpanded },
                                        onLongClick = {
                                            com.example.util.Haptics.tick(context)
                                            pointMenuFor = point
                                        }
                                    )
                                    .padding(horizontal = 13.dp, vertical = 11.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(13.dp))
                                            .background(
                                                androidx.compose.ui.graphics.Brush.linearGradient(
                                                    if (point.isBase) listOf(Color(0xFFF59E0B), Color(0xFFD97706))
                                                    else listOf(Color(0xFF10B981), Color(0xFF0D9488))
                                                )
                                            )
                                            .border(
                                                0.9.dp,
                                                Color.White.copy(alpha = 0.30f),
                                                RoundedCornerShape(13.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (point.isBase) Icons.Default.Warehouse else Icons.Default.Inventory2,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(11.dp))

                                    Column {
                                        Text(
                                            text = point.name,
                                            color = TacticalTextPrimary,
                                            fontSize = 14.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val basePrefix = if (profileMode.isMilitary) "Базовый · " else "Основной · "
                                        Text(
                                            text = (if (point.isBase) basePrefix else "") + pointUnitBreakdown,
                                            color = if (point.isBase) TacticalGoldText else TacticalTextMuted,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(11.dp))
                                            .background(SageGreenDark)
                                            .border(1.dp, SageGreenPrimary.copy(alpha = 0.45f), RoundedCornerShape(11.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "$activePointPositions поз.",
                                            color = SageGreenBright,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    val chevronRotation by androidx.compose.animation.core.animateFloatAsState(
                                        targetValue = if (isExpanded) 180f else 0f,
                                        label = "chevron"
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ExpandMore,
                                        contentDescription = if (isExpanded) "Свернуть" else "Развернуть",
                                        tint = SageGreenPrimary,
                                        modifier = Modifier
                                            .size(22.dp)
                                            .graphicsLayer { rotationZ = chevronRotation }
                                    )
                                }
                            }

                            // Collapsible content inside list
                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(TacticalSurfaceLight)
                                        .padding(8.dp)
                                ) {
                                    PointStockTableView(
                                        rows = pointRows,
                                        searchQuery = searchQuery,
                                        showPointColumn = false,
                                        onAdjustClick = { row ->
                                            adjustingStock = PendingAdjustStock(
                                                pointId = row.pointId,
                                                pointName = row.pointName,
                                                itemId = row.item.id,
                                                itemName = row.item.name,
                                                quantity = row.quantity,
                                                unit = row.item.unit
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // MODAL DIALOG: ADJUST STOCK
    adjustingStock?.let { adj ->
        AdjustStockDialog(
            pointName = adj.pointName,
            pointId = adj.pointId,
            itemName = adj.itemName,
            itemId = adj.itemId,
            currentQuantity = adj.quantity,
            unit = adj.unit,
            onDismiss = { adjustingStock = null },
            onConfirm = { pId, pName, iId, iName, itemUnit, newQty, comment ->
                onAdjustStock(pId, pName, iId, iName, itemUnit, newQty, comment)
                adjustingStock = null
            }
        )
    }

    // LONG-PRESS ACTIONS FOR A POINT
    pointMenuFor?.let { menuPoint ->
        val index = points.indexOfFirst { it.id == menuPoint.id }
        val menuRows = rowsByPoint[menuPoint.id].orEmpty().filter { it.quantity > 0 }
        val menuSummary = com.example.ui.components.formatUnitBreakdown(
            unitQuantities = menuRows.map { it.item.unit to it.quantity },
            maxUnits = 3,
            emptyLabel = "остатков нет"
        )
        PointActionsSheet(
            point = menuPoint,
            stockSummary = if (menuRows.isNotEmpty()) "на остатке: $menuSummary" else menuSummary,
            hasStock = menuRows.isNotEmpty(),
            isMilitary = profileMode.isMilitary,
            canMoveUp = index > 0,
            canMoveDown = index in 0 until points.lastIndex,
            onMove = { delta ->
                val list = points.toMutableList()
                val from = list.indexOfFirst { it.id == menuPoint.id }
                val to = from + delta
                if (from >= 0 && to in list.indices) {
                    java.util.Collections.swap(list, from, to)
                    com.example.util.Haptics.tick(context)
                    onReorderPoints(list)
                }
            },
            onEdit = { pointMenuFor = null; onEditPointClick(menuPoint) },
            onDelete = { pointMenuFor = null; onDeletePoint(menuPoint.id) },
            onDismiss = { pointMenuFor = null }
        )
    }

    // MODAL DIALOG: REORDER POINTS
    if (showReorderPointsDialog) {
        ReorderPointsDialog(
            points = points,
            onDismiss = { showReorderPointsDialog = false },
            onSaveOrder = { newOrder ->
                onReorderPoints(newOrder)
            }
        )
    }
}

@Composable
private fun PointStockTableView(
    rows: List<TableInventoryRow>,
    searchQuery: String,
    showPointColumn: Boolean = false,
    onAdjustClick: (TableInventoryRow) -> Unit
) {
    if (rows.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, TacticalBorderSubtle)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isNotEmpty()) {
                        "По запросу «$searchQuery» ничего не найдено"
                    } else {
                        "На этом складе пока нет имущества на остатке"
                    },
                    color = TacticalTextMuted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    // Compact table: one line per item, numbers in aligned columns.
    // Never sum mixed units (e.g. pieces + liters) into one meaningless number!
    val distinctUnits = rows.map { it.item.unit.trim().ifBlank { "шт." } }.distinct()
    val singleUnit = distinctUnits.size == 1
    val totalIncome = rows.sumOf { it.incomeTotal }
    val totalExpense = rows.sumOf { it.expenseTotal }
    val totalQty = rows.sumOf { it.quantity }
    val activeRowsCount = rows.count { it.quantity > 0 }
    val unitSummary = com.example.ui.components.formatUnitBreakdown(
        unitQuantities = rows.filter { it.quantity > 0 }.map { it.item.unit to it.quantity },
        maxUnits = 4,
        emptyLabel = "Нет остатков"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(TacticalSurface)
            .border(1.dp, TacticalBorderSubtle, RoundedCornerShape(14.dp))
    ) {
        CompactStockRow(
            name = "Наименование",
            income = "Приход",
            expense = "Расход",
            balance = "Остаток",
            isHeader = true
        )
        rows.forEachIndexed { index, rowData ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(TacticalBorderSubtle.copy(alpha = 0.6f))
                )
            }
            CompactStockRow(
                name = rowData.item.name,
                subtitle = listOfNotNull(
                    rowData.item.unit.takeIf { it.isNotBlank() },
                    rowData.pointName.takeIf { showPointColumn }
                ).joinToString(" • "),
                income = rowData.incomeTotal.toString(),
                expense = rowData.expenseTotal.toString(),
                balance = rowData.quantity.toString(),
                balanceIsZero = rowData.quantity <= 0,
                striped = index % 2 == 1,
                onClick = { onAdjustClick(rowData) }
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(SageGreenPrimary.copy(alpha = 0.4f))
        )
        CompactStockRow(
            name = if (singleUnit) "Итого: ${rows.size} наим. (${distinctUnits.first()})" else "Всего позиций: $activeRowsCount из ${rows.size}",
            subtitle = if (!singleUnit) unitSummary else "",
            income = if (singleUnit) totalIncome.toString() else "—",
            expense = if (singleUnit) totalExpense.toString() else "—",
            balance = if (singleUnit) totalQty.toString() else "$activeRowsCount поз.",
            isTotal = true
        )
    }
    Text(
        text = "Нажмите на строку, чтобы изменить остаток",
        color = TacticalTextMuted,
        fontSize = 10.sp,
        modifier = Modifier.padding(start = 4.dp, top = 6.dp)
    )
}

@Composable
private fun CompactStockRow(
    name: String,
    income: String,
    expense: String,
    balance: String,
    subtitle: String = "",
    isHeader: Boolean = false,
    isTotal: Boolean = false,
    balanceIsZero: Boolean = false,
    striped: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val labelColor = TacticalTextMuted
    val background = when {
        isHeader -> TacticalSurfaceLight
        isTotal -> SageGreenDark.copy(alpha = 0.55f)
        striped -> TacticalSurfaceLight.copy(alpha = 0.35f)
        else -> Color.Transparent
    }
    val numberSize = if (isHeader) 10.sp else 13.sp
    val weight = if (isHeader) FontWeight.SemiBold else FontWeight.Bold
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 12.dp, vertical = if (isHeader) 7.dp else 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                color = if (isHeader) labelColor else TacticalTextPrimary,
                fontSize = if (isHeader) 10.sp else 12.5.sp,
                fontWeight = if (isHeader || isTotal) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 15.sp
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    color = TacticalTextMuted,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Text(
            text = if (isHeader || income == "—") income else if (income == "0") "0" else "+$income",
            color = if (isHeader || income == "—") labelColor else com.example.ui.theme.TacticalEmeraldText,
            fontSize = numberSize,
            fontWeight = weight,
            fontFamily = if (isHeader || income == "—") FontFamily.Default else FontFamily.Monospace,
            textAlign = TextAlign.End,
            modifier = Modifier.width(58.dp)
        )
        Text(
            text = if (isHeader || expense == "—") expense else if (expense == "0") "0" else "−$expense",
            color = when {
                isHeader || expense == "—" -> labelColor
                expense != "0" -> TacticalRedText
                else -> TacticalTextMuted
            },
            fontSize = numberSize,
            fontWeight = weight,
            fontFamily = if (isHeader || expense == "—") FontFamily.Default else FontFamily.Monospace,
            textAlign = TextAlign.End,
            modifier = Modifier.width(58.dp)
        )
        Text(
            text = balance,
            color = when {
                isHeader -> labelColor
                balanceIsZero -> TacticalRedText
                isTotal -> SageGreenBright
                else -> TacticalTextPrimary
            },
            fontSize = if (isHeader) 10.sp else 15.sp,
            fontWeight = if (isHeader) FontWeight.SemiBold else FontWeight.ExtraBold,
            fontFamily = if (isHeader) FontFamily.Default else FontFamily.Monospace,
            textAlign = TextAlign.End,
            modifier = Modifier.width(64.dp)
        )
    }
}

@Composable
private fun TableHeaderCell(
    text: String,
    modifier: Modifier = Modifier,
    width: androidx.compose.ui.unit.Dp? = null,
    color: Color = TacticalTextSecondary
) {
    val boxModifier = if (width != null) {
        modifier.width(width).padding(vertical = 6.dp, horizontal = 2.dp)
    } else {
        modifier.padding(vertical = 6.dp, horizontal = 3.dp)
    }
    Box(
        modifier = boxModifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            letterSpacing = 0.3.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TableVerticalDivider() {
    Box(
        modifier = Modifier
            .width(0.8.dp)
            .fillMaxHeight()
            .background(TacticalBorder)
    )
}

@Composable
private fun DashboardSummaryMetric(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    onGradient: Boolean = false
) {
    val numeric = value.toIntOrNull()
    val animated by androidx.compose.animation.core.animateIntAsState(
        targetValue = numeric ?: 0,
        animationSpec = androidx.compose.animation.core.tween(700),
        label = "metric"
    )
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (numeric != null) animated.toString() else value,
            color = if (onGradient) Color.White else TacticalTextPrimary,
            fontSize = 23.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = if (onGradient) Color.White.copy(alpha = 0.84f) else TacticalTextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            lineHeight = 12.sp,
            maxLines = 2
        )
    }
}

@Composable
private fun SectionIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(TacticalSurface)
            .border(1.dp, TacticalBorderSubtle, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = TacticalTextSecondary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SleekOperationTile(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentBg: Color = accentColor.copy(alpha = 0.14f)
) {
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(if (pressed) 0.94f else 1f, label = "press")
    Column(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(18.dp))
            .background(TacticalSurface)
            .border(1.dp, accentColor.copy(alpha = 0.28f), RoundedCornerShape(18.dp))
            .clickable(interactionSource = interaction, indication = androidx.compose.material3.ripple()) { onClick() }
            .padding(vertical = 11.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(accentBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            color = TacticalTextPrimary,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}
