with open("app/src/main/java/com/example/data/sync/FirebaseSyncManager.kt") as f:
    text = f.read()

assert "fun deleteWarehousePointAsync" in text
assert "whereEqualTo(\"pointId\", pointId)" in text
assert "delete().await()" in text
assert "orphanedPointIds" in text
assert "val parts = if (docId.contains(\"___\"))" in text
print("All assertions passed!")
