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
import androidx.compose.ui.unit.sp
import com.example.data.model.InventoryItem
import com.example.data.model.StockRecord
import com.example.data.model.UserProfile
import com.example.data.model.WarehousePoint
import com.example.ui.components.AdjustStockDialog
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
    onHelpClick: () -> Unit = {}
) {
    var selectedPointFilterId by remember { mutableStateOf<String?>(null) } // null = Все склады
    var adjustingStock by remember { mutableStateOf<PendingAdjustStock?>(null) }
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

    // Prepare table items for a specific point or all points
    fun getItemsForPoint(pointId: String): List<TableInventoryRow> {
        val cleanQuery = searchQuery.trim().lowercase()
        val stocksForPoint = (stockMap[pointId] ?: emptyList()).associateBy { it.itemId }
        val pt = points.firstOrNull { it.id == pointId } ?: return emptyList()

        return catalogItems.mapNotNull { item ->
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
    val allStockRows = remember(points, catalogItems, stockRecords, searchQuery, selectedCategory) {
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
                onHelpClick = onHelpClick
            )
        }

        // 4 MAIN OPERATION TILES (Приход, Перенос, Выдача, Расход ф.8)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SleekOperationTile(
                        title = "Приход",
                        subtitle = "Поступление",
                        icon = Icons.Default.LocalShipping,
                        accentColor = SageGreenBright,
                        onClick = onIncomeClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("op_income_button")
                    )

                    SleekOperationTile(
                        title = "Перенос",
                        subtitle = "Локации",
                        icon = Icons.AutoMirrored.Filled.Send,
                        accentColor = TacticalTealText,
                        onClick = onTransferClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("op_transfer_button")
                    )

                    SleekOperationTile(
                        title = "Выдача",
                        subtitle = "В руки / Бойцу",
                        icon = Icons.Default.FlightTakeoff,
                        accentColor = TacticalGoldText,
                        onClick = onIssueClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("op_issue_button")
                    )

                    SleekOperationTile(
                        title = "Расход",
                        subtitle = "Акт ф.8",
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
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ТОЧКИ И СКЛАДЫ УЧЕТА",
                            color = TacticalTextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Add Point button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(SageGreenDark)
                            .border(1.dp, SageGreenPrimary, RoundedCornerShape(6.dp))
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
                            text = "Склад / Точка",
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
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isAllSelected) SageGreenDark else TacticalSurface)
                            .border(
                                1.dp,
                                if (isAllSelected) SageGreenPrimary else TacticalBorder,
                                RoundedCornerShape(6.dp)
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
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isPtSelected) SageGreenDark else TacticalSurface)
                                .border(
                                    1.dp,
                                    if (isPtSelected) SageGreenPrimary else TacticalBorder,
                                    RoundedCornerShape(6.dp)
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
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TacticalBorder)
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
                                                    .clip(RoundedCornerShape(4.dp))
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
                                        text = "ОСТАТОК НА ТОЧКЕ",
                                        color = TacticalTextMuted,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = "$pointStockSum ЕД.",
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
                            text = "РЕЗУЛЬТАТЫ ПОИСКА: ${allStockRows.size} ПОЗ.",
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
                // Collapsible Point Lists to avoid information overload
                itemsIndexed(points, key = { _, pt -> pt.id }) { _, point ->
                    val pointRows = getItemsForPoint(point.id)
                    val pointStockSum = pointRows.sumOf { it.quantity }
                    val isExpanded = expandedPointIds[point.id] ?: false

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TacticalBorder)
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
                                            .clip(RoundedCornerShape(6.dp))
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
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(SageGreenDark)
                                            .border(1.dp, SageGreenPrimary.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
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
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TacticalBorderSubtle)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ВСЕГО ПО ПОДРАЗДЕЛЕНИЮ: $overallPositionsCount ПОЗ.",
                                color = TacticalTextSecondary,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "ОСТАТОК: $overallStockSum ЕД.",
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
}

@Composable
private fun PointStockTableView(
    rows: List<TableInventoryRow>,
    searchQuery: String,
    showPointColumn: Boolean = false,
    onAdjustClick: (TableInventoryRow) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = TacticalSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, TacticalBorder)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Table Header with visible borders
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFDDE5DC))
                    .border(androidx.compose.foundation.BorderStroke(0.8.dp, TacticalBorder))
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TableHeaderCell(text = "№", width = 30.dp)
                TableVerticalDivider()
                TableHeaderCell(text = "НАИМЕНОВАНИЕ / СЛУЖБА", modifier = Modifier.weight(2.6f))
                TableVerticalDivider()
                if (showPointColumn) {
                    TableHeaderCell(text = "ТОЧКА", modifier = Modifier.weight(1.1f))
                    TableVerticalDivider()
                } else {
                    TableHeaderCell(text = "ЕД.", modifier = Modifier.weight(0.6f))
                    TableVerticalDivider()
                }
                TableHeaderCell(text = "ПРИХ.", modifier = Modifier.weight(0.7f))
                TableVerticalDivider()
                TableHeaderCell(text = "РАСХ.", modifier = Modifier.weight(0.7f))
                TableVerticalDivider()
                TableHeaderCell(text = "ОСТАТОК", modifier = Modifier.weight(1.0f), color = SageGreenBright)
                TableVerticalDivider()
                TableHeaderCell(text = "ИЗМ.", modifier = Modifier.weight(0.55f))
            }

            if (rows.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotEmpty()) "По запросу «$searchQuery» ничего не найдено" else "На этой точке пока нет имущества на остатке",
                        color = TacticalTextMuted,
                        fontSize = 11.5.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                rows.forEachIndexed { index, rowData ->
                    val isEven = index % 2 == 0
                    val isZero = rowData.quantity <= 0

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isEven) TacticalSurface else TacticalSurfaceLight)
                            .border(androidx.compose.foundation.BorderStroke(0.5.dp, TacticalBorderSubtle))
                            .height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Cell 1: Index №
                        Box(
                            modifier = Modifier
                                .width(30.dp)
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                color = TacticalTextMuted,
                                fontSize = 9.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        TableVerticalDivider()

                        // Cell 2: Item Name & Service Category
                        Column(
                            modifier = Modifier
                                .weight(2.6f)
                                .padding(horizontal = 6.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = rowData.item.name,
                                color = TacticalTextPrimary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = rowData.item.subType.uppercase(),
                                    color = TacticalTextMuted,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                if (rowData.item.standardCode.isNotBlank()) {
                                    Text(
                                        text = " • ${rowData.item.standardCode}",
                                        color = SageGreenBright,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        TableVerticalDivider()

                        // Cell 3: Unit or Point
                        if (showPointColumn) {
                            Box(
                                modifier = Modifier
                                    .weight(1.1f)
                                    .padding(horizontal = 4.dp, vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = rowData.pointName,
                                    color = TacticalTextSecondary,
                                    fontSize = 9.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            TableVerticalDivider()
                        } else {
                            Box(
                                modifier = Modifier
                                    .weight(0.6f)
                                    .padding(horizontal = 2.dp, vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = rowData.item.unit,
                                    color = TacticalTextSecondary,
                                    fontSize = 9.5.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                            TableVerticalDivider()
                        }

                        // Cell 4: Income
                        Box(
                            modifier = Modifier
                                .weight(0.7f)
                                .padding(horizontal = 2.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (rowData.incomeTotal > 0) "+${rowData.incomeTotal}" else "-",
                                color = if (rowData.incomeTotal > 0) SageGreenBright else TacticalTextDim,
                                fontSize = 10.5.sp,
                                fontWeight = if (rowData.incomeTotal > 0) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center
                            )
                        }
                        TableVerticalDivider()

                        // Cell 5: Expense
                        Box(
                            modifier = Modifier
                                .weight(0.7f)
                                .padding(horizontal = 2.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (rowData.expenseTotal > 0) "-${rowData.expenseTotal}" else "-",
                                color = if (rowData.expenseTotal > 0) TacticalGoldText else TacticalTextDim,
                                fontSize = 10.5.sp,
                                fontWeight = if (rowData.expenseTotal > 0) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center
                            )
                        }
                        TableVerticalDivider()

                        // Cell 6: Remaining Stock Quantity Badge
                        Box(
                            modifier = Modifier
                                .weight(1.0f)
                                .padding(horizontal = 4.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(if (isZero) TacticalRedDark else SageGreenDark)
                                    .border(
                                        0.8.dp,
                                        if (isZero) TacticalRed.copy(alpha = 0.5f) else SageGreenPrimary.copy(alpha = 0.5f),
                                        RoundedCornerShape(5.dp)
                                    )
                                    .padding(vertical = 2.5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${rowData.quantity}",
                                    color = if (isZero) TacticalRedText else SageGreenBright,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        TableVerticalDivider()

                        // Cell 7: Adjust Action Button
                        Box(
                            modifier = Modifier
                                .weight(0.55f)
                                .clickable { onAdjustClick(rowData) }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Корректировка",
                                tint = SageGreenPrimary,
                                modifier = Modifier.size(15.dp)
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
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = title,
                color = TacticalTextPrimary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Text(
                text = subtitle,
                color = TacticalTextMuted,
                fontSize = 8.5.sp,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}
