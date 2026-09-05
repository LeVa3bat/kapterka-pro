package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SageGreenBright
import com.example.ui.theme.SageGreenContainer
import com.example.ui.theme.SageGreenDark
import com.example.ui.theme.SageGreenPrimary
import com.example.ui.theme.TacticalBg
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalGold
import com.example.ui.theme.TacticalGoldText
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalSurfaceLight
import com.example.ui.theme.TacticalTextDim
import com.example.ui.theme.TacticalTextMuted
import com.example.ui.theme.TacticalTextPrimary
import com.example.ui.theme.TacticalTextSecondary
import kotlinx.coroutines.delay
import kotlin.math.sin

@Composable
fun SplashScreen(
    onInitializationComplete: () -> Unit
) {
    // 6.0 seconds total animation
    var progress by remember { mutableFloatStateOf(0.0f) }
    var statusText by remember { mutableStateOf("ИНИЦИАЛИЗАЦИЯ ТАКТИЧЕСКОГО МОДУЛЯ АСУ...") }
    var statusCode by remember { mutableStateOf("SYS_BOOT_INIT_01") }
    var timeRemainingMs by remember { mutableLongStateOf(6000L) }

    // Pulsing and continuous sweep transitions
    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepAngle"
    )

    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )

    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    // 6-Second Smooth Animation Sequence (6000 ms)
    LaunchedEffect(Unit) {
        val totalMs = 6000L
        val interval = 50L
        var elapsed = 0L

        while (elapsed < totalMs) {
            delay(interval)
            elapsed += interval
            progress = (elapsed.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
            timeRemainingMs = (totalMs - elapsed).coerceAtLeast(0L)

            when {
                elapsed < 1200L -> {
                    statusText = "ИНИЦИАЛИЗАЦИЯ ТАКТИЧЕСКОГО МОДУЛЯ АСУ..."
                    statusCode = "SYS_BOOT_CORE // OK"
                }
                elapsed < 2400L -> {
                    statusText = "ПРОВЕРКА КРИПТОКЛЮЧА И БАЗЫ ДАННЫХ ROOM..."
                    statusCode = "SEC_CRYPTO_256 // VERIFIED"
                }
                elapsed < 3600L -> {
                    statusText = "СКАНИРОВАНИЕ ОПОРНЫХ ПУНКТОВ И СКЛАДОВ РАВ..."
                    statusCode = "GRID_SCAN_POINTS // 3 ACTIVE"
                }
                elapsed < 4800L -> {
                    statusText = "СИНХРОНИЗАЦИЯ НОМЕНКЛАТУРЫ И СЛУЖБ ТЫЛА..."
                    statusCode = "CATALOG_SYNC // COMPLETE"
                }
                else -> {
                    statusText = "СИСТЕМА БОЕВОГО УЧЕТА ГОТОВА К РАБОТЕ"
                    statusCode = "ONLINE // OFFLINE-READY 100%"
                }
            }
        }
        delay(200)
        onInitializationComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TacticalBg)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            // TOP HUD HEADER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(SageGreenBright)
                            .alpha(pulseGlow)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "АСУ «КАПТЁРКА» PRO",
                        color = SageGreenBright,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // 6s Timer Readout
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(TacticalSurfaceLight)
                        .border(1.dp, SageGreenPrimary.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = String.format("00:0%d.%02d", timeRemainingMs / 1000, (timeRemainingMs % 1000) / 10),
                        color = TacticalGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // CENTER RADAR & TELEMETRY DISPLAY
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Bright Glowing Radar Display
                Box(
                    modifier = Modifier
                        .size(210.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0B140F))
                        .border(2.5.dp, SageGreenPrimary, CircleShape)
                        .shadow(elevation = 12.dp, shape = CircleShape, spotColor = SageGreenBright),
                    contentAlignment = Alignment.Center
                ) {
                    RadarSweepCanvas(sweepAngle = sweepAngle)

                    // Center tactical reticle & shield badge
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF142B1D))
                            .border(1.5.dp, SageGreenBright, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = SageGreenBright,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Brand Name
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "КАПТЁРКА",
                        color = TacticalTextPrimary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "ПРО",
                        color = SageGreenBright,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )
                }

                Text(
                    text = "Автоматизированный воинский учет и снабжение",
                    color = TacticalTextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Live Audio / Waveform Visualizer
                WaveformTelemetry(phase = wavePhase)

                Spacer(modifier = Modifier.height(18.dp))

                // Progress Bar & Diagnostic Readout
                Column(
                    modifier = Modifier.fillMaxWidth(0.92f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = statusCode,
                            color = SageGreenBright,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            color = TacticalGoldText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = SageGreenBright,
                        trackColor = TacticalSurfaceLight
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = statusText,
                        color = TacticalTextPrimary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "GRID: 48.019° N  37.802° E • AES-256 ROOM DB",
                        color = TacticalTextMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Skip Button
                Button(
                    onClick = onInitializationComplete,
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(38.dp)
                        .testTag("skip_splash_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SageGreenPrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Пропустить (6 сек)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // BOTTOM CREDITS & VERSION (MANDATORY REQUIREMENT)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Разработчик: Васев Алексей Евгеньевич",
                    color = SageGreenBright,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Версия программы: v3.1.6 PRO (Tactical Edition)",
                    color = TacticalTextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun WaveformTelemetry(phase: Float) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .height(26.dp)
    ) {
        val barCount = 28
        val spacing = size.width / barCount
        val barWidth = spacing * 0.55f

        for (i in 0 until barCount) {
            val norm = i.toFloat() / barCount.toFloat()
            val amp = (sin(norm * 12f + phase) * 0.5f + 0.5f) * 0.7f + 0.3f
            val barHeight = size.height * amp

            val color = if (i % 5 == 0) Color(0xFFE5C468) else Color(0xFF8EBF9F)

            drawRoundRect(
                color = color,
                topLeft = Offset(i * spacing, (size.height - barHeight) / 2f),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }
    }
}

@Composable
private fun RadarSweepCanvas(sweepAngle: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.width / 2

        // Concentric radar circles
        drawCircle(
            color = Color(0xFF264C35),
            radius = radius * 0.85f,
            style = Stroke(width = 1.5.dp.toPx())
        )
        drawCircle(
            color = Color(0xFF1E3D2A),
            radius = radius * 0.6f,
            style = Stroke(width = 1.dp.toPx())
        )
        drawCircle(
            color = Color(0xFF1B3223),
            radius = radius * 0.35f,
            style = Stroke(width = 1.dp.toPx())
        )

        // Crosshairs
        drawLine(
            color = Color(0xFF2E593D),
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = Color(0xFF2E593D),
            start = Offset(size.width / 2, 0f),
            end = Offset(size.width / 2, size.height),
            strokeWidth = 1.dp.toPx()
        )

        // Tactical Target Blips (e.g. Warehouses and firing positions)
        drawCircle(
            color = Color(0xFFE5C468),
            radius = 4.dp.toPx(),
            center = Offset(center.x + radius * 0.45f, center.y - radius * 0.35f)
        )
        drawCircle(
            color = Color(0xFFA2CEB5),
            radius = 3.5.dp.toPx(),
            center = Offset(center.x - radius * 0.45f, center.y + radius * 0.3f)
        )
        drawCircle(
            color = Color(0xFFE26D5C),
            radius = 3.5.dp.toPx(),
            center = Offset(center.x + radius * 0.25f, center.y + radius * 0.6f)
        )

        // Radar Sweep Beam
        rotate(degrees = sweepAngle, pivot = center) {
            val sweepBrush = Brush.sweepGradient(
                0.0f to Color(0x00A2CEB5),
                0.8f to Color(0x15A2CEB5),
                1.0f to Color(0x95A2CEB5),
                center = center
            )
            drawCircle(
                brush = sweepBrush,
                radius = radius
            )
            drawLine(
                color = Color(0xFFA2CEB5),
                start = center,
                end = Offset(size.width, center.y),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}
