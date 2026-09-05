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
                callsign = "",
                unitName = "",
                unitKey = "kapt_" + java.util.UUID.randomUUID().toString().take(6),
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

        // Launch online synchronization for this unit only if user is logged in
        if (activeProfile.isLoggedIn && activeProfile.unitKey.isNotBlank()) {
            syncManager?.startSyncForUnit(activeProfile.unitKey, activeProfile.callsign, activeProfile.unitName)
        }

        
        // Always try to insert default items to ensure updates like new ammo are present (ConflictStrategy is REPLACE/IGNORE)
        dao.insertItems(InitialData.getDefaultItems())

        val currentPoints = dao.getAllPoints().first()

        if (currentPoints.isEmpty()) {
            dao.insertPoints(InitialData.getDefaultPoints())
            dao.insertItems(InitialData.getDefaultItems())
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
    


    suspend fun deleteCategory(categoryName: String, deleteItems: Boolean = false) {
        if (deleteItems) {
            val itemsToDelete = dao.getItemsByCategory(categoryName).first()
            dao.deleteItemsByCategory(categoryName)
            val unitKey = getCurrentUnitKey()
            for (item in itemsToDelete) {
                syncManager?.deleteInventoryItemAsync(unitKey, item.id)
            }
        }
    }

    val syncState: kotlinx.coroutines.flow.StateFlow<com.example.data.sync.SyncState> = syncManager?.syncState ?: kotlinx.coroutines.flow.MutableStateFlow(com.example.data.sync.SyncState())

    suspend fun triggerCloudSync() {
        val p = dao.getUserProfile().first() ?: return
        syncManager?.startSyncForUnit(p.unitKey, p.callsign, p.unitName)
    }

    suspend fun clearAllData() {
        dao.clearAllStockRecords()
        dao.clearAllOperations()
        dao.clearAllRequisitions()
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        dao.saveUserProfile(profile)
    }

    suspend fun recordIncome(toPointId: String, toPointName: String, supplier: String, items: List<OperationItemEntry>, comment: String, actor: String) {
        val summary = items.joinToString(", ") { "${it.itemName} - ${it.quantity} ${it.unit}" }
        val itemsJson = serializeOperationItems(items)
        val op = OperationRecord(java.util.UUID.randomUUID().toString(), OperationType.INCOME, supplier, toPointName, "", actor, comment, System.currentTimeMillis(), summary, itemsJson)
        dao.insertOperation(op)
        val updatedStocks = mutableListOf<StockRecord>()
        for (item in items) {
            updatedStocks.add(adjustStockQuantity(toPointId, item.itemId, item.quantity, isIncome = true))
        }
        syncManager?.pushOperationAsync(getCurrentUnitKey(), op, updatedStocks)
    }

    suspend fun recordTransfer(fromPointId: String, fromPointName: String, toPointId: String, toPointName: String, items: List<OperationItemEntry>, comment: String, actor: String) {
        val summary = items.joinToString(", ") { "${it.itemName} - ${it.quantity} ${it.unit}" }
        val itemsJson = serializeOperationItems(items)
        val op = OperationRecord(java.util.UUID.randomUUID().toString(), OperationType.TRANSFER, fromPointName, toPointName, "", actor, comment, System.currentTimeMillis(), summary, itemsJson)
        dao.insertOperation(op)
        val updatedStocks = mutableListOf<StockRecord>()
        for (item in items) {
            updatedStocks.add(adjustStockQuantity(fromPointId, item.itemId, -item.quantity, isIncome = false))
            updatedStocks.add(adjustStockQuantity(toPointId, item.itemId, item.quantity, isIncome = true))
        }
        syncManager?.pushOperationAsync(getCurrentUnitKey(), op, updatedStocks)
    }

    suspend fun recordIssue(fromPointId: String, fromPointName: String, toPointId: String, toPointName: String, items: List<OperationItemEntry>, comment: String, actor: String) {
        val summary = items.joinToString(", ") { "${it.itemName} - ${it.quantity} ${it.unit}" }
        val itemsJson = serializeOperationItems(items)
        val op = OperationRecord(java.util.UUID.randomUUID().toString(), OperationType.ISSUE, fromPointName, toPointName, "", actor, comment, System.currentTimeMillis(), summary, itemsJson)
        dao.insertOperation(op)
        val updatedStocks = mutableListOf<StockRecord>()
        for (item in items) {
            updatedStocks.add(adjustStockQuantity(fromPointId, item.itemId, -item.quantity, isIncome = false))
            // No destination stock update because it's issued to soldiers (off-balance)
        }
        syncManager?.pushOperationAsync(getCurrentUnitKey(), op, updatedStocks)
    }

    suspend fun recordExpenditure(fromPointId: String, pointName: String, docNumber: String, responsiblePerson: String, items: List<OperationItemEntry>, comment: String) {
        val summary = items.joinToString(", ") { "${it.itemName} - ${it.quantity} ${it.unit}" }
        val itemsJson = serializeOperationItems(items)
        val op = OperationRecord(java.util.UUID.randomUUID().toString(), OperationType.EXPENDITURE, pointName, "Списание", docNumber, responsiblePerson, comment, System.currentTimeMillis(), summary, itemsJson)
        dao.insertOperation(op)
        val updatedStocks = mutableListOf<StockRecord>()
        for (item in items) {
            updatedStocks.add(adjustStockQuantity(fromPointId, item.itemId, -item.quantity, isIncome = false))
        }
        syncManager?.pushOperationAsync(getCurrentUnitKey(), op, updatedStocks)
    }

    suspend fun addWarehousePoint(name: String, desc: String) {
        val p = WarehousePoint(java.util.UUID.randomUUID().toString(), name, desc)
        dao.insertPoint(p)
        syncManager?.pushWarehousePointAsync(getCurrentUnitKey(), p)
    }

    suspend fun updateWarehousePoint(p: WarehousePoint) {
        dao.updatePoint(p)
        syncManager?.pushWarehousePointAsync(getCurrentUnitKey(), p)
    }

    suspend fun deleteWarehousePoint(id: String) {
        dao.deletePoint(id)
        syncManager?.deleteWarehousePointAsync(getCurrentUnitKey(), id)
    }

    suspend fun addCustomInventoryItem(name: String, category: String, subCategory: String, unit: String) {
        val i = InventoryItem(java.util.UUID.randomUUID().toString(), name, category, subCategory, unit, "Кат. 1")
        dao.insertItem(i)
        syncManager?.pushInventoryItemAsync(getCurrentUnitKey(), i)
    }

    suspend fun updateInventoryItem(i: InventoryItem) {
        dao.insertItem(i)
        syncManager?.pushInventoryItemAsync(getCurrentUnitKey(), i)
    }

    suspend fun deleteInventoryItem(id: String) {
        dao.deleteItem(id)
        syncManager?.deleteInventoryItemAsync(getCurrentUnitKey(), id)
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
        dao.deleteRequisition(id)
        syncManager?.deleteRequisitionAsync(getCurrentUnitKey(), id)
    }

    suspend fun adjustStockQuantity(pointId: String, itemId: String, change: Int, isIncome: Boolean = true): StockRecord {
        val current = dao.getStockItem(pointId, itemId)
        val newRecord = if (current != null) {
            current.copy(
                quantity = current.quantity + change,
                incomeTotal = current.incomeTotal + if(isIncome && change > 0) change else 0,
                expenseTotal = current.expenseTotal + if(!isIncome || change < 0) java.lang.Math.abs(change) else 0
            )
        } else {
            StockRecord(pointId, itemId, change, if(isIncome && change > 0) change else 0, if(!isIncome || change < 0) java.lang.Math.abs(change) else 0)
        }
        dao.insertOrUpdateStock(newRecord)
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
