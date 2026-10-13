package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.ReviewPromptPolicy
import com.example.util.ReviewPromptPreferences
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ReviewPromptPreferencesTest {
    private lateinit var context: Context
    private val now = 1_800_000_000_000L

    @Before
    fun resetPreferences() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("kapterka_app_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    private fun store() = ReviewPromptPreferences(
        context.getSharedPreferences("kapterka_app_prefs", Context.MODE_PRIVATE)
    )

    @Test
    fun `cooldown and two request limit survive recreating the store`() {
        assertTrue(store().recordPromptAfterSavedExport(now))
        assertFalse(store().recordPromptAfterSavedExport(now + 1))
        assertTrue(store().recordPromptAfterSavedExport(now + ReviewPromptPolicy.LATER_INTERVAL_MS))
        assertFalse(store().recordPromptAfterSavedExport(now + 2 * ReviewPromptPolicy.LATER_INTERVAL_MS))
    }

    @Test
    fun `later and refusal remain saved across recreation`() {
        assertTrue(store().recordPromptAfterSavedExport(now))
        store().later(now + 60_000L)
        assertFalse(store().recordPromptAfterSavedExport(now + ReviewPromptPolicy.LATER_INTERVAL_MS))
        store().stop()
        assertFalse(store().recordPromptAfterSavedExport(now + 365L * 24 * 60 * 60 * 1000))
    }

    @Test
    fun `invalid saved state does not restart requests`() {
        context.getSharedPreferences("kapterka_app_prefs", Context.MODE_PRIVATE)
            .edit().putString("review_request_state", "invalid").commit()
        assertFalse(store().recordPromptAfterSavedExport(now))
    }
}
