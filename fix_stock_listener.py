with open("app/src/main/java/com/example/data/sync/FirebaseSyncManager.kt") as f:
    text = f.read()

target = """                    val s = StockRecord(
                        pointId = dc.document.getString("pointId") ?: "",
                        itemId = dc.document.getString("itemId") ?: "",
                        quantity = dc.document.getLong("quantity")?.toInt() ?: 0,
                        incomeTotal = dc.document.getLong("incomeTotal")?.toInt() ?: 0,
                        expenseTotal = dc.document.getLong("expenseTotal")?.toInt() ?: 0,
                        lastUpdated = dc.document.getLong("lastUpdated") ?: 0L
                    )
                    if (dc.type == DocumentChange.Type.REMOVED) {
                        dao.deleteStockRecord(s.pointId, s.itemId)
                    } else {
                        dao.insertOrUpdateStock(s)
                    }"""

replacement = """                    val docId = dc.document.id
                    val parts = if (docId.contains("___")) docId.split("___") else emptyList()
                    val pId = dc.document.getString("pointId") ?: if (parts.size >= 2) parts[0] else ""
                    val iId = dc.document.getString("itemId") ?: if (parts.size >= 2) parts[1] else ""

                    if (dc.type == DocumentChange.Type.REMOVED) {
                        if (pId.isNotBlank() && iId.isNotBlank()) {
                            dao.deleteStockRecord(pId, iId)
                        }
                    } else {
                        val s = StockRecord(
                            pointId = pId,
                            itemId = iId,
                            quantity = dc.document.getLong("quantity")?.toInt() ?: 0,
                            incomeTotal = dc.document.getLong("incomeTotal")?.toInt() ?: 0,
                            expenseTotal = dc.document.getLong("expenseTotal")?.toInt() ?: 0,
                            lastUpdated = dc.document.getLong("lastUpdated") ?: 0L
                        )
                        if (s.pointId.isNotBlank() && s.itemId.isNotBlank()) {
                            dao.insertOrUpdateStock(s)
                        }
                    }"""

if target in text:
    text = text.replace(target, replacement, 1)
    with open("app/src/main/java/com/example/data/sync/FirebaseSyncManager.kt", "w") as f:
        f.write(text)
    print("Fixed stock listener!")
else:
    print("Error: target not found")
