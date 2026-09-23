package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InventoryItem
import com.example.data.model.OperationRecord
import com.example.data.model.OperationType
import com.example.data.model.RequestStatus
import com.example.data.model.RequisitionRequest
import com.example.data.model.StockRecord
import com.example.data.model.UserProfile
import com.example.data.model.WarehousePoint
import com.example.universal.WarehouseProfileCatalog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val UniversalInk = Color(0xFF111827)
private val UniversalMuted = Color(0xFF6B7280)
private val UniversalBg = Color(0xFFF5F7FB)
private val UniversalSurface = Color.White
private val UniversalPrimary = Color(0xFF5B5CE2)
private val UniversalPrimarySoft = Color(0xFFEEEEFF)
private val UniversalGreen = Color(0xFF159A72)
private val UniversalGreenSoft = Color(0xFFE9F8F3)
private val UniversalOrange = Color(0xFFE88B22)
private val UniversalOrangeSoft = Color(0xFFFFF4E5)
private val UniversalRed = Color(0xFFD94C4C)
private val UniversalRedSoft = Color(0xFFFFEEEE)
private val UniversalBlue = Color(0xFF3977D8)
private val UniversalBlueSoft = Color(0xFFEAF2FF)

@Composable
fun UniversalDashboardScreen(
    userProfile: UserProfile?,
    warehouseProfileId: String?,
    points: List<WarehousePoint>,
    selectedPointId: String,
    catalogItems: List<InventoryItem>,
    stockRecords: List<StockRecord>,
    operations: List<OperationRecord>,
    requisitions: List<RequisitionRequest>,
    onSelectPoint: (String) -> Unit,
    onIncomeClick: () -> Unit,
    onTransferClick: () -> Unit,
    onIssueClick: () -> Unit,
    onWriteOffClick: () -> Unit,
    onAddItemClick: () -> Unit,
    onOpenCatalog: () -> Unit,
    onOpenOperations: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val profileTemplate = WarehouseProfileCatalog.find(warehouseProfileId)
    val vocab = profileTemplate.operations
    val selectedPoint = remember(points, selectedPointId) {
        points.firstOrNull { it.id == selectedPointId } ?: points.firstOrNull()
    }
    val selectedPointStocks = remember(stockRecords, selectedPoint?.id) {
        val pointId = selectedPoint?.id
        if (pointId == null) emptyList() else stockRecords.filter { it.pointId == pointId }
    }
    val activePositions = remember(selectedPointStocks) {
        selectedPointStocks.filter { it.quantity > 0 }.map { it.itemId }.distinct().size
    }
    val catalogById = remember(catalogItems) {
        catalogItems.associateBy { it.id }
    }
    val selectedPointBalances = remember(selectedPointStocks, catalogById) {
        selectedPointStocks
            .filter { it.quantity != 0 }
            .mapNotNull { stock ->
                catalogById[stock.itemId]?.let { item -> item to stock }
            }
            .sortedWith(
                compareBy<Pair<InventoryItem, StockRecord>>(
                    { it.first.serviceCategory },
                    { it.first.subType },
                    { it.first.name }
                )
            )
    }
    val selectedPointOperations = remember(operations, selectedPoint?.name) {
        val pointName = selectedPoint?.name.orEmpty()
        if (pointName.isBlank()) {
            operations
        } else {
            operations.filter {
                it.fromPointName == pointName || it.toPointName == pointName
            }
        }
    }
    val todayStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val todayOps = remember(selectedPointOperations, todayStart) {
        selectedPointOperations.count { it.timestamp >= todayStart }
    }
    val recentOperations = remember(selectedPointOperations) {
        selectedPointOperations.sortedByDescending { it.timestamp }.take(2)
    }
    val formatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(UniversalBg)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Сегодня",
                            color = UniversalInk,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = buildString {
                                append(greeting(userProfile?.callsign))
                                val unit = userProfile?.unitName?.trim().orEmpty()
                                if (unit.isNotBlank()) append(" • ").append(unit)
                            },
                            color = UniversalMuted,
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(UniversalPrimarySoft)
                            .clickable(onClick = onOpenProfile)
                            .padding(horizontal = 11.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = profileTemplate.emoji, fontSize = 15.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = profileTemplate.title,
                            color = UniversalPrimary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (points.isNotEmpty()) {
                    Text(
                        text = "МЕСТО ХРАНЕНИЯ",
                        color = UniversalMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        points.forEach { point ->
                            WarehouseMiniCard(
                                point = point,
                                positions = stockRecords
                                    .filter { it.pointId == point.id && it.quantity > 0 }
                                    .map { it.itemId }
                                    .distinct()
                                    .size,
                                selected = point.id == selectedPoint?.id,
                                onClick = { onSelectPoint(point.id) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF20265C), Color(0xFF5B5CE2))
                                )
                            )
                            .padding(horizontal = 18.dp, vertical = 17.dp)
                    ) {
                        Column {
                            Text(
                                text = selectedPoint?.name ?: "Основной склад",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = activePositions.toString(),
                                    color = Color.White,
                                    fontSize = 34.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(modifier = Modifier.width(7.dp))
                                Text(
                                    text = "позиций с остатком",
                                    color = Color.White.copy(alpha = 0.76f),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(bottom = 5.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(7.dp))
                            Text(
                                text = "Каталог: " + catalogItems.size +
                                    " • Операций сегодня: " + todayOps,
                                color = Color.White.copy(alpha = 0.72f),
                                fontSize = 10.5.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionTitle(
                        "Остатки • " + (selectedPoint?.name ?: "склад"),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Весь склад",
                        color = UniversalPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(onClick = onOpenCatalog)
                    )
                }

                Spacer(modifier = Modifier.height(9.dp))

                if (selectedPointBalances.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = UniversalSurface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "На этом складе пока нет остатков",
                                color = UniversalInk,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "После поступления здесь появятся конкретные позиции и их количество.",
                                color = UniversalMuted,
                                fontSize = 10.5.sp
                            )
                        }
                    }
                } else {
                    selectedPointBalances.take(6).forEach { (item, stock) ->
                        DashboardStockRow(item = item, stock = stock)
                        Spacer(modifier = Modifier.height(7.dp))
                    }
                    if (selectedPointBalances.size > 6) {
                        Text(
                            text = "Ещё " + (selectedPointBalances.size - 6) + " позиций • открыть весь склад",
                            color = UniversalPrimary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onOpenCatalog)
                                .padding(vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("Быстрые действия")

                Spacer(modifier = Modifier.height(9.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    UniversalActionCard(
                        title = vocab.income,
                        subtitle = "",
                        icon = Icons.Default.ArrowDownward,
                        accent = UniversalGreen,
                        soft = UniversalGreenSoft,
                        onClick = onIncomeClick,
                        modifier = Modifier.weight(1f)
                    )
                    UniversalActionCard(
                        title = vocab.issue,
                        subtitle = "",
                        icon = Icons.Default.ArrowUpward,
                        accent = UniversalOrange,
                        soft = UniversalOrangeSoft,
                        onClick = onIssueClick,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    UniversalActionCard(
                        title = vocab.transfer,
                        subtitle = "",
                        icon = Icons.Default.SwapHoriz,
                        accent = UniversalBlue,
                        soft = UniversalBlueSoft,
                        onClick = onTransferClick,
                        modifier = Modifier.weight(1f)
                    )
                    UniversalActionCard(
                        title = vocab.writeOff,
                        subtitle = "",
                        icon = Icons.Default.MoreHoriz,
                        accent = UniversalRed,
                        soft = UniversalRedSoft,
                        onClick = onWriteOffClick,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedPointOperations.isEmpty() && activePositions == 0) {
                    StarterGuideCard(
                        catalogCount = catalogItems.size,
                        onIncomeClick = onIncomeClick,
                        onOpenCatalog = onOpenCatalog
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionTitle("Последние операции • " + (selectedPoint?.name ?: "склад"), modifier = Modifier.weight(1f))
                    Text(
                        text = "Открыть журнал",
                        color = UniversalPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(onClick = onOpenOperations)
                    )
                }

                Spacer(modifier = Modifier.height(9.dp))

                if (recentOperations.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = UniversalSurface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Пока нет операций",
                                color = UniversalInk,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Начните с поступления или добавьте первую позицию.",
                                color = UniversalMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                } else {
                    recentOperations.forEach { op ->
                        RecentOperationRow(
                            operation = op,
                            label = when (op.type) {
                                OperationType.INCOME -> vocab.income
                                OperationType.TRANSFER -> vocab.transfer
                                OperationType.ISSUE -> vocab.issue
                                OperationType.EXPENDITURE -> vocab.writeOff
                            },
                            time = formatter.format(Date(op.timestamp))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(26.dp))
            }
        }
    }
}

@Composable
private fun DashboardStockRow(
    item: InventoryItem,
    stock: StockRecord
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = UniversalSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 13.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(UniversalPrimarySoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = UniversalPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    color = UniversalInk,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(item.serviceCategory)
                        if (item.subType.isNotBlank()) append(" • ").append(item.subType)
                    },
                    color = UniversalMuted,
                    fontSize = 9.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Остаток",
                    color = UniversalMuted,
                    fontSize = 8.5.sp
                )
                Text(
                    text = stock.quantity.toString(),
                    color = if (stock.quantity < 0) UniversalRed else UniversalInk,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = item.unit,
                    color = UniversalMuted,
                    fontSize = 9.5.sp
                )
            }
        }
    }
}

@Composable
private fun HeroMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(15.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .padding(horizontal = 9.dp, vertical = 9.dp)
    ) {
        Text(
            text = value,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.62f),
            fontSize = 8.5.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun UniversalActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    soft: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = UniversalSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(soft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                color = UniversalInk,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    color = UniversalMuted,
                    fontSize = 9.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SmallCommandCard(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(17.dp))
            .background(UniversalSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(31.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(UniversalPrimarySoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = UniversalPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(9.dp))
        Text(
            text = text,
            color = UniversalInk,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = Color(0xFFB3BAC6),
            modifier = Modifier.size(15.dp)
        )
    }
}

@Composable
private fun StarterGuideCard(
    catalogCount: Int,
    onIncomeClick: () -> Unit,
    onOpenCatalog: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = UniversalPrimarySoft),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Рабочее пространство готово",
                color = UniversalInk,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (catalogCount > 0)
                    "Каталог уже подготовлен: $catalogCount поз. с нулевым остатком. Внесите первое поступление — история и показатели заполнятся автоматически."
                else
                    "Добавьте первую позицию или внесите поступление. Никаких тестовых остатков мы не создаём.",
                color = UniversalMuted,
                fontSize = 10.5.sp,
                lineHeight = 15.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallCommandCard(
                    text = "Первый приход",
                    icon = Icons.Default.ArrowDownward,
                    onClick = onIncomeClick,
                    modifier = Modifier.weight(1f)
                )
                SmallCommandCard(
                    text = "Открыть каталог",
                    icon = Icons.Default.Inventory2,
                    onClick = onOpenCatalog,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AttentionCard(
    zeroPositions: Int,
    pendingRequests: Int,
    catalogIsEmpty: Boolean
) {
    val title: String
    val subtitle: String
    val accent: Color
    val soft: Color

    when {
        catalogIsEmpty -> {
            title = "Склад пока пуст"
            subtitle = "Добавьте первую позицию и начните учёт."
            accent = UniversalPrimary
            soft = UniversalPrimarySoft
        }
        zeroPositions > 0 -> {
            title = "$zeroPositions поз. с нулевым остатком"
            subtitle = "Проверьте, нужно ли пополнение."
            accent = UniversalRed
            soft = UniversalRedSoft
        }
        pendingRequests > 0 -> {
            title = "$pendingRequests заявок ждут обработки"
            subtitle = "Откройте раздел заявок и проверьте статус."
            accent = UniversalOrange
            soft = UniversalOrangeSoft
        }
        else -> {
            title = "Всё в порядке"
            subtitle = "Критичных состояний сейчас нет."
            accent = UniversalGreen
            soft = UniversalGreenSoft
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(soft)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.8f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (zeroPositions > 0) Icons.Default.WarningAmber else Icons.Default.Inventory2,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(21.dp)
            )
        }
        Spacer(modifier = Modifier.width(11.dp))
        Column {
            Text(
                text = title,
                color = UniversalInk,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = UniversalMuted,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun WarehouseMiniCard(
    point: WarehousePoint,
    positions: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) UniversalPrimary else UniversalSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Warehouse,
            contentDescription = null,
            tint = if (selected) Color.White else UniversalPrimary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(7.dp))
        Column {
            Text(
                text = point.name,
                color = if (selected) Color.White else UniversalInk,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = positions.toString() + " поз. с остатком",
                color = if (selected) Color.White.copy(alpha = 0.75f) else UniversalMuted,
                fontSize = 8.5.sp
            )
        }
    }
}

@Composable
private fun RecentOperationRow(
    operation: OperationRecord,
    label: String,
    time: String
) {
    val (accent, soft, icon) = when (operation.type) {
        OperationType.INCOME -> Triple(UniversalGreen, UniversalGreenSoft, Icons.Default.ArrowDownward)
        OperationType.TRANSFER -> Triple(UniversalBlue, UniversalBlueSoft, Icons.Default.SwapHoriz)
        OperationType.ISSUE -> Triple(UniversalOrange, UniversalOrangeSoft, Icons.Default.ArrowUpward)
        OperationType.EXPENDITURE -> Triple(UniversalRed, UniversalRedSoft, Icons.Default.MoreHoriz)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(UniversalSurface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(soft),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = UniversalInk,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = operation.itemsSummary.ifBlank {
                    listOf(operation.fromPointName, operation.toPointName)
                        .filter { it.isNotBlank() }
                        .joinToString(" → ")
                },
                color = UniversalMuted,
                fontSize = 9.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = time,
            color = Color(0xFF9CA3AF),
            fontSize = 9.5.sp
        )
    }
}

@Composable
private fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = UniversalInk,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
}

private fun greeting(name: String?): String {
    val prefix = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Доброе утро"
        in 12..17 -> "Добрый день"
        else -> "Добрый вечер"
    }
    val clean = name?.trim().orEmpty()
    return if (clean.isBlank()) prefix else "$prefix, $clean"
}
