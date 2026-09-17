with open("app/src/main/java/com/example/data/sync/FirebaseSyncManager.kt") as f:
    text = f.read()

target = """            // If unit has no points at all in cloud, seed defaults
            if (existingPointsMap.isEmpty()) {
                val defaults = com.example.data.local.InitialData.getDefaultPoints()
                for (p in defaults) {
                    existingPointsMap[p.id] = p
                    pushWarehousePointAsync(cleanKey, p)
                }
            }

            // Always ensure base warehouse exists
            if (!existingPointsMap.containsKey("base_sklad")) {
                val base = defaultPointsMap["base_sklad"] ?: WarehousePoint(
                    id = "base_sklad",
                    name = "Базовый склад (КЗ)",
                    description = "Основной склад подразделения",
                    isBase = true
                )
                existingPointsMap["base_sklad"] = base
                pushWarehousePointAsync(cleanKey, base)
            }"""

replacement = """            // Always ensure the root base warehouse exists (only base_sklad is protected)
            if (!existingPointsMap.containsKey("base_sklad")) {
                val base = defaultPointsMap["base_sklad"] ?: WarehousePoint(
                    id = "base_sklad",
                    name = "Базовый склад (КЗ)",
                    description = "Основной склад подразделения",
                    isBase = true
                )
                existingPointsMap["base_sklad"] = base
                pushWarehousePointAsync(cleanKey, base)
            }"""

if target in text:
    text = text.replace(target, replacement, 1)
    with open("app/src/main/java/com/example/data/sync/FirebaseSyncManager.kt", "w") as f:
        f.write(text)
    print("Fixed seed logic: only base_sklad is auto-ensured, deleted points won't come back!")
else:
    print("Error: target not found")
