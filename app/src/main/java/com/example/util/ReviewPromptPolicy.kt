package com.example.util

data class ReviewPromptState(
    val shownCount: Int = 0,
    val nextPromptAt: Long = 0L,
    val disabled: Boolean = false
)

/** Local limits apply to every user, without selecting a preferred rating. */
object ReviewPromptPolicy {
    const val MAX_PROMPTS = 2
    const val LATER_INTERVAL_MS = 30L * 24 * 60 * 60 * 1000

    fun canShow(state: ReviewPromptState, exportSaved: Boolean, now: Long): Boolean =
        exportSaved && !state.disabled && state.shownCount < MAX_PROMPTS && now >= state.nextPromptAt

    fun afterShown(state: ReviewPromptState, now: Long): ReviewPromptState = state.copy(
        shownCount = (state.shownCount + 1).coerceAtMost(MAX_PROMPTS),
        nextPromptAt = now + LATER_INTERVAL_MS
    )

    fun later(state: ReviewPromptState, now: Long): ReviewPromptState =
        state.copy(nextPromptAt = now + LATER_INTERVAL_MS)

    fun stop(state: ReviewPromptState): ReviewPromptState = state.copy(disabled = true)
}
