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
import com.google.firebase.firestore.DocumentChange
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
import kotlinx.coroutines.tasks.await
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
    private val firestore: FirebaseFirestore?
        get() = FirebaseSafeHelper.getFirestore(context)

    private val _syncState = MutableStateFlow(SyncState())
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _syncEvents = kotlinx.coroutines.flow.MutableSharedFlow<String>()
    val syncEvents: kotlinx.coroutines.flow.SharedFlow<String> = _syncEvents

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
            val db = firestore
            if (db == null) {
                _syncState.value = _syncState.value.copy(
                    isSyncing = false,
                    isOnline = false,
                    syncMessage = "Локальный режим: облачная база не настроена"
                )
                return
            }

            registerUnitListeners(cleanKey)
            sendPresencePing(cleanKey, callsign, unitName)

            // Проверяем облачную базу: если подразделение уже существует в облаке,
            // мы берём данные из облака, а не перезаписываем чужую базу начальными шаблонами.
            // Если в облаке пусто - инициализируем базу подразделения.
            scope.launch(Dispatchers.IO) {
                try {
                    val unitPointsSnapshot = db.collection("units")
                        .document(cleanKey)
                        .collection("warehouse_points")
                        .limit(1)
                        .get()
                        .await()

                    if (unitPointsSnapshot.isEmpty) {
                        pushAllLocalData(cleanKey)
                    } else {
                        _syncState.value = _syncState.value.copy(
                            isSyncing = false,
                            isOnline = true,
                            lastSyncTime = System.currentTimeMillis(),
                            syncMessage = "Подключено к действующей базе подразделения"
                        )
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error checking remote unit existence", e)
                    _syncState.value = _syncState.value.copy(
                        isSyncing = false,
                        isOnline = false,
                        syncMessage = "Режим офлайн / сеть недоступна"
                    )
                }
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
        val db = firestore ?: return
        val unitRef = db.collection("units").document(unitKey)
        var isFirstOpLoad = true

        // 1. Warehouse Points listener (с обработкой добавлений, правок и удалений)
        val pointsListener = unitRef.collection("warehouse_points")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Points listener error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    scope.launch(Dispatchers.IO) {
                        for (change in snapshot.documentChanges) {
                            val doc = change.document
                            val id = doc.getString("id") ?: doc.id
                            when (change.type) {
                                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                    val name = doc.getString("name") ?: continue
                                    val description = doc.getString("description") ?: ""
                                    val isBase = doc.getBoolean("isBase") ?: false
                                    val orderIndex = (doc.getLong("orderIndex") ?: 0L).toInt()
                                    val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                                    dao.insertPoint(WarehousePoint(id, name, description, isBase, orderIndex, createdAt))
                                }
                                DocumentChange.Type.REMOVED -> {
                                    dao.deletePoint(id)
                                    dao.deleteStockForPoint(id)
                                }
                            }
                        }
                    }
                }
            }
        listeners.add(pointsListener)

        // 2. Inventory Items (Catalog) listener (с обработкой добавлений, правок и удалений)
        val itemsListener = unitRef.collection("inventory_items")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Items listener error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    scope.launch(Dispatchers.IO) {
                        for (change in snapshot.documentChanges) {
                            val doc = change.document
                            val id = doc.getString("id") ?: doc.id
                            when (change.type) {
                                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                    val name = doc.getString("name") ?: continue
                                    val category = doc.getString("serviceCategory") ?: "Общие"
                                    val subType = doc.getString("subType") ?: ""
                                    val unit = doc.getString("unit") ?: "шт."
                                    val categoryClass = doc.getString("categoryClass") ?: "Кат. 1"
                                    val standardCode = doc.getString("standardCode") ?: ""
                                    val isCustom = doc.getBoolean("isCustom") ?: false
                                    dao.insertItem(InventoryItem(id, name, category, subType, unit, categoryClass, standardCode, isCustom))
                                }
                                DocumentChange.Type.REMOVED -> {
                                    dao.deleteItem(id)
                                    dao.deleteStockForItem(id)
                                }
                            }
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
                if (snapshot != null) {
                    scope.launch(Dispatchers.IO) {
                        for (change in snapshot.documentChanges) {
                            val doc = change.document
                            val pointId = doc.getString("pointId") ?: continue
                            val itemId = doc.getString("itemId") ?: continue
                            when (change.type) {
                                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                    val quantity = (doc.getLong("quantity") ?: 0L).toInt()
                                    val incomeTotal = (doc.getLong("incomeTotal") ?: 0L).toInt()
                                    val expenseTotal = (doc.getLong("expenseTotal") ?: 0L).toInt()
                                    val lastUpdated = doc.getLong("lastUpdated") ?: System.currentTimeMillis()
                                    dao.insertOrUpdateStock(StockRecord(pointId, itemId, quantity, incomeTotal, expenseTotal, lastUpdated))
                                }
                                DocumentChange.Type.REMOVED -> {
                                    dao.deleteStockRecord(pointId, itemId)
                                }
                            }
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
                if (snapshot != null) {
                    val isInitial = isFirstOpLoad
                    isFirstOpLoad = false
                    
                    scope.launch(Dispatchers.IO) {
                        for (change in snapshot.documentChanges) {
                            val doc = change.document
                            val id = doc.getString("id") ?: doc.id
                            
                            if (change.type == DocumentChange.Type.ADDED && !isInitial && !doc.metadata.hasPendingWrites()) {
                                val typeStr = doc.getString("type") ?: ""
                                val resp = doc.getString("responsiblePerson") ?: ""
                                val opName = when(typeStr) { "INCOME" -> "Приход"; "EXPENDITURE" -> "Списание"; "ISSUE" -> "Выдача"; "TRANSFER" -> "Перемещение"; else -> "Операция" }
                                _syncEvents.emit("☁️ Новая операция от [$resp]: $opName")
                            }
                            
                            when (change.type) {
                                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                    try {
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
                                DocumentChange.Type.REMOVED -> {
                                    dao.deleteOperation(id)
                                }
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
                if (snapshot != null) {
                    scope.launch(Dispatchers.IO) {
                        for (change in snapshot.documentChanges) {
                            val doc = change.document
                            val id = doc.getString("id") ?: doc.id
                            when (change.type) {
                                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                    try {
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
                                DocumentChange.Type.REMOVED -> {
                                    dao.deleteRequisition(id)
                                }
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
        val db = firestore ?: return
        val unitRef = db.collection("units").document(unitKey)
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
        val db = firestore ?: return
        _syncState.value = _syncState.value.copy(isSyncing = true, syncMessage = "Отправка локальных данных в облако...")
        val unitRef = db.collection("units").document(unitKey)

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
            try {
                val db = firestore ?: return@launch
                val unitRef = db.collection("units").document(unitKey)
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
                Log.w(TAG, "Failed pushing op live, saved locally", e)
            }
        }
    }

    fun pushStockRecordAsync(unitKey: String, s: StockRecord) {
        if (unitKey.isEmpty()) return
        scope.launch(Dispatchers.IO) {
            try {
                val db = firestore ?: return@launch
                val docId = "${s.pointId}___${s.itemId}"
                db.collection("units").document(unitKey)
                    .collection("stock_records").document(docId).set(
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
            } catch (e: Exception) {
                Log.w(TAG, "Failed pushing stock record live", e)
            }
        }
    }

    fun pushRequisitionAsync(unitKey: String, r: RequisitionRequest) {
        if (unitKey.isEmpty()) return
        scope.launch(Dispatchers.IO) {
            try {
                val db = firestore ?: return@launch
                db.collection("units").document(unitKey)
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

    fun deleteRequisitionAsync(unitKey: String, reqId: String) {
        if (unitKey.isEmpty()) return
        scope.launch(Dispatchers.IO) {
            try {
                val db = firestore ?: return@launch
                db.collection("units").document(unitKey)
                    .collection("requisitions").document(reqId).delete()
            } catch (e: Exception) {
                Log.w(TAG, "Failed deleting req live", e)
            }
        }
    }

    fun deleteOperationAsync(unitKey: String, opId: String) {
        if (unitKey.isEmpty()) return
        scope.launch(Dispatchers.IO) {
            try {
                val db = firestore ?: return@launch
                db.collection("units").document(unitKey)
                    .collection("operation_records").document(opId).delete()
            } catch (e: Exception) {
                Log.w(TAG, "Failed deleting operation live", e)
            }
        }
    }

    fun pushWarehousePointAsync(unitKey: String, p: WarehousePoint) {
        if (unitKey.isEmpty()) return
        scope.launch(Dispatchers.IO) {
            try {
                val db = firestore ?: return@launch
                db.collection("units").document(unitKey)
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
                val db = firestore ?: return@launch
                db.collection("units").document(unitKey)
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
                val db = firestore ?: return@launch
                db.collection("units").document(unitKey)
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
                val db = firestore ?: return@launch
                db.collection("units").document(unitKey)
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

    fun clearCloudDataAsync(unitKey: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val cleanKey = unitKey.trim()
                if (cleanKey.isEmpty()) return@launch
                val db = firestore ?: return@launch
                val unitRef = db.collection("units").document(cleanKey)
                val collectionsToClear = listOf("stock_records", "operation_records", "requisitions")
                for (col in collectionsToClear) {
                    val snapshot = unitRef.collection(col).get().await()
                    for (doc in snapshot.documents) {
                        doc.reference.delete().await()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing cloud data", e)
            }
        }
    }
}
