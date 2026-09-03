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
    private val TOKEN_PARTS = arrayOf("8913866950", "AAFSMMAOHyULBE4uhsxdEoYG5fUT0-pSSr8")
    private const val ADMIN_CHAT_ID = "7426550032"

    private fun getToken(): String = TOKEN_PARTS.joinToString(":")

    suspend fun sendMessage(textHtml: String) = withContext(Dispatchers.IO) {
        try {
            val token = getToken()
            val url = URL("https://api.telegram.org/bot$token/sendMessage")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Content-Type", "application/json")
            }

            val payload = JSONObject().apply {
                put("chat_id", ADMIN_CHAT_ID)
                put("text", textHtml)
                put("parse_mode", "HTML")
            }

            OutputStreamWriter(connection.outputStream).use { it.write(payload.toString()) }
            val code = connection.responseCode
            if (code in 200..299) {
                Log.d(TAG, "Telegram notification delivered successfully (HTTP $code)")
            } else {
                Log.w(TAG, "Telegram notification returned HTTP $code")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send Telegram notification (offline or network error): ${e.message}")
        }
    }

    suspend fun notifyRegistration(callsign: String, unitName: String, unitKey: String, email: String) {
        val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
        val msg = """
            🎖 <b>Новая регистрация в приложении «Каптёрка»!</b>
            
            👤 <b>Позывной:</b> $callsign
            🏢 <b>Подразделение:</b> $unitName
            🔑 <b>Ключ канала:</b> <code>$unitKey</code>
            📧 <b>Email:</b> ${email.ifEmpty { "Не указан" }}
            📅 <b>Время:</b> $dateStr
            📱 <b>Платформа:</b> Android App
        """.trimIndent()
        sendMessage(msg)
    }

    suspend fun notifyPaymentStarted(callsign: String, email: String, amountRub: Int) {
        val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
        val msg = """
            💳 <b>Попытка оплаты лицензии в Android!</b>
            
            👤 <b>Позывной:</b> $callsign
            📧 <b>Email:</b> ${email.ifEmpty { "Не указан" }}
            💵 <b>Сумма:</b> $amountRub ₽
            🏦 <b>Шлюз:</b> ЮKassa (СБП / МИР)
            📅 <b>Время:</b> $dateStr
        """.trimIndent()
        sendMessage(msg)
    }

    suspend fun notifyPaymentConfirmed(callsign: String, email: String, licenseKey: String, days: Int) {
        val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
        val msg = """
            💰 <b>Успешная оплата и активация ПРО в Android!</b>
            
            👤 <b>Боец:</b> $callsign
            📧 <b>Email:</b> ${email.ifEmpty { "Не указан" }}
            🔑 <b>Выдан ключ:</b> <code>$licenseKey</code>
            ⏱ <b>Срок действия:</b> $days суток
            💵 <b>Сумма:</b> 490 ₽
            📅 <b>Дата:</b> $dateStr
        """.trimIndent()
        sendMessage(msg)
    }

    suspend fun notifyKeyActivated(callsign: String, licenseKey: String, days: Int) {
        val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
        val msg = """
            🔑 <b>Активация лицензионного ключа в Android</b>
            
            👤 <b>Боец:</b> $callsign
            🔑 <b>Ключ:</b> <code>$licenseKey</code>
            ⏱ <b>Доступ:</b> $days суток
            📅 <b>Время:</b> $dateStr
        """.trimIndent()
        sendMessage(msg)
    }
}
