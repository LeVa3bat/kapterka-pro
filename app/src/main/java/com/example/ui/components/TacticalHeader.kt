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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val unitKey = profile?.unitKey?.trim().orEmpty()
    val workspaceName = profile?.unitName?.takeIf { it.isNotBlank() } ?: "Основной склад"
    val userName = profile?.callsign?.takeIf { it.isNotBlank() } ?: "Пользователь"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(TacticalBg)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Каптёрка",
                        color = TacticalTextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.4).sp
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "ПРО",
                        color = SageGreenPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(SageGreenDark)
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
                Text(
                    text = "Учёт склада и имущества",
                    color = TacticalTextMuted,
                    fontSize = 12.sp
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(TacticalSurface)
                    .border(1.dp, TacticalBorderSubtle, RoundedCornerShape(18.dp))
                    .clickable { onProfileClick() }
                    .padding(start = 10.dp, end = 7.dp, top = 6.dp, bottom = 6.dp),
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
                        text = if (profile?.isProActive == true) "PRO активно" else "Профиль",
                        color = if (profile?.isProActive == true) SageGreenPrimary else TacticalTextMuted,
                        fontSize = 9.5.sp,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.width(7.dp))
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(SageGreenDark),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Профиль",
                        tint = SageGreenPrimary,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(13.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = workspaceName,
                            color = TacticalTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Текущая группа учёта",
                            color = TacticalTextMuted,
                            fontSize = 10.5.sp
                        )
                    }

                    if (unitKey.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(TacticalSurfaceLight)
                                .clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("workspace_code", unitKey))
                                    Toast.makeText(context, "Код группы скопирован", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 9.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = unitKey,
                                color = TacticalTextSecondary,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(11.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModernHeaderAction(
                        label = "Синхр.",
                        icon = Icons.Default.Sync,
                        onClick = onSyncClick,
                        modifier = Modifier.weight(1f).testTag("header_sync_button")
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

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (profile?.isProActive == true) "Лицензия PRO" else "Проверить лицензию",
                color = if (profile?.isProActive == true) SageGreenPrimary else TacticalGold,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onBannerClick() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isDarkTheme) "Светлая тема" else "Тёмная тема",
                color = TacticalTextMuted,
                fontSize = 10.5.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onToggleTheme() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun ModernHeaderAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(13.dp))
            .background(TacticalSurfaceLight)
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = SageGreenPrimary,
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
    }
}
