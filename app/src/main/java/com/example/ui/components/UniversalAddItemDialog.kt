package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.window.DialogProperties
import com.example.universal.WarehouseGroupCatalog
import com.example.universal.WarehouseProfileCatalog

private val ItemPrimary = Color(0xFF5B5CE2)
private val ItemInk = Color(0xFF111827)
private val ItemMuted = Color(0xFF6B7280)
private val ItemBorder = Color(0xFFE2E6EE)
private val ItemSoft = Color(0xFFF5F6FF)

@Composable
fun UniversalAddItemDialog(
    warehouseProfileId: String?,
    availableCategories: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, category: String, group: String, unit: String) -> Unit
) {
    val profile = WarehouseProfileCatalog.find(warehouseProfileId)
    val categories = remember(warehouseProfileId, availableCategories) {
        (profile.categories + availableCategories)
            .filter { it.isNotBlank() && it != "Все виды" && it != "Все категории" }
            .distinct()
    }

    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(categories.firstOrNull().orEmpty()) }
    var group by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("шт.") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var groupExpanded by remember { mutableStateOf(false) }

    val groupSuggestions = remember(warehouseProfileId, category) {
        WarehouseGroupCatalog.groupsFor(warehouseProfileId, category)
    }
    val units = listOf("шт.", "компл.", "уп.", "кор.", "ящ.", "кг", "л", "м", "м²", "пара")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 18.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(ItemSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = ItemPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(11.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Новая позиция",
                            color = ItemInk,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = profile.title,
                            color = ItemMuted,
                            fontSize = 10.5.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = Color(0xFF8A93A2)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                UniversalItemField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Название позиции",
                    placeholder = "Например: Фильтр масляный"
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Категория",
                    color = ItemInk,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(15.dp))
                            .background(Color(0xFFF9FAFB))
                            .clickable { categoryExpanded = true }
                            .padding(horizontal = 13.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            tint = ItemPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(9.dp))
                        Text(
                            text = category.ifBlank { "Выберите категорию" },
                            color = if (category.isBlank()) Color(0xFF9CA3AF) else ItemInk,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = ItemMuted
                        )
                    }

                    DropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        categories.forEach { value ->
                            DropdownMenuItem(
                                text = { Text(value, color = ItemInk, fontSize = 12.sp) },
                                onClick = {
                                    category = value
                                    group = ""
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Группа / вид",
                    color = ItemInk,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Box {
                    OutlinedTextField(
                        value = group,
                        onValueChange = { group = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = {
                            Text(
                                if (groupSuggestions.isEmpty()) "Введите свою группу" else "Выберите или введите свою",
                                color = Color(0xFF9CA3AF),
                                fontSize = 11.5.sp
                            )
                        },
                        trailingIcon = {
                            if (groupSuggestions.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Показать группы",
                                    tint = ItemPrimary,
                                    modifier = Modifier.clickable { groupExpanded = true }
                                )
                            }
                        },
                        shape = RoundedCornerShape(15.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF9FAFB),
                            unfocusedContainerColor = Color(0xFFF9FAFB),
                            focusedBorderColor = ItemPrimary,
                            unfocusedBorderColor = ItemBorder,
                            focusedTextColor = ItemInk,
                            unfocusedTextColor = ItemInk
                        )
                    )

                    DropdownMenu(
                        expanded = groupExpanded,
                        onDismissRequest = { groupExpanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        groupSuggestions.forEach { value ->
                            DropdownMenuItem(
                                text = { Text(value, color = ItemInk, fontSize = 12.sp) },
                                onClick = {
                                    group = value
                                    groupExpanded = false
                                }
                            )
                        }
                    }
                }

                if (groupSuggestions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(7.dp))
                    Text(
                        text = "${groupSuggestions.size} готовых групп для этой категории",
                        color = ItemMuted,
                        fontSize = 9.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Единица измерения",
                    color = ItemInk,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(7.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    units.forEach { value ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(if (unit == value) ItemPrimary else Color(0xFFF3F4F6))
                                .clickable { unit = value }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = value,
                                color = if (unit == value) Color.White else Color(0xFF5D6574),
                                fontSize = 10.5.sp,
                                fontWeight = if (unit == value) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(19.dp))

                Button(
                    onClick = {
                        val finalGroup = group.trim().ifBlank { "Без группы" }
                        if (name.isNotBlank() && category.isNotBlank()) {
                            onConfirm(name.trim(), category.trim(), finalGroup, unit)
                            onDismiss()
                        }
                    },
                    enabled = name.isNotBlank() && category.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(17.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ItemPrimary,
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFE2E5EB),
                        disabledContentColor = Color(0xFF9CA3AF)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = "Добавить в каталог",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun UniversalItemField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(label, fontSize = 11.sp) },
        placeholder = { Text(placeholder, color = Color(0xFF9CA3AF), fontSize = 11.5.sp) },
        shape = RoundedCornerShape(15.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xFFF9FAFB),
            unfocusedContainerColor = Color(0xFFF9FAFB),
            focusedBorderColor = ItemPrimary,
            unfocusedBorderColor = ItemBorder,
            focusedTextColor = ItemInk,
            unfocusedTextColor = ItemInk,
            focusedLabelColor = ItemPrimary,
            unfocusedLabelColor = ItemMuted
        )
    )
}
