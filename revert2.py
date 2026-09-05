import re
def revert_initial_data(c):
    with open('app/src/main/java/com/example/data/local/InitialData.kt', 'r', encoding='utf-8') as f:
        c = f.read()
    
    start = c.find('fun getDefaultItems(appMode:')
    end = c.find('fun getDefaultPoints(appMode:')
    
    items = """    fun getDefaultItems(): List<InventoryItem> {
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

"""
    points = """    fun getDefaultPoints(): List<WarehousePoint> {
        return listOf(
            WarehousePoint("base_sklad", "Базовый склад РАВ (КЗ)", "Основной склад подразделения"),
            WarehousePoint("point_1", "Передовая точка (ЛБС)", "Для выдачи на позиции"),
            WarehousePoint("med_sklad", "Медпункт", "Медицинское обеспечение")
        )
    }
}
"""
    c = c[:start] + items + points
    with open('app/src/main/java/com/example/data/local/InitialData.kt', 'w', encoding='utf-8') as f:
        f.write(c)

revert_initial_data('')
