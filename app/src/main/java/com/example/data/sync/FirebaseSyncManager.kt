package com.example.data.sync

// Re-deploy trigger
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
import com.example.data.model.SyncTombstone
import com.example.data.model.WarehousePoint
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
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
    private var presenceHeartbeatJob: Job? = null

    private companion object {
        const val ACTIVE_DEVICE_WINDOW_MS = 15 * 60 * 1000L
        const val PRESENCE_HEARTBEAT_MS = 5 * 60 * 1000L
        const val TOMBSTONE_POINT = "warehouse_point"
        const val TOMBSTONE_ITEM = "inventory_item"
        const val TOMBSTONE_OPERATION = "operation"
        const val TOMBSTONE_REQUISITION = "requisition"
        const val TOMBSTONE_RETENTION_MS = 120L * 24L * 60L * 60L * 1000L
    }

    suspend fun prepareDeletionTombstone(
        unitKey: String,
        entityType: String,
        entityId: String
    ): SyncTombstone = withContext(Dispatchers.IO) {
        val cleanKey = unitKey.trim()
        val tombstone = SyncTombstone.create(cleanKey, entityType, entityId)
        dao.upsertSyncTombstone(tombstone)
        if (cleanKey.isNotBlank()) {
            try {
                firestore.collection("units").document(cleanKey)
                    .collection("sync_tombstones").document(tombstone.id)
                    .set(
                        hashMapOf(
                            "id" to tombstone.id,
                            "unitKey" to cleanKey,
                            "entityType" to tombstone.entityType,
                            "entityId" to tombstone.entityId,
                            "deletedAt" to tombstone.deletedAt,
                            "deviceId" to deviceId
                        ),
                        SetOptions.merge()
                    ).await()
            } catch (e: Exception) {
                Log.w(TAG, "Tombstone saved locally; cloud publish deferred", e)
            }
        }
        tombstone
    }

    private suspend fun applyTombstone(tombstone: SyncTombstone) {
        when (tombstone.entityType) {
            TOMBSTONE_POINT -> {
                if (tombstone.entityId != "base_sklad") {
                    dao.deleteStockForPoint(tombstone.entityId)
                    dao.deletePoint(tombstone.entityId)
                }
            }
            TOMBSTONE_ITEM -> {
                dao.deleteStockForItem(tombstone.entityId)
                dao.deleteItem(tombstone.entityId)
            }
            TOMBSTONE_OPERATION -> dao.deleteOperation(tombstone.entityId)
            TOMBSTONE_REQUISITION -> dao.deleteRequisition(tombstone.entityId)
        }
    }

    private suspend fun isTombstoned(
        unitKey: String,
        entityType: String,
        entityId: String
    ): Boolean {
        if (unitKey.isBlank() || entityId.isBlank()) return false
        val id = SyncTombstone.create(unitKey, entityType, entityId, 1L).id
        return dao.getSyncTombstoneById(id) != null
    }

    private suspend fun syncTombstones(unitKey: String) {
        if (unitKey.isBlank()) return
        val unitRef = firestore.collection("units").document(unitKey)

        for (local in dao.getSyncTombstonesForUnit(unitKey)) {
            try {
                unitRef.collection("sync_tombstones").document(local.id).set(
                    hashMapOf(
                        "id" to local.id,
                        "unitKey" to local.unitKey,
                        "entityType" to local.entityType,
                        "entityId" to local.entityId,
                        "deletedAt" to local.deletedAt,
                        "deviceId" to deviceId
                    ),
                    SetOptions.merge()
                ).await()
            } catch (e: Exception) {
                Log.w(TAG, "Failed publishing local tombstone", e)
            }
        }

        val cloud = unitRef.collection("sync_tombstones").get().await()
        for (doc in cloud.documents) {
            val type = doc.getString("entityType").orEmpty()
            val entityId = doc.getString("entityId").orEmpty()
            val deletedAt = doc.getLong("deletedAt") ?: 0L
            if (type.isBlank() || entityId.isBlank() || deletedAt <= 0L) continue

            val tombstone = SyncTombstone.create(
                unitKey = unitKey,
                entityType = type,
                entityId = entityId,
                deletedAt = deletedAt
            )
            val existing = dao.getSyncTombstoneById(tombstone.id)
            if (existing == null || tombstone.deletedAt > existing.deletedAt) {
                dao.upsertSyncTombstone(tombstone)
            }
            applyTombstone(tombstone)
        }

        dao.pruneOldSyncTombstones(System.currentTimeMillis() - TOMBSTONE_RETENTION_MS)
    }

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

        
            registerUnitListeners(cleanKey)
        sendPresencePing(cleanKey, callsign, unitName)
        startPresenceHeartbeat(cleanKey, callsign, unitName)

        _syncState.value = _syncState.value.copy(
            isSyncing = false,
            isOnline = true,
            syncMessage = "Подключено к подразделению"
        )
    }

    private fun registerUnitListeners(unitKey: String) {
        val db = firestore
        val unitRef = db.collection("units").document(unitKey)

        val tombstoneReg = unitRef.collection("sync_tombstones").addSnapshotListener { snap, e ->
            if (e != null || snap == null) return@addSnapshotListener
            scope.launch(Dispatchers.IO) {
                for (dc in snap.documentChanges) {
                    if (dc.type == DocumentChange.Type.REMOVED) continue
                    val type = dc.document.getString("entityType").orEmpty()
                    val entityId = dc.document.getString("entityId").orEmpty()
                    val deletedAt = dc.document.getLong("deletedAt") ?: 0L
                    if (type.isBlank() || entityId.isBlank() || deletedAt <= 0L) continue
                    val tombstone = SyncTombstone.create(unitKey, type, entityId, deletedAt)
                    dao.upsertSyncTombstone(tombstone)
                    applyTombstone(tombstone)
                }
            }
        }
        listeners.add(tombstoneReg)

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
                    if (dc.type != DocumentChange.Type.REMOVED &&
                        !isTombstoned(unitKey, TOMBSTONE_POINT, p.id)
                    ) {
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
                    if (dc.type != DocumentChange.Type.REMOVED &&
                        !isTombstoned(unitKey, TOMBSTONE_ITEM, item.id)
                    ) {
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
                    val docId = dc.document.id
                    val parts = if (docId.contains("___")) docId.split("___") else emptyList()
                    val pId = dc.document.getString("pointId") ?: if (parts.size >= 2) parts[0] else ""
                    val iId = dc.document.getString("itemId") ?: if (parts.size >= 2) parts[1] else ""

                    if (dc.type != DocumentChange.Type.REMOVED) {
                        val s = StockRecord(
                            pointId = pId,
                            itemId = iId,
                            quantity = dc.document.getLong("quantity")?.toInt() ?: 0,
                            incomeTotal = dc.document.getLong("incomeTotal")?.toInt() ?: 0,
                            expenseTotal = dc.document.getLong("expenseTotal")?.toInt() ?: 0,
                            lastUpdated = dc.document.getLong("lastUpdated") ?: 0L
                        )
                        if (s.pointId.isNotBlank() && s.itemId.isNotBlank() &&
                            !isTombstoned(unitKey, TOMBSTONE_POINT, s.pointId) &&
                            !isTombstoned(unitKey, TOMBSTONE_ITEM, s.itemId)
                        ) {
                            dao.insertOrUpdateStock(s)
                            // Do not create generic placeholder from stock record if nameHint is missing;
                            // operations listener will register it with exact name from itemsJson.
                            // ensureItemExists(unitKey, s.itemId)
                        }
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
                    if (dc.type != DocumentChange.Type.REMOVED &&
                        !isTombstoned(unitKey, TOMBSTONE_OPERATION, op.id)
                    ) {
                        dao.insertOperation(op)
                        extractAndRegisterItemsFromOperation(unitKey, op)
                        if (!isFirstOpLoad && dc.type == DocumentChange.Type.ADDED) {
                            val typeName = when (op.type) {
                                OperationType.INCOME -> "📥 Поставка на «${op.toPointName.ifBlank { "склад" }}»"
                                OperationType.TRANSFER -> "🔄 Перемещение: ${op.fromPointName.ifBlank { "Склад" }} ➔ ${op.toPointName}"
                                OperationType.ISSUE -> "🎯 Выдача на «${op.toPointName}»"
                                OperationType.EXPENDITURE -> "💥 Списание ф. 8 на «${op.fromPointName.ifBlank { op.toPointName }}»"
                            }
                            val docPart = if (op.docNumber.isNotBlank() && !op.docNumber.trim().equals("документ", ignoreCase = true)) " • Акт № ${op.docNumber}" else ""
                            val summaryPart = if (op.itemsSummary.isNotBlank()) ": ${op.itemsSummary}" else ""
                            _syncEvents.emit("Синхронизация: $typeName$docPart$summaryPart")
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
                    if (dc.type != DocumentChange.Type.REMOVED &&
                        !isTombstoned(unitKey, TOMBSTONE_REQUISITION, req.id)
                    ) {
                        dao.insertRequisition(req)
                    }
                }
            }
        }
        listeners.add(reqReg)
        
        val presReg = unitRef.collection("devices").addSnapshotListener { snap, e ->
            if (e != null || snap == null) return@addSnapshotListener
            val now = System.currentTimeMillis()
            val activeDevices = snap.documents.count { doc ->
                val lastSeen = doc.getLong("timestampMillis") ?: 0L
                lastSeen > 0L && (now - lastSeen) <= ACTIVE_DEVICE_WINDOW_MS
            }
            _syncState.value = _syncState.value.copy(connectedDevicesCount = activeDevices.coerceAtLeast(1))
        }
        listeners.add(presReg)
    }

    private fun startPresenceHeartbeat(unitKey: String, callsign: String, unitName: String) {
        presenceHeartbeatJob?.cancel()
        presenceHeartbeatJob = scope.launch(Dispatchers.IO) {
            while (isActive && activeUnitKey == unitKey) {
                delay(PRESENCE_HEARTBEAT_MS)
                if (isActive && activeUnitKey == unitKey) {
                    sendPresencePing(unitKey, callsign, unitName)
                }
            }
        }
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

    suspend fun syncAndReconcileAll(unitKey: String, callsign: String, unitName: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val cleanKey = unitKey.trim()
        if (cleanKey.isEmpty()) return@withContext Pair(false, "Не указан код подразделения")

        _syncState.value = _syncState.value.copy(isSyncing = true, syncMessage = "Синхронизация базы [$cleanKey]...")
        try {
            val db = firestore
            val unitRef = db.collection("units").document(cleanKey)

            syncTombstones(cleanKey)

            // 1. Fetch Cloud Stock Records first to know which points have inventory
            val cloudStocksSnap = unitRef.collection("stock_records").get().await()
            val stockPointIds = cloudStocksSnap.documents.mapNotNull { it.getString("pointId") }.filter { it.isNotBlank() }.toSet()

            // 2. Reconcile Warehouse Points
            val defaultPointsMap = com.example.data.local.InitialData.getDefaultPoints().associateBy { it.id }
            val cloudPointsSnap = unitRef.collection("warehouse_points").get().await()
            val existingPointsMap = mutableMapOf<String, WarehousePoint>()

            for (doc in cloudPointsSnap.documents) {
                val p = WarehousePoint(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    description = doc.getString("description") ?: "",
                    isBase = doc.getBoolean("isBase") ?: false,
                    orderIndex = doc.getLong("orderIndex")?.toInt() ?: 0,
                    createdAt = doc.getLong("createdAt") ?: 0L
                )
                if (p.name.isNotBlank() && !isTombstoned(cleanKey, TOMBSTONE_POINT, p.id)) {
                    existingPointsMap[p.id] = p
                }
            }

            // A broken/incomplete cloud snapshot must never trigger destructive cleanup.
            // Orphaned cloud stock is logged and ignored until explicit tombstone semantics exist.
            val orphanedPointIds = stockPointIds.filter { !existingPointsMap.containsKey(it) && it != "base_sklad" }
            if (orphanedPointIds.isNotEmpty()) {
                Log.w(
                    TAG,
                    "Cloud contains stock for missing points; preserving local/cloud data: " +
                        orphanedPointIds.joinToString()
                )
            }

            // Always ensure the root base warehouse exists (only base_sklad is protected)
            if (!existingPointsMap.containsKey("base_sklad")) {
                val base = defaultPointsMap["base_sklad"] ?: WarehousePoint(
                    id = "base_sklad",
                    name = "Базовый склад (КЗ)",
                    description = "Основной склад подразделения",
                    isBase = true
                )
                existingPointsMap["base_sklad"] = base
                pushWarehousePointAsync(cleanKey, base)
            }

            // Safety rule for next-safe:
            // absence in a cloud snapshot is NOT proof that a local point was intentionally deleted.
            // Merge cloud points locally without deleting existing local points.
            dao.insertPoints(existingPointsMap.values.toList())

            // 3. Reconcile Stock Records
            // Authoritative: If cloud has stock records, local must match cloud exactly.
            if (!cloudStocksSnap.isEmpty) {
                val cloudStockKeys = mutableSetOf<String>()
                val recordsToInsert = mutableListOf<StockRecord>()

                for (doc in cloudStocksSnap.documents) {
                    val ptId = doc.getString("pointId") ?: ""
                    val itemId = doc.getString("itemId") ?: ""
                    val qty = doc.getLong("quantity")?.toInt() ?: 0
                    val inc = doc.getLong("incomeTotal")?.toInt() ?: 0
                    val exp = doc.getLong("expenseTotal")?.toInt() ?: 0
                    val updated = doc.getLong("lastUpdated") ?: 0L

                    if (ptId.isNotBlank() && itemId.isNotBlank()) {
                        // Do not re-insert stocks belonging to points that no longer exist in cloud
                        if ((existingPointsMap.containsKey(ptId) || ptId == "base_sklad") &&
                            !isTombstoned(cleanKey, TOMBSTONE_POINT, ptId) &&
                            !isTombstoned(cleanKey, TOMBSTONE_ITEM, itemId)
                        ) {
                            cloudStockKeys.add("${ptId}:::${itemId}")
                            recordsToInsert.add(
                                StockRecord(
                                    pointId = ptId,
                                    itemId = itemId,
                                    quantity = qty,
                                    incomeTotal = inc,
                                    expenseTotal = exp,
                                    lastUpdated = updated
                                )
                            )
                        }
                    }
                }

                // Never delete local stock merely because a cloud snapshot does not contain it.
                // Explicit tombstones/versioned deletes will be introduced before release.
                // For now, cloud records are merged over local records.
                // Upsert cloud stock records
                for (s in recordsToInsert) {
                    dao.insertOrUpdateStock(s)
                    // ensureItemExists will be handled with real names during operation reconciliation below
                }
            } else {
                // Empty cloud must never erase local stock.
                // Keep local records until an explicit, verifiable deletion model exists.
                Log.i(TAG, "Cloud stock is empty; preserving local stock in next-safe mode")
            }

            // 4. Reconcile Operation Records
            val cloudOpsSnap = unitRef.collection("operation_records").get().await()
            // Merge cloud history locally. Do not delete local history by absence alone.
            for (doc in cloudOpsSnap.documents) {
                val opTypeStr = doc.getString("type") ?: "INCOME"
                val type = try { OperationType.valueOf(opTypeStr) } catch (ex: Exception) { OperationType.INCOME }
                val op = OperationRecord(
                    id = doc.id,
                    type = type,
                    fromPointName = doc.getString("fromPointName") ?: "",
                    toPointName = doc.getString("toPointName") ?: "",
                    docNumber = doc.getString("docNumber") ?: "",
                    responsiblePerson = doc.getString("responsiblePerson") ?: "",
                    comment = doc.getString("comment") ?: "",
                    timestamp = doc.getLong("timestamp") ?: 0L,
                    itemsSummary = doc.getString("itemsSummary") ?: "",
                    itemsJson = doc.getString("itemsJson") ?: ""
                )
                if (!isTombstoned(cleanKey, TOMBSTONE_OPERATION, op.id)) {
                    dao.insertOperation(op)
                    extractAndRegisterItemsFromOperation(cleanKey, op)
                }
            }

            // 5. Reconcile Requisitions
            val cloudReqSnap = unitRef.collection("requisitions").get().await()
            // Merge cloud requisitions locally. Do not delete local data by absence alone.
            for (doc in cloudReqSnap.documents) {
                val statusStr = doc.getString("status") ?: "PENDING"
                val status = try { RequestStatus.valueOf(statusStr) } catch (ex: Exception) { RequestStatus.PENDING }
                val req = RequisitionRequest(
                    id = doc.id,
                    pointName = doc.getString("pointName") ?: "",
                    applicantName = doc.getString("applicantName") ?: "",
                    status = status,
                    comment = doc.getString("comment") ?: "",
                    timestamp = doc.getLong("timestamp") ?: 0L,
                    itemsSummary = doc.getString("itemsSummary") ?: "",
                    itemsJson = doc.getString("itemsJson") ?: ""
                )
                if (!isTombstoned(cleanKey, TOMBSTONE_REQUISITION, req.id)) {
                    dao.insertRequisition(req)
                }
            }

            // 6. Reconcile Custom Items
            val cloudItemsSnap = unitRef.collection("inventory_items").get().await()
            for (doc in cloudItemsSnap.documents) {
                val item = InventoryItem(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    serviceCategory = doc.getString("serviceCategory") ?: "",
                    subType = doc.getString("subType") ?: "",
                    unit = doc.getString("unit") ?: "шт.",
                    categoryClass = doc.getString("categoryClass") ?: "Кат. 1",
                    standardCode = doc.getString("standardCode") ?: "",
                    isCustom = doc.getBoolean("isCustom") ?: false
                )
                if (!isTombstoned(cleanKey, TOMBSTONE_ITEM, item.id)) {
                    dao.insertItem(item)
                }
            }

            // 7. Connect and register realtime snapshot listeners
            startSyncForUnit(cleanKey, callsign, unitName)

            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                isOnline = true,
                lastSyncTime = System.currentTimeMillis(),
                syncMessage = "Синхронизировано с подразделением [$cleanKey]"
            )
            Pair(true, "База синхронизирована с каналом [$cleanKey]")
        } catch (e: Exception) {
            Log.e(TAG, "Error in syncAndReconcileAll", e)
            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                syncMessage = "Ошибка синхронизации: ${e.message}"
            )
            Pair(false, "Сбой связи: ${e.localizedMessage}")
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

                if (op.itemsJson.isNotBlank()) {
                    try {
                        val entries = parseOperationItems(op.itemsJson)
                        for (entry in entries) {
                            if (entry.itemId.isNotBlank()) {
                                val localItem = dao.getItemById(entry.itemId)
                                val defaultMatch = com.example.data.local.InitialData.getDefaultItems().find { it.id == entry.itemId }
                                val fallbackName = formatFallbackItemName(entry.itemId)
                                val resolvedName = if (entry.itemName.isNotBlank()) entry.itemName else fallbackName
                                val resolvedUnit = if (entry.unit.isNotBlank()) entry.unit else "шт."
                                val resolvedCatClass = if (entry.categoryClass.isNotBlank()) entry.categoryClass else "Кат. 1"
                                val itemToPush = localItem ?: defaultMatch ?: InventoryItem(
                                    id = entry.itemId,
                                    name = resolvedName,
                                    serviceCategory = resolveServiceCategory(entry.itemId, resolvedName),
                                    subType = "Снабжение",
                                    unit = resolvedUnit,
                                    categoryClass = resolvedCatClass,
                                    isCustom = true
                                )
                                unitRef.collection("inventory_items").document(itemToPush.id).set(
                                    hashMapOf(
                                        "id" to itemToPush.id,
                                        "name" to itemToPush.name,
                                        "serviceCategory" to itemToPush.serviceCategory,
                                        "subType" to itemToPush.subType,
                                        "unit" to itemToPush.unit,
                                        "categoryClass" to itemToPush.categoryClass,
                                        "standardCode" to itemToPush.standardCode,
                                        "isCustom" to itemToPush.isCustom
                                    ),
                                    SetOptions.merge()
                                )
                            }
                        }
                    } catch (ie: Exception) {
                        Log.w(TAG, "Failed pushing op items to cloud inventory_items", ie)
                    }
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
                val unitRef = db.collection("units").document(unitKey)
                // Delete warehouse point document in cloud with await
                unitRef.collection("warehouse_points").document(pointId).delete().await()

                // Delete all stock_records for this point in cloud with await
                val stocksSnap = unitRef.collection("stock_records")
                    .whereEqualTo("pointId", pointId)
                    .get().await()
                for (doc in stocksSnap.documents) {
                    try {
                        doc.reference.delete().await()
                    } catch (de: Exception) {
                        Log.w(TAG, "Error deleting stock doc ${doc.id}", de)
                    }
                }
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

    fun resolveServiceCategory(itemId: String, nameHint: String = ""): String {
        val lowerName = nameHint.lowercase()
        return when {
            itemId.startsWith("rav_") || lowerName.contains("мина") || lowerName.contains("снаряд") || lowerName.contains("выстрел") || lowerName.contains("взрыватель") || lowerName.contains("патрон") -> "Служба РАВ"
            itemId.startsWith("auto_") || lowerName.contains("уаз") || lowerName.contains("фильтр") || lowerName.contains("масло") || lowerName.contains("ремень") || lowerName.contains("колодк") -> "Автомобильная и БТ служба"
            itemId.startsWith("gsm_") || lowerName.contains("дизель") || lowerName.contains("бензин") || lowerName.contains("топливо") -> "Служба ГСМ"
            itemId.startsWith("rhbz_") || lowerName.contains("противогаз") || lowerName.contains("впхр") || lowerName.contains("л-1") -> "Служба РХБЗ"
            itemId.startsWith("med_") || lowerName.contains("жгут") || lowerName.contains("бинт") || lowerName.contains("аптечк") || lowerName.contains("промедол") || lowerName.contains("нефопам") -> "Медицинская служба"
            itemId.startsWith("vesh_") || lowerName.contains("бронежилет") || lowerName.contains("шлем") || lowerName.contains("маскхалат") || lowerName.contains("форма") -> "Вещевая служба и СИБЗ"
            itemId.startsWith("ing_") || lowerName.contains("мон-") || lowerName.contains("пмн-") || lowerName.contains("тротил") -> "Инженерная служба"
            itemId.startsWith("prod_") || lowerName.contains("ирп") || lowerName.contains("вода") || lowerName.contains("тушенк") -> "Продовольственная служба"
            itemId.startsWith("bpla_") || lowerName.contains("мавик") || lowerName.contains("fpv") || lowerName.contains("дрон") -> "Служба БПЛА и робототехники"
            itemId.startsWith("svyaz_") || lowerName.contains("радио") || lowerName.contains("антенн") || lowerName.contains("рэб") -> "Служба связи и РЭБ"
            else -> "Служба РАВ"
        }
    }

    fun formatFallbackItemName(itemId: String): String {
        return when (itemId) {
            "auto_01" -> "Комплект фильтров УАЗ Патриот Пикап"
            "auto_02" -> "Масло моторное 10W-40 (Канистра 5л)"
            "auto_03" -> "Антифриз G12 (Канистра 5л)"
            "auto_04" -> "Ремень генератора УАЗ Патриот"
            "auto_05" -> "Колодки тормозные передние УАЗ"
            "auto_06" -> "Свечи зажигания ЗМЗ-409 (комплект 4 шт.)"
            "auto_07" -> "Трос буксировочный динамический 12т"
            "auto_08" -> "Домкрат реечный (Хайджек)"
            "auto_09" -> "Набор автоинструмента (82 предм.)"
            "auto_10" -> "Канистра металлическая 20л"
            "auto_11" -> "Аккумулятор автомобильный 6СТ-75"
            "auto_12" -> "Шина повышенной проходимости УАЗ"
            "rav_27" -> "Мина 120-мм дымовая Д-843А"
            "rav_28" -> "Мина 120-мм осветительная С-843"
            "rav_29" -> "Мина 82-мм дымовая Д-832ДУ"
            "rav_30" -> "Мина 82-мм осветительная С-832С"
            "rav_31" -> "Порох минометный (метательный заряд) НБЛ-35"
            "rav_32" -> "Заряд дальнобойный минометный"
            else -> "Имущество ($itemId)"
        }
    }

    private suspend fun ensureItemExists(unitKey: String, itemId: String, nameHint: String? = null, unitHint: String? = null, catClassHint: String? = null) {
        if (itemId.isBlank()) return
        val existing = dao.getItemById(itemId)
        if (existing != null) {
            // If existing item has a generic fallback name and we now have a real nameHint, update it!
            if (!nameHint.isNullOrBlank() && (existing.name.startsWith("Имущество (") || existing.name.isBlank())) {
                val nonNullName: String = nameHint
                val updated = existing.copy(
                    name = nonNullName,
                    serviceCategory = resolveServiceCategory(itemId, nonNullName),
                    unit = if (!unitHint.isNullOrBlank()) unitHint else existing.unit,
                    categoryClass = if (!catClassHint.isNullOrBlank()) catClassHint else existing.categoryClass
                )
                dao.insertItem(updated)
                if (unitKey.isNotBlank()) {
                    pushInventoryItemAsync(unitKey, updated)
                }
            }
            return
        }

        val defaultMatch = com.example.data.local.InitialData.getDefaultItems().find { it.id == itemId }
        val itemToInsert = defaultMatch ?: InventoryItem(
            id = itemId,
            name = nameHint?.ifBlank { null } ?: formatFallbackItemName(itemId),
            serviceCategory = resolveServiceCategory(itemId, nameHint ?: ""),
            subType = "Снабжение",
            unit = unitHint?.ifBlank { null } ?: (if (itemId.startsWith("vesh_") || itemId.startsWith("auto_")) "компл." else if (itemId.startsWith("gsm_")) "л." else if (itemId.startsWith("prod_")) "кг." else "шт."),
            categoryClass = catClassHint?.ifBlank { null } ?: "Кат. 1",
            isCustom = true
        )
        dao.insertItem(itemToInsert)
        if (unitKey.isNotBlank()) {
            pushInventoryItemAsync(unitKey, itemToInsert)
        }
    }

    data class ParsedItemEntry(
        val itemId: String,
        val itemName: String,
        val unit: String,
        val categoryClass: String
    )

    private fun parseOperationItems(json: String): List<ParsedItemEntry> {
        if (json.isBlank()) return emptyList()
        return try {
            val list = mutableListOf<ParsedItemEntry>()
            val arr = org.json.JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    ParsedItemEntry(
                        itemId = obj.optString("itemId", ""),
                        itemName = obj.optString("itemName", ""),
                        unit = obj.optString("unit", "шт."),
                        categoryClass = obj.optString("categoryClass", "Кат. 1")
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun extractAndRegisterItemsFromOperation(unitKey: String, op: OperationRecord) {
        if (op.itemsJson.isBlank()) return
        try {
            val entries = parseOperationItems(op.itemsJson)
            for (entry in entries) {
                if (entry.itemId.isNotBlank()) {
                    ensureItemExists(
                        unitKey = unitKey,
                        itemId = entry.itemId,
                        nameHint = entry.itemName,
                        unitHint = entry.unit,
                        catClassHint = entry.categoryClass
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error extracting items from op.itemsJson", e)
        }
    }

    fun stopSync() {
        presenceHeartbeatJob?.cancel()
        presenceHeartbeatJob = null
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
