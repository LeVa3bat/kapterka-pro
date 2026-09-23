package com.example.universal

import com.example.data.model.InventoryItem
import java.util.UUID

/**
 * Safe zero-stock starter nomenclature for a Sklad PRO profile.
 *
 * Existing starter IDs are deliberately preserved so previously recorded stock
 * stays attached after an APK update. Additional prepared positions only add
 * new zero-stock catalog rows; they never invent balances or operations.
 */
object WarehouseStarterCatalog {

    fun itemsFor(profileId: String?): List<InventoryItem> {
        val profile = WarehouseProfileCatalog.find(profileId)

        // Keep the original first six starter IDs stable for update compatibility.
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

        if (profile.id == "military") {
            // Military IDs already exist in Alpha installs. Never renumber them.
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

        // Non-military profiles previously received only six starter rows even
        // though their prepared category/group dictionaries were much richer.
        // Add every real preset name with new stable IDs while keeping the old six intact.
        val baseSemanticKeys = base
            .map { Triple(it.serviceCategory, it.subType, it.name) }
            .toSet()
        val detailed = mutableListOf<InventoryItem>()

        profile.categories.forEach { category ->
            WarehouseGroupCatalog.groupsFor(profile.id, category).forEach { group ->
                WarehouseItemPresetCatalog.namesFor(profile.id, category, group).forEach { name ->
                    val key = Triple(category, group, name)
                    if (key !in baseSemanticKeys) {
                        detailed += InventoryItem(
                            id = stablePresetId(profile.id, category, group, name),
                            name = name,
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
        }

        return (base + detailed).distinctBy { it.id }
    }

    private fun stablePresetId(
        profileId: String,
        category: String,
        group: String,
        name: String
    ): String {
        val identity = "$profileId|$category|$group|$name"
        val uuid = UUID.nameUUIDFromBytes(identity.toByteArray(Charsets.UTF_8))
        return "starter_" + profileId + "_preset_" + uuid
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
