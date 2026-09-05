with open('app/src/main/java/com/example/data/repository/KapterkaRepository.kt', 'r', encoding='utf-8') as f:
    content = f.read()

target = "    suspend fun recordIncome"

missing = """
    suspend fun deleteCategory(categoryName: String, deleteItems: Boolean = false) {
        if (deleteItems) {
            dao.deleteItemsByCategory(categoryName)
        }
    }

    val syncState = syncManager?.syncState ?: kotlinx.coroutines.flow.flowOf("Оффлайн (отключен)")

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
"""

if "fun deleteCategory" not in content:
    content = content.replace(target, missing + "\n" + target)

with open('app/src/main/java/com/example/data/repository/KapterkaRepository.kt', 'w', encoding='utf-8') as f:
    f.write(content)
print("Added missing methods back")
