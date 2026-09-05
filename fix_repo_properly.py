import re

with open('app/src/main/java/com/example/data/repository/KapterkaRepository.kt', 'r', encoding='utf-8') as f:
    content = f.read()

target_start = "    suspend fun recordIncome("
if target_start in content:
    content = content[:content.find(target_start)]

new_funcs = """    suspend fun recordIncome(toPointId: String, toPointName: String, supplier: String, items: List<OperationItemEntry>, comment: String, actor: String) {
        val summary = items.joinToString(", ") { "${it.itemName} - ${it.quantity} ${it.unit}" }
        val itemsJson = serializeOperationItems(items)
        val op = OperationRecord(java.util.UUID.randomUUID().toString(), OperationType.INCOME, supplier, toPointName, "", actor, comment, System.currentTimeMillis(), summary, itemsJson)
        dao.insertOperation(op)
        for (item in items) {
            adjustStockQuantity(toPointId, item.itemId, item.quantity, isIncome = true)
        }
    }

    suspend fun recordTransfer(fromPointId: String, fromPointName: String, toPointId: String, toPointName: String, items: List<OperationItemEntry>, comment: String, actor: String) {
        val summary = items.joinToString(", ") { "${it.itemName} - ${it.quantity} ${it.unit}" }
        val itemsJson = serializeOperationItems(items)
        val op = OperationRecord(java.util.UUID.randomUUID().toString(), OperationType.TRANSFER, fromPointName, toPointName, "", actor, comment, System.currentTimeMillis(), summary, itemsJson)
        dao.insertOperation(op)
        for (item in items) {
            adjustStockQuantity(fromPointId, item.itemId, -item.quantity, isIncome = false)
            adjustStockQuantity(toPointId, item.itemId, item.quantity, isIncome = true)
        }
    }

    suspend fun recordIssue(fromPointId: String, fromPointName: String, toPointId: String, toPointName: String, items: List<OperationItemEntry>, comment: String, actor: String) {
        val summary = items.joinToString(", ") { "${it.itemName} - ${it.quantity} ${it.unit}" }
        val itemsJson = serializeOperationItems(items)
        val op = OperationRecord(java.util.UUID.randomUUID().toString(), OperationType.ISSUE, fromPointName, toPointName, "", actor, comment, System.currentTimeMillis(), summary, itemsJson)
        dao.insertOperation(op)
        for (item in items) {
            adjustStockQuantity(fromPointId, item.itemId, -item.quantity, isIncome = false)
            // No destination stock update because it's issued to soldiers (off-balance)
        }
    }

    suspend fun recordExpenditure(fromPointId: String, pointName: String, docNumber: String, responsiblePerson: String, items: List<OperationItemEntry>, comment: String) {
        val summary = items.joinToString(", ") { "${it.itemName} - ${it.quantity} ${it.unit}" }
        val itemsJson = serializeOperationItems(items)
        val op = OperationRecord(java.util.UUID.randomUUID().toString(), OperationType.EXPENDITURE, pointName, "Списание", docNumber, responsiblePerson, comment, System.currentTimeMillis(), summary, itemsJson)
        dao.insertOperation(op)
        for (item in items) {
            adjustStockQuantity(fromPointId, item.itemId, -item.quantity, isIncome = false)
        }
    }

    suspend fun addWarehousePoint(name: String, desc: String) = dao.insertPoint(WarehousePoint(java.util.UUID.randomUUID().toString(), name, desc))
    suspend fun updateWarehousePoint(p: WarehousePoint) = dao.updatePoint(p)
    suspend fun deleteWarehousePoint(id: String) = dao.deletePoint(id)
    suspend fun addCustomInventoryItem(name: String, category: String, subCategory: String, unit: String) = dao.insertItem(InventoryItem(java.util.UUID.randomUUID().toString(), name, category, subCategory, unit, "Кат. 1"))
    suspend fun updateInventoryItem(i: InventoryItem) = dao.insertItem(i)
    suspend fun deleteInventoryItem(id: String) = dao.deleteItem(id)
    suspend fun createRequisition(pointName: String, applicant: String, items: List<RequisitionItemEntry>, comment: String) {
        val summary = items.joinToString(", ") { "${it.itemName} - ${it.quantity} ${it.unit}" }
        val itemsJson = serializeRequisitionItems(items)
        dao.insertRequisition(RequisitionRequest(java.util.UUID.randomUUID().toString(), pointName, applicant, RequestStatus.PENDING, comment, System.currentTimeMillis(), summary, itemsJson))
    }
    
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
                incomeTotal = current.incomeTotal + if(isIncome && change > 0) change else 0,
                expenseTotal = current.expenseTotal + if(!isIncome || change < 0) java.lang.Math.abs(change) else 0
            ))
        } else {
            dao.insertOrUpdateStock(StockRecord(pointId, itemId, change, if(isIncome && change > 0) change else 0, if(!isIncome || change < 0) java.lang.Math.abs(change) else 0))
        }
    }

    fun generateForm8ExcelText(ops: List<OperationRecord>, unit: String): String = "Отчет"
    fun generateForm18ExcelText(ops: List<OperationRecord>, unit: String): String = "Отчет"
    
    private fun serializeOperationItems(items: List<OperationItemEntry>): String {
        val arr = org.json.JSONArray()
        items.forEach { 
            val obj = org.json.JSONObject()
            obj.put("itemId", it.itemId)
            obj.put("itemName", it.itemName)
            obj.put("unit", it.unit)
            obj.put("quantity", it.quantity)
            obj.put("categoryClass", it.categoryClass)
            obj.put("reason", it.reason)
            arr.put(obj)
        }
        return arr.toString()
    }
    
    fun parseOperationItems(json: String): List<OperationItemEntry> {
        if (json.isBlank()) return emptyList()
        return try {
            val list = mutableListOf<OperationItemEntry>()
            val arr = org.json.JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(OperationItemEntry(
                    itemId = obj.optString("itemId", ""),
                    itemName = obj.optString("itemName", ""),
                    unit = obj.optString("unit", ""),
                    quantity = obj.optInt("quantity", 0),
                    categoryClass = obj.optString("categoryClass", ""),
                    reason = obj.optString("reason", "")
                ))
            }
            list
        } catch (e: Exception) { emptyList() }
    }
    
    private fun serializeRequisitionItems(items: List<RequisitionItemEntry>): String {
        val arr = org.json.JSONArray()
        items.forEach { 
            val obj = org.json.JSONObject()
            obj.put("itemName", it.itemName)
            obj.put("unit", it.unit)
            obj.put("quantity", it.quantity)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun parseRequisitionItems(json: String): List<RequisitionItemEntry> {
        if (json.isBlank()) return emptyList()
        return try {
            val list = mutableListOf<RequisitionItemEntry>()
            val arr = org.json.JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(RequisitionItemEntry(
                    itemName = obj.optString("itemName", ""),
                    unit = obj.optString("unit", ""),
                    quantity = obj.optInt("quantity", 0)
                ))
            }
            list
        } catch (e: Exception) { emptyList() }
    }
}
"""

content += new_funcs

with open('app/src/main/java/com/example/data/repository/KapterkaRepository.kt', 'w', encoding='utf-8') as f:
    f.write(content)

print("Proper functions applied to KapterkaRepository.kt")
