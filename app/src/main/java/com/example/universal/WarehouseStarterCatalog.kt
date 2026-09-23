package com.example.universal

import com.example.data.model.InventoryItem

/**
 * Safe zero-stock starter nomenclature for a Sklad PRO profile.
 * Presets never invent quantities or operations.
 */
object WarehouseStarterCatalog {

    fun itemsFor(profileId: String?): List<InventoryItem> {
        val profile = WarehouseProfileCatalog.find(profileId)

        val base = profile.categories
            .take(6)
            .mapIndexed { index, category ->
                val group = WarehouseGroupCatalog
                    .groupsFor(profile.id, category)
                    .firstOrNull()
                    .orEmpty()
                    .ifBlank { "Основное" }

                val presetName = WarehouseItemPresetCatalog
                    .namesFor(profile.id, category, group)
                    .firstOrNull()
                    .orEmpty()
                    .ifBlank { group }

                InventoryItem(
                    id = "starter_" + profile.id + "_" + (index + 1),
                    name = presetName,
                    serviceCategory = category,
                    subType = group,
                    unit = "шт.",
                    categoryClass = "Кат. 1",
                    standardCode = "",
                    isCustom = false,
                    profileId = profile.id
                )
            }

        if (profile.id != "military") return base

        val detailed = mutableListOf<InventoryItem>()
        var counter = 1

        profile.categories.forEach { category ->
            WarehouseGroupCatalog.groupsFor(profile.id, category).forEach { group ->
                WarehouseItemPresetCatalog.namesFor(profile.id, category, group).forEach { name ->
                    detailed += InventoryItem(
                        id = "starter_military_preset_" + counter++,
                        name = name,
                        serviceCategory = category,
                        subType = group,
                        unit = inferMilitaryUnit(category, group),
                        categoryClass = if (
                            group.contains("Вооруж", ignoreCase = true) ||
                            group.contains("Пулем", ignoreCase = true) ||
                            group.contains("Автомат", ignoreCase = true)
                        ) "Кат. 2" else "Кат. 1",
                        standardCode = "",
                        isCustom = false,
                        profileId = profile.id
                    )
                }
            }
        }

        return (base + detailed).distinctBy { it.id }
    }

    private fun inferMilitaryUnit(category: String, group: String): String = when {
        category == "Служба ГСМ" &&
            (group.contains("Топливо") ||
                group.contains("масл", ignoreCase = true) ||
                group.contains("жидк", ignoreCase = true)) -> "л"
        category == "Продовольственная служба" &&
            (group == "Крупы" || group == "Бакалея") -> "кг"
        else -> "шт."
    }
}
