package com.example.data.sync

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.data.local.KapterkaDao
import com.example.data.model.InventoryItem
import com.example.data.model.OperationRecord
import com.example.data.model.OperationType
import com.example.data.model.RequisitionRequest
import com.example.data.model.RequestStatus
import com.example.data.model.StockRecord
import com.example.data.model.WarehousePoint
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

data class SyncState(
    val isSyncing: Boolean = false,
    val lastSyncTime: Long = 0L,
    val isOnline: Boolean = true,
    val connectedDevicesCount: Int = 1,
    val syncMessage: String = "Готов к синхронизации"
)

class FirebaseSyncManager(
    private val context: Context,
    private val dao: KapterkaDao,
    private val scope: CoroutineScope
) {
    private val TAG = "KapterkaSync"
    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    private val _syncState = MutableStateFlow(SyncState())
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val deviceId: String by lazy {
        val prefs = context.getSharedPreferences("kapterka_sync_prefs", Context.MODE_PRIVATE)
        var id = prefs.getString("device_uuid", null)
        if (id == null) {
            id = "dev_" + UUID.randomUUID().toString().take(8)
            prefs.edit().putString("device_uuid", id).apply()
        }
        id
    }

    private var activeUnitKey: String = ""
    private var listeners = mutableListOf<ListenerRegistration>()

    fun startSyncForUnit(unitKey: String, callsign: String, unitName: String) {
        val cleanKey = unitKey.trim()
        if (cleanKey.isEmpty()) return

        if (activeUnitKey == cleanKey && listeners.isNotEmpty()) {
            // Already listening for this unit, just send presence ping
            sendPresencePing(cleanKey, callsign, unitName)
            return
        }

        stopSync()
        activeUnitKey = cleanKey
        _syncState.value = _syncState.value.copy(
            isSyncing = true,
            syncMessage = "Подключение к каналу подразделения [$cleanKey]..."
        )

        try {
            registerUnitListeners(cleanKey)
            sendPresencePing(cleanKey, callsign, unitName)
            // Push initial local state so cloud has complete data
            scope.launch(Dispatchers.IO) {
                pushAllLocalData(cleanKey)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting sync", e)
            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                isOnline = false,
                syncMessage = "Ошибка подключения: ${e.localizedMessage ?: "Нет сети"}"
            )
        }
    }

    private fun registerUnitListeners(unitKey: String) {
        val unitRef = firestore.collection("units").document(unitKey)

        // 1. Warehouse Points listener
        val pointsListener = unitRef.collection("warehouse_points")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Points listener error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    val points = snapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.getString("id") ?: doc.id
                            val name = doc.getString("name") ?: return@mapNotNull null
                            val description = doc.getString("description") ?: ""
                            val isBase = doc.getBoolean("isBase") ?: false
                            val orderIndex = (doc.getLong("orderIndex") ?: 0L).toInt()
                            val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                            WarehousePoint(id, name, description, isBase, orderIndex, createdAt)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    if (points.isNotEmpty()) {
                        scope.launch(Dispatchers.IO) {
                            dao.insertPoints(points)
                        }
                    }
                }
            }
        listeners.add(pointsListener)

        // 2. Inventory Items (Catalog) listener
        val itemsListener = unitRef.collection("inventory_items")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Items listener error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    val items = snapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.getString("id") ?: doc.id
                            val name = doc.getString("name") ?: return@mapNotNull null
                            val category = doc.getString("serviceCategory") ?: "Общие"
                            val subType = doc.getString("subType") ?: ""
                            val unit = doc.getString("unit") ?: "шт."
                            val categoryClass = doc.getString("categoryClass") ?: "Кат. 1"
                            val standardCode = doc.getString("standardCode") ?: ""
                            val isCustom = doc.getBoolean("isCustom") ?: false
                            InventoryItem(id, name, category, subType, unit, categoryClass, standardCode, isCustom)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    if (items.isNotEmpty()) {
                        scope.launch(Dispatchers.IO) {
                            dao.insertItems(items)
                        }
                    }
                }
            }
        listeners.add(itemsListener)

        // 3. Stock Records listener
        val stockListener = unitRef.collection("stock_records")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Stock listener error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    val records = snapshot.documents.mapNotNull { doc ->
                        try {
                            val pointId = doc.getString("pointId") ?: return@mapNotNull null
                            val itemId = doc.getString("itemId") ?: return@mapNotNull null
                            val quantity = (doc.getLong("quantity") ?: 0L).toInt()
                            val incomeTotal = (doc.getLong("incomeTotal") ?: 0L).toInt()
                            val expenseTotal = (doc.getLong("expenseTotal") ?: 0L).toInt()
                            val lastUpdated = doc.getLong("lastUpdated") ?: System.currentTimeMillis()
                            StockRecord(pointId, itemId, quantity, incomeTotal, expenseTotal, lastUpdated)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    if (records.isNotEmpty()) {
                        scope.launch(Dispatchers.IO) {
                            dao.insertOrUpdateStockList(records)
                        }
                    }
                }
            }
        listeners.add(stockListener)

        // 4. Operation Records (History) listener
        val opListener = unitRef.collection("operation_records")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Operations listener error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    scope.launch(Dispatchers.IO) {
                        for (doc in snapshot.documents) {
                            try {
                                val id = doc.getString("id") ?: doc.id
                                val typeStr = doc.getString("type") ?: "INCOME"
                                val type = try { OperationType.valueOf(typeStr) } catch (_: Exception) { OperationType.INCOME }
                                val fromPoint = doc.getString("fromPointName") ?: ""
                                val toPoint = doc.getString("toPointName") ?: ""
                                val docNum = doc.getString("docNumber") ?: ""
                                val resp = doc.getString("responsiblePerson") ?: ""
                                val comm = doc.getString("comment") ?: ""
                                val time = doc.getLong("timestamp") ?: System.currentTimeMillis()
                                val summary = doc.getString("itemsSummary") ?: ""
                                val json = doc.getString("itemsJson") ?: ""

                                val op = OperationRecord(id, type, fromPoint, toPoint, docNum, resp, comm, time, summary, json)
                                dao.insertOperation(op)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing operation doc", e)
                            }
                        }
                        _syncState.value = _syncState.value.copy(
                            lastSyncTime = System.currentTimeMillis(),
                            isOnline = true,
                            isSyncing = false,
                            syncMessage = "Синхронизировано в режиме онлайн"
                        )
                    }
                }
            }
        listeners.add(opListener)

        // 5. Requisition Requests listener
        val reqListener = unitRef.collection("requisitions")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Requisition listener error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    scope.launch(Dispatchers.IO) {
                        for (doc in snapshot.documents) {
                            try {
                                val id = doc.getString("id") ?: doc.id
                                val pointName = doc.getString("pointName") ?: ""
                                val applicant = doc.getString("applicantName") ?: ""
                                val statusStr = doc.getString("status") ?: "PENDING"
                                val status = try { RequestStatus.valueOf(statusStr) } catch (_: Exception) { RequestStatus.PENDING }
                                val comm = doc.getString("comment") ?: ""
                                val time = doc.getLong("timestamp") ?: System.currentTimeMillis()
                                val summary = doc.getString("itemsSummary") ?: ""
                                val json = doc.getString("itemsJson") ?: ""

                                val req = RequisitionRequest(id, pointName, applicant, status, comm, time, summary, json)
                                dao.insertRequisition(req)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing requisition doc", e)
                            }
                        }
                    }
                }
            }
        listeners.add(reqListener)

        // 6. Active Devices / Presence listener
        val presenceListener = unitRef.collection("devices")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val now = System.currentTimeMillis()
                    val activeCount = snapshot.documents.count { doc ->
                        val lastSeen = doc.getLong("timestampMillis") ?: 0L
                        (now - lastSeen) < (15 * 60 * 1000) // 15 mins window
                    }
                    _syncState.value = _syncState.value.copy(
                        connectedDevicesCount = if (activeCount > 0) activeCount else 1,
                        isOnline = true
                    )
                }
            }
        listeners.add(presenceListener)
    }

    private fun sendPresencePing(unitKey: String, callsign: String, unitName: String) {
        val unitRef = firestore.collection("units").document(unitKey)
        val data = hashMapOf(
            "deviceId" to deviceId,
            "callsign" to callsign,
            "unitName" to unitName,
            "deviceModel" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "timestamp" to FieldValue.serverTimestamp(),
            "timestampMillis" to System.currentTimeMillis()
        )
        unitRef.collection("devices").document(deviceId)
            .set(data, SetOptions.merge())

        // Also update unit meta
        unitRef.set(
            hashMapOf(
                "unitKey" to unitKey,
                "unitName" to unitName,
                "lastActivity" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        )
    }

    suspend fun pushAllLocalData(unitKey: String) {
        if (unitKey.isEmpty()) return
        _syncState.value = _syncState.value.copy(isSyncing = true, syncMessage = "Отправка локальных данных в облако...")
        val unitRef = firestore.collection("units").document(unitKey)

        try {
            // Push points
            val points = dao.getAllPoints().first()
            for (p in points) {
                unitRef.collection("warehouse_points").document(p.id).set(
                    hashMapOf(
                        "id" to p.id,
                        "name" to p.name,
                        "description" to p.description,
                        "isBase" to p.isBase,
                        "orderIndex" to p.orderIndex,
                        "createdAt" to p.createdAt
                    ),
                    SetOptions.merge()
                )
            }

            // Push catalog items
            val items = dao.getAllItems().first()
            for (item in items) {
                unitRef.collection("inventory_items").document(item.id).set(
                    hashMapOf(
                        "id" to item.id,
                        "name" to item.name,
                        "serviceCategory" to item.serviceCategory,
                        "subType" to item.subType,
                        "unit" to item.unit,
                        "categoryClass" to item.categoryClass,
                        "standardCode" to item.standardCode,
                        "isCustom" to item.isCustom
                    ),
                    SetOptions.merge()
                )
            }

            // Push stocks
            val stocks = dao.getAllStockRecords().first()
            for (s in stocks) {
                val docId = "${s.pointId}___${s.itemId}"
                unitRef.collection("stock_records").document(docId).set(
                    hashMapOf(
                        "pointId" to s.pointId,
                        "itemId" to s.itemId,
                        "quantity" to s.quantity,
                        "incomeTotal" to s.incomeTotal,
                        "expenseTotal" to s.expenseTotal,
                        "lastUpdated" to s.lastUpdated
                    ),
                    SetOptions.merge()
                )
            }

            // Push operations
            val ops = dao.getAllOperations().first()
            for (op in ops) {
                unitRef.collection("operation_records").document(op.id).set(
                    hashMapOf(
                        "id" to op.id,
                        "type" to op.type.name,
                        "fromPointName" to op.fromPointName,
                        "toPointName" to op.toPointName,
                        "docNumber" to op.docNumber,
                        "responsiblePerson" to op.responsiblePerson,
                        "comment" to op.comment,
                        "timestamp" to op.timestamp,
                        "itemsSummary" to op.itemsSummary,
                        "itemsJson" to op.itemsJson
                    ),
                    SetOptions.merge()
                )
            }

            // Push requisitions
            val reqs = dao.getAllRequisitions().first()
            for (r in reqs) {
                unitRef.collection("requisitions").document(r.id).set(
                    hashMapOf(
                        "id" to r.id,
                        "pointName" to r.pointName,
                        "applicantName" to r.applicantName,
                        "status" to r.status.name,
                        "comment" to r.comment,
                        "timestamp" to r.timestamp,
                        "itemsSummary" to r.itemsSummary,
                        "itemsJson" to r.itemsJson
                    ),
                    SetOptions.merge()
                )
            }

            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                isOnline = true,
                lastSyncTime = System.currentTimeMillis(),
                syncMessage = "База подразделения синхронизирована (онлайн)"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error pushing local data", e)
            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                syncMessage = "Данные сохранены локально (ожидание сети)"
            )
        }
    }

    fun pushOperationAsync(unitKey: String, op: OperationRecord, updatedStocks: List<StockRecord>) {
        if (unitKey.isEmpty()) return
        scope.launch(Dispatchers.IO) {
            val unitRef = firestore.collection("units").document(unitKey)
            try {
                unitRef.collection("operation_records").document(op.id).set(
                    hashMapOf(
                        "id" to op.id,
                        "type" to op.type.name,
                        "fromPointName" to op.fromPointName,
                        "toPointName" to op.toPointName,
                        "docNumber" to op.docNumber,
                        "responsiblePerson" to op.responsiblePerson,
                        "comment" to op.comment,
                        "timestamp" to op.timestamp,
                        "itemsSummary" to op.itemsSummary,
                        "itemsJson" to op.itemsJson
                    ),
                    SetOptions.merge()
                )

                for (s in updatedStocks) {
                    val docId = "${s.pointId}___${s.itemId}"
                    unitRef.collection("stock_records").document(docId).set(
                        hashMapOf(
                            "pointId" to s.pointId,
                            "itemId" to s.itemId,
                            "quantity" to s.quantity,
                            "incomeTotal" to s.incomeTotal,
                            "expenseTotal" to s.expenseTotal,
                            "lastUpdated" to s.lastUpdated
                        ),
                        SetOptions.merge()
                    )
                }

                _syncState.value = _syncState.value.copy(
                    lastSyncTime = System.currentTimeMillis(),
                    isOnline = true,
                    syncMessage = "Операция синхронизирована"
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed pushing op live, saved in Firestore cache", e)
            }
        }
    }

    fun pushRequisitionAsync(unitKey: String, r: RequisitionRequest) {
        if (unitKey.isEmpty()) return
        scope.launch(Dispatchers.IO) {
            try {
                firestore.collection("units").document(unitKey)
                    .collection("requisitions").document(r.id).set(
                        hashMapOf(
                            "id" to r.id,
                            "pointName" to r.pointName,
                            "applicantName" to r.applicantName,
                            "status" to r.status.name,
                            "comment" to r.comment,
                            "timestamp" to r.timestamp,
                            "itemsSummary" to r.itemsSummary,
                            "itemsJson" to r.itemsJson
                        ),
                        SetOptions.merge()
                    )
            } catch (e: Exception) {
                Log.w(TAG, "Failed pushing req live", e)
            }
        }
    }

    fun pushWarehousePointAsync(unitKey: String, p: WarehousePoint) {
        if (unitKey.isEmpty()) return
        scope.launch(Dispatchers.IO) {
            try {
                firestore.collection("units").document(unitKey)
                    .collection("warehouse_points").document(p.id).set(
                        hashMapOf(
                            "id" to p.id,
                            "name" to p.name,
                            "description" to p.description,
                            "isBase" to p.isBase,
                            "orderIndex" to p.orderIndex,
                            "createdAt" to p.createdAt
                        ),
                        SetOptions.merge()
                    )
            } catch (e: Exception) {
                Log.w(TAG, "Failed pushing point live", e)
            }
        }
    }

    fun deleteWarehousePointAsync(unitKey: String, pointId: String) {
        if (unitKey.isEmpty()) return
        scope.launch(Dispatchers.IO) {
            try {
                firestore.collection("units").document(unitKey)
                    .collection("warehouse_points").document(pointId).delete()
            } catch (e: Exception) {
                Log.w(TAG, "Failed deleting point live", e)
            }
        }
    }

    fun pushInventoryItemAsync(unitKey: String, item: InventoryItem) {
        if (unitKey.isEmpty()) return
        scope.launch(Dispatchers.IO) {
            try {
                firestore.collection("units").document(unitKey)
                    .collection("inventory_items").document(item.id).set(
                        hashMapOf(
                            "id" to item.id,
                            "name" to item.name,
                            "serviceCategory" to item.serviceCategory,
                            "subType" to item.subType,
                            "unit" to item.unit,
                            "categoryClass" to item.categoryClass,
                            "standardCode" to item.standardCode,
                            "isCustom" to item.isCustom
                        ),
                        SetOptions.merge()
                    )
            } catch (e: Exception) {
                Log.w(TAG, "Failed pushing item live", e)
            }
        }
    }

    fun deleteInventoryItemAsync(unitKey: String, itemId: String) {
        if (unitKey.isEmpty()) return
        scope.launch(Dispatchers.IO) {
            try {
                firestore.collection("units").document(unitKey)
                    .collection("inventory_items").document(itemId).delete()
            } catch (e: Exception) {
                Log.w(TAG, "Failed deleting item live", e)
            }
        }
    }

    fun stopSync() {
        for (l in listeners) {
            try {
                l.remove()
            } catch (_: Exception) {}
        }
        listeners.clear()
    }
}
