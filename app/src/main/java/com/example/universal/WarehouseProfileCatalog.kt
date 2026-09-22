package com.example.universal

data class OperationVocabulary(
    val income: String = "Поступление",
    val transfer: String = "Перемещение",
    val issue: String = "Выдача",
    val writeOff: String = "Списание"
)

data class WarehouseProfileTemplate(
    val id: String,
    val title: String,
    val subtitle: String,
    val emoji: String,
    val categories: List<String>,
    val operations: OperationVocabulary = OperationVocabulary(),
    val cameraAllowed: Boolean = true,
    val photosAllowed: Boolean = true
)

object WarehouseProfileCatalog {
    val profiles = listOf(
        WarehouseProfileTemplate(
            "universal", "Универсальный склад", "Товары, материалы, оборудование и расходники", "📦",
            listOf("Товары", "Материалы", "Расходники", "Инструменты", "Оборудование", "Запчасти", "Упаковка", "Прочее")
        ),
        WarehouseProfileTemplate(
            "retail", "Магазин и розница", "Продажи, остатки, возвраты и резерв", "🛒",
            listOf("Товары для продажи", "Расходные материалы", "Упаковка", "Возвраты", "Брак", "Резерв", "Хозтовары", "Прочее"),
            operations = OperationVocabulary("Поступление", "Перемещение", "Продажа / выдача", "Списание")
        ),
        WarehouseProfileTemplate(
            "auto", "Автосервис", "Запчасти, масла, инструмент и оборудование", "🚗",
            listOf("Запчасти", "Фильтры", "Масла и жидкости", "Шины и диски", "Электрика", "Крепёж", "Инструменты", "Оборудование", "Расходники"),
            operations = OperationVocabulary("Поступление", "Перемещение", "В работу", "Списание")
        ),
        WarehouseProfileTemplate(
            "construction", "Стройка", "Материалы, инструмент, СИЗ и расходники", "🏗️",
            listOf("Стройматериалы", "Крепёж", "Электрика", "Сантехника", "Инструменты", "СИЗ", "Оборудование", "Расходники"),
            operations = OperationVocabulary("Поступление", "Перемещение", "Выдать на объект", "Списание")
        ),
        WarehouseProfileTemplate(
            "tools", "Инструменты и имущество", "Выдача сотрудникам, возвраты и ремонт", "🧰",
            listOf("Электроинструмент", "Ручной инструмент", "Оснастка", "Измерительный инструмент", "СИЗ", "Оборудование", "Расходники", "На ремонте"),
            operations = OperationVocabulary("Принять", "Переместить", "Выдать", "Списать")
        ),
        WarehouseProfileTemplate(
            "manufacturing", "Производство", "Сырьё, комплектующие и готовая продукция", "🏭",
            listOf("Сырьё", "Материалы", "Комплектующие", "Полуфабрикаты", "Готовая продукция", "Упаковка", "Инструменты", "Брак и отходы"),
            operations = OperationVocabulary("Поступление", "Перемещение", "В производство", "Списание")
        ),
        WarehouseProfileTemplate(
            "food", "Продукты и общепит", "Продукты, напитки, упаковка и сроки", "🍽️",
            listOf("Продукты", "Напитки", "Бакалея", "Охлаждённое", "Заморозка", "Овощи и фрукты", "Упаковка", "Хозтовары"),
            operations = OperationVocabulary("Поступление", "Перемещение", "Выдать / расход", "Списание")
        ),
        WarehouseProfileTemplate(
            "medical", "Медицина", "Расходники, оборудование и средства защиты", "🩺",
            listOf("Медикаменты", "Перевязочные материалы", "Медицинские расходники", "Инструменты", "Оборудование", "СИЗ", "Дезинфекция", "Прочее"),
            operations = OperationVocabulary("Поступление", "Перемещение", "Выдача", "Списание")
        ),
        WarehouseProfileTemplate(
            "office_it", "Офис и IT", "Техника, мебель, периферия и расходники", "💻",
            listOf("Компьютеры", "Мобильные устройства", "Периферия", "Сетевое оборудование", "Кабели и адаптеры", "Мебель", "Канцтовары", "Расходники"),
            operations = OperationVocabulary("Поступление", "Перемещение", "Выдать сотруднику", "Списание")
        ),
        WarehouseProfileTemplate(
            "education", "Учебное учреждение", "Техника, мебель, лаборатории и хозимущество", "🎓",
            listOf("Учебное оборудование", "Компьютерная техника", "Мебель", "Канцтовары", "Лабораторное оборудование", "Спортинвентарь", "Хозтовары", "Расходники"),
            operations = OperationVocabulary("Поступление", "Перемещение", "Выдача", "Списание")
        ),
        WarehouseProfileTemplate(
            "wholesale", "Оптовый склад", "Приёмка, резерв, возвраты и отгрузка", "📚",
            listOf("Основной товар", "Резерв", "Возвраты", "Брак", "Упаковка", "Паллеты и тара", "Расходники", "Прочее"),
            operations = OperationVocabulary("Приёмка", "Перемещение", "Отгрузка", "Списание")
        ),
        WarehouseProfileTemplate(
            "logistics", "Логистика и фулфилмент", "Приёмка, хранение, комплектация и отгрузка", "🚚",
            listOf("Товар клиентов", "На приёмку", "На хранении", "На комплектацию", "На отгрузку", "Возвраты", "Паллеты и тара", "Упаковка"),
            operations = OperationVocabulary("Приёмка", "Перемещение", "Отгрузка", "Списание")
        ),
        WarehouseProfileTemplate(
            "service", "Сервис и мастерская", "Запчасти, материалы, инструмент и ремонт", "🔧",
            listOf("Запчасти", "Материалы", "Расходники", "Инструменты", "Оборудование", "Выдано мастерам", "На ремонте", "Брак"),
            operations = OperationVocabulary("Поступление", "Перемещение", "В работу", "Списание")
        ),
        WarehouseProfileTemplate(
            "military", "Военный склад", "Имущество, снабжение и специализированный учёт", "🎖️",
            listOf(
                "Служба РАВ",
                "Служба БПЛА и робототехники",
                "Служба связи и РЭБ",
                "Служба ГСМ",
                "Медицинская служба",
                "Вещевая служба и СИБЗ",
                "Инженерная служба",
                "Продовольственная служба",
                "Автомобильная и БТ служба",
                "Служба РХБЗ",
                "Трофеи",
                "Прочее"
            ),
            operations = OperationVocabulary("Приход", "Перемещение", "Выдача", "Списание"),
            cameraAllowed = false,
            photosAllowed = false
        )
    )

    val allPresetCategories: Set<String> = profiles.flatMap { it.categories }.toSet()

    fun find(id: String?): WarehouseProfileTemplate =
        profiles.firstOrNull { it.id == id } ?: profiles.first()
}
