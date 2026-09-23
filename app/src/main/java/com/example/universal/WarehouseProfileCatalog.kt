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

/**
 * Sklad PRO intentionally exposes only three clear accounting modes.
 *
 * Old alpha profile IDs are normalized here instead of being deleted. This
 * keeps existing warehouses/data compatible while removing profile clutter
 * from the user interface.
 */
object WarehouseProfileCatalog {
    private val universal = WarehouseProfileTemplate(
        id = "universal",
        title = "Универсальный склад",
        subtitle = "Материалы, имущество, инструмент, оборудование и запасы",
        emoji = "📦",
        categories = listOf(
            "Товары и имущество",
            "Материалы",
            "Расходники",
            "Инструменты",
            "Оборудование",
            "Запчасти",
            "Упаковка и тара",
            "Прочее"
        )
    )

    private val retail = WarehouseProfileTemplate(
        id = "retail",
        title = "Магазин / торговля",
        subtitle = "Товар, упаковка, резерв, возвраты и брак",
        emoji = "🛒",
        categories = listOf(
            "Товары для продажи",
            "Резерв",
            "Возвраты",
            "Брак",
            "Упаковка",
            "Расходные материалы",
            "Хозтовары",
            "Прочее"
        ),
        operations = OperationVocabulary(
            income = "Поступление",
            transfer = "Перемещение",
            issue = "Продажа / выдача",
            writeOff = "Списание"
        )
    )

    private val military = WarehouseProfileTemplate(
        id = "military",
        title = "Военный склад",
        subtitle = "Имущество, снабжение и специализированный учёт",
        emoji = "🎖️",
        categories = listOf(
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
        operations = OperationVocabulary(
            income = "Приход",
            transfer = "Перемещение",
            issue = "Выдача",
            writeOff = "Списание"
        ),
        cameraAllowed = false,
        photosAllowed = false
    )

    /** Profiles shown to users. Keep this deliberately short. */
    val profiles: List<WarehouseProfileTemplate> = listOf(
        universal,
        retail,
        military
    )

    val allPresetCategories: Set<String> =
        profiles.flatMap { it.categories }.toSet()

    /**
     * Compatibility mapping for alpha builds that offered too many profiles.
     * No rows are deleted; warehouses are migrated to the nearest of the three
     * supported modes.
     */
    fun normalizeId(id: String?): String = when (id?.trim()) {
        "military" -> "military"
        "retail", "wholesale" -> "retail"
        "universal",
        "auto",
        "construction",
        "tools",
        "manufacturing",
        "food",
        "medical",
        "office_it",
        "education",
        "logistics",
        "service",
        null,
        "" -> "universal"
        else -> "universal"
    }

    fun isSupported(id: String?): Boolean =
        profiles.any { it.id == id }

    fun find(id: String?): WarehouseProfileTemplate {
        val normalized = normalizeId(id)
        return profiles.first { it.id == normalized }
    }
}
