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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.data.model.WorkspaceProfileMode
import com.example.ui.components.Card3DSurface
import com.example.ui.components.IconOrb3DButton
import com.example.ui.components.LegalDocumentTab
import com.example.ui.components.LegalDocumentsDialog
import com.example.ui.components.PulsingStatusDot
import com.example.ui.components.Volumetric3DButton
import com.example.ui.theme.SageGreenBright
import com.example.ui.theme.SageGreenDark
import com.example.ui.theme.SageGreenPrimary
import com.example.ui.theme.TacticalBg
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalBorderSubtle
import com.example.ui.theme.TacticalEmerald
import com.example.ui.theme.TacticalEmeraldDark
import com.example.ui.theme.TacticalEmeraldText
import com.example.ui.theme.TacticalGold
import com.example.ui.theme.TacticalGoldDark
import com.example.ui.theme.TacticalGoldText
import com.example.ui.theme.TacticalRed
import com.example.ui.theme.TacticalRedDark
import com.example.ui.theme.TacticalRedText
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalSurfaceLight
import com.example.ui.theme.TacticalTeal
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
    workspaceProfileMode: WorkspaceProfileMode = WorkspaceProfileMode.MILITARY,
    @Suppress("UNUSED_PARAMETER") onSwitchProfileMode: (WorkspaceProfileMode) -> Unit = {},
    onImportTemplateCatalog: (WorkspaceProfileMode) -> Unit = {},
    onOpenProfileSelection: () -> Unit = {},
    onExportWriteOffActClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val isMil = workspaceProfileMode.isMilitary
    val unitKey = profile?.unitKey?.trim().orEmpty()
    val unitKeyDisplay = unitKey.ifBlank { "не настроен" }
    var devVersionTaps by remember { mutableIntStateOf(0) }
    var devTapWindowStartedAt by remember { mutableLongStateOf(0L) }

    // Neatly grouped collapsible sections (collapsed by default for calm overview)
    var expandedReports by remember { mutableStateOf(false) }
    var expandedSync by remember { mutableStateOf(false) }
    var expandedBackup by remember { mutableStateOf(false) }
    var expandedCategories by remember { mutableStateOf(false) }
    var expandedLegal by remember { mutableStateOf(false) }
    var expandedCommunity by remember { mutableStateOf(false) }
    var expandedDangerZone by remember { mutableStateOf(false) }

    var showLegalDialog by remember { mutableStateOf(false) }
    var legalDialogTab by remember { mutableStateOf(LegalDocumentTab.PRIVACY) }

    // Dialog states
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
                    text = "Данные профиля",
                    color = SageGreenBright,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = if (isMil) "Позывной / ФИО:" else "Имя / ФИО:",
                        color = TacticalTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = editCallsign,
                        onValueChange = { editCallsign = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(if (isMil) "Позывной или ФИО" else "Имя или ФИО", color = TacticalTextMuted) },
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
                        text = if (isMil) "Подразделение:" else "Название склада / организации:",
                        color = TacticalTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = editUnitName,
                        onValueChange = { editUnitName = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                if (isMil) "Например: 1-я рота / Склад МТО" else "Например: Мастерская / Основной склад",
                                color = TacticalTextMuted
                            )
                        },
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
                        text = "Email (для ключа и восстановления PRO):",
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
                        val fallbackUser = if (isMil) "Старшина" else "Пользователь"
                        val updated = (profile ?: UserProfile()).copy(
                            callsign = editCallsign.trim().ifEmpty { profile?.callsign ?: fallbackUser },
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
        val expectedToken = requiredUnitKey.ifBlank { "УДАЛИТЬ" }
        val isResetConfirmed = resetDataConfirmKey.trim().equals(expectedToken, ignoreCase = true)

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
                        text = "Будут удалены остатки, операции и заявки текущей группы учёта локально и из облачной синхронизации. После синхронизации изменения затронут другие подключённые телефоны. Склады и каталог останутся.",
                        color = TacticalTextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (requiredUnitKey.isNotBlank()) {
                            "Для подтверждения введите текущий код подключения:"
                        } else {
                            "Для подтверждения введите слово УДАЛИТЬ:"
                        },
                        color = TacticalTextMuted,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    OutlinedTextField(
                        value = resetDataConfirmKey,
                        onValueChange = { resetDataConfirmKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(expectedToken, color = TacticalTextMuted) },
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
                    text = "Вы уверены, что хотите удалить категорию «$categoryToDelete»? Все закреплённые за ней позиции будут удалены из каталога.",
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
                        text = "Введите название новой категории:",
                        color = TacticalTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                if (isMil) "Например: Инженерная служба" else "Например: Электроинструмент / Крепёж",
                                color = TacticalTextMuted
                            )
                        },
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

    val (syncPillText, syncPillBg, syncPillFg) = when {
        unitKey.isBlank() -> Triple("ЛОКАЛЬНО", TacticalSurfaceLight, TacticalTextMuted)
        syncState.isPaused -> Triple("ПАУЗА", TacticalGoldDark, TacticalGoldText)
        syncState.isSyncing -> Triple("ОБМЕН…", TacticalGoldDark, TacticalGoldText)
        !syncState.isOnline -> Triple("ОФЛАЙН", TacticalRedDark, TacticalRedText)
        else -> Triple("В СЕТИ", SageGreenDark, SageGreenBright)
    }
    val deviceCount = syncState.connectedDevicesCount
    val deviceWord = when {
        deviceCount % 100 in 11..14 -> "устройств"
        deviceCount % 10 == 1 -> "устройство"
        deviceCount % 10 in 2..4 -> "устройства"
        else -> "устройств"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(TacticalBg)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // 1. TOP SCREEN TITLE + QUICK THEME ORB
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Ещё",
                        color = TacticalTextPrimary,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Профиль, подписка, синхронизация и отчёты",
                        color = TacticalTextMuted,
                        fontSize = 12.sp
                    )
                }
                IconOrb3DButton(
                    icon = if (isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                    contentDescription = if (isDarkTheme) "Светлая тема" else "Тёмная тема",
                    tint = if (isDarkTheme) TacticalGold else SageGreenPrimary,
                    onClick = onToggleTheme,
                    size = 40.dp
                )
            }
        }

        // 2. COMPACT 3D PROFILE & WORKSPACE HUB CARD
        item {
            Card3DSurface(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 22.dp,
                accentBorder = SageGreenPrimary.copy(alpha = 0.45f),
                elevated = true
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(Brush.linearGradient(listOf(SageGreenPrimary, TacticalTeal)))
                                .border(0.9.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(15.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(25.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile?.callsign?.ifBlank { "Пользователь" } ?: "Пользователь",
                                color = TacticalTextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = profile?.unitName?.ifBlank { "Основной склад" } ?: "Основной склад",
                                color = SageGreenBright,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val emailClean = profile?.email?.trim().orEmpty()
                            if (emailClean.isNotBlank()) {
                                Text(
                                    text = emailClean,
                                    color = TacticalTextMuted,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(SageGreenDark)
                                .border(1.dp, SageGreenPrimary.copy(alpha = 0.4f), RoundedCornerShape(9.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = workspaceProfileMode.shortBadge,
                                color = SageGreenBright,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

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
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(13.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TacticalTextPrimary),
                            border = BorderStroke(1.dp, TacticalBorder)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = SageGreenBright, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Данные", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }

                        Button(
                            onClick = onOpenProfileSelection,
                            modifier = Modifier
                                .weight(1.3f)
                                .height(40.dp)
                                .testTag("open_profile_selection_button"),
                            shape = RoundedCornerShape(13.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SageGreenDark,
                                contentColor = SageGreenBright
                            ),
                            border = BorderStroke(1.dp, SageGreenPrimary.copy(alpha = 0.55f))
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Профиль и склады", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // SECTION 1: ПОДПИСКА И ОТЧЁТЫ
        item {
            SettingsSectionLabel("Подписка и отчёты")
        }

        // 3D PRO Card — 1 tap to open tariffs/license directly (no redundant accordion!)
        item {
            val isPro = profile?.isProActive == true
            Card3DSurface(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp,
                accentBorder = if (isPro) TacticalEmerald else TacticalGold
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(
                                    Brush.linearGradient(
                                        if (isPro) listOf(Color(0xFF10B981), Color(0xFF059669))
                                        else listOf(Color(0xFFF59E0B), Color(0xFFD97706))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isPro) "Каптёрка PRO активна" else "Лицензия PRO",
                                color = TacticalTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = if (isPro) {
                                    "Осталось ${profile?.proDaysLeft ?: 0} дн. • все функции открыты"
                                } else {
                                    "Базовый режим (1 склад, до 15 поз.) • PRO от 490 ₽"
                                },
                                color = if (isPro) SageGreenBright else TacticalGoldText,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Volumetric3DButton(
                        text = if (isPro) "Моя лицензия / Продлить" else "Открыть тарифы PRO (ЮKassa)",
                        icon = Icons.Default.Star,
                        onClick = onOpenPaymentPro,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("subscribe_pro_button"),
                        height = 46.dp,
                        cornerRadius = 14.dp,
                        gradientStart = if (isPro) Color(0xFF10B981) else Color(0xFFF59E0B),
                        gradientEnd = if (isPro) Color(0xFF059669) else Color(0xFFD97706),
                        extrusionColor = if (isPro) Color(0xFF065F46) else Color(0xFF92400E),
                        contentColor = if (isPro) Color.White else Color(0xFF1C1405)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    TextButton(
                        onClick = onRestoreLicenseFromCloud,
                        modifier = Modifier.fillMaxWidth().height(34.dp)
                    ) {
                        Text(
                            text = "Восстановить оплаченную лицензию по Email",
                            color = TacticalTextSecondary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(9.dp))
        }

        // Reports Accordion — strictly profile-appropriate forms only!
        item {
            CollapsibleCard3D(
                title = "Отчёты и бланки Excel",
                subtitle = if (isMil) {
                    "Форма № 8, Форма № 18, Акт списания, Сводная"
                } else {
                    "Оборотная ТМЦ, М-11 / ТОРГ-13, МБ-7, МБ-8"
                },
                icon = Icons.Default.TableChart,
                iconColor = TacticalEmerald,
                isExpanded = expandedReports,
                onToggle = { expandedReports = !expandedReports }
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReportCompactRow(
                        title = if (isMil) "Сводная ведомость МТО (Все позиции)" else "Оборотная ведомость остатков ТМЦ",
                        docType = "Все склады и категории",
                        onClick = onExportFullConsolidatedClick
                    )
                    ReportCompactRow(
                        title = if (isMil) "Ведомости остатков по точкам" else "Ведомости остатков по складам",
                        docType = "Приход, расход и фактический остаток",
                        onClick = onExportPointSummaryClick
                    )
                    ReportCompactRow(
                        title = if (isMil) "Форма № 8 (ОКУД 6002203 • Раздаточная)" else "Накладная М-11 / ТОРГ-13 (Перемещения)",
                        docType = if (isMil) "Приказ МО РФ № 139" else "Внутреннее перемещение ТМЦ",
                        onClick = onExportForm8Click
                    )
                    ReportCompactRow(
                        title = if (isMil) "Форма № 18 (ОКУД 0504042 • Книга учёта)" else "Ведомость выдачи на руки (МБ-7)",
                        docType = if (isMil) "Наличие и движение по наименованиям" else "Выдача инструмента и возврат",
                        onClick = onExportForm18Click
                    )
                    ReportCompactRow(
                        title = if (isMil) "Акт списания (расхода) МЦ" else "Акт списания ТМЦ (МБ-8 / ТОРГ-16)",
                        docType = if (isMil) "Комиссионное списание по акту" else "Износ, поломка и расход ТМЦ",
                        onClick = onExportWriteOffActClick
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // SECTION 2: СИНХРОНИЗАЦИЯ И ДАННЫЕ
        item {
            SettingsSectionLabel("Синхронизация и данные")
        }

        // Unified Sync + 2nd Phone Card (replaces 2 duplicate accordions!)
        item {
            CollapsibleCard3D(
                title = if (isMil) "Облако и 2-й телефон" else "Общий склад и 2-й телефон",
                subtitle = "$syncPillText • $deviceCount $deviceWord • $unitKeyDisplay",
                icon = Icons.Default.Cloud,
                iconColor = TacticalTeal,
                isExpanded = expandedSync,
                onToggle = { expandedSync = !expandedSync },
                trailingBadge = {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(syncPillBg)
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (unitKey.isNotBlank() && syncState.isOnline && !syncState.isPaused) {
                            PulsingStatusDot(color = SageGreenBright, size = 6.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(syncPillText, color = syncPillFg, fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(TacticalBg)
                            .border(1.dp, SageGreenPrimary.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .clickable(enabled = unitKey.isNotBlank()) { copyToClip(context, unitKey) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isMil) "КОД ПОДРАЗДЕЛЕНИЯ" else "КОД ОБЩЕГО СКЛАДА",
                                color = TacticalGoldText,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = unitKeyDisplay,
                                color = SageGreenBright,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${syncState.syncMessage} • $deviceCount $deviceWord",
                                color = TacticalTextMuted,
                                fontSize = 10.5.sp
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onSyncClick,
                            modifier = Modifier.weight(1f).height(40.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SageGreenPrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Синхр.", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onOpenConnectCodeDialog,
                            modifier = Modifier
                                .weight(1.2f)
                                .height(40.dp)
                                .testTag("open_code_dialog_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SageGreenBright),
                            border = BorderStroke(1.dp, SageGreenPrimary.copy(alpha = 0.6f))
                        ) {
                            Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("QR и 2-й тел.", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(9.dp))
        }

        // Backup Accordion
        item {
            CollapsibleCard3D(
                title = "Резервная копия",
                subtitle = com.example.ui.components.backupSubtitle(lastCloudBackupAt, lastAutoBackupAt),
                icon = Icons.Default.Restore,
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
                    onRestoreCloud = onRestoreCloudBackup,
                    profileMode = workspaceProfileMode
                )
            }
            Spacer(modifier = Modifier.height(9.dp))
        }

        // Categories Accordion (clean, zero military buttons in Civilian/Empty)
        item {
            CollapsibleCard3D(
                title = "Категории справочника",
                subtitle = "Активных категорий: ${availableCategories.size}",
                icon = Icons.Default.Category,
                iconColor = SageGreenBright,
                isExpanded = expandedCategories,
                onToggle = { expandedCategories = !expandedCategories }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Ненужные категории можно удалить — они исчезнут из фильтров и списков.",
                        color = TacticalTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

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
                                    contentDescription = "Удалить категорию $category",
                                    tint = TacticalRedText,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showAddCategoryDialog = true },
                            modifier = Modifier.weight(1f).height(38.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SageGreenPrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Добавить", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onResetCategories,
                            modifier = Modifier.weight(1f).height(38.dp),
                            border = BorderStroke(1.dp, TacticalBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TacticalTextSecondary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Сброс", fontSize = 11.5.sp)
                        }
                    }

                    if (workspaceProfileMode == WorkspaceProfileMode.MILITARY) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { onImportTemplateCatalog(WorkspaceProfileMode.MILITARY) },
                            modifier = Modifier.fillMaxWidth().height(38.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, TacticalBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TacticalTextPrimary)
                        ) {
                            Text("+ Дополнить каталог служб", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    } else if (workspaceProfileMode == WorkspaceProfileMode.CIVILIAN) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { onImportTemplateCatalog(WorkspaceProfileMode.CIVILIAN) },
                            modifier = Modifier.fillMaxWidth().height(38.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, TacticalBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TacticalTextPrimary)
                        ) {
                            Text("+ Дополнить каталог Мастерской", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // SECTION 3: СПРАВКА И УПРАВЛЕНИЕ
        item {
            SettingsSectionLabel("Справка и управление")
        }

        // 1-Tap Direct Action Row: User Manual (no extra accordion!)
        item {
            Card3DSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenManualClick() }
                    .testTag("open_user_manual_button"),
                cornerRadius = 18.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SageGreenPrimary.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.HelpOutline, contentDescription = null, tint = SageGreenBright, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Инструкция пользователя", color = TacticalTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Пошаговый гид по всем функциям", color = TacticalTextMuted, fontSize = 11.sp)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TacticalTextMuted, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.height(9.dp))
        }

        // Support & Links (neatly collapsed at the bottom instead of taking the whole top screen!)
        item {
            CollapsibleCard3D(
                title = "Связь, сайт и поддержка",
                subtitle = "kapterka-pro.ru • Telegram-канал • бот помощи",
                icon = Icons.Default.SupportAgent,
                iconColor = Color(0xFF38BDF8),
                isExpanded = expandedCommunity,
                onToggle = { expandedCommunity = !expandedCommunity }
            ) {
                com.example.ui.components.CommunityLinksCard(modifier = Modifier.padding(10.dp))
            }
            Spacer(modifier = Modifier.height(9.dp))
        }

        // Legal Docs (152-ФЗ)
        item {
            CollapsibleCard3D(
                title = "Правовые документы (152-ФЗ)",
                subtitle = "Политика конфиденциальности, оферта и согласие",
                icon = Icons.Default.Security,
                iconColor = SageGreenBright,
                isExpanded = expandedLegal,
                onToggle = { expandedLegal = !expandedLegal }
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LegalDocRow(Icons.Default.Security, "Политика конфиденциальности (152-ФЗ)") {
                        legalDialogTab = LegalDocumentTab.PRIVACY
                        showLegalDialog = true
                    }
                    LegalDocRow(Icons.Default.Description, "Пользовательское соглашение (Оферта)") {
                        legalDialogTab = LegalDocumentTab.TERMS
                        showLegalDialog = true
                    }
                    LegalDocRow(Icons.Default.CheckCircle, "Согласие на обработку данных") {
                        legalDialogTab = LegalDocumentTab.CONSENT
                        showLegalDialog = true
                    }
                }
            }
            Spacer(modifier = Modifier.height(9.dp))
        }

        // Danger Zone
        item {
            CollapsibleCard3D(
                title = "Опасная зона",
                subtitle = "Очистка остатков, журнала и заявок",
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
                        text = "Очистка удалит операции, заявки и остатки текущей группы учёта локально и из облака. Для защиты потребуется ввести код подключения.",
                        color = TacticalRedText,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                    Button(
                        onClick = { showResetDataConfirmDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("reset_data_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TacticalRedDark,
                            contentColor = TacticalRedText
                        ),
                        border = BorderStroke(1.dp, TacticalRed.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ОЧИСТИТЬ УЧЁТ", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Logout button
        item {
            OutlinedButton(
                onClick = onLogoutClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .testTag("logout_button"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TacticalTextMuted),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, TacticalBorder)
            ) {
                Text("Выйти из аккаунта / Сменить пользователя", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Footer (preserves 7-tap developer backdoor)
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
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Разработчик: Васев Алексей Евгеньевич",
                    color = SageGreenBright,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = if (isMil) {
                        "Версия: v${com.example.BuildConfig.VERSION_NAME} PRO (Воинский учёт)"
                    } else {
                        "Версия: v${com.example.BuildConfig.VERSION_NAME} PRO"
                    },
                    color = TacticalTextMuted,
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))
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
private fun SettingsSectionLabel(title: String) {
    Text(
        text = title.uppercase(),
        color = TacticalTextMuted,
        fontSize = 10.5.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 7.dp)
    )
}

@Composable
private fun LegalDocRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TacticalBg)
            .border(1.dp, TacticalBorderSubtle, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = SageGreenBright, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, color = TacticalTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TacticalTextMuted, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun CollapsibleCard3D(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    trailingBadge: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card3DSurface(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        accentBorder = if (isExpanded) iconColor.copy(alpha = 0.45f) else null,
        elevated = isExpanded
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
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
                            .background(iconColor.copy(alpha = 0.13f)),
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
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            color = TacticalTextMuted,
                            fontSize = 10.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (trailingBadge != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    trailingBadge()
                }
                Spacer(modifier = Modifier.width(6.dp))

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(TacticalSurfaceLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = if (isExpanded) SageGreenPrimary else TacticalTextMuted,
                        modifier = Modifier.size(18.dp)
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
                .background(TacticalEmeraldDark)
                .padding(horizontal = 9.dp, vertical = 4.dp)
        ) {
            Text(
                text = "Excel",
                color = TacticalEmeraldText,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun copyToClip(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("UnitCode", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Код $text скопирован", Toast.LENGTH_SHORT).show()
}
