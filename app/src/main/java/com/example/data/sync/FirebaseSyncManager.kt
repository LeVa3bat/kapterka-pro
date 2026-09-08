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
    private val firestore: FirebaseFirestore
        by lazy { FirebaseFirestore.getInstance() }

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

        
            registerUnitListeners(unitKey)
        sendPresencePing(cleanKey, callsign, unitName)
        
        _syncState.value = _syncState.value.copy(
            isSyncing = false,
            isOnline = true,
            syncMessage = "Подключено к подразделению"
        )
    }

    private fun registerUnitListeners(unitKey: String) {
        val db = firestore
        val unitRef = db.collection("units").document(unitKey)

        val pointsReg = unitRef.collection("warehouse_points").addSnapshotListener { snap, e ->
            if (e != null || snap == null) return@addSnapshotListener
            scope.launch(Dispatchers.IO) {
                for (dc in snap.documentChanges) {
                    val p = WarehousePoint(
                        id = dc.document.id,
                        name = dc.document.getString("name") ?: "",
                        description = dc.document.getString("description") ?: "",
                        isBase = dc.document.getBoolean("isBase") ?: false,
                        orderIndex = dc.document.getLong("orderIndex")?.toInt() ?: 0,
                        createdAt = dc.document.getLong("createdAt") ?: 0L
                    )
                    if (dc.type == DocumentChange.Type.REMOVED) {
                        dao.deletePoint(p.id)
                    } else {
                        dao.insertPoint(p)
                    }
                }
            }
        }
        listeners.add(pointsReg)

        val itemsReg = unitRef.collection("inventory_items").addSnapshotListener { snap, e ->
            if (e != null || snap == null) return@addSnapshotListener
            scope.launch(Dispatchers.IO) {
                for (dc in snap.documentChanges) {
                    val item = InventoryItem(
                        id = dc.document.id,
                        name = dc.document.getString("name") ?: "",
                        serviceCategory = dc.document.getString("serviceCategory") ?: "",
                        subType = dc.document.getString("subType") ?: "",
                        unit = dc.document.getString("unit") ?: "шт.",
                        categoryClass = dc.document.getString("categoryClass") ?: "Кат. 1",
                        standardCode = dc.document.getString("standardCode") ?: "",
                        isCustom = dc.document.getBoolean("isCustom") ?: false
                    )
                    if (dc.type == DocumentChange.Type.REMOVED) {
                        dao.deleteItem(item.id)
                    } else {
                        dao.insertItem(item)
                    }
                }
            }
        }
        listeners.add(itemsReg)

        val stockReg = unitRef.collection("stock_records").addSnapshotListener { snap, e ->
            if (e != null || snap == null) return@addSnapshotListener
            scope.launch(Dispatchers.IO) {
                for (dc in snap.documentChanges) {
                    val s = StockRecord(
                        pointId = dc.document.getString("pointId") ?: "",
                        itemId = dc.document.getString("itemId") ?: "",
                        quantity = dc.document.getLong("quantity")?.toInt() ?: 0,
                        incomeTotal = dc.document.getLong("incomeTotal")?.toInt() ?: 0,
                        expenseTotal = dc.document.getLong("expenseTotal")?.toInt() ?: 0,
                        lastUpdated = dc.document.getLong("lastUpdated") ?: 0L
                    )
                    if (dc.type != DocumentChange.Type.REMOVED) {
                        dao.insertOrUpdateStock(s)
                    }
                }
            }
        }
        listeners.add(stockReg)

        
        var isFirstOpLoad = true
        val opReg = unitRef.collection("operation_records").addSnapshotListener { snap, e ->
            if (e != null || snap == null) return@addSnapshotListener
            scope.launch(Dispatchers.IO) {
                for (dc in snap.documentChanges) {
                    val opTypeStr = dc.document.getString("type") ?: "INCOME"
                    val type = try { OperationType.valueOf(opTypeStr) } catch (ex: Exception) { OperationType.INCOME }
                    val op = OperationRecord(
                        id = dc.document.id,
                        type = type,
                        fromPointName = dc.document.getString("fromPointName") ?: "",
                        toPointName = dc.document.getString("toPointName") ?: "",
                        docNumber = dc.document.getString("docNumber") ?: "",
                        responsiblePerson = dc.document.getString("responsiblePerson") ?: "",
                        comment = dc.document.getString("comment") ?: "",
                        timestamp = dc.document.getLong("timestamp") ?: 0L,
                        itemsSummary = dc.document.getString("itemsSummary") ?: "",
                        itemsJson = dc.document.getString("itemsJson") ?: ""
                    )
                    if (dc.type == DocumentChange.Type.REMOVED) {
                        dao.deleteOperation(op.id)
                    } else {
                        dao.insertOperation(op)
                        if (!isFirstOpLoad && dc.type == DocumentChange.Type.ADDED) {
                            _syncEvents.emit("Новая операция: ${op.type.name} (Док. ${op.docNumber})")
                        }
                    }
                }
                isFirstOpLoad = false
            }
        }

        listeners.add(opReg)

        val reqReg = unitRef.collection("requisitions").addSnapshotListener { snap, e ->
            if (e != null || snap == null) return@addSnapshotListener
            scope.launch(Dispatchers.IO) {
                for (dc in snap.documentChanges) {
                    val statusStr = dc.document.getString("status") ?: "PENDING"
                    val status = try { RequestStatus.valueOf(statusStr) } catch (ex: Exception) { RequestStatus.PENDING }
                    val req = RequisitionRequest(
                        id = dc.document.id,
                        pointName = dc.document.getString("pointName") ?: "",
                        applicantName = dc.document.getString("applicantName") ?: "",
                        status = status,
                        comment = dc.document.getString("comment") ?: "",
                        timestamp = dc.document.getLong("timestamp") ?: 0L,
                        itemsSummary = dc.document.getString("itemsSummary") ?: "",
                        itemsJson = dc.document.getString("itemsJson") ?: ""
                    )
                    if (dc.type == DocumentChange.Type.REMOVED) {
                        dao.deleteRequisition(req.id)
                    } else {
                        dao.insertRequisition(req)
                    }
                }
            }
        }
        listeners.add(reqReg)
        
        val presReg = unitRef.collection("devices").addSnapshotListener { snap, e ->
            if (e != null || snap == null) return@addSnapshotListener
            _syncState.value = _syncState.value.copy(connectedDevicesCount = snap.documents.size)
        }
        listeners.add(presReg)
    }

    private fun sendPresencePing(unitKey: String, callsign: String, unitName: String) {
        val db = firestore
        val unitRef = db.collection("units").document(unitKey)
        val data = hashMapOf(
            "deviceId" to deviceId,
            "callsign" to callsign,
            "unitName" to unitName,
            "deviceModel" to (Build.MANUFACTURER + " " + Build.MODEL),
            "timestamp" to FieldValue.serverTimestamp(),
            "timestampMillis" to System.currentTimeMillis()
        )
        unitRef.collection("devices").document(deviceId).set(data, SetOptions.merge())
        val uData = hashMapOf(
            "unitKey" to unitKey,
            "unitName" to unitName,
            "lastActivity" to FieldValue.serverTimestamp()
        )
        unitRef.set(uData, SetOptions.merge())
    }

    suspend fun pushAllLocalData(unitKey: String) {
        if (unitKey.isEmpty()) return
        val db = firestore
        _syncState.value = _syncState.value.copy(isSyncing = true, syncMessage = "Отправка локальных данных в облако...")
        try {
            dao.getAllPoints().first().forEach { p -> pushWarehousePointAsync(unitKey, p) }
            dao.getAllItems().first().forEach { i -> pushInventoryItemAsync(unitKey, i) }
            dao.getAllStockRecords().first().forEach { s -> pushStockRecordAsync(unitKey, s) }
            dao.getAllOperations().first().forEach { o -> pushOperationAsync(unitKey, o, emptyList()) }
            dao.getAllRequisitions().first().forEach { r -> pushRequisitionAsync(unitKey, r) }

            _syncState.value = _syncState.value.copy(isSyncing = false, syncMessage = "Синхронизация завершена")
        } catch (e: Exception) {
            _syncState.value = _syncState.value.copy(isSyncing = false, syncMessage = "Ошибка синхронизации")
        }
    }

    fun pushOperationAsync(unitKey: String, op: OperationRecord, updatedStocks: List<StockRecord>) {
        if (unitKey.isEmpty()) return
        scope.launch(Dispatchers.IO) {
            try {
                val db = firestore
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
                val db = firestore
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
                val db = firestore
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
                val db = firestore
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
                val db = firestore
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
                val db = firestore
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
                val db = firestore
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
                val db = firestore
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
                val db = firestore
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
                val db = firestore
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
