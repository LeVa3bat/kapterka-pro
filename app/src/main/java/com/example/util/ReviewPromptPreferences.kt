package com.example.util

import android.content.SharedPreferences
import org.json.JSONObject

/** One separate entry in the app's existing local preferences; no account or network data. */
class ReviewPromptPreferences(private val preferences: SharedPreferences) {
    private fun read(): ReviewPromptState {
        val raw = preferences.getString(KEY, null) ?: return ReviewPromptState()
        return runCatching {
            val value = JSONObject(raw)
            ReviewPromptState(
                shownCount = value.optInt("shownCount", 0).coerceAtLeast(0),
                nextPromptAt = value.optLong("nextPromptAt", 0L).coerceAtLeast(0L),
                disabled = value.optBoolean("disabled", false)
            )
        }.getOrElse { ReviewPromptState(disabled = true) }
    }

    private fun write(state: ReviewPromptState) {
        val value = JSONObject()
            .put("shownCount", state.shownCount)
            .put("nextPromptAt", state.nextPromptAt)
            .put("disabled", state.disabled)
        preferences.edit().putString(KEY, value.toString()).apply()
    }

    fun recordPromptAfterSavedExport(now: Long = System.currentTimeMillis()): Boolean {
        val state = read()
        if (!ReviewPromptPolicy.canShow(state, exportSaved = true, now = now)) return false
        write(ReviewPromptPolicy.afterShown(state, now))
        return true
    }

    fun later(now: Long = System.currentTimeMillis()) = write(ReviewPromptPolicy.later(read(), now))

    fun stop() = write(ReviewPromptPolicy.stop(read()))

    private companion object {
        const val KEY = "review_request_state"
    }
}
