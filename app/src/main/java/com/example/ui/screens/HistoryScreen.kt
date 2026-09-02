package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@Composable
fun HistoryScreen(
    operations: List<OperationRecord>,
    filterType: OperationType?,
    searchQuery: String,
    onFilterChange: (OperationType?) -> Unit,
    onSearchChange: (String) -> Unit,
    parseItems: (String) -> List<OperationItemEntry>
) {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru"))

    val filteredOperations = operations.filter { op ->
        val matchesType = filterType == null || op.type == filterType
        val matchesQuery = if (searchQuery.trim().isEmpty()) true
        else {
            val q = searchQuery.trim().lowercase()
            op.docNumber.lowercase().contains(q) ||
                    op.fromPointName.lowercase().contains(q) ||
                    op.toPointName.lowercase().contains(q) ||
                    op.itemsSummary.lowercase().contains(q) ||
                    op.comment.lowercase().contains(q) ||
                    op.responsiblePerson.lowercase().contains(q)
        }
        matchesType && matchesQuery
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
                    text = "История приходов, перемещений, выдач и расхода (ф.8)",
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
                placeholder = { Text("Введите название", color = TacticalTextDim, fontSize = 13.sp) },
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

        // Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HistoryFilterChip(
                    label = "Все операции (${operations.size})",
                    isSelected = filterType == null,
                    onClick = { onFilterChange(null) }
                )
                HistoryFilterChip(
                    label = "Расход (ф. 8)",
                    isSelected = filterType == OperationType.EXPENDITURE,
                    onClick = { onFilterChange(OperationType.EXPENDITURE) }
                )
                HistoryFilterChip(
                    label = "Привезли",
                    isSelected = filterType == OperationType.INCOME,
                    onClick = { onFilterChange(OperationType.INCOME) }
                )
                HistoryFilterChip(
                    label = "Перемещение",
                    isSelected = filterType == OperationType.TRANSFER,
                    onClick = { onFilterChange(OperationType.TRANSFER) }
                )
                HistoryFilterChip(
                    label = "Подняли",
                    isSelected = filterType == OperationType.ISSUE,
                    onClick = { onFilterChange(OperationType.ISSUE) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Operation Accordions List
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
                OperationAccordionCard(
                    operation = op,
                    dateFormat = dateFormat,
                    parseItems = parseItems
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
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
    parseItems: (String) -> List<OperationItemEntry>
) {
    var expanded by remember { mutableStateOf(false) }
    val items = parseItems(operation.itemsJson)

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
            .clickable { expanded = !expanded }
            .testTag("operation_card_${operation.id}"),
        colors = CardDefaults.cardColors(containerColor = TacticalSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, TacticalBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header: Type badge & Date & Expand icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "№ ${operation.docNumber}",
                        color = TacticalTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dateFormat.format(Date(operation.timestamp)),
                        color = TacticalTextMuted,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = TacticalTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Route: From -> To
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = operation.fromPointName,
                    color = TacticalTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = " ➔ ",
                    color = SageGreenPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = operation.toPointName,
                    color = SageGreenBright,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Items Summary
            Text(
                text = operation.itemsSummary,
                color = TacticalTextSecondary,
                fontSize = 12.sp,
                maxLines = if (expanded) Int.MAX_VALUE else 2
            )

            // EXPANDABLE ACCORDION DETAILS
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .background(TacticalBg)
                        .border(1.dp, TacticalBorderSubtle, RoundedCornerShape(6.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "ДЕТАЛИЗАЦИЯ ПРОВОДКИ:",
                        color = TacticalTextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    if (items.isNotEmpty()) {
                        items.forEachIndexed { idx, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${idx + 1}. ${item.itemName}",
                                    color = TacticalTextPrimary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${item.quantity} ${item.unit}",
                                    color = SageGreenBright,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            if (item.reason.isNotEmpty()) {
                                Text(
                                    text = "   Цель: ${item.reason}",
                                    color = TacticalTextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    if (operation.comment.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Примечание: ${operation.comment}",
                            color = TacticalGoldText,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ответственный: ${operation.responsiblePerson.ifEmpty { "Старшина" }}",
                        color = TacticalTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
