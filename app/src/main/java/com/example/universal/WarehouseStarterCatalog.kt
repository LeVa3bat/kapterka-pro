package com.example.universal

import com.example.data.model.InventoryItem

/**
 * Minimal starter nomenclature for a brand-new Sklad PRO workspace.
 *
 * It creates only zero-stock catalog rows from the selected profile template.
 * No quantities or operations are invented. Existing user catalog data is never
 * overwritten or removed.
 */
object WarehouseStarterCatalog {

    fun itemsFor(profileId: String?): List<InventoryItem> {
        val profile = WarehouseProfileCatalog.find(profileId)

        return profile.categories
            .take(6)
            .mapIndexed { index, category ->
                val group = WarehouseGroupCatalog
                    .groupsFor(profile.id, category)
                    .firstOrNull()
                    .orEmpty()
                    .ifBlank { "Основное" }

                InventoryItem(
                    id = "starter_" + profile.id + "_" + (index + 1),
                    name = group,
                    serviceCategory = category,
                    subType = group,
                    unit = "шт.",
                    categoryClass = "Кат. 1",
                    standardCode = "",
                    isCustom = false,
                    profileId = profile.id
                )
            }
    }
}
