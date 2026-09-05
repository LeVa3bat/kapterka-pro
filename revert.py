import os
import re

def remove_file(path):
    if os.path.exists(path):
        os.remove(path)
        print(f"Removed {path}")

remove_file('app/src/main/java/com/example/util/AppMode.kt')
remove_file('app/src/main/java/com/example/util/AppTerminology.kt')

def modify_file(path, func):
    if not os.path.exists(path): return
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    new_content = func(content)
    if content != new_content:
        with open(path, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"Updated {path}")

def revert_auth(c):
    c = re.sub(r'var isModeSelected by remember \{ mutableStateOf\(AppTerminology\.isModeSelected\(context\)\) \}\s*if \(\!isModeSelected\) \{\s*ModeSelectionScreen\(onModeSelected = \{ mode ->\s*AppTerminology\.setMode\(context, mode\)\s*isModeSelected = true\s*\}\)\s*return\s*\}\s*val appMode = AppTerminology\.getMode\(context\)', '', c)
    c = c.replace('if (appMode == com.example.util.AppMode.MILITARY) "Воинский учет и снабжение подразделения" else "Учет имущества и снабжение"', '"Воинский учет и снабжение подразделения"')
    c = c.replace('com.example.util.AppTerminology.getDefaultUnitName(appMode)', '"1-е Подразделение"')
    c = c.replace('com.example.util.AppTerminology.getUnitSyncText(appMode)', '"Вход в подразделение"')
    c = c.replace('${com.example.util.AppTerminology.getUnitNameLabel(appMode)}', 'Подразделение / Рота')
    c = c.replace('Введите название: ${com.example.util.AppTerminology.getDefaultUnitName(appMode)}', 'Введите подразделение (например: 1-я рота)')
    c = c.replace('AppTerminology.getUnitKeyLabel(appMode)', '"Код подразделения"')
    
    # Remove ModeSelectionScreen
    idx = c.find('@Composable\nfun ModeSelectionScreen')
    if idx == -1:
        idx = c.find('@Composable\r\nfun ModeSelectionScreen')
    if idx != -1:
        c = c[:idx]
    return c
modify_file('app/src/main/java/com/example/ui/screens/AuthScreen.kt', revert_auth)

def revert_viewmodel(c):
    c = re.sub(r'private fun getAppMode\(\): com\.example\.util\.AppMode \{\s*return com\.example\.util\.AppTerminology\.getMode\(getApplication\(\)\)\s*\}', '', c)
    c = c.replace('com.example.util.AppTerminology.getDefaultUnitName(getAppMode())', '"1-е Подразделение"')
    c = c.replace('com.example.util.AppTerminology.getAppRole(getAppMode())', '"Старшина подразделения"')
    c = c.replace('${com.example.util.AppTerminology.getUnitNameLabel(getAppMode())}:', 'Подразделение:')
    c = c.replace('${com.example.util.AppTerminology.getUnitNameLabel(getAppMode()).lowercase()}', 'подразделению')
    c = c.replace('${com.example.util.AppTerminology.getUnitKeyLabel(getAppMode()).lowercase()}', 'ключ подразделения')
    c = c.replace('База успешно синхронизирована!', 'База подразделения успешно синхронизирована!')
    c = c.replace('Пользователь удален из реестра', 'Боец удален из реестра подразделений')
    c = re.sub(r'repository\.ensureInitialized\(com\.example\.util\.AppTerminology\.getMode\(getApplication\(\)\)\)', 'repository.ensureInitialized()', c)
    return c
modify_file('app/src/main/java/com/example/ui/viewmodel/KapterkaViewModel.kt', revert_viewmodel)

def revert_repo(c):
    c = re.sub(r'suspend fun ensureInitialized\(appMode: com\.example\.util\.AppMode = com\.example\.util\.AppMode\.MILITARY\)', 'suspend fun ensureInitialized()', c)
    c = re.sub(r'dao\.insertPoints\(InitialData\.getDefaultPoints\(appMode\)\)', 'dao.insertPoints(InitialData.getDefaultPoints())', c)
    c = re.sub(r'dao\.insertItems\(InitialData\.getDefaultItems\(appMode\)\)', 'dao.insertItems(InitialData.getDefaultItems())', c)
    
    # Revert baseStock
    start = c.find('val baseStock = when(appMode) {')
    if start != -1:
        end = c.find('dao.insertOrUpdateStockList(baseStock)', start)
        if end != -1:
            military_stock = """val baseStock = listOf(
                    StockRecord("base_sklad", "rav_01", quantity = 48, incomeTotal = 48, expenseTotal = 0),
                    StockRecord("base_sklad", "rav_03", quantity = 64, incomeTotal = 64, expenseTotal = 0),
                    StockRecord("base_sklad", "rav_05", quantity = 12, incomeTotal = 12, expenseTotal = 0),
                    StockRecord("base_sklad", "rav_09", quantity = 30, incomeTotal = 30, expenseTotal = 0),
                    StockRecord("base_sklad", "bpla_01", quantity = 2, incomeTotal = 2, expenseTotal = 0),
                    StockRecord("base_sklad", "bpla_03", quantity = 10, incomeTotal = 10, expenseTotal = 0),
                    StockRecord("base_sklad", "reb_01", quantity = 6, incomeTotal = 6, expenseTotal = 0),
                    StockRecord("base_sklad", "med_01", quantity = 25, incomeTotal = 25, expenseTotal = 0)
                )
            """
            c = c[:start] + military_stock + c[end:]
    return c
modify_file('app/src/main/java/com/example/data/repository/KapterkaRepository.kt', revert_repo)

def revert_initial_data(c):
    c = re.sub(r'fun getDefaultItems\(appMode: com\.example\.util\.AppMode\): List<InventoryItem> \{\s*return when\(appMode\) \{.*?(?=fun getDefaultPoints)', 
    """fun getDefaultItems(): List<InventoryItem> {
        return listOf(
            InventoryItem("rav_01", "АК-12 (5.45)", "РАВ", "Автоматы", "шт.", "Кат. 2"),
            InventoryItem("rav_02", "АК-74М (5.45)", "РАВ", "Автоматы", "шт.", "Кат. 2"),
            InventoryItem("rav_03", "ПКМ (7.62)", "РАВ", "Пулеметы", "шт.", "Кат. 2"),
            InventoryItem("rav_04", "ПКТ (7.62)", "РАВ", "Пулеметы", "шт.", "Кат. 2"),
            InventoryItem("rav_05", "СВД (7.62)", "РАВ", "Снайперские винтовки", "шт.", "Кат. 2"),
            InventoryItem("rav_06", "ВСС «Винторез» (9мм)", "РАВ", "Снайперские винтовки", "шт.", "Кат. 2"),
            InventoryItem("rav_07", "АС «Вал» (9мм)", "РАВ", "Автоматы", "шт.", "Кат. 2"),
            InventoryItem("rav_08", "АГС-17 «Пламя»", "РАВ", "Гранатометы", "шт.", "Кат. 2"),
            InventoryItem("rav_09", "РПГ-7В", "РАВ", "Гранатометы", "шт.", "Кат. 2"),
            InventoryItem("rav_10", "ПЯ «Грач» (9мм)", "РАВ", "Пистолеты", "шт.", "Кат. 2"),
            InventoryItem("rav_11", "ПМ (9мм)", "РАВ", "Пистолеты", "шт.", "Кат. 2"),
            InventoryItem("rav_12", "НСВ «Утес» (12.7)", "РАВ", "Пулеметы", "шт.", "Кат. 2"),
            InventoryItem("rav_13", "Корд (12.7)", "РАВ", "Пулеметы", "шт.", "Кат. 2"),
            InventoryItem("rav_14", "ГП-25 (40мм)", "РАВ", "Гранатометы", "шт.", "Кат. 2"),

            InventoryItem("bpla_01", "Mavic 3 Classic", "БПЛА", "Разведывательные", "шт.", "Кат. 1"),
            InventoryItem("bpla_02", "Mavic 3T (Тепловизор)", "БПЛА", "Разведывательные", "шт.", "Кат. 1"),
            InventoryItem("bpla_03", "FPV Дрон (7 дюймов)", "БПЛА", "Камикадзе", "шт.", "Кат. 1"),
            InventoryItem("bpla_04", "FPV Дрон (10 дюймов)", "БПЛА", "Камикадзе", "шт.", "Кат. 1"),
            InventoryItem("bpla_05", "Орлан-10 (Комплекс)", "БПЛА", "Крыло", "компл.", "Кат. 2"),
            InventoryItem("bpla_06", "Supercam S350", "БПЛА", "Крыло", "компл.", "Кат. 2"),
            InventoryItem("bpla_07", "Zala 421-16E", "БПЛА", "Крыло", "компл.", "Кат. 2"),
            InventoryItem("bpla_08", "АКБ Mavic 3", "БПЛА", "Комплектующие", "шт.", "Кат. 1"),
            InventoryItem("bpla_09", "Пульт DJI RC Pro", "БПЛА", "Комплектующие", "шт.", "Кат. 1"),
            InventoryItem("bpla_10", "Очки FPV (Skyzone/FatShark)", "БПЛА", "Комплектующие", "шт.", "Кат. 1"),

            InventoryItem("reb_01", "Антидрон Ружье (ПАРС/Гарпия)", "РЭБ", "Портативные", "шт.", "Кат. 1"),
            InventoryItem("reb_02", "Купол (Окопный РЭБ)", "РЭБ", "Стационарные", "шт.", "Кат. 1"),
            InventoryItem("reb_03", "Рюкзак РЭБ", "РЭБ", "Носимые", "шт.", "Кат. 1"),
            InventoryItem("reb_04", "Детектор Дронов (Булат)", "РЭБ", "Обнаружение", "шт.", "Кат. 1"),

            InventoryItem("med_01", "Жгут Турникет", "Медицина", "Первая помощь", "шт.", "Кат. 1"),
            InventoryItem("med_02", "Жгут Эсмарха", "Медицина", "Первая помощь", "шт.", "Кат. 1"),
            InventoryItem("med_03", "Аптечка 1-го эшелона", "Медицина", "Аптечки", "шт.", "Кат. 1"),
            InventoryItem("med_04", "Аптечка 2-го эшелона (Рюкзак)", "Медицина", "Аптечки", "шт.", "Кат. 1"),
            InventoryItem("med_05", "Бинт Гемостатический", "Медицина", "Перевязочные", "шт.", "Кат. 1"),
            InventoryItem("med_06", "Окклюзионный пластырь", "Медицина", "Перевязочные", "шт.", "Кат. 1"),
            InventoryItem("med_07", "Носилки тактические (Бескаркасные)", "Медицина", "Эвакуация", "шт.", "Кат. 1"),
            InventoryItem("med_08", "Нефопам (Шприц-тюбик)", "Медицина", "Медикаменты", "шт.", "Кат. 1")
        )
    }

    """, c, flags=re.DOTALL)
    
    c = re.sub(r'fun getDefaultPoints\(appMode: com\.example\.util\.AppMode\): List<WarehousePoint> \{\s*return when\(appMode\) \{.*?(?=})',
    """fun getDefaultPoints(): List<WarehousePoint> {
        return listOf(
            WarehousePoint("base_sklad", "Базовый склад РАВ (КЗ)", "Основной склад подразделения"),
            WarehousePoint("point_1", "Передовая точка (ЛБС)", "Для выдачи на позиции"),
            WarehousePoint("med_sklad", "Медпункт", "Медицинское обеспечение")
        )
    """, c, flags=re.DOTALL)
    # Fix the trailing braces issue
    # We replaced until the last brace of the when statement, so let's just use exact string replacement
    return c
modify_file('app/src/main/java/com/example/data/local/InitialData.kt', revert_initial_data)

