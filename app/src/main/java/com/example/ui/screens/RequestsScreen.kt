@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AssignmentTurnedIn
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Warehouse
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InventoryItem
import com.example.data.model.RequestStatus
import com.example.data.model.RequisitionItemEntry
import com.example.data.model.RequisitionRequest
import com.example.data.model.StockRecord
import com.example.data.model.UserProfile
import com.example.data.model.WarehousePoint
import com.example.ui.components.QuantityStepper
import com.example.ui.components.TacticalSearchableItemDropdown
import com.example.ui.theme.SageGreenBright
import com.example.ui.theme.SageGreenPrimary
import com.example.ui.theme.TacticalBg
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalBorderSubtle
import com.example.ui.theme.TacticalGoldText
import com.example.ui.theme.TacticalRedText
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalSurfaceLight
import com.example.ui.theme.TacticalTealText
import com.example.ui.theme.TacticalTextMuted
import com.example.ui.theme.TacticalTextPrimary
import com.example.ui.theme.TacticalTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val URGENT = "[СРОЧНО] "

/** Pipeline colour for each status. */
@Composable
private fun statusColor(status: RequestStatus): Color = when (status) {
    RequestStatus.PENDING -> TacticalGoldText
    RequestStatus.ASSEMBLING -> TacticalTealText
    RequestStatus.COLLECTED -> SageGreenBright
    RequestStatus.ISSUED -> TacticalTextMuted
}

/**
 * Requests board. Tabs by stage (Новые / Собираются / Собраны / Выданы) with
 * counts; each card shows a 4-step progress line, what is asked and whether
 * the warehouse has it, and one big button for the next step. "Выдать"
 * posts the real issue operation, so the request also shows up in the
 * journal and in Form 8. New requests are made in a bottom sheet.
 */
@Composable
fun RequestsScreen(
    profile: UserProfile?,
    points: List<WarehousePoint>,
    catalogItems: List<InventoryItem>,
    stockRecords: List<StockRecord> = emptyList(),
    requisitions: List<RequisitionRequest>,
    onCreateRequisition: (pointName: String, applicantName: String, items: List<RequisitionItemEntry>, comment: String) -> Unit,
    onUpdateStatus: (RequisitionRequest, RequestStatus) -> Unit,
    onDeleteRequisition: (String) -> Unit,
    parseItems: (String) -> List<RequisitionItemEntry>,
    onIssueRequisition: (RequisitionRequest) -> Unit = { onUpdateStatus(it, RequestStatus.ISSUED) }
) {
    var tab by remember { mutableStateOf(RequestStatus.PENDING) }
    var showForm by remember { mutableStateOf(false) }
    var confirmIssue by remember { mutableStateOf<RequisitionRequest?>(null) }
    var confirmDelete by remember { mutableStateOf<RequisitionRequest?>(null) }

    val counts = remember(requisitions) { requisitions.groupingBy { it.status }.eachCount() }
    val shown = remember(requisitions, tab) {
        requisitions.filter { it.status == tab }
            .sortedWith(compareByDescending<RequisitionRequest> { it.comment.startsWith(URGENT) }.thenByDescending { it.timestamp })
    }
    // Stock per warehouse name → item name → quantity, for "хватает / не хватает".
    val stockByPointAndName = remember(stockRecords, points, catalogItems) {
        val namesById = catalogItems.associate { it.id to it.name.lowercase() }
        points.associate { p ->
            p.name to stockRecords.filter { it.pointId == p.id }
                .groupBy { namesById[it.itemId] ?: it.itemId }
                .mapValues { (_, r) -> r.sumOf { it.quantity } }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(TacticalBg)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text("Заявки", color = TacticalTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text("Запросы имущества: сборка и выдача со склада", color = TacticalTextMuted, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(12.dp))
            }
            stickyHeader {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TacticalBg)
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RequestStatus.pipeline.forEach { status ->
                        StageTab(
                            title = when (status) {
                                RequestStatus.PENDING -> "Новые"
                                RequestStatus.ASSEMBLING -> "Собираются"
                                RequestStatus.COLLECTED -> "Собраны"
                                RequestStatus.ISSUED -> "Выданы"
                            },
                            count = counts[status] ?: 0,
                            color = statusColor(status),
                            selected = tab == status
                        ) { tab = status }
                    }
                }
            }
            if (shown.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Rounded.AssignmentTurnedIn, contentDescription = null, tint = TacticalTextMuted, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            if (tab == RequestStatus.PENDING) "Новых заявок нет" else "Здесь пока пусто",
                            color = TacticalTextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Нажмите «Новая заявка» внизу", color = TacticalTextMuted, fontSize = 12.sp)
                    }
                }
            }
            items(shown, key = { it.id }) { req ->
                Box(modifier = Modifier.animateItem()) {
                    RequestCard(
                        req = req,
                        items = parseItems(req.itemsJson),
                        stock = stockByPointAndName[req.pointName].orEmpty(),
                        onNext = {
                            when (req.status) {
                                RequestStatus.PENDING -> onUpdateStatus(req, RequestStatus.ASSEMBLING)
                                RequestStatus.ASSEMBLING -> onUpdateStatus(req, RequestStatus.COLLECTED)
                                RequestStatus.COLLECTED -> confirmIssue = req
                                RequestStatus.ISSUED -> Unit
                            }
                        },
                        onBack = {
                            val prev = RequestStatus.pipeline.getOrNull(req.status.step - 1)
                            if (prev != null && req.status != RequestStatus.ISSUED) onUpdateStatus(req, prev)
                        },
                        onDelete = { confirmDelete = req }
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
            item { Spacer(modifier = Modifier.height(90.dp)) }
        }

        // Floating "new request" button
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(18.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(SageGreenPrimary, TacticalTealText)))
                .clickable { showForm = true }
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Новая заявка", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }

    if (showForm) {
        NewRequestSheet(
            profile = profile,
            points = points,
            catalogItems = catalogItems,
            stockRecords = stockRecords,
            onDismiss = { showForm = false },
            onSubmit = { pointName, applicant, items, comment ->
                onCreateRequisition(pointName, applicant, items, comment)
                tab = RequestStatus.PENDING
                showForm = false
            }
        )
    }

    confirmIssue?.let { req ->
        AlertDialog(
            onDismissRequest = { confirmIssue = null },
            title = { Text("Выдать по заявке?") },
            text = {
                Text(
                    "Со склада «${req.pointName}» будут списаны позиции заявки и выданы: ${req.applicantName}. " +
                        "Выдача попадёт в журнал операций и в форму 8."
                )
            },
            confirmButton = {
                TextButton(onClick = { onIssueRequisition(req); confirmIssue = null; tab = RequestStatus.ISSUED }) { Text("Выдать") }
            },
            dismissButton = { TextButton(onClick = { confirmIssue = null }) { Text("Отмена") } }
        )
    }
    confirmDelete?.let { req ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Удалить заявку?") },
            text = { Text("Заявка от ${req.applicantName} будет удалена у всех телефонов подразделения. Остатки не изменятся.") },
            confirmButton = { TextButton(onClick = { onDeleteRequisition(req.id); confirmDelete = null }) { Text("Удалить", color = TacticalRedText) } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun StageTab(title: String, count: Int, color: Color, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) color.copy(alpha = 0.18f) else TacticalSurface, label = "tabBg")
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, if (selected) color else TacticalBorderSubtle, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(6.dp))
        Text(title, color = if (selected) TacticalTextPrimary else TacticalTextSecondary, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
        if (count > 0) {
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.25f)).padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text("$count", color = TacticalTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RequestCard(
    req: RequisitionRequest,
    items: List<RequisitionItemEntry>,
    stock: Map<String, Int>,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onDelete: () -> Unit
) {
    val color = statusColor(req.status)
    val urgent = req.comment.startsWith(URGENT)
    val comment = req.comment.removePrefix(URGENT)
    val date = remember(req.timestamp) { SimpleDateFormat("d MMM, HH:mm", Locale("ru")).format(Date(req.timestamp)) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(TacticalSurface)
            .border(1.dp, if (urgent && req.status != RequestStatus.ISSUED) TacticalRedText.copy(alpha = 0.6f) else TacticalBorderSubtle, RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (urgent) {
                        Icon(Icons.Rounded.LocalFireDepartment, contentDescription = "Срочно", tint = TacticalRedText, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(req.applicantName.ifBlank { "Без получателя" }, color = TacticalTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Warehouse, contentDescription = null, tint = TacticalTextMuted, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("со склада «${req.pointName}» • $date", color = TacticalTextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Box(
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.18f)).padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(req.status.titleRu, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        StageProgress(step = req.status.step, color = color)
        Spacer(modifier = Modifier.height(12.dp))

        // Positions with availability
        val lines = items.ifEmpty { listOf(RequisitionItemEntry(req.itemsSummary.ifBlank { "Позиции не указаны" }, 0, "")) }
        lines.forEach { entry ->
            val have = stock[entry.itemName.lowercase()]
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(entry.itemName, color = TacticalTextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (entry.quantity > 0) {
                    Text("${entry.quantity} ${entry.unit}", color = TacticalTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                if (req.status != RequestStatus.ISSUED && entry.quantity > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    val enough = (have ?: 0) >= entry.quantity
                    Text(
                        if (enough) "✓" else "есть ${have ?: 0}",
                        color = if (enough) SageGreenBright else TacticalRedText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        if (comment.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text("«$comment»", color = TacticalTextSecondary, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (req.status != RequestStatus.ISSUED) {
                Button(
                    onClick = onNext,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (req.status == RequestStatus.COLLECTED) SageGreenPrimary else color.copy(alpha = 0.22f),
                        contentColor = if (req.status == RequestStatus.COLLECTED) Color.White else TacticalTextPrimary
                    )
                ) {
                    Text(
                        when (req.status) {
                            RequestStatus.PENDING -> "Начать сборку"
                            RequestStatus.ASSEMBLING -> "Собрана"
                            else -> "Выдать"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (req.status.step > 0) {
                    TextButton(onClick = onBack) { Text("Назад", color = TacticalTextMuted, fontSize = 13.sp) }
                }
            } else {
                Text("✓ Выдано и проведено в журнале", color = SageGreenBright, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            }
            Icon(
                Icons.Rounded.DeleteOutline,
                contentDescription = "Удалить заявку",
                tint = TacticalTextMuted,
                modifier = Modifier.clip(CircleShape).clickable(onClick = onDelete).padding(8.dp).size(20.dp)
            )
        }
    }
}

/** Four dots joined by a line; the filled part animates to the current step. */
@Composable
private fun StageProgress(step: Int, color: Color) {
    val progress by animateFloatAsState(step / 3f, tween(600), label = "stage")
    val labels = listOf("Новая", "Сборка", "Собрана", "Выдана")
    Column {
        Box(modifier = Modifier.fillMaxWidth().height(14.dp), contentAlignment = Alignment.CenterStart) {
            Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(TacticalSurfaceLight))
            Box(modifier = Modifier.fillMaxWidth(progress.coerceIn(0.001f, 1f)).height(4.dp).clip(RoundedCornerShape(2.dp)).background(color))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                repeat(4) { i ->
                    val reached = i <= step
                    val scale by animateFloatAsState(if (i == step) 1.25f else 1f, spring(dampingRatio = 0.4f), label = "dot")
                    Box(
                        modifier = Modifier
                            .size((12 * scale).dp)
                            .clip(CircleShape)
                            .background(if (reached) color else TacticalSurfaceLight)
                            .border(2.dp, TacticalSurface, CircleShape)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEachIndexed { i, l ->
                Text(l, color = if (i <= step) TacticalTextSecondary else TacticalTextMuted, fontSize = 10.sp, fontWeight = if (i == step) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

private class DraftLine(val item: InventoryItem, qty: String) {
    var qty by mutableStateOf(qty)
}

@Composable
private fun NewRequestSheet(
    profile: UserProfile?,
    points: List<WarehousePoint>,
    catalogItems: List<InventoryItem>,
    stockRecords: List<StockRecord>,
    onDismiss: () -> Unit,
    onSubmit: (String, String, List<RequisitionItemEntry>, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var source by remember { mutableStateOf(points.firstOrNull { it.isBase } ?: points.firstOrNull()) }
    var recipient by remember { mutableStateOf("") }
    var urgent by remember { mutableStateOf(false) }
    var comment by remember { mutableStateOf("") }
    val lines = remember { mutableStateListOf<DraftLine>() }
    var picked by remember { mutableStateOf<InventoryItem?>(null) }
    var pickedQty by remember { mutableStateOf("1") }
    val stockAtSource = remember(stockRecords, source) {
        stockRecords.filter { it.pointId == source?.id }.groupBy { it.itemId }.mapValues { (_, r) -> r.sumOf { it.quantity } }
    }
    val canSubmit = source != null && recipient.isNotBlank() && lines.isNotEmpty()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = TacticalSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Новая заявка", color = TacticalTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                Icon(Icons.Rounded.Close, contentDescription = "Закрыть", tint = TacticalTextMuted, modifier = Modifier.clip(CircleShape).clickable(onClick = onDismiss).padding(6.dp))
            }
            Spacer(modifier = Modifier.height(14.dp))

            SheetLabel("1. Со склада")
            ChipRow(points.map { it.name }, source?.name) { name -> source = points.firstOrNull { it.name == name } }

            Spacer(modifier = Modifier.height(14.dp))
            SheetLabel("2. Кому")
            OutlinedTextField(
                value = recipient,
                onValueChange = { recipient = it.take(60) },
                placeholder = { Text("Позывной, ФИО или подразделение", color = TacticalTextMuted) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = fieldColors()
            )
            val otherPoints = points.filter { it.id != source?.id }.map { it.name }
            if (otherPoints.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                ChipRow(otherPoints, recipient) { recipient = it }
            }
            profile?.callsign?.takeIf { it.isNotBlank() && recipient.isBlank() }?.let { me ->
                Text("Для себя: $me", color = SageGreenBright, fontSize = 12.sp, modifier = Modifier.clickable { recipient = me }.padding(vertical = 6.dp))
            }

            Spacer(modifier = Modifier.height(14.dp))
            SheetLabel("3. Что нужно")
            TacticalSearchableItemDropdown(
                label = "Выберите позицию",
                catalogItems = catalogItems,
                selectedItem = picked,
                onItemSelected = { picked = it },
                availableStocksMap = stockAtSource
            )
            picked?.let { item ->
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    QuantityStepper(value = pickedQty, onValueChange = { pickedQty = it }, unit = item.unit, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val q = pickedQty.toIntOrNull()?.coerceAtLeast(1) ?: 1
                            val existing = lines.firstOrNull { it.item.id == item.id }
                            if (existing != null) existing.qty = ((existing.qty.toIntOrNull() ?: 0) + q).toString()
                            else lines.add(DraftLine(item, q.toString()))
                            picked = null
                            pickedQty = "1"
                        },
                        modifier = Modifier.height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SageGreenPrimary, contentColor = Color.White)
                    ) {
                        Icon(Icons.Rounded.PlaylistAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Добавить", fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (lines.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(TacticalSurfaceLight).padding(10.dp)
                ) {
                    lines.forEach { line ->
                        val have = stockAtSource[line.item.id] ?: 0
                        val q = line.qty.toIntOrNull() ?: 0
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(line.item.name, color = TacticalTextPrimary, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(
                                    if (have >= q) "на складе $have ${line.item.unit} — хватает" else "на складе только $have ${line.item.unit}",
                                    color = if (have >= q) SageGreenBright else TacticalRedText,
                                    fontSize = 11.sp
                                )
                            }
                            Text("$q ${line.item.unit}", color = TacticalTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Убрать",
                                tint = TacticalTextMuted,
                                modifier = Modifier.clip(CircleShape).clickable { lines.remove(line) }.padding(6.dp).size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            SheetLabel("4. Детали")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (urgent) TacticalRedText.copy(alpha = 0.14f) else TacticalSurfaceLight)
                    .border(1.dp, if (urgent) TacticalRedText else TacticalBorder, RoundedCornerShape(14.dp))
                    .clickable { urgent = !urgent }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = if (urgent) TacticalRedText else TacticalTextMuted)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Срочно", color = TacticalTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("заявка будет первой в списке", color = TacticalTextMuted, fontSize = 11.sp)
                }
                Text(if (urgent) "ВКЛ" else "ВЫКЛ", color = if (urgent) TacticalRedText else TacticalTextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it.take(200) },
                placeholder = { Text("Комментарий (необязательно)", color = TacticalTextMuted) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = fieldColors()
            )

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val src = source ?: return@Button
                    val entries = lines.map { RequisitionItemEntry(it.item.name, it.qty.toIntOrNull()?.coerceAtLeast(1) ?: 1, it.item.unit) }
                    onSubmit(src.name, recipient.trim(), entries, (if (urgent) URGENT else "") + comment.trim())
                },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SageGreenPrimary, contentColor = Color.White)
            ) {
                Text(
                    if (canSubmit) "Отправить заявку (${lines.size} поз.)" else "Заполните склад, получателя и позиции",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SheetLabel(text: String) {
    Text(text, color = TacticalTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
}

@Composable
private fun ChipRow(options: List<String>, selected: String?, onPick: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            val isSel = option == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSel) SageGreenPrimary else TacticalSurfaceLight)
                    .border(1.dp, if (isSel) SageGreenBright else TacticalBorder, RoundedCornerShape(12.dp))
                    .clickable { onPick(option) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(option, color = if (isSel) Color.White else TacticalTextSecondary, fontSize = 13.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SageGreenBright,
    unfocusedBorderColor = TacticalBorder,
    focusedTextColor = TacticalTextPrimary,
    unfocusedTextColor = TacticalTextPrimary
)
