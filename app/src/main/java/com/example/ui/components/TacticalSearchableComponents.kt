package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.data.model.InventoryItem
import com.example.data.model.StockRecord
import com.example.data.model.WarehousePoint
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
import com.example.ui.theme.TacticalTextDim
import com.example.ui.theme.TacticalTextMuted
import com.example.ui.theme.TacticalTextPrimary
import com.example.ui.theme.TacticalTextSecondary

/**
 * Expandable search-in-field Autocomplete item selector for tactical military inventory.
 */
@Composable
fun TacticalSearchableItemDropdown(
    label: String,
    catalogItems: List<InventoryItem>,
    selectedItem: InventoryItem?,
    onItemSelected: (InventoryItem) -> Unit,
    availableStocksMap: Map<String, Int>? = null,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredItems = remember(searchQuery, catalogItems) {
        if (searchQuery.isBlank()) {
            catalogItems
        } else {
            val q = searchQuery.trim().lowercase()
            catalogItems.filter {
                it.name.lowercase().contains(q) ||
                it.subType.lowercase().contains(q) ||
                it.standardCode.lowercase().contains(q) ||
                it.serviceCategory.lowercase().contains(q)
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (label.isNotEmpty()) {
            Text(
                text = label,
                color = TacticalTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            if (selectedItem != null && !isExpanded) {
                // Display cleanly formatted selected item container with change button
                val currentStock = availableStocksMap?.get(selectedItem.id)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(TacticalSurfaceLight)
                        .border(1.dp, SageGreenPrimary.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                        .clickable {
                            isExpanded = true
                            searchQuery = ""
                        }
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedItem.name,
                            color = TacticalTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = selectedItem.serviceCategory,
                                color = TacticalTextMuted,
                                fontSize = 10.sp
                            )
                            Text(
                                text = " • ${selectedItem.unit}",
                                color = SageGreenBright,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (currentStock != null) {
                                Text(
                                    text = " • На точке: $currentStock ${selectedItem.unit}",
                                    color = if (currentStock > 0) SageGreenBright else TacticalRedText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                isExpanded = true
                                searchQuery = ""
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Выбрать другое",
                                tint = SageGreenBright,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        isExpanded = true
                    },
                    placeholder = {
                        Text(
                            text = "Введите название",
                            color = TacticalTextDim,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Поиск",
                            tint = if (isExpanded) SageGreenPrimary else TacticalTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Очистить",
                                        tint = TacticalTextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = { isExpanded = !isExpanded },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = if (isExpanded) "Свернуть" else "Развернуть",
                                    tint = SageGreenPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = TacticalSurfaceLight,
                        unfocusedContainerColor = TacticalSurfaceLight,
                        focusedBorderColor = SageGreenPrimary,
                        unfocusedBorderColor = TacticalBorderSubtle,
                        focusedTextColor = TacticalTextPrimary,
                        unfocusedTextColor = TacticalTextPrimary
                    )
                )
            }

            // Inline suggestions list
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .heightIn(max = 250.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, SageGreenPrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    color = TacticalSurfaceElevated,
                    shadowElevation = 8.dp
                ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Quick count header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(TacticalSurfaceLight)
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "НАЙДЕНО: ${filteredItems.size}",
                                    color = TacticalTextMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                                Text(
                                    text = "Нажмите для выбора",
                                    color = SageGreenBright,
                                    fontSize = 10.sp
                                )
                            }

                            if (filteredItems.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Ничего не найдено по запросу",
                                        color = TacticalTextMuted,
                                        fontSize = 12.sp
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 240.dp)
                                ) {
                                    items(filteredItems, key = { it.id }) { item ->
                                        val isSelected = selectedItem?.id == item.id
                                        val stock = availableStocksMap?.get(item.id)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(if (isSelected) SageGreenDark else Color.Transparent)
                                                .clickable {
                                                    onItemSelected(item)
                                                    searchQuery = ""
                                                    isExpanded = false
                                                }
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                             horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.name,
                                                    color = if (isSelected) SageGreenBright else TacticalTextPrimary,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = item.serviceCategory,
                                                        color = TacticalTextMuted,
                                                        fontSize = 10.sp
                                                    )
                                                    Text(
                                                        text = " • ${item.subType}",
                                                        color = SageGreenPrimary.copy(alpha = 0.8f),
                                                        fontSize = 10.sp
                                                    )
                                                    if (stock != null) {
                                                        Text(
                                                            text = " • В наличии: $stock ${item.unit}",
                                                            color = if (stock > 0) SageGreenBright else TacticalRedText,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(TacticalSurfaceLight)
                                                    .border(0.5.dp, TacticalBorderSubtle, RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = item.unit,
                                                    color = SageGreenBright,
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold
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
    }

/**
 * Expandable search-in-field Autocomplete point/warehouse selector.
 */
@Composable
fun TacticalSearchablePointDropdown(
    label: String,
    points: List<WarehousePoint>,
    selectedPoint: WarehousePoint?,
    onPointSelected: (WarehousePoint) -> Unit,
    stockRecords: List<StockRecord> = emptyList(),
    catalogItems: List<InventoryItem> = emptyList(),
    showStockSummary: Boolean = true,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredPoints = remember(searchQuery, points) {
        if (searchQuery.isBlank()) {
            points
        } else {
            val q = searchQuery.trim().lowercase()
            points.filter {
                it.name.lowercase().contains(q) || it.description.lowercase().contains(q)
            }
        }
    }

    val pointStocks = remember(selectedPoint, stockRecords) {
        if (selectedPoint == null) emptyList()
        else stockRecords.filter { it.pointId == selectedPoint.id && it.quantity > 0 }
    }
    val totalPointUnits = remember(pointStocks) { pointStocks.sumOf { it.quantity } }
    val itemsMap = remember(catalogItems) { catalogItems.associateBy { it.id } }

    Column(modifier = modifier.fillMaxWidth()) {
        if (label.isNotEmpty()) {
            Text(
                text = label,
                color = TacticalTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = if (isExpanded) searchQuery else (selectedPoint?.name ?: searchQuery),
                onValueChange = {
                    searchQuery = it
                    isExpanded = true
                },
                placeholder = {
                    Text(
                        text = selectedPoint?.name ?: "Введите название",
                        color = if (selectedPoint != null) TacticalTextPrimary else TacticalTextDim,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Поиск точки",
                        tint = if (isExpanded) SageGreenPrimary else TacticalTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = if (isExpanded) "Свернуть" else "Развернуть",
                            tint = SageGreenPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = true },
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = TacticalSurfaceLight,
                    unfocusedContainerColor = TacticalSurfaceLight,
                    focusedBorderColor = SageGreenPrimary,
                    unfocusedBorderColor = TacticalBorderSubtle,
                    focusedTextColor = TacticalTextPrimary,
                    unfocusedTextColor = TacticalTextPrimary
                )
            )

            // Live preview summary of items on the selected point
            if (showStockSummary && selectedPoint != null && stockRecords.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(TacticalBg)
                        .border(0.5.dp, SageGreenPrimary.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    if (pointStocks.isEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "📦 На точке «${selectedPoint.name}»: нет имущества на остатке (0 ед.)",
                                color = TacticalTextMuted,
                                fontSize = 11.sp
                            )
                        }
                    } else {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📦 Наличие на «${selectedPoint.name}» (${pointStocks.size} поз., всего: $totalPointUnits ед.):",
                                    color = SageGreenBright,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            val previewList = pointStocks.take(5).map { st ->
                                val itm = itemsMap[st.itemId]
                                val name = itm?.name ?: "Имущество"
                                val unit = itm?.unit ?: "ед."
                                "$name: ${st.quantity} $unit"
                            }.joinToString(" • ")
                            val extraCount = pointStocks.size - 5
                            val fullPreview = if (extraCount > 0) "$previewList • ...еще $extraCount поз." else previewList
                            Text(
                                text = fullPreview,
                                color = TacticalTextSecondary,
                                fontSize = 10.5.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .heightIn(max = 240.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, SageGreenPrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    color = TacticalSurfaceElevated,
                    shadowElevation = 8.dp
                ) {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(filteredPoints, key = { it.id }) { pt ->
                                val isSelected = selectedPoint?.id == pt.id
                                val ptStocks = stockRecords.filter { it.pointId == pt.id && it.quantity > 0 }
                                val ptUnits = ptStocks.sumOf { it.quantity }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (isSelected) SageGreenDark else Color.Transparent)
                                        .clickable {
                                            onPointSelected(pt)
                                            searchQuery = ""
                                            isExpanded = false
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = pt.name,
                                            color = if (isSelected) SageGreenBright else TacticalTextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                        if (pt.description.isNotEmpty()) {
                                            Text(
                                                text = pt.description,
                                                color = TacticalTextMuted,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                    if (stockRecords.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (ptUnits > 0) SageGreenDark else TacticalSurfaceLight)
                                                .border(0.5.dp, if (ptUnits > 0) SageGreenPrimary else TacticalBorderSubtle, RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = if (ptUnits > 0) "$ptUnits ед. (${ptStocks.size} поз.)" else "0 ед.",
                                                color = if (ptUnits > 0) SageGreenBright else TacticalTextMuted,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
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

/**
 * Expandable search-in-field Autocomplete text selector for reasons, suppliers, applicants.
 */
@Composable
fun TacticalSearchableTextDropdown(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<String>,
    placeholder: String = "",
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    var isExpanded by remember { mutableStateOf(false) }

    val filteredSuggestions = remember(value, suggestions) {
        if (value.isBlank()) {
            suggestions
        } else {
            val q = value.trim().lowercase()
            suggestions.filter { it.lowercase().contains(q) }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (label.isNotEmpty()) {
            Text(
                text = label,
                color = TacticalTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = value,
                onValueChange = {
                    onValueChange(it)
                    isExpanded = true
                },
                placeholder = { Text(if (placeholder.isNotBlank()) placeholder else "Введите название", color = TacticalTextDim, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                trailingIcon = {
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = "Варианты",
                            tint = SageGreenPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = TacticalSurfaceLight,
                    unfocusedContainerColor = TacticalSurfaceLight,
                    focusedBorderColor = SageGreenPrimary,
                    unfocusedBorderColor = TacticalBorderSubtle,
                    focusedTextColor = TacticalTextPrimary,
                    unfocusedTextColor = TacticalTextPrimary
                )
            )

            AnimatedVisibility(
                visible = isExpanded && filteredSuggestions.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .heightIn(max = 200.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, SageGreenPrimary.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                    color = TacticalSurfaceElevated,
                    shadowElevation = 6.dp
                ) {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(filteredSuggestions) { item ->
                            val isSelected = value.equals(item, ignoreCase = true)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isSelected) SageGreenDark else Color.Transparent)
                                    .clickable {
                                        onValueChange(item)
                                        isExpanded = false
                                    }
                                    .padding(horizontal = 14.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item,
                                    color = if (isSelected) SageGreenBright else TacticalTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Standard Strict Military Tactical Button with responsive text alignment and guaranteed fit.
 */
@Composable
fun TacticalFitButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    containerColor: Color = SageGreenPrimary,
    contentColor: Color = Color(0xFF0D0E10),
    borderColor: Color = Color.Transparent,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp)),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = TacticalSurfaceLight,
            disabledContentColor = TacticalTextDim
        ),
        border = if (borderColor != Color.Transparent) androidx.compose.foundation.BorderStroke(1.dp, borderColor) else null,
        shape = RoundedCornerShape(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                color = contentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
