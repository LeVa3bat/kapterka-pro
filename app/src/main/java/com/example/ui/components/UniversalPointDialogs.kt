package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.WarehousePoint
import com.example.universal.WarehouseProfileCatalog

private val WarehousePrimary = Color(0xFF5B5CE2)
private val WarehouseInk = Color(0xFF111827)
private val WarehouseMuted = Color(0xFF6B7280)
private val WarehouseBorder = Color(0xFFE2E6EE)
private val WarehouseSoft = Color(0xFFF4F5FF)
private val WarehouseDanger = Color(0xFFB42318)
private val WarehouseDangerSoft = Color(0xFFFFEEEE)

@Composable
fun UniversalAddPointDialog(
    initialProfileId: String = "universal",
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String, profileId: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var profileId by remember { mutableStateOf(WarehouseProfileCatalog.find(initialProfileId).id) }

    UniversalWarehouseDialogShell(
        title = "Новый склад",
        subtitle = "Добавьте место хранения или отдельную точку учёта.",
        icon = {
            Icon(
                imageVector = Icons.Default.Warehouse,
                contentDescription = null,
                tint = WarehousePrimary,
                modifier = Modifier.size(22.dp)
            )
        },
        onDismiss = onDismiss
    ) {
        UniversalWarehouseField(
            value = name,
            onValueChange = { name = it },
            label = "Название склада",
            placeholder = "Например: Основной склад"
        )

        Spacer(modifier = Modifier.height(12.dp))

        UniversalWarehouseField(
            value = description,
            onValueChange = { description = it },
            label = "Описание",
            placeholder = "Например: Центральное место хранения"
        )

        Spacer(modifier = Modifier.height(12.dp))

        UniversalWarehouseProfileSelector(
            selectedProfileId = profileId,
            enabled = true,
            onSelected = { profileId = it }
        )

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = {
                if (name.isNotBlank()) {
                    onConfirm(name.trim(), description.trim(), profileId)
                    onDismiss()
                }
            },
            enabled = name.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("save_point_button"),
            shape = RoundedCornerShape(17.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = WarehousePrimary,
                contentColor = Color.White,
                disabledContainerColor = Color(0xFFE2E5EB),
                disabledContentColor = Color(0xFF9CA3AF)
            )
        ) {
            Text(
                text = "Создать склад",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun UniversalEditPointDialog(
    point: WarehousePoint,
    canChangeProfile: Boolean = true,
    onDismiss: () -> Unit,
    onSave: (WarehousePoint) -> Unit,
    onDelete: (String) -> Unit
) {
    var name by remember(point.id) { mutableStateOf(point.name) }
    var description by remember(point.id) { mutableStateOf(point.description) }
    var profileId by remember(point.id) {
        mutableStateOf(WarehouseProfileCatalog.find(point.profileId).id)
    }
    var confirmDelete by remember(point.id) { mutableStateOf(false) }

    UniversalWarehouseDialogShell(
        title = "Редактирование склада",
        subtitle = if (point.isBase) {
            "Основной склад нельзя удалить, но название и описание можно изменить."
        } else {
            "Изменения применятся только к этому складу."
        },
        icon = {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = WarehousePrimary,
                modifier = Modifier.size(21.dp)
            )
        },
        onDismiss = onDismiss
    ) {
        UniversalWarehouseField(
            value = name,
            onValueChange = { name = it },
            label = "Название склада",
            placeholder = "Название"
        )

        Spacer(modifier = Modifier.height(12.dp))

        UniversalWarehouseField(
            value = description,
            onValueChange = { description = it },
            label = "Описание",
            placeholder = "Необязательно"
        )

        Spacer(modifier = Modifier.height(12.dp))

        UniversalWarehouseProfileSelector(
            selectedProfileId = profileId,
            enabled = canChangeProfile,
            onSelected = { profileId = it }
        )
        if (!canChangeProfile) {
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = "Профиль нельзя менять, пока на складе есть остатки. Сначала перенесите или спишите имущество.",
                color = WarehouseMuted,
                fontSize = 9.5.sp,
                lineHeight = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            if (!point.isBase) {
                Button(
                    onClick = { confirmDelete = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WarehouseDangerSoft,
                        contentColor = WarehouseDanger
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("Удалить", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }

            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(point.copy(name = name.trim(), description = description.trim(), profileId = profileId))
                        onDismiss()
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier
                    .weight(if (point.isBase) 1f else 1.45f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WarehousePrimary,
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFE2E5EB),
                    disabledContentColor = Color(0xFF9CA3AF)
                )
            ) {
                Text("Сохранить", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White,
            title = {
                Text(
                    text = "Удалить склад?",
                    color = WarehouseInk,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Склад «${point.name}» будет удалён. Отменить это действие после подтверждения нельзя.",
                    color = WarehouseMuted,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDelete(point.id)
                        onDismiss()
                    }
                ) {
                    Text("Удалить", color = WarehouseDanger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Отмена", color = WarehousePrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

@Composable
private fun UniversalWarehouseDialogShell(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
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
                            .background(WarehouseSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        icon()
                    }

                    Spacer(modifier = Modifier.width(11.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            color = WarehouseInk,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = subtitle,
                            color = WarehouseMuted,
                            fontSize = 10.5.sp,
                            lineHeight = 14.sp
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
                content()
            }
        }
    }
}

@Composable
private fun UniversalWarehouseProfileSelector(
    selectedProfileId: String,
    enabled: Boolean,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = WarehouseProfileCatalog.find(selectedProfileId)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Профиль склада",
            color = WarehouseInk,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(15.dp))
                .background(Color(0xFFF9FAFB))
                .clickable(enabled = enabled) { expanded = true }
                .padding(horizontal = 13.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(selected.emoji, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = selected.title,
                    color = WarehouseInk,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = selected.subtitle,
                    color = WarehouseMuted,
                    fontSize = 9.sp,
                    maxLines = 1
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Выбрать профиль",
                tint = if (enabled) WarehousePrimary else WarehouseMuted
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            WarehouseProfileCatalog.profiles.forEach { profile ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = profile.emoji + " " + profile.title,
                                color = WarehouseInk,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = profile.subtitle,
                                color = WarehouseMuted,
                                fontSize = 9.sp
                            )
                        }
                    },
                    onClick = {
                        onSelected(profile.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun UniversalWarehouseField(
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
        placeholder = {
            Text(
                text = placeholder,
                color = Color(0xFF9CA3AF),
                fontSize = 11.5.sp
            )
        },
        shape = RoundedCornerShape(15.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xFFF9FAFB),
            unfocusedContainerColor = Color(0xFFF9FAFB),
            focusedBorderColor = WarehousePrimary,
            unfocusedBorderColor = WarehouseBorder,
            focusedTextColor = WarehouseInk,
            unfocusedTextColor = WarehouseInk,
            focusedLabelColor = WarehousePrimary,
            unfocusedLabelColor = WarehouseMuted
        )
    )
}
