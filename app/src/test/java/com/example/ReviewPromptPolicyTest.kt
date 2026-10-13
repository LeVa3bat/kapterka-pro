package com.example

import com.example.util.ReviewPromptPolicy
import com.example.util.ReviewPromptState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewPromptPolicyTest {
    private val now = 1_800_000_000_000L

    @Test
    fun `failed or cancelled export does not trigger a request`() {
        assertFalse(ReviewPromptPolicy.canShow(ReviewPromptState(), exportSaved = false, now = now))
        assertTrue(ReviewPromptPolicy.canShow(ReviewPromptState(), exportSaved = true, now = now))
    }

    @Test
    fun `closing a request does not ask on the next export`() {
        val shown = ReviewPromptPolicy.afterShown(ReviewPromptState(), now)
        assertFalse(ReviewPromptPolicy.canShow(shown, exportSaved = true, now = now + 1))
        assertFalse(ReviewPromptPolicy.canShow(shown, exportSaved = true, now = shown.nextPromptAt - 1))
        assertTrue(ReviewPromptPolicy.canShow(shown, exportSaved = true, now = shown.nextPromptAt))
    }

    @Test
    fun `later waits thirty days from the choice`() {
        val chosenAt = now + 60_000L
        val deferred = ReviewPromptPolicy.later(ReviewPromptPolicy.afterShown(ReviewPromptState(), now), chosenAt)
        assertFalse(ReviewPromptPolicy.canShow(deferred, exportSaved = true, now = chosenAt + ReviewPromptPolicy.LATER_INTERVAL_MS - 1))
        assertTrue(ReviewPromptPolicy.canShow(deferred, exportSaved = true, now = chosenAt + ReviewPromptPolicy.LATER_INTERVAL_MS))
    }

    @Test
    fun `request is shown at most twice even after the cooldown`() {
        val first = ReviewPromptPolicy.afterShown(ReviewPromptState(), now)
        val second = ReviewPromptPolicy.afterShown(first, first.nextPromptAt)
        assertFalse(ReviewPromptPolicy.canShow(second, exportSaved = true, now = second.nextPromptAt))
        assertFalse(ReviewPromptPolicy.canShow(second, exportSaved = true, now = second.nextPromptAt + ReviewPromptPolicy.LATER_INTERVAL_MS))
    }

    @Test
    fun `do not ask again or opening the store prevents any later requests`() {
        val stopped = ReviewPromptPolicy.stop(ReviewPromptPolicy.afterShown(ReviewPromptState(), now))
        assertFalse(ReviewPromptPolicy.canShow(stopped, exportSaved = true, now = now + 365L * 24 * 60 * 60 * 1000))
    }
}
