package com.example.data.repository

import com.example.BuildConfig

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
import com.example.data.sync.WarehouseSyncGateway
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
    private val syncManager: WarehouseSyncGateway? = null
) {

    val userProfile: Flow<UserProfile?> = dao.getUserProfile()
    val allPoints: Flow<List<WarehousePoint>> = dao.getAllPoints()
    val allItems: Flow<List<InventoryItem>> = dao.getAllItems()
    val allOperations: Flow<List<OperationRecord>> = dao.getAllOperations()
    val allRequisitions: Flow<List<RequisitionRequest>> = dao.getAllRequisitions()
    val allStockRecords: Flow<List<StockRecord>> = dao.getAllStockRecords()
    
    val syncEvents: kotlinx.coroutines.flow.SharedFlow<String>? = syncManager?.syncEvents

    private suspend fun getCurrentUnitKey(): String {
        return dao.getUserProfile().first()?.unitKey?.trim().orEmpty()
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

    suspend fun claimUnassignedUniversalItems(profileId: String) {
        if (!BuildConfig.IS_UNIVERSAL_APP || profileId.isBlank()) return

        val unassigned = dao.getUnassignedItems()
        if (unassigned.isEmpty()) return

        for (item in unassigned) {
            val candidates = com.example.universal.WarehouseProfileCatalog.profiles
                .filter { template -> item.serviceCategory in template.categories }
                .map { it.id }
                .distinct()

            val resolvedProfile = if (candidates.size == 1) {
                candidates.first()
            } else {
                profileId
            }

            val claimed = item.copy(profileId = resolvedProfile)
            dao.insertItem(claimed)
            syncManager?.pushInventoryItemAsync("", claimed)
        }
    }

    suspend fun ensureUniversalStarterCatalog(profileId: String): Int {
        if (!BuildConfig.IS_UNIVERSAL_APP) return 0

        claimUnassignedUniversalItems(profileId)

        val allItems = dao.getAllItems().first()
        val existing = allItems.filter { it.profileId == profileId }
        val starters = com.example.universal.WarehouseStarterCatalog.itemsFor(profileId)
        if (starters.isEmpty()) return 0

        var added = 0
        for (starter in starters) {
            val current = allItems.firstOrNull { it.id == starter.id }
            when {
                current == null -> {
                    dao.insertItem(starter)
                    syncManager?.pushInventoryItemAsync("", starter)
                    added += 1
                }
                !current.isCustom && current.name == current.subType -> {
                    // Upgrade old Alpha placeholder text without changing the item ID,
                    // so any existing stock remains attached to the same row.
                    dao.insertItem(
                        starter.copy(
                            standardCode = current.standardCode
                        )
                    )
                    syncManager?.pushInventoryItemAsync("", starter)
                }
            }
        }

        return added
    }

    suspend fun ensureInitialized() {
        val currentProfile = dao.getUserProfile().first()
        val activeProfile = if (currentProfile == null) {
            val defaultProfile = UserProfile(
                id = 1,
                callsign = "",
                unitName = "",
                unitKey = "",
                email = "",
                isLoggedIn = false,
                isProActive = false,
                demoDaysLeft = 3,
                proDaysLeft = 30,
                isOnline = true,
                onlineCount = 1
            )
            dao.saveUserProfile(defaultProfile)
            defaultProfile
        } else {
            currentProfile
        }

        // Launch online synchronization for this unit if unitKey is configured
        if (BuildConfig.IS_UNIVERSAL_APP || activeProfile.unitKey.isNotBlank()) {
            syncManager?.syncAndReconcileAll(
                activeProfile.unitKey,
                activeProfile.callsign,
                activeProfile.unitName
            )
        }

        // Add newly introduced standard items without overwriting existing user-edited rows.
        dao.insertItemsIfMissing(InitialData.getDefaultItems())

        val currentPoints = dao.getAllPoints().first()

        if (currentPoints.isEmpty()) {
            val defaults = InitialData.getDefaultPoints()
            dao.insertPoints(defaults)
            dao.insertItemsIfMissing(InitialData.getDefaultItems())
            if (activeProfile.unitKey.isNotBlank()) {
                defaults.forEach { p -> syncManager?.pushWarehousePointAsync(activeProfile.unitKey, p) }
            }
        }
    }
    


    suspend fun prepareUniversalCustomItemOwnership() {
        if (!BuildConfig.IS_UNIVERSAL_APP) return

        val items = dao.getAllItems().first()
        val stocks = dao.getAllStockRecords().first()

        items.asSequence()
            .filter { it.isCustom && it.warehouseId.isBlank() }
            .forEach { item ->
                val warehouseIds = stocks
                    .asSequence()
                    .filter {
                        it.itemId == item.id &&
                            (it.quantity != 0 || it.incomeTotal != 0 || it.expenseTotal != 0)
                    }
                    .map { it.pointId }
                    .distinct()
                    .toList()

                if (warehouseIds.size == 1) {
                    val claimed = item.copy(warehouseId = warehouseIds.first())
                    dao.insertItem(claimed)
                    syncManager?.pushInventoryItemAsync("", claimed)
                }
            }
    }

    suspend fun prepareUniversalWarehouses(defaultProfileId: String) {
        if (!BuildConfig.IS_UNIVERSAL_APP) return

        val safeProfile = com.example.universal.WarehouseProfileCatalog.find(defaultProfileId).id
        val current = dao.getAllPoints().first()
        if (current.isEmpty()) return

        current.forEach { point ->
            val updated = point.copy(
                profileId = point.profileId.ifBlank { safeProfile },
                syncKey = point.syncKey.ifBlank { generateWarehouseSyncKey() }
            )
            if (updated != point) {
                dao.updatePoint(updated)
                syncManager?.pushWarehousePointAsync("", updated)
            }
        }
    }

    private fun generateWarehouseSyncKey(): String {
        val raw = java.util.UUID.randomUUID()
            .toString()
            .replace("-", "")
            .uppercase(Locale.ROOT)
        return "SKL-" + raw.take(4) + "-" + raw.drop(4).take(4)
    }

    suspend fun deleteCategory(
        categoryName: String,
        deleteItems: Boolean = false,
        profileId: String = ""
    ) {
        if (deleteItems) {
            val itemsToDelete = if (BuildConfig.IS_UNIVERSAL_APP && profileId.isNotBlank()) {
                dao.getItemsByCategoryAndProfile(profileId, categoryName).first()
            } else {
                dao.getItemsByCategory(categoryName).first()
            }
            val unitKey = getCurrentUnitKey()
            for (item in itemsToDelete) {
                syncManager?.prepareDeletionTombstone(unitKey, "inventory_item", item.id)
            }
            if (BuildConfig.IS_UNIVERSAL_APP && profileId.isNotBlank()) {
                dao.deleteItemsByCategoryAndProfile(profileId, categoryName)
            } else {
                dao.deleteItemsByCategory(categoryName)
            }
            for (item in itemsToDelete) {
                dao.deleteStockForItem(item.id)
                syncManager?.deleteInventoryItemAsync(unitKey, item.id)
            }
        }
    }

    val syncState: kotlinx.coroutines.flow.StateFlow<com.example.data.sync.SyncState> = syncManager?.syncState ?: kotlinx.coroutines.flow.MutableStateFlow(com.example.data.sync.SyncState())

    suspend fun triggerCloudSync(): Pair<Boolean, String> {
        val p = dao.getUserProfile().first() ?: return Pair(false, "Профиль не найден")
        return syncManager?.syncAndReconcileAll(p.unitKey, p.callsign, p.unitName) ?: Pair(false, "Синхронизация отключена")
    }

    suspend fun clearAllData() {
        val unitKey = getCurrentUnitKey()

        // Full reset is an explicit synchronized deletion, not an inference from absence.
        // Persist tombstones first so another device cannot resurrect stale rows later.
        val stocks = dao.getAllStockRecords().first()
        val operations = dao.getAllOperations().first()
        val requisitions = dao.getAllRequisitions().first()

        for (stock in stocks) {
            syncManager?.prepareDeletionTombstone(
                unitKey,
                "stock_record",
                "${stock.pointId}:::${stock.itemId}"
            )
        }
        for (operation in operations) {
            syncManager?.prepareDeletionTombstone(unitKey, "operation", operation.id)
        }
        for (requisition in requisitions) {
            syncManager?.prepareDeletionTombstone(unitKey, "requisition", requisition.id)
        }

        dao.clearAllStockRecords()
        dao.clearAllOperations()
        dao.clearAllRequisitions()

        // Physical cloud cleanup is best-effort; tombstones remain authoritative even
        // if the network disappears before this asynchronous cleanup completes.
        syncManager?.clearCloudDataAsync(unitKey)
    }

    suspend fun clearLocalUnitData() {
        dao.clearAllStockRecords()
        dao.clearAllOperations()
        dao.clearAllRequisitions()
    }

    /**
     * A unit-key change is safe only when this installation has no unit-specific
     * working data. Default catalog/points do not count as user data.
     */
    suspend fun hasLocalUnitData(): Boolean {
        if (dao.getAllStockRecords().first().isNotEmpty()) return true
        if (dao.getAllOperations().first().isNotEmpty()) return true
        if (dao.getAllRequisitions().first().isNotEmpty()) return true

        val defaultPointIds = InitialData.getDefaultPoints().map { it.id }.toSet()
        if (dao.getAllPoints().first().any { it.id !in defaultPointIds }) return true

        if (dao.getAllItems().first().any { it.isCustom }) return true
        return false
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        dao.saveUserProfile(profile)
        if (BuildConfig.IS_UNIVERSAL_APP || profile.unitKey.isNotBlank()) {
            syncManager?.syncAndReconcileAll(
                profile.unitKey,
                profile.callsign,
                profile.unitName
            )
        }
    }

    suspend fun recordIncome(toPointId: String, toPointName: String, supplier: String, items: List<OperationItemEntry>, comment: String, actor: String) {
        val summary = items.joinToString(", ") { "${it.itemName} - ${it.quantity} ${it.unit}" }
        val itemsJson = serializeOperationItems(items)
        val src = supplier.ifBlank { "Служба снабжения / Тыл" }
        val dest = toPointName.ifBlank { "Базовый склад" }
        val op = OperationRecord(java.util.UUID.randomUUID().toString(), OperationType.INCOME, src, dest, "", actor, comment, System.currentTimeMillis(), summary, itemsJson)
        val stagedStocks = linkedMapOf<String, StockRecord>()
        for (item in items) {
            stageAdjustedStock(stagedStocks, toPointId, item.itemId, item.quantity, isIncome = true)
        }
        val updatedStocks = stagedStocks.values.toList()
        dao.commitOperationAndStocks(op, updatedStocks)
        syncManager?.pushOperationAsync(getCurrentUnitKey(), op, updatedStocks)
    }

    suspend fun recordTransfer(fromPointId: String, fromPointName: String, toPointId: String, toPointName: String, items: List<OperationItemEntry>, comment: String, actor: String) {
        val summary = items.joinToString(", ") { "${it.itemName} - ${it.quantity} ${it.unit}" }
        val itemsJson = serializeOperationItems(items)
        val op = OperationRecord(java.util.UUID.randomUUID().toString(), OperationType.TRANSFER, fromPointName, toPointName, "", actor, comment, System.currentTimeMillis(), summary, itemsJson)
        val stagedStocks = linkedMapOf<String, StockRecord>()
        for (item in items) {
            stageAdjustedStock(stagedStocks, fromPointId, item.itemId, -item.quantity, isIncome = false)
            stageAdjustedStock(stagedStocks, toPointId, item.itemId, item.quantity, isIncome = true)
        }
        val updatedStocks = stagedStocks.values.toList()
        dao.commitOperationAndStocks(op, updatedStocks)
        syncManager?.pushOperationAsync(getCurrentUnitKey(), op, updatedStocks)
    }

    suspend fun recordIssue(fromPointId: String, fromPointName: String, toPointId: String, toPointName: String, items: List<OperationItemEntry>, comment: String, actor: String) {
        val summary = items.joinToString(", ") { "${it.itemName} - ${it.quantity} ${it.unit}" }
        val itemsJson = serializeOperationItems(items)
        val op = OperationRecord(java.util.UUID.randomUUID().toString(), OperationType.ISSUE, fromPointName, toPointName, "", actor, comment, System.currentTimeMillis(), summary, itemsJson)
        val stagedStocks = linkedMapOf<String, StockRecord>()
        for (item in items) {
            stageAdjustedStock(stagedStocks, fromPointId, item.itemId, -item.quantity, isIncome = false)

            // Issue also increases the destination point in the same local transaction.
            stageAdjustedStock(stagedStocks, toPointId, item.itemId, item.quantity, isIncome = true)
        }
        val updatedStocks = stagedStocks.values.toList()
        dao.commitOperationAndStocks(op, updatedStocks)
        syncManager?.pushOperationAsync(getCurrentUnitKey(), op, updatedStocks)
    }

    suspend fun recordExpenditure(fromPointId: String, pointName: String, docNumber: String, responsiblePerson: String, items: List<OperationItemEntry>, comment: String) {
        val summary = items.joinToString(", ") { "${it.itemName} - ${it.quantity} ${it.unit}" }
        val itemsJson = serializeOperationItems(items)
        val destinationLabel = if (BuildConfig.IS_UNIVERSAL_APP) "Списание" else "Списание (ф. 8)"
        val op = OperationRecord(java.util.UUID.randomUUID().toString(), OperationType.EXPENDITURE, pointName, destinationLabel, docNumber, responsiblePerson, comment, System.currentTimeMillis(), summary, itemsJson)
        val stagedStocks = linkedMapOf<String, StockRecord>()
        for (item in items) {
            stageAdjustedStock(stagedStocks, fromPointId, item.itemId, -item.quantity, isIncome = false)
        }
        val updatedStocks = stagedStocks.values.toList()
        dao.commitOperationAndStocks(op, updatedStocks)
        syncManager?.pushOperationAsync(getCurrentUnitKey(), op, updatedStocks)
    }

    suspend fun addWarehousePoint(
        name: String,
        desc: String,
        profileId: String = ""
    ) {
        val resolvedProfile = if (BuildConfig.IS_UNIVERSAL_APP) {
            com.example.universal.WarehouseProfileCatalog.find(profileId).id
        } else {
            ""
        }
        val p = WarehousePoint(
            id = java.util.UUID.randomUUID().toString(),
            name = name,
            description = desc,
            profileId = resolvedProfile,
            syncKey = if (BuildConfig.IS_UNIVERSAL_APP) generateWarehouseSyncKey() else ""
        )
        dao.insertPoint(p)
        syncManager?.pushWarehousePointAsync(getCurrentUnitKey(), p)
    }

    suspend fun updateWarehousePoint(p: WarehousePoint) {
        dao.updatePoint(p)
        syncManager?.pushWarehousePointAsync(getCurrentUnitKey(), p)
    }

    suspend fun deleteWarehousePoint(id: String) {
        val unitKey = getCurrentUnitKey()
        syncManager?.prepareDeletionTombstone(unitKey, "warehouse_point", id)
        dao.deleteStockForPoint(id)
        dao.deletePoint(id)
        syncManager?.deleteWarehousePointAsync(unitKey, id)
    }

    suspend fun reorderWarehousePoints(orderedPoints: List<WarehousePoint>) {
        val updated = orderedPoints.mapIndexed { index, p ->
            p.copy(orderIndex = index)
        }
        dao.insertPoints(updated)
        val unitKey = getCurrentUnitKey()
        if (BuildConfig.IS_UNIVERSAL_APP || unitKey.isNotBlank()) {
            updated.forEach { p ->
                syncManager?.pushWarehousePointAsync(unitKey, p)
            }
        }
    }

    suspend fun addCustomInventoryItem(
        name: String,
        category: String,
        subCategory: String,
        unit: String,
        profileId: String = "",
        warehouseId: String = ""
    ) {
        val i = InventoryItem(
            id = java.util.UUID.randomUUID().toString(),
            name = name,
            serviceCategory = category,
            subType = subCategory,
            unit = unit,
            categoryClass = "Кат. 1",
            isCustom = true,
            profileId = profileId,
            warehouseId = warehouseId
        )
        dao.insertItem(i)
        syncManager?.pushInventoryItemAsync(getCurrentUnitKey(), i)
    }

    suspend fun updateInventoryItem(i: InventoryItem) {
        dao.insertItem(i)
        syncManager?.pushInventoryItemAsync(getCurrentUnitKey(), i)
    }

    suspend fun deleteInventoryItem(id: String) {
        val unitKey = getCurrentUnitKey()
        syncManager?.prepareDeletionTombstone(unitKey, "inventory_item", id)
        dao.deleteStockForItem(id)
        dao.deleteItem(id)
        syncManager?.deleteInventoryItemAsync(unitKey, id)
    }

    suspend fun createRequisition(pointName: String, applicant: String, items: List<RequisitionItemEntry>, comment: String) {
        val summary = items.joinToString(", ") { "${it.itemName} - ${it.quantity} ${it.unit}" }
        val itemsJson = serializeRequisitionItems(items)
        val r = RequisitionRequest(java.util.UUID.randomUUID().toString(), pointName, applicant, RequestStatus.PENDING, comment, System.currentTimeMillis(), summary, itemsJson)
        dao.insertRequisition(r)
        syncManager?.pushRequisitionAsync(getCurrentUnitKey(), r)
    }
    
    suspend fun updateRequisitionStatus(id: String, st: RequestStatus) {
        val r = dao.getAllRequisitions().first().find { it.id == id }
        if (r != null) {
            val updated = r.copy(status = st)
            dao.updateRequisition(updated)
            syncManager?.pushRequisitionAsync(getCurrentUnitKey(), updated)
        }
    }
    suspend fun deleteRequisition(id: String) {
        val unitKey = getCurrentUnitKey()
        syncManager?.prepareDeletionTombstone(unitKey, "requisition", id)
        dao.deleteRequisition(id)
        syncManager?.deleteRequisitionAsync(unitKey, id)
    }

    private suspend fun stageAdjustedStock(
        staged: MutableMap<String, StockRecord>,
        pointId: String,
        itemId: String,
        change: Int,
        isIncome: Boolean
    ): StockRecord {
        val key = "$pointId:::$itemId"
        val current = staged[key] ?: dao.getStockItem(pointId, itemId)
        val now = System.currentTimeMillis()
        val next = if (current != null) {
            current.copy(
                quantity = current.quantity + change,
                incomeTotal = current.incomeTotal + if (isIncome && change > 0) change else 0,
                expenseTotal = current.expenseTotal + if (!isIncome || change < 0) java.lang.Math.abs(change) else 0,
                lastUpdated = now
            )
        } else {
            StockRecord(
                pointId = pointId,
                itemId = itemId,
                quantity = change,
                incomeTotal = if (isIncome && change > 0) change else 0,
                expenseTotal = if (!isIncome || change < 0) java.lang.Math.abs(change) else 0,
                lastUpdated = now
            )
        }
        staged[key] = next
        return next
    }

    suspend fun adjustStockQuantity(pointId: String, itemId: String, change: Int, isIncome: Boolean = true): StockRecord {
        val current = dao.getStockItem(pointId, itemId)
        val newRecord = if (current != null) {
            current.copy(
                quantity = current.quantity + change,
                incomeTotal = current.incomeTotal + if(isIncome && change > 0) change else 0,
                expenseTotal = current.expenseTotal + if(!isIncome || change < 0) java.lang.Math.abs(change) else 0,
                lastUpdated = System.currentTimeMillis()
            )
        } else {
            StockRecord(pointId, itemId, change, if(isIncome && change > 0) change else 0, if(!isIncome || change < 0) java.lang.Math.abs(change) else 0, System.currentTimeMillis())
        }
        dao.insertOrUpdateStock(newRecord)
        syncManager?.pushStockRecordAsync(getCurrentUnitKey(), newRecord)
        return newRecord
    }

    suspend fun setStockAbsoluteQuantity(pointId: String, itemId: String, absoluteQuantity: Int): StockRecord {
        val current = dao.getStockItem(pointId, itemId)
        val newRecord = if (current != null) {
            current.copy(
                quantity = absoluteQuantity,
                lastUpdated = System.currentTimeMillis()
            )
        } else {
            StockRecord(
                pointId = pointId,
                itemId = itemId,
                quantity = absoluteQuantity,
                incomeTotal = absoluteQuantity,
                expenseTotal = 0,
                lastUpdated = System.currentTimeMillis()
            )
        }
        dao.insertOrUpdateStock(newRecord)
        syncManager?.pushStockRecordAsync(getCurrentUnitKey(), newRecord)
        return newRecord
    }

    fun generateForm8ExcelText(ops: List<OperationRecord>, unit: String): String = "Отчет"
    fun generateForm18ExcelText(ops: List<OperationRecord>, unit: String): String = "Отчет"
    
    private fun serializeOperationItems(items: List<OperationItemEntry>): String {
        val arr = org.json.JSONArray()
        items.forEach { 
            val obj = org.json.JSONObject()
            obj.put("itemId", it.itemId)
            obj.put("itemName", it.itemName)
            obj.put("unit", it.unit)
            obj.put("quantity", it.quantity)
            obj.put("categoryClass", it.categoryClass)
            obj.put("reason", it.reason)
            arr.put(obj)
        }
        return arr.toString()
    }
    
    fun parseOperationItems(json: String): List<OperationItemEntry> {
        if (json.isBlank()) return emptyList()
        return try {
            val list = mutableListOf<OperationItemEntry>()
            val arr = org.json.JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(OperationItemEntry(
                    itemId = obj.optString("itemId", ""),
                    itemName = obj.optString("itemName", ""),
                    unit = obj.optString("unit", ""),
                    quantity = obj.optInt("quantity", 0),
                    categoryClass = obj.optString("categoryClass", ""),
                    reason = obj.optString("reason", "")
                ))
            }
            list
        } catch (e: Exception) { emptyList() }
    }
    
    private fun serializeRequisitionItems(items: List<RequisitionItemEntry>): String {
        val arr = org.json.JSONArray()
        items.forEach { 
            val obj = org.json.JSONObject()
            obj.put("itemName", it.itemName)
            obj.put("unit", it.unit)
            obj.put("quantity", it.quantity)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun parseRequisitionItems(json: String): List<RequisitionItemEntry> {
        if (json.isBlank()) return emptyList()
        return try {
            val list = mutableListOf<RequisitionItemEntry>()
            val arr = org.json.JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(RequisitionItemEntry(
                    itemName = obj.optString("itemName", ""),
                    unit = obj.optString("unit", ""),
                    quantity = obj.optInt("quantity", 0)
                ))
            }
            list
        } catch (e: Exception) { emptyList() }
    }
}
