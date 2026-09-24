package com.example

import com.example.data.model.RequestStatus
import com.example.util.UnitQr
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UnitQrTest {

    @Test
    fun payloadRoundTrip() {
        val invite = UnitQr.parse(UnitQr.payload("kapt_a1B2", "Минбат №3"))
        assertEquals("kapt_a1B2", invite?.unitKey)
        assertEquals("Минбат №3", invite?.unitName)
    }

    @Test
    fun bareKeyAccepted() {
        assertEquals("kapt_1111", UnitQr.parse("  kapt_1111 ")?.unitKey)
    }

    @Test
    fun foreignOrMalformedCodesRejected() {
        assertNull(UnitQr.parse(null))
        assertNull(UnitQr.parse(""))
        assertNull(UnitQr.parse("https://evil.example/join?k=kapt_1111"))
        assertNull(UnitQr.parse("kapterka://join?k=../../etc"))
        assertNull(UnitQr.parse("kapterka://join?k=kapt 1111"))
        assertNull(UnitQr.parse("kapterka://join"))
        assertNull(UnitQr.parse("x".repeat(400)))
    }

    @Test
    fun qrBitmapIsSquare() {
        val bmp = UnitQr.bitmap(UnitQr.payload("kapt_1111", "Тест"), 256)
        assertEquals(256, bmp.width)
        assertEquals(256, bmp.height)
    }

    @Test
    fun requestStatusPipelineOrder() {
        assertEquals(
            listOf(RequestStatus.PENDING, RequestStatus.ASSEMBLING, RequestStatus.COLLECTED, RequestStatus.ISSUED),
            RequestStatus.pipeline
        )
        // Stored names of the original statuses must stay unchanged for old data and old app versions.
        assertEquals(listOf("PENDING", "COLLECTED", "ISSUED"), RequestStatus.values().take(3).map { it.name })
    }
}
