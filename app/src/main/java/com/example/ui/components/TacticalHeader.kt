package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.data.sync.SyncState
import com.example.ui.theme.TacticalRed
import com.example.ui.theme.SageGreenBright
import com.example.ui.theme.SageGreenDark
import com.example.ui.theme.SageGreenPrimary
import com.example.ui.theme.TacticalBg
import com.example.ui.theme.TacticalBorderSubtle
import com.example.ui.theme.TacticalGold
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalSurfaceLight
import com.example.ui.theme.TacticalTextMuted
import com.example.ui.theme.TacticalTextPrimary
import com.example.ui.theme.TacticalTextSecondary

@Composable
fun TacticalHeader(
    profile: UserProfile?,
    onSyncClick: () -> Unit,
    onSecondPhoneClick: () -> Unit = {},
    onExportClick: () -> Unit,
    onProfileClick: () -> Unit,
    onHelpClick: () -> Unit = {},
    onBannerClick: () -> Unit = {},
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    syncState: SyncState = SyncState(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val unitKey = profile?.unitKey?.trim().orEmpty()
    val workspaceName = profile?.unitName?.takeIf { it.isNotBlank() } ?: "Основной склад"
    val userName = profile?.callsign?.takeIf { it.isNotBlank() } ?: "Пользователь"

    val isPro = profile?.isProActive == true
    val licenseText = if (isPro) "PRO • ${profile?.proDaysLeft ?: 0} дн." else "Демо"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(TacticalBg)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp)
    ) {
        // Row 1: unit (what I'm working with) + profile/licence pill.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = workspaceName,
                    color = TacticalTextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.3).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Каптёрка ПРО",
                        color = TacticalTextMuted,
                        fontSize = 11.5.sp
                    )
                    if (unitKey.isNotBlank()) {
                        Text(
                            text = "  •  $unitKey",
                            color = TacticalTextSecondary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("workspace_code", unitKey))
                                    Toast.makeText(context, "Ключ подразделения скопирован", Toast.LENGTH_SHORT).show()
                                }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(TacticalSurface)
                    .border(1.dp, TacticalBorderSubtle, RoundedCornerShape(20.dp))
                    .clickable { if (isPro) onProfileClick() else onBannerClick() }
                    .padding(start = 10.dp, end = 5.dp, top = 5.dp, bottom = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = userName,
                        color = TacticalTextPrimary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = licenseText,
                        color = if (isPro) SageGreenPrimary else TacticalGold,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(SageGreenDark),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Профиль",
                        tint = SageGreenPrimary,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Row 2: service actions. Sync shows its own state under the label.
        val syncBadge = rememberSyncBadge(syncState, unitKey.isNotBlank(), onSyncClick)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModernHeaderAction(
                label = "Синхр.",
                icon = Icons.Default.Sync,
                onClick = onSyncClick,
                modifier = Modifier.weight(1f).testTag("header_sync_button"),
                subLabel = syncBadge.text,
                accent = syncBadge.color
            )
            ModernHeaderAction(
                label = "Подключить",
                icon = Icons.Default.QrCode,
                onClick = onSecondPhoneClick,
                modifier = Modifier.weight(1f)
            )
            ModernHeaderAction(
                label = "Отчёты",
                icon = Icons.Default.FileDownload,
                onClick = onExportClick,
                modifier = Modifier.weight(1f).testTag("header_export_button")
            )
            ModernHeaderAction(
                label = "Помощь",
                icon = Icons.Default.HelpOutline,
                onClick = onHelpClick,
                modifier = Modifier.weight(1f)
            )
        }

    }
}

private class SyncBadge(val text: String, val color: Color)

/**
 * Короткий статус для кнопки «Синхр.»: можно ли доверять цифрам. Следит за интернетом
 * напрямую; когда связь возвращается, сразу запускает синхронизацию.
 */
@Composable
private fun rememberSyncBadge(syncState: SyncState, hasUnit: Boolean, onRetry: () -> Unit): SyncBadge {
    val context = LocalContext.current
    var hasNetwork by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(isNetworkAvailable(context)) }
    val currentRetry by androidx.compose.runtime.rememberUpdatedState(onRetry)
    val syncEnabled by androidx.compose.runtime.rememberUpdatedState(hasUnit)
    androidx.compose.runtime.DisposableEffect(Unit) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        val callback = object : android.net.ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                val wasOffline = !hasNetwork
                hasNetwork = true
                // Связь вернулась: сразу отправляем накопленное.
                if (wasOffline && syncEnabled) currentRetry()
            }

            override fun onLost(network: android.net.Network) {
                hasNetwork = isNetworkAvailable(context)
            }
        }
        try {
            cm?.registerDefaultNetworkCallback(callback)
        } catch (_: Exception) {
        }
        onDispose {
            try {
                cm?.unregisterNetworkCallback(callback)
            } catch (_: Exception) {
            }
        }
    }
    var now by androidx.compose.runtime.remember { androidx.compose.runtime.mutableLongStateOf(System.currentTimeMillis()) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(30_000)
            now = System.currentTimeMillis()
        }
    }
    return when {
        hasUnit && syncState.isPaused -> SyncBadge("на паузе", TacticalGold)
        !hasNetwork -> SyncBadge("нет сети", TacticalRed)
        !hasUnit -> SyncBadge("не подключено", TacticalTextSecondary)
        syncState.isSyncing -> SyncBadge("идёт…", TacticalGold)
        !syncState.isOnline -> SyncBadge("нет связи", TacticalRed)
        syncState.lastSyncTime > 0L -> {
            val minutes = ((now - syncState.lastSyncTime) / 60_000L).coerceAtLeast(0L)
            SyncBadge(
                when {
                    minutes < 1L -> "только что"
                    minutes < 60L -> "$minutes мин назад"
                    minutes < 24L * 60L -> "${minutes / 60L} ч назад"
                    else -> "${minutes / (24L * 60L)} дн. назад"
                },
                SageGreenBright
            )
        }
        else -> SyncBadge("подключено", TacticalTextSecondary)
    }
}

@Composable
private fun ModernHeaderAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subLabel: String? = null,
    accent: Color = SageGreenPrimary
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(13.dp))
            .background(TacticalSurfaceLight)
            .clickable { onClick() }
            .heightIn(min = 58.dp)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = accent,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = TacticalTextSecondary,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
        if (subLabel != null) {
            Text(
                text = subLabel,
                color = accent,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

private fun isNetworkAvailable(context: Context): Boolean {
    return try {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            ?: return true
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } catch (_: Exception) {
        true
    }
}
