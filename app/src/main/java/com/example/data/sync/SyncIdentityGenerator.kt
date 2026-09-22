package com.example.data.sync

import java.security.SecureRandom

/**
 * Generates identities for new sync installations/units.
 *
 * Existing keys are never rewritten. Longer random identifiers reduce collision
 * and guessing risk while remaining compatible with Firestore document IDs and
 * the existing manual connection UI.
 */
object SyncIdentityGenerator {
    private const val HEX = "0123456789abcdef"
    private val random = SecureRandom()

    private fun randomHex(length: Int): String = buildString(length) {
        repeat(length) {
            append(HEX[random.nextInt(HEX.length)])
        }
    }

    fun newUnitKey(): String = "kapt_" + randomHex(20)

    fun newDeviceId(): String = "dev_" + randomHex(16)
}
