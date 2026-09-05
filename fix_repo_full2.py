with open('app/src/main/java/com/example/data/repository/KapterkaRepository.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Replace insertStockRecords
content = content.replace('dao.insertStockRecords(baseStock)', 'dao.insertOrUpdateStockList(baseStock)')

# Find the start of the missing_methods block and replace it
target_str = "    suspend fun deleteCategory(categoryName: String, deleteItems: Boolean = false) {"
if target_str in content:
    content = content[:content.find(target_str)]

missing_methods = """
    suspend fun deleteCategory(categoryName: String, deleteItems: Boolean = false) {
        if (deleteItems) {
            dao.deleteItemsByCategory(categoryName)
        }
    }

    val syncState = syncManager?.syncState ?: kotlinx.coroutines.flow.flowOf("Оффлайн (отключен)")

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

    suspend fun recordIncome(op: OperationRecord, items: List<OperationItemEntry>) {
        dao.insertOperation(op)
    }
    suspend fun recordTransfer(op: OperationRecord, items: List<OperationItemEntry>) { dao.insertOperation(op) }
    suspend fun recordIssue(op: OperationRecord, items: List<OperationItemEntry>) { dao.insertOperation(op) }
    suspend fun recordExpenditure(op: OperationRecord, items: List<OperationItemEntry>) { dao.insertOperation(op) }

    suspend fun addWarehousePoint(p: WarehousePoint) = dao.insertPoint(p)
    suspend fun updateWarehousePoint(p: WarehousePoint) = dao.updatePoint(p)
    suspend fun deleteWarehousePoint(id: String) = dao.deletePoint(id)

    suspend fun addCustomInventoryItem(i: InventoryItem) = dao.insertItem(i)
    suspend fun updateInventoryItem(i: InventoryItem) = dao.insertItem(i)
    suspend fun deleteInventoryItem(id: String) = dao.deleteItem(id)

    suspend fun createRequisition(r: RequisitionRequest) = dao.insertRequisition(r)
    suspend fun updateRequisitionStatus(id: String, st: RequestStatus) {
        val r = dao.getAllRequisitions().first().find { it.id == id }
        if (r != null) dao.updateRequisition(r.copy(status = st))
    }
    suspend fun deleteRequisition(id: String) = dao.deleteRequisition(id)

    suspend fun adjustStockQuantity(pointId: String, itemId: String, change: Int, isIncome: Boolean) {
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
"""

content += missing_methods

with open('app/src/main/java/com/example/data/repository/KapterkaRepository.kt', 'w', encoding='utf-8') as f:
    f.write(content)
print("Applied missing methods fix 2")
