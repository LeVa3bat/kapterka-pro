package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.data.model.WorkspaceProfileMode
import com.example.data.sync.SyncState
import com.example.ui.theme.*

@Composable
fun TacticalHeader(
    profile: UserProfile?,
    onSyncClick: () -> Unit,
    onSecondPhoneClick: () -> Unit = {},
    onExportClick: () -> Unit,
    onProfileClick: () -> Unit,
    onHelpClick: () -> Unit = {},
    onBannerClick: () -> Unit = {},
    isDarkTheme: Boolean = true,
    onToggleTheme: () -> Unit = {},
    syncState: SyncState = SyncState(),
    profileMode: WorkspaceProfileMode = WorkspaceProfileMode.MILITARY,
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
        // Row 1: Workspace title + Profile badge + Theme toggle + User/PRO pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = workspaceName,
                        color = TacticalTextPrimary,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.4).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(SageGreenDark)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = profileMode.shortBadge,
                            color = SageGreenBright,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                    if (unitKey.isNotBlank()) {
                        Text(
                            text = unitKey,
                            color = TacticalTextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("workspace_code", unitKey))
                                    Toast.makeText(context, "Код группы скопирован", Toast.LENGTH_SHORT).show()
                                }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Quick Light/Dark theme switch button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(TacticalSurface)
                    .border(1.dp, TacticalBorderSubtle, CircleShape)
                    .clickable { onToggleTheme() }
                    .testTag("header_theme_toggle"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                    contentDescription = if (isDarkTheme) "Светлая тема" else "Тёмная тема",
                    tint = if (isDarkTheme) TacticalGold else SageGreenPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Profile / License Pill
            Row(
                modifier = Modifier
                    .widthIn(max = 164.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(TacticalSurface)
                    .border(1.dp, TacticalBorderSubtle, RoundedCornerShape(20.dp))
                    .clickable { if (isPro) onProfileClick() else onBannerClick() }
                    .padding(start = 10.dp, end = 5.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = userName,
                        color = TacticalTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = licenseText,
                        color = if (isPro) TacticalEmeraldText else TacticalGoldText,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(SageGreenDark),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = "Профиль",
                        tint = SageGreenPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Row 2: 4 crisp uniform service action cards (zero wrapping!)
        val syncBadge = rememberSyncBadge(syncState, unitKey.isNotBlank(), onSyncClick)
        val syncPaused = unitKey.isNotBlank() && syncState.isPaused
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModernHeaderAction(
                label = "Синхр.",
                icon = if (syncPaused) Icons.Rounded.Pause else Icons.Rounded.Sync,
                onClick = onSyncClick,
                modifier = Modifier.weight(1f).fillMaxHeight().testTag("header_sync_button"),
                subLabel = syncBadge.text,
                accent = syncBadge.color,
                statusDot = if (unitKey.isNotBlank()) syncBadge.color else null
            )
            ModernHeaderAction(
                label = "QR-код",
                icon = Icons.Rounded.QrCode2,
                onClick = onSecondPhoneClick,
                accent = TacticalTeal,
                subLabel = "2-й тел.",
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            ModernHeaderAction(
                label = "Отчёты",
                icon = Icons.Rounded.Description,
                onClick = onExportClick,
                accent = TacticalEmerald,
                subLabel = if (profileMode.isMilitary) "Ф-8 / Ф-18" else "Excel",
                modifier = Modifier.weight(1f).fillMaxHeight().testTag("header_export_button")
            )
            ModernHeaderAction(
                label = "Помощь",
                icon = Icons.Rounded.HelpOutline,
                onClick = onHelpClick,
                accent = SageGreenPrimary,
                subLabel = "Гид",
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
    }
}

private class SyncBadge(val text: String, val color: Color)

@Composable
private fun rememberSyncBadge(syncState: SyncState, hasUnit: Boolean, onRetry: () -> Unit): SyncBadge {
    val context = LocalContext.current
    var hasNetwork by remember { mutableStateOf(isNetworkAvailable(context)) }
    val currentRetry by rememberUpdatedState(onRetry)
    val syncEnabled by rememberUpdatedState(hasUnit && !syncState.isPaused)
    DisposableEffect(Unit) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        val callback = object : android.net.ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                val wasOffline = !hasNetwork
                hasNetwork = true
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
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(30_000)
            now = System.currentTimeMillis()
        }
    }
    return when {
        hasUnit && syncState.isPaused -> SyncBadge("пауза", TacticalGold)
        !hasNetwork -> SyncBadge("офлайн", TacticalRed)
        !hasUnit -> SyncBadge("локально", TacticalTextMuted)
        syncState.isSyncing -> SyncBadge("обмен…", TacticalGold)
        !syncState.isOnline -> SyncBadge("нет сети", TacticalRed)
        syncState.lastSyncTime > 0L -> {
            val minutes = ((now - syncState.lastSyncTime) / 60_000L).coerceAtLeast(0L)
            SyncBadge(
                when {
                    minutes < 1L -> "в сети"
                    minutes < 60L -> "$minutes мин"
                    minutes < 24L * 60L -> "${minutes / 60L} ч"
                    else -> "${minutes / (24L * 60L)} дн"
                },
                TacticalEmerald
            )
        }
        else -> SyncBadge("в сети", TacticalEmerald)
    }
}

@Composable
private fun ModernHeaderAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subLabel: String? = null,
    accent: Color = SageGreenPrimary,
    statusDot: Color? = null
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(TacticalSurface)
            .border(1.dp, TacticalBorderSubtle, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .heightIn(min = 60.dp)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
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
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = label,
                color = TacticalTextPrimary,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            if (subLabel != null) {
                Text(
                    text = subLabel,
                    color = accent,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (statusDot != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 6.dp)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(statusDot)
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
