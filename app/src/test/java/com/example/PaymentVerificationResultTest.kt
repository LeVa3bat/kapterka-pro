package com.example

import com.example.data.payment.PaymentVerificationResult
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentVerificationResultTest {
    private val now = 1_800_000_000_000L
    private val confirmed = PaymentVerificationResult(
        success = true,
        paid = true,
        status = "succeeded",
        licenseKey = "KAPT-AB12-CD34-EF56",
        expiresAt = now + 86_400_000L
    )

    @Test
    fun serverConfirmationIsRequiredEvenWhenLicenseDataIsPresent() {
        assertFalse(confirmed.copy(success = false).isReadyForActivation(now))
        assertFalse(confirmed.copy(paid = false, status = "pending").isReadyForActivation(now))
    }

    @Test
    fun paidResponseWithoutValidKeyAndFutureExpiryCannotActivate() {
        assertFalse(confirmed.copy(licenseKey = "").isReadyForActivation(now))
        assertFalse(confirmed.copy(licenseKey = "invalid").isReadyForActivation(now))
        assertFalse(confirmed.copy(expiresAt = 0L).isReadyForActivation(now))
        assertFalse(confirmed.copy(expiresAt = now).isReadyForActivation(now))
    }

    @Test
    fun confirmedLicenseRemainsValidOnlyUntilItsServerExpiry() {
        assertTrue(confirmed.isReadyForActivation(now))
        assertFalse(confirmed.isReadyForActivation(confirmed.expiresAt))
    }
}
