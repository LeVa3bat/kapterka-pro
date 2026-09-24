package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.ui.theme.SageGreenBright
import com.example.ui.theme.SageGreenPrimary
import com.example.ui.theme.TacticalBg
import com.example.ui.theme.TacticalGoldText
import com.example.ui.theme.TacticalSurfaceLight
import com.example.ui.theme.TacticalTealText
import com.example.ui.theme.TacticalTextMuted
import com.example.ui.theme.TacticalTextPrimary
import com.example.ui.theme.TacticalTextSecondary
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * Short, modern launch screen (~1.9 s): logo pops in with a spring, a glowing
 * ring orbits it, the title appears letter by letter over a softly moving
 * gradient, and a thin progress line fills at the bottom.
 */
@Composable
fun SplashScreen(
    onInitializationComplete: () -> Unit
) {
    val title = "Каптёрка"
    val logoScale = remember { Animatable(0.4f) }
    val logoAlpha = remember { Animatable(0f) }
    val letters = remember { List(title.length) { Animatable(0f) } }
    val badgeAlpha = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }
    val progress = remember { Animatable(0f) }

    val ambient = rememberInfiniteTransition(label = "ambient")
    val ringAngle by ambient.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing)),
        label = "ring"
    )
    val drift by ambient.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "drift"
    )
    val glow by ambient.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )

    LaunchedEffect(Unit) {
        launch { progress.animateTo(1f, tween(1750, easing = FastOutSlowInEasing)) }
        launch { logoAlpha.animateTo(1f, tween(350)) }
        logoScale.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow))
        letters.mapIndexed { i, anim ->
            async {
                delay(i * 45L)
                anim.animateTo(1f, tween(260, easing = FastOutSlowInEasing))
            }
        }.awaitAll()
        launch { badgeAlpha.animateTo(1f, tween(220)) }
        subtitleAlpha.animateTo(1f, tween(260))
        progress.animateTo(1f, tween(120))
        delay(180)
        onInitializationComplete()
    }

    val primary = SageGreenPrimary
    val bright = SageGreenBright
    val teal = TacticalTealText
    val gold = TacticalGoldText

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TacticalBg)
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        // Softly drifting colour blobs in the background.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = w * 0.75f
            fun blob(color: Color, cx: Float, cy: Float) = drawCircle(
                brush = Brush.radialGradient(listOf(color.copy(alpha = 0.22f), Color.Transparent), Offset(cx, cy), r),
                radius = r,
                center = Offset(cx, cy)
            )
            blob(primary, w * (0.25f + 0.08f * sin(drift)), h * (0.28f + 0.05f * cos(drift)))
            blob(teal, w * (0.8f + 0.07f * cos(drift)), h * (0.62f + 0.06f * sin(drift)))
            blob(gold, w * (0.35f + 0.06f * sin(drift + 2f)), h * (0.92f + 0.03f * cos(drift)))
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Logo: rounded tile with an orbiting glow ring.
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .graphicsLayer {
                        scaleX = logoScale.value
                        scaleY = logoScale.value
                        alpha = logoAlpha.value
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 4.dp.toPx()
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(bright.copy(alpha = 0.28f * glow), Color.Transparent)
                        ),
                        radius = size.minDimension / 2f
                    )
                    rotate(ringAngle) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(Color.Transparent, teal, bright, Color.Transparent)
                            ),
                            startAngle = 0f,
                            sweepAngle = 300f,
                            useCenter = false,
                            topLeft = Offset(stroke * 2, stroke * 2),
                            size = androidx.compose.ui.geometry.Size(
                                size.width - stroke * 4,
                                size.height - stroke * 4
                            ),
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Brush.linearGradient(listOf(primary, teal))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(46.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Title, letter by letter.
            Row(verticalAlignment = Alignment.CenterVertically) {
                title.forEachIndexed { i, ch ->
                    val a = letters[i].value
                    Text(
                        text = ch.toString(),
                        color = TacticalTextPrimary,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.graphicsLayer {
                            alpha = a
                            translationY = (1f - a) * 24.dp.toPx()
                        }
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ПРО",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .graphicsLayer { alpha = badgeAlpha.value; scaleX = 0.8f + 0.2f * badgeAlpha.value; scaleY = scaleX }
                        .clip(RoundedCornerShape(9.dp))
                        .background(Brush.linearGradient(listOf(primary, teal)))
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Учёт склада и имущества подразделения",
                color = TacticalTextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.graphicsLayer { alpha = subtitleAlpha.value }
            )
        }

        // Bottom: thin progress line + version.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(TacticalSurfaceLight)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.value)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Brush.horizontalGradient(listOf(primary, bright)))
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Версия ${BuildConfig.VERSION_NAME}",
                color = TacticalTextMuted,
                fontSize = 11.sp
            )
        }
    }
}
