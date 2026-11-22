package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalTacticalColors
import com.example.ui.theme.SageGreenBright
import com.example.ui.theme.SageGreenPrimary
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalBorderSubtle
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalSurfaceElevated
import com.example.ui.theme.TacticalSurfaceLight
import com.example.ui.theme.TacticalTextMuted
import com.example.ui.theme.TacticalTextPrimary
import com.example.ui.theme.TacticalTextSecondary
import kotlin.math.cos
import kotlin.math.sin

/**
 * Formats positive stock quantities strictly separated by unit (never summing pieces + liters!).
 * Example: [( "шт.", 18 ), ( "л.", 40 ), ( "компл.", 5 )] -> "40 л. · 18 шт. · 5 компл."
 */
fun formatUnitBreakdown(
    unitQuantities: List<Pair<String, Int>>,
    maxUnits: Int = 3,
    emptyLabel: String = "Остатков пока нет"
): String {
    val grouped = linkedMapOf<String, Int>()
    for ((rawUnit, qty) in unitQuantities) {
        if (qty <= 0) continue
        val unit = rawUnit.trim().ifBlank { "шт." }
        grouped[unit] = (grouped[unit] ?: 0) + qty
    }
    if (grouped.isEmpty()) return emptyLabel
    val sorted = grouped.entries.sortedByDescending { it.value }
    val shown = sorted.take(maxUnits).joinToString(" · ") { "${it.value} ${it.key}" }
    val extra = sorted.size - maxUnits
    return if (extra > 0) "$shown · +$extra ед." else shown
}

/**
 * Specular vertical glass rim brush for 3D cards & modals (bright top edge fading down).
 */
@Composable
fun glassRimBrush(accent: Color? = null): Brush {
    val isDark = LocalTacticalColors.current.isDark
    return if (accent != null) {
        Brush.verticalGradient(
            listOf(
                accent.copy(alpha = if (isDark) 0.65f else 0.50f),
                accent.copy(alpha = if (isDark) 0.18f else 0.16f)
            )
        )
    } else if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.16f),
                Color.White.copy(alpha = 0.04f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.95f),
                Color(0xFFC6DCD4)
            )
        )
    }
}

/**
 * Tactile 3D glass card with subtle plinth depth shadow and specular top rim.
 */
@Composable
fun Card3DSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    accentBorder: Color? = null,
    elevated: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = LocalTacticalColors.current.isDark
    val shape = RoundedCornerShape(cornerRadius)
    val baseFill = if (elevated) TacticalSurfaceElevated else TacticalSurface
    val faceBrush = if (isDark) {
        Brush.verticalGradient(
            listOf(
                if (elevated) Color(0xFF213632) else Color(0xFF152320),
                baseFill
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFFFFFFFF),
                Color(0xFFF7FBF9)
            )
        )
    }
    val shadowDp = if (isDark) (if (elevated) 10.dp else 5.dp) else (if (elevated) 8.dp else 4.dp)

    Column(
        modifier = modifier
            .shadow(
                elevation = shadowDp,
                shape = shape,
                ambientColor = if (isDark) Color.Black.copy(alpha = 0.55f) else Color(0xFF0A4D3C).copy(alpha = 0.14f),
                spotColor = if (isDark) Color.Black.copy(alpha = 0.65f) else Color(0xFF0A4D3C).copy(alpha = 0.16f)
            )
            .clip(shape)
            .background(faceBrush)
            .border(1.dp, glassRimBrush(accentBorder), shape),
        content = content
    )
}

/**
 * Volumetric 3D Hero Stage Card with extruded bottom plinth, diagonal glass sheen,
 * live custom Compose Canvas 3D Isometric Warehouse Cube, unit-safe metrics, and 3 glass pills.
 */
@Composable
fun Hero3DStageCard(
    eyebrow: String,
    positionsCount: Int,
    unitBreakdown: String,
    warehousesCount: Int,
    todayOpsCount: Int,
    pendingRequestsCount: Int,
    warehousesLabel: String = "Складов",
    modifier: Modifier = Modifier
) {
    val animatedPositions by animateIntAsState(
        targetValue = positionsCount,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "heroPositions"
    )
    val shape = RoundedCornerShape(24.dp)
    val plinthColor = Color(0xFF06362A)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        // 3D Extruded darker base lip
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(top = 5.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = shape,
                    ambientColor = Color(0xFF059669).copy(alpha = 0.35f),
                    spotColor = Color(0xFF064E3B).copy(alpha = 0.55f)
                )
                .clip(shape)
                .background(plinthColor)
        )

        // Elevated glossy 3D face
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 5.dp)
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0B5D46),
                            Color(0xFF0D7C65),
                            Color(0xFF0A4F60)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(900f, 650f)
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.34f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    shape = shape
                )
        ) {
            // Subtle glossy radial sheen overlay
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF6EE7B7).copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * 0.12f),
                        radius = size.width * 0.55f
                    ),
                    radius = size.width * 0.55f,
                    center = Offset(size.width * 0.85f, size.height * 0.12f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF22D3EE).copy(alpha = 0.14f), Color.Transparent),
                        center = Offset(size.width * 0.15f, size.height * 0.95f),
                        radius = size.width * 0.45f
                    ),
                    radius = size.width * 0.45f,
                    center = Offset(size.width * 0.15f, size.height * 0.95f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = eyebrow.uppercase(),
                            color = Color(0xFFA7F3D0),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$animatedPositions",
                                color = Color.White,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.8).sp,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "поз. в наличии",
                                color = Color.White.copy(alpha = 0.88f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier.padding(bottom = 5.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.22f))
                                .border(0.75.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = unitBreakdown,
                                color = Color(0xFFD1FAE5),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Live floating 3D Isometric Glass Warehouse Cube
                    FloatingIsometric3DCube(modifier = Modifier.size(76.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3 Frosted Glass Mini-Pills (no mixed units!)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HeroGlassMiniPill(
                        value = warehousesCount.toString(),
                        label = warehousesLabel,
                        modifier = Modifier.weight(1f)
                    )
                    HeroGlassMiniPill(
                        value = todayOpsCount.toString(),
                        label = "Сегодня",
                        modifier = Modifier.weight(1f)
                    )
                    HeroGlassMiniPill(
                        value = pendingRequestsCount.toString(),
                        label = "Заявок",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroGlassMiniPill(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(13.dp))
            .background(Color.White.copy(alpha = 0.13f))
            .border(0.8.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(13.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.80f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Custom Jetpack Compose 3D Isometric Translucent Warehouse Cube with floating animation.
 */
@Composable
fun FloatingIsometric3DCube(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "cube3d")
    val floatPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cubeFloat"
    )
    val bobY = sin(floatPhase) * 4.5f
    val glowAlpha = 0.28f + 0.12f * ((cos(floatPhase) + 1f) / 2f)

    Canvas(modifier = modifier) {
        val cx = size.width * 0.5f
        val cy = size.height * 0.50f + bobY
        val r = size.minDimension * 0.34f
        val dx = r * 0.866f
        val dy = r * 0.50f

        // Ground shadow ellipse
        drawOval(
            color = Color.Black.copy(alpha = 0.26f),
            topLeft = Offset(cx - dx * 0.72f, size.height * 0.84f),
            size = androidx.compose.ui.geometry.Size(dx * 1.44f, r * 0.36f)
        )

        // Ambient emerald halo behind cube
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF6EE7B7).copy(alpha = glowAlpha), Color.Transparent),
                center = Offset(cx, cy),
                radius = r * 1.45f
            ),
            radius = r * 1.45f,
            center = Offset(cx, cy)
        )

        // Cube vertices
        val top = Offset(cx, cy - r)
        val rightTop = Offset(cx + dx, cy - dy)
        val center = Offset(cx, cy)
        val leftTop = Offset(cx - dx, cy - dy)
        val leftBottom = Offset(cx - dx, cy + dy)
        val bottom = Offset(cx, cy + r)
        val rightBottom = Offset(cx + dx, cy + dy)

        // Left shaded face
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
                colors = listOf(Color(0xFF34D399).copy(alpha = 0.82f), Color(0xFF047857).copy(alpha = 0.92f)),
                start = leftTop,
                end = bottom
            )
        )

        // Right deeper face
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
                colors = listOf(Color(0xFF22D3EE).copy(alpha = 0.65f), Color(0xFF0E7490).copy(alpha = 0.92f)),
                start = rightTop,
                end = bottom
            )
        )

        // Top glossy luminous face
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
                colors = listOf(Color(0xFFD1FAE5), Color(0xFF34D399)),
                start = top,
                end = center
            )
        )

        // Tape/seal ribbon across top face for warehouse crate character
        drawLine(
            color = Color.White.copy(alpha = 0.72f),
            start = Offset((top.x + leftTop.x) / 2f, (top.y + leftTop.y) / 2f),
            end = Offset((rightTop.x + center.x) / 2f, (rightTop.y + center.y) / 2f),
            strokeWidth = 3.2f,
            cap = StrokeCap.Round
        )

        // Crisp specular edges
        val rimStroke = Stroke(width = 1.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawPath(topFace, color = Color.White.copy(alpha = 0.75f), style = rimStroke)
        drawPath(leftFace, color = Color.White.copy(alpha = 0.35f), style = rimStroke)
        drawPath(rightFace, color = Color.White.copy(alpha = 0.30f), style = rimStroke)

        // Floating specular spark dots
        drawCircle(
            color = Color(0xFFFDE68A).copy(alpha = 0.9f),
            radius = 2.6f,
            center = Offset(cx + dx * 0.92f, cy - r * 0.85f - bobY * 0.5f)
        )
        drawCircle(
            color = Color(0xFFA7F3D0).copy(alpha = 0.85f),
            radius = 2.1f,
            center = Offset(cx - dx * 0.95f, cy - r * 0.45f + bobY * 0.4f)
        )
    }
}

/**
 * True extruded 3D tactile action tile matching the approved prototype:
 * darker bottom extrusion lip, vibrant 160° gradient face, specular white top rim,
 * frosted 3D icon medallion, and real spring press-down physics.
 */
@Composable
fun Volumetric3DActionTile(
    title: String,
    icon: ImageVector,
    gradientStart: Color,
    gradientEnd: Color,
    extrusionColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val density = LocalDensity.current
    val maxPressPx = with(density) { 4.dp.toPx() }
    val pressFraction by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessHigh),
        label = "tilePressY"
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessHigh),
        label = "tileScale"
    )
    val shape = RoundedCornerShape(18.dp)

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .height(86.dp)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
    ) {
        // Darker 3D bottom extrusion lip
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 5.dp)
                .shadow(
                    elevation = if (pressed) 2.dp else 8.dp,
                    shape = shape,
                    ambientColor = extrusionColor.copy(alpha = 0.55f),
                    spotColor = extrusionColor.copy(alpha = 0.65f)
                )
                .clip(shape)
                .background(extrusionColor)
        )

        // Elevated glossy gradient face that depresses smoothly via GPU translationY
        // (never animates padding directly so spring overshoot can never produce negative Dp).
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 5.dp)
                .graphicsLayer {
                    translationY = (pressFraction * maxPressPx).coerceIn(0f, maxPressPx)
                }
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(gradientStart, gradientEnd),
                        start = Offset(0f, 0f),
                        end = Offset(240f, 280f)
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.42f),
                            Color.White.copy(alpha = 0.06f)
                        )
                    ),
                    shape = shape
                )
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.22f))
                    .border(
                        width = 0.8.dp,
                        color = Color.White.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Full-width or inline tactile 3D extruded primary button for modals, CTAs, and header actions.
 * Uses matchParentSize() for the 3D bottom lip and wrap-content width for the face Row with
 * propagateMinConstraints = true so it works identically both inline ("+ Заявка") and full-width.
 */
@Composable
fun Volumetric3DButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 50.dp,
    cornerRadius: Dp = 16.dp,
    gradientStart: Color = Color(0xFF10B981),
    gradientEnd: Color = Color(0xFF059669),
    extrusionColor: Color = Color(0xFF065F46),
    contentColor: Color = Color.White
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val density = LocalDensity.current
    val maxPressPx = with(density) { 3.dp.toPx() }
    val pressFraction by animateFloatAsState(
        targetValue = if (pressed && enabled) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessHigh),
        label = "btn3dY"
    )
    val shape = RoundedCornerShape(cornerRadius)
    val effStart = if (enabled) gradientStart else TacticalSurfaceLight
    val effEnd = if (enabled) gradientEnd else TacticalSurfaceLight
    val effLip = if (enabled) extrusionColor else TacticalBorder
    val effText = if (enabled) contentColor else TacticalTextMuted

    Box(
        modifier = modifier
            .height(height)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        propagateMinConstraints = true
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(top = 4.dp)
                .clip(shape)
                .background(effLip)
        )
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(bottom = 4.dp)
                .graphicsLayer {
                    translationY = (pressFraction * maxPressPx).coerceIn(0f, maxPressPx)
                }
                .clip(shape)
                .background(Brush.linearGradient(listOf(effStart, effEnd)))
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = if (enabled) 0.35f else 0.08f),
                            Color.White.copy(alpha = 0.04f)
                        )
                    ),
                    shape = shape
                )
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = effText,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                color = effText,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Glossy 3D circular/squircle icon orb button for headers and dialogs.
 */
@Composable
fun IconOrb3DButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    val isDark = LocalTacticalColors.current.isDark
    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = if (isDark) 5.dp else 3.dp,
                shape = CircleShape,
                ambientColor = Color.Black.copy(alpha = 0.35f)
            )
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(
                    if (isDark) listOf(Color(0xFF1E302C), Color(0xFF111C1A))
                    else listOf(Color(0xFFFFFFFF), Color(0xFFE7F0EC))
                )
            )
            .border(1.dp, glassRimBrush(), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

/**
 * 3D Spring Pop-in wrapper for ALL popup dialogs: smoothly animates scale, vertical lift,
 * perspective tilt (rotationX) and alpha with glass specular border.
 */
@Composable
fun Animated3DDialogContainer(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 26.dp,
    accentBorder: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.72f,
                stiffness = 420f
            )
        )
    }
    val p = progress.value
    val isDark = LocalTacticalColors.current.isDark
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .graphicsLayer {
                alpha = p.coerceIn(0f, 1f)
                val sc = 0.90f + 0.10f * p
                scaleX = sc
                scaleY = sc
                translationY = (1f - p) * 38f
                rotationX = (1f - p) * 7f
                cameraDistance = 14f * density
            }
            .shadow(
                elevation = 20.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = if (isDark) 0.72f else 0.22f),
                spotColor = (accentBorder ?: SageGreenPrimary).copy(alpha = if (isDark) 0.32f else 0.18f)
            )
            .clip(shape)
            .background(TacticalSurface)
            .border(1.1.dp, glassRimBrush(accentBorder), shape),
        content = content
    )
}

/**
 * Sleek 3D pill filter chip for categories & tabs across Dashboard, History, Catalog.
 */
@Composable
fun Filter3DPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                if (selected) {
                    Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF0D9488)))
                } else {
                    Brush.verticalGradient(listOf(TacticalSurfaceElevated, TacticalSurface))
                }
            )
            .border(
                width = 1.dp,
                brush = if (selected) {
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.40f), Color(0xFF059669)))
                } else {
                    glassRimBrush()
                },
                shape = shape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick
            )
            .padding(horizontal = 11.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else TacticalTextSecondary,
            fontSize = 11.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Pulsing live status dot for online sync / active indicator.
 */
@Composable
fun PulsingStatusDot(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 8.dp
) {
    val transition = rememberInfiniteTransition(label = "statusDot")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "dotAlpha"
    )
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha))
    )
}
