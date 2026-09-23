package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.InventoryItem
import com.example.data.model.RequisitionItemEntry
import com.example.data.model.RequisitionRequest
import com.example.data.model.RequestStatus
import com.example.data.model.StockRecord
import com.example.data.model.UserProfile
import com.example.data.model.WarehousePoint
import com.example.ui.components.TacticalSearchableItemDropdown
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ReqBg = Color(0xFFF5F7FB)
private val ReqInk = Color(0xFF111827)
private val ReqMuted = Color(0xFF6B7280)
private val ReqPrimary = Color(0xFF5B5CE2)
private val ReqPrimarySoft = Color(0xFFEEEEFF)
private val ReqOrange = Color(0xFFE88B22)
private val ReqOrangeSoft = Color(0xFFFFF4E5)
private val ReqGreen = Color(0xFF159A72)
private val ReqGreenSoft = Color(0xFFE9F8F3)

private class UniversalRequestDraft(
    item: InventoryItem? = null,
    quantity: String = "1"
) {
    var item by mutableStateOf(item)
    var quantity by mutableStateOf(quantity)
}

@Composable
fun UniversalRequestsScreen(
    profile: UserProfile?,
    points: List<WarehousePoint>,
    catalogItems: List<InventoryItem>,
    stockRecords: List<StockRecord>,
    requisitions: List<RequisitionRequest>,
    onCreateRequisition: (String, String, List<RequisitionItemEntry>, String) -> Unit,
    onUpdateStatus: (RequisitionRequest, RequestStatus) -> Unit,
    onDeleteRequisition: (String) -> Unit,
    parseItems: (String) -> List<RequisitionItemEntry>
) {
    var showCreate by remember { mutableStateOf(false) }
    val pending = requisitions.count { it.status == RequestStatus.PENDING }
    val collected = requisitions.count { it.status == RequestStatus.COLLECTED }
    val issued = requisitions.count { it.status == RequestStatus.ISSUED }
    val dateFormat = remember { SimpleDateFormat("dd MMM • HH:mm", Locale("ru")) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ReqBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Заявки",
                        color = ReqInk,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Запросы на выдачу и комплектацию",
                        color = ReqMuted,
                        fontSize = 12.sp
                    )
                }
                Button(
                    onClick = { showCreate = true },
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ReqPrimary,
                        contentColor = Color.White
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 13.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Новая", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RequestMetric(pending.toString(), "в работе", ReqOrange, ReqOrangeSoft, Modifier.weight(1f))
                RequestMetric(collected.toString(), "собрано", ReqPrimary, ReqPrimarySoft, Modifier.weight(1f))
                RequestMetric(issued.toString(), "выдано", ReqGreen, ReqGreenSoft, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        if (requisitions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(ReqPrimarySoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = ReqPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Заявок пока нет",
                            color = ReqInk,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Создайте заявку, когда нужно собрать и выдать несколько позиций со склада.",
                            color = ReqMuted,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        } else {
            items(requisitions.sortedByDescending { it.timestamp }, key = { it.id }) { req ->
                val visual = requestVisual(req.status)
                val reqItems = remember(req.itemsJson, req.itemsSummary) {
                    parseItems(req.itemsJson)
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(21.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(13.dp))
                                    .background(visual.soft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = visual.icon,
                                    contentDescription = null,
                                    tint = visual.accent,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = req.pointName.ifBlank { "Заявка" },
                                    color = ReqInk,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = buildString {
                                        append(visual.label)
                                        if (req.applicantName.isNotBlank()) append(" • ${req.applicantName}")
                                    },
                                    color = visual.accent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = dateFormat.format(Date(req.timestamp)),
                                color = Color(0xFF9CA3AF),
                                fontSize = 9.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (req.itemsSummary.isNotBlank()) req.itemsSummary
                            else if (reqItems.isNotEmpty()) reqItems.joinToString(", ") { "${it.itemName} ×${it.quantity}" }
                            else "Без позиций",
                            color = ReqInk,
                            fontSize = 11.5.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (req.comment.isNotBlank()) {
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = req.comment,
                                color = ReqMuted,
                                fontSize = 10.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(11.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            when (req.status) {
                                RequestStatus.PENDING -> {
                                    RequestActionButton("Собрана", ReqPrimary, Modifier.weight(1f)) {
                                        onUpdateStatus(req, RequestStatus.COLLECTED)
                                    }
                                }
                                RequestStatus.COLLECTED -> {
                                    RequestActionButton("Выдана", ReqGreen, Modifier.weight(1f)) {
                                        onUpdateStatus(req, RequestStatus.ISSUED)
                                    }
                                }
                                RequestStatus.ISSUED -> Box(modifier = Modifier.weight(1f))
                            }

                            IconButton(
                                onClick = { onDeleteRequisition(req.id) },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFFEEEE))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Удалить",
                                    tint = Color(0xFFD94C4C),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    if (showCreate) {
        CreateUniversalRequestDialog(
            profile = profile,
            points = points,
            catalogItems = catalogItems,
            stockRecords = stockRecords,
            onDismiss = { showCreate = false },
            onConfirm = { pointName, applicant, requestItems, comment ->
                onCreateRequisition(pointName, applicant, requestItems, comment)
                showCreate = false
            }
        )
    }
}

@Composable
private fun RequestMetric(
    value: String,
    label: String,
    accent: Color,
    soft: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(17.dp))
            .background(Color.White)
            .padding(12.dp)
    ) {
        Text(value, color = accent, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = ReqMuted, fontSize = 9.5.sp)
    }
}

@Composable
private fun RequestActionButton(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(38.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
    ) {
        Text(text, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
    }
}

private data class RequestVisual(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val accent: Color,
    val soft: Color
)

private fun requestVisual(status: RequestStatus): RequestVisual = when (status) {
    RequestStatus.PENDING -> RequestVisual("В обработке", Icons.Default.HourglassTop, ReqOrange, ReqOrangeSoft)
    RequestStatus.COLLECTED -> RequestVisual("Собрана", Icons.Default.CheckCircle, ReqPrimary, ReqPrimarySoft)
    RequestStatus.ISSUED -> RequestVisual("Выдана", Icons.Default.LocalShipping, ReqGreen, ReqGreenSoft)
}

@Composable
private fun CreateUniversalRequestDialog(
    profile: UserProfile?,
    points: List<WarehousePoint>,
    catalogItems: List<InventoryItem>,
    stockRecords: List<StockRecord>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, List<RequisitionItemEntry>, String) -> Unit
) {
    var selectedPoint by remember {
        mutableStateOf(points.firstOrNull() ?: WarehousePoint("main_warehouse", "Основной склад", isBase = true))
    }
    var applicant by remember { mutableStateOf(profile?.callsign.orEmpty()) }
    var comment by remember { mutableStateOf("") }
    var pointExpanded by remember { mutableStateOf(false) }
    val drafts = remember { mutableStateListOf(UniversalRequestDraft()) }

    val stockMap = remember(stockRecords, selectedPoint) {
        stockRecords
            .filter { it.pointId == selectedPoint.id }
            .associate { it.itemId to it.quantity }
    }
    val availableItems = remember(catalogItems, stockMap) {
        catalogItems.filter { (stockMap[it.id] ?: 0) > 0 }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 18.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Новая заявка", color = ReqInk, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Выберите склад и позиции", color = ReqMuted, fontSize = 10.5.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = Color(0xFF8A93A2))
                    }
                }

                Spacer(modifier = Modifier.height(15.dp))

                Text("Склад", color = ReqInk, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(15.dp))
                            .background(Color(0xFFF9FAFB))
                            .clickable { pointExpanded = true }
                            .padding(horizontal = 13.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(selectedPoint.name, color = ReqInk, fontSize = 12.5.sp, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = ReqPrimary)
                    }
                    DropdownMenu(
                        expanded = pointExpanded,
                        onDismissRequest = { pointExpanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        points.forEach { point ->
                            DropdownMenuItem(
                                text = { Text(point.name, color = ReqInk, fontSize = 12.sp) },
                                onClick = {
                                    selectedPoint = point
                                    drafts.forEach { it.item = null }
                                    pointExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                RequestField(applicant, { applicant = it }, "Заявитель", "Имя сотрудника")

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Позиции",
                        color = ReqInk,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "+ добавить",
                        color = ReqPrimary,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .clickable { drafts.add(UniversalRequestDraft()) }
                            .background(ReqPrimarySoft)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(7.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(drafts.size) { index ->
                        val draft = drafts[index]
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "#${index + 1}",
                                        color = ReqMuted,
                                        fontSize = 9.5.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (drafts.size > 1) {
                                        IconButton(
                                            onClick = { drafts.removeAt(index) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Удалить строку",
                                                tint = Color(0xFFD94C4C),
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }

                                TacticalSearchableItemDropdown(
                                    label = "",
                                    catalogItems = availableItems,
                                    selectedItem = draft.item,
                                    availableStocksMap = stockMap,
                                    onItemSelected = { draft.item = it }
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                OutlinedTextField(
                                    value = draft.quantity,
                                    onValueChange = { draft.quantity = it.filter(Char::isDigit) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    label = { Text("Количество", fontSize = 10.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(13.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White,
                                        focusedBorderColor = ReqPrimary,
                                        unfocusedBorderColor = Color(0xFFE3E7EE),
                                        focusedTextColor = ReqInk,
                                        unfocusedTextColor = ReqInk
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                RequestField(comment, { comment = it }, "Комментарий", "Необязательно")
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val entries = drafts.mapNotNull { draft ->
                            val item = draft.item ?: return@mapNotNull null
                            val qty = draft.quantity.toIntOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
                            RequisitionItemEntry(item.name, qty, item.unit)
                        }
                        if (entries.isNotEmpty()) {
                            onConfirm(
                                selectedPoint.name,
                                applicant.trim().ifBlank { "Сотрудник" },
                                entries,
                                comment.trim()
                            )
                        }
                    },
                    enabled = drafts.any { it.item != null && (it.quantity.toIntOrNull() ?: 0) > 0 },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ReqPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Text("Создать заявку", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun RequestField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(label, fontSize = 10.5.sp) },
        placeholder = { Text(placeholder, color = Color(0xFF9CA3AF), fontSize = 11.sp) },
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xFFF9FAFB),
            unfocusedContainerColor = Color(0xFFF9FAFB),
            focusedBorderColor = ReqPrimary,
            unfocusedBorderColor = Color(0xFFE3E7EE),
            focusedTextColor = ReqInk,
            unfocusedTextColor = ReqInk
        )
    )
}
