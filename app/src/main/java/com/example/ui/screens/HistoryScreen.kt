package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InventoryItem
import com.example.data.model.OperationItemEntry
import com.example.data.model.OperationRecord
import com.example.data.model.OperationType
import com.example.ui.theme.SageGreenBright
import com.example.ui.theme.SageGreenDark
import com.example.ui.theme.SageGreenPrimary
import com.example.ui.theme.TacticalBg
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalBorderSubtle
import com.example.ui.theme.TacticalGoldDark
import com.example.ui.theme.TacticalGoldText
import com.example.ui.theme.TacticalRedDark
import com.example.ui.theme.TacticalRedText
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalSurfaceLight
import com.example.ui.theme.TacticalTealDark
import com.example.ui.theme.TacticalTealText
import com.example.ui.theme.TacticalTextDim
import com.example.ui.theme.TacticalTextMuted
import com.example.ui.theme.TacticalTextPrimary
import com.example.ui.theme.TacticalTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Category detection prefers the saved catalog. Unknown historical rows stay neutral.
fun resolveItemCategory(item: OperationItemEntry, catalog: List<InventoryItem>): String {
    return catalog
        .firstOrNull { it.id == item.itemId || it.name.equals(item.itemName, ignoreCase = true) }
        ?.serviceCategory
        ?.takeIf { it.isNotBlank() }
        ?: "Прочее"
}

fun getCategoryEmoji(category: String): String {
    val value = category.lowercase()
    return when {
        "авто" in value || "запчаст" in value -> "🚗"
        "мед" in value -> "🩺"
        "продукт" in value || "напит" in value || "еда" in value -> "🍽️"
        "инструмент" in value || "оснаст" in value -> "🧰"
        "компьют" in value || "it" in value || "перифер" in value -> "💻"
        "стро" in value || "крепёж" in value -> "🏗️"
        "сырь" in value || "производ" in value || "комплектующ" in value -> "🏭"
        "логист" in value || "отгруз" in value || "приёмк" in value -> "🚚"
        else -> "📦"
    }
}

@Composable
fun HistoryScreen(
    operations: List<OperationRecord>,
    filterType: OperationType?,
    searchQuery: String,
    catalogItems: List<InventoryItem> = emptyList(),
    availableCategories: List<String> = emptyList(),
    onFilterChange: (OperationType?) -> Unit,
    onSearchChange: (String) -> Unit,
    parseItems: (String) -> List<OperationItemEntry>
) {
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru")) }
    var selectedCategoryFilter by remember { mutableStateOf("Все категории") }
    val expandedOpIds = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(filterType, selectedCategoryFilter, searchQuery) {
        expandedOpIds.clear()
    }

    val filteredOperations = remember(operations, filterType, selectedCategoryFilter, searchQuery, catalogItems) {
        operations.filter { op ->
            val matchesType = filterType == null || op.type == filterType
            val parsedItems = parseItems(op.itemsJson)

            val matchesCategory = if (selectedCategoryFilter == "Все категории") {
                true
            } else {
                parsedItems.any { resolveItemCategory(it, catalogItems) == selectedCategoryFilter } ||
                        op.itemsSummary.contains(selectedCategoryFilter, ignoreCase = true)
            }

            val matchesQuery = if (searchQuery.trim().isEmpty()) true
            else {
                val q = searchQuery.trim().lowercase()
                op.docNumber.lowercase().contains(q) ||
                        op.fromPointName.lowercase().contains(q) ||
                        op.toPointName.lowercase().contains(q) ||
                        op.itemsSummary.lowercase().contains(q) ||
                        op.comment.lowercase().contains(q) ||
                        op.responsiblePerson.lowercase().contains(q) ||
                        parsedItems.any { 
                            it.itemName.lowercase().contains(q) || 
                                    resolveItemCategory(it, catalogItems).lowercase().contains(q) 
                        }
            }
            matchesType && matchesCategory && matchesQuery
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(TacticalBg)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // Top Header
        item {
            Column {
                Text(
                    text = "Операции",
                    color = TacticalTextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "История движения имущества и документов",
                    color = TacticalTextMuted,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // Search Field
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Поиск по операциям", color = TacticalTextDim, fontSize = 13.sp) },
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
                    .testTag("history_search_input"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = TacticalSurface,
                    unfocusedContainerColor = TacticalSurface,
                    focusedBorderColor = SageGreenPrimary,
                    unfocusedBorderColor = TacticalBorder,
                    focusedTextColor = TacticalTextPrimary,
                    unfocusedTextColor = TacticalTextPrimary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        // UNIFIED SINGLE FILTER ROW (One clean, informative line)
        item {
            val countIncome = remember(operations) { operations.count { it.type == OperationType.INCOME } }
            val countTransfer = remember(operations) { operations.count { it.type == OperationType.TRANSFER } }
            val countIssue = remember(operations) { operations.count { it.type == OperationType.ISSUE } }
            val countExpenditure = remember(operations) { operations.count { it.type == OperationType.EXPENDITURE } }

            val serviceChips = availableCategories.ifEmpty { listOf("Прочее") }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // All
                HistoryFilterChip(
                    label = "Все (${operations.size})",
                    isSelected = filterType == null && selectedCategoryFilter == "Все категории",
                    onClick = {
                        onFilterChange(null)
                        selectedCategoryFilter = "Все категории"
                    }
                )

                // Types
                HistoryFilterChip(
                    label = "Приход ($countIncome)",
                    isSelected = filterType == OperationType.INCOME,
                    onClick = {
                        onFilterChange(if (filterType == OperationType.INCOME) null else OperationType.INCOME)
                    }
                )
                HistoryFilterChip(
                    label = "Перемещение ($countTransfer)",
                    isSelected = filterType == OperationType.TRANSFER,
                    onClick = {
                        onFilterChange(if (filterType == OperationType.TRANSFER) null else OperationType.TRANSFER)
                    }
                )
                HistoryFilterChip(
                    label = "Выдача ($countIssue)",
                    isSelected = filterType == OperationType.ISSUE,
                    onClick = {
                        onFilterChange(if (filterType == OperationType.ISSUE) null else OperationType.ISSUE)
                    }
                )
                HistoryFilterChip(
                    label = "Списание ($countExpenditure)",
                    isSelected = filterType == OperationType.EXPENDITURE,
                    onClick = {
                        onFilterChange(if (filterType == OperationType.EXPENDITURE) null else OperationType.EXPENDITURE)
                    }
                )

                // Divider between types and services
                Box(
                    modifier = Modifier
                        .height(20.dp)
                        .width(1.dp)
                        .background(TacticalBorderSubtle)
                )

                // Services
                serviceChips.forEach { cat ->
                    val isSel = selectedCategoryFilter == cat
                    val emoji = getCategoryEmoji(cat)
                    val shortName = cat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSel) SageGreenDark else TacticalSurfaceLight)
                            .border(
                                1.dp,
                                if (isSel) SageGreenBright else TacticalBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                selectedCategoryFilter = if (selectedCategoryFilter == cat) "Все категории" else cat
                            }
                            .padding(horizontal = 9.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "$emoji $shortName",
                            color = if (isSel) SageGreenBright else TacticalTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }

        // Sub-header: Count indicator & Expand/Collapse Toggle
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Записей: ${filteredOperations.size}" +
                            if (selectedCategoryFilter != "Все категории") " • $selectedCategoryFilter" else "",
                    color = TacticalTextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (filteredOperations.isNotEmpty()) {
                    val allExpanded = filteredOperations.all { expandedOpIds[it.id] == true }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(TacticalSurfaceLight)
                            .border(1.dp, TacticalBorderSubtle, RoundedCornerShape(10.dp))
                            .clickable {
                                val target = !allExpanded
                                filteredOperations.forEach { expandedOpIds[it.id] = target }
                            }
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (allExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = SageGreenBright,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (allExpanded) "Свернуть все" else "Развернуть все",
                            color = SageGreenBright,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }

        // Main Operations Display
        if (filteredOperations.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = TacticalTextDim,
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Операций по заданным критериям не найдено",
                        color = TacticalTextMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(filteredOperations, key = { it.id }) { op ->
                val isOpExpanded = expandedOpIds[op.id] ?: false
                OperationAccordionCard(
                    operation = op,
                    dateFormat = dateFormat,
                    catalogItems = catalogItems,
                    parseItems = parseItems,
                    isExpanded = isOpExpanded,
                    onToggleExpand = {
                        expandedOpIds[op.id] = !isOpExpanded
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HistoryFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) SageGreenPrimary else TacticalSurfaceLight)
            .border(
                1.dp,
                if (isSelected) SageGreenBright else TacticalBorder,
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else TacticalTextSecondary,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun OperationAccordionCard(
    operation: OperationRecord,
    dateFormat: SimpleDateFormat,
    catalogItems: List<InventoryItem>,
    parseItems: (String) -> List<OperationItemEntry>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val items = remember(operation.itemsJson) { parseItems(operation.itemsJson) }

    val (badgeBg, badgeText, badgeTitle) = when (operation.type) {
        OperationType.INCOME -> Triple(SageGreenDark, SageGreenBright, "ПРИХОД")
        OperationType.TRANSFER -> Triple(TacticalTealDark, TacticalTealText, "ПЕРЕМЕЩЕНИЕ")
        OperationType.ISSUE -> Triple(TacticalGoldDark, TacticalGoldText, "ВЫДАЧА")
        OperationType.EXPENDITURE -> Triple(TacticalRedDark, TacticalRedText, "СПИСАНИЕ")
        OperationType.CORRECTION -> Triple(TacticalTealDark, TacticalTealText, "КОРРЕКТИРОВКА")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onToggleExpand() }
            .testTag("operation_card_${operation.id}"),
        colors = CardDefaults.cardColors(containerColor = TacticalSurface),
        border = BorderStroke(1.dp, if (isExpanded) SageGreenPrimary.copy(alpha = 0.35f) else TacticalBorderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 2.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // ROW 1: Type badge, doc number, date & chevron
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(badgeBg)
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = badgeTitle,
                            color = badgeText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (operation.docNumber.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(TacticalSurfaceLight)
                                .border(1.dp, TacticalBorderSubtle, RoundedCornerShape(10.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Акт № ${operation.docNumber}",
                                color = TacticalTextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dateFormat.format(Date(operation.timestamp)),
                        color = TacticalTextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Свернуть" else "Развернуть",
                        tint = if (isExpanded) SageGreenBright else TacticalTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ROW 2: Route Display - single clean line with balanced weights
            val (fromName, toName) = when (operation.type) {
                OperationType.INCOME -> Pair(
                    operation.fromPointName.ifBlank { "Поставщик / Снабжение" },
                    operation.toPointName.ifBlank { "Базовый склад" }
                )
                OperationType.TRANSFER -> Pair(
                    operation.fromPointName.ifBlank { "Склад отправки" },
                    operation.toPointName.ifBlank { "Склад назначения" }
                )
                OperationType.ISSUE -> Pair(
                    operation.fromPointName.ifBlank { "Базовый склад" },
                    operation.toPointName.ifBlank { "Получатель" }
                )
                OperationType.EXPENDITURE -> Pair(
                    operation.fromPointName.ifBlank { operation.toPointName.ifBlank { "Позиция" } },
                    "Списание"
                )
                OperationType.CORRECTION -> Pair(
                    operation.fromPointName.ifBlank { operation.toPointName.ifBlank { "Склад" } },
                    "Корректировка"
                )
            }

            // ROW 2: Route & Count (Compact single line)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = fromName,
                        color = SageGreenBright,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "➔",
                        color = SageGreenPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = toName,
                        color = TacticalTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val totalUnits = remember(items) { items.sumOf { it.quantity } }
                val countSummary = if (items.isNotEmpty()) {
                    "${items.size} наим. • $totalUnits ед."
                } else if (operation.itemsSummary.isNotBlank()) {
                    operation.itemsSummary.take(20)
                } else ""

                if (countSummary.isNotBlank()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "📦 $countSummary",
                        color = TacticalTextMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // EXPANDED SECTION: DETAILS & ACCORDION BY CATEGORY
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(TacticalBorderSubtle)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Responsible Person & Comment
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = TacticalTextDim,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Ответственный: ${operation.responsiblePerson.ifBlank { "Ответственный" }}",
                                color = TacticalTextMuted,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (operation.comment.isNotBlank()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "💬 ${operation.comment}",
                                color = TacticalGoldText,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                    }

                    // Summary of items banner
                    if (operation.itemsSummary.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(TacticalBg, RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = SageGreenPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = operation.itemsSummary,
                                color = TacticalTextSecondary,
                                fontSize = 12.sp,
                                maxLines = 6,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (items.isNotEmpty()) {
                        val groupedByCategory = remember(items, catalogItems) {
                            items.groupBy { resolveItemCategory(it, catalogItems) }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            groupedByCategory.forEach { (categoryName, categoryItems) ->
                                val emoji = getCategoryEmoji(categoryName)
                                val totalUnits = categoryItems.sumOf { it.quantity }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(TacticalBg)
                                        .border(1.dp, TacticalBorderSubtle, RoundedCornerShape(12.dp))
                                        .padding(8.dp)
                                ) {
                                    // Service Header
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f, fill = false)
                                        ) {
                                            Text(text = emoji, fontSize = 14.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = categoryName,
                                                color = SageGreenBright,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = "$totalUnits ед.",
                                            color = SageGreenBright,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Items
                                    categoryItems.forEachIndexed { idx, item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${idx + 1}. ${item.itemName}",
                                                color = TacticalTextPrimary,
                                                fontSize = 11.5.sp,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "${item.quantity} ${item.unit}",
                                                color = TacticalTextSecondary,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        if (item.reason.isNotBlank()) {
                                            Text(
                                                text = "   Цель: ${item.reason}",
                                                color = TacticalTextMuted,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(bottom = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else if (operation.itemsSummary.isNotBlank()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(TacticalBg)
                                .border(1.dp, TacticalBorderSubtle, RoundedCornerShape(12.dp))
                                .padding(8.dp)
                        ) {
                            operation.itemsSummary.split(",").forEachIndexed { idx, part ->
                                val clean = part.trim()
                                if (clean.isNotBlank()) {
                                    Text(
                                        text = "${idx + 1}. $clean",
                                        color = TacticalTextPrimary,
                                        fontSize = 11.5.sp,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (operation.comment.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Примечание: ${operation.comment}",
                            color = TacticalGoldText,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Зафиксировал: ${operation.responsiblePerson.ifBlank { "Ответственный" }} • ${dateFormat.format(Date(operation.timestamp))}",
                        color = TacticalTextDim,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

