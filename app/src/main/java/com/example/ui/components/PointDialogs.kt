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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.SwapVert
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.WarehousePoint
import com.example.ui.theme.SageGreenBright
import com.example.ui.theme.SageGreenDark
import com.example.ui.theme.SageGreenPrimary
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalGold
import com.example.ui.theme.TacticalGoldDark
import com.example.ui.theme.TacticalGoldText
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
            shape = RoundedCornerShape(22.dp),
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
                            text = "Новый склад / точка",
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
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp)
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
            shape = RoundedCornerShape(22.dp),
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
                            shape = RoundedCornerShape(14.dp)
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
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Сохранить", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ReorderPointsDialog(
    points: List<WarehousePoint>,
    onDismiss: () -> Unit,
    onSaveOrder: (List<WarehousePoint>) -> Unit
) {
    var workingList by remember(points) { mutableStateOf(points) }
    var selectedPointId by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, TacticalBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = null,
                            tint = SageGreenBright,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "ПОРЯДОК И СОРТИРОВКА ТОЧЕК",
                                color = SageGreenBright,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Нажмите на точку для перемещения стрелками",
                                color = TacticalTextMuted,
                                fontSize = 10.5.sp
                            )
                        }
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

                Spacer(modifier = Modifier.height(10.dp))

                // Quick sort buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Sort A-Z button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(TacticalSurfaceLight)
                            .border(1.dp, TacticalBorder, RoundedCornerShape(12.dp))
                            .clickable {
                                val base = workingList.filter { it.isBase }
                                val others = workingList.filter { !it.isBase }.sortedBy { it.name.lowercase() }
                                workingList = base + others
                            }
                            .padding(vertical = 6.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SortByAlpha, contentDescription = null, tint = SageGreenBright, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("По алфавиту (А–Я)", fontSize = 10.5.sp, color = TacticalTextPrimary, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Reset / Default order button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(TacticalSurfaceLight)
                            .border(1.dp, TacticalBorder, RoundedCornerShape(12.dp))
                            .clickable {
                                val base = workingList.filter { it.isBase }
                                val others = workingList.filter { !it.isBase }.sortedBy { it.createdAt }
                                workingList = base + others
                            }
                            .padding(vertical = 6.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("По умолчанию", fontSize = 10.5.sp, color = TacticalTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Control panel for active selected point
                val selectedIndex = workingList.indexOfFirst { it.id == selectedPointId }
                if (selectedIndex != -1) {
                    val selPoint = workingList[selectedIndex]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(TacticalGoldDark.copy(alpha = 0.35f))
                            .border(1.dp, TacticalGold.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Выбрана: «${selPoint.name}» (№${selectedIndex + 1})",
                                color = TacticalGoldText,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = "Перемещение точки в списке:",
                                color = TacticalTextMuted,
                                fontSize = 10.sp
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Move Up
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (selectedIndex > 0) SageGreenPrimary else TacticalSurfaceLight)
                                    .clickable(enabled = selectedIndex > 0) {
                                        val list = workingList.toMutableList()
                                        val item = list.removeAt(selectedIndex)
                                        list.add(selectedIndex - 1, item)
                                        workingList = list
                                    }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = "Выше",
                                        tint = if (selectedIndex > 0) Color.White else TacticalTextMuted,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "Выше",
                                        fontSize = 10.5.sp,
                                        color = if (selectedIndex > 0) Color.White else TacticalTextMuted,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Move Down
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (selectedIndex < workingList.size - 1) SageGreenPrimary else TacticalSurfaceLight)
                                    .clickable(enabled = selectedIndex < workingList.size - 1) {
                                        val list = workingList.toMutableList()
                                        val item = list.removeAt(selectedIndex)
                                        list.add(selectedIndex + 1, item)
                                        workingList = list
                                    }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = "Ниже",
                                        tint = if (selectedIndex < workingList.size - 1) Color.White else TacticalTextMuted,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "Ниже",
                                        fontSize = 10.5.sp,
                                        color = if (selectedIndex < workingList.size - 1) Color.White else TacticalTextMuted,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Interactive Points List (with scroll)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    workingList.forEachIndexed { idx, pt ->
                        val isSelected = pt.id == selectedPointId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) TacticalGoldDark.copy(alpha = 0.25f) else TacticalSurfaceLight)
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) TacticalGold else TacticalBorder,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    selectedPointId = if (isSelected) null else pt.id
                                }
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Order number
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (pt.isBase) TacticalGoldDark else TacticalSurface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${idx + 1}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (pt.isBase) TacticalGoldText else SageGreenBright,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = pt.name,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TacticalTextPrimary
                                        )
                                        if (pt.isBase) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "★ Базовый",
                                                color = TacticalGoldText,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    if (pt.description.isNotBlank()) {
                                        Text(
                                            text = pt.description,
                                            fontSize = 10.sp,
                                            color = TacticalTextMuted,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            // Quick individual arrow controls per row
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(TacticalSurface)
                                        .clickable(enabled = idx > 0) {
                                            val list = workingList.toMutableList()
                                            val item = list.removeAt(idx)
                                            list.add(idx - 1, item)
                                            workingList = list
                                            selectedPointId = pt.id
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = "Вверх",
                                        tint = if (idx > 0) SageGreenBright else TacticalTextMuted.copy(alpha = 0.4f),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(TacticalSurface)
                                        .clickable(enabled = idx < workingList.size - 1) {
                                            val list = workingList.toMutableList()
                                            val item = list.removeAt(idx)
                                            list.add(idx + 1, item)
                                            workingList = list
                                            selectedPointId = pt.id
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = "Вниз",
                                        tint = if (idx < workingList.size - 1) SageGreenBright else TacticalTextMuted.copy(alpha = 0.4f),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TacticalSurfaceLight,
                            contentColor = TacticalTextSecondary
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Отмена", fontSize = 12.5.sp)
                    }

                    Button(
                        onClick = {
                            onSaveOrder(workingList)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1.4f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SageGreenPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Применить", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AddCustomItemDialog(
    availableCategories: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, serviceCategory: String, subType: String, unit: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var serviceCategory by remember { mutableStateOf(availableCategories.firstOrNull { it != "Все виды" } ?: "Служба РАВ") }
    var subType by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("шт.") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, TacticalBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Добавить позицию",
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
                TacticalSearchableTextDropdown(
                    label = "Служба обеспечения",
                    value = serviceCategory,
                    onValueChange = { serviceCategory = it },
                    suggestions = availableCategories.filter { it != "Все виды" },
                    placeholder = "Выберите или введите..."
                )
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
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp)
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
            shape = RoundedCornerShape(22.dp),
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
                            text = "Редактировать позицию",
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
                            .clip(RoundedCornerShape(14.dp))
                            .background(TacticalSurfaceLight)
                            .border(1.dp, TacticalBorder, RoundedCornerShape(14.dp))
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
                        shape = RoundedCornerShape(14.dp)
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
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp)
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
            shape = RoundedCornerShape(22.dp),
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
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (delta > 0) SageGreenDark else TacticalSurfaceLight)
                                .border(1.dp, TacticalBorder, RoundedCornerShape(12.dp))
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
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Обновить остаток", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}
