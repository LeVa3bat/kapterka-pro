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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Warehouse
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.ui.theme.LocalTacticalColors
import com.example.ui.theme.SageGreenBright
import com.example.ui.theme.TacticalTextMuted
import com.example.ui.theme.TacticalTextPrimary
import com.example.ui.theme.TacticalTextSecondary
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Bright, vibrant modern 3D animated launch screen (~4.2 s, tap to skip):
 * floating 3D isometric glass warehouse cube inside dual tilted luminous orbits,
 * animated volumetric aurora glow, kinetic letter-by-letter logo reveal,
 * and 4 perspective-animated 3D glass feature cards.
 */
@Composable
fun SplashScreen(
    onInitializationComplete: () -> Unit
) {
    val isDark = LocalTacticalColors.current.isDark
    val title = "Каптёрка"
    val cubeReveal = remember { Animatable(0f) }
    val letters = remember { List(title.length) { Animatable(0f) } }
    val badgeReveal = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }
    val progress = remember { Animatable(0f) }

    val features = remember {
        listOf(
            SplashFeature(
                icon = Icons.Rounded.Warehouse,
                title = "Точные остатки",
                subtitle = "по складам и отдельно по каждой единице",
                gradStart = Color(0xFF10B981),
                gradEnd = Color(0xFF059669)
            ),
            SplashFeature(
                icon = Icons.Rounded.SwapHoriz,
                title = "Операции в 1 клик",
                subtitle = "приход, выдача, возврат и списание в журнале",
                gradStart = Color(0xFFF59E0B),
                gradEnd = Color(0xFFD97706)
            ),
            SplashFeature(
                icon = Icons.Rounded.Sync,
                title = "Общий склад и QR",
                subtitle = "синхронизация телефонов и работа офлайн",
                gradStart = Color(0xFF06B6D4),
                gradEnd = Color(0xFF0891B2)
            ),
            SplashFeature(
                icon = Icons.Rounded.Description,
                title = "Готовые бланки Excel",
                subtitle = "ведомости и акты формата А4 за секунду",
                gradStart = Color(0xFF8B5CF6),
                gradEnd = Color(0xFF6D28D9)
            )
        )
    }
    val featureAnims = remember { List(4) { Animatable(0f) } }
    var finished by remember { mutableStateOf(false) }
    val finish = {
        if (!finished) {
            finished = true
            onInitializationComplete()
        }
    }

    val ambient = rememberInfiniteTransition(label = "splash3d")
    val phase by ambient.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing)),
        label = "phase"
    )
    val orbitAngle by ambient.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3400, easing = LinearEasing)),
        label = "orbit"
    )
    val pulse by ambient.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    LaunchedEffect(Unit) {
        launch { progress.animateTo(1f, tween(4200, easing = LinearEasing)) }
        cubeReveal.animateTo(1f, spring(dampingRatio = 0.58f, stiffness = Spring.StiffnessLow))
        letters.mapIndexed { i, anim ->
            async {
                delay(i * 48L)
                anim.animateTo(1f, spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow))
            }
        }.awaitAll()
        launch { badgeReveal.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium)) }
        subtitleAlpha.animateTo(1f, tween(280))
        featureAnims.forEach { anim ->
            delay(200)
            launch { anim.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)) }
        }
        delay(1_250)
        finish()
    }

    val bgBrush = if (isDark) {
        Brush.verticalGradient(
            listOf(Color(0xFF061513), Color(0xFF091B18), Color(0xFF071014))
        )
    } else {
        Brush.verticalGradient(
            listOf(Color(0xFFE7F8F1), Color(0xFFF2FBF8), Color(0xFFE8F4F8))
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { if (progress.value > 0.20f) finish() }
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        // Vivid animated aurora light spheres + subtle 3D horizon grid
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val rad = w * 0.72f
            fun aura(color: Color, alpha: Float, cx: Float, cy: Float, r: Float = rad) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(color.copy(alpha = alpha), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = r
                    ),
                    radius = r,
                    center = Offset(cx, cy)
                )
            }
            val boost = if (isDark) 1f else 0.78f
            aura(Color(0xFF10B981), 0.34f * boost, w * (0.22f + 0.08f * sin(phase)), h * (0.20f + 0.04f * cos(phase)))
            aura(Color(0xFF22D3EE), 0.28f * boost, w * (0.82f + 0.07f * cos(phase)), h * (0.34f + 0.05f * sin(phase)))
            aura(Color(0xFFF59E0B), 0.20f * boost, w * (0.50f + 0.09f * sin(phase + 1.8f)), h * (0.84f + 0.04f * cos(phase)), w * 0.65f)
            aura(Color(0xFF8B5CF6), 0.16f * boost, w * (0.14f + 0.05f * cos(phase + 2.4f)), h * (0.68f + 0.04f * sin(phase)), w * 0.55f)

            // Subtle 3D perspective floor lines at bottom
            val gridColor = if (isDark) Color(0xFF34D399).copy(alpha = 0.09f) else Color(0xFF0D9488).copy(alpha = 0.08f)
            val horizonY = h * 0.78f
            for (i in 0..4) {
                val t = i / 4f
                val y = horizonY + (h - horizonY) * t * t
                drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1.2f)
            }
            for (i in -3..3) {
                val topX = w * 0.5f + i * w * 0.09f
                val botX = w * 0.5f + i * w * 0.28f
                drawLine(gridColor, Offset(topX, horizonY), Offset(botX, h), strokeWidth = 1.1f)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 3D ANIMATED ISOMETRIC WAREHOUSE CUBE HERO STAGE
            Box(
                modifier = Modifier
                    .size(164.dp)
                    .graphicsLayer {
                        val v = cubeReveal.value
                        alpha = v.coerceIn(0f, 1f)
                        val sc = 0.45f + 0.55f * v
                        scaleX = sc
                        scaleY = sc
                        rotationX = (1f - v) * 26f
                        cameraDistance = 14f * density
                    },
                contentAlignment = Alignment.Center
            ) {
                Splash3DHeroCubeCanvas(
                    phase = phase,
                    orbitAngle = orbitAngle,
                    pulse = pulse,
                    isDark = isDark,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Kinetic Title + 3D Extruded Gold/Emerald PRO Pill
            Row(verticalAlignment = Alignment.CenterVertically) {
                title.forEachIndexed { i, ch ->
                    val a = letters[i].value
                    Text(
                        text = ch.toString(),
                        color = TacticalTextPrimary,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.8).sp,
                        modifier = Modifier.graphicsLayer {
                            alpha = a.coerceIn(0f, 1f)
                            translationY = (1f - a) * 26.dp.toPx()
                            scaleX = 0.85f + 0.15f * a
                            scaleY = 0.85f + 0.15f * a
                        }
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            val b = badgeReveal.value
                            alpha = b.coerceIn(0f, 1f)
                            scaleX = 0.65f + 0.35f * b
                            scaleY = 0.65f + 0.35f * b
                        }
                        .shadow(10.dp, RoundedCornerShape(10.dp), spotColor = Color(0xFFF59E0B))
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFEAB308))))
                        .border(1.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "PRO",
                        color = Color(0xFF1C1405),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.6.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Умный 3D-учёт складов, инструмента и ТМЦ",
                color = TacticalTextSecondary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { alpha = subtitleAlpha.value }
            )

            Spacer(modifier = Modifier.height(22.dp))

            // 4 Bright 3D Glass Feature Cards
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                features.forEachIndexed { i, f ->
                    val a = featureAnims[i].value
                    val cardShape = RoundedCornerShape(18.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                alpha = a.coerceIn(0f, 1f)
                                translationY = (1f - a) * 28.dp.toPx()
                                rotationX = (1f - a) * 14f
                                cameraDistance = 14f * density
                            }
                            .shadow(
                                elevation = if (isDark) 8.dp else 5.dp,
                                shape = cardShape,
                                ambientColor = f.gradStart.copy(alpha = 0.25f),
                                spotColor = f.gradStart.copy(alpha = 0.30f)
                            )
                            .clip(cardShape)
                            .background(
                                Brush.verticalGradient(
                                    if (isDark) listOf(Color(0xFF172B27).copy(alpha = 0.88f), Color(0xFF0E1B19).copy(alpha = 0.92f))
                                    else listOf(Color.White.copy(alpha = 0.94f), Color(0xFFF3FAF7).copy(alpha = 0.92f))
                                )
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    listOf(
                                        f.gradStart.copy(alpha = if (isDark) 0.50f else 0.42f),
                                        Color.White.copy(alpha = 0.08f)
                                    )
                                ),
                                shape = cardShape
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .shadow(6.dp, RoundedCornerShape(12.dp), spotColor = f.gradStart)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(listOf(f.gradStart, f.gradEnd)))
                                .border(0.9.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = f.icon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = f.title,
                                color = TacticalTextPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = f.subtitle,
                                color = TacticalTextMuted,
                                fontSize = 11.5.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }

        // Bottom 3D Progress Bar + version + tap hint
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 36.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isDark) Color.White.copy(alpha = 0.10f) else Color(0xFF0D9488).copy(alpha = 0.14f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.value.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF10B981), Color(0xFF22D3EE), Color(0xFFF59E0B))
                            )
                        )
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "v${BuildConfig.VERSION_NAME}",
                    color = SageGreenBright,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("•", color = TacticalTextMuted, fontSize = 11.sp)
                Text(
                    text = "Нажмите, чтобы пропустить",
                    color = TacticalTextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun Splash3DHeroCubeCanvas(
    phase: Float,
    orbitAngle: Float,
    pulse: Float,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cx = size.width * 0.5f
        val cy = size.height * 0.50f
        val bobY = sin(phase) * 9f
        val cubeY = cy + bobY
        val r = size.minDimension * 0.255f
        val dx = r * 0.866f
        val dy = r * 0.50f

        // 1. Soft vibrant halo behind the 3D cube
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    Color(0xFF34D399).copy(alpha = (if (isDark) 0.42f else 0.30f) * pulse),
                    Color(0xFF22D3EE).copy(alpha = 0.15f * pulse),
                    Color.Transparent
                ),
                center = Offset(cx, cubeY),
                radius = size.minDimension * 0.50f
            ),
            radius = size.minDimension * 0.50f,
            center = Offset(cx, cubeY)
        )

        // 2. Ground shadow ellipse (scales subtly as cube floats up/down)
        val shadowScale = 1f - (bobY / 50f)
        drawOval(
            color = if (isDark) Color.Black.copy(alpha = 0.42f) else Color(0xFF065F46).copy(alpha = 0.16f),
            topLeft = Offset(cx - dx * 0.88f * shadowScale, size.height * 0.85f),
            size = Size(dx * 1.76f * shadowScale, r * 0.32f * shadowScale)
        )

        // 3. Tilted luminous 3D orbital rings
        val orbitW = size.width * 0.86f
        val orbitH = size.height * 0.36f
        rotate(degrees = -24f, pivot = Offset(cx, cy)) {
            drawOval(
                brush = Brush.sweepGradient(
                    listOf(Color.Transparent, Color(0xFF22D3EE), Color(0xFF34D399), Color.Transparent),
                    center = Offset(cx, cy)
                ),
                topLeft = Offset(cx - orbitW / 2f, cy - orbitH / 2f),
                size = Size(orbitW, orbitH),
                style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
            )
            val rad1 = (orbitAngle * PI / 180.0)
            val orbX1 = cx + (orbitW / 2f) * cos(rad1).toFloat()
            val orbY1 = cy + (orbitH / 2f) * sin(rad1).toFloat()
            drawCircle(Color(0xFF67E8F9), radius = 5.5.dp.toPx(), center = Offset(orbX1, orbY1))
            drawCircle(Color.White, radius = 2.6.dp.toPx(), center = Offset(orbX1, orbY1))
        }

        rotate(degrees = 24f, pivot = Offset(cx, cy)) {
            drawOval(
                brush = Brush.sweepGradient(
                    listOf(Color.Transparent, Color(0xFFF59E0B), Color(0xFFFDE68A), Color.Transparent),
                    center = Offset(cx, cy)
                ),
                topLeft = Offset(cx - orbitW / 2f, cy - orbitH / 2f),
                size = Size(orbitW, orbitH),
                style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
            )
            val rad2 = ((-orbitAngle + 140f) * PI / 180.0)
            val orbX2 = cx + (orbitW / 2f) * cos(rad2).toFloat()
            val orbY2 = cy + (orbitH / 2f) * sin(rad2).toFloat()
            drawCircle(Color(0xFFFBBF24), radius = 4.6.dp.toPx(), center = Offset(orbX2, orbY2))
            drawCircle(Color.White, radius = 2.1.dp.toPx(), center = Offset(orbX2, orbY2))
        }

        // 4. Centerpiece 3D Isometric Crystal Warehouse Cube
        val top = Offset(cx, cubeY - r)
        val rightTop = Offset(cx + dx, cubeY - dy)
        val center = Offset(cx, cubeY)
        val leftTop = Offset(cx - dx, cubeY - dy)
        val leftBottom = Offset(cx - dx, cubeY + dy)
        val bottom = Offset(cx, cubeY + r)
        val rightBottom = Offset(cx + dx, cubeY + dy)

        val leftFace = Path().apply {
            moveTo(leftTop.x, leftTop.y)
            lineTo(center.x, center.y)
            lineTo(bottom.x, bottom.y)
            lineTo(leftBottom.x, leftBottom.y)
            close()
        }
        drawPath(
            path = leftFace,
            brush = Brush.linearGradient(
                listOf(Color(0xFF34D399), Color(0xFF059669), Color(0xFF065F46)),
                start = leftTop,
                end = bottom
            )
        )

        val rightFace = Path().apply {
            moveTo(center.x, center.y)
            lineTo(rightTop.x, rightTop.y)
            lineTo(rightBottom.x, rightBottom.y)
            lineTo(bottom.x, bottom.y)
            close()
        }
        drawPath(
            path = rightFace,
            brush = Brush.linearGradient(
                listOf(Color(0xFF22D3EE), Color(0xFF0891B2), Color(0xFF155E75)),
                start = rightTop,
                end = bottom
            )
        )

        val topFace = Path().apply {
            moveTo(top.x, top.y)
            lineTo(rightTop.x, rightTop.y)
            lineTo(center.x, center.y)
            lineTo(leftTop.x, leftTop.y)
            close()
        }
        drawPath(
            path = topFace,
            brush = Brush.linearGradient(
                listOf(Color(0xFFECFDF5), Color(0xFF6EE7B7), Color(0xFF10B981)),
                start = top,
                end = center
            )
        )

        // Bright golden packing seal across the top lid
        drawLine(
            color = Color(0xFFFDE68A),
            start = Offset((top.x + leftTop.x) / 2f, (top.y + leftTop.y) / 2f),
            end = Offset((rightTop.x + center.x) / 2f, (rightTop.y + center.y) / 2f),
            strokeWidth = 4.2.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Inner floating bright bar detail on right side (looks like modern 3D barcode/label)
        drawLine(
            color = Color.White.copy(alpha = 0.65f),
            start = Offset(cx + dx * 0.25f, cubeY + dy * 0.42f),
            end = Offset(cx + dx * 0.72f, cubeY + dy * 0.16f),
            strokeWidth = 2.6.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Crisp specular wireframe bevel
        val bevelStroke = Stroke(width = 1.9.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawPath(topFace, color = Color.White.copy(alpha = 0.88f), style = bevelStroke)
        drawPath(leftFace, color = Color.White.copy(alpha = 0.42f), style = bevelStroke)
        drawPath(rightFace, color = Color.White.copy(alpha = 0.36f), style = bevelStroke)
    }
}

private data class SplashFeature(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val gradStart: Color,
    val gradEnd: Color
)
