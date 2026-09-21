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
import com.example.BuildConfig
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
import java.security.MessageDigest

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
    onSuccessAuth: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val authPrefs = remember(context) {
        context.getSharedPreferences("kapterka_dev_access", Context.MODE_PRIVATE)
    }
    var secretKeyInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var failedAttempts by remember { mutableIntStateOf(authPrefs.getInt("failed_attempts", 0)) }
    var lockedUntilMillis by remember { mutableLongStateOf(authPrefs.getLong("locked_until_millis", 0L)) }

    fun sha256Hex(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    fun isDeveloperKeyValid(value: String): Boolean {
        val expected = BuildConfig.DEV_ADMIN_KEY_SHA256
        if (expected.length != 64) return false
        val actual = sha256Hex(value.trim())
        return MessageDigest.isEqual(actual.toByteArray(), expected.toByteArray())
    }

    Dialog(onDismissRequest = onDismiss) {
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
                    IconButton(onClick = onDismiss, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = TacticalTextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Доступ к единому реестру бойцов и управлению лицензиями всех подразделений.",
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
                    TextButton(onClick = onDismiss) {
                        Text("Отмена", color = TacticalTextMuted, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val nowMillis = System.currentTimeMillis()
                            if (nowMillis < lockedUntilMillis) {
                                val seconds = ((lockedUntilMillis - nowMillis + 999L) / 1000L).coerceAtLeast(1L)
                                errorMessage = "Слишком много попыток. Повторите через $seconds сек."
                            } else if (isDeveloperKeyValid(secretKeyInput)) {
                                secretKeyInput = ""
                                failedAttempts = 0
                                lockedUntilMillis = 0L
                                authPrefs.edit()
                                    .remove("failed_attempts")
                                    .remove("locked_until_millis")
                                    .apply()
                                onSuccessAuth()
                            } else {
                                failedAttempts += 1
                                secretKeyInput = ""
                                if (failedAttempts >= 4) {
                                    failedAttempts = 0
                                    lockedUntilMillis = nowMillis + 30_000L
                                    authPrefs.edit()
                                        .putInt("failed_attempts", 0)
                                        .putLong("locked_until_millis", lockedUntilMillis)
                                        .apply()
                                    errorMessage = "Доступ временно заблокирован на 30 секунд."