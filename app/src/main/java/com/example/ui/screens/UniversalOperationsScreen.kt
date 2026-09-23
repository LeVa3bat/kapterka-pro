package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OperationRecord
import com.example.data.model.OperationType
import com.example.data.model.WarehousePoint
import com.example.universal.WarehouseProfileCatalog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
import java.util.Locale

private val OpsBg = Color(0xFFF5F7FB)
private val OpsInk = Color(0xFF111827)
private val OpsMuted = Color(0xFF6B7280)
private val OpsPrimary = Color(0xFF5B5CE2)
private val OpsPrimarySoft = Color(0xFFEEEEFF)

@Composable
fun UniversalOperationsScreen(
    warehouseProfileId: String?,
    points: List<WarehousePoint>,
    selectedPointId: String,
    onSelectPoint: (String) -> Unit,
    operations: List<OperationRecord>
) {
    val template = WarehouseProfileCatalog.find(warehouseProfileId)
    val vocab = template.operations
    var query by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf<OperationType?>(null) }
    var showAllWarehouses by remember { mutableStateOf(false) }
    val selectedPoint = remember(points, selectedPointId) {
        points.firstOrNull { it.id == selectedPointId } ?: points.firstOrNull()
    }
    val scopedOperations = remember(operations, selectedPoint?.name, showAllWarehouses) {
        if (showAllWarehouses || selectedPoint == null) {
            operations
        } else {
            val pointName = selectedPoint.name
            operations.filter {
                it.fromPointName == pointName || it.toPointName == pointName
            }
        }
    }

    val filtered = remember(scopedOperations, query, typeFilter) {
        val q = query.trim().lowercase()
        scopedOperations
            .asSequence()
            .filter { typeFilter == null || it.type == typeFilter }
            .filter {
                q.isBlank() ||
                    it.itemsSummary.lowercase().contains(q) ||
                    it.fromPointName.lowercase().contains(q) ||
                    it.toPointName.lowercase().contains(q) ||
                    it.comment.lowercase().contains(q) ||
                    it.docNumber.lowercase().contains(q)
            }
            .sortedByDescending { it.timestamp }
            .toList()
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM • HH:mm", Locale("ru")) }
    val todayStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val todayCount = remember(scopedOperations, todayStart) {
        scopedOperations.count { it.timestamp >= todayStart }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OpsBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Операции",
                color = OpsInk,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Журнал всех движений по складу",
                color = OpsMuted,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OperationSummaryCard(
                    value = scopedOperations.size.toString(),
                    label = if (showAllWarehouses) "все склады" else "этот склад",
                    modifier = Modifier.weight(1f)
                )
                OperationSummaryCard(
                    value = todayCount.toString(),
                    label = "сегодня",
                    modifier = Modifier.weight(1f)
                )
                OperationSummaryCard(
                    value = filtered.size.toString(),
                    label = "показано",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (points.isNotEmpty()) {
                Text(
                    text = "Склад",
                    color = OpsMuted,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    UniversalFilterChip("Все склады", showAllWarehouses) {
                        showAllWarehouses = true
                    }
                    points.forEach { point ->
                        UniversalFilterChip(
                            point.name,
                            !showAllWarehouses && point.id == selectedPoint?.id
                        ) {
                            showAllWarehouses = false
                            onSelectPoint(point.id)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Поиск по истории", color = Color(0xFF9CA3AF)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = OpsPrimary
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(17.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = OpsPrimary,
                    unfocusedBorderColor = Color(0xFFE3E7EE),
                    focusedTextColor = OpsInk,
                    unfocusedTextColor = OpsInk
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                UniversalFilterChip("Все", typeFilter == null) { typeFilter = null }
                UniversalFilterChip(vocab.income, typeFilter == OperationType.INCOME) { typeFilter = OperationType.INCOME }
                UniversalFilterChip(vocab.issue, typeFilter == OperationType.ISSUE) { typeFilter = OperationType.ISSUE }
                UniversalFilterChip(vocab.transfer, typeFilter == OperationType.TRANSFER) { typeFilter = OperationType.TRANSFER }
                UniversalFilterChip(vocab.writeOff, typeFilter == OperationType.EXPENDITURE) { typeFilter = OperationType.EXPENDITURE }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        if (filtered.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = if (scopedOperations.isEmpty()) "Операций пока нет" else "Ничего не найдено",
                            color = OpsInk,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (scopedOperations.isEmpty())
                                "Для выбранного склада операций пока нет."
                            else
                                "Измените фильтр или поисковый запрос.",
                            color = OpsMuted,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        } else {
            items(filtered, key = { it.id }) { op ->
                val visual = operationVisual(op.type, vocab)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(21.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(visual.soft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = visual.icon,
                                contentDescription = null,
                                tint = visual.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.size(11.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = visual.label,
                                    color = OpsInk,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = dateFormat.format(Date(op.timestamp)),
                                    color = Color(0xFF9CA3AF),
                                    fontSize = 9.5.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = op.itemsSummary.ifBlank { "Без описания позиций" },
                                color = OpsInk,
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (op.fromPointName.isNotBlank() || op.toPointName.isNotBlank()) {
                                Spacer(modifier = Modifier.height(5.dp))
                                Text(
                                    text = listOf(op.fromPointName, op.toPointName)
                                        .filter { it.isNotBlank() }
                                        .joinToString(" → "),
                                    color = OpsMuted,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (op.comment.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = op.comment,
                                    color = Color(0xFF7D8594),
                                    fontSize = 9.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun OperationSummaryCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp)) {
            Text(
                text = value,
                color = OpsInk,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = label,
                color = OpsMuted,
                fontSize = 9.5.sp
            )
        }
    }
}

@Composable
private fun UniversalFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(if (selected) OpsPrimary else Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Color(0xFF606979),
            fontSize = 10.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

private data class OperationVisual(
    val label: String,
    val icon: ImageVector,
    val accent: Color,
    val soft: Color
)

private fun operationVisual(
    type: OperationType,
    vocab: com.example.universal.OperationVocabulary
): OperationVisual = when (type) {
    OperationType.INCOME -> OperationVisual(
        vocab.income,
        Icons.Default.ArrowDownward,
        Color(0xFF159A72),
        Color(0xFFE9F8F3)
    )
    OperationType.TRANSFER -> OperationVisual(
        vocab.transfer,
        Icons.Default.SwapHoriz,
        Color(0xFF3977D8),
        Color(0xFFEAF2FF)
    )
    OperationType.ISSUE -> OperationVisual(
        vocab.issue,
        Icons.Default.ArrowUpward,
        Color(0xFFE88B22),
        Color(0xFFFFF4E5)
    )
    OperationType.EXPENDITURE -> OperationVisual(
        vocab.writeOff,
        Icons.Default.DeleteOutline,
        Color(0xFFD94C4C),
        Color(0xFFFFEEEE)
    )
}
