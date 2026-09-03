package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.InventoryItem
import com.example.data.model.OperationRecord
import com.example.data.model.RequisitionRequest
import com.example.data.model.StockRecord
import com.example.data.model.UserProfile
import com.example.data.model.WarehousePoint
import kotlinx.coroutines.flow.Flow

@Dao
interface KapterkaDao {
    // User Profile
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserProfile(profile: UserProfile)

    // Warehouses / Points
    @Query("SELECT * FROM warehouse_points ORDER BY orderIndex ASC, createdAt ASC")
    fun getAllPoints(): Flow<List<WarehousePoint>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoint(point: WarehousePoint)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoints(points: List<WarehousePoint>)

    @Query("DELETE FROM warehouse_points WHERE id = :pointId")
    suspend fun deletePoint(pointId: String)

    @Update
    suspend fun updatePoint(point: WarehousePoint)

    // Inventory Items (Catalog)
    @Query("SELECT * FROM inventory_items ORDER BY serviceCategory ASC, subType ASC, name ASC")
    fun getAllItems(): Flow<List<InventoryItem>>

    @Query("SELECT * FROM inventory_items WHERE serviceCategory = :category ORDER BY name ASC")
    fun getItemsByCategory(category: String): Flow<List<InventoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: InventoryItem)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItems(items: List<InventoryItem>)

    @Query("DELETE FROM inventory_items WHERE id = :itemId")
    suspend fun deleteItem(itemId: String)

    @Query("DELETE FROM inventory_items WHERE serviceCategory = :category")
    suspend fun deleteItemsByCategory(category: String)

    // Stock Records
    @Query("SELECT * FROM stock_records WHERE pointId = :pointId")
    fun getStockForPoint(pointId: String): Flow<List<StockRecord>>

    @Query("SELECT * FROM stock_records")
    fun getAllStockRecords(): Flow<List<StockRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStock(record: StockRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStockList(records: List<StockRecord>)

    @Query("SELECT * FROM stock_records WHERE pointId = :pointId AND itemId = :itemId LIMIT 1")
    suspend fun getStockItem(pointId: String, itemId: String): StockRecord?

    // Operations / History
    @Query("SELECT * FROM operation_records ORDER BY timestamp DESC")
    fun getAllOperations(): Flow<List<OperationRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperation(operation: OperationRecord)

    @Query("DELETE FROM operation_records WHERE id = :operationId")
    suspend fun deleteOperation(operationId: String)

    // Requisitions / Requests
    @Query("SELECT * FROM requisitions ORDER BY timestamp DESC")
    fun getAllRequisitions(): Flow<List<RequisitionRequest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequisition(requisition: RequisitionRequest)

    @Update
    suspend fun updateRequisition(requisition: RequisitionRequest)

    @Query("DELETE FROM requisitions WHERE id = :requisitionId")
    suspend fun deleteRequisition(requisitionId: String)

    @Query("DELETE FROM stock_records WHERE pointId = :pointId")
    suspend fun deleteStockForPoint(pointId: String)

    @Query("DELETE FROM stock_records WHERE itemId = :itemId")
    suspend fun deleteStockForItem(itemId: String)

    @Query("DELETE FROM stock_records WHERE pointId = :pointId AND itemId = :itemId")
    suspend fun deleteStockRecord(pointId: String, itemId: String)

    @Query("DELETE FROM stock_records")
    suspend fun clearAllStockRecords()

    @Query("DELETE FROM operation_records")
    suspend fun clearAllOperations()

    @Query("DELETE FROM requisitions")
    suspend fun clearAllRequisitions()
}
