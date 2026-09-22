package com.example.data.notification

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class EmailConfig(
    val brevoApiKey: String = "",
    val resendApiKey: String = "",
    val smtpHost: String = "",
    val smtpPort: Int = 0,
    val smtpUser: String = "",
    val smtpPass: String = "",
    val senderName: String = "Каптёрка ПРО",
    val senderEmail: String = ""
)

object EmailDeliveryService {
    private const val TAG = "EmailDeliveryService"
    private const val PREFS_NAME = "email_delivery_prefs"

    fun getConfig(context: Context): EmailConfig {
        clearLegacySecrets(context)
        return EmailConfig()
    }

    fun saveConfig(
        context: Context,
        brevoApiKey: String,
        resendApiKey: String,
        smtpHost: String,
        smtpPort: Int,
        smtpUser: String,
        smtpPass: String,
        senderName: String,
        senderEmail: String
    ) {
        @Suppress("UNUSED_VARIABLE")
        val ignored = listOf(
            brevoApiKey, resendApiKey, smtpHost, smtpPort.toString(),
            smtpUser, smtpPass, senderName, senderEmail
        )
        clearLegacySecrets(context)
    }

    private fun clearLegacySecrets(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove("brevo_api_key")
            .remove("resend_api_key")
            .remove("smtp_host")
            .remove("smtp_port")
            .remove("smtp_user")
            .remove("smtp_pass")
            .remove("sender_name")
            .remove("sender_email")
            .apply()
    }

    fun formatLicenseEmail(
        callsign: String,
        licenseKey: String,
        days: Int = 30
    ): Pair<String, String> {
        val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
        val subject = "Ваш персональный лицензионный ключ «Каптёрка ПРО» ($days дней)"
        val bodyText = """
            Здравствуйте, $callsign!

            Ваш лицензионный ключ «Каптёрка ПРО»:
            $licenseKey

            Срок действия: $days суток.
            Дата: $dateStr

            Официальный сайт: https://kapterka-pro.ru/
            Поддержка: alex.666.881@gmail.com
        """.trimIndent()
        return Pair(subject, bodyText)
    }

    suspend fun sendLicenseKeyEmail(
        context: Context,
        recipientEmail: String,
        callsign: String,
        licenseKey: String,
        days: Int = 30
    ): Boolean = withContext(Dispatchers.IO) {
        clearLegacySecrets(context)

        val email = recipientEmail.trim().lowercase(Locale.ROOT)
        val key = licenseKey.trim().uppercase(Locale.ROOT)
        if (!email.matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))) {
            Log.w(TAG, "Invalid license email recipient")
            return@withContext false
        }
        if (!key.matches(Regex("^KAPT-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}$"))) {
            Log.w(TAG, "Invalid license key format for email")
            return@withContext false
        }

        val backend = BuildConfig.PAYMENT_API_URL.trim().trimEnd('/')
        if (backend.isBlank()) {
            Log.w(TAG, "Email backend is not configured")
            return@withContext false
        }

        try {
            val endpoint = URL(backend + "?action=send_license_email")
            val connection = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                doInput = true
                connectTimeout = 12000
                readTimeout = 12000
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
            }

            try {
                val payload = JSONObject().apply {
                    put("license_key", key)
                    put("email", email)
                }
                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use {
                    it.write(payload.toString())
                }

                val stream = if (connection.responseCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }
                val body = stream?.let {
                    BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { reader ->
                        reader.readText()
                    }
                }.orEmpty()
                val response = if (body.isBlank()) JSONObject() else JSONObject(body)
                val sent = connection.responseCode in 200..299 && response.optBoolean("ok", false)
                if (sent) {
                    TelegramNotifier.notifyLicenseEmailDispatched(
                        callsign = callsign,
                        email = email,
                        licenseKey = key,
                        subject = formatLicenseEmail(callsign, key, days).first
                    )
                }
                sent
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Server license email error", e)
            false
        }
    }

    fun openEmailClientWithKey(
        context: Context,
        toEmail: String,
        callsign: String,
        licenseKey: String
    ) {
        try {
            val (subject, bodyText) = formatLicenseEmail(callsign, licenseKey, 30)
            val uri = Uri.parse(
                "mailto:$toEmail?subject=${Uri.encode(subject)}&body=${Uri.encode(bodyText)}"
            )
            context.startActivity(
                Intent(Intent.ACTION_SENDTO, uri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open email client", e)
        }
    }
}
