package com.example.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.admin.FighterAdminRecord
import com.example.ui.theme.SageGreenBright
import com.example.ui.theme.SageGreenDark
import com.example.ui.theme.SageGreenPrimary
import com.example.ui.theme.TacticalBg
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalBorderSubtle
import com.example.ui.theme.TacticalGold
import com.example.ui.theme.TacticalGoldDark
import com.example.ui.theme.TacticalGoldText
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalSurfaceLight
import com.example.ui.theme.TacticalTextDim
import com.example.ui.theme.TacticalTextMuted
import com.example.ui.theme.TacticalTextPrimary
import com.example.ui.theme.TacticalTextSecondary
import kotlinx.coroutines.launch

data class DeveloperDiagnosticsSnapshot(
    val appVersion: String,
    val versionCode: Int,
    val applicationId: String,
    val databaseVersion: Int,
    val androidVersion: String,
    val deviceModel: String,
    val fighterId: String,
    val syncDeviceId: String,
    val callsign: String,
    val unitName: String,
    val unitKey: String,
    val isLoggedIn: Boolean,
    val licenseState: String,
    val licenseExpires: String,
    val syncState: String,
    val isOnline: Boolean,
    val connectedDevices: Int,
    val lastSync: String,
    val pointsCount: Int,
    val catalogItemsCount: Int,
    val stockRecordsCount: Int,
    val operationsCount: Int,
    val requisitionsCount: Int
) {
    private fun masked(value: String, visibleTail: Int = 4): String {
        if (value.isBlank()) return "—"
        val tail = value.takeLast(visibleTail.coerceAtMost(value.length))
        return "••••$tail"
    }

    fun toSupportReport(): String = buildString {
        appendLine("Каптёрка PRO — диагностический отчёт")
        appendLine("Версия: $appVersion ($versionCode)")
        appendLine("Package: $applicationId")
        appendLine("Room DB: v$databaseVersion")
        appendLine("Android: $androidVersion")
        appendLine("Устройство: $deviceModel")
        appendLine("Fighter ID: ${masked(fighterId, 6)}")
        appendLine("Sync Device ID: ${masked(syncDeviceId, 6)}")
        appendLine("Позывной: ${callsign.ifBlank { "—" }}")
        appendLine("Подразделение: ${unitName.ifBlank { "—" }}")
        appendLine("Unit key: ${masked(unitKey, 4)}")
        appendLine("Вход: ${if (isLoggedIn) "выполнен" else "нет"}")
        appendLine("Лицензия: $licenseState")
        appendLine("Срок: ${licenseExpires.ifBlank { "—" }}")
        appendLine("Синхронизация: $syncState")
        appendLine("Онлайн: ${if (isOnline) "да" else "нет"}")
        appendLine("Устройств в подразделении: $connectedDevices")
        appendLine("Последняя синхронизация: $lastSync")
        appendLine("Локально: точки=$pointsCount, каталог=$catalogItemsCount, остатки=$stockRecordsCount, операции=$operationsCount, заявки=$requisitionsCount")
    }
}

/**
 * Секретный диалог авторизации разработчика (не отображается в общем интерфейсе)
 */
@Composable
fun DeveloperAccessPromptDialog(
    onAuthenticate: suspend (String) -> Pair<Boolean, String>,
    onSuccessAuth: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val authPrefs = remember(context) {
        context.getSharedPreferences("kapterka_dev_access", Context.MODE_PRIVATE)
    }
    var secretKeyInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var failedAttempts by remember { mutableIntStateOf(authPrefs.getInt("failed_attempts", 0)) }
    var lockedUntilMillis by remember { mutableLongStateOf(authPrefs.getLong("locked_until_millis", 0L)) }

    Dialog(onDismissRequest = { if (!isSubmitting) onDismiss() }) {
        Card(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalSurface),
            border = BorderStroke(1.dp, TacticalGold.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = TacticalGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "СЛУЖЕБНЫЙ ТЕРМИНАЛ",
                            color = TacticalGoldText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        enabled = !isSubmitting,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = TacticalTextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Служебный ключ проверяется сервером и не хранится в приложении.",
                    color = TacticalTextSecondary,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = secretKeyInput,
                    onValueChange = {
                        secretKeyInput = it
                        errorMessage = ""
                    },
                    enabled = !isSubmitting,
                    placeholder = { Text("Секретный ключ разработчика", color = TacticalTextDim, fontSize = 12.sp) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = TacticalSurfaceLight,
                        unfocusedContainerColor = TacticalSurfaceLight,
                        focusedBorderColor = TacticalGold,
                        unfocusedBorderColor = TacticalBorder,
                        focusedTextColor = TacticalTextPrimary,
                        unfocusedTextColor = TacticalTextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = errorMessage, color = Color(0xFFFF6B6B), fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                        Text("Отмена", color = TacticalTextMuted, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val nowMillis = System.currentTimeMillis()
                            if (nowMillis < lockedUntilMillis) {
                                val seconds = ((lockedUntilMillis - nowMillis + 999L) / 1000L).coerceAtLeast(1L)
                                errorMessage = "Слишком много попыток. Повторите через $seconds сек."
                            } else if (secretKeyInput.isBlank()) {
                                errorMessage = "Введите служебный ключ."
                            } else {
                                val candidate = secretKeyInput
                                secretKeyInput = ""
                                isSubmitting = true
                                coroutineScope.launch {
                                    val (success, message) = onAuthenticate(candidate)
                                    isSubmitting = false
                                    if (success) {
                                        failedAttempts = 0
                                        lockedUntilMillis = 0L
                                        authPrefs.edit()
                                            .remove("failed_attempts")
                                            .remove("locked_until_millis")
                                            .apply()
                                        onSuccessAuth()
                                    } else {
                                        failedAttempts += 1
                                        if (failedAttempts >= 4) {
                                            failedAttempts = 0
                                            lockedUntilMillis = System.currentTimeMillis() + 30_000L
                                            authPrefs.edit()
                                                .putInt("failed_attempts", 0)
                                                .putLong("locked_until_millis", lockedUntilMillis)
                                                .apply()
                                            errorMessage = "Доступ временно заблокирован на 30 секунд."
                                        } else {
                                            authPrefs.edit().putInt("failed_attempts", failedAttempts).apply()
                                            errorMessage = message.ifBlank { "Сервер отклонил доступ." }
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TacticalGold,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(if (isSubmitting) "Проверка…" else "Войти", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/**
 * Секретное меню разработчика: реестр пользователей всех подразделений
 */
@Composable
fun DeveloperAdminDialog(
    fightersList: List<FighterAdminRecord>,
    diagnostics: DeveloperDiagnosticsSnapshot,
    onDeleteFighter: (String) -> Unit,
    onGrantLicense: (String, Int) -> Unit,
    onRefreshList: () -> Unit,
    onForceSync: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var searchQuery by remember { mutableStateOf("") }
    var fighterToDelete by remember { mutableStateOf<FighterAdminRecord?>(null) }
    var fighterToGrant by remember { mutableStateOf<FighterAdminRecord?>(null) }

    // Состояния сворачивания (по умолчанию ВСЁ СКРЫТО)
    var expandDiagnostics by remember { mutableStateOf(true) }
    var expandStats by remember { mutableStateOf(false) }
    var expandSearch by remember { mutableStateOf(false) }
    var expandRegistry by remember { mutableStateOf(false) }
    var expandedUnits by remember { mutableStateOf(setOf<String>()) }

    val filteredList = remember(fightersList, searchQuery) {
        if (searchQuery.isBlank()) {
            fightersList
        } else {
            val q = searchQuery.trim().lowercase()
            fightersList.filter {
                it.callsign.lowercase().contains(q) ||
                it.unitName.lowercase().contains(q) ||
                it.unitKey.lowercase().contains(q) ||
                it.licenseKey.lowercase().contains(q) ||
                it.role.lowercase().contains(q)
            }
        }
    }

    val groupedByUnit = remember(filteredList) {
        filteredList.groupBy { "${it.unitName} [${it.unitKey}]" }
    }

    val totalCount = fightersList.size
    val onlineCount = fightersList.count { it.isOnline }
    val proCount = fightersList.count { it.isProActive }
    val isAnyExpanded = expandDiagnostics || expandStats || expandSearch || expandRegistry

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.98f)
                .fillMaxHeight(if (isAnyExpanded) 0.90f else 0.45f)
                .padding(vertical = 6.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalBg),
            border = BorderStroke(1.5.dp, TacticalGold)
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(TacticalGoldDark),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = TacticalGoldText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "РЕЕСТР ВСЕХ ПОДРАЗДЕЛЕНИЙ",
                                color = TacticalGoldText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Секретный терминал разработчика",
                                color = TacticalTextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Кнопка свернуть/развернуть всё
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    val targetState = !isAnyExpanded
                                    expandDiagnostics = targetState
                                    expandStats = targetState
                                    expandSearch = targetState
                                    expandRegistry = targetState
                                    if (!targetState) {
                                        expandedUnits = emptySet()
                                    } else {
                                        expandedUnits = groupedByUnit.keys.toSet()
                                    }
                                },
                            color = TacticalSurfaceLight,
                            border = BorderStroke(0.5.dp, TacticalBorderSubtle)
                        ) {
                            Text(
                                text = if (isAnyExpanded) "Скрыть всё" else "Развернуть",
                                fontSize = 9.sp,
                                color = TacticalGoldText,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        IconButton(onClick = onRefreshList, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = "Обновить", tint = SageGreenBright, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = TacticalTextMuted, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable content area for collapsed/expanded accordions
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. СОСТОЯНИЕ ТЕКУЩЕГО ПРИЛОЖЕНИЯ — только чтение + безопасная синхронизация
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { expandDiagnostics = !expandDiagnostics },
                        color = TacticalSurface,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (expandDiagnostics) SageGreenPrimary else TacticalBorderSubtle)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = SageGreenBright, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "СОСТОЯНИЕ ПРИЛОЖЕНИЯ",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SageGreenBright,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Icon(
                                    imageVector = if (expandDiagnostics) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = SageGreenBright
                                )
                            }

                            AnimatedVisibility(visible = expandDiagnostics) {
                                Column(
                                    modifier = Modifier.padding(top = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    DeveloperDiagnosticRow("Версия", "${diagnostics.appVersion} (${diagnostics.versionCode})")
                                    DeveloperDiagnosticRow("Package", diagnostics.applicationId)
                                    DeveloperDiagnosticRow("Room DB", "v${diagnostics.databaseVersion}")
                                    DeveloperDiagnosticRow("Пользователь", diagnostics.callsign.ifBlank { "—" })
                                    DeveloperDiagnosticRow("Подразделение", diagnostics.unitName.ifBlank { "—" })
                                    DeveloperDiagnosticRow("Ключ подразделения", diagnostics.unitKey.ifBlank { "—" })
                                    DeveloperDiagnosticRow("Fighter ID", diagnostics.fighterId.ifBlank { "—" })
                                    DeveloperDiagnosticRow("Device ID", diagnostics.syncDeviceId.ifBlank { "—" })
                                    DeveloperDiagnosticRow("Лицензия", diagnostics.licenseState)
                                    DeveloperDiagnosticRow("Срок", diagnostics.licenseExpires.ifBlank { "—" })
                                    DeveloperDiagnosticRow("Синхронизация", diagnostics.syncState)
                                    DeveloperDiagnosticRow("Устройства", diagnostics.connectedDevices.toString())
                                    DeveloperDiagnosticRow("Последняя синхронизация", diagnostics.lastSync)
                                    DeveloperDiagnosticRow(
                                        "Локальные данные",
                                        "точки ${diagnostics.pointsCount} • каталог ${diagnostics.catalogItemsCount} • остатки ${diagnostics.stockRecordsCount} • операции ${diagnostics.operationsCount} • заявки ${diagnostics.requisitionsCount}"
                                    )
                                    DeveloperDiagnosticRow("Android", "${diagnostics.androidVersion} • ${diagnostics.deviceModel}")

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = onForceSync,
                                            modifier = Modifier.weight(1f),
                                            border = BorderStroke(1.dp, SageGreenPrimary),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = SageGreenBright)
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text("Синхронизация", fontSize = 10.sp, color = SageGreenBright)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                clipboard.setText(AnnotatedString(diagnostics.toSupportReport()))
                                                Toast.makeText(context, "Диагностический отчёт скопирован", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.weight(1f),
                                            border = BorderStroke(1.dp, TacticalGold),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = TacticalGoldText)
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text("Копировать отчёт", fontSize = 10.sp, color = TacticalGoldText)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. АККОРДЕОН: СВОДКА И СТАТИСТИКА
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { expandStats = !expandStats },
                        color = TacticalSurface,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (expandStats) TacticalGold else TacticalBorderSubtle)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Shield, contentDescription = null, tint = TacticalGoldText, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "СВОДКА И СТАТИСТИКА",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TacticalGoldText,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "($totalCount чел. / $onlineCount в сети)",
                                        fontSize = 10.sp,
                                        color = TacticalTextMuted
                                    )
                                }
                                Icon(
                                    imageVector = if (expandStats) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = TacticalGoldText
                                )
                            }

                            AnimatedVisibility(visible = expandStats) {
                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceAround,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("БОЙЦОВ", color = TacticalTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            Text("$totalCount", color = TacticalTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Box(modifier = Modifier.width(1.dp).height(20.dp).background(TacticalBorder))
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("В СЕТИ", color = SageGreenBright, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            Text("$onlineCount", color = SageGreenBright, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Box(modifier = Modifier.width(1.dp).height(20.dp).background(TacticalBorder))
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("С ЛИЦЕНЗИЕЙ", color = TacticalGoldText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            Text("$proCount", color = TacticalGoldText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. АККОРДЕОН: ПОИСК И ФИЛЬТР
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { expandSearch = !expandSearch },
                        color = TacticalSurface,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (expandSearch) TacticalGold else TacticalBorderSubtle)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = TacticalGoldText, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "ПОИСК И ФИЛЬТР",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TacticalGoldText,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    if (searchQuery.isNotBlank()) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "• «$searchQuery»",
                                            fontSize = 10.sp,
                                            color = SageGreenBright
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = if (expandSearch) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = TacticalGoldText
                                )
                            }

                            AnimatedVisibility(visible = expandSearch) {
                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        placeholder = { Text("Позывной, рота, ключ, лицензия...", color = TacticalTextDim, fontSize = 11.sp) },
                                        singleLine = true,
                                        trailingIcon = {
                                            if (searchQuery.isNotEmpty()) {
                                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                                                    Icon(Icons.Default.Close, contentDescription = "Очистить", tint = TacticalTextMuted, modifier = Modifier.size(14.dp))
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = TacticalSurfaceLight,
                                            unfocusedContainerColor = TacticalSurfaceLight,
                                            focusedBorderColor = TacticalGold,
                                            unfocusedBorderColor = TacticalBorderSubtle,
                                            focusedTextColor = TacticalTextPrimary,
                                            unfocusedTextColor = TacticalTextPrimary
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // 4. АККОРДЕОН: РЕЕСТР БОЙЦОВ ПО ПОДРАЗДЕЛЕНИЯМ
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { expandRegistry = !expandRegistry },
                        color = TacticalSurface,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (expandRegistry) TacticalGold else TacticalBorderSubtle)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = TacticalGoldText, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "СПИСОК БОЙЦОВ И ПОДРАЗДЕЛЕНИЙ",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TacticalGoldText,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (expandRegistry) "Скрыть" else "Открыть ($totalCount)",
                                        fontSize = 10.sp,
                                        color = TacticalTextMuted
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = if (expandRegistry) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = TacticalGoldText
                                    )
                                }
                            }

                            AnimatedVisibility(visible = expandRegistry) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (filteredList.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("Зарегистрированных бойцов пока нет", color = TacticalTextMuted, fontSize = 11.sp)
                                        }
                                    } else {
                                        groupedByUnit.forEach { (unitHeader, fightersInUnit) ->
                                            val isUnitOpen = expandedUnits.contains(unitHeader)

                                            // Заголовок подразделения (также сворачиваемый!)
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .clickable {
                                                        expandedUnits = if (isUnitOpen) {
                                                            expandedUnits - unitHeader
                                                        } else {
                                                            expandedUnits + unitHeader
                                                        }
                                                    },
                                                color = Color(0xFF1B241E),
                                                border = BorderStroke(0.5.dp, SageGreenPrimary.copy(alpha = 0.5f)),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = Icons.Default.Key,
                                                            contentDescription = null,
                                                            tint = SageGreenBright,
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = unitHeader,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = SageGreenBright,
                                                            fontFamily = FontFamily.Monospace
                                                        )
                                                    }
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = "${fightersInUnit.size} чел.",
                                                            fontSize = 9.sp,
                                                            color = TacticalTextMuted
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Icon(
                                                            imageVector = if (isUnitOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                            contentDescription = null,
                                                            tint = SageGreenBright,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            // Список бойцов подразделения (раскрывается при клике на подразделение)
                                            AnimatedVisibility(visible = isUnitOpen) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(start = 6.dp, top = 4.dp),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    fightersInUnit.forEach { fighter ->
                                                        FighterAdminCard(
                                                            record = fighter,
                                                            onCopyKey = { key ->
                                                                clipboard.setText(AnnotatedString(key))
                                                                Toast.makeText(context, "Скопировано: $key", Toast.LENGTH_SHORT).show()
                                                            },
                                                            onGrantLicense = { fighterToGrant = fighter },
                                                            onDeleteClick = { fighterToDelete = fighter }
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
        }
    }

    // Подтверждение выдачи лицензии: исключает случайное изменение платного статуса.
    fighterToGrant?.let { fighter ->
        AlertDialog(
            onDismissRequest = { fighterToGrant = null },
            title = {
                Text(
                    text = "Выдать 30 дней PRO?",
                    color = TacticalTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Пользователь «${fighter.callsign}», подразделение «${fighter.unitName}». Срок лицензии будет изменён штатным механизмом на 30 дней.",
                    color = TacticalTextSecondary,
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onGrantLicense(fighter.id, 30)
                        fighterToGrant = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalGoldDark, contentColor = TacticalGoldText),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Выдать 30 дней", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { fighterToGrant = null }) {
                    Text("Отмена", color = TacticalTextMuted, fontSize = 12.sp)
                }
            },
            containerColor = TacticalSurface,
            shape = RoundedCornerShape(10.dp)
        )
    }

    // Подтверждение удаления бойца
    fighterToDelete?.let { fighter ->
        AlertDialog(
            onDismissRequest = { fighterToDelete = null },
            title = {
                Text(
                    text = "Удалить пользователя?",
                    color = TacticalTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Удалить запись «${fighter.callsign}» из служебного реестра? Лицензия и складские данные не удаляются; при следующей регистрации запись может появиться снова.",
                    color = TacticalTextSecondary,
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteFighter(fighter.id)
                        fighterToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B1E1E), contentColor = Color.White),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Удалить", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { fighterToDelete = null }) {
                    Text("Отмена", color = TacticalTextMuted, fontSize = 12.sp)
                }
            },
            containerColor = TacticalSurface,
            shape = RoundedCornerShape(10.dp)
        )
    }
}

@Composable
private fun DeveloperDiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            color = TacticalTextMuted,
            fontSize = 9.sp,
            modifier = Modifier.weight(0.42f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            color = TacticalTextPrimary,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(0.58f)
        )
    }
}

/**
 * Карточка одного бойца в меню разработчика
 */
@Composable
private fun FighterAdminCard(
    record: FighterAdminRecord,
    onCopyKey: (String) -> Unit,
    onGrantLicense: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = TacticalSurface),
        border = BorderStroke(1.dp, if (record.isOnline) SageGreenPrimary.copy(alpha = 0.5f) else TacticalBorderSubtle)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // 1. Верхний ряд: Статус онлайн, Позывной, Роль и Удалить
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Онлайн индикатор
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (record.isOnline) SageGreenBright else TacticalTextDim)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = record.callsign.uppercase(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TacticalTextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• ${record.role}",
                        fontSize = 10.sp,
                        color = TacticalTextSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Статус сети
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (record.isOnline) Color(0xFF162C1E) else TacticalSurfaceLight)
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (record.isOnline) "В СЕТИ" else record.lastSeenFormatted.ifEmpty { "ОФЛАЙН" },
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (record.isOnline) SageGreenBright else TacticalTextMuted
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Кнопка удаления пользователя
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Удалить",
                            tint = Color(0xFFD9534F),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 2. Средний ряд: Подразделение и дата регистрации
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${record.unitName} (${record.unitKey})",
                    fontSize = 10.sp,
                    color = TacticalGoldText,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Рег: ${record.registeredAtFormatted}",
                    fontSize = 9.sp,
                    color = TacticalTextMuted
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 3. Нижний ряд: Лицензия и ключ
            Surface(
                color = TacticalSurfaceLight,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(0.5.dp, TacticalBorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = if (record.licenseKey.isNotEmpty()) TacticalGold else TacticalTextDim,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        if (record.licenseKey.isNotEmpty()) {
                            Text(
                                text = record.licenseKey,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TacticalTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = { onCopyKey(record.licenseKey) },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Копировать", tint = SageGreenBright, modifier = Modifier.size(11.dp))
                            }
                        } else {
                            Text("Без ключа (демо)", fontSize = 10.sp, color = TacticalTextMuted)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (record.isProActive) "${record.licenseDaysLeft} дн." else "Истекла",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (record.isProActive) SageGreenBright else Color(0xFFFF8B8B)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // Кнопка выдать лицензию в 1 клик
                        Button(
                            onClick = onGrantLicense,
                            modifier = Modifier.height(22.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TacticalGoldDark,
                                contentColor = TacticalGoldText
                            ),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("+30 дн.", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
