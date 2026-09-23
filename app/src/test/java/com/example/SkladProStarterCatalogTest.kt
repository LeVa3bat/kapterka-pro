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
}
