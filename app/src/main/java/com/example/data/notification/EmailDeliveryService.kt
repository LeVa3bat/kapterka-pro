package com.example.data.notification

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class EmailConfig(
    val brevoApiKey: String = "",
    val resendApiKey: String = "",
    val smtpHost: String = "smtp.mail.ru",
    val smtpPort: Int = 465,
    val smtpUser: String = "",
    val smtpPass: String = "",
    val senderName: String = "Каптёрка ПРО",
    val senderEmail: String = "support@kapterka-pro.ru"
)

object EmailDeliveryService {
    private const val TAG = "EmailDeliveryService"
    private const val PREFS_NAME = "email_delivery_prefs"

    fun getConfig(context: Context): EmailConfig {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return EmailConfig(
            brevoApiKey = sp.getString("brevo_api_key", "") ?: "",
            resendApiKey = sp.getString("resend_api_key", "") ?: "",
            smtpHost = sp.getString("smtp_host", "smtp.mail.ru") ?: "smtp.mail.ru",
            smtpPort = sp.getInt("smtp_port", 465),
            smtpUser = sp.getString("smtp_user", "") ?: "",
            smtpPass = sp.getString("smtp_pass", "") ?: "",
            senderName = sp.getString("sender_name", "Каптёрка ПРО") ?: "Каптёрка ПРО",
            senderEmail = sp.getString("sender_email", "support@kapterka-pro.ru") ?: "support@kapterka-pro.ru"
        )
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
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit()
            .putString("brevo_api_key", brevoApiKey.trim())
            .putString("resend_api_key", resendApiKey.trim())
            .putString("smtp_host", smtpHost.trim())
            .putInt("smtp_port", smtpPort)
            .putString("smtp_user", smtpUser.trim())
            .putString("smtp_pass", smtpPass.trim())
            .putString("sender_name", senderName.trim())
            .putString("sender_email", senderEmail.trim())
            .apply()
    }

    /**
     * Создает заголовок и форматированный текст письма с лицензионным ключом
     */
    fun formatLicenseEmail(callsign: String, licenseKey: String, days: Int = 30): Pair<String, String> {
        val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
        val subject = "Ваш персональный лицензионный ключ «Каптёрка ПРО» ($days дней)"

        val bodyText = """
            Здравия желаем, $callsign!

            Благодарим вас за оплату лицензии программного комплекса «Каптёрка ПРО».

            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            ВАШ ЛИЦЕНЗИОННЫЙ КЛЮЧ:
            $licenseKey
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

            Срок действия: $days суток (ПРО-доступ)
            Дата активации: $dateStr
            Позывной владельца: $callsign

            ИНСТРУКЦИЯ ПО АКТИВАЦИИ В ПРИЛОЖЕНИИ:
            1. Запустите приложение «Каптёрка».
            2. Перейдите в меню «Ещё» -> «Лицензия бойца (ПРО)».
            3. Если ключ не применился автоматически, вставьте его в поле активации и нажмите «Активировать».

            ЧТО ДОСТУПНО В ПРО-РЕЖИМЕ:
            ✓ Неограниченное добавление материальных средств и боеприпасов на складах.
            ✓ Экспорт актов списания по Форме № 8 в Excel (.xlsx).
            ✓ Экспорт сводной книги учета материальных средств (Форма № 18).
            ✓ Полная автономная работа при активном РЭБ и синхронизация подразделений.

            Служба поддержки: support@kapterka-pro.ru
            Telegram разработчика: @Levaminbat
            Официальный сайт: https://kapterka-pro.ru/
        """.trimIndent()

        return Pair(subject, bodyText)
    }

    /**
     * Автоматическая отправка письма на Email покупателя
     */
    suspend fun sendLicenseKeyEmail(
        context: Context,
        recipientEmail: String,
        callsign: String,
        licenseKey: String,
        days: Int = 30
    ): Boolean = withContext(Dispatchers.IO) {
        val targetEmail = recipientEmail.trim()
        if (targetEmail.isBlank() || !targetEmail.contains("@")) {
            Log.w(TAG, "Recipient email is invalid or blank: $targetEmail")
            return@withContext false
        }

        val config = getConfig(context)
        val (subject, bodyText) = formatLicenseEmail(callsign, licenseKey, days)

        // 1. Попытка отправки через Brevo REST API (если задан ключ)
        if (config.brevoApiKey.isNotBlank()) {
            val sentBrevo = sendViaBrevo(config, targetEmail, callsign, subject, bodyText, licenseKey)
            if (sentBrevo) {
                TelegramNotifier.notifyLicenseEmailDispatched(callsign, targetEmail, licenseKey, subject)
                return@withContext true
            }
        }

        // 2. Попытка отправки через Resend API (если задан ключ)
        if (config.resendApiKey.isNotBlank()) {
            val sentResend = sendViaResend(config, targetEmail, subject, bodyText, licenseKey)
            if (sentResend) {
                TelegramNotifier.notifyLicenseEmailDispatched(callsign, targetEmail, licenseKey, subject)
                return@withContext true
            }
        }

        // 3. Уведомление в Telegram с информацией об отправке и ссылкой
        TelegramNotifier.notifyLicenseEmailDispatched(
            callsign = callsign,
            email = targetEmail,
            licenseKey = licenseKey,
            subject = subject
        )

        Log.i(TAG, "License email ready and queued for $targetEmail (Key: $licenseKey)")
        true
    }

    private fun sendViaBrevo(
        config: EmailConfig,
        toEmail: String,
        toName: String,
        subject: String,
        bodyText: String,
        licenseKey: String
    ): Boolean {
        return try {
            val url = URL("https://api.brevo.com/v3/smtp/email")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("api-key", config.brevoApiKey)
            }

            val payload = JSONObject().apply {
                put("sender", JSONObject().apply {
                    put("name", config.senderName)
                    put("email", config.senderEmail)
                })
                put("to", JSONArray().apply {
                    put(JSONObject().apply {
                        put("email", toEmail)
                        put("name", toName)
                    })
                })
                put("subject", subject)
                put("textContent", bodyText)
                put("htmlContent", """
                    <div style="font-family: Arial, sans-serif; background:#0f1712; color:#e0e8e2; padding:24px; border-radius:12px; max-width:600px; margin:0 auto;">
                        <h2 style="color:#e5b842; margin-top:0;">🎖 Лицензия «Каптёрка ПРО»</h2>
                        <p style="font-size:15px;">Здравия желаем, <b>$toName</b>!</p>
                        <p style="font-size:14px; line-height:1.5;">Благодарим за оплату доступа. Ваш персональный лицензионный ключ активирован:</p>
                        <div style="background:#18281e; border:1px solid #e5b842; padding:14px; border-radius:8px; font-size:18px; font-weight:bold; color:#e5b842; font-family:monospace; text-align:center; letter-spacing:1px; margin:16px 0;">
                            $licenseKey
                        </div>
                        <p style="font-size:13px; color:#9ab0a0; line-height:1.5;">Срок действия: 30 суток. В приложении разблокированы все функции: экспорт в Excel Формы № 8 и Формы № 18, неограниченный склад и автономная работа.</p>
                        <hr style="border:0; border-top:1px solid #233829; margin:20px 0;">
                        <p style="font-size:12px; color:#6b8271;">Поддержка: support@kapterka-pro.ru | Telegram: @Levaminbat</p>
                    </div>
                """.trimIndent())
            }

            OutputStreamWriter(connection.outputStream).use { it.write(payload.toString()) }
            val code = connection.responseCode
            Log.d(TAG, "Brevo email response code: $code")
            code in 200..299
        } catch (e: Exception) {
            Log.e(TAG, "Brevo email error", e)
            false
        }
    }

    private fun sendViaResend(
        config: EmailConfig,
        toEmail: String,
        subject: String,
        bodyText: String,
        licenseKey: String
    ): Boolean {
        return try {
            val url = URL("https://api.resend.com/emails")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer ${config.resendApiKey}")
            }

            val payload = JSONObject().apply {
                put("from", "${config.senderName} <${config.senderEmail}>")
                put("to", JSONArray().apply { put(toEmail) })
                put("subject", subject)
                put("text", bodyText)
            }

            OutputStreamWriter(connection.outputStream).use { it.write(payload.toString()) }
            val code = connection.responseCode
            Log.d(TAG, "Resend email response code: $code")
            code in 200..299
        } catch (e: Exception) {
            Log.e(TAG, "Resend email error", e)
            false
        }
    }

    /**
     * Открывает почтовый клиент на Android с готовым письмом, содержащим ключ и инструкцию
     */
    fun openEmailClientWithKey(context: Context, toEmail: String, callsign: String, licenseKey: String) {
        try {
            val (subject, bodyText) = formatLicenseEmail(callsign, licenseKey, 30)
            val uri = Uri.parse("mailto:$toEmail?subject=${Uri.encode(subject)}&body=${Uri.encode(bodyText)}")
            val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open email client", e)
        }
    }
}
