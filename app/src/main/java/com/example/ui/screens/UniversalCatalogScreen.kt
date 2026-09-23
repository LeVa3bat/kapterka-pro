package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warehouse
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
import com.example.universal.WarehouseProfileCatalog

private val CatalogInk = Color(0xFF111827)
private val CatalogMuted = Color(0xFF667085)
private val CatalogBg = Color(0xFFF6F7FB)
private val CatalogPrimary = Color(0xFF5B5CE2)
private val CatalogSoft = Color(0xFFEEEEFF)
private val CatalogRed = Color(0xFFD94C4C)

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
    var category by remember(warehouseProfileId, selectedPointId) { mutableStateOf<String?>(null) }
    var group by remember(warehouseProfileId, selectedPointId) { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<InventoryItem?>(null) }
    var showWarehousePicker by remember { mutableStateOf(false) }

    val selectedPoint = remember(points, selectedPointId) {
        points.firstOrNull { it.id == selectedPointId } ?: points.firstOrNull()
    }
    val profile = WarehouseProfileCatalog.find(
        selectedPoint?.profileId?.takeIf { it.isNotBlank() } ?: warehouseProfileId
    )

    val pointStocks = remember(stockRecords, selectedPoint?.id) {
        val pointId = selectedPoint?.id
        stockRecords
            .filter { pointId == null || it.pointId == pointId }
            .associateBy { it.itemId }
    }

    val categories = remember(availableCategories, items, profile.id) {
        (profile.categories + availableCategories + items.map { it.serviceCategory })
            .filter { it.isNotBlank() && it != "Все виды" && it != "Все категории" }
            .distinct()
    }

    val groups = remember(profile.id, category, items) {
        val selectedCategory = category
        if (selectedCategory == null) {
            emptyList()
        } else {
            (
                WarehouseGroupCatalog.groupsFor(profile.id, selectedCategory) +
                    items
                        .filter { it.serviceCategory == selectedCategory }
                        .map { it.subType }
                        .filter { it.isNotBlank() }
                ).distinct()
        }
    }

    val searchResults = remember(items, query) {
        val q = query.trim().lowercase()
        if (q.isBlank()) {
            emptyList()
        } else {
            items.filter { item ->
                item.name.lowercase().contains(q) ||
                    item.serviceCategory.lowercase().contains(q) ||
                    item.subType.lowercase().contains(q) ||
                    item.standardCode.lowercase().contains(q)
            }.sortedWith(
                compareBy<InventoryItem> { it.serviceCategory }
                    .thenBy { it.subType }
                    .thenBy { it.name }
            )
        }
    }

    val selectedItems = remember(items, category, group) {
        if (category == null || group == null) {
            emptyList()
        } else {
            items.filter { it.serviceCategory == category && it.subType == group }
                .sortedBy { it.name }
        }
    }

    val activePositions = remember(items, pointStocks) {
        items.count { (pointStocks[it.id]?.quantity ?: 0) > 0 }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CatalogBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Каталог",
                        color = CatalogInk,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = profile.title,
                        color = CatalogMuted,
                        fontSize = 11.sp
                    )
                }
                Button(
                    onClick = onAddItem,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CatalogPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Позиция", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            SelectedWarehouseBar(
                point = selectedPoint,
                activePositions = activePositions,
                onClick = { showWarehousePicker = true }
            )
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Поиск по названию, группе или коду", color = Color(0xFF98A2B3)) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = CatalogPrimary)
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = CatalogPrimary,
                    unfocusedBorderColor = Color(0xFFE4E7EC),
                    focusedTextColor = CatalogInk,
                    unfocusedTextColor = CatalogInk
                )
            )
        }

        if (query.isNotBlank()) {
            item {
                Text(
                    text = "Найдено: " + searchResults.size,
                    color = CatalogMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (searchResults.isEmpty()) {
                item { EmptyCatalogState("Ничего не найдено", "Измени запрос или очисти поиск.") }
            } else {
                items(searchResults, key = { it.id }) { item ->
                    CompactCatalogItem(
                        item = item,
                        stock = pointStocks[item.id],
                        onEdit = { editing = item }
                    )
                }
            }
        } else if (category == null) {
            item {
                TreeHeader(
                    title = "Категории",
                    subtitle = "Выбери раздел — длинного общего списка больше нет"
                )
            }
            if (categories.isEmpty()) {
                item { EmptyCatalogState("Категорий пока нет", "Добавь первую позицию.") }
            } else {
                items(categories, key = { it }) { cat ->
                    val catItems = items.filter { it.serviceCategory == cat }
                    val catGroups = (
                        WarehouseGroupCatalog.groupsFor(profile.id, cat) +
                            catItems.map { it.subType }.filter { it.isNotBlank() }
                        ).distinct()
                    TreeRow(
                        title = cat,
                        subtitle = catGroups.size.toString() + " групп • " + catItems.size + " позиций",
                        onClick = {
                            category = cat
                            group = null
                        }
                    )
                }
            }
        } else if (group == null) {
            item {
                BreadcrumbRow(
                    prefix = "Категории",
                    current = category.orEmpty(),
                    onBack = {
                        category = null
                        group = null
                    }
                )
            }
            item {
                TreeHeader(
                    title = "Виды / группы",
                    subtitle = category.orEmpty()
                )
            }
            if (groups.isEmpty()) {
                item {
                    EmptyCatalogState(
                        "Групп пока нет",
                        "Добавь позицию и укажи свой вид/группу."
                    )
                }
            } else {
                items(groups, key = { it }) { value ->
                    val groupItems = items.filter {
                        it.serviceCategory == category && it.subType == value
                    }
                    val inStock = groupItems.count { (pointStocks[it.id]?.quantity ?: 0) > 0 }
                    TreeRow(
                        title = value,
                        subtitle = groupItems.size.toString() + " позиций • " + inStock + " с остатком",
                        onClick = { group = value }
                    )
                }
            }
        } else {
            item {
                BreadcrumbRow(
                    prefix = category.orEmpty(),
                    current = group.orEmpty(),
                    onBack = { group = null }
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = group.orEmpty(),
                        color = CatalogInk,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = selectedItems.size.toString() + " поз.",
                        color = CatalogMuted,
                        fontSize = 10.sp
                    )
                }
            }
            if (selectedItems.isEmpty()) {
                item {
                    EmptyCatalogState(
                        "В этой группе пока нет позиций",
                        "Добавь готовое или своё наименование."
                    )
                }
            } else {
                items(selectedItems, key = { it.id }) { item ->
                    CompactCatalogItem(
                        item = item,
                        stock = pointStocks[item.id],
                        onEdit = { editing = item }
                    )
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }

    if (showWarehousePicker) {
        CatalogWarehousePicker(
            points = points,
            selectedPointId = selectedPoint?.id.orEmpty(),
            onSelect = {
                onSelectPoint(it.id)
                category = null
                group = null
                showWarehousePicker = false
            },
            onDismiss = { showWarehousePicker = false }
        )
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
private fun SelectedWarehouseBar(
    point: WarehousePoint?,
    activePositions: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(17.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(CatalogSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Warehouse, contentDescription = null, tint = CatalogPrimary)
        }
        Spacer(Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = point?.name ?: "Склад не выбран",
                color = CatalogInk,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = activePositions.toString() + " поз. с остатком • нажми, чтобы сменить",
                color = CatalogMuted,
                fontSize = 8.8.sp
            )
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = CatalogMuted)
    }
}

@Composable
private fun TreeHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)) {
        Text(title, color = CatalogInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = CatalogMuted, fontSize = 9.5.sp)
    }
}

@Composable
private fun TreeRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = CatalogInk,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = CatalogMuted,
                fontSize = 8.8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF98A2B3),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun BreadcrumbRow(
    prefix: String,
    current: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CatalogSoft)
            .clickable(onClick = onBack)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = "Назад",
            tint = CatalogPrimary,
            modifier = Modifier.size(17.dp)
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = prefix + "  ›  " + current,
            color = CatalogPrimary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CompactCatalogItem(
    item: InventoryItem,
    stock: StockRecord?,
    onEdit: () -> Unit
) {
    val quantity = stock?.quantity ?: 0
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 11.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CatalogSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = CatalogPrimary,
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    color = CatalogInk,
                    fontSize = 11.2.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.standardCode.isNotBlank()) {
                    Text(
                        text = item.standardCode,
                        color = CatalogMuted,
                        fontSize = 8.5.sp,
                        maxLines = 1
                    )
                }
                Text(
                    text = "+" + (stock?.incomeTotal ?: 0) + " • -" + (stock?.expenseTotal ?: 0),
                    color = CatalogMuted,
                    fontSize = 8.4.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = quantity.toString() + " " + item.unit,
                    color = if (quantity > 0) CatalogInk else CatalogRed,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "остаток",
                    color = CatalogMuted,
                    fontSize = 8.2.sp
                )
            }
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Изменить",
                    tint = CatalogPrimary,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyCatalogState(title: String, subtitle: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = CatalogInk, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = CatalogMuted, fontSize = 9.5.sp)
        }
    }
}

@Composable
private fun CatalogWarehousePicker(
    points: List<WarehousePoint>,
    selectedPointId: String,
    onSelect: (WarehousePoint) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Выберите склад",
                    color = CatalogInk,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(10.dp))
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    items(points, key = { it.id }) { point ->
                        val profile = WarehouseProfileCatalog.find(point.profileId)
                        val selected = point.id == selectedPointId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(15.dp))
                                .background(if (selected) CatalogSoft else CatalogBg)
                                .clickable { onSelect(point) }
                                .padding(horizontal = 11.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(profile.emoji, fontSize = 18.sp)
                            Spacer(Modifier.width(9.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = point.name,
                                    color = CatalogInk,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = profile.title,
                                    color = CatalogMuted,
                                    fontSize = 8.8.sp
                                )
                            }
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = CatalogPrimary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Закрыть",
                    color = CatalogPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.End)
                        .clickable(onClick = onDismiss)
                        .padding(8.dp)
                )
            }
        }
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
