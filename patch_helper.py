import re

with open('app/src/main/java/com/example/util/TacticalNotificationHelper.kt', 'r') as f:
    content = f.read()

target = """        val bigText = buildString {
            append("Маршрут: $fromPoint ➔ $toPoint")
            append("Выдано: $itemsSummary")
            if (baseWarehouseStockSummary.isNotEmpty()) {
                append("📦 Текущий остаток склада:$baseWarehouseStockSummary")
            }
        }"""

replacement = r"""        val bigText = buildString {
            append("Маршрут: $fromPoint ➔ $toPoint\n")
            append("Выдано: $itemsSummary\n\n")
            if (baseWarehouseStockSummary.isNotEmpty()) {
                append("📦 Текущий остаток склада:\n$baseWarehouseStockSummary")
            }
        }"""

content = content.replace(target, replacement)

with open('app/src/main/java/com/example/util/TacticalNotificationHelper.kt', 'w') as f:
    f.write(content)
