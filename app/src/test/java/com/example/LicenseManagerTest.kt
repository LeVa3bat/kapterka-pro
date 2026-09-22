package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.license.LicenseManager
import com.example.data.local.KapterkaDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
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
        context.getSharedPreferences("kapterka_fighter_license_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
        context.getSharedPreferences("kapterka_license_permanent_vault", Context.MODE_PRIVATE)
            .edit().clear().commit()
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
    fun testServerVerifiedLicenseUsesServerExpiry() = runBlocking {
        val key = licenseManager.generateLicenseKey()
        val expiresAt = System.currentTimeMillis() + 2L * 24L * 60L * 60L * 1000L

        val activated = licenseManager.activateServerVerifiedLicense(
            licenseKey = key,
            expiresAt = expiresAt,
            paymentId = "payment_test_123"
        )

        assertTrue(activated)
        val status = licenseManager.licenseStatus.value
        assertTrue(status.isProActive)
        assertEquals(key, status.licenseKey)
        assertTrue(status.daysRemaining in 1..2)
    }

    @Test
    fun testServerVerifiedLicenseRejectsExpiredOrMalformedData() = runBlocking {
        val validKey = licenseManager.generateLicenseKey()

        assertFalse(
            licenseManager.activateServerVerifiedLicense(
                licenseKey = validKey,
                expiresAt = System.currentTimeMillis() - 1000L,
                paymentId = "payment_expired"
            )
        )
        assertFalse(
            licenseManager.activateServerVerifiedLicense(
                licenseKey = "KAPT-FAKE-FAKE-FAKE",
                expiresAt = System.currentTimeMillis() + 86400000L,
                paymentId = "payment_bad_key"
            )
        )
        assertFalse(
            licenseManager.activateServerVerifiedLicense(
                licenseKey = validKey,
                expiresAt = System.currentTimeMillis() + 86400000L,
                paymentId = ""
            )
        )
    }

    @Test
    fun testResetLicensePreservesIdentityAndDemoStart() {
        val fighterIdBefore = licenseManager.getFighterPersonalId()
        licenseManager.refreshLicenseStatus()

        val prefs = context.getSharedPreferences(
            "kapterka_fighter_license_prefs",
            Context.MODE_PRIVATE
        )
        val demoStartBefore = prefs.getLong("demo_first_launch_time", 0L)
        assertTrue(demoStartBefore > 0L)

        licenseManager.resetLicense()

        val fighterIdAfter = licenseManager.getFighterPersonalId()
        val demoStartAfter = prefs.getLong("demo_first_launch_time", 0L)

        assertEquals(fighterIdBefore, fighterIdAfter)
        assertEquals(demoStartBefore, demoStartAfter)
    }

    @Test
    fun testInitialLicenseState() {
        licenseManager.refreshLicenseStatus()
        val status = licenseManager.licenseStatus.value
        assertNotNull(status)
        assertNotNull(status.fighterId)
    }
}
