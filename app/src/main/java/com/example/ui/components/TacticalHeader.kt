package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.ui.theme.SageGreenBright
import com.example.ui.theme.SageGreenDark
import com.example.ui.theme.SageGreenPrimary
import com.example.ui.theme.TacticalBg
import com.example.ui.theme.TacticalBorder
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val unitKey = profile?.unitKey ?: "kapt_59e13b"

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(TacticalBg)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // TOP STATUS BAR & DEMO BADGE + GUIDE BUTTON
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Unit & Callsign summary
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(TacticalSurfaceLight)
                    .border(1.dp, TacticalBorderSubtle, RoundedCornerShape(20.dp))
                    .clickable { onProfileClick() }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(SageGreenPrimary)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = profile?.callsign ?: "Старшина",
                    color = TacticalTextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = " • ${profile?.unitName ?: "1-я Рота"}",
                    color = TacticalTextSecondary,
                    fontSize = 11.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Interactive Guide Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(SageGreenDark)
                        .border(1.dp, SageGreenPrimary, RoundedCornerShape(100.dp))
                        .clickable { onHelpClick() }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "Инструкция",
                        tint = SageGreenBright,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ИНСТРУКЦИЯ",
                        color = SageGreenBright,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Sleek Demo Tag or PRO Tag
                if (profile?.isProActive == true) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(SageGreenDark)
                            .border(1.dp, SageGreenPrimary.copy(alpha = 0.5f), RoundedCornerShape(100.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "ТАРИФ PRO",
                            color = SageGreenBright,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(TacticalGold)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "ДЕМО (3 ДНЯ)",
                            color = Color(0xFF0D0E10),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // MAIN HEADER ROW: КАПТЁРКА ПРО + UNIT KEY PILL
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "КАПТЁРКА",
                        color = TacticalTextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "ПРО",
                        color = SageGreenPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )
                }
                Text(
                    text = "СКЛАД • УЧЕТ • СНАБЖЕНИЕ",
                    color = TacticalTextMuted,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
            }

            // Unit Key Pill with quick copy
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(TacticalSurfaceLight)
                        .border(1.dp, TacticalBorder, RoundedCornerShape(12.dp))
                        .clickable {
                            val clipManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipManager.setPrimaryClip(ClipData.newPlainText("UnitKey", unitKey))
                            Toast.makeText(context, "Код скопирован: $unitKey", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(SageGreenPrimary)
                            .alpha(pulseAlpha)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = unitKey,
                        color = SageGreenPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "КОД ПОДРАЗДЕЛЕНИЯ / СКЛАДА",
                    color = TacticalTextMuted,
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // QUICK ACTION HEADER BUTTONS: Синхр, Экспорт
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HeaderActionButton(
                label = "Синхронизация",
                icon = Icons.Default.Sync,
                onClick = onSyncClick,
                modifier = Modifier
                    .weight(1f)
                    .testTag("header_sync_button")
            )

            HeaderActionButton(
                label = "Отчеты Excel",
                icon = Icons.Default.FileDownload,
                onClick = onExportClick,
                modifier = Modifier
                    .weight(1f)
                    .testTag("header_export_button")
            )
        }
    }
}

@Composable
private fun HeaderActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(TacticalSurfaceLight)
            .border(1.dp, TacticalBorderSubtle, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = SageGreenPrimary,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = TacticalTextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

