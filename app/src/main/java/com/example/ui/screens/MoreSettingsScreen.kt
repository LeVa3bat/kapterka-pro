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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TableChart
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
    onResetProfileAndLicense: () -> Unit = {},
    onResetDataClick: () -> Unit,
    onOpenManualClick: () -> Unit = {},
    onOpenDeveloperBackdoor: () -> Unit = {}
) {
    val context = LocalContext.current
    val unitKey = profile?.unitKey ?: "kapt_59e13b"
    var devVersionTaps by remember { androidx.compose.runtime.mutableIntStateOf(0) }

    // Accordions state: all collapsed by default to save space as requested
    var expandedProfile by remember { mutableStateOf(false) }
    var expandedConnectCode by remember { mutableStateOf(false) }
    var expandedProPlan by remember { mutableStateOf(false) }
    var expandedReports by remember { mutableStateOf(false) }
    var expandedCategories by remember { mutableStateOf(false) }
    var expandedGuide by remember { mutableStateOf(false) }
    var expandedDangerZone by remember { mutableStateOf(false) }

    // Dialog state for deleting category
    var categoryToDelete by remember { mutableStateOf<String?>(null) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showResetDataConfirmDialog by remember { mutableStateOf(false) }
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
                    text = "Редактировать профиль бойца",
                    color = SageGreenBright,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Позывной:",
                        color = TacticalTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = editCallsign,
                        onValueChange = { editCallsign = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Ваш позывной", color = TacticalTextMuted) },
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
                        text = "Подразделение:",
                        color = TacticalTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = editUnitName,
                        onValueChange = { editUnitName = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Название подразделения", color = TacticalTextMuted) },
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
                            unitName = editUnitName.trim().ifEmpty { profile?.unitName ?: "1-е Подразделение" },
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
        AlertDialog(
            onDismissRequest = { showResetDataConfirmDialog = false },
            containerColor = TacticalSurface,
            title = {
                Text(
                    text = "Сброс всех данных!",
                    color = TacticalRedText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "ВНИМАНИЕ! Это действие необратимо удалит все операции, перемещения, выдачи и остатки имущества со всех точек на этом устройстве. Имущество в справочнике и склады останутся, но их балансы обнулятся. Вы действительно хотите удалить данные?",
                    color = TacticalTextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetDataClick()
                        showResetDataConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TacticalRed,
                        contentColor = Color.White
                    )
                ) {
                    Text("ПОЛНЫЙ СБРОС ДАННЫХ", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDataConfirmDialog = false }) {
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
                    text = "Удалить службу/группу?",
                    color = TacticalRedText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "Вы уверены, что хотите удалить штатную группу «${categoryToDelete}»? Все закрепленные за ней позиции будут удалены из каталога.",
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
                    text = "Добавить штатную группу",
                    color = SageGreenBright,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Введите название новой службы или группы снабжения:",
                        color = TacticalTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Введите название", color = TacticalTextMuted) },
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
                    text = "НАСТРОЙКИ И ОТЧЕТНОСТЬ",
                    color = SageGreenBright,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Компактное меню управления подразделением и группами",
                    color = TacticalTextMuted,
                    fontSize = 12.sp
                )
            }
        }

        // 1. COLLAPSIBLE ACCORDION: ПРОФИЛЬ ПОДРАЗДЕЛЕНИЯ
        item {
            CollapsibleCard(
                title = "Облачная база Google Firebase",
                subtitle = "Канал: ${profile?.unitKey ?: "kapt_59e13b"} • Онлайн синхронизация",
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
                                text = "Позывной: ${profile?.callsign ?: "Старшина"}",
                                color = TacticalTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Подразделение: ${profile?.unitName ?: "1-е Подразделение"}",
                                color = SageGreenBright,
                                fontSize = 12.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (syncState.isSyncing) TacticalGold.copy(alpha = 0.2f) else SageGreenDark)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (syncState.isSyncing) "СИНХРОНИЗАЦИЯ..." else "ОНЛАЙН СИНХР.",
                                color = if (syncState.isSyncing) TacticalGold else SageGreenBright,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Cloud info box
                    Surface(
                        color = Color(0xFF131C16),
                        shape = RoundedCornerShape(8.dp),
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
                                    text = "База Firestore:",
                                    fontSize = 11.sp,
                                    color = TacticalTextMuted
                                )
                                Text(
                                    text = "kapterka-pro",
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
                                    text = unitKey,
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
                            if (syncState.connectedDevicesCount > 1) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Устройств в сети подразделения:",
                                        fontSize = 11.sp,
                                        color = TacticalTextMuted
                                    )
                                    Text(
                                        text = "${syncState.connectedDevicesCount} устройства",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SageGreenBright
                                    )
                                }
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
                            shape = RoundedCornerShape(6.dp),
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
                            shape = RoundedCornerShape(6.dp)
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
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TacticalGoldText),
                        border = BorderStroke(1.dp, TacticalGold.copy(alpha = 0.6f))
                    ) {
                        Text("☁️ Восстановить оплаченную лицензию из базы", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 2. COLLAPSIBLE ACCORDION: КОД ПОДКЛЮЧЕНИЯ ДРУГИХ ПОЛЬЗОВАТЕЛЕЙ (БЕЗ QR-КОДА)
        item {
            CollapsibleCard(
                title = "Код подключения бойцов (без QR)",
                subtitle = "Секретный код: $unitKey",
                icon = Icons.Default.Key,
                iconColor = SageGreenBright,
                isExpanded = expandedConnectCode,
                onToggle = { expandedConnectCode = !expandedConnectCode }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Для подключения других пользователей передайте им этот код. При входе на другом телефоне боец нажимает «Вход» и вводит данный код.",
                        color = TacticalTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(TacticalBg)
                            .border(1.dp, SageGreenPrimary.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .clickable {
                                copyToClip(context, unitKey)
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ЕДИНЫЙ КОД РОТЫ",
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
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Скопировать и показать код", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 3. COLLAPSIBLE ACCORDION: ПЕРСОНАЛЬНАЯ ЛИЦЕНЗИЯ БОЙЦА (30 ДНЕЙ)
        item {
            CollapsibleCard(
                title = "Лицензия бойца (ЮKassa / 30 дней)",
                subtitle = if (profile?.isProActive == true) "Активна (Осталось ${profile.proDaysLeft} дн.) • Персональный ключ" else "Требуется продление (30 дней / 490 ₽)",
                icon = Icons.Default.Star,
                iconColor = TacticalGoldText,
                isExpanded = expandedProPlan,
                onToggle = { expandedProPlan = !expandedProPlan }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Персональная лицензия закрепляется строго за вашим личным аккаунтом бойца. Срок действия выдается строго на 30 дней с момента оплаты через ЮKassa (СБП, карты МИР).",
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
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (profile?.isProActive == true) "Моя лицензия / Продлить (ЮKassa)" else "Оплатить лицензию на 30 дн. (ЮKassa)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 4. COLLAPSIBLE ACCORDION: АРМЕЙСКАЯ ОТЧЕТНОСТЬ И ВЕДОМОСТИ
        item {
            CollapsibleCard(
                title = "Армейская отчетность и ведомости",
                subtitle = "Форма № 8, Форма № 18, Сводные отчеты в Excel",
                icon = Icons.Default.TableChart,
                iconColor = SageGreenBright,
                isExpanded = expandedReports,
                onToggle = { expandedReports = !expandedReports }
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReportCompactRow(
                        title = "Полная сводная ведомость (Все позиции)",
                        docType = "Все склады и службы",
                        onClick = onExportFullConsolidatedClick
                    )
                    ReportCompactRow(
                        title = "Ведомости остатков по точкам",
                        docType = "Что есть, расход, приход",
                        onClick = onExportPointSummaryClick
                    )
                    ReportCompactRow(
                        title = "Форма № 8 (Акт списания / расхода)",
                        docType = "Акт расхода РАВ",
                        onClick = onExportForm8Click
                    )
                    ReportCompactRow(
                        title = "Форма № 18 (Книга учета движения)",
                        docType = "Ведомость движения",
                        onClick = onExportForm18Click
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 5. COLLAPSIBLE ACCORDION: УПРАВЛЕНИЕ ШТАТНЫМИ ГРУППАМИ (УДАЛЕНИЕ / ДОБАВЛЕНИЕ)
        item {
            CollapsibleCard(
                title = "Управление штатными группами",
                subtitle = "Активно служб: ${availableCategories.size} • Удаление ненужных",
                icon = Icons.Default.Category,
                iconColor = SageGreenBright,
                isExpanded = expandedCategories,
                onToggle = { expandedCategories = !expandedCategories }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Если некоторые штатные группы не нужны в вашем подразделении, вы можете их удалить. Они исчезнут из списков и фильтров.",
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
                                .clip(RoundedCornerShape(6.dp))
                                .background(TacticalBg)
                                .border(1.dp, TacticalBorderSubtle, RoundedCornerShape(6.dp))
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
                            shape = RoundedCornerShape(6.dp)
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
                            shape = RoundedCornerShape(6.dp)
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
                        a = "Передайте коллегам или бойцам код склада/подразделения (вверху экрана). На другом телефоне при запуске выберите «Подключиться по коду» и введите код. Все склады, остатки и номенклатура мгновенно синхронизируются."
                    )
                    GuideItem(
                        q = "2. Списание и акты расхода (Форма 8)",
                        a = "Нажмите «Расход» на главном экране, выберите точку, количество и причину (например, «Боевой расход», «Списание на нужды службы»). Запись автоматически сформирует акт Формы № 8."
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
                subtitle = "Сброс всех операций и остатков базы",
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
                        text = "Сброс данных удалит историю операций (приходы, расходы, перемещения, выдачи), а также обнулит все остатки на складах и точках. Эта операция локальная и необратимая.",
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
                            text = "СБРОСИТЬ ВСЕ ДАННЫЕ (ОЧИСТКА)",
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TacticalBorder)
                ) {
                    Text("Сменить позывной / Выйти из подразделения", fontSize = 12.sp)
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
                        devVersionTaps++
                        if (devVersionTaps >= 5) {
                            devVersionTaps = 0
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
                    text = "Версия программы: v3.0.1 PRO (Tactical Edition)",
                    color = TacticalTextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
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
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = TacticalSurface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isExpanded) SageGreenPrimary.copy(alpha = 0.6f) else TacticalBorder
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
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
                            .size(30.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF14241B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = title,
                            color = TacticalTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = subtitle,
                            color = TacticalTextMuted,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = if (isExpanded) SageGreenBright else TacticalTextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TacticalSurfaceLight.copy(alpha = 0.4f))
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
            .clip(RoundedCornerShape(6.dp))
            .background(TacticalBg)
            .border(1.dp, TacticalBorderSubtle, RoundedCornerShape(6.dp))
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
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF1B3D2B))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(text = "Excel", color = SageGreenBright, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun GuideItem(q: String, a: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
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
