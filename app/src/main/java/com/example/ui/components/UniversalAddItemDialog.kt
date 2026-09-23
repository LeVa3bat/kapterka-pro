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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.universal.WarehouseGroupCatalog
import com.example.universal.WarehouseItemPresetCatalog
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

    var category by remember(warehouseProfileId) {
        mutableStateOf(categories.firstOrNull().orEmpty())
    }
    var group by remember(warehouseProfileId, category) { mutableStateOf("") }
    var name by remember(warehouseProfileId) { mutableStateOf("") }
    var unit by remember(warehouseProfileId) { mutableStateOf("шт.") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var groupExpanded by remember { mutableStateOf(false) }
    var nameExpanded by remember { mutableStateOf(false) }

    val groupSuggestions = remember(warehouseProfileId, category) {
        WarehouseGroupCatalog.groupsFor(warehouseProfileId, category)
    }
    val nameSuggestions = remember(warehouseProfileId, category, group) {
        WarehouseItemPresetCatalog.namesFor(warehouseProfileId, category, group)
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
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .heightIn(max = 680.dp)
                    .verticalScroll(rememberScrollState())
            ) {
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

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "1. Категория",
                    color = ItemInk,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Box {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { value ->
                            category = value
                            group = ""
                            name = ""
                            categoryExpanded = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = null,
                                tint = ItemPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Категории",
                                tint = ItemPrimary,
                                modifier = Modifier.clickable { categoryExpanded = true }
                            )
                        },
                        placeholder = { Text("Выберите категорию", fontSize = 11.5.sp) },
                        shape = RoundedCornerShape(15.dp),
                        colors = universalItemColors()
                    )

                    val filteredCategories = categories.filter {
                        category.isBlank() || it.contains(category, ignoreCase = true)
                    }
                    DropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        filteredCategories.forEach { value ->
                            DropdownMenuItem(
                                text = { Text(value, color = ItemInk, fontSize = 12.sp) },
                                onClick = {
                                    category = value
                                    group = ""
                                    name = ""
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "2. Вид / группа",
                    color = ItemInk,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Box {
                    OutlinedTextField(
                        value = group,
                        onValueChange = {
                            group = it
                            name = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = {
                            Text(
                                if (groupSuggestions.isEmpty()) "Введите свой вид" else "Выберите вид",
                                color = Color(0xFF9CA3AF),
                                fontSize = 11.5.sp
                            )
                        },
                        trailingIcon = {
                            if (groupSuggestions.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Виды",
                                    tint = ItemPrimary,
                                    modifier = Modifier.clickable { groupExpanded = true }
                                )
                            }
                        },
                        shape = RoundedCornerShape(15.dp),
                        colors = universalItemColors()
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
                                    name = ""
                                    groupExpanded = false
                                }
                            )
                        }
                    }
                }

                if (groupSuggestions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(7.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        groupSuggestions.take(8).forEach { value ->
                            PresetChip(
                                text = value,
                                selected = group == value
                            ) {
                                group = value
                                name = ""
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "3. Наименование",
                    color = ItemInk,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Box {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            nameExpanded = nameSuggestions.isNotEmpty()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = {
                            Text(
                                if (nameSuggestions.isEmpty())
                                    "Введите наименование"
                                else
                                    "Выберите готовое или введите своё",
                                color = Color(0xFF9CA3AF),
                                fontSize = 11.5.sp
                            )
                        },
                        trailingIcon = {
                            if (nameSuggestions.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Наименования",
                                    tint = ItemPrimary,
                                    modifier = Modifier.clickable { nameExpanded = true }
                                )
                            }
                        },
                        shape = RoundedCornerShape(15.dp),
                        colors = universalItemColors()
                    )

                    val filteredNames = nameSuggestions.filter {
                        name.isBlank() || it.contains(name, ignoreCase = true)
                    }
                    DropdownMenu(
                        expanded = nameExpanded,
                        onDismissRequest = { nameExpanded = false },
                        modifier = Modifier
                            .background(Color.White)
                            .heightIn(max = 320.dp)
                    ) {
                        filteredNames.forEach { value ->
                            DropdownMenuItem(
                                text = { Text(value, color = ItemInk, fontSize = 11.5.sp) },
                                onClick = {
                                    name = value
                                    nameExpanded = false
                                }
                            )
                        }
                    }
                }

                if (nameSuggestions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(7.dp))
                    Text(
                        text = nameSuggestions.size.toString() + " готовых наименований для выбранного вида",
                        color = ItemMuted,
                        fontSize = 9.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "4. Единица измерения",
                    color = ItemInk,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(7.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    units.forEach { value ->
                        PresetChip(
                            text = value,
                            selected = unit == value
                        ) { unit = value }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

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
                        .height(52.dp),
                    shape = RoundedCornerShape(17.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ItemPrimary,
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFE2E5EB),
                        disabledContentColor = Color(0xFF9CA3AF)
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(7.dp))
                    Text("Добавить в каталог", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PresetChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = text,
        color = if (selected) Color.White else ItemPrimary,
        fontSize = 10.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(if (selected) ItemPrimary else ItemSoft)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 7.dp)
    )
}

@Composable
private fun universalItemColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color(0xFFF9FAFB),
    unfocusedContainerColor = Color(0xFFF9FAFB),
    focusedBorderColor = ItemPrimary,
    unfocusedBorderColor = ItemBorder,
    focusedTextColor = ItemInk,
    unfocusedTextColor = ItemInk,
    focusedLabelColor = ItemPrimary,
    unfocusedLabelColor = ItemMuted
)
