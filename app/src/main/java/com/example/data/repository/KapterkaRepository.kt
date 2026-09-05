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
            dao.deleteItemsByCategory(categoryName)
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
        val op = OperationRecord(UUID.randomUUID().toString(), OperationType.INCOME, supplier, toPointName, "", actor, comment, System.currentTimeMillis(), "", "")
        dao.insertOperation(op)
    }

    suspend fun recordTransfer(fromPointId: String, fromPointName: String, toPointId: String, toPointName: String, items: List<OperationItemEntry>, comment: String, actor: String) {
        val op = OperationRecord(UUID.randomUUID().toString(), OperationType.TRANSFER, fromPointName, toPointName, "", actor, comment, System.currentTimeMillis(), "", "")
        dao.insertOperation(op)
    }

    suspend fun recordIssue(fromPointId: String, fromPointName: String, toPointId: String, toPointName: String, items: List<OperationItemEntry>, comment: String, actor: String) {
        val op = OperationRecord(UUID.randomUUID().toString(), OperationType.ISSUE, fromPointName, toPointName, "", actor, comment, System.currentTimeMillis(), "", "")
        dao.insertOperation(op)
    }

    suspend fun recordExpenditure(fromPointId: String, pointName: String, docNumber: String, responsiblePerson: String, items: List<OperationItemEntry>, comment: String) {
        val op = OperationRecord(UUID.randomUUID().toString(), OperationType.EXPENDITURE, pointName, "Списание", docNumber, responsiblePerson, comment, System.currentTimeMillis(), "", "")
        dao.insertOperation(op)
    }

    suspend fun addWarehousePoint(name: String, desc: String) = dao.insertPoint(WarehousePoint(UUID.randomUUID().toString(), name, desc))

    suspend fun updateWarehousePoint(p: WarehousePoint) = dao.updatePoint(p)

    suspend fun deleteWarehousePoint(id: String) = dao.deletePoint(id)

    suspend fun addCustomInventoryItem(name: String, category: String, subCategory: String, unit: String) = dao.insertItem(InventoryItem(UUID.randomUUID().toString(), name, category, subCategory, unit, "Кат. 1"))

    suspend fun updateInventoryItem(i: InventoryItem) = dao.insertItem(i)

    suspend fun deleteInventoryItem(id: String) = dao.deleteItem(id)

    suspend fun createRequisition(pointName: String, applicant: String, items: List<RequisitionItemEntry>, comment: String) = dao.insertRequisition(RequisitionRequest(UUID.randomUUID().toString(), pointName, applicant, RequestStatus.PENDING, comment, System.currentTimeMillis(), "", ""))

    suspend fun updateRequisitionStatus(req: RequisitionRequest, st: RequestStatus) {
        // val r
        dao.updateRequisition(req.copy(status = st))
    }

    suspend fun deleteRequisition(id: String) = dao.deleteRequisition(id)

    suspend fun adjustStockQuantity(pointId: String, itemId: String, change: Int, isIncome: Boolean = true) {
        val current = dao.getStockItem(pointId, itemId)
        if (current != null) {
            dao.insertOrUpdateStock(current.copy(
                quantity = current.quantity + change,
                incomeTotal = current.incomeTotal + if(isIncome) change else 0,
                expenseTotal = current.expenseTotal + if(!isIncome) java.lang.Math.abs(change) else 0
            ))
        } else {
            dao.insertOrUpdateStock(StockRecord(pointId, itemId, change, if(isIncome) change else 0, if(!isIncome) java.lang.Math.abs(change) else 0))
        }
    }

    fun generateForm8ExcelText(ops: List<OperationRecord>, unit: String): String = "Отчет"
    fun generateForm18ExcelText(ops: List<OperationRecord>, unit: String): String = "Отчет"
    
    fun parseOperationItems(json: String): List<OperationItemEntry> = emptyList()
    fun parseRequisitionItems(json: String): List<RequisitionItemEntry> = emptyList()
}
