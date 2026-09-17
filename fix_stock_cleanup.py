with open("app/src/main/java/com/example/data/sync/FirebaseSyncManager.kt") as f:
    text = f.read()

target = """                for (doc in cloudStocksSnap.documents) {
                    val ptId = doc.getString("pointId") ?: ""
                    val itemId = doc.getString("itemId") ?: ""
                    val qty = doc.getLong("quantity")?.toInt() ?: 0
                    val inc = doc.getLong("incomeTotal")?.toInt() ?: 0
                    val exp = doc.getLong("expenseTotal")?.toInt() ?: 0
                    val updated = doc.getLong("lastUpdated") ?: 0L

                    if (ptId.isNotBlank() && itemId.isNotBlank()) {
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
                }"""

replacement = """                for (doc in cloudStocksSnap.documents) {
                    val ptId = doc.getString("pointId") ?: ""
                    val itemId = doc.getString("itemId") ?: ""
                    val qty = doc.getLong("quantity")?.toInt() ?: 0
                    val inc = doc.getLong("incomeTotal")?.toInt() ?: 0
                    val exp = doc.getLong("expenseTotal")?.toInt() ?: 0
                    val updated = doc.getLong("lastUpdated") ?: 0L

                    if (ptId.isNotBlank() && itemId.isNotBlank()) {
                        // Do not re-insert stocks belonging to points that no longer exist in cloud
                        if (existingPointsMap.containsKey(ptId) || ptId == "base_sklad") {
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
                }"""

if target in text:
    text = text.replace(target, replacement, 1)
    with open("app/src/main/java/com/example/data/sync/FirebaseSyncManager.kt", "w") as f:
        f.write(text)
    print("Fixed stock records filter for existing points only!")
else:
    print("Error: target not found")
