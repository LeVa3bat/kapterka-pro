package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.admin.CommandCenterStats
import com.example.data.admin.OnlineDevice
import com.example.ui.theme.SageGreenBright
import com.example.ui.theme.SageGreenDark
import com.example.ui.theme.SageGreenPrimary
import com.example.ui.theme.TacticalBg
import com.example.ui.theme.TacticalBorderSubtle
import com.example.ui.theme.TacticalGoldText
import com.example.ui.theme.TacticalRedText
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalSurfaceLight
import com.example.ui.theme.TacticalTealText
import com.example.ui.theme.TacticalTextMuted
import com.example.ui.theme.TacticalTextPrimary
import com.example.ui.theme.TacticalTextSecondary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Owner-only "Командный центр": who is online right now, users, licences and
 * revenue. Opened from the secret developer entry after server-side login.
 */
@Composable
fun CommandCenterDialog(
    stats: CommandCenterStats?,
    errorMessage: String,
    onRefresh: () -> Unit,
    onOpenRegistry: () -> Unit,
    onDismiss: () -> Unit
) {
    // Live: refresh every 30 seconds while the screen is open.
    LaunchedEffect(Unit) {
        while (true) {
            onRefresh()
            delay(30_000)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CommandCenterContent(stats, errorMessage, onRefresh, onOpenRegistry, onDismiss)
    }
}

@Composable
fun CommandCenterContent(
    stats: CommandCenterStats?,
    errorMessage: String,
    onRefresh: () -> Unit,
    onOpenRegistry: () -> Unit,
    onDismiss: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale("ru")) }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(TacticalBg),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 10.dp, top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(SageGreenPrimary, TacticalTealText))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Командный центр", color = TacticalTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        text = stats?.let { "Обновлено в ${timeFormat.format(Date(it.generatedAt))} • авто каждые 30 с" }
                            ?: "Загрузка данных…",
                        color = TacticalTextMuted,
                        fontSize = 11.sp
                    )
                }
                RoundIcon(Icons.Default.Refresh, "Обновить", onRefresh)
                Spacer(modifier = Modifier.width(6.dp))
                RoundIcon(Icons.Default.Close, "Закрыть", onDismiss)
            }
        }

        if (errorMessage.isNotBlank()) {
            item {
                Text(
                    text = errorMessage,
                    color = TacticalRedText,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
            }
        }

        item { OnlineHero(stats) }

        item {
            val s = stats ?: CommandCenterStats()
            Column(
                modifier = Modifier.padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(Icons.Default.Groups, "Пользователи", s.usersTotal.toString(),
                        "+${s.usersNewWeek} за неделю", SageGreenBright, Modifier.weight(1f))
                    StatTile(Icons.Default.SystemUpdate, "На версии 3.6", s.usersOn36.toString(),
                        percent(s.usersOn36, s.usersTotal) + " от всех", TacticalTealText, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(Icons.Default.VerifiedUser, "Лицензии PRO", (s.licensesVerified + s.licensesLegacy).toString(),
                        "${s.licensesVerified} подтверждены сервером", TacticalGoldText, Modifier.weight(1f))
                    StatTile(Icons.Default.HourglassBottom, "Истекают ≤7 дн.", s.licensesExpiring7d.toString(),
                        "напомнить о продлении", TacticalRedText, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(Icons.Default.Payments, "Выручка за месяц", "${s.revenueMonthRub} ₽",
                        "${s.paidThisMonth} оплат", SageGreenBright, Modifier.weight(1f))
                    StatTile(Icons.Default.PersonAdd, "Новые сегодня", s.usersNewToday.toString(),
                        "${s.unitsTotal} подразделений всего", TacticalTealText, Modifier.weight(1f))
                }
            }
        }

        item { RegistrationsChart(stats?.registrations14d ?: List(14) { 0 }) }

        item {
            SectionTitle("Сейчас в сети", stats?.onlineList?.size?.toString() ?: "0")
        }
        val online = stats?.onlineList.orEmpty()
        if (online.isEmpty()) {
            item {
                Text(
                    text = if (stats == null) "Загрузка…" else "Сейчас никого нет в сети",
                    color = TacticalTextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
            }
        } else {
            items(online) { OnlineRow(it) }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SageGreenDark)
                    .clickable { onOpenRegistry() }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Реестр бойцов и выдача лицензий →", color = SageGreenBright, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun OnlineHero(stats: CommandCenterStats?) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val ring by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Restart),
        label = "ring"
    )
    val ringAlpha by pulse.animateFloat(
        initialValue = 0.55f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Restart),
        label = "ringAlpha"
    )
    val devices by animateIntAsState(stats?.devicesOnline ?: 0, tween(900), label = "devices")
    val live = SageGreenBright

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(SageGreenDark, TacticalSurface)))
            .border(1.dp, SageGreenPrimary.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(22.dp)) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .scale(ring)
                        .clip(CircleShape)
                        .background(live.copy(alpha = ringAlpha))
                )
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(live))
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text("ОНЛАЙН СЕЙЧАС", color = live, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = devices.toString(),
                color = TacticalTextPrimary,
                fontSize = 56.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.padding(bottom = 10.dp)) {
                Text(plural(devices, "устройство", "устройства", "устройств"), color = TacticalTextSecondary, fontSize = 14.sp)
                Text("в ${stats?.unitsOnline ?: 0} подразделениях", color = TacticalTextMuted, fontSize = 12.sp)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniPill("Сегодня: ${stats?.devicesToday ?: 0}")
            MiniPill("За неделю: ${stats?.devicesWeek ?: 0}")
        }
    }
}

@Composable
private fun StatTile(
    icon: ImageVector,
    label: String,
    value: String,
    hint: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(TacticalSurface)
            .border(1.dp, TacticalBorderSubtle, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(17.dp))
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(value, color = TacticalTextPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
        Text(label, color = TacticalTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(hint, color = TacticalTextMuted, fontSize = 10.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun RegistrationsChart(values: List<Int>) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val progress by animateFloatAsState(if (started) 1f else 0f, tween(900), label = "bars")
    val barColor = SageGreenBright
    val todayColor = TacticalGoldText
    val track = TacticalSurfaceLight
    val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(TacticalSurface)
            .border(1.dp, TacticalBorderSubtle, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Регистрации за 14 дней", color = TacticalTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("всего ${values.sum()}", color = TacticalTextMuted, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Canvas(modifier = Modifier.fillMaxWidth().height(90.dp)) {
            val gap = 6.dp.toPx()
            val barWidth = (size.width - gap * (values.size - 1)) / values.size
            values.forEachIndexed { i, v ->
                val x = i * (barWidth + gap)
                drawRoundRect(track, Offset(x, 0f), Size(barWidth, size.height), CornerRadius(6.dp.toPx()))
                val h = size.height * (v.toFloat() / max) * progress
                if (h > 0f) {
                    drawRoundRect(
                        color = if (i == values.lastIndex) todayColor else barColor,
                        topLeft = Offset(x, size.height - h),
                        size = Size(barWidth, h),
                        cornerRadius = CornerRadius(6.dp.toPx())
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row {
            Text("2 недели назад", color = TacticalTextMuted, fontSize = 10.sp, modifier = Modifier.weight(1f))
            Text("сегодня: ${values.lastOrNull() ?: 0}", color = todayColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun OnlineRow(device: OnlineDevice) {
    val minutes = ((System.currentTimeMillis() - device.lastSeen) / 60_000L).coerceAtLeast(0)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(TacticalSurface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(SageGreenDark),
            contentAlignment = Alignment.Center
        ) {
            Text(device.callsign.take(1).uppercase(), color = SageGreenBright, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(device.callsign, color = TacticalTextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOf(device.unitName, device.deviceModel).filter { it.isNotBlank() }.joinToString(" • "),
                color = TacticalTextMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = if (minutes < 1) "сейчас" else "$minutes мин",
            color = SageGreenBright,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SectionTitle(title: String, badge: String) {
    Row(
        modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = TacticalTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(6.dp))
        Text(badge, color = TacticalTextMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MiniPill(text: String) {
    Text(
        text = text,
        color = TacticalTextSecondary,
        fontSize = 11.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(TacticalSurfaceLight)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

@Composable
private fun RoundIcon(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(TacticalSurface)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = TacticalTextSecondary, modifier = Modifier.size(20.dp))
    }
}

private fun percent(part: Int, total: Int): String =
    if (total <= 0) "0%" else "${(part * 100 / total)}%"

private fun plural(n: Int, one: String, few: String, many: String): String {
    val mod10 = n % 10
    val mod100 = n % 100
    return when {
        mod10 == 1 && mod100 != 11 -> one
        mod10 in 2..4 && mod100 !in 12..14 -> few
        else -> many
    }
}
