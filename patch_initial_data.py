import re

with open('app/src/main/java/com/example/data/local/InitialData.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# I want to completely replace InitialData.kt content with a new version that accepts AppMode.
new_content = """package com.example.data.local

import com.example.data.model.InventoryItem
import com.example.data.model.WarehousePoint
import com.example.util.AppMode

object InitialData {

    fun getDefaultPoints(mode: AppMode): List<WarehousePoint> = when(mode) {
        AppMode.CONSTRUCTION -> listOf(
            WarehousePoint("base_sklad", "Главный склад (База)", "Основной склад инструмента и материалов"),
            WarehousePoint("point_1", "Объект 'Альфа'", "Текущая стройка"),
            WarehousePoint("point_2", "Бригада Монтажников", "Инструмент на руках")
        )
        AppMode.WAREHOUSE -> listOf(
            WarehousePoint("base_sklad", "Центральный склад", "Основное хранение"),
            WarehousePoint("point_1", "Зона отгрузки", "Транзитная зона"),
            WarehousePoint("point_2", "Склад брака", "Списание и ремонт")
        )
        AppMode.UNIVERSAL -> listOf(
            WarehousePoint("base_sklad", "Главный склад", "Основное хранение"),
            WarehousePoint("point_1", "В эксплуатации", "Выданное имущество")
        )
        else -> listOf(
            WarehousePoint("base_sklad", "Базовый склад РАВ (КЗ)", "Основной склад подразделения"),
            WarehousePoint("point_1", "1-й взвод (на руках)", "Имущество выданное личному составу"),
            WarehousePoint("point_2", "Склад ГСМ", "Топливо и смазочные материалы")
        )
    }

    fun getDefaultCategories(mode: AppMode): List<String> = when(mode) {
        AppMode.CONSTRUCTION -> listOf("Электроинструмент", "Расходники", "Стройматериалы", "СИЗ и Спецодежда", "Оборудование")
        AppMode.WAREHOUSE -> listOf("Товары", "Упаковка", "Техника", "Униформа", "Хозинвентарь")
        AppMode.UNIVERSAL -> listOf("Мебель", "Оргтехника", "Канцелярия", "Расходники", "Инвентарь")
        else -> listOf(
            "Служба РАВ", "Служба БПЛА и робототехники", "Служба связи и РЭБ",
            "Вещевая служба и СИБЗ", "Медицинская служба", "Продовольственная служба",
            "Служба ГСМ", "Инженерная служба", "Автомобильная и БТ служба",
            "Служба РХБЗ", "Топографическая и штабная", "Хозяйственное имущество"
        )
    }

    fun getDefaultItems(mode: AppMode): List<InventoryItem> = when(mode) {
        AppMode.CONSTRUCTION -> listOf(
            InventoryItem("const_01", "Перфоратор Makita SDS-Plus", "Электроинструмент", "Бурение", "шт.", "Исправно"),
            InventoryItem("const_02", "УШМ Bosch 125мм", "Электроинструмент", "Резка", "шт.", "Исправно"),
            InventoryItem("const_03", "Диск отрезной 125x1.0", "Расходники", "Резка", "шт.", "Новое"),
            InventoryItem("const_04", "Цемент М500", "Стройматериалы", "Смеси", "меш.", "Новое"),
            InventoryItem("const_05", "Каска строительная", "СИЗ и Спецодежда", "Защита", "шт.", "Исправно")
        )
        AppMode.WAREHOUSE -> listOf(
            InventoryItem("wh_01", "Коробка гофрокартон 60x40x40", "Упаковка", "Тара", "шт.", "Новое"),
            InventoryItem("wh_02", "Скотч упаковочный прозрачный", "Упаковка", "Расходники", "рул.", "Новое"),
            InventoryItem("wh_03", "Стрейч-пленка", "Упаковка", "Расходники", "рул.", "Новое"),
            InventoryItem("wh_04", "Рохля гидравлическая 2т", "Техника", "Складское", "шт.", "Исправно"),
            InventoryItem("wh_05", "Сканер штрихкодов Zebra", "Техника", "Учет", "шт.", "Исправно")
        )
        AppMode.UNIVERSAL -> listOf(
            InventoryItem("univ_01", "Бумага А4 СветоКопи", "Канцелярия", "Офис", "пач.", "Новое"),
            InventoryItem("univ_02", "Ноутбук Lenovo ThinkPad", "Оргтехника", "ИТ", "шт.", "Исправно"),
            InventoryItem("univ_03", "Кресло офисное", "Мебель", "Интерьер", "шт.", "Исправно")
        )
        else -> listOf(
            // Military items
            InventoryItem("rav_01", "Автомат Калашникова АК-12 (5.45)", "Служба РАВ", "Стрелковое оружие", "шт.", "Кат. 2"),
            InventoryItem("rav_03", "Магазин 5.45х39 (АК-12/АК-74)", "Служба РАВ", "ЗИП", "шт.", "Кат. 2"),
            InventoryItem("rav_05", "Прицел коллиматорный 1П87", "Служба РАВ", "Оптика", "шт.", "Кат. 2"),
            InventoryItem("rav_09", "Гранатомет РПГ-7В", "Служба РАВ", "Стрелковое оружие", "шт.", "Кат. 2"),
            InventoryItem("bpla_01", "Квадрокоптер DJI Mavic 3 Classic", "Служба БПЛА и робототехники", "Разведка", "шт.", "Кат. 1"),
            InventoryItem("bpla_03", "Аккумулятор DJI Mavic 3 Intelligent Flight Battery", "Служба БПЛА и робототехники", "АКБ", "шт.", "Кат. 1"),
            InventoryItem("reb_01", "Радиостанция «Азарт» Р-187П1", "Служба связи и РЭБ", "Портативные радиостанции", "компл.", "Кат. 1"),
            InventoryItem("med_01", "Жгут-турникет кровоостанавливающий CAT Gen7", "Медицинская служба", "Первая помощь", "шт.", "Кат. 1")
        )
    }
}
"""

with open('app/src/main/java/com/example/data/local/InitialData.kt', 'w', encoding='utf-8') as f:
    f.write(new_content)

print("Updated InitialData.kt")
