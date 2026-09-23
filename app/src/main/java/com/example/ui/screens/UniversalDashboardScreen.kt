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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.InventoryItem
import com.example.data.model.OperationRecord
import com.example.data.model.RequisitionRequest
import com.example.data.model.StockRecord
import com.example.data.model.UserProfile
import com.example.data.model.WarehousePoint
import com.example.universal.WarehouseProfileCatalog
import java.util.Calendar

private val HomeInk = Color(0xFF111827)
private val HomeMuted = Color(0xFF667085)
private val HomeBg = Color(0xFFF6F7FB)
private val HomeSurface = Color.White
private val HomePrimary = Color(0xFF5B5CE2)
private val HomePrimarySoft = Color(0xFFEEEEFF)
private val HomeGreen = Color(0xFF159A72)
private val HomeGreenSoft = Color(0xFFE9F8F3)
private val HomeOrange = Color(0xFFE88B22)
private val HomeOrangeSoft = Color(0xFFFFF4E5)
private val HomeRed = Color(0xFFD94C4C)
private val HomeRedSoft = Color(0xFFFFEEEE)
private val HomeBlue = Color(0xFF3977D8)
private val HomeBlueSoft = Color(0xFFEAF2FF)

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
    onOpenReport: () -> Unit,
    onOpenProfile: () -> Unit
) {
    var showWarehousePicker by remember { mutableStateOf(false) }

    val selectedPoint = remember(points, selectedPointId) {
        points.firstOrNull { it.id == selectedPointId } ?: points.firstOrNull()
    }
    val selectedProfile = WarehouseProfileCatalog.find(
        selectedPoint?.profileId?.takeIf { it.isNotBlank() } ?: warehouseProfileId
    )
    val vocab = selectedProfile.operations

    val selectedStocks = remember(stockRecords, selectedPoint?.id) {
        val pointId = selectedPoint?.id
        if (pointId == null) emptyList() else stockRecords.filter { it.pointId == pointId }
    }
    val catalogById = remember(catalogItems) { catalogItems.associateBy { it.id } }
    val balances = remember(selectedStocks, catalogById) {
        selectedStocks
            .filter { it.quantity != 0 || it.incomeTotal != 0 || it.expenseTotal != 0 }
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
    val activePositions = balances.count { it.second.quantity > 0 }
    val categoryCount = catalogItems.map { it.serviceCategory }.filter { it.isNotBlank() }.distinct().size

    val warehouseOps = remember(operations, selectedPoint?.id, selectedPoint?.name) {
        val id = selectedPoint?.id.orEmpty()
        val name = selectedPoint?.name.orEmpty()
        operations.filter { op ->
            val hasStableIds = op.fromPointId.isNotBlank() || op.toPointId.isNotBlank()
            if (hasStableIds) {
                op.fromPointId == id || op.toPointId == id
            } else {
                op.fromPointName == name || op.toPointName == name
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
    val todayOps = warehouseOps.count { it.timestamp >= todayStart }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Склад",
                        color = HomeInk,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Остатки и движения без лишних экранов",
                        color = HomeMuted,
                        fontSize = 11.5.sp
                    )
                }
                Text(
                    text = selectedProfile.emoji,
                    fontSize = 22.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(HomePrimarySoft)
                        .clickable(onClick = onOpenProfile)
                        .padding(10.dp)
                )
            }
        }

        item {
            CurrentWarehouseCard(
                point = selectedPoint,
                profileTitle = selectedProfile.title,
                positions = activePositions,
                onClick = { showWarehousePicker = true }
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HomeMetric(
                    value = activePositions.toString(),
                    label = "с остатком",
                    modifier = Modifier.weight(1f)
                )
                HomeMetric(
                    value = todayOps.toString(),
                    label = "операций сегодня",
                    modifier = Modifier.weight(1f)
                )
                HomeMetric(
                    value = categoryCount.toString(),
                    label = "категорий",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Text(
                text = "Быстрые операции",
                color = HomeInk,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(7.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickChip(vocab.income, Icons.Default.ArrowDownward, HomeGreen, HomeGreenSoft, onIncomeClick)
                QuickChip(vocab.issue, Icons.Default.ArrowUpward, HomeOrange, HomeOrangeSoft, onIssueClick)
                QuickChip(vocab.transfer, Icons.Default.SwapHoriz, HomeBlue, HomeBlueSoft, onTransferClick)
                QuickChip(vocab.writeOff, Icons.Default.MoreHoriz, HomeRed, HomeRedSoft, onWriteOffClick)
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Остатки",
                    color = HomeInk,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Отчёт",
                    color = HomeGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable(onClick = onOpenReport)
                        .padding(6.dp)
                )
                Text(
                    text = "Каталог",
                    color = HomePrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable(onClick = onOpenCatalog)
                        .padding(6.dp)
                )
            }
        }

        if (balances.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = HomeSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "На выбранном складе пока нет движения",
                            color = HomeInk,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Сделайте поступление — здесь сразу появятся пришло, ушло и остаток.",
                            color = HomeMuted,
                            fontSize = 10.5.sp
                        )
                    }
                }
            }
        } else {
            items(balances.take(4), key = { it.first.id }) { (item, stock) ->
                CompactBalanceRow(item, stock)
            }
            if (balances.size > 4) {
                item {
                    Text(
                        text = "Показать ещё ${balances.size - 4} поз.",
                        color = HomePrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenCatalog)
                            .padding(vertical = 8.dp)
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallLinkCard(
                    title = "Все операции",
                    subtitle = "История выбранного склада",
                    onClick = onOpenOperations,
                    modifier = Modifier.weight(1f)
                )
                SmallLinkCard(
                    title = "Добавить позицию",
                    subtitle = "В каталог этого склада",
                    onClick = onAddItemClick,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(18.dp))
        }
    }

    if (showWarehousePicker) {
        WarehousePickerDialog(
            points = points,
            selectedPointId = selectedPoint?.id.orEmpty(),
            onSelect = {
                onSelectPoint(it.id)
                showWarehousePicker = false
            },
            onDismiss = { showWarehousePicker = false }
        )
    }
}

@Composable
private fun CurrentWarehouseCard(
    point: WarehousePoint?,
    profileTitle: String,
    positions: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF20265C)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Color.White.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warehouse,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(23.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ТЕКУЩИЙ СКЛАД",
                    color = Color.White.copy(alpha = 0.62f),
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = point?.name ?: "Склад не выбран",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$profileTitle • $positions поз. с остатком",
                    color = Color.White.copy(alpha = 0.76f),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Сменить",
                    color = Color(0xFFD7D9FF),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Выбрать склад",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun WarehousePickerDialog(
    points: List<WarehousePoint>,
    selectedPointId: String,
    onSelect: (WarehousePoint) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Выберите склад",
                    color = HomeInk,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "У каждого склада свои профиль, остатки, каталог и отчёты.",
                    color = HomeMuted,
                    fontSize = 10.5.sp
                )
                Spacer(Modifier.height(12.dp))

                if (points.isEmpty()) {
                    Text(
                        text = "Складов пока нет. Добавьте первый склад в разделе «Ещё».",
                        color = HomeMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        items(points, key = { it.id }) { point ->
                            val profile = WarehouseProfileCatalog.find(point.profileId)
                            val selected = point.id == selectedPointId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (selected) HomePrimarySoft else HomeBg)
                                    .clickable { onSelect(point) }
                                    .padding(horizontal = 12.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(profile.emoji, fontSize = 18.sp)
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = point.name,
                                        color = HomeInk,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = profile.title,
                                        color = HomeMuted,
                                        fontSize = 9.5.sp
                                    )
                                }
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = HomePrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Закрыть",
                    color = HomePrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.End)
                        .clickable(onClick = onDismiss)
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun HomeMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(17.dp))
            .background(HomeSurface)
            .padding(horizontal = 10.dp, vertical = 11.dp)
    ) {
        Text(
            text = value,
            color = HomeInk,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = label,
            color = HomeMuted,
            fontSize = 8.8.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun QuickChip(
    title: String,
    icon: ImageVector,
    accent: Color,
    soft: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(15.dp))
            .background(HomeSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(soft),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = title,
            color = HomeInk,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun CompactBalanceRow(item: InventoryItem, stock: StockRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = HomeSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(HomePrimarySoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = HomePrimary,
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    color = HomeInk,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOf(item.serviceCategory, item.subType)
                        .filter { it.isNotBlank() }
                        .joinToString(" • "),
                    color = HomeMuted,
                    fontSize = 8.8.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(6.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${stock.quantity} ${item.unit}",
                    color = if (stock.quantity > 0) HomeInk else HomeRed,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "+${stock.incomeTotal} / -${stock.expenseTotal}",
                    color = HomeMuted,
                    fontSize = 8.6.sp
                )
            }
        }
    }
}

@Composable
private fun SmallLinkCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(17.dp))
            .background(HomeSurface)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Text(title, color = HomeInk, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(
            subtitle,
            color = HomeMuted,
            fontSize = 8.8.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
