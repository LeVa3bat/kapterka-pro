package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
    onAdjustStock: (pointId: String, pointName: String, itemId: String, itemName: String, newQuantity: Int) -> Unit,
    onSyncClick: () -> Unit,
    onSecondPhoneClick: () -> Unit = {},
    onExportClick: () -> Unit,
    onBannerClick: () -> Unit,
    onProfileClick: () -> Unit,
    onHelpClick: () -> Unit = {},
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onReorderPoints: (List<WarehousePoint>) -> Unit = {}
) {
    var selectedPointFilterId by remember { mutableStateOf<String?>(null) } // null = Все склады
    var adjustingStock by remember { mutableStateOf<PendingAdjustStock?>(null) }
    var showReorderPointsDialog by remember { mutableStateOf(false) }
    val expandedPointIds = remember { mutableStateMapOf<String, Boolean>() }

    val categories = remember(availableCategories) {
        if (availableCategories.isNotEmpty()) {
            listOf("Все виды") + availableCategories
        } else {
            listOf(
                "Все виды",
                "Служба РАВ",
                "Служба БПЛА и робототехники",
                "Служба связи и РЭБ",
                "Вещевая служба и СИБЗ",
                "Медицинская служба",
                "Инженерная служба",
                "Служба ГСМ",
                "Продовольственная служба",
                "Автомобильная и БТ служба",
                "Служба РХБЗ"
            )
        }
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
                val defaultItem = com.example.data.local.InitialData.getDefaultItems().find { it.id == st.itemId }
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
                        else -> "Служба РАВ"
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
    val overallStockSum = remember(stockRecords) { stockRecords.sumOf { it.quantity } }
    val overallPositionsCount = remember(stockRecords) { stockRecords.filter { it.quantity > 0 }.map { it.itemId }.distinct().size }

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
                onToggleTheme = onToggleTheme
            )
        }

        // DEMO OR PRO STATUS BANNER
        item {
            DemoBanner(
                profile = profile,
                onBannerClick = onBannerClick
            )
        }

        // COMPACT OVERVIEW
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, TacticalBorderSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DashboardSummaryMetric(
                        value = overallStockSum.toString(),
                        label = "единиц на учёте",
                        modifier = Modifier.weight(1f)
                    )
                    DashboardSummaryMetric(
                        value = overallPositionsCount.toString(),
                        label = "активных позиций",
                        modifier = Modifier.weight(1f)
                    )
                    DashboardSummaryMetric(
                        value = points.size.toString(),
                        label = "складов и точек",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // PRIMARY ACTIONS — one row, thumb-friendly
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SleekOperationTile(
                    title = "Приход",
                    icon = Icons.Default.MoveToInbox,
                    accentColor = SageGreenBright,
                    onClick = onIncomeClick,
                    modifier = Modifier.weight(1f).testTag("op_income_button")
                )
                SleekOperationTile(
                    title = "Выдача",
                    icon = Icons.Default.AssignmentInd,
                    accentColor = TacticalGoldText,
                    onClick = onIssueClick,
                    modifier = Modifier.weight(1f).testTag("op_issue_button")
                )
                SleekOperationTile(
                    title = "Перемещ.",
                    icon = Icons.Default.LocalShipping,
                    accentColor = TacticalTealText,
                    onClick = onTransferClick,
                    modifier = Modifier.weight(1f).testTag("op_transfer_button")
                )
                SleekOperationTile(
                    title = "Списание",
                    icon = Icons.Default.DeleteSweep,
                    accentColor = TacticalRedText,
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
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = TacticalSurface,
                    unfocusedContainerColor = TacticalSurface,
                    focusedBorderColor = SageGreenPrimary,
                    unfocusedBorderColor = TacticalBorder,
                    focusedTextColor = TacticalTextPrimary,
                    unfocusedTextColor = TacticalTextPrimary
                )
            )
        }

        // CATEGORY FILTER CHIPS
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = cat == selectedCategory
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) SageGreenPrimary else TacticalSurface)
                            .border(
                                1.dp,
                                if (isSelected) SageGreenBright else TacticalBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onSelectCategory(cat) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = cat,
                            color = if (isSelected) Color.White else TacticalTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
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
                    text = "Склады и точки",
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
                val pointStockSum = pointRows.sumOf { it.quantity }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        // Point Summary Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TacticalBorderSubtle),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
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
                                                    text = "Базовый склад",
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
                                        text = "Остаток на точке",
                                        color = TacticalTextMuted,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = "$pointStockSum ед.",
                                        color = SageGreenBright,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
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
                // Collapsible Point Lists to avoid information overload - hidden by default upon entrance
                itemsIndexed(points, key = { _, pt -> pt.id }) { _, point ->
                    val pointRows = rowsByPoint[point.id].orEmpty()
                    val pointStockSum = pointRows.sumOf { it.quantity }
                    // Default to collapsed (false) so that on entrance all lists are hidden
                    val isExpanded = expandedPointIds[point.id] ?: false

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TacticalBorderSubtle),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Point Header Row (Tappable to expand / collapse)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedPointIds[point.id] = !isExpanded
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (point.isBase) TacticalGoldDark else SageGreenDark),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (point.isBase) Icons.Default.Warehouse else Icons.Default.Inventory2,
                                            contentDescription = null,
                                            tint = if (point.isBase) TacticalGoldText else SageGreenBright,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column {
                                        Text(
                                            text = point.name,
                                            color = TacticalTextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = (if (point.isBase) "Базовый • " else "") +
                                                "${pointRows.size} наим. • +${pointRows.sumOf { it.incomeTotal }}" +
                                                " / −${pointRows.sumOf { it.expenseTotal }}",
                                            color = if (point.isBase) TacticalGoldText else TacticalTextMuted,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Изменить склад",
                                        tint = TacticalTextMuted,
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .clickable { onEditPointClick(point) }
                                            .padding(7.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(SageGreenDark)
                                            .border(1.dp, SageGreenPrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "$pointStockSum ед.",
                                            color = SageGreenBright,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
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
            onConfirm = { pId, pName, iId, iName, newQty ->
                onAdjustStock(pId, pName, iId, iName, newQty)
                adjustingStock = null
            }
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
    val totalIncome = rows.sumOf { it.incomeTotal }
    val totalExpense = rows.sumOf { it.expenseTotal }
    val totalQty = rows.sumOf { it.quantity }

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
            name = "Итого: ${rows.size} наим.",
            income = totalIncome.toString(),
            expense = totalExpense.toString(),
            balance = totalQty.toString(),
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
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
            text = if (isHeader) income else if (income == "0") "0" else "+$income",
            color = if (isHeader) labelColor else SageGreenBright,
            fontSize = numberSize,
            fontWeight = weight,
            fontFamily = if (isHeader) FontFamily.Default else FontFamily.Monospace,
            textAlign = TextAlign.End,
            modifier = Modifier.width(58.dp)
        )
        Text(
            text = if (isHeader) expense else if (expense == "0") "0" else "−$expense",
            color = when {
                isHeader -> labelColor
                expense != "0" -> TacticalRedText
                else -> TacticalTextMuted
            },
            fontSize = numberSize,
            fontWeight = weight,
            fontFamily = if (isHeader) FontFamily.Default else FontFamily.Monospace,
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
    modifier: Modifier = Modifier
) {
    // Numbers count up smoothly when they change.
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
            color = TacticalTextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1
        )
        Text(
            text = label,
            color = TacticalTextMuted,
            fontSize = 9.5.sp,
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
    modifier: Modifier = Modifier
) {
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(if (pressed) 0.94f else 1f, label = "press")
    Column(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(16.dp))
            .background(TacticalSurface)
            .border(1.dp, TacticalBorderSubtle, RoundedCornerShape(16.dp))
            .clickable(interactionSource = interaction, indication = androidx.compose.material3.ripple()) { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accentColor,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            color = TacticalTextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
