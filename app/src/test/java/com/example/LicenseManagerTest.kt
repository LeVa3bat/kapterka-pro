package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.license.LicenseManager
import com.example.data.local.KapterkaDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LicenseManagerTest {

    private lateinit var licenseManager: LicenseManager
    private lateinit var db: KapterkaDatabase
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, KapterkaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val scope = CoroutineScope(Dispatchers.Unconfined)
        licenseManager = LicenseManager(context, db.kapterkaDao(), scope)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testLicenseKeyFormat() {
        val key = licenseManager.generateLicenseKey()
        assertNotNull(key)
        // Format: KAPT-XXXX-XXXX-XXXX (19 characters)
        assertEquals(19, key.length)
        assertTrue("Key should start with KAPT-", key.startsWith("KAPT-"))
        val parts = key.split("-")
        assertEquals(4, parts.size)
        assertEquals("KAPT", parts[0])
        assertEquals(4, parts[1].length)
        assertEquals(4, parts[2].length)
        assertEquals(4, parts[3].length)
    }

    @Test
    fun testFighterPersonalIdGenerationAndPersistence() {
        val id1 = licenseManager.getFighterPersonalId()
        assertNotNull(id1)
        assertTrue("Personal ID must start with БОЕЦ-", id1.startsWith("БОЕЦ-"))

        // Second call must return the exact same persisted ID
        val id2 = licenseManager.getFighterPersonalId()
        assertEquals("Fighter personal ID must be idempotent", id1, id2)
    }

    @Test
    fun testInitialLicenseState() {
        licenseManager.refreshLicenseStatus()
        val status = licenseManager.licenseStatus.value
        assertNotNull(status)
        assertNotNull(status.fighterId)
    }
}
