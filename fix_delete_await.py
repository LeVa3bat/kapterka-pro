with open("app/src/main/java/com/example/data/sync/FirebaseSyncManager.kt") as f:
    text = f.read()

target = """    fun deleteWarehousePointAsync(unitKey: String, pointId: String) {
        if (unitKey.isEmpty()) return
        scope.launch(Dispatchers.IO) {
            try {
                val db = firestore
                val unitRef = db.collection("units").document(unitKey)
                // Delete warehouse point document in cloud
                unitRef.collection("warehouse_points").document(pointId).delete()

                // Delete all stock_records for this point in cloud
                val stocksSnap = unitRef.collection("stock_records")
                    .whereEqualTo("pointId", pointId)
                    .get().await()
                for (doc in stocksSnap.documents) {
                    doc.reference.delete()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed deleting point live", e)
            }
        }
    }"""

replacement = """    fun deleteWarehousePointAsync(unitKey: String, pointId: String) {
        if (unitKey.isEmpty()) return
        scope.launch(Dispatchers.IO) {
            try {
                val db = firestore
                val unitRef = db.collection("units").document(unitKey)
                // Delete warehouse point document in cloud with await
                unitRef.collection("warehouse_points").document(pointId).delete().await()

                // Delete all stock_records for this point in cloud with await
                val stocksSnap = unitRef.collection("stock_records")
                    .whereEqualTo("pointId", pointId)
                    .get().await()
                for (doc in stocksSnap.documents) {
                    try {
                        doc.reference.delete().await()
                    } catch (de: Exception) {
                        Log.w(TAG, "Error deleting stock doc ${doc.id}", de)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed deleting point live", e)
            }
        }
    }"""

if target in text:
    text = text.replace(target, replacement, 1)
    with open("app/src/main/java/com/example/data/sync/FirebaseSyncManager.kt", "w") as f:
        f.write(text)
    print("Updated deleteWarehousePointAsync with awaits")
else:
    print("Target not found!")
