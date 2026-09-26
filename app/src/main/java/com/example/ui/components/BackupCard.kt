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
import androidx.compose.ui.platform.LocalContext
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

/** «Резервная копия» и «Отчёт о сбое» на экране «Ещё». */
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

    Surface(
        color = androidx.compose.ui.graphics.Color(0xFF131C16),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, TacticalBorderSubtle),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Резервная копия", color = TacticalTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Склады, остатки, операции и заявки сохраняются в файл. Его можно перенести на другой телефон и восстановить.",
                color = TacticalTextSecondary, fontSize = 11.sp, lineHeight = 15.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (lastAutoBackupAt > 0L) {
                    "Автокопия раз в неделю: " +
                        SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(lastAutoBackupAt))
                } else "Автокопия раз в неделю: ещё не создавалась",
                color = TacticalTextMuted, fontSize = 10.sp
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { saveLauncher.launch(suggestedFileName) },
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SageGreenBright),
                    border = BorderStroke(1.dp, SageGreenPrimary.copy(alpha = 0.6f))
                ) { Text("Сохранить копию", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                OutlinedButton(
                    onClick = { openLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) },
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TacticalGold),
                    border = BorderStroke(1.dp, TacticalGold.copy(alpha = 0.6f))
                ) { Text("Восстановить", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                if (lastCloudBackupAt > 0L) {
                    "Копия в облаке подразделения: " +
                        SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(lastCloudBackupAt)) +
                        " (обновляется раз в сутки)"
                } else "Копия в облаке подразделения: ещё не создавалась",
                color = TacticalTextMuted, fontSize = 10.sp
            )
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onSaveCloud,
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SageGreenBright),
                    border = BorderStroke(1.dp, SageGreenPrimary.copy(alpha = 0.6f))
                ) { Text("В облако", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                OutlinedButton(
                    onClick = { confirmCloudRestore = true },
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TacticalGold),
                    border = BorderStroke(1.dp, TacticalGold.copy(alpha = 0.6f))
                ) { Text("Из облака", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            }

            if (crashText != null) {
                Spacer(Modifier.height(12.dp))
                Text("Отчёт о сбое", color = TacticalTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Приложение недавно закрылось с ошибкой. В отчёте только версия, модель телефона и техническая запись, без ваших данных.",
                    color = TacticalTextSecondary, fontSize = 11.sp, lineHeight = 15.sp
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
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
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SageGreenBright),
                        border = BorderStroke(1.dp, SageGreenPrimary.copy(alpha = 0.6f))
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

    if (confirmCloudRestore) {
        AlertDialog(
            onDismissRequest = { confirmCloudRestore = false },
            title = { Text("Восстановить из облака?") },
            text = {
                Text(
                    "Текущие данные на этом телефоне будут заменены последней облачной копией подразделения. " +
                        "Перед заменой приложение сохранит текущие данные в папку приложения."
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
                        "Перед заменой приложение само сохранит текущие данные в папку приложения."
                )
            },
            confirmButton = {
                TextButton(onClick = { pendingRestore = null; onRestoreBackup(uri) }) { Text("Восстановить") }
            },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text("Отмена") } }
        )
    }
}
