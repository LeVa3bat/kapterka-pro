package com.example

import android.app.Application
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.ui.components.AppLinks
import com.example.ui.components.CommunityLinksCard
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "w360dp-h780dp-xhdpi")
class PublicLinksShareTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun showCard() {
        compose.setContent {
            MyApplicationTheme { CommunityLinksCard() }
        }
        compose.waitForIdle()
    }

    @Test
    fun `opening card does not launch sharing`() {
        assertNull(shadowOf(compose.activity).nextStartedActivity)
    }

    @Test
    fun `template button shares only a public link through system chooser`() {
        assertPublicShare("Поделиться пустым шаблоном", AppLinks.EMPTY_TEMPLATE)
    }

    @Test
    fun `recommendation button shares only a public link through system chooser`() {
        assertPublicShare("Посоветовать приложение", AppLinks.RECOMMEND_APP)
    }

    private fun assertPublicShare(button: String, url: String) {
        compose.onNodeWithText(button).performClick()
        val chooser = requireNotNull(shadowOf(compose.activity).nextStartedActivity)
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        val send = requireNotNull(chooser.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java))
        assertEquals(Intent.ACTION_SEND, send.action)
        assertEquals("text/plain", send.type)
        assertTrue(requireNotNull(send.getStringExtra(Intent.EXTRA_TEXT)).endsWith("\n$url"))
        assertEquals(setOf(Intent.EXTRA_TITLE, Intent.EXTRA_TEXT), send.extras?.keySet())
        assertNull(send.data)
        assertNull(send.clipData)
        assertEquals(0, send.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION)
        assertNull(shadowOf(compose.activity).nextStartedActivity)
    }
}
