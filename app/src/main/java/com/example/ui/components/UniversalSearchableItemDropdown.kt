package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
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
import com.example.data.model.InventoryItem

private val UniversalSelectPrimary = Color(0xFF5B5CE2)
private val UniversalSelectInk = Color(0xFF111827)
private val UniversalSelectMuted = Color(0xFF6B7280)
private val UniversalSelectBorder = Color(0xFFE2E6EE)
private val UniversalSelectSoft = Color(0xFFF5F6FF)

@Composable
fun UniversalSearchableItemDropdown(
    label: String,
    catalogItems: List<InventoryItem>,
    selectedItem: InventoryItem?,
    onItemSelected: (InventoryItem) -> Unit,
    availableStocksMap: Map<String, Int>? = null,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    val filteredItems = remember(query, catalogItems) {
        val normalized = query.trim().lowercase()
        if (normalized.isBlank()) {
            catalogItems
        } else {
            catalogItems.filter { item ->
                item.name.lowercase().contains(normalized) ||
                    item.serviceCategory.lowercase().contains(normalized) ||
                    item.subType.lowercase().contains(normalized)
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (label.isNotBlank()) {
            Text(
                text = label,
                color = UniversalSelectInk,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (selectedItem != null && !expanded) {
            val stock = availableStocksMap?.get(selectedItem.id)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .clickable {
                        query = ""
                        expanded = true
                    }
                    .padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedItem.name,
                        color = UniversalSelectInk,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildString {
                            append(selectedItem.serviceCategory)
                            if (selectedItem.subType.isNotBlank()) append(" • ").append(selectedItem.subType)
                            if (stock != null) append(" • Остаток: ").append(stock).append(" ").append(selectedItem.unit)
                        },
                        color = UniversalSelectMuted,
                        fontSize = 9.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Выбрать другую позицию",
                    tint = UniversalSelectPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    expanded = true
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = UniversalSelectPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (query.isNotEmpty()) {
                            IconButton(
                                onClick = { query = "" },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Очистить",
                                    tint = UniversalSelectMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = { expanded = !expanded },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = UniversalSelectPrimary,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }
                },
                placeholder = {
                    Text(
                        text = "Найти позицию",
                        color = Color(0xFF9CA3AF),
                        fontSize = 11.5.sp
                    )
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = UniversalSelectPrimary,
                    unfocusedBorderColor = UniversalSelectBorder,
                    focusedTextColor = UniversalSelectInk,
                    unfocusedTextColor = UniversalSelectInk
                )
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(UniversalSelectSoft)
                            .padding(horizontal = 11.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Найдено: ${filteredItems.size}",
                            color = UniversalSelectMuted,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Выберите позицию",
                            color = UniversalSelectPrimary,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (filteredItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Ничего не найдено",
                                color = UniversalSelectMuted,
                                fontSize = 11.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp)
                        ) {
                            items(filteredItems, key = { it.id }) { item ->
                                val stock = availableStocksMap?.get(item.id)
                                val active = selectedItem?.id == item.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (active) UniversalSelectSoft else Color.Transparent)
                                        .clickable {
                                            onItemSelected(item)
                                            query = ""
                                            expanded = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            color = UniversalSelectInk,
                                            fontSize = 11.5.sp,
                                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = buildString {
                                                append(item.serviceCategory)
                                                if (item.subType.isNotBlank()) append(" • ").append(item.subType)
                                            },
                                            color = UniversalSelectMuted,
                                            fontSize = 9.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    if (stock != null) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "$stock ${item.unit}",
                                            color = if (stock > 0) Color(0xFF159A72) else Color(0xFFB42318),
                                            fontSize = 9.5.sp,
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
