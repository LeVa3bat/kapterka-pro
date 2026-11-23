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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InventoryItem
import com.example.ui.components.EditCatalogItemDialog
import com.example.ui.theme.SageGreenBright
import com.example.ui.theme.SageGreenDark
import com.example.ui.theme.SageGreenPrimary
import com.example.ui.theme.TacticalBg
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalBorderSubtle
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalSurfaceLight
import com.example.ui.theme.TacticalTextDim
import com.example.ui.theme.TacticalTextMuted
import com.example.ui.theme.TacticalTextPrimary
import com.example.ui.theme.TacticalTextSecondary

@Composable
fun InventoryCatalogScreen(
    items: List<InventoryItem>,
    availableCategories: List<String> = emptyList(),
    stockRecords: List<com.example.data.model.StockRecord> = emptyList(),
    profileMode: com.example.data.model.WorkspaceProfileMode = com.example.data.model.WorkspaceProfileMode.MILITARY,
    onAddNewItemClick: () -> Unit,
    onUpdateItem: (InventoryItem) -> Unit = {},
    onDeleteItem: (String, String) -> Unit = { _, _ -> }
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedService by remember { mutableStateOf("Все категории") }
    val expandedCategoriesMap = remember { mutableStateMapOf<String, Boolean>() }
    var editingItem by remember { mutableStateOf<InventoryItem?>(null) }

    val totalStockByItemId = remember(stockRecords) {
        stockRecords.groupBy { it.itemId }.mapValues { (_, list) -> list.sumOf { it.quantity } }
    }

    val services = remember(availableCategories, items, profileMode) {
        val fromItems = items.map { it.serviceCategory }.filter { it.isNotBlank() }.distinct()
        val combined = (availableCategories + fromItems).distinct()
        if (combined.isNotEmpty()) {
            listOf("Все категории") + combined
        } else {
            val defaults = com.example.data.local.InitialData.getCategoriesFor(profileMode)
            if (defaults.isEmpty()) listOf("Все категории") else listOf("Все категории") + defaults
        }
    }

    val filteredItems = items.filter { item ->
        val matchesService = selectedService == "Все категории" || item.serviceCategory == selectedService
        val matchesQuery = if (searchQuery.trim().isEmpty()) true
        else {
            val q = searchQuery.trim().lowercase()
            item.name.lowercase().contains(q) ||
                    item.subType.lowercase().contains(q) ||
                    item.standardCode.lowercase().contains(q) ||
                    item.serviceCategory.lowercase().contains(q)
        }
        matchesService && matchesQuery
    }

    // Group items by Category -> SubType
    val groupedByService = filteredItems.groupBy { it.serviceCategory }

    // Keep categories collapsed by default on tab entry
    // Users can click on a category header or use search to view items
    LaunchedEffect(searchQuery) {
        if (searchQuery.trim().isNotEmpty()) {
            groupedByService.keys.forEach { service ->
                expandedCategoriesMap[service] = true
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(TacticalBg)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // Header with "+ Позиция"
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Имущество",
                        color = TacticalTextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.testTag("catalog_header_title")
                    )
                    Text(
                        text = "Каталог • ${items.size} позиций",
                        color = TacticalTextMuted,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                com.example.ui.components.Volumetric3DButton(
                    text = "Добавить",
                    icon = Icons.Default.Add,
                    onClick = onAddNewItemClick,
                    modifier = Modifier.testTag("catalog_add_button"),
                    height = 40.dp,
                    cornerRadius = 13.dp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // Inline Search Box with "Введите название"
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
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
                    .testTag("catalog_search_input"),
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

        // Service Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                services.forEach { srv ->
                    val count = if (srv == "Все категории") items.size else items.count { it.serviceCategory == srv }
                    com.example.ui.components.Filter3DPill(
                        text = "$srv ($count)",
                        selected = srv == selectedService,
                        onClick = { selectedService = srv }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // Expand / Collapse all toggle
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Категории",
                    color = TacticalTextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.2.sp
                )

                val allExpanded = groupedByService.keys.all { expandedCategoriesMap[it] == true }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(TacticalSurfaceLight)
                        .border(1.dp, TacticalBorderSubtle, RoundedCornerShape(12.dp))
                        .clickable {
                            val target = !allExpanded
                            groupedByService.keys.forEach { expandedCategoriesMap[it] = target }
                        }
                        .padding(horizontal = 7.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (allExpanded) Icons.Default.UnfoldLess else Icons.Default.UnfoldMore,
                        contentDescription = null,
                        tint = TacticalTextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (allExpanded) "Свернуть все" else "Развернуть",
                        color = TacticalTextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Collapsible Grouped Services
        if (groupedByService.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Inventory,
                        contentDescription = null,
                        tint = TacticalTextDim,
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (items.isEmpty()) "Каталог пуст — добавьте свою первую позицию" else "В выбранной категории позиции не найдены",
                        color = TacticalTextMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    if (items.isEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        androidx.compose.material3.Button(
                            onClick = onAddNewItemClick,
                            shape = RoundedCornerShape(14.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = SageGreenPrimary,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Добавить позицию", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            groupedByService.forEach { (serviceCategory, itemList) ->
                val isExpanded = expandedCategoriesMap[serviceCategory] ?: false

                item(key = serviceCategory) {
                    com.example.ui.components.Card3DSurface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        cornerRadius = 18.dp,
                        accentBorder = if (isExpanded) SageGreenPrimary.copy(alpha = 0.5f) else null,
                        elevated = isExpanded
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Service Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isExpanded) TacticalSurfaceLight else TacticalSurface)
                                    .clickable { expandedCategoriesMap[serviceCategory] = !isExpanded }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(SageGreenPrimary)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = serviceCategory,
                                        color = SageGreenBright,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "(${itemList.size})",
                                        color = TacticalTextMuted,
                                        fontSize = 11.sp
                                    )
                                }

                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = SageGreenPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Items inside expanded category
                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    itemList.forEach { item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 3.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(TacticalSurfaceLight)
                                                .border(0.5.dp, TacticalBorderSubtle, RoundedCornerShape(12.dp))
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.name,
                                                    color = TacticalTextPrimary,
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    if (item.standardCode.isNotEmpty()) {
                                                        Text(
                                                            text = item.standardCode,
                                                            color = TacticalTextMuted,
                                                            fontSize = 10.sp,
                                                            fontFamily = FontFamily.Monospace
                                                        )
                                                    }
                                                    Text(
                                                        text = item.subType,
                                                        color = SageGreenPrimary,
                                                        fontSize = 10.5.sp
                                                    )
                                                    Text(
                                                        text = "• ${item.unit}",
                                                        color = TacticalTextSecondary,
                                                        fontSize = 10.5.sp
                                                    )
                                                }
                                            }

                                            val currentQty = totalStockByItemId[item.id] ?: 0
                                            if (currentQty > 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .padding(end = 6.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(com.example.ui.theme.TacticalEmeraldDark)
                                                        .padding(horizontal = 7.dp, vertical = 3.dp)
                                                ) {
                                                    Text(
                                                        text = "$currentQty ${item.unit}",
                                                        color = com.example.ui.theme.TacticalEmeraldText,
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }
                                            }

                                            // Edit Button
                                            IconButton(
                                                onClick = { editingItem = item },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Редактировать",
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
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Modal Edit Dialog
    editingItem?.let { item ->
        EditCatalogItemDialog(
            item = item,
            onDismiss = { editingItem = null },
            onSave = { updated ->
                onUpdateItem(updated)
                editingItem = null
            },
            onDelete = { id, name ->
                onDeleteItem(id, name)
                editingItem = null
            }
        )
    }
}
