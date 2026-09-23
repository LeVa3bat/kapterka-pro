package com.example.data.sync

import com.example.data.model.InventoryItem
import com.example.data.model.OperationRecord
import com.example.data.model.RequisitionRequest
import com.example.data.model.StockRecord
import com.example.data.model.SyncTombstone
import com.example.data.model.WarehousePoint
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface WarehouseSyncGateway {
    val syncState: StateFlow<SyncState>
    val syncEvents: SharedFlow<String>

    suspend fun prepareDeletionTombstone(
        unitKey: String,
        entityType: String,
        entityId: String
    ): SyncTombstone

    suspend fun syncAndReconcileAll(
        unitKey: String,
        callsign: String,
        unitName: String
    ): Pair<Boolean, String>

    fun pushOperationAsync(
        unitKey: String,
        op: OperationRecord,
        updatedStocks: List<StockRecord>
    )

    fun pushStockRecordAsync(unitKey: String, stock: StockRecord)
    fun pushRequisitionAsync(unitKey: String, requisition: RequisitionRequest)
    fun deleteRequisitionAsync(unitKey: String, requisitionId: String)
    fun deleteOperationAsync(unitKey: String, operationId: String)
    fun pushWarehousePointAsync(unitKey: String, point: WarehousePoint)
    fun deleteWarehousePointAsync(unitKey: String, pointId: String)
    fun pushInventoryItemAsync(unitKey: String, item: InventoryItem)
    fun deleteInventoryItemAsync(unitKey: String, itemId: String)
    fun clearCloudDataAsync(unitKey: String)
}

class LegacySyncGateway(
    private val delegate: FirebaseSyncManager
) : WarehouseSyncGateway {
    override val syncState = delegate.syncState
    override val syncEvents = delegate.syncEvents

    override suspend fun prepareDeletionTombstone(
        unitKey: String,
        entityType: String,
        entityId: String
    ): SyncTombstone = delegate.prepareDeletionTombstone(unitKey, entityType, entityId)

    override suspend fun syncAndReconcileAll(
        unitKey: String,
        callsign: String,
        unitName: String
    ): Pair<Boolean, String> =
        delegate.syncAndReconcileAll(unitKey, callsign, unitName)

    override fun pushOperationAsync(
        unitKey: String,
        op: OperationRecord,
        updatedStocks: List<StockRecord>
    ) = delegate.pushOperationAsync(unitKey, op, updatedStocks)

    override fun pushStockRecordAsync(unitKey: String, stock: StockRecord) =
        delegate.pushStockRecordAsync(unitKey, stock)

    override fun pushRequisitionAsync(unitKey: String, requisition: RequisitionRequest) =
        delegate.pushRequisitionAsync(unitKey, requisition)

    override fun deleteRequisitionAsync(unitKey: String, requisitionId: String) =
        delegate.deleteRequisitionAsync(unitKey, requisitionId)

    override fun deleteOperationAsync(unitKey: String, operationId: String) =
        delegate.deleteOperationAsync(unitKey, operationId)

    override fun pushWarehousePointAsync(unitKey: String, point: WarehousePoint) =
        delegate.pushWarehousePointAsync(unitKey, point)

    override fun deleteWarehousePointAsync(unitKey: String, pointId: String) =
        delegate.deleteWarehousePointAsync(unitKey, pointId)

    override fun pushInventoryItemAsync(unitKey: String, item: InventoryItem) =
        delegate.pushInventoryItemAsync(unitKey, item)

    override fun deleteInventoryItemAsync(unitKey: String, itemId: String) =
        delegate.deleteInventoryItemAsync(unitKey, itemId)

    override fun clearCloudDataAsync(unitKey: String) =
        delegate.clearCloudDataAsync(unitKey)
}
