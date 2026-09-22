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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
        listOf("Все виды") + availableCategories.ifEmpty {
            com.example.data.local.InitialData.getDefaultCategories()
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
                    val fromOp = operationItemsMap[st.itemId]
                    val friendlyName = fromOp?.itemName?.takeIf { it.isNotBlank() }
                        ?: existing?.name?.takeIf { it.isNotBlank() }
                        ?: "Позиция ${st.itemId.take(8)}"
                    val resolvedCat = existing?.serviceCategory?.takeIf { it.isNotBlank() }
                        ?: availableCategories.firstOrNull()
                        ?: "Прочее"
                    val unit = fromOp?.unit?.takeIf { it.isNotBlank() }
                        ?: existing?.unit?.takeIf { it.isNotBlank() }
                        ?: "шт."
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
    val allStockRows = remember(points, catalogItems, stockRecords, searchQuery, selectedCategory, operations) {
        points.flatMap { getItemsForPoint(it.id) }
    }
    val overallStockSum = stockRecords.sumOf { it.quantity }
    val overallPositionsCount = stockRecords.filter { it.quantity > 0 }.map { it.itemId }.distinct().size

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

        // PRIMARY ACTIONS — optimized for phone screens
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "БЫСТРЫЕ ОПЕРАЦИИ",
                    color = TacticalTextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SleekOperationTile(
                        title = "Приход",
                        subtitle = "Поступление на склад",
                        icon = Icons.Default.LocalShipping,
                        accentColor = SageGreenBright,
                        onClick = onIncomeClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("op_income_button")
                    )

                    SleekOperationTile(
                        title = "Перемещение",
                        subtitle = "Между складами",
                        icon = Icons.AutoMirrored.Filled.Send,
                        accentColor = TacticalTealText,
                        onClick = onTransferClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("op_transfer_button")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SleekOperationTile(
                        title = "Выдача",
                        subtitle = "Передача получателю",
                        icon = Icons.Default.FlightTakeoff,
                        accentColor = TacticalGoldText,
                        onClick = onIssueClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("op_issue_button")
                    )

                    SleekOperationTile(
                        title = "Списание",
                        subtitle = "Расход имущества",
                        icon = Icons.Default.NorthEast,
                        accentColor = TacticalRedText,
                        isHighlighted = true,
                        onClick = onExpenditureClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("op_expenditure_button")
                    )
                }
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

        // WAREHOUSE / POINT FILTER TABS & ADD POINT ACTION
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warehouse,
                            contentDescription = null,
                            tint = SageGreenPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "СКЛАДЫ И ТОЧКИ УЧЁТА",
                            color = TacticalTextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Add Point button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(SageGreenDark)
                            .border(1.dp, SageGreenPrimary, RoundedCornerShape(12.dp))
                            .clickable { onAddPointClick() }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Добавить склад",
                            tint = SageGreenBright,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Добавить",
                            color = SageGreenBright,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Point Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // All Points Chip
                    val isAllSelected = selectedPointFilterId == null
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isAllSelected) SageGreenDark else TacticalSurface)
                            .border(
                                1.dp,
                                if (isAllSelected) SageGreenPrimary else TacticalBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedPointFilterId = null }
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "Все склады (${points.size})",
                            color = if (isAllSelected) SageGreenBright else TacticalTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    points.forEach { pt ->
                        val isPtSelected = selectedPointFilterId == pt.id
                        val pointItemCount = (stockMap[pt.id] ?: emptyList()).filter { it.quantity > 0 }.size

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isPtSelected) SageGreenDark else TacticalSurface)
                                .border(
                                    1.dp,
                                    if (isPtSelected) SageGreenPrimary else TacticalBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedPointFilterId = pt.id }
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = pt.name,
                                color = if (isPtSelected) SageGreenBright else TacticalTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isPtSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (pt.isBase) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "★",
                                    color = TacticalGold,
                                    fontSize = 10.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "($pointItemCount)",
                                color = if (isPtSelected) SageGreenBright else TacticalTextMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Редактировать",
                                tint = TacticalTextMuted,
                                modifier = Modifier
                                    .size(12.dp)
                                    .clickable { onEditPointClick(pt) }
                            )
                        }
                    }

                    // Quick Reorder Button Chip
                    if (points.size > 1) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(TacticalSurface)
                                .border(1.dp, TacticalBorder, RoundedCornerShape(12.dp))
                                .clickable { showReorderPointsDialog = true }
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = "Порядок точек",
                                tint = SageGreenBright,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Порядок",
                                color = TacticalTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // CONTENT SECTION:
        // Case A: Specific Point is selected -> Concise Stock & Remaining Balance for that Point
        // Case B: All Points -> Collapsible point cards (information hidden in lists) + search results
        if (selectedPointFilterId != null) {
            val currentPoint = points.firstOrNull { it.id == selectedPointFilterId }
            if (currentPoint != null) {
                val pointRows = getItemsForPoint(currentPoint.id)
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
                // Quick Expand/Collapse all warehouses bar
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Склады и точки (${points.size})",
                            color = TacticalTextMuted,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (points.size > 1) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(TacticalGoldDark.copy(alpha = 0.25f))
                                        .border(1.dp, TacticalGold.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                                        .clickable {
                                            showReorderPointsDialog = true
                                        }
                                        .padding(horizontal = 7.dp, vertical = 3.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.SwapVert,
                                            contentDescription = "Порядок",
                                            tint = TacticalGoldText,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Порядок",
                                            color = TacticalGoldText,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(TacticalSurfaceLight)
                                    .clickable {
                                        points.forEach { pt -> expandedPointIds[pt.id] = false }
                                    }
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "Свернуть все",
                                    color = TacticalTextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(TacticalSurfaceLight)
                                    .clickable {
                                        points.forEach { pt -> expandedPointIds[pt.id] = true }
                                    }
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "Развернуть все",
                                    color = SageGreenBright,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                // Collapsible Point Lists to avoid information overload - hidden by default upon entrance
                itemsIndexed(points, key = { _, pt -> pt.id }) { _, point ->
                    val pointRows = getItemsForPoint(point.id)
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
                                            imageVector = Icons.Default.Warehouse,
                                            contentDescription = null,
                                            tint = if (point.isBase) TacticalGoldText else SageGreenBright,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = point.name,
                                                color = TacticalTextPrimary,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (point.isBase) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "★ Базовый",
                                                    color = TacticalGoldText,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${pointRows.size} наим. • остаток: $pointStockSum ед.",
                                            color = TacticalTextMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
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

                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = if (isExpanded) "Свернуть" else "Развернуть",
                                        tint = SageGreenPrimary,
                                        modifier = Modifier.size(22.dp)
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

                // Overall Battalion Summary Bar
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TacticalBorderSubtle),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$overallPositionsCount активных позиций",
                                color = TacticalTextSecondary,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$overallStockSum ед. на учёте",
                                color = SageGreenBright,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
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

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEach { rowData ->
            val isZero = rowData.quantity <= 0

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, TacticalBorderSubtle),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(13.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = rowData.item.name,
                                color = TacticalTextPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(3.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                if (rowData.item.subType.isNotBlank()) {
                                    Text(
                                        text = rowData.item.subType,
                                        color = TacticalTextMuted,
                                        fontSize = 10.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (rowData.item.standardCode.isNotBlank()) {
                                    Text(
                                        text = "• ${rowData.item.standardCode}",
                                        color = TacticalTextMuted,
                                        fontSize = 10.5.sp,
                                        maxLines = 1
                                    )
                                }
                            }

                            if (showPointColumn) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = rowData.pointName,
                                    color = SageGreenPrimary,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Остаток",
                                color = TacticalTextMuted,
                                fontSize = 9.5.sp
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = rowData.quantity.toString(),
                                    color = if (isZero) TacticalRedText else TacticalTextPrimary,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = rowData.item.unit,
                                    color = TacticalTextMuted,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(bottom = 3.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(SageGreenDark)
                                .padding(horizontal = 9.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Приход +${rowData.incomeTotal}",
                                color = SageGreenBright,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (rowData.expenseTotal > 0) TacticalRedDark else TacticalSurfaceLight)
                                .padding(horizontal = 9.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Расход −${rowData.expenseTotal}",
                                color = if (rowData.expenseTotal > 0) TacticalRedText else TacticalTextMuted,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(TacticalSurfaceLight)
                                .clickable { onAdjustClick(rowData) }
                                .padding(horizontal = 9.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Изменить остаток",
                                tint = SageGreenPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Изменить",
                                color = TacticalTextSecondary,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
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
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
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
private fun SleekOperationTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) TacticalRedDark.copy(alpha = 0.4f) else TacticalSurface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isHighlighted) TacticalRed.copy(alpha = 0.5f) else TacticalBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = title,
                color = TacticalTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Text(
                text = subtitle,
                color = TacticalTextMuted,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}
