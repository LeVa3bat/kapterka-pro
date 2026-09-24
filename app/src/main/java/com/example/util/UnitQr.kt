package com.example.util

import android.graphics.Bitmap
import android.net.Uri
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * QR invitation to a unit. The QR carries exactly what the text key carries
 * (the key itself), so scanning grants nothing beyond typing the key; the
 * server still decides membership through unit_join.
 *
 * Format: kapterka://join?k=<unitKey>&u=<unitName>
 */
object UnitQr {
    private val KEY_RE = Regex("^[A-Za-z0-9_\\-]{4,64}$")

    fun payload(unitKey: String, unitName: String): String =
        "kapterka://join?k=${Uri.encode(unitKey.trim())}&u=${Uri.encode(unitName.trim().take(60))}"

    data class Invite(val unitKey: String, val unitName: String)

    /** Accepts our QR payload or a bare key; anything else is rejected. */
    fun parse(raw: String?): Invite? {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty() || text.length > 300) return null
        if (text.startsWith("kapterka://join")) {
            val uri = runCatching { Uri.parse(text) }.getOrNull() ?: return null
            val key = uri.getQueryParameter("k")?.trim().orEmpty()
            if (!KEY_RE.matches(key)) return null
            return Invite(key, uri.getQueryParameter("u")?.trim()?.take(60).orEmpty())
        }
        return if (KEY_RE.matches(text)) Invite(text, "") else null
    }

    fun bitmap(content: String, sizePx: Int = 720, dark: Int = 0xFF0B1210.toInt(), light: Int = 0xFFFFFFFF.toInt()): Bitmap {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val pixels = IntArray(sizePx * sizePx)
        for (y in 0 until sizePx) {
            val row = y * sizePx
            for (x in 0 until sizePx) pixels[row + x] = if (matrix[x, y]) dark else light
        }
        return Bitmap.createBitmap(pixels, sizePx, sizePx, Bitmap.Config.ARGB_8888)
    }
}
