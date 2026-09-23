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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.InventoryItem
import com.example.data.model.StockRecord
import com.example.data.model.WarehousePoint
import com.example.universal.WarehouseGroupCatalog

private val CatalogBg = Color(0xFFF5F7FB)
private val CatalogInk = Color(0xFF111827)
private val CatalogMuted = Color(0xFF6B7280)
private val CatalogPrimary = Color(0xFF5B5CE2)
private val CatalogSoft = Color(0xFFEEEEFF)

@Composable
fun UniversalCatalogScreen(
    warehouseProfileId: String?,
    points: List<WarehousePoint>,
    selectedPointId: String,
    items: List<InventoryItem>,
    stockRecords: List<StockRecord>,
    availableCategories: List<String>,
    onSelectPoint: (String) -> Unit,
    onAddItem: () -> Unit,
    onUpdateItem: (InventoryItem) -> Unit,
    onDeleteItem: (String, String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var category by remember(warehouseProfileId) {
        mutableStateOf<String?>(
            com.example.universal.WarehouseProfileCatalog
                .find(warehouseProfileId)
                .categories
                .firstOrNull()
        )
    }
    var group by remember(warehouseProfileId) { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<InventoryItem?>(null) }

    val selectedPoint = remember(points, selectedPointId) {
        points.firstOrNull { it.id == selectedPointId } ?: points.firstOrNull()
    }
    val stocksByItem = remember(stockRecords, selectedPoint?.id) {
        val pointId = selectedPoint?.id
        stockRecords
            .filter { pointId == null || it.pointId == pointId }
            .associateBy { it.itemId }
    }

    val inStockCount = remember(items, stocksByItem) {
        items.count { (stocksByItem[it.id]?.quantity ?: 0) > 0 }
    }
    val zeroStockCount = (items.size - inStockCount).coerceAtLeast(0)

    val groupSuggestions = remember(warehouseProfileId, category, items) {
        val templateGroups = category
            ?.let { WarehouseGroupCatalog.groupsFor(warehouseProfileId, it) }
            .orEmpty()
        val catalogGroups = items
            .asSequence()
            .filter { category != null && it.serviceCategory == category }
            .map { it.subType }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()
        (templateGroups + catalogGroups).distinct()
    }

    val filtered = remember(items, query, category, group) {
        val q = query.trim().lowercase()
        items.filter { item ->
            (category == null || item.serviceCategory == category) &&
                (group == null || item.subType == group) &&
                (q.isBlank() ||
                    item.name.lowercase().contains(q) ||
                    item.serviceCategory.lowercase().contains(q) ||
                    item.subType.lowercase().contains(q) ||
                    item.standardCode.lowercase().contains(q))
        }.sortedWith(
            compareBy<InventoryItem> { it.serviceCategory }
                .thenBy { it.subType }
                .thenBy { it.name }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CatalogBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Имущество",
                        color = CatalogInk,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = if (selectedPoint == null)
                            items.size.toString() + " позиций • общий каталог профиля"
                        else
                            items.size.toString() + " позиций • остатки: " + selectedPoint.name,
                        color = CatalogMuted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Button(
                    onClick = onAddItem,
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CatalogPrimary,
                        contentColor = Color.White
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Добавить", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CatalogMetric(
                    value = items.size.toString(),
                    label = "в каталоге",
                    modifier = Modifier.weight(1f)
                )
                CatalogMetric(
                    value = inStockCount.toString(),
                    label = "с остатком",
                    modifier = Modifier.weight(1f)
                )
                CatalogMetric(
                    value = zeroStockCount.toString(),
                    label = "без остатка",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (points.isNotEmpty()) {
                Text(
                    text = "Склад",
                    color = CatalogMuted,
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
                    points.forEach { point ->
                        CatalogChip(
                            text = point.name,
                            selected = point.id == selectedPoint?.id
                        ) {
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
                singleLine = true,
                placeholder = { Text("Поиск по каталогу", color = Color(0xFF9CA3AF)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = CatalogPrimary
                    )
                },
                shape = RoundedCornerShape(17.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = CatalogPrimary,
                    unfocusedBorderColor = Color(0xFFE3E7EE),
                    focusedTextColor = CatalogInk,
                    unfocusedTextColor = CatalogInk
                )
            )

            Spacer(modifier = Modifier.height(11.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                CatalogChip("Все", category == null) {
                    category = null
                    group = null
                }
                availableCategories.forEach { value ->
                    CatalogChip(value, category == value) {
                        category = value
                        group = null
                    }
                }
            }

            if (category != null && groupSuggestions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Вид / группа • " + groupSuggestions.size,
                    color = CatalogMuted,
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
                    CatalogChip("Все виды", group == null) { group = null }
                    groupSuggestions.forEach { value ->
                        CatalogChip(value, group == value) { group = value }
                    }
                }
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
                            text = if (items.isEmpty()) "Каталог пока пуст" else "Ничего не найдено",
                            color = CatalogInk,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (items.isEmpty())
                                "Добавьте первую позицию и начните учёт."
                            else
                                "Попробуйте другой запрос или категорию.",
                            color = CatalogMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        } else {
            items(filtered, key = { it.id }) { item ->
                val stock = stocksByItem[item.id]
                val qty = stock?.quantity ?: 0
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(CatalogSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = CatalogPrimary,
                                modifier = Modifier.size(21.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(11.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.name,
                                color = CatalogInk,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = buildString {
                                    append(item.serviceCategory)
                                    if (item.subType.isNotBlank()) append(" • ${item.subType}")
                                },
                                color = CatalogMuted,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (item.standardCode.isNotBlank()) {
                                Text(
                                    text = item.standardCode,
                                    color = Color(0xFF9CA3AF),
                                    fontSize = 9.sp
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Пришло " + (stock?.incomeTotal ?: 0),
                                color = Color(0xFF159A72),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Ушло " + (stock?.expenseTotal ?: 0),
                                color = Color(0xFFE88B22),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Остаток " + qty + " " + item.unit,
                                color = if (qty > 0) CatalogInk else Color(0xFFD94C4C),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            IconButton(
                                onClick = { editing = item },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Изменить",
                                    tint = CatalogPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    editing?.let { item ->
        EditUniversalItemDialog(
            item = item,
            onDismiss = { editing = null },
            onSave = {
                onUpdateItem(it)
                editing = null
            },
            onDelete = {
                onDeleteItem(item.id, item.name)
                editing = null
            }
        )
    }
}

@Composable
private fun CatalogMetric(
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
                color = CatalogInk,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = label,
                color = CatalogMuted,
                fontSize = 9.5.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CatalogChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(if (selected) CatalogPrimary else Color.White)
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

@Composable
private fun EditUniversalItemDialog(
    item: InventoryItem,
    onDismiss: () -> Unit,
    onSave: (InventoryItem) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember(item.id) { mutableStateOf(item.name) }
    var category by remember(item.id) { mutableStateOf(item.serviceCategory) }
    var subType by remember(item.id) { mutableStateOf(item.subType) }
    var unit by remember(item.id) { mutableStateOf(item.unit) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Редактировать позицию",
                    color = CatalogInk,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(16.dp))

                UniversalEditField(name, { name = it }, "Название")
                Spacer(modifier = Modifier.height(10.dp))
                UniversalEditField(category, { category = it }, "Категория")
                Spacer(modifier = Modifier.height(10.dp))
                UniversalEditField(subType, { subType = it }, "Группа / вид")
                Spacer(modifier = Modifier.height(10.dp))
                UniversalEditField(unit, { unit = it }, "Единица измерения")

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onDelete,
                        modifier = Modifier.weight(0.8f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFEEEE),
                            contentColor = Color(0xFFD94C4C)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Button(
                        onClick = {
                            onSave(
                                item.copy(
                                    name = name.trim(),
                                    serviceCategory = category.trim(),
                                    subType = subType.trim(),
                                    unit = unit.trim()
                                )
                            )
                        },
                        enabled = name.isNotBlank() && category.isNotBlank() && unit.isNotBlank(),
                        modifier = Modifier.weight(2f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CatalogPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Сохранить", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun UniversalEditField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 11.sp) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xFFF9FAFB),
            unfocusedContainerColor = Color(0xFFF9FAFB),
            focusedBorderColor = CatalogPrimary,
            unfocusedBorderColor = Color(0xFFE3E7EE),
            focusedTextColor = CatalogInk,
            unfocusedTextColor = CatalogInk
        )
    )
}
