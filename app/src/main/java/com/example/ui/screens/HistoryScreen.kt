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

// Utility: Service Category detection
fun resolveItemCategory(item: OperationItemEntry, catalog: List<InventoryItem>): String {
    val found = catalog.firstOrNull { it.id == item.itemId || it.name.equals(item.itemName, ignoreCase = true) }
    if (found != null && found.serviceCategory.isNotBlank()) return found.serviceCategory
    val lower = item.itemName.lowercase()
    return when {
        lower.contains("мина") || lower.contains("выстрел") || lower.contains("патрон") || lower.contains("снаряд") ||
                lower.contains("вог") || lower.contains("граната") || lower.contains("автомат") || lower.contains("пулемет") ||
                lower.contains("ак-") || lower.contains("пкм") || lower.contains("рпг") || lower.contains("оружие") || lower.contains("рав") -> "Служба РАВ"
        lower.contains("дрон") || lower.contains("мавик") || lower.contains("mavic") || lower.contains("fpv") ||
                lower.contains("бпла") || lower.contains("аккумулятор") || lower.contains("батарея") || lower.contains("ретранслятор") || lower.contains("пульт") -> "Служба БПЛА и робототехники"
        lower.contains("форма") || lower.contains("ботинки") || lower.contains("вкпо") || lower.contains("плиты") ||
                lower.contains("бронежилет") || lower.contains("шлем") || lower.contains("разгрузка") || lower.contains("подсумок") ||
                lower.contains("костюм") || lower.contains("перчатки") || lower.contains("спальник") || lower.contains("каремат") -> "Вещевая служба"
        lower.contains("аптечка") || lower.contains("жгут") || lower.contains("турникет") || lower.contains("бинт") ||
                lower.contains("промедол") || lower.contains("нефопам") || lower.contains("пластырь") || lower.contains("ампул") ||
                lower.contains("ножницы") || lower.contains("окклюзионн") || lower.contains("гемостатик") || lower.contains("медицин") -> "Медицинская служба"
        lower.contains("вода") || lower.contains("сухпай") || lower.contains("ирп") || lower.contains("тушенка") ||
                lower.contains("консерв") || lower.contains("хлеб") || lower.contains("крупа") || lower.contains("сахар") ||
                lower.contains("чай") || lower.contains("кофе") || lower.contains("макарон") || lower.contains("продоволь") -> "Продовольственная служба"
        lower.contains("дизель") || lower.contains("бензин") || lower.contains("масло") || lower.contains("гсм") ||
                lower.contains("канистра") || lower.contains("топливо") || lower.contains("солярка") || lower.contains("генератор") -> "ГСМ / Техническая служба"
        lower.contains("рация") || lower.contains("радиостанция") || lower.contains("кабель") || lower.contains("антенна") ||
                lower.contains("та-57") || lower.contains("связь") || lower.contains("гарнитура") -> "Служба связи"
        else -> "Прочее имущество"
    }
}

fun getCategoryEmoji(category: String): String {
    return when {
        category.contains("РАВ") -> "💣"
        category.contains("БПЛА") -> "🛸"
        category.contains("Вещев") -> "👕"
        category.contains("Медицин") -> "💊"
        category.contains("Продоволь") -> "🥫"
        category.contains("ГСМ") || category.contains("Техническ") -> "⛽"
        category.contains("Связь") -> "📻"
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
    var selectedCategoryFilter by remember { mutableStateOf("Все службы") }
    val expandedOpIds = remember { mutableStateMapOf<String, Boolean>() }

    val filteredOperations = remember(operations, filterType, selectedCategoryFilter, searchQuery, catalogItems) {
        operations.filter { op ->
            val matchesType = filterType == null || op.type == filterType
            val parsedItems = parseItems(op.itemsJson)

            val matchesCategory = if (selectedCategoryFilter == "Все службы") {
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
                    text = "ЖУРНАЛ ОПЕРАЦИЙ",
                    color = SageGreenBright,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Тактическая история движения и проводок имущества",
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
                placeholder = { Text("Поиск: склад, поставщик, наименование, акт №...", color = TacticalTextDim, fontSize = 13.sp) },
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
                shape = RoundedCornerShape(8.dp),
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

            val serviceChips = listOf(
                "Служба РАВ",
                "Служба БПЛА и робототехники",
                "Вещевая служба",
                "Медицинская служба",
                "Продовольственная служба",
                "ГСМ / Техническая служба",
                "Служба связи",
                "Прочее имущество"
            )

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
                    isSelected = filterType == null && selectedCategoryFilter == "Все службы",
                    onClick = {
                        onFilterChange(null)
                        selectedCategoryFilter = "Все службы"
                    }
                )

                // Types
                HistoryFilterChip(
                    label = "Привезли ($countIncome)",
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
                    label = "Подняли ($countIssue)",
                    isSelected = filterType == OperationType.ISSUE,
                    onClick = {
                        onFilterChange(if (filterType == OperationType.ISSUE) null else OperationType.ISSUE)
                    }
                )
                HistoryFilterChip(
                    label = "Расход ($countExpenditure)",
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
                    val shortName = when (cat) {
                        "Служба РАВ" -> "РАВ"
                        "Служба БПЛА и робототехники" -> "БПЛА"
                        "Вещевая служба" -> "Вещевая"
                        "Медицинская служба" -> "Мед"
                        "Продовольственная служба" -> "Прод"
                        "ГСМ / Техническая служба" -> "ГСМ"
                        "Служба связи" -> "Связь"
                        else -> "Прочее"
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSel) SageGreenDark else TacticalSurfaceLight)
                            .border(
                                1.dp,
                                if (isSel) SageGreenBright else TacticalBorder,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable {
                                selectedCategoryFilter = if (selectedCategoryFilter == cat) "Все службы" else cat
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
                    text = "Проводок: ${filteredOperations.size}" +
                            if (selectedCategoryFilter != "Все службы") " • $selectedCategoryFilter" else "",
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
                            .clip(RoundedCornerShape(4.dp))
                            .background(TacticalSurfaceLight)
                            .border(1.dp, TacticalBorderSubtle, RoundedCornerShape(4.dp))
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
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) SageGreenPrimary else TacticalSurfaceLight)
            .border(
                1.dp,
                if (isSelected) SageGreenBright else TacticalBorder,
                RoundedCornerShape(6.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) Color(0xFF0F1B14) else TacticalTextSecondary,
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
        OperationType.INCOME -> Triple(SageGreenDark, SageGreenBright, "ПРИВЕЗЛИ")
        OperationType.TRANSFER -> Triple(TacticalTealDark, TacticalTealText, "ПЕРЕМЕЩЕНИЕ")
        OperationType.ISSUE -> Triple(TacticalGoldDark, TacticalGoldText, "ПОДНЯЛИ")
        OperationType.EXPENDITURE -> Triple(TacticalRedDark, TacticalRedText, "ОТСТРЕЛ (Ф. 8)")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onToggleExpand() }
            .testTag("operation_card_${operation.id}"),
        colors = CardDefaults.cardColors(containerColor = TacticalSurface),
        border = BorderStroke(1.dp, if (isExpanded) SageGreenPrimary else TacticalBorder)
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
                            .clip(RoundedCornerShape(4.dp))
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
                                .clip(RoundedCornerShape(4.dp))
                                .background(TacticalSurfaceLight)
                                .border(1.dp, TacticalBorderSubtle, RoundedCornerShape(4.dp))
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
                    operation.fromPointName.ifBlank { "Снабжение / Тыл" },
                    operation.toPointName.ifBlank { "Базовый склад" }
                )
                OperationType.TRANSFER -> Pair(
                    operation.fromPointName.ifBlank { "Склад отправки" },
                    operation.toPointName.ifBlank { "Склад назначения" }
                )
                OperationType.ISSUE -> Pair(
                    operation.fromPointName.ifBlank { "Базовый склад" },
                    operation.toPointName.ifBlank { "Подразделение" }
                )
                OperationType.EXPENDITURE -> Pair(
                    operation.fromPointName.ifBlank { operation.toPointName.ifBlank { "Позиция" } },
                    "Списание (ф. 8)"
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = fromName,
                    color = SageGreenBright,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "➔",
                    color = SageGreenPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = toName,
                    color = TacticalTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            // ROW 3: Summary of items (always visible on card)
            if (operation.itemsSummary.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TacticalBg, RoundedCornerShape(4.dp))
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
                        maxLines = if (isExpanded) 6 else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // ROW 4: Responsible Person and Comment (Always visible)
            Spacer(modifier = Modifier.height(6.dp))
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
                        text = "Ответственный: ${operation.responsiblePerson.ifBlank { "Старшина" }}",
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

            // EXPANDED SECTION: ACCORDION BY CATEGORY
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(TacticalBorderSubtle)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

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
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(TacticalBg)
                                        .border(1.dp, TacticalBorderSubtle, RoundedCornerShape(6.dp))
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
                                .clip(RoundedCornerShape(6.dp))
                                .background(TacticalBg)
                                .border(1.dp, TacticalBorderSubtle, RoundedCornerShape(6.dp))
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
                            text = "Примечание / Задача: ${operation.comment}",
                            color = TacticalGoldText,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Зафиксировал в системе: ${operation.responsiblePerson.ifBlank { "Старшина" }} • ${dateFormat.format(Date(operation.timestamp))}",
                        color = TacticalTextDim,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

