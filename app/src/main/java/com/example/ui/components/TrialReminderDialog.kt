package com.example.ui.components

import android.content.Context
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.data.license.FighterLicenseStatus
import kotlinx.coroutines.delay

private const val REMINDER_PREFS = "kapterka_reminders"
private const val KEY_LAST_DAY = "trial_last_day_shown"
private const val KEY_EXPIRED = "trial_expired_shown"

/**
 * One-time in-app reminders about the end of the demo period: on the last day and
 * once after it ended. Shown inside the app (no system notifications, they are off
 * by design). Nothing is shown for PRO users.
 */
@Composable
fun TrialReminderDialog(
    licenseStatus: FighterLicenseStatus,
    isProActive: Boolean,
    onOpenPayment: () -> Unit
) {
    val context = LocalContext.current
    var kind by remember { mutableStateOf<String?>(null) }

    // The status flow can settle a moment after start; a change restarts this effect,
    // so a transient value never triggers (or burns) a reminder.
    LaunchedEffect(licenseStatus.isProActive, licenseStatus.isDemoActive, licenseStatus.demoDaysLeft, isProActive) {
        if (isProActive || licenseStatus.isProActive) return@LaunchedEffect
        delay(1500)
        val prefs = context.getSharedPreferences(REMINDER_PREFS, Context.MODE_PRIVATE)
        val expired = !licenseStatus.isDemoActive
        val lastDay = licenseStatus.isDemoActive && licenseStatus.demoDaysLeft <= 1
        when {
            expired && !prefs.getBoolean(KEY_EXPIRED, false) -> {
                prefs.edit().putBoolean(KEY_EXPIRED, true).putBoolean(KEY_LAST_DAY, true).apply()
                kind = "expired"
            }
            lastDay && !prefs.getBoolean(KEY_LAST_DAY, false) -> {
                prefs.edit().putBoolean(KEY_LAST_DAY, true).apply()
                kind = "last_day"
            }
        }
    }

    val current = kind ?: return
    val (title, text, action) = if (current == "expired") {
        Triple(
            "Демо-период закончился",
            "Остатки и история остаются на месте. Для списания и выгрузки отчётов нужен PRO.",
            "Активировать PRO"
        )
    } else {
        Triple(
            "Демо-период заканчивается",
            "Осталось меньше суток. Чтобы не потерять возможность списывать имущество и выгружать отчёты, оформите PRO заранее.",
            "Продлить"
        )
    }
    AlertDialog(
        onDismissRequest = { kind = null },
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = { kind = null; onOpenPayment() }) { Text(action) } },
        dismissButton = { TextButton(onClick = { kind = null }) { Text("Позже") } }
    )
}
