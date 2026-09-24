package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * "Done" mark: the badge pops in with a spring, a ring sweeps around it and
 * the check is drawn stroke by stroke, with a light haptic tick. Replays
 * whenever [key] changes.
 */
@Composable
fun AnimatedCheck(
    key: Any?,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    val pop = remember { Animatable(0.4f) }
    val ring = remember { Animatable(0f) }
    val tick = remember { Animatable(0f) }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(key) {
        pop.snapTo(0.4f)
        ring.snapTo(0f)
        tick.snapTo(0f)
        com.example.util.Haptics.success(context)
        launch { pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium)) }
        ring.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
        tick.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
    }

    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer { scaleX = pop.value; scaleY = pop.value }
    ) {
        val stroke = this.size.minDimension * 0.09f
        drawCircle(
            brush = Brush.radialGradient(listOf(color.copy(alpha = 0.35f), color.copy(alpha = 0.08f))),
            radius = this.size.minDimension / 2f
        )
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * ring.value,
            useCenter = false,
            topLeft = Offset(stroke / 2, stroke / 2),
            size = androidx.compose.ui.geometry.Size(this.size.width - stroke, this.size.height - stroke),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        val w = this.size.width
        val h = this.size.height
        val full = Path().apply {
            moveTo(w * 0.28f, h * 0.52f)
            lineTo(w * 0.44f, h * 0.67f)
            lineTo(w * 0.73f, h * 0.37f)
        }
        val measure = PathMeasure().apply { setPath(full, false) }
        val part = Path()
        measure.getSegment(0f, measure.length * tick.value, part, true)
        drawPath(part, color = color, style = Stroke(width = stroke * 1.2f, cap = StrokeCap.Round))
    }
}
