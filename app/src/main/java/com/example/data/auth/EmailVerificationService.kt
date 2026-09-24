package com.example.data.auth

import android.net.Uri
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/** Outcome of an e-mail confirmation step. */
data class EmailCodeResult(
    val success: Boolean,
    val message: String = "",
    /** True when the server cannot be reached or mail is not set up — the user may continue unconfirmed. */
    val canSkip: Boolean = false,
    val retryAfterSeconds: Int = 0
)

/** Talks to the backend: sends a 6-digit code to the e-mail and checks it. */
class EmailVerificationService {
    private val tag = "EmailVerification"

    suspend fun sendCode(email: String, fighterId: String): EmailCodeResult = withContext(Dispatchers.IO) {
        call("email_code_send", JSONObject().apply {
            put("email", email.trim().lowercase())
            put("fighter_id", fighterId)
        }) { EmailCodeResult(true, "Код отправлен на ${email.trim()}") }
    }

    suspend fun verifyCode(
        email: String,
        fighterId: String,
        code: String,
        callsign: String,
        unitName: String,
        unitKey: String
    ): EmailCodeResult = withContext(Dispatchers.IO) {
        call("email_code_verify", JSONObject().apply {
            put("email", email.trim().lowercase())
            put("fighter_id", fighterId)
            put("code", code)
            put("callsign", callsign)
            put("unit_name", unitName)
            put("unit_key", unitKey)
        }) { EmailCodeResult(true, "Почта подтверждена") }
    }

    private fun call(action: String, payload: JSONObject, onOk: () -> EmailCodeResult): EmailCodeResult {
        val base = BuildConfig.PAYMENT_API_URL.trim().trimEnd('/')
        if (base.isBlank()) return EmailCodeResult(false, "Сервер не настроен", canSkip = true)
        return try {
            val connection = (URL("$base?action=${Uri.encode(action)}").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 12_000
                readTimeout = 15_000
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            try {
                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(payload.toString()) }
                val code = connection.responseCode
                val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                val json = runCatching { JSONObject(body) }.getOrDefault(JSONObject())
                if (code in 200..299 && json.optBoolean("ok")) onOk() else mapError(json)
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            Log.w(tag, "$action failed: ${e.javaClass.simpleName}")
            EmailCodeResult(
                false,
                "Нет связи с сервером. Проверьте интернет или продолжите без подтверждения — подтвердить почту можно позже.",
                canSkip = true
            )
        }
    }

    private fun mapError(json: JSONObject): EmailCodeResult {
        val retry = json.optInt("retry_after_seconds", 0)
        return when (json.optString("error")) {
            "INVALID_EMAIL" -> EmailCodeResult(false, "Проверьте адрес почты.")
            "EMAIL_CODE_TOO_SOON" -> EmailCodeResult(false, "Код уже отправлен. Повторно — через $retry с.", retryAfterSeconds = retry)
            "EMAIL_CODE_HOURLY_LIMIT" -> EmailCodeResult(false, "Слишком много писем за час. Попробуйте позже.", retryAfterSeconds = retry)
            "RATE_LIMITED" -> EmailCodeResult(false, "Слишком часто. Подождите минуту.", retryAfterSeconds = retry)
            "EMAIL_PROVIDER_UNAVAILABLE" -> EmailCodeResult(false, "Отправка писем временно недоступна. Можно продолжить без подтверждения.", canSkip = true)
            "WRONG_CODE" -> EmailCodeResult(false, "Неверный код. Осталось попыток: ${json.optInt("attempts_left")}")
            "CODE_EXPIRED" -> EmailCodeResult(false, "Код устарел. Отправьте новый.")
            "TOO_MANY_ATTEMPTS" -> EmailCodeResult(false, "Слишком много неверных попыток. Отправьте новый код.")
            "INVALID_CODE" -> EmailCodeResult(false, "Введите 6 цифр из письма.")
            else -> EmailCodeResult(false, "Сервер отклонил запрос. Попробуйте ещё раз.", canSkip = true)
        }
    }
}
