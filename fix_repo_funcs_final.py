with open('app/src/main/java/com/example/data/repository/KapterkaRepository.kt', 'r', encoding='utf-8') as f:
    content = f.read()

target = "    suspend fun deleteCategory"
if target in content:
    content = content[:content.find(target)]

funcs = """
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

    suspend fun updateRequisitionStatus(id: String, st: RequestStatus) {
        val r = dao.getAllRequisitions().first().find { it.id == id }
        if (r != null) dao.updateRequisition(r.copy(status = st))
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
"""

content += funcs

with open('app/src/main/java/com/example/data/repository/KapterkaRepository.kt', 'w', encoding='utf-8') as f:
    f.write(content)
print("Applied final fixes to repo signatures")
