package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SageGreenBright
import com.example.ui.theme.SageGreenPrimary
import com.example.ui.theme.TacticalBorderSubtle
import com.example.ui.theme.TacticalGold
import com.example.ui.theme.TacticalTextMuted
import com.example.ui.theme.TacticalTextPrimary
import com.example.ui.theme.TacticalTextSecondary
import com.example.util.CrashReporter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun formatBackupTime(ms: Long): String =
    SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(ms))

/** Короткая строка для заголовка сворачиваемой карточки. */
fun backupSubtitle(lastCloudBackupAt: Long, lastAutoBackupAt: Long): String = when {
    lastCloudBackupAt > 0L -> "В облаке: " + formatBackupTime(lastCloudBackupAt)
    lastAutoBackupAt > 0L -> "На телефоне: " + formatBackupTime(lastAutoBackupAt)
    else -> "Сохраните данные в файл или в облако"
}

/**
 * Содержимое карточки «Резервная копия»: два блока (файл и облако) и, если было падение,
 * отчёт о сбое. Рамку и заголовок рисует сворачиваемая карточка на экране «Ещё».
 */
@Composable
fun BackupAndDiagnosticsCard(
    suggestedFileName: String,
    lastAutoBackupAt: Long,
    onSaveBackup: (Uri) -> Unit,
    onRestoreBackup: (Uri) -> Unit,
    lastCloudBackupAt: Long = 0L,
    onSaveCloud: () -> Unit = {},
    onRestoreCloud: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var pendingRestore by remember { mutableStateOf<Uri?>(null) }
    var confirmCloudRestore by remember { mutableStateOf(false) }
    var crashText by remember { mutableStateOf(CrashReporter.read(context)) }

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) onSaveBackup(uri) }
    val openLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) pendingRestore = uri }

    Column(modifier = modifier.fillMaxWidth().padding(12.dp)) {
        Text(
            "Склады, остатки, операции и заявки. Копия защищает данные, если телефон потерян или сброшен.",
            color = TacticalTextSecondary, fontSize = 11.sp, lineHeight = 15.sp
        )
        Spacer(Modifier.height(10.dp))

        BackupSection(
            title = "В ФАЙЛ",
            status = if (lastAutoBackupAt > 0L) {
                "Автокопия на телефоне раз в неделю. Последняя: " + formatBackupTime(lastAutoBackupAt)
            } else "Автокопия на телефоне раз в неделю. Пока не создавалась",
            hint = "Файл можно сохранить на Google Диск или перенести на другой телефон.",
            primaryLabel = "Сохранить",
            onPrimary = { saveLauncher.launch(suggestedFileName) },
            secondaryLabel = "Восстановить",
            onSecondary = { openLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) }
        )

        Spacer(Modifier.height(10.dp))

        BackupSection(
            title = "В ОБЛАКЕ ПОДРАЗДЕЛЕНИЯ",
            status = if (lastCloudBackupAt > 0L) {
                "Обновляется раз в сутки. Последняя: " + formatBackupTime(lastCloudBackupAt)
            } else "Обновляется раз в сутки. Пока не создавалась",
            hint = "Нужен общий учёт (код подразделения) и интернет.",
            primaryLabel = "В облако",
            onPrimary = onSaveCloud,
            secondaryLabel = "Из облака",
            onSecondary = { confirmCloudRestore = true }
        )

        if (crashText != null) {
            Spacer(Modifier.height(10.dp))
            Surface(
                color = Color(0xFF131C16),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, TacticalGold.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        "ОТЧЁТ О СБОЕ", color = TacticalGold, fontSize = 9.sp,
                        fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Приложение недавно закрылось с ошибкой. В отчёте только версия, модель телефона и техническая запись, без ваших данных.",
                        color = TacticalTextSecondary, fontSize = 11.sp, lineHeight = 15.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val send = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Отчёт о сбое Каптёрка PRO")
                                    putExtra(Intent.EXTRA_TEXT, crashText)
                                }
                                context.startActivity(Intent.createChooser(send, "Отправить отчёт"))
                            },
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SageGreenPrimary, contentColor = Color.White)
                        ) { Text("Отправить", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        OutlinedButton(
                            onClick = { CrashReporter.clear(context); crashText = null },
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TacticalTextMuted),
                            border = BorderStroke(1.dp, TacticalBorderSubtle)
                        ) { Text("Удалить", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }

    if (confirmCloudRestore) {
        AlertDialog(
            onDismissRequest = { confirmCloudRestore = false },
            title = { Text("Восстановить из облака?") },
            text = {
                Text(
                    "Текущие данные на этом телефоне будут заменены последней облачной копией подразделения. " +
                        "Перед заменой приложение сохранит текущие данные в папку приложения. " +
                        "Если подключён общий учёт, такое же состояние получат и все остальные телефоны подразделения."
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmCloudRestore = false; onRestoreCloud() }) { Text("Восстановить") }
            },
            dismissButton = { TextButton(onClick = { confirmCloudRestore = false }) { Text("Отмена") } }
        )
    }

    pendingRestore?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text("Восстановить из копии?") },
            text = {
                Text(
                    "Текущие склады, остатки, операции и заявки на этом телефоне будут заменены данными из файла. " +
                        "Перед заменой приложение само сохранит текущие данные в папку приложения. " +
                        "Если подключён общий учёт, такое же состояние получат и все остальные телефоны подразделения."
                )
            },
            confirmButton = {
                TextButton(onClick = { pendingRestore = null; onRestoreBackup(uri) }) { Text("Восстановить") }
            },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun BackupSection(
    title: String,
    status: String,
    hint: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String,
    onSecondary: () -> Unit
) {
    Surface(
        color = Color(0xFF131C16),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, TacticalBorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                title, color = TacticalGold, fontSize = 9.sp,
                fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.height(4.dp))
            Text(status, color = TacticalTextPrimary, fontSize = 11.sp, lineHeight = 15.sp)
            Text(hint, color = TacticalTextMuted, fontSize = 10.sp, lineHeight = 14.sp)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onPrimary,
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SageGreenPrimary, contentColor = Color.White)
                ) { Text(primaryLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                OutlinedButton(
                    onClick = onSecondary,
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SageGreenBright),
                    border = BorderStroke(1.dp, SageGreenPrimary.copy(alpha = 0.6f))
                ) { Text(secondaryLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}
