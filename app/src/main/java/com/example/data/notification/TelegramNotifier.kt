package com.example.data.notification

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TelegramNotifier {
    private const val TAG = "TelegramNotifier"

    // Public endpoint only. Telegram bot credentials stay on the server.
    private const val NOTIFICATION_API_URL =
        "https://script.google.com/macros/s/AKfycbwuwY74vD9El1R6ZVvO3DDpJ7BkY-wX0ljRphWRSA-jgB33-duUAqEp0g03D_7oFzjqmA/exec"
    private const val ADMIN_CHAT_ID = "7426550032"

    private fun html(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    private fun maskUnitKey(value: String): String {
        val clean = value.trim()
        if (clean.length <= 5) return if (clean.isBlank()) "Не указан" else "***"
        return clean.take(5) + "***" + clean.takeLast(2)
    }

    private fun maskLicenseKey(value: String): String {
        val clean = value.trim()
        if (clean.isBlank()) return "Не указан"
        val parts = clean.split("-")
        return if (parts.size == 4) {
            "${parts[0]}-****-****-${parts[3]}"
        } else {
            clean.take(4) + "…"
        }
    }

    suspend fun sendMessage(textHtml: String) = withContext(Dispatchers.IO) {
        try {
            val url = URL(NOTIFICATION_API_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Content-Type", "text/plain;charset=utf-8")
            }

            val payload = JSONObject().apply {
                put("action", "send_telegram")
                put("text", textHtml)
                put("chat_id", ADMIN_CHAT_ID)
            }

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use {
                it.write(payload.toString())
            }

            val code = connection.responseCode
            if (code in 200..299) {
                Log.d(TAG, "Telegram notification delivered through server (HTTP $code)")
            } else {
                Log.w(TAG, "Notification server returned HTTP $code")
            }
            connection.disconnect()
        } catch (e: Exception) {
            Log.w(TAG, "Notification server unavailable: ${e.message}")
        }
    }

    suspend fun notifyRegistration(callsign: String, unitName: String, unitKey: String, email: String) {
        val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
        val msg = """
            🎖 <b>Новая регистрация в приложении «Каптёрка»!</b>
            
            👤 <b>Позывной:</b> ${html(callsign)}
            🏢 <b>Подразделение:</b> ${html(unitName)}
            🔑 <b>Ключ канала:</b> <code>${html(maskUnitKey(unitKey))}</code>
            📧 <b>Email:</b> ${html(email.ifEmpty { "Не указан" })}
            📅 <b>Время:</b> ${html(dateStr)}
            📱 <b>Платформа:</b> Android App
        """.trimIndent()
        sendMessage(msg)
    }

    suspend fun notifyPaymentStarted(callsign: String, email: String, amountRub: Int) {
        val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
        val msg = """
            💳 <b>Попытка оплаты лицензии в Android</b>
            
            👤 <b>Позывной:</b> ${html(callsign)}
            📧 <b>Email:</b> ${html(email.ifEmpty { "Не указан" })}
            💵 <b>Сумма:</b> $amountRub ₽
            🏦 <b>Шлюз:</b> ЮKassa
            📅 <b>Время:</b> ${html(dateStr)}
        """.trimIndent()
        sendMessage(msg)
    }

    suspend fun notifyPaymentConfirmed(callsign: String, email: String, licenseKey: String, days: Int) {
        val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
        val msg = """
            💰 <b>Оплата и активация PRO в Android</b>
            
            👤 <b>Пользователь:</b> ${html(callsign)}
            📧 <b>Email:</b> ${html(email.ifEmpty { "Не указан" })}
            🔑 <b>Ключ:</b> <code>${html(maskLicenseKey(licenseKey))}</code>
            ⏱ <b>Срок действия:</b> $days суток
            📅 <b>Дата:</b> ${html(dateStr)}
        """.trimIndent()
        sendMessage(msg)
    }

    suspend fun notifyKeyActivated(callsign: String, licenseKey: String, days: Int) {
        val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
        val msg = """
            🔑 <b>Активация лицензионного ключа в Android</b>
            
            👤 <b>Пользователь:</b> ${html(callsign)}
            🔑 <b>Ключ:</b> <code>${html(maskLicenseKey(licenseKey))}</code>
            ⏱ <b>Доступ:</b> $days суток
            📅 <b>Время:</b> ${html(dateStr)}
        """.trimIndent()
        sendMessage(msg)
    }

    suspend fun notifyLicenseEmailDispatched(callsign: String, email: String, licenseKey: String, subject: String) {
        val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
        val msg = """
            ✉️ <b>Письмо с лицензией отправлено пользователю</b>
            
            👤 <b>Пользователь:</b> ${html(callsign)}
            📧 <b>Email:</b> <code>${html(email)}</code>
            🔑 <b>Ключ:</b> <code>${html(maskLicenseKey(licenseKey))}</code>
            📋 <b>Тема:</b> ${html(subject)}
            📅 <b>Время:</b> ${html(dateStr)}
        """.trimIndent()
        sendMessage(msg)
    }
}
