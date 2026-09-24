package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Warehouse
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WarehousePoint
import com.example.ui.theme.SageGreenBright
import com.example.ui.theme.SageGreenPrimary
import com.example.ui.theme.TacticalBorderSubtle
import com.example.ui.theme.TacticalGoldText
import com.example.ui.theme.TacticalRedText
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalSurfaceLight
import com.example.ui.theme.TacticalTealText
import com.example.ui.theme.TacticalTextMuted
import com.example.ui.theme.TacticalTextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Long-press actions for a warehouse point: move up / down, edit, delete.
 * Tiles bounce in one after another; deleting needs a second tap and the
 * base warehouse cannot be deleted.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PointActionsSheet(
    point: WarehousePoint,
    stockUnits: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMove: (Int) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var armedDelete by remember { mutableStateOf(false) }
    val tiles = remember { List(4) { Animatable(0f) } }
    LaunchedEffect(Unit) {
        tiles.forEachIndexed { i, anim ->
            launch {
                delay(60L * i)
                anim.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow))
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = TacticalSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .navigationBarsPadding()
                .padding(bottom = 18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Brush.linearGradient(listOf(SageGreenPrimary, TacticalTealText))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (point.isBase) Icons.Rounded.Warehouse else Icons.Rounded.Inventory2,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(point.name, color = TacticalTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        (if (point.isBase) "Базовый склад • " else "") + "на остатке $stockUnits ед.",
                        color = if (point.isBase) TacticalGoldText else TacticalTextMuted,
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionTile(Icons.Rounded.ArrowUpward, "Выше", "поднять в списке", SageGreenBright, canMoveUp, tiles[0].value, Modifier.weight(1f)) { onMove(-1) }
                ActionTile(Icons.Rounded.ArrowDownward, "Ниже", "опустить в списке", SageGreenBright, canMoveDown, tiles[1].value, Modifier.weight(1f)) { onMove(1) }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionTile(Icons.Rounded.Edit, "Изменить", "название и описание", TacticalTealText, true, tiles[2].value, Modifier.weight(1f)) { onEdit() }
                ActionTile(
                    icon = Icons.Rounded.DeleteForever,
                    title = if (armedDelete) "Точно удалить?" else "Удалить",
                    subtitle = when {
                        point.isBase -> "базовый склад не удаляется"
                        armedDelete && stockUnits > 0 -> "на нём $stockUnits ед. — нажмите ещё раз"
                        armedDelete -> "нажмите ещё раз"
                        else -> "убрать точку"
                    },
                    tint = TacticalRedText,
                    enabled = !point.isBase,
                    appear = tiles[3].value,
                    modifier = Modifier.weight(1f),
                    highlighted = armedDelete
                ) {
                    if (armedDelete) onDelete() else armedDelete = true
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Подсказка: удерживайте карточку склада, чтобы открыть это меню.",
                color = TacticalTextMuted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ActionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    enabled: Boolean,
    appear: Float,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        if (highlighted) tint.copy(alpha = 0.18f) else TacticalSurfaceLight,
        label = "tileBg"
    )
    Column(
        modifier = modifier
            .graphicsLayer {
                alpha = appear.coerceIn(0f, 1f) * (if (enabled) 1f else 0.45f)
                val s = 0.7f + 0.3f * appear
                scaleX = s
                scaleY = s
                translationY = (1f - appear) * 40f
            }
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.dp, if (highlighted) tint else TacticalBorderSubtle, RoundedCornerShape(20.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(title, color = TacticalTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = TacticalTextMuted, fontSize = 11.sp, lineHeight = 14.sp)
    }
}
