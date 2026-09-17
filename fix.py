with open("app/src/main/java/com/example/data/sync/FirebaseSyncManager.kt", "r") as f:
    content = f.read()

# 1. In registerUnitListeners pointsReg:
old_listener = """                    if (dc.type == DocumentChange.Type.REMOVED) {
                        dao.deletePoint(p.id)
                    } else {
                        dao.insertPoint(p)
                    }"""

new_listener = """                    if (dc.type == DocumentChange.Type.REMOVED) {
                        dao.deletePoint(p.id)
                        dao.deleteStockForPoint(p.id)
                    } else {
                        dao.insertPoint(p)
                    }"""

if old_listener in content:
    content = content.replace(old_listener, new_listener, 1)
    print("Replaced listener logic successfully")
else:
    print("Warning: old_listener not found")

# 2. In deleteWarehousePointAsync: delete point AND all its stock_records in Firestore
old_del = """    fun deleteWarehousePointAsync(unitKey: String, pointId: String) {
        if (unitKey.isEmpty()) return
        scope.launch(Dispatchers.IO) {
            try {
                val db = firestore
                db.collection("units").document(unitKey)
                    .collection("warehouse_points").document(pointId).delete()
            } catch (e: Exception) {
                Log.w(TAG, "Failed deleting point live", e)
            }
        }
    }"""

new_del = """    fun deleteWarehousePointAsync(unitKey: String, pointId: String) {
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

if old_del in content:
    content = content.replace(old_del, new_del, 1)
    print("Replaced deleteWarehousePointAsync successfully")
else:
    print("Warning: old_del not found")

# 3. In syncUnitData: DO NOT auto-resurrect points that are not in cloud points collection!
# Points should only exist if they are actually in warehouse_points collection or if the unit is completely fresh!
old_resurrect = """            // Restore any points referenced by active cloud stocks if missing from cloud points
            for (ptId in stockPointIds) {
                if (!existingPointsMap.containsKey(ptId)) {
                    val fallbackPt = defaultPointsMap[ptId] ?: WarehousePoint(
                        id = ptId,
                        name = when (ptId) {
                            "med_sklad" -> "Медпункт"
                            "point_1" -> "Передовая точка (ЛБС)"
                            "base_sklad" -> "Базовый склад (КЗ)"
                            else -> "Склад $ptId"
                        },
                        description = if (ptId == "med_sklad") "Медицинское обеспечение" else "Точка учета",
                        isBase = (ptId == "base_sklad")
                    )
                    existingPointsMap[ptId] = fallbackPt
                    pushWarehousePointAsync(cleanKey, fallbackPt)
                }
            }"""

new_resurrect = """            // If a warehouse point was deleted from cloud, also clean up any orphaned stock records for it
            val orphanedPointIds = stockPointIds.filter { !existingPointsMap.containsKey(it) && it != "base_sklad" }
            if (orphanedPointIds.isNotEmpty()) {
                for (orphanId in orphanedPointIds) {
                    dao.deleteStockForPoint(orphanId)
                    // Also clean up in cloud asynchronously
                    deleteWarehousePointAsync(cleanKey, orphanId)
                }
            }"""

if old_resurrect in content:
    content = content.replace(old_resurrect, new_resurrect, 1)
    print("Replaced resurrect logic successfully")
else:
    print("Warning: old_resurrect not found")

with open("app/src/main/java/com/example/data/sync/FirebaseSyncManager.kt", "w") as f:
    f.write(content)
print("Saved FirebaseSyncManager.kt")
