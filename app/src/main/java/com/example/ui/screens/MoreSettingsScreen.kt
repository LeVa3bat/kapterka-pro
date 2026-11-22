package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TableChart
import com.example.ui.components.LegalDocumentTab
import com.example.ui.components.LegalDocumentsDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
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
import com.example.ui.theme.TacticalSurfaceLight
import com.example.ui.theme.TacticalTextDim
import com.example.ui.theme.TacticalTextMuted
import com.example.ui.theme.TacticalTextPrimary
import com.example.ui.theme.TacticalTextSecondary

@Composable
fun MoreSettingsScreen(

    profile: UserProfile?,
    availableCategories: List<String>,
    syncState: com.example.data.sync.SyncState = com.example.data.sync.SyncState(),
    onDeleteCategory: (String) -> Unit,
    onAddCategory: (String) -> Unit,
    onResetCategories: () -> Unit,
    onSyncClick: () -> Unit,
    onOpenConnectCodeDialog: () -> Unit,
    onOpenPaymentPro: () -> Unit,
    onExportFullConsolidatedClick: () -> Unit = {},
    onExportPointSummaryClick: () -> Unit = {},
    onExportForm8Click: () -> Unit,
    onExportForm18Click: () -> Unit,
    onLogoutClick: () -> Unit,
    onUpdateProfile: (UserProfile) -> Unit = {},
    onRestoreLicenseFromCloud: () -> Unit = {},
    onResetDataClick: () -> Unit,
    onOpenManualClick: () -> Unit = {},
    onOpenDeveloperBackdoor: () -> Unit = {},
    backupFileName: String = "kapterka-kopiya.json",
    lastAutoBackupAt: Long = 0L,
    onSaveBackup: (android.net.Uri) -> Unit = {},
    onRestoreBackup: (android.net.Uri) -> Unit = {},
    lastCloudBackupAt: Long = 0L,
    onSaveCloudBackup: () -> Unit = {},
    onRestoreCloudBackup: () -> Unit = {},
    isDarkTheme: Boolean = true,
    onToggleTheme: () -> Unit = {},
    workspaceProfileMode: com.example.data.model.WorkspaceProfileMode = com.example.data.model.WorkspaceProfileMode.MILITARY,
    onSwitchProfileMode: (com.example.data.model.WorkspaceProfileMode) -> Unit = {},
    onImportTemplateCatalog: (com.example.data.model.WorkspaceProfileMode) -> Unit = {},
    onOpenProfileSelection: () -> Unit = {},
    onExportWriteOffActClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val unitKey = profile?.unitKey?.trim().orEmpty()
    val unitKeyDisplay = unitKey.ifBlank { "не настроен" }
    var devVersionTaps by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var devTapWindowStartedAt by remember { androidx.compose.runtime.mutableLongStateOf(0L) }

    // Accordions state: all collapsed by default to save space as requested
    var expandedProfileMode by remember { mutableStateOf(false) }
    var expandedProfile by remember { mutableStateOf(false) }
    var expandedConnectCode by remember { mutableStateOf(false) }
    var expandedBackup by remember { mutableStateOf(false) }
    var expandedProPlan by remember { mutableStateOf(false) }
    var expandedReports by remember { mutableStateOf(false) }
    var expandedCategories by remember { mutableStateOf(false) }
    var expandedGuide by remember { mutableStateOf(false) }
    var expandedDangerZone by remember { mutableStateOf(false) }
    var expandedLegal by remember { mutableStateOf(false) }
    var showLegalDialog by remember { mutableStateOf(false) }
    var legalDialogTab by remember { mutableStateOf(LegalDocumentTab.PRIVACY) }

    // Dialog state for deleting category
    var categoryToDelete by remember { mutableStateOf<String?>(null) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showResetDataConfirmDialog by remember { mutableStateOf(false) }
    var resetDataConfirmKey by remember { mutableStateOf("") }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var editCallsign by remember(profile) { mutableStateOf(profile?.callsign ?: "") }
    var editUnitName by remember(profile) { mutableStateOf(profile?.unitName ?: "") }
    var editEmail by remember(profile) { mutableStateOf(profile?.email ?: "") }
    var newCategoryName by remember { mutableStateOf("") }

    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            containerColor = TacticalSurface,
            title = {
                Text(
                    text = "Редактировать профиль",
                    color = SageGreenBright,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Имя пользователя:",
                        color = TacticalTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = editCallsign,
                        onValueChange = { editCallsign = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Имя или короткое обозначение", color = TacticalTextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SageGreenPrimary,
                            unfocusedBorderColor = TacticalBorder,
                            focusedTextColor = TacticalTextPrimary,
                            unfocusedTextColor = TacticalTextPrimary,
                            cursorColor = SageGreenBright
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Организация / подразделение:",
                        color = TacticalTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = editUnitName,
                        onValueChange = { editUnitName = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Например: Инструменты / ГСМ / Оборудование", color = TacticalTextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SageGreenPrimary,
                            unfocusedBorderColor = TacticalBorder,
                            focusedTextColor = TacticalTextPrimary,
                            unfocusedTextColor = TacticalTextPrimary,
                            cursorColor = SageGreenBright
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Email (для восстановления лицензий):",
                        color = TacticalTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = editEmail,
                        onValueChange = { editEmail = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("example@mail.ru", color = TacticalTextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SageGreenPrimary,
                            unfocusedBorderColor = TacticalBorder,
                            focusedTextColor = TacticalTextPrimary,
                            unfocusedTextColor = TacticalTextPrimary,
                            cursorColor = SageGreenBright
                        ),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = (profile ?: UserProfile()).copy(
                            callsign = editCallsign.trim().ifEmpty { profile?.callsign ?: "Боец" },
                            unitName = editUnitName.trim().ifEmpty { profile?.unitName ?: "Основной склад" },
                            email = editEmail.trim(),
                            isLoggedIn = true
                        )
                        onUpdateProfile(updated)
                        showEditProfileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SageGreenPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Text("Сохранить", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Отмена", color = TacticalTextMuted)
                }
            }
        )
    }

    if (showResetDataConfirmDialog) {
        val requiredUnitKey = profile?.unitKey?.trim().orEmpty()
        val isResetConfirmed = requiredUnitKey.isNotBlank() &&
            resetDataConfirmKey.trim().equals(requiredUnitKey, ignoreCase = true)

        AlertDialog(
            onDismissRequest = {
                resetDataConfirmKey = ""
                showResetDataConfirmDialog = false
            },
            containerColor = TacticalSurface,
            title = {
                Text(
                    text = "Опасная очистка данных",
                    color = TacticalRedText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Будут удалены остатки, операции и заявки текущей группы учёта локально и из облачной синхронизации. После синхронизации изменения затронут другие подключённые устройства. Склады, точки учёта и каталог останутся.",
                        color = TacticalTextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Для подтверждения введите текущий код группы учёта:",
                        color = TacticalTextMuted,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    OutlinedTextField(
                        value = resetDataConfirmKey,
                        onValueChange = { resetDataConfirmKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(requiredUnitKey.ifBlank { "kapt_..." }, color = TacticalTextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TacticalRedText,
                            unfocusedBorderColor = TacticalBorder,
                            focusedTextColor = TacticalTextPrimary,
                            unfocusedTextColor = TacticalTextPrimary,
                            cursorColor = TacticalRedText
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetDataClick()
                        resetDataConfirmKey = ""
                        showResetDataConfirmDialog = false
                    },
                    enabled = isResetConfirmed,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TacticalRed,
                        contentColor = Color.White,
                        disabledContainerColor = TacticalSurfaceLight,
                        disabledContentColor = TacticalTextMuted
                    )
                ) {
                    Text("ОЧИСТИТЬ УЧЁТ", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    resetDataConfirmKey = ""
                    showResetDataConfirmDialog = false
                }) {
                    Text("Отмена", color = TacticalTextMuted)
                }
            }
        )
    }

    if (categoryToDelete != null) {
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            containerColor = TacticalSurface,
            title = {
                Text(
                    text = "Удалить категорию?",
                    color = TacticalRedText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "Вы уверены, что хотите удалить категорию «${categoryToDelete}»? Все закреплённые за ней позиции будут удалены из каталога.",
                    color = TacticalTextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        categoryToDelete?.let { onDeleteCategory(it) }
                        categoryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TacticalRed,
                        contentColor = Color.White
                    )
                ) {
                    Text("Удалить", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("Отмена", color = TacticalTextMuted)
                }
            }
        )
    }

    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            containerColor = TacticalSurface,
            title = {
                Text(
                    text = "Добавить категорию",
                    color = SageGreenBright,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Введите название новой категории имущества:",
                        color = TacticalTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Название организации или подразделения", color = TacticalTextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SageGreenPrimary,
                            unfocusedBorderColor = TacticalBorder,
                            focusedTextColor = TacticalTextPrimary,
                            unfocusedTextColor = TacticalTextPrimary,
                            cursorColor = SageGreenBright
                        ),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            onAddCategory(newCategoryName.trim())
                            newCategoryName = ""
                            showAddCategoryDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SageGreenPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Text("Добавить", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("Отмена", color = TacticalTextMuted)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(TacticalBg)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // TOP SCREEN TITLE
        item {
            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                Text(
                    text = "Ещё",
                    color = TacticalTextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Профиль, синхронизация, отчёты и настройки",
                    color = TacticalTextMuted,
                    fontSize = 12.sp
                )
            }
        }

        // Official website, Telegram channel and support bot.
        item {
            com.example.ui.components.CommunityLinksCard(modifier = Modifier.padding(bottom = 10.dp))
        }

        // 0. THEME SWITCH CARD (Светлая / Тёмная)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                border = BorderStroke(1.dp, TacticalBorderSubtle),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleTheme() }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(SageGreenDark),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isDarkTheme) "🌙" else "☀️",
                                fontSize = 20.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Тема: ${if (isDarkTheme) "Тёмная" else "Светлая"}",
                                color = TacticalTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isDarkTheme) "Комфортная тёмная палитра" else "Чистая светлая палитра",
                                color = TacticalTextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = { onToggleTheme() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SageGreenPrimary,
                            uncheckedThumbColor = TacticalTextMuted,
                            uncheckedTrackColor = TacticalSurfaceLight
                        )
                    )
                }
            }
        }

        // 0b. WORKSPACE PROFILE MODE (Гражданский / Военный / Пустой склад)
        item {
            CollapsibleCard(
                title = "Профиль учёта и бланков",
                subtitle = "${workspaceProfileMode.titleRu} • ${if (workspaceProfileMode.isMilitary) "Ф-8 и Ф-18" else "М-11, МБ-7, МБ-8"}",
                icon = Icons.Default.Settings,
                iconColor = SageGreenPrimary,
                isExpanded = expandedProfileMode,
                onToggle = { expandedProfileMode = !expandedProfileMode }
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Переключение профиля меняет набор бланков Excel, кнопки и подсказки. Чтобы выбрать профиль со стартовыми складами, откройте экран выбора.",
                        color = TacticalTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                    Button(
                        onClick = onOpenProfileSelection,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("open_profile_selection_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SageGreenPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "Открыть выбор профиля и складов",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    com.example.data.model.WorkspaceProfileMode.values().forEach { mode ->
                        val active = mode == workspaceProfileMode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (active) SageGreenDark else TacticalSurface)
                                .border(
                                    width = if (active) 1.5.dp else 1.dp,
                                    color = if (active) SageGreenPrimary else TacticalBorderSubtle,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onSwitchProfileMode(mode) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mode.titleRu,
                                    color = if (active) SageGreenBright else TacticalTextPrimary,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = mode.subtitleRu,
                                    color = TacticalTextMuted,
                                    fontSize = 10.5.sp,
                                    lineHeight = 14.sp
                                )
                            }
                            if (active) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Активно",
                                    tint = SageGreenPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    if (workspaceProfileMode == com.example.data.model.WorkspaceProfileMode.MILITARY) {
                        OutlinedButton(
                            onClick = { onImportTemplateCatalog(com.example.data.model.WorkspaceProfileMode.MILITARY) },
                            modifier = Modifier.fillMaxWidth().height(38.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, TacticalBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TacticalTextPrimary)
                        ) {
                            Text("+ Дополнить каталог служб", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    } else if (workspaceProfileMode == com.example.data.model.WorkspaceProfileMode.CIVILIAN) {
                        OutlinedButton(
                            onClick = { onImportTemplateCatalog(com.example.data.model.WorkspaceProfileMode.CIVILIAN) },
                            modifier = Modifier.fillMaxWidth().height(38.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, TacticalBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TacticalTextPrimary)
                        ) {
                            Text("+ Дополнить каталог Мастерской", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 1. COLLAPSIBLE ACCORDION: ПРОФИЛЬ ПОДРАЗДЕЛЕНИЯ
        item {
            CollapsibleCard(
                title = "Профиль и синхронизация",
                subtitle = "Общая группа учёта • код: $unitKeyDisplay",
                icon = Icons.Default.Cloud,
                iconColor = SageGreenBright,
                isExpanded = expandedProfile,
                onToggle = { expandedProfile = !expandedProfile }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Пользователь: ${profile?.callsign ?: "Пользователь"}",
                                color = TacticalTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Организация / подразделение: ${profile?.unitName ?: "Основной склад"}",
                                color = SageGreenBright,
                                fontSize = 12.sp
                            )
                        }

                        val (syncPillText, syncPillBg, syncPillFg) = when {
                            unitKey.isBlank() -> Triple("ЛОКАЛЬНО", TacticalSurfaceLight, TacticalTextMuted)
                            syncState.isPaused -> Triple("ПАУЗА", TacticalGoldDark, TacticalGoldText)
                            syncState.isSyncing -> Triple("СИНХРОНИЗАЦИЯ...", TacticalGold.copy(alpha = 0.2f), TacticalGold)
                            !syncState.isOnline -> Triple("ОФЛАЙН", TacticalRedDark, TacticalRedText)
                            else -> Triple("В СЕТИ", SageGreenDark, SageGreenBright)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(syncPillBg)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = syncPillText,
                                color = syncPillFg,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Cloud info box
                    Surface(
                        color = TacticalBg,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, TacticalBorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Режим хранения:",
                                    fontSize = 11.sp,
                                    color = TacticalTextMuted
                                )
                                Text(
                                    text = if (unitKey.isNotBlank() && !syncState.isPaused) "Облако + телефон" else "Только телефон",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TacticalTextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Ключ синхронизации:",
                                    fontSize = 11.sp,
                                    color = TacticalTextMuted
                                )
                                Text(
                                    text = unitKeyDisplay,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TacticalGold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Статус подключения:",
                                    fontSize = 11.sp,
                                    color = TacticalTextMuted
                                )
                                Text(
                                    text = syncState.syncMessage,
                                    fontSize = 11.sp,
                                    color = SageGreenBright
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Устройств в синхронизации:",
                                    fontSize = 11.sp,
                                    color = TacticalTextMuted
                                )
                                val deviceCount = syncState.connectedDevicesCount
                                val deviceWord = when {
                                    deviceCount % 100 in 11..14 -> "устройств"
                                    deviceCount % 10 == 1 -> "устройство"
                                    deviceCount % 10 in 2..4 -> "устройства"
                                    else -> "устройств"
                                }
                                Text(
                                    text = "$deviceCount $deviceWord",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SageGreenBright
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                editCallsign = profile?.callsign.orEmpty()
                                editUnitName = profile?.unitName.orEmpty()
                                editEmail = profile?.email.orEmpty()
                                showEditProfileDialog = true
                            },
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SageGreenBright),
                            border = BorderStroke(1.dp, SageGreenPrimary.copy(alpha = 0.6f))
                        ) {
                            Text("Изменить данные", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onSyncClick,
                            modifier = Modifier.weight(1f).height(38.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SageGreenPrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Синхронизация", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Прямая кнопка восстановления оплаченной лицензии
                    OutlinedButton(
                        onClick = onRestoreLicenseFromCloud,
                        modifier = Modifier.fillMaxWidth().height(38.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TacticalGoldText),
                        border = BorderStroke(1.dp, TacticalGold.copy(alpha = 0.6f))
                    ) {
                        Text("Восстановить оплаченную лицензию", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            CollapsibleCard(
                title = "Резервная копия",
                subtitle = com.example.ui.components.backupSubtitle(lastCloudBackupAt, lastAutoBackupAt),
                icon = Icons.Default.Cloud,
                iconColor = SageGreenBright,
                isExpanded = expandedBackup,
                onToggle = { expandedBackup = !expandedBackup }
            ) {
                com.example.ui.components.BackupAndDiagnosticsCard(
                    suggestedFileName = backupFileName,
                    lastAutoBackupAt = lastAutoBackupAt,
                    onSaveBackup = onSaveBackup,
                    onRestoreBackup = onRestoreBackup,
                    lastCloudBackupAt = lastCloudBackupAt,
                    onSaveCloud = onSaveCloudBackup,
                    onRestoreCloud = onRestoreCloudBackup
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 2. COLLAPSIBLE ACCORDION: КОД ПОДКЛЮЧЕНИЯ ДРУГИХ ПОЛЬЗОВАТЕЛЕЙ (БЕЗ QR-КОДА)
        item {
            CollapsibleCard(
                title = "Код подключения пользователей",
                subtitle = "Код группы учёта: $unitKeyDisplay",
                icon = Icons.Default.Key,
                iconColor = SageGreenBright,
                isExpanded = expandedConnectCode,
                onToggle = { expandedConnectCode = !expandedConnectCode }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Для подключения других пользователей передайте им код группы учёта. На другом телефоне пользователь нажимает «Вход» и вводит этот код. После подключения общие данные синхронизируются.",
                        color = TacticalTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(TacticalBg)
                            .border(1.dp, SageGreenPrimary.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                            .clickable(enabled = unitKey.isNotBlank()) {
                                copyToClip(context, unitKey)
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ЕДИНЫЙ КОД ГРУППЫ",
                                color = TacticalGold,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = unitKey,
                                color = SageGreenBright,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Копировать",
                            tint = SageGreenBright,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = onOpenConnectCodeDialog,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .testTag("open_code_dialog_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SageGreenPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Скопировать и показать код группы", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 3. COLLAPSIBLE ACCORDION: ПЕРСОНАЛЬНАЯ ЛИЦЕНЗИЯ БОЙЦА
        item {
            CollapsibleCard(
                title = "Лицензия PRO (ЮKassa / от 490 ₽)",
                subtitle = if (profile?.isProActive == true) "Активна (Осталось ${profile.proDaysLeft} дн.) • Персональный ключ" else "Базовый режим (1 склад, до 15 позиций) • PRO от 490 ₽",
                icon = Icons.Default.Star,
                iconColor = TacticalGoldText,
                isExpanded = expandedProPlan,
                onToggle = { expandedProPlan = !expandedProPlan }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "После 3 дней демо работают 1 основной склад и до 15 позиций без облака. Лицензия PRO снимает все ограничения: 30 дней — 490 ₽, 90 дней — 1 190 ₽, 365 дней — 3 490 ₽ (ЮKassa / СБП).",
                        color = TacticalTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onOpenPaymentPro,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .testTag("subscribe_pro_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TacticalGold,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (profile?.isProActive == true) "Моя лицензия / Продлить (ЮKassa)" else "Выбрать тариф PRO (ЮKassa)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 4. COLLAPSIBLE ACCORDION: ОТЧЁТНОСТЬ И ВЕДОМОСТИ (Военные / Гражданские)
        item {
            CollapsibleCard(
                title = "Отчёты и ведомости Excel",
                subtitle = if (workspaceProfileMode.isMilitary) {
                    "Форма № 8 (ОКУД 6002203), Форма № 18, Сводные в Excel"
                } else {
                    "Накладная М-11 / ТОРГ-13, МБ-7, МБ-8, Оборотная ТМЦ"
                },
                icon = Icons.Default.TableChart,
                iconColor = com.example.ui.theme.TacticalEmerald,
                isExpanded = expandedReports,
                onToggle = { expandedReports = !expandedReports }
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReportCompactRow(
                        title = if (workspaceProfileMode.isMilitary) "Сводная ведомость МТО (Все позиции)" else "Оборотная ведомость остатков ТМЦ",
                        docType = "Все склады и категории",
                        onClick = onExportFullConsolidatedClick
                    )
                    ReportCompactRow(
                        title = if (workspaceProfileMode.isMilitary) "Ведомости остатков по точкам" else "Ведомости остатков по складам",
                        docType = "Приход, расход и фактический остаток",
                        onClick = onExportPointSummaryClick
                    )
                    ReportCompactRow(
                        title = if (workspaceProfileMode.isMilitary) "Форма № 8 (ОКУД 6002203 • Раздаточная)" else "Накладная М-11 / ТОРГ-13 (Перемещения)",
                        docType = if (workspaceProfileMode.isMilitary) "Приказ МО РФ № 139" else "Внутреннее перемещение ТМЦ",
                        onClick = onExportForm8Click
                    )
                    ReportCompactRow(
                        title = if (workspaceProfileMode.isMilitary) "Форма № 18 (ОКУД 0504042 • Книга учёта)" else "Ведомость выдачи на руки (МБ-7)",
                        docType = if (workspaceProfileMode.isMilitary) "Наличие и движение по наименованиям" else "Выдача инструмента и возврат",
                        onClick = onExportForm18Click
                    )
                    ReportCompactRow(
                        title = if (workspaceProfileMode.isMilitary) "Акт списания (расхода) МЦ" else "Акт списания ТМЦ (МБ-8 / ТОРГ-16)",
                        docType = if (workspaceProfileMode.isMilitary) "Комиссионное списание по акту" else "Износ, поломка и расход ТМЦ",
                        onClick = onExportWriteOffActClick
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 5. COLLAPSIBLE ACCORDION: УПРАВЛЕНИЕ ШТАТНЫМИ ГРУППАМИ (УДАЛЕНИЕ / ДОБАВЛЕНИЕ)
        item {
            CollapsibleCard(
                title = "Категории имущества",
                subtitle = "Активных категорий: ${availableCategories.size} • Настройка списка",
                icon = Icons.Default.Category,
                iconColor = SageGreenBright,
                isExpanded = expandedCategories,
                onToggle = { expandedCategories = !expandedCategories }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Если некоторые категории не нужны для вашего учёта, их можно удалить. Они исчезнут из списков и фильтров.",
                        color = TacticalTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // List of categories with delete buttons
                    availableCategories.forEach { category ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(TacticalBg)
                                .border(1.dp, TacticalBorderSubtle, RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = category,
                                color = TacticalTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )

                            IconButton(
                                onClick = { categoryToDelete = category },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Удалить группу $category",
                                    tint = TacticalRedText,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Buttons to Add Custom Group & Reset
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showAddCategoryDialog = true },
                            modifier = Modifier.weight(1f).height(36.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SageGreenPrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Добавить", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onResetCategories,
                            modifier = Modifier.weight(1f).height(36.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TacticalSurfaceLight,
                                contentColor = TacticalTextSecondary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Сброс", fontSize = 11.sp)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 6. COLLAPSIBLE ACCORDION: РУКОВОДСТВО ПО ЭКСПЛУАТАЦИИ
        item {
            CollapsibleCard(
                title = "Инструкция и руководство пользователя",
                subtitle = "Подробное иллюстрированное руководство по всем разделам",
                icon = Icons.Default.HelpOutline,
                iconColor = SageGreenBright,
                isExpanded = expandedGuide,
                onToggle = { expandedGuide = !expandedGuide }
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onOpenManualClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("open_user_manual_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SageGreenPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Открыть иллюстрированную инструкцию",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                    }

                    GuideItem(
                        q = "1. Как подключить других пользователей?",
                        a = "Передайте коллегам код группы учёта. На другом телефоне при запуске выберите «Подключиться по коду» и введите этот код. Склады, остатки и каталог будут доступны после синхронизации."
                    )
                    GuideItem(
                        q = if (workspaceProfileMode.isMilitary) "2. Списание и акты расхода (Форма 8)" else "2. Возврат и списание ТМЦ (МБ-7 / МБ-8)",
                        a = if (workspaceProfileMode.isMilitary) {
                            "Нажмите «Списание» на главном экране, выберите склад или точку, количество и причину. Запись попадёт в журнал, Акт списания и Форму № 18."
                        } else {
                            "Кнопка «Возврат» возвращает выданный инструмент на склад и отмечает возврат в ведомости МБ-7. «Списание» формирует Акт списания ТМЦ (МБ-8 / ТОРГ-16)."
                        }
                    )
                    GuideItem(
                        q = "3. Автономный режим 100% без интернета",
                        a = "Приложение полностью сохраняет данные в локальную защищенную базу SQLite/Room. Все действия доступны в оффлайне, а при появлении сети изменения автоматически объединяются."
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // 7. COLLAPSIBLE ACCORDION: ОПАСНАЯ ЗОНА (Сброс данных)
        item {
            CollapsibleCard(
                title = "Опасная зона",
                subtitle = "Очистка учёта на этом и других телефонах",
                icon = Icons.Default.Delete,
                iconColor = TacticalRedText,
                isExpanded = expandedDangerZone,
                onToggle = { expandedDangerZone = !expandedDangerZone }
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Очистка удалит операции, заявки и остатки текущей группы учёта локально и из облачной синхронизации. Для защиты потребуется ввести код группы.",
                        color = TacticalRedText,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    Button(
                        onClick = { showResetDataConfirmDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("reset_data_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TacticalRedDark,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ОЧИСТИТЬ УЧЁТ НА ВСЕХ ТЕЛЕФОНАХ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // LOGOUT / SWITCH CALLSIGN BUTTON
        item {
            CollapsibleCard(
                title = "Правовые документы и безопасность",
                subtitle = "Политика конфиденциальности, пользовательское соглашение и 152-ФЗ",
                icon = Icons.Default.Security,
                iconColor = SageGreenBright,
                isExpanded = expandedLegal,
                onToggle = { expandedLegal = !expandedLegal }
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "В соответствии с законодательством РФ (152-ФЗ) и требованиями каталога RuStore:",
                        color = TacticalTextMuted,
                        fontSize = 11.sp
                    )

                    Button(
                        onClick = {
                            legalDialogTab = LegalDocumentTab.PRIVACY
                            showLegalDialog = true
                        },
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TacticalSurfaceLight,
                            contentColor = TacticalTextPrimary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, TacticalBorder)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = SageGreenBright, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Политика конфиденциальности (152-ФЗ)", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            legalDialogTab = LegalDocumentTab.TERMS
                            showLegalDialog = true
                        },
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TacticalSurfaceLight,
                            contentColor = TacticalTextPrimary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, TacticalBorder)
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = SageGreenBright, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Пользовательское соглашение (Оферта)", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            legalDialogTab = LegalDocumentTab.CONSENT
                            showLegalDialog = true
                        },
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TacticalSurfaceLight,
                            contentColor = TacticalTextPrimary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, TacticalBorder)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SageGreenBright, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Согласие на обработку данных", fontSize = 12.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // CHANGE PROFILE & LOGOUT BUTTONS
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onOpenProfileSelection,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("change_workspace_profile_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SageGreenDark,
                        contentColor = SageGreenBright
                    ),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SageGreenPrimary.copy(alpha = 0.5f))
                ) {
                    Text("Изменить профиль и склады", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onLogoutClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .testTag("logout_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TacticalSurfaceLight,
                        contentColor = TacticalTextMuted
                    ),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TacticalBorder)
                ) {
                    Text("Выйти из аккаунта / Сменить пользователя", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // MANDATORY FOOTER WITH DEVELOPER AND VERSION AS SPECIFIED
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val now = android.os.SystemClock.elapsedRealtime()
                        if (devTapWindowStartedAt == 0L || now - devTapWindowStartedAt > 4_000L) {
                            devTapWindowStartedAt = now
                            devVersionTaps = 1
                        } else {
                            devVersionTaps++
                        }
                        if (devVersionTaps >= 7) {
                            devVersionTaps = 0
                            devTapWindowStartedAt = 0L
                            onOpenDeveloperBackdoor()
                        }
                    }
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Разработчик: Васев Алексей Евгеньевич",
                    color = SageGreenBright,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = if (workspaceProfileMode.isMilitary) {
                        "Версия программы: v${com.example.BuildConfig.VERSION_NAME} PRO (Воинский учёт)"
                    } else {
                        "Версия программы: v${com.example.BuildConfig.VERSION_NAME} PRO"
                    },
                    color = TacticalTextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    if (showLegalDialog) {
        LegalDocumentsDialog(
            initialTab = legalDialogTab,
            onDismiss = { showLegalDialog = false }
        )
    }
}

@Composable
private fun CollapsibleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = TacticalSurface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isExpanded) SageGreenPrimary.copy(alpha = 0.35f) else TacticalBorderSubtle
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 2.dp else 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(iconColor.copy(alpha = 0.10f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            color = TacticalTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            color = TacticalTextMuted,
                            fontSize = 10.5.sp,
                            maxLines = 1
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(TacticalSurfaceLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = if (isExpanded) SageGreenPrimary else TacticalTextMuted,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TacticalSurfaceLight.copy(alpha = 0.55f))
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun ReportCompactRow(
    title: String,
    docType: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TacticalBg)
            .border(1.dp, TacticalBorderSubtle, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = TacticalTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(text = docType, color = SageGreenBright, fontSize = 10.sp)
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(com.example.ui.theme.TacticalEmeraldDark)
                .padding(horizontal = 9.dp, vertical = 4.dp)
        ) {
            Text(
                text = "Excel",
                color = com.example.ui.theme.TacticalEmeraldText,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun GuideItem(q: String, a: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TacticalBg)
            .padding(8.dp)
    ) {
        Text(text = q, color = SageGreenBright, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(3.dp))
        Text(text = a, color = TacticalTextSecondary, fontSize = 10.5.sp, lineHeight = 14.sp)
    }
}

private fun copyToClip(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("UnitCode", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Код $text скопирован", Toast.LENGTH_SHORT).show()
}
