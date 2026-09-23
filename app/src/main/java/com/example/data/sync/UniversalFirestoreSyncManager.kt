package com.example.data.sync

import android.content.Context
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class UniversalFirestoreSyncManager(
    context: Context,
    private val dao: KapterkaDao,
    private val scope: CoroutineScope
) : WarehouseSyncGateway {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _syncState = MutableStateFlow(
        SyncState(
            isSyncing = false,
            isOnline = false,
            connectedDevicesCount = 1,
            syncMessage = "Облачная синхронизация доступна в PRO"
        )
    )
    override val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _syncEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    override val syncEvents: SharedFlow<String> = _syncEvents.asSharedFlow()

    @Volatile
    private var enabled = false

    private companion object {
        const val TAG = "SkladProSync"
        const val POINT = "warehouse_point"
        const val ITEM = "inventory_item"
        const val STOCK = "stock_record"
        const val OPERATION = "operation"
        const val REQUISITION = "requisition"
    }

    fun setEnabled(value: Boolean) {
        enabled = value
        if (!value) {
            _syncState.value = SyncState(
                isSyncing = false,
                isOnline = false,
                connectedDevicesCount = 1,
                syncMessage = "Облачная синхронизация доступна в PRO"
            )
        }
    }

    private fun verifiedUid(): String? {
        val user = auth.currentUser ?: return null
        if (!user.isEmailVerified) return null
        return user.uid.takeIf { it.isNotBlank() }
    }

    private fun workspace(uid: String) =
        db.collection("workspaces").document(uid)

    private suspend fun ensureWorkspace(uid: String, callsign: String, unitName: String) {
        val root = workspace(uid)
        root.set(
            hashMapOf(
                "ownerUid" to uid,
                "name" to unitName.ifBlank { "Мой склад" },
                "profileName" to callsign,
                "schemaVersion" to 1,
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        ).await()

        val memberRef = root.collection("members").document(uid)
        if (!memberRef.get().await().exists()) {
            memberRef.set(
                hashMapOf(
                    "uid" to uid,
                    "role" to "owner",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    override suspend fun syncAndReconcileAll(
        unitKey: String,
        callsign: String,
        unitName: String
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (!enabled) {
            return@withContext Pair(false, "Облачная синхронизация доступна в PRO")
        }

        val uid = verifiedUid()
            ?: return@withContext Pair(false, "Подтвердите email и войдите в аккаунт")

        _syncState.value = _syncState.value.copy(
            isSyncing = true,
            syncMessage = "Синхронизация Склад ПРО..."
        )

        try {
            ensureWorkspace(uid, callsign, unitName)
            pullTombstones(uid)
            pullWarehouses(uid)
            pullItems(uid)
            pullStocks(uid)
            pullOperations(uid)
            pullRequisitions(uid)
            pushAllLocal(uid)

            _syncState.value = SyncState(
                isSyncing = false,
                lastSyncTime = System.currentTimeMillis(),
                isOnline = true,
                connectedDevicesCount = 1,
                syncMessage = "Данные синхронизированы"
            )
            _syncEvents.tryEmit("Склад ПРО синхронизирован")
            Pair(true, "Синхронизация завершена")
        } catch (e: Exception) {
            Log.w(TAG, "sync failed", e)
            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                isOnline = false,
                syncMessage = "Нет связи с облаком"
            )
            Pair(false, "Не удалось синхронизировать данные")
        }
    }

    override suspend fun prepareDeletionTombstone(
        unitKey: String,
        entityType: String,
        entityId: String
    ): SyncTombstone {
        val uid = verifiedUid().orEmpty()
        val storageKey = uid.ifBlank { "pending" }
        val tombstone = SyncTombstone.create(storageKey, entityType, entityId)
        dao.upsertSyncTombstone(tombstone)

        if (enabled && uid.isNotBlank()) {
            publishTombstone(uid, tombstone)
        }
        return tombstone
    }

    private suspend fun publishTombstone(uid: String, tombstone: SyncTombstone) {
        workspace(uid).collection("tombstones").document(tombstone.id).set(
            hashMapOf(
                "id" to tombstone.id,
                "entityType" to tombstone.entityType,
                "entityId" to tombstone.entityId,
                "deletedAt" to tombstone.deletedAt,
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        ).await()
    }

    private suspend fun pullTombstones(uid: String) {
        val docs = workspace(uid).collection("tombstones").get().await()
        for (doc in docs.documents) {
            val type = doc.getString("entityType").orEmpty()
            val entityId = doc.getString("entityId").orEmpty()
            val deletedAt = doc.getLong("deletedAt") ?: 0L
            if (type.isBlank() || entityId.isBlank() || deletedAt <= 0L) continue

            val incoming = SyncTombstone.create(uid, type, entityId, deletedAt)
            val previous = dao.getSyncTombstoneById(incoming.id)
            val effective = if (previous == null || incoming.deletedAt > previous.deletedAt) {
                dao.upsertSyncTombstone(incoming)
                incoming
            } else {
                previous
            }
            applyTombstone(effective)
        }
    }

    private suspend fun applyTombstone(tombstone: SyncTombstone) {
        when (tombstone.entityType) {
            POINT -> {
                dao.deleteStockForPoint(tombstone.entityId)
                dao.deletePoint(tombstone.entityId)
            }
            ITEM -> {
                dao.deleteStockForItem(tombstone.entityId)
                dao.deleteItem(tombstone.entityId)
            }
            STOCK -> {
                val parts = tombstone.entityId.split(":::", limit = 2)
                if (parts.size == 2) {
                    dao.deleteStockRecord(parts[0], parts[1])
                }
            }
            OPERATION -> dao.deleteOperation(tombstone.entityId)
            REQUISITION -> dao.deleteRequisition(tombstone.entityId)
        }
    }

    private suspend fun tombstoned(uid: String, type: String, entityId: String): Boolean {
        val id = SyncTombstone.create(uid, type, entityId, 1L).id
        return dao.getSyncTombstoneById(id) != null
    }

    private suspend fun pullWarehouses(uid: String) {
        val docs = workspace(uid).collection("warehouses").get().await()
        for (doc in docs.documents) {
            if (tombstoned(uid, POINT, doc.id)) continue
            dao.insertPoint(
                WarehousePoint(
                    id = doc.id,
                    name = doc.getString("name").orEmpty(),
                    description = doc.getString("description").orEmpty(),
                    isBase = doc.getBoolean("isBase") ?: false,
                    orderIndex = doc.getLong("orderIndex")?.toInt() ?: 0,
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                    profileId = doc.getString("profileId").orEmpty(),
                    syncKey = doc.getString("syncKey").orEmpty()
                )
            )
        }
    }

    private suspend fun pullItems(uid: String) {
        val docs = workspace(uid).collection("items").get().await()
        for (doc in docs.documents) {
            if (tombstoned(uid, ITEM, doc.id)) continue
            val local = dao.getItemById(doc.id)
            val cloudProfileId = doc.getString("profileId").orEmpty()
            dao.insertItem(
                InventoryItem(
                    id = doc.id,
                    name = doc.getString("name").orEmpty(),
                    serviceCategory = doc.getString("serviceCategory").orEmpty(),
                    subType = doc.getString("subType").orEmpty(),
                    unit = doc.getString("unit") ?: "шт.",
                    categoryClass = doc.getString("categoryClass") ?: "Кат. 1",
                    standardCode = doc.getString("standardCode").orEmpty(),
                    isCustom = doc.getBoolean("isCustom") ?: false,
                    profileId = cloudProfileId.ifBlank { local?.profileId.orEmpty() },
                    warehouseId = doc.getString("warehouseId").orEmpty()
                        .ifBlank { local?.warehouseId.orEmpty() }
                )
            )
        }
    }

    private suspend fun pullStocks(uid: String) {
        val docs = workspace(uid).collection("stocks").get().await()
        for (doc in docs.documents) {
            val pointId = doc.getString("pointId").orEmpty()
            val itemId = doc.getString("itemId").orEmpty()
            if (pointId.isBlank() || itemId.isBlank()) continue
            val entityId = pointId + ":::" + itemId
            if (tombstoned(uid, STOCK, entityId)) continue

            val cloud = StockRecord(
                pointId = pointId,
                itemId = itemId,
                quantity = doc.getLong("quantity")?.toInt() ?: 0,
                incomeTotal = doc.getLong("incomeTotal")?.toInt() ?: 0,
                expenseTotal = doc.getLong("expenseTotal")?.toInt() ?: 0,
                lastUpdated = doc.getLong("lastUpdated") ?: 0L
            )
            val local = dao.getStockItem(pointId, itemId)
            if (local == null || cloud.lastUpdated >= local.lastUpdated) {
                dao.insertOrUpdateStock(cloud)
            }
        }
    }

    private suspend fun pullOperations(uid: String) {
        val localIds = dao.getAllOperations().first().map { it.id }.toHashSet()
        val docs = workspace(uid).collection("operations").get().await()
        for (doc in docs.documents) {
            if (doc.id in localIds || tombstoned(uid, OPERATION, doc.id)) continue
            val type = try {
                OperationType.valueOf(doc.getString("type") ?: "INCOME")
            } catch (_: Exception) {
                OperationType.INCOME
            }
            dao.insertOperation(
                OperationRecord(
                    id = doc.id,
                    type = type,
                    fromPointName = doc.getString("fromPointName").orEmpty(),
                    toPointName = doc.getString("toPointName").orEmpty(),
                    docNumber = doc.getString("docNumber").orEmpty(),
                    responsiblePerson = doc.getString("responsiblePerson").orEmpty(),
                    comment = doc.getString("comment").orEmpty(),
                    timestamp = doc.getLong("timestamp") ?: 0L,
                    itemsSummary = doc.getString("itemsSummary").orEmpty(),
                    itemsJson = doc.getString("itemsJson").orEmpty()
                )
            )
        }
    }

    private suspend fun pullRequisitions(uid: String) {
        val docs = workspace(uid).collection("requisitions").get().await()
        for (doc in docs.documents) {
            if (tombstoned(uid, REQUISITION, doc.id)) continue
            val status = try {
                RequestStatus.valueOf(doc.getString("status") ?: "PENDING")
            } catch (_: Exception) {
                RequestStatus.PENDING
            }
            dao.insertRequisition(
                RequisitionRequest(
                    id = doc.id,
                    pointName = doc.getString("pointName").orEmpty(),
                    applicantName = doc.getString("applicantName").orEmpty(),
                    status = status,
                    comment = doc.getString("comment").orEmpty(),
                    timestamp = doc.getLong("timestamp") ?: 0L,
                    itemsSummary = doc.getString("itemsSummary").orEmpty(),
                    itemsJson = doc.getString("itemsJson").orEmpty()
                )
            )
        }
    }

    private suspend fun pushAllLocal(uid: String) {
        for (t in dao.getSyncTombstonesForUnit(uid)) publishTombstone(uid, t)
        for (p in dao.getAllPoints().first()) pushWarehouse(uid, p)
        for (i in dao.getAllItems().first()) pushItem(uid, i)
        for (s in dao.getAllStockRecords().first()) pushStock(uid, s)
        for (o in dao.getAllOperations().first()) pushOperation(uid, o)
        for (r in dao.getAllRequisitions().first()) pushRequisition(uid, r)
    }

    private suspend fun pushWarehouse(uid: String, point: WarehousePoint) {
        if (tombstoned(uid, POINT, point.id)) return
        workspace(uid).collection("warehouses").document(point.id).set(
            hashMapOf(
                "id" to point.id,
                "name" to point.name,
                "description" to point.description,
                "isBase" to point.isBase,
                "orderIndex" to point.orderIndex,
                "createdAt" to point.createdAt,
                "profileId" to point.profileId,
                "syncKey" to point.syncKey,
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        ).await()
    }

    private suspend fun pushItem(uid: String, item: InventoryItem) {
        if (tombstoned(uid, ITEM, item.id)) return
        workspace(uid).collection("items").document(item.id).set(
            hashMapOf(
                "id" to item.id,
                "name" to item.name,
                "serviceCategory" to item.serviceCategory,
                "subType" to item.subType,
                "unit" to item.unit,
                "categoryClass" to item.categoryClass,
                "standardCode" to item.standardCode,
                "isCustom" to item.isCustom,
                "profileId" to item.profileId,
                "warehouseId" to item.warehouseId,
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        ).await()
    }

    private suspend fun pushStock(uid: String, stock: StockRecord) {
        val entityId = stock.pointId + ":::" + stock.itemId
        if (tombstoned(uid, STOCK, entityId)) return
        val docId = stock.pointId + "___" + stock.itemId
        workspace(uid).collection("stocks").document(docId).set(
            hashMapOf(
                "pointId" to stock.pointId,
                "itemId" to stock.itemId,
                "quantity" to stock.quantity,
                "incomeTotal" to stock.incomeTotal,
                "expenseTotal" to stock.expenseTotal,
                "lastUpdated" to stock.lastUpdated,
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        ).await()
    }

    private suspend fun pushOperation(uid: String, op: OperationRecord) {
        if (tombstoned(uid, OPERATION, op.id)) return
        val ref = workspace(uid).collection("operations").document(op.id)
        db.runTransaction { transaction ->
            val current = transaction.get(ref)
            if (!current.exists()) {
                transaction.set(
                    ref,
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
                        "itemsJson" to op.itemsJson,
                        "createdBy" to uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            null
        }.await()
    }

    private suspend fun pushRequisition(uid: String, request: RequisitionRequest) {
        if (tombstoned(uid, REQUISITION, request.id)) return
        workspace(uid).collection("requisitions").document(request.id).set(
            hashMapOf(
                "id" to request.id,
                "pointName" to request.pointName,
                "applicantName" to request.applicantName,
                "status" to request.status.name,
                "comment" to request.comment,
                "timestamp" to request.timestamp,
                "itemsSummary" to request.itemsSummary,
                "itemsJson" to request.itemsJson,
                "createdBy" to uid,
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        ).await()
    }

    private fun readyUid(): String? =
        if (enabled) verifiedUid() else null

    private fun markSynced() {
        _syncState.value = _syncState.value.copy(
            isOnline = true,
            lastSyncTime = System.currentTimeMillis(),
            syncMessage = "Изменения синхронизированы"
        )
    }

    override fun pushOperationAsync(
        unitKey: String,
        op: OperationRecord,
        updatedStocks: List<StockRecord>
    ) {
        val uid = readyUid() ?: return
        scope.launch(Dispatchers.IO) {
            try {
                pushOperation(uid, op)
                for (stock in updatedStocks) pushStock(uid, stock)
                markSynced()
            } catch (e: Exception) {
                Log.w(TAG, "operation push failed", e)
            }
        }
    }

    override fun pushStockRecordAsync(unitKey: String, stock: StockRecord) {
        val uid = readyUid() ?: return
        scope.launch(Dispatchers.IO) {
            try {
                pushStock(uid, stock)
                markSynced()
            } catch (e: Exception) {
                Log.w(TAG, "stock push failed", e)
            }
        }
    }

    override fun pushRequisitionAsync(unitKey: String, requisition: RequisitionRequest) {
        val uid = readyUid() ?: return
        scope.launch(Dispatchers.IO) {
            try {
                pushRequisition(uid, requisition)
                markSynced()
            } catch (e: Exception) {
                Log.w(TAG, "requisition push failed", e)
            }
        }
    }

    override fun pushWarehousePointAsync(unitKey: String, point: WarehousePoint) {
        val uid = readyUid() ?: return
        scope.launch(Dispatchers.IO) {
            try {
                pushWarehouse(uid, point)
                markSynced()
            } catch (e: Exception) {
                Log.w(TAG, "warehouse push failed", e)
            }
        }
    }

    override fun pushInventoryItemAsync(unitKey: String, item: InventoryItem) {
        val uid = readyUid() ?: return
        scope.launch(Dispatchers.IO) {
            try {
                pushItem(uid, item)
                markSynced()
            } catch (e: Exception) {
                Log.w(TAG, "item push failed", e)
            }
        }
    }

    override fun deleteRequisitionAsync(unitKey: String, requisitionId: String) = Unit
    override fun deleteOperationAsync(unitKey: String, operationId: String) = Unit
    override fun deleteWarehousePointAsync(unitKey: String, pointId: String) = Unit
    override fun deleteInventoryItemAsync(unitKey: String, itemId: String) = Unit
    override fun clearCloudDataAsync(unitKey: String) = Unit
}
