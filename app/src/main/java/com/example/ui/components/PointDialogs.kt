package com.example.ui.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.WarehousePoint
import com.example.ui.theme.SageGreenBright
import com.example.ui.theme.SageGreenDark
import com.example.ui.theme.SageGreenPrimary
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalRed
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalSurfaceLight
import com.example.ui.theme.TacticalTextMuted
import com.example.ui.theme.TacticalTextPrimary
import com.example.ui.theme.TacticalTextSecondary

@Composable
fun AddPointDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, TacticalBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warehouse,
                            contentDescription = null,
                            tint = SageGreenBright,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "НОВАЯ ТОЧКА / СКЛАД",
                            color = SageGreenBright,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = TacticalTextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                TacticalInputField(
                    label = "Название точки / позиции",
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "например: ОП «Тайфун» / НП «Север»"
                )

                Spacer(modifier = Modifier.height(10.dp))

                TacticalInputField(
                    label = "Описание / Назначение",
                    value = description,
                    onValueChange = { description = it },
                    placeholder = "например: Передовой склад боепитания 2-го взвода"
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onConfirm(name, description)
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("save_point_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SageGreenPrimary,
                        contentColor = Color(0xFF0F1B14)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Создать точку", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun EditPointDialog(
    point: WarehousePoint,
    onDismiss: () -> Unit,
    onSave: (WarehousePoint) -> Unit,
    onDelete: (String) -> Unit
) {
    var name by remember { mutableStateOf(point.name) }
    var description by remember { mutableStateOf(point.description) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, TacticalBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = SageGreenBright,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "РЕДАКТИРОВАНИЕ ТОЧКИ",
                            color = SageGreenBright,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = TacticalTextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                TacticalInputField(
                    label = "Название точки",
                    value = name,
                    onValueChange = { name = it }
                )

                Spacer(modifier = Modifier.height(10.dp))

                TacticalInputField(
                    label = "Описание / Назначение",
                    value = description,
                    onValueChange = { description = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!point.isBase) {
                        Button(
                            onClick = {
                                onDelete(point.id)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TacticalRed.copy(alpha = 0.2f),
                                contentColor = TacticalRed
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Удалить",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Удалить", fontSize = 12.sp)
                        }
                    }

                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(point.copy(name = name, description = description))
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SageGreenPrimary,
                            contentColor = Color(0xFF0F1B14)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Сохранить", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AddCustomItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, serviceCategory: String, subType: String, unit: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var serviceCategory by remember { mutableStateOf("Служба РАВ") }
    var subType by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("шт.") }

    val categories = listOf(
        "Служба РАВ",
        "Служба БПЛА и робототехники",
        "Служба связи и РЭБ",
        "Вещевая служба и СИБЗ",
        "Медицинская служба",
        "Инженерная служба",
        "Служба ГСМ",
        "Продовольственная служба",
        "Автомобильная и БТ служба",
        "Служба РХБЗ",
        "Топографическая и штабная"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, TacticalBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ДОБАВИТЬ НОМЕНКЛАТУРУ",
                        color = SageGreenBright,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = TacticalTextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TacticalInputField(
                    label = "Наименование имущества",
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "например: Мина 120-мм ОФ-843Б"
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Service Category selector
                Text(
                    text = "Служба обеспечения",
                    color = TacticalTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))

                var catExpanded by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(TacticalSurfaceLight)
                        .border(1.dp, TacticalBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = serviceCategory,
                        color = TacticalTextPrimary,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TacticalInputField(
                        label = "Вид / Группа",
                        value = subType,
                        onValueChange = { subType = it },
                        placeholder = "Мины / Патроны",
                        modifier = Modifier.weight(1.3f)
                    )
                    TacticalInputField(
                        label = "Ед. изм.",
                        value = unit,
                        onValueChange = { unit = it },
                        placeholder = "шт./ящ.",
                        modifier = Modifier.weight(0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onConfirm(name, serviceCategory, subType, unit)
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("submit_custom_item_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SageGreenPrimary,
                        contentColor = Color(0xFF0F1B14)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Внести в каталог", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun EditCatalogItemDialog(
    item: com.example.data.model.InventoryItem,
    onDismiss: () -> Unit,
    onSave: (com.example.data.model.InventoryItem) -> Unit,
    onDelete: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(item.name) }
    var serviceCategory by remember { mutableStateOf(item.serviceCategory) }
    var subType by remember { mutableStateOf(item.subType) }
    var unit by remember { mutableStateOf(item.unit) }
    var standardCode by remember { mutableStateOf(item.standardCode) }
    var catExpanded by remember { mutableStateOf(false) }

    val categories = listOf(
        "Служба РАВ",
        "Служба БПЛА и робототехники",
        "Служба связи и РЭБ",
        "Вещевая служба и СИБЗ",
        "Медицинская служба",
        "Инженерная служба",
        "Служба ГСМ",
        "Продовольственная служба",
        "Автомобильная и БТ служба",
        "Служба РХБЗ",
        "Топографическая и штабная"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .padding(vertical = 14.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, TacticalBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = SageGreenBright,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ИЗМЕНИТЬ ПОЗИЦИЮ",
                            color = SageGreenBright,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = TacticalTextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TacticalInputField(
                    label = "Наименование",
                    value = name,
                    onValueChange = { name = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Служба обеспечения",
                    color = TacticalTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(TacticalSurfaceLight)
                            .border(1.dp, TacticalBorder, RoundedCornerShape(8.dp))
                            .clickable { catExpanded = true }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = serviceCategory,
                                color = TacticalTextPrimary,
                                fontSize = 13.sp
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = SageGreenPrimary
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = catExpanded,
                        onDismissRequest = { catExpanded = false },
                        modifier = Modifier
                            .background(TacticalSurface)
                            .border(1.dp, TacticalBorder)
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat, color = TacticalTextPrimary, fontSize = 12.sp) },
                                onClick = {
                                    serviceCategory = cat
                                    catExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TacticalInputField(
                        label = "Вид / Группа",
                        value = subType,
                        onValueChange = { subType = it },
                        modifier = Modifier.weight(1.3f)
                    )
                    TacticalInputField(
                        label = "Ед. изм.",
                        value = unit,
                        onValueChange = { unit = it },
                        modifier = Modifier.weight(0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                TacticalInputField(
                    label = "Номенклатурный код / Артикул",
                    value = standardCode,
                    onValueChange = { standardCode = it },
                    placeholder = "например: ГРАУ-3ВМ12"
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onDelete(item.id, item.name)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TacticalRed.copy(alpha = 0.2f),
                            contentColor = TacticalRed
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Удалить",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Удалить", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(
                                    item.copy(
                                        name = name.trim(),
                                        serviceCategory = serviceCategory,
                                        subType = subType.trim().ifEmpty { "Прочее" },
                                        unit = unit.trim().ifEmpty { "шт." },
                                        standardCode = standardCode.trim()
                                    )
                                )
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SageGreenPrimary,
                            contentColor = Color(0xFF0F1B14)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Сохранить", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AdjustStockDialog(
    pointName: String,
    pointId: String,
    itemName: String,
    itemId: String,
    currentQuantity: Int,
    unit: String,
    onDismiss: () -> Unit,
    onConfirm: (pointId: String, pointName: String, itemId: String, itemName: String, newQuantity: Int) -> Unit
) {
    var qtyText by remember { mutableStateOf(currentQuantity.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, TacticalBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "КОРРЕКТИРОВКА ОСТАТКА",
                        color = SageGreenBright,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = TacticalTextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Точка: $pointName",
                    color = SageGreenPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Имущество: $itemName",
                    color = TacticalTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Steppers + Quick buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(-10, -5, -1, 1, 5, 10).forEach { delta ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (delta > 0) SageGreenDark else TacticalSurfaceLight)
                                .border(1.dp, TacticalBorder, RoundedCornerShape(6.dp))
                                .clickable {
                                    val cur = qtyText.toIntOrNull() ?: currentQuantity
                                    val next = (cur + delta).coerceAtLeast(0)
                                    qtyText = next.toString()
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (delta > 0) "+$delta" else "$delta",
                                color = if (delta > 0) SageGreenBright else TacticalTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TacticalInputField(
                    label = "Текущий фактический остаток ($unit)",
                    value = qtyText,
                    onValueChange = { qtyText = it.filter { ch -> ch.isDigit() } },
                    placeholder = "0"
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val finalQty = qtyText.toIntOrNull() ?: currentQuantity
                        onConfirm(pointId, pointName, itemId, itemName, finalQty)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SageGreenPrimary,
                        contentColor = Color(0xFF0F1B14)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Обновить остаток", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}
