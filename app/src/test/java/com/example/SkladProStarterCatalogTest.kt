package com.example

import com.example.universal.WarehouseProfileCatalog
import com.example.universal.WarehouseStarterCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SkladProStarterCatalogTest {

    @Test
    fun everyWarehouseProfile_hasSafeStarterCatalog() {
        WarehouseProfileCatalog.profiles.forEach { profile ->
            val items = WarehouseStarterCatalog.itemsFor(profile.id)

            assertTrue("Starter catalog must not be empty for " + profile.id, items.isNotEmpty())
            assertEquals(items.size, items.map { it.id }.distinct().size)

            items.forEach { item ->
                assertTrue(item.id.startsWith("starter_" + profile.id + "_"))
                assertTrue(profile.categories.contains(item.serviceCategory))
                assertTrue(item.name.isNotBlank())
                assertTrue(item.unit.isNotBlank())
                assertTrue(!item.isCustom)
            }
        }
    }

    @Test
    fun starterCatalogUsesHumanItemNamesInsteadOfGroupPlaceholders() {
        WarehouseProfileCatalog.profiles.forEach { profile ->
            val items = WarehouseStarterCatalog.itemsFor(profile.id)
            items.take(6).forEach { item ->
                assertTrue(
                    "Starter name should be a real item for " + profile.id + ": " + item.subType,
                    item.name.isNotBlank() && item.name != item.subType
                )
            }
        }
    }
    @Test
    fun nonMilitaryProfilesExposePreparedGroupsInStarterCatalog() {
        WarehouseProfileCatalog.profiles
            .filterNot { it.id == "military" }
            .forEach { profile ->
                val items = WarehouseStarterCatalog.itemsFor(profile.id)
                val groups = items.map { it.subType }.filter { it.isNotBlank() }.distinct()

                assertTrue(
                    "Non-military profile should expose more than one prepared group: " + profile.id,
                    groups.size > 1
                )
                assertTrue(
                    "Non-military starter catalog should no longer be limited to six rows: " + profile.id,
                    items.size > 6
                )
            }
    }

    @Test
    fun nonMilitaryPresetIdsAreStableAcrossCalls() {
        WarehouseProfileCatalog.profiles
            .filterNot { it.id == "military" }
            .forEach { profile ->
                val first = WarehouseStarterCatalog.itemsFor(profile.id).map { it.id }
                val second = WarehouseStarterCatalog.itemsFor(profile.id).map { it.id }
                assertEquals(first, second)
            }
    }
}
