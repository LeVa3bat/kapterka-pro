with open("app/src/main/java/com/example/data/sync/FirebaseSyncManager.kt") as f:
    text = f.read()

target = """            // If a warehouse point was deleted from cloud, also clean up any orphaned stock records for it
            val orphanedPointIds = stockPointIds.filter { !existingPointsMap.containsKey(it) && it != "base_sklad" }
            if (orphanedPointIds.isNotEmpty()) {
                for (orphanId in orphanedPointIds) {
                    dao.deleteStockForPoint(orphanId)
                    // Also clean up in cloud asynchronously
                    deleteWarehousePointAsync(cleanKey, orphanId)
                }
            }"""

replacement = """            // If a warehouse point was deleted from cloud, also clean up any orphaned stock records for it
            val orphanedPointIds = stockPointIds.filter { !existingPointsMap.containsKey(it) && it != "base_sklad" }
            if (orphanedPointIds.isNotEmpty()) {
                for (orphanId in orphanedPointIds) {
                    dao.deleteStockForPoint(orphanId)
                    try {
                        val orphanDocs = unitRef.collection("stock_records")
                            .whereEqualTo("pointId", orphanId)
                            .get().await()
                        for (doc in orphanDocs.documents) {
                            doc.reference.delete().await()
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed deleting orphaned cloud stock records for $orphanId", e)
                    }
                }
            }"""

if target in text:
    text = text.replace(target, replacement, 1)
    with open("app/src/main/java/com/example/data/sync/FirebaseSyncManager.kt", "w") as f:
        f.write(text)
    print("Fixed syncAndReconcileAll orphaned stock cleanup synchronously!")
else:
    print("Error: target not found")
