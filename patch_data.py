import re

with open('app/src/main/java/com/example/data/local/InitialData.kt', 'r') as f:
    content = f.read()

new_items = """
        // Артиллерийское вооружение и боеприпасы (добавлено по запросу)
        InventoryItem("rav_27", "Мина 120-мм дымовая Д-843А", "Служба РАВ", "Минометные выстрелы", "шт.", "Кат. 1"),
        InventoryItem("rav_28", "Мина 120-мм осветительная С-843", "Служба РАВ", "Минометные выстрелы", "шт.", "Кат. 1"),
        InventoryItem("rav_29", "Мина 82-мм дымовая Д-832ДУ", "Служба РАВ", "Минометные выстрелы", "шт.", "Кат. 1"),
        InventoryItem("rav_30", "Мина 82-мм осветительная С-832С", "Служба РАВ", "Минометные выстрелы", "шт.", "Кат. 1"),
        InventoryItem("rav_31", "Порох минометный (метательный заряд) НБЛ-35", "Служба РАВ", "Пороха", "шт.", "Кат. 1"),
        InventoryItem("rav_32", "Заряд дальнобойный минометный", "Служба РАВ", "Пороха", "шт.", "Кат. 1"),"""

content = content.replace("// Артиллерийское вооружение и боеприпасы (добавлено по запросу)", new_items.strip())

with open('app/src/main/java/com/example/data/local/InitialData.kt', 'w') as f:
    f.write(content)
