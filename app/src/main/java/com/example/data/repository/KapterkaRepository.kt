package com.example.data.repository

import com.example.data.local.InitialData
import com.example.data.local.KapterkaDao
import com.example.data.model.InventoryItem
import com.example.data.model.ItemWithStock
import com.example.data.model.OperationItemEntry
import com.example.data.model.OperationRecord
import com.example.data.model.OperationType
import com.example.data.model.RequisitionItemEntry
import com.example.data.model.RequisitionRequest
import com.example.data.model.RequestStatus
import com.example.data.model.StockRecord
import com.example.data.model.UserProfile
import com.example.data.model.WarehousePoint
import com.example.data.sync.FirebaseSyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class KapterkaRepository(
    private val dao: KapterkaDao,
    private val syncManager: FirebaseSyncManager? = null
) {

    val userProfile: Flow<UserProfile?> = dao.getUserProfile()
    val allPoints: Flow<List<WarehousePoint>> = dao.getAllPoints()
    val allItems: Flow<List<InventoryItem>> = dao.getAllItems()
    val allOperations: Flow<List<OperationRecord>> = dao.getAllOperations()
    val allRequisitions: Flow<List<RequisitionRequest>> = dao.getAllRequisitions()
    val allStockRecords: Flow<List<StockRecord>> = dao.getAllStockRecords()

    private suspend fun getCurrentUnitKey(): String {
        return dao.getUserProfile().first()?.unitKey ?: "kapt_59e13b"
    }

    fun getStockForPoint(pointId: String): Flow<List<StockRecord>> = dao.getStockForPoint(pointId)

    fun getItemsWithStockForPoint(pointId: String): Flow<List<ItemWithStock>> {
        return combine(dao.getAllItems(), dao.getStockForPoint(pointId)) { items, stocks ->
            val stockMap = stocks.associateBy { it.itemId }
            items.map { item ->
                val stock = stockMap[item.id]
                ItemWithStock(
                    item = item,
                    quantity = stock?.quantity ?: 0,
                    incomeTotal = stock?.incomeTotal ?: 0,
                    expenseTotal = stock?.expenseTotal ?: 0
                )
            }
        }
    }

    suspend fun ensureInitialized() {
        val currentProfile = dao.getUserProfile().first()
        val activeProfile = if (currentProfile == null) {
            val defaultProfile = UserProfile(
                id = 1,
                callsign = "лева",
                unitName = "1-е Подразделение",
                unitKey = "kapt_59e13b",
                email = "alex.666.881@gmail.com",
                isLoggedIn = true,
                isProActive = false,
                demoDaysLeft = 2,
                proDaysLeft = 29,
                isOnline = true,
                onlineCount = 1
            )
            dao.saveUserProfile(defaultProfile)
            defaultProfile
        } else {
            currentProfile
        }

        // Launch online synchronization for this unit
        syncManager?.startSyncForUnit(activeProfile.unitKey, activeProfile.callsign, activeProfile.unitName)

        val currentPoints = dao.getAllPoints().first()
        if (currentPoints.isEmpty()) {
            dao.insertPoints(InitialData.defaultPoints)
            dao.insertItems(InitialData.defaultItems)
            val baseStock = listOf(
                StockRecord("base_sklad", "rav_01", quantity = 48, incomeTotal = 48, expenseTotal = 0),
                StockRecord("base_sklad", "rav_03", quantity = 64, incomeTotal = 64, expenseTotal = 0),
                StockRecord("base_sklad", "rav_05", quantity = 12, incomeTotal = 12, expenseTotal = 0),
                StockRecord("base_sklad", "rav_09", quantity = 30, incomeTotal = 30, expenseTotal = 0),
                StockRecord("base_sklad", "bpla_01", quantity = 2, incomeTotal = 2, expenseTotal = 0),
                StockRecord("base_sklad", "bpla_03", quantity = 10, incomeTotal = 10, expenseTotal = 0),
                StockRecord("base_sklad", "reb_01", quantity = 6, incomeTotal = 6, expenseTotal = 0),
                StockRecord("base_sklad", "med_01", quantity = 25, incomeTotal = 25, expenseTotal = 0)
            )
            dao.insertOrUpdateStockList(baseStock)
        }
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        dao.saveUserProfile(profile)
        syncManager?.startSyncForUnit(profile.unitKey, profile.callsign, profile.unitName)
    }

    suspend fun addWarehousePoint(name: String, description: String = "") {
        val id = "point_" + UUID.randomUUID().toString().take(8)
        val point = WarehousePoint(
            id = id,
            name = name.trim(),
            description = description.trim(),
            isBase = false,
            orderIndex = 10
        )
        dao.insertPoint(point)
        syncManager?.pushWarehousePointAsync(getCurrentUnitKey(), point)
    }

    suspend fun deleteWarehousePoint(pointId: String) {
        dao.deletePoint(pointId)
        syncManager?.deleteWarehousePointAsync(getCurrentUnitKey(), pointId)
    }

    suspend fun updateWarehousePoint(point: WarehousePoint) {
        dao.updatePoint(point)
        syncManager?.pushWarehousePointAsync(getCurrentUnitKey(), point)
    }

    suspend fun addCustomInventoryItem(
        name: String,
        serviceCategory: String,
        subType: String,
        unit: String,
        standardCode: String = ""
    ) {
        val id = "item_custom_" + UUID.randomUUID().toString().take(8)
        val item = InventoryItem(
            id = id,
            name = name.trim(),
            serviceCategory = serviceCategory,
            subType = subType.ifEmpty { "Прочее" },
            unit = unit.ifEmpty { "шт." },
            categoryClass = "Кат. 1",
            standardCode = standardCode,
            isCustom = true
        )
        dao.insertItem(item)
        syncManager?.pushInventoryItemAsync(getCurrentUnitKey(), item)
    }

    suspend fun updateInventoryItem(item: InventoryItem) {
        dao.insertItem(item)
        syncManager?.pushInventoryItemAsync(getCurrentUnitKey(), item)
    }

    suspend fun deleteInventoryItem(itemId: String) {
        dao.deleteItem(itemId)
        syncManager?.deleteInventoryItemAsync(getCurrentUnitKey(), itemId)
    }

    suspend fun adjustStockQuantity(pointId: String, itemId: String, newQuantity: Int) {
        val existing = dao.getStockItem(pointId, itemId)
        val recordToSave = if (existing != null) {
            existing.copy(quantity = newQuantity, lastUpdated = System.currentTimeMillis())
        } else {
            StockRecord(
                pointId = pointId,
                itemId = itemId,
                quantity = newQuantity,
                incomeTotal = newQuantity,
                expenseTotal = 0,
                lastUpdated = System.currentTimeMillis()
            )
        }
        dao.insertOrUpdateStock(recordToSave)
        // Push stock adjust to cloud
        val op = OperationRecord(
            id = "adj_" + UUID.randomUUID().toString().take(10),
            type = OperationType.INCOME,
            fromPointName = "Корректировка остатка",
            toPointName = pointId,
            docNumber = "КОР-${System.currentTimeMillis().toString().takeLast(4)}",
            responsiblePerson = "Инвентаризация",
            comment = "Ручная корректировка: $newQuantity",
            timestamp = System.currentTimeMillis(),
            itemsSummary = "Остаток установлен: $newQuantity",
            itemsJson = ""
        )
        syncManager?.pushOperationAsync(getCurrentUnitKey(), op, listOf(recordToSave))
    }

    // OPERATIONS: «Привезли» (Income)
    suspend fun recordIncome(
        toPointId: String,
        toPointName: String,
        supplier: String,
        items: List<OperationItemEntry>,
        comment: String,
        actor: String
    ) {
        val summary = items.joinToString(", ") { "${it.itemName} (${it.quantity} ${it.unit})" }
        val json = serializeItems(items)
        val op = OperationRecord(
            id = "op_" + UUID.randomUUID().toString().take(10),
            type = OperationType.INCOME,
            fromPointName = supplier.ifEmpty { "Служба РАВ / Тыл" },
            toPointName = toPointName,
            docNumber = "ПР-${System.currentTimeMillis().toString().takeLast(4)}",
            responsiblePerson = actor.ifEmpty { "Ответственный" },
            comment = comment,
            timestamp = System.currentTimeMillis(),
            itemsSummary = summary,
            itemsJson = json
        )
        dao.insertOperation(op)

        // Update stocks
        val updatedStocks = mutableListOf<StockRecord>()
        for (item in items) {
            val current = dao.getStockItem(toPointId, item.itemId)
            val currentQty = current?.quantity ?: 0
            val currentIncome = current?.incomeTotal ?: 0
            val s = StockRecord(
                pointId = toPointId,
                itemId = item.itemId,
                quantity = currentQty + item.quantity,
                incomeTotal = currentIncome + item.quantity,
                expenseTotal = current?.expenseTotal ?: 0,
                lastUpdated = System.currentTimeMillis()
            )
            dao.insertOrUpdateStock(s)
            updatedStocks.add(s)
        }
        syncManager?.pushOperationAsync(getCurrentUnitKey(), op, updatedStocks)
    }

    // OPERATIONS: «Перемещение» (Transfer)
    suspend fun recordTransfer(
        fromPointId: String,
        fromPointName: String,
        toPointId: String,
        toPointName: String,
        items: List<OperationItemEntry>,
        comment: String,
        actor: String
    ) {
        val summary = items.joinToString(", ") { "${it.itemName} (${it.quantity} ${it.unit})" }
        val json = serializeItems(items)
        val op = OperationRecord(
            id = "op_" + UUID.randomUUID().toString().take(10),
            type = OperationType.TRANSFER,
            fromPointName = fromPointName,
            toPointName = toPointName,
            docNumber = "ТР-${System.currentTimeMillis().toString().takeLast(4)}",
            responsiblePerson = actor.ifEmpty { "Ответственный" },
            comment = comment,
            timestamp = System.currentTimeMillis(),
            itemsSummary = summary,
            itemsJson = json
        )
        dao.insertOperation(op)

        // Decrease from source, increase to destination
        val updatedStocks = mutableListOf<StockRecord>()
        for (item in items) {
            val fromStock = dao.getStockItem(fromPointId, item.itemId)
            val fromQty = (fromStock?.quantity ?: 0) - item.quantity
            val sFrom = StockRecord(
                pointId = fromPointId,
                itemId = item.itemId,
                quantity = if (fromQty < 0) 0 else fromQty,
                incomeTotal = fromStock?.incomeTotal ?: 0,
                expenseTotal = (fromStock?.expenseTotal ?: 0) + item.quantity,
                lastUpdated = System.currentTimeMillis()
            )
            dao.insertOrUpdateStock(sFrom)
            updatedStocks.add(sFrom)

            val toStock = dao.getStockItem(toPointId, item.itemId)
            val toQty = (toStock?.quantity ?: 0) + item.quantity
            val sTo = StockRecord(
                pointId = toPointId,
                itemId = item.itemId,
                quantity = toQty,
                incomeTotal = (toStock?.incomeTotal ?: 0) + item.quantity,
                expenseTotal = toStock?.expenseTotal ?: 0,
                lastUpdated = System.currentTimeMillis()
            )
            dao.insertOrUpdateStock(sTo)
            updatedStocks.add(sTo)
        }
        syncManager?.pushOperationAsync(getCurrentUnitKey(), op, updatedStocks)
    }

    // OPERATIONS: «Подняли» (Issue)
    suspend fun recordIssue(
        fromPointId: String,
        fromPointName: String,
        toPointId: String,
        toPointName: String,
        items: List<OperationItemEntry>,
        comment: String,
        actor: String
    ) {
        val summary = items.joinToString(", ") { "${it.itemName} (${it.quantity} ${it.unit})" }
        val json = serializeItems(items)
        val op = OperationRecord(
            id = "op_" + UUID.randomUUID().toString().take(10),
            type = OperationType.ISSUE,
            fromPointName = fromPointName,
            toPointName = toPointName.ifEmpty { "ОП (Огневая позиция)" },
            docNumber = "ПД-${System.currentTimeMillis().toString().takeLast(4)}",
            responsiblePerson = actor.ifEmpty { "Старшина" },
            comment = comment,
            timestamp = System.currentTimeMillis(),
            itemsSummary = summary,
            itemsJson = json
        )
        dao.insertOperation(op)

        val updatedStocks = mutableListOf<StockRecord>()
        for (item in items) {
            val fromStock = dao.getStockItem(fromPointId, item.itemId)
            val fromQty = (fromStock?.quantity ?: 0) - item.quantity
            val sFrom = StockRecord(
                pointId = fromPointId,
                itemId = item.itemId,
                quantity = if (fromQty < 0) 0 else fromQty,
                incomeTotal = fromStock?.incomeTotal ?: 0,
                expenseTotal = (fromStock?.expenseTotal ?: 0) + item.quantity,
                lastUpdated = System.currentTimeMillis()
            )
            dao.insertOrUpdateStock(sFrom)
            updatedStocks.add(sFrom)

            // Increase to destination if it's a valid point (not empty)
            if (toPointId.isNotEmpty()) {
                val toStock = dao.getStockItem(toPointId, item.itemId)
                val toQty = (toStock?.quantity ?: 0) + item.quantity
                val sTo = StockRecord(
                    pointId = toPointId,
                    itemId = item.itemId,
                    quantity = toQty,
                    incomeTotal = (toStock?.incomeTotal ?: 0) + item.quantity,
                    expenseTotal = toStock?.expenseTotal ?: 0,
                    lastUpdated = System.currentTimeMillis()
                )
                dao.insertOrUpdateStock(sTo)
                updatedStocks.add(sTo)
            }
        }
        syncManager?.pushOperationAsync(getCurrentUnitKey(), op, updatedStocks)
    }

    // OPERATIONS: «Отстрел» (Expenditure Form 8)
    suspend fun recordExpenditure(
        fromPointId: String,
        pointName: String,
        docNumber: String,
        responsiblePerson: String,
        items: List<OperationItemEntry>,
        comment: String
    ) {
        val summary = items.joinToString(", ") { "${it.itemName} (${it.quantity} ${it.unit})" }
        val json = serializeItems(items)
        val op = OperationRecord(
            id = "op_" + UUID.randomUUID().toString().take(10),
            type = OperationType.EXPENDITURE,
            fromPointName = pointName,
            toPointName = "Расход (Акт списания)",
            docNumber = docNumber.ifEmpty { "АКТ-${System.currentTimeMillis().toString().takeLast(4)}" },
            responsiblePerson = responsiblePerson.ifEmpty { "Командир расчета" },
            comment = comment,
            timestamp = System.currentTimeMillis(),
            itemsSummary = summary,
            itemsJson = json
        )
        dao.insertOperation(op)

        val updatedStocks = mutableListOf<StockRecord>()
        for (item in items) {
            val fromStock = dao.getStockItem(fromPointId, item.itemId)
            val fromQty = (fromStock?.quantity ?: 0) - item.quantity
            val s = StockRecord(
                pointId = fromPointId,
                itemId = item.itemId,
                quantity = if (fromQty < 0) 0 else fromQty,
                incomeTotal = fromStock?.incomeTotal ?: 0,
                expenseTotal = (fromStock?.expenseTotal ?: 0) + item.quantity,
                lastUpdated = System.currentTimeMillis()
            )
            dao.insertOrUpdateStock(s)
            updatedStocks.add(s)
        }
        syncManager?.pushOperationAsync(getCurrentUnitKey(), op, updatedStocks)
    }

    // REQUISITIONS
    suspend fun createRequisition(
        pointName: String,
        applicantName: String,
        items: List<RequisitionItemEntry>,
        comment: String
    ) {
        val summary = items.joinToString(", ") { "${it.itemName} — ${it.quantity} ${it.unit}" }
        val json = serializeRequisitionItems(items)
        val req = RequisitionRequest(
            id = "req_" + UUID.randomUUID().toString().take(8),
            pointName = pointName,
            applicantName = applicantName,
            status = RequestStatus.PENDING,
            comment = comment,
            timestamp = System.currentTimeMillis(),
            itemsSummary = summary,
            itemsJson = json
        )
        dao.insertRequisition(req)
        syncManager?.pushRequisitionAsync(getCurrentUnitKey(), req)
    }

    suspend fun updateRequisitionStatus(requisition: RequisitionRequest, newStatus: RequestStatus) {
        val updated = requisition.copy(status = newStatus)
        dao.updateRequisition(updated)
        syncManager?.pushRequisitionAsync(getCurrentUnitKey(), updated)
    }

    suspend fun deleteRequisition(requisitionId: String) {
        dao.deleteRequisition(requisitionId)
    }

    suspend fun deleteCategory(category: String, deleteAssociatedItems: Boolean = true) {
        if (deleteAssociatedItems) {
            dao.deleteItemsByCategory(category)
        }
    }

    // JSON Serializers & Parsers
    private fun serializeItems(items: List<OperationItemEntry>): String {
        val arr = JSONArray()
        for (item in items) {
            val obj = JSONObject()
            obj.put("itemId", item.itemId)
            obj.put("itemName", item.itemName)
            obj.put("unit", item.unit)
            obj.put("quantity", item.quantity)
            obj.put("categoryClass", item.categoryClass)
            obj.put("reason", item.reason)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun parseOperationItems(json: String): List<OperationItemEntry> {
        if (json.isEmpty()) return emptyList()
        val list = mutableListOf<OperationItemEntry>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    OperationItemEntry(
                        itemId = obj.optString("itemId", ""),
                        itemName = obj.optString("itemName", ""),
                        unit = obj.optString("unit", "шт."),
                        quantity = obj.optInt("quantity", 0),
                        categoryClass = obj.optString("categoryClass", "Кат. 1"),
                        reason = obj.optString("reason", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun serializeRequisitionItems(items: List<RequisitionItemEntry>): String {
        val arr = JSONArray()
        for (item in items) {
            val obj = JSONObject()
            obj.put("itemName", item.itemName)
            obj.put("quantity", item.quantity)
            obj.put("unit", item.unit)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun parseRequisitionItems(json: String): List<RequisitionItemEntry> {
        if (json.isEmpty()) return emptyList()
        val list = mutableListOf<RequisitionItemEntry>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    RequisitionItemEntry(
                        itemName = obj.optString("itemName", ""),
                        quantity = obj.optInt("quantity", 0),
                        unit = obj.optString("unit", "шт.")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    // EXCEL / REPORT STRING BUILDERS (For Intent sharing)
    fun generateForm8ExcelText(operations: List<OperationRecord>, unitName: String): String {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru"))
        val sb = StringBuilder()
        sb.append("УТВЕРЖДАЮ\n")
        sb.append("Командир $unitName\n")
        sb.append("АКТ СПИСАНИЯ (РАСХОДА) МАТЕРИАЛЬНЫХ ЦЕННОСТЕЙ (ФОРМА № 8)\n\n")
        sb.append("№ п/п\tНаименование имущества\tКатегория\tЕд. изм.\tКоличество\tПричина расхода / Цель\tДата / № Документа\n")

        var index = 1
        val expOps = operations.filter { it.type == OperationType.EXPENDITURE }
        for (op in expOps) {
            val items = parseOperationItems(op.itemsJson)
            val dateStr = dateFormat.format(Date(op.timestamp))
            if (items.isNotEmpty()) {
                for (item in items) {
                    val reason = if (item.reason.isNotEmpty()) item.reason else (if (op.comment.isNotEmpty()) op.comment else "Боевая работа")
                    sb.append("$index\t${item.itemName}\t${item.categoryClass}\t${item.unit}\t${item.quantity}\t$reason\t$dateStr (${op.docNumber})\n")
                    index++
                }
            } else {
                sb.append("$index\t${op.itemsSummary}\tКат. 1\tшт.\t-\t${op.comment.ifEmpty { "Боевая работа" }}\t$dateStr (${op.docNumber})\n")
                index++
            }
        }
        if (expOps.isEmpty()) {
            sb.append("1\tМина 120-мм ОФ-843Б\tКат. 1\tшт.\t24\tПодавление опорного пункта противника\t01.09.2026 (АКТ-104)\n")
            sb.append("2\tВыстрелы ВОГ-17М\tКат. 1\tуп.\t5\tОгневое прикрытие группы\t01.09.2026 (АКТ-104)\n")
        }
        sb.append("\nОтветственное лицо: ________________ / ________\n")
        return sb.toString()
    }

    fun generateForm18ExcelText(operations: List<OperationRecord>, unitName: String): String {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("ru"))
        val sb = StringBuilder()
        sb.append("КНИГА УЧЕТА НАЛИЧИЯ И ДВИЖЕНИЯ МАТЕРИАЛЬНЫХ СРЕДСТВ (ФОРМА № 18)\n")
        sb.append("Подразделение: $unitName\n\n")
        sb.append("Дата\tНаименование документа\tОт кого получено / Кому выдано\tПриход\tРасход\tОстаток\n")

        var rollingStock = 100
        for (op in operations) {
            val dateStr = dateFormat.format(Date(op.timestamp))
            val doc = "${op.type.titleRu} № ${op.docNumber}"
            val parties = "${op.fromPointName} ➔ ${op.toPointName}"
            val items = parseOperationItems(op.itemsJson)
            val totalQty = items.sumOf { it.quantity }
            val (prihod, rashod) = when (op.type) {
                OperationType.INCOME -> {
                    rollingStock += totalQty
                    "$totalQty (${op.itemsSummary})" to "-"
                }
                OperationType.EXPENDITURE, OperationType.ISSUE -> {
                    rollingStock -= totalQty
                    "-" to "$totalQty (${op.itemsSummary})"
                }
                OperationType.TRANSFER -> {
                    "$totalQty" to "$totalQty (${op.itemsSummary})"
                }
            }
            sb.append("$dateStr\t$doc\t$parties\t$prihod\t$rashod\t$rollingStock\n")
        }
        return sb.toString()
    }

    suspend fun clearAllData() {
        dao.clearAllOperations()
        dao.clearAllRequisitions()
        dao.clearAllStockRecords()
    }

    val syncState = syncManager?.syncState

    suspend fun triggerCloudSync() {
        val key = getCurrentUnitKey()
        val profile = dao.getUserProfile().first()
        if (profile != null) {
            syncManager?.startSyncForUnit(profile.unitKey, profile.callsign, profile.unitName)
        }
        syncManager?.pushAllLocalData(key)
    }
}
