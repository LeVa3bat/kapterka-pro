with open("app/src/main/java/com/example/data/repository/KapterkaRepository.kt") as f:
    text = f.read()

target = """    suspend fun deleteWarehousePoint(id: String) {
        dao.deletePoint(id)
        dao.deleteStockForPoint(id)
        syncManager?.deleteWarehousePointAsync(getCurrentUnitKey(), id)
    }"""

replacement = """    suspend fun deleteWarehousePoint(id: String) {
        dao.deletePoint(id)
        dao.deleteStockForPoint(id)
        syncManager?.deleteWarehousePointAsync(getCurrentUnitKey(), id)
    }"""

print("Target in repo:", target in text)
