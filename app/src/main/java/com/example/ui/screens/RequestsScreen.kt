package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InventoryItem
import com.example.data.model.RequisitionItemEntry
import com.example.data.model.RequisitionRequest
import com.example.data.model.RequestStatus
import com.example.data.model.StockRecord
import com.example.data.model.UserProfile
import com.example.data.model.WarehousePoint
import com.example.ui.components.TacticalFitButton
import com.example.ui.components.TacticalInputField
import com.example.ui.components.TacticalSearchableItemDropdown
import com.example.ui.components.TacticalSearchablePointDropdown
import com.example.ui.components.TacticalSearchableTextDropdown
import com.example.ui.theme.SageGreenBright
import com.example.ui.theme.SageGreenContainer
import com.example.ui.theme.SageGreenDark
import com.example.ui.theme.SageGreenPrimary
import com.example.ui.theme.TacticalBg
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalBorderSubtle
import com.example.ui.theme.TacticalGold
import com.example.ui.theme.TacticalGoldDark
import com.example.ui.theme.TacticalGoldText
import com.example.ui.theme.TacticalRed
import com.example.ui.theme.TacticalRedDark
import com.example.ui.theme.TacticalRedText
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalSurfaceLight
import com.example.ui.theme.TacticalTextDim
import com.example.ui.theme.TacticalTextMuted
import com.example.ui.theme.TacticalTextPrimary
import com.example.ui.theme.TacticalTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RequisitionDraftItem(
    selectedItem: InventoryItem? = null,
    quantityString: String = "1"
) {
    var selectedItem by mutableStateOf(selectedItem)
    var quantityString by mutableStateOf(quantityString)
}

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
    parseItems: (String) -> List<RequisitionItemEntry>
) {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru"))
    var selectedPoint by remember {
        mutableStateOf(points.firstOrNull() ?: WarehousePoint("base", "Базовый склад"))
    }
    var applicantCallsign by remember {
        mutableStateOf(profile?.callsign ?: "")
    }
    var comment by remember { mutableStateOf("") }
    val draftItems = remember {
        mutableStateListOf(RequisitionDraftItem(selectedItem = null, quantityString = "1"))
    }

    var pendingWarningItems by remember { mutableStateOf<List<String>?>(null) }

    // Map total stock available across all warehouses/points
    val totalAvailableStocksMap = remember(stockRecords) {
        stockRecords.groupBy { it.itemId }.mapValues { (_, records) -> records.sumOf { it.quantity } }
    }

    val callsignPresets = listOf(
        "Командир 1-го взвода",
        "Командир 2-го взвода",
        "Старшина роты",
        "Оператор БПЛА «Сокол»",
        "Начальник связи",
        "Медик подразделения",
        "Командир расчета"
    )

    if (pendingWarningItems != null) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { pendingWarningItems = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, TacticalRed)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = TacticalRedText,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ПРЕДУПРЕЖДЕНИЕ: ОСТАТОК МЕНЬШЕ",
                            color = TacticalRedText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "На складе(ах) официально числится меньше имущества, чем указано в заявке:",
                        color = TacticalTextPrimary,
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(TacticalRedDark.copy(alpha = 0.4f))
                            .padding(8.dp)
                    ) {
                        pendingWarningItems!!.forEach { itemDesc ->
                            Text(
                                text = "• $itemDesc",
                                color = TacticalRedText,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TacticalFitButton(
                            text = "ИСПРАВИТЬ",
                            containerColor = TacticalSurfaceLight,
                            contentColor = TacticalTextPrimary,
                            onClick = { pendingWarningItems = null },
                            modifier = Modifier.weight(1f)
                        )
                        TacticalFitButton(
                            text = "ВСЁ РАВНО ОТПРАВИТЬ",
                            containerColor = TacticalRed,
                            contentColor = Color.White,
                            onClick = {
                                val validItems = draftItems.mapNotNull { d ->
                                    val itm = d.selectedItem ?: return@mapNotNull null
                                    val q = d.quantityString.toIntOrNull() ?: 1
                                    RequisitionItemEntry(itm.name, q, itm.unit)
                                }
                                if (validItems.isNotEmpty()) {
                                    onCreateRequisition(selectedPoint.name, applicantCallsign, validItems, comment)
                                    comment = ""
                                    pendingWarningItems = null
                                }
                            },
                            modifier = Modifier.weight(1.3f)
                        )
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(TacticalBg)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // Title & Description
        item {
            Column {
                Text(
                    text = "ЗАЯВКИ НА СНАБЖЕНИЕ",
                    color = SageGreenBright,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Новая заявка поступает на склад. При сборке переводится в статус «Собрана», затем «Выдана».",
                    color = TacticalTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // CARD: НОВАЯ ЗАЯВКА
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, TacticalBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(SageGreenDark),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assignment,
                                contentDescription = null,
                                tint = SageGreenBright,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ФОРМИРОВАНИЕ ЗАЯВКИ",
                            color = SageGreenBright,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Destination point searchable with stock summary
                    TacticalSearchablePointDropdown(
                        label = "Куда доставить / Назначение",
                        points = points,
                        selectedPoint = selectedPoint,
                        stockRecords = stockRecords,
                        catalogItems = catalogItems,
                        onPointSelected = { selectedPoint = it }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    TacticalSearchableTextDropdown(
                        label = "Позывной заявителя",
                        value = applicantCallsign,
                        onValueChange = { applicantCallsign = it },
                        suggestions = callsignPresets,
                        placeholder = "Позывной заявителя"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Draft items list
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ПОЗИЦИИ ЗАЯВКИ",
                            color = TacticalTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SageGreenDark)
                                .clickable {
                                    draftItems.add(
                                        RequisitionDraftItem(
                                            selectedItem = null,
                                            quantityString = "1"
                                        )
                                    )
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Добавить",
                                tint = SageGreenBright,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "+ позиция",
                                color = SageGreenBright,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    draftItems.forEachIndexed { index, draft ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = TacticalBg),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TacticalBorderSubtle)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Строка #${index + 1}",
                                        color = TacticalTextMuted,
                                        fontSize = 11.sp
                                    )
                                    if (draftItems.size > 1) {
                                        IconButton(
                                            onClick = { draftItems.removeAt(index) },
                                            modifier = Modifier.size(22.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Удалить",
                                                tint = TacticalRed,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Item Autocomplete Dropdown with total available stocks
                                TacticalSearchableItemDropdown(
                                    label = "",
                                    catalogItems = catalogItems,
                                    selectedItem = draft.selectedItem,
                                    availableStocksMap = totalAvailableStocksMap,
                                    onItemSelected = { selected ->
                                        draft.selectedItem = selected
                                    }
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = draft.quantityString,
                                        onValueChange = { draft.quantityString = it },
                                        label = { Text("Кол-во", color = TacticalTextSecondary, fontSize = 11.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = TacticalSurfaceLight,
                                            unfocusedContainerColor = TacticalSurfaceLight,
                                            focusedBorderColor = SageGreenPrimary,
                                            unfocusedBorderColor = TacticalBorder,
                                            focusedTextColor = TacticalTextPrimary,
                                            unfocusedTextColor = TacticalTextPrimary
                                        )
                                    )

                                    Text(
                                        text = draft.selectedItem?.unit ?: "ед.",
                                        color = SageGreenBright,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }

                                // Warning if requested quantity exceeds total warehouse stock
                                val selected = draft.selectedItem
                                val totalAvail = if (selected != null) totalAvailableStocksMap[selected.id] ?: 0 else null
                                val reqQty = draft.quantityString.toIntOrNull() ?: 0
                                if (totalAvail != null && reqQty > totalAvail) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(TacticalRedDark.copy(alpha = 0.45f))
                                            .border(0.5.dp, TacticalRed.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = TacticalRedText,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "⚠️ На складе нет столько (официально)! В наличии: $totalAvail ${selected?.unit ?: "ед."}",
                                            color = TacticalRedText,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    TacticalInputField(
                        label = "Комментарий (срочность, координаты, примечание)",
                        value = comment,
                        onValueChange = { comment = it },
                        placeholder = "Срочно для 2-го расчета / на вечерний рейс"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    TacticalFitButton(
                        text = "ОТПРАВИТЬ ЗАЯВКУ НА СКЛАД",
                        icon = Icons.Default.Send,
                        containerColor = SageGreenPrimary,
                        contentColor = Color.White,
                        onClick = {
                            val validItems = draftItems.mapNotNull { d ->
                                val itm = d.selectedItem ?: return@mapNotNull null
                                val q = d.quantityString.toIntOrNull() ?: 1
                                RequisitionItemEntry(itm.name, q, itm.unit)
                            }
                            if (validItems.isNotEmpty()) {
                                val insufficient = draftItems.mapNotNull { d ->
                                    val itm = d.selectedItem ?: return@mapNotNull null
                                    val q = d.quantityString.toIntOrNull() ?: 1
                                    val avail = totalAvailableStocksMap[itm.id] ?: 0
                                    if (q > avail) {
                                        "${itm.name}: на остатках $avail ${itm.unit}, запрошено: $q ${itm.unit}"
                                    } else null
                                }
                                if (insufficient.isNotEmpty()) {
                                    pendingWarningItems = insufficient
                                } else {
                                    onCreateRequisition(selectedPoint.name, applicantCallsign, validItems, comment)
                                    comment = ""
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("submit_requisition_button")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // SECTION: СПИСОК ЗАЯВОК
        item {
            Text(
                text = "СПИСОК ЗАЯВОК (${requisitions.size})",
                color = TacticalTextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (requisitions.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Assignment,
                        contentDescription = null,
                        tint = TacticalTextDim,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Активных заявок пока нет. Создайте первую заявку выше.",
                        color = TacticalTextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(requisitions, key = { it.id }) { req ->
                RequisitionCardItem(
                    req = req,
                    dateFormat = dateFormat,
                    onUpdateStatus = { nextStatus -> onUpdateStatus(req, nextStatus) },
                    onDelete = { onDeleteRequisition(req.id) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun RequisitionCardItem(
    req: RequisitionRequest,
    dateFormat: SimpleDateFormat,
    onUpdateStatus: (RequestStatus) -> Unit,
    onDelete: () -> Unit
) {
    val (statusColor, statusBg, statusIcon) = when (req.status) {
        RequestStatus.PENDING -> Triple(TacticalGoldText, TacticalGoldDark, Icons.Default.HourglassTop)
        RequestStatus.COLLECTED -> Triple(SageGreenBright, SageGreenDark, Icons.Default.CheckCircle)
        RequestStatus.ISSUED -> Triple(TacticalTextMuted, TacticalSurfaceLight, Icons.Default.DoneAll)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("requisition_card_${req.id}"),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = TacticalSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, TacticalBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Row 1: Point name & Status badge & Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(statusBg)
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = statusIcon,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = req.status.titleRu,
                                color = statusColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Точка: ${req.pointName}",
                        color = SageGreenBright,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Удалить заявку",
                        tint = TacticalTextDim,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Items
            Text(
                text = req.itemsSummary,
                color = TacticalTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            if (req.comment.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Примечание: ${req.comment}",
                    color = TacticalGoldText,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Footer: Date + applicant + Advance Status Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${dateFormat.format(Date(req.timestamp))} • ${req.applicantName}",
                    color = TacticalTextMuted,
                    fontSize = 11.sp
                )

                // Workflow status transition button
                val nextStatus = when (req.status) {
                    RequestStatus.PENDING -> RequestStatus.COLLECTED
                    RequestStatus.COLLECTED -> RequestStatus.ISSUED
                    RequestStatus.ISSUED -> RequestStatus.PENDING
                }
                val btnLabel = when (req.status) {
                    RequestStatus.PENDING -> "Отметить «Собрана»"
                    RequestStatus.COLLECTED -> "Отметить «Выдана»"
                    RequestStatus.ISSUED -> "В обработку"
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(TacticalSurfaceLight)
                        .border(1.dp, TacticalBorder, RoundedCornerShape(6.dp))
                        .clickable { onUpdateStatus(nextStatus) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = btnLabel,
                        color = SageGreenBright,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
