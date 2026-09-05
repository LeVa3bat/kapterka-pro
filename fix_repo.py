import re

with open('app/src/main/java/com/example/data/repository/KapterkaRepository.kt', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('suspend fun initialize(): UserProfile {', 'suspend fun initialize(appMode: com.example.util.AppMode = com.example.util.AppMode.MILITARY): UserProfile {')
content = content.replace('dao.insertPoints(InitialData.defaultPoints)', 'dao.insertPoints(InitialData.getDefaultPoints(appMode))')
content = content.replace('dao.insertItems(InitialData.defaultItems)', 'dao.insertItems(InitialData.getDefaultItems(appMode))')

base_stock_regex = r'val baseStock = listOf\([\s\S]*?dao\.insertStockRecords\(baseStock\)'
new_base_stock = """val baseStock = when(appMode) {
                com.example.util.AppMode.CONSTRUCTION -> listOf(
                    StockRecord("base_sklad", "const_01", quantity = 2, incomeTotal = 2, expenseTotal = 0),
                    StockRecord("base_sklad", "const_02", quantity = 5, incomeTotal = 5, expenseTotal = 0),
                    StockRecord("base_sklad", "const_03", quantity = 50, incomeTotal = 50, expenseTotal = 0)
                )
                com.example.util.AppMode.WAREHOUSE -> listOf(
                    StockRecord("base_sklad", "wh_01", quantity = 500, incomeTotal = 500, expenseTotal = 0),
                    StockRecord("base_sklad", "wh_02", quantity = 100, incomeTotal = 100, expenseTotal = 0)
                )
                com.example.util.AppMode.UNIVERSAL -> listOf(
                    StockRecord("base_sklad", "univ_01", quantity = 20, incomeTotal = 20, expenseTotal = 0)
                )
                else -> listOf(
                    StockRecord("base_sklad", "rav_01", quantity = 48, incomeTotal = 48, expenseTotal = 0),
                    StockRecord("base_sklad", "rav_03", quantity = 64, incomeTotal = 64, expenseTotal = 0),
                    StockRecord("base_sklad", "rav_05", quantity = 12, incomeTotal = 12, expenseTotal = 0),
                    StockRecord("base_sklad", "rav_09", quantity = 30, incomeTotal = 30, expenseTotal = 0),
                    StockRecord("base_sklad", "bpla_01", quantity = 2, incomeTotal = 2, expenseTotal = 0),
                    StockRecord("base_sklad", "bpla_03", quantity = 10, incomeTotal = 10, expenseTotal = 0),
                    StockRecord("base_sklad", "reb_01", quantity = 6, incomeTotal = 6, expenseTotal = 0),
                    StockRecord("base_sklad", "med_01", quantity = 25, incomeTotal = 25, expenseTotal = 0)
                )
            }
            dao.insertStockRecords(baseStock)"""

content = re.sub(base_stock_regex, new_base_stock, content)

with open('app/src/main/java/com/example/data/repository/KapterkaRepository.kt', 'w', encoding='utf-8') as f:
    f.write(content)
print("Fixed Repo")
