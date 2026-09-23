package com.example

import com.example.universal.WarehouseGroupCatalog
import com.example.universal.WarehouseProfileCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SkladProProductInvariantTest {

    @Test
    fun universalBuild_isStandaloneProduct() {
        assertTrue(BuildConfig.IS_UNIVERSAL_APP)
        assertEquals("com.aistudio.skladpro", BuildConfig.APPLICATION_ID)
        assertTrue(
            BuildConfig.PAYMENT_API_URL.isBlank() ||
                BuildConfig.PAYMENT_API_URL.startsWith("https://")
        )
        assertFalse(BuildConfig.PAYMENT_API_URL.contains("kapterka-pro-default-rtdb"))
        assertFalse(BuildConfig.PAYMENT_API_URL.contains("kapterka-pro.cloudfunctions"))
    }

    @Test
    fun warehouseProfiles_areCompleteAndUnique() {
        val profiles = WarehouseProfileCatalog.profiles

        assertEquals(3, profiles.size)
        assertEquals(
            listOf("universal", "retail", "military"),
            profiles.map { it.id }
        )
        assertEquals(profiles.size, profiles.map { it.id }.distinct().size)

        profiles.forEach { profile ->
            assertTrue("Profile ${profile.id} must have categories", profile.categories.isNotEmpty())
            assertTrue("Profile ${profile.id} must have a title", profile.title.isNotBlank())

            profile.categories.forEach { category ->
                val groups = WarehouseGroupCatalog.groupsFor(profile.id, category)
                assertTrue(
                    "Missing prepared groups for ${profile.id} / $category",
                    groups.isNotEmpty()
                )
            }
        }
    }

    @Test
    fun militaryProfile_disablesCameraAndPhotosOnlyThere() {
        val military = WarehouseProfileCatalog.find("military")
        assertFalse(military.cameraAllowed)
        assertFalse(military.photosAllowed)

        WarehouseProfileCatalog.profiles
            .filterNot { it.id == "military" }
            .forEach { profile ->
                assertTrue("Camera unexpectedly disabled for ${profile.id}", profile.cameraAllowed)
                assertTrue("Photos unexpectedly disabled for ${profile.id}", profile.photosAllowed)
            }
    }

    @Test
    fun operationVocabulary_isAlwaysDefined() {
        WarehouseProfileCatalog.profiles.forEach { profile ->
            val vocabulary = profile.operations
            assertTrue(vocabulary.income.isNotBlank())
            assertTrue(vocabulary.transfer.isNotBlank())
            assertTrue(vocabulary.issue.isNotBlank())
            assertTrue(vocabulary.writeOff.isNotBlank())
        }
    }
}
