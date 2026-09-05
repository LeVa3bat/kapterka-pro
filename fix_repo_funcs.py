with open('app/src/main/java/com/example/data/repository/KapterkaRepository.kt', 'r', encoding='utf-8') as f:
    content = f.read()

target = "    suspend fun recordIncome(op: OperationRecord, items: List<OperationItemEntry>) {\n        dao.insertOperation(op)\n    }"

if target in content:
    content = content[:content.find(target)]

funcs = """
    suspend fun recordIncome(toPointId: String, toPointName: String, supplier: String, items: List<OperationItemEntry>, comment: String, actor: String) {
        val op = OperationRecord(UUID.randomUUID().toString(), OperationType.INCOME, supplier, toPointName, "", actor, comment, System.currentTimeMillis(), "", "")
        dao.insertOperation(op)
    }
    suspend fun recordTransfer(fromPointId: String, fromPointName: String, toPointId: String, toPointName: String, items: List<OperationItemEntry>, comment: String, actor: String) {
        val op = OperationRecord(UUID.randomUUID().toString(), OperationType.TRANSFER, fromPointName, toPointName, "", actor, comment, System.currentTimeMillis(), "", "")
        dao.insertOperation(op)
    }
    suspend fun recordIssue(fromPointId: String, fromPointName: String, toPerson: String, items: List<OperationItemEntry>, comment: String, actor: String) {
        val op = OperationRecord(UUID.randomUUID().toString(), OperationType.ISSUE, fromPointName, toPerson, "", actor, comment, System.currentTimeMillis(), "", "")
        dao.insertOperation(op)
    }
    suspend fun recordExpenditure(fromPointId: String, fromPointName: String, reason: String, items: List<OperationItemEntry>, comment: String, actor: String) {
        val op = OperationRecord(UUID.randomUUID().toString(), OperationType.EXPENDITURE, fromPointName, reason, "", actor, comment, System.currentTimeMillis(), "", "")
        dao.insertOperation(op)
    }

    suspend fun addWarehousePoint(id: String, name: String, desc: String) = dao.insertPoint(WarehousePoint(id, name, desc))
    suspend fun updateWarehousePoint(p: WarehousePoint) = dao.updatePoint(p)
    suspend fun deleteWarehousePoint(id: String) = dao.deletePoint(id)

    suspend fun addCustomInventoryItem(id: String, name: String, category: String, subCategory: String, unit: String) = dao.insertItem(InventoryItem(id, name, category, subCategory, unit, "Кат. 1"))
    suspend fun updateInventoryItem(i: InventoryItem) = dao.insertItem(i)
    suspend fun deleteInventoryItem(id: String) = dao.deleteItem(id)

    suspend fun createRequisition(pointName: String, applicant: String, items: List<RequisitionItemEntry>, comment: String) = dao.insertRequisition(RequisitionRequest(UUID.randomUUID().toString(), pointName, applicant, RequestStatus.PENDING, comment, System.currentTimeMillis(), "", ""))
    suspend fun updateRequisitionStatus(req: RequisitionRequest, st: RequestStatus) {
        dao.updateRequisition(req.copy(status = st))
    }
    suspend fun deleteRequisition(id: String) = dao.deleteRequisition(id)

    suspend fun adjustStockQuantity(pointId: String, itemId: String, change: Int) {
        val current = dao.getStockItem(pointId, itemId)
        val isIncome = change > 0
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
print("Fixed functions")
