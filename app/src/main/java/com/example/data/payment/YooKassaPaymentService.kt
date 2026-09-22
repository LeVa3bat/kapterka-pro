package com.example.data.payment

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
import java.util.UUID

data class YooKassaConfig(
    val shopId: String = "",
    val secretKey: String = "",
    val isTestMode: Boolean = false,
    val priceRubles: Int = 490
)

data class PaymentInitResult(
    val success: Boolean,
    val paymentId: String = "",
    val confirmationUrl: String = "",
    val errorMessage: String? = null
)

/**
 * Future-safe payment client.
 *
 * IMPORTANT:
 * - YooKassa shop secret must never be stored in the APK or SharedPreferences.
 * - Android talks only to the Kapterka payment backend.
 * - The backend is responsible for creating and verifying YooKassa payments.
 *
 * The public method signatures intentionally stay compatible with 3.4.9 ViewModel code,
 * so this migration can be developed without touching the production release.
 */
class YooKassaPaymentService(private val context: Context) {
    private val tag = "YooKassaService"
    private val prefsName = "yookassa_settings_prefs"

    companion object {
        const val DIRECT_PAYMENT_URL = "https://yookassa.ru/my/i/apiQMG65ZHIE/l"
        private const val DEFAULT_PRICE_RUBLES = 490
    }

    private fun backendBaseUrl(): String =
        BuildConfig.PAYMENT_API_URL.trim().trimEnd('/')

    fun getConfig(): YooKassaConfig {
        val sp = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        return YooKassaConfig(
            shopId = "",
            secretKey = "",
            isTestMode = false,
            priceRubles = sp.getInt("price_rubles", DEFAULT_PRICE_RUBLES)
        )
    }

    /**
     * Kept only for source compatibility with the current developer UI.
     * Secret material is deliberately ignored and never persisted.
     */
    fun saveConfig(
        shopId: String,
        secretKey: String,
        isTestMode: Boolean,
        priceRubles: Int = DEFAULT_PRICE_RUBLES
    ) {
        @Suppress("UNUSED_VARIABLE")
        val ignoredLegacyValues = Triple(shopId, secretKey, isTestMode)
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .remove("shop_id")
            .remove("secret_key")
            .remove("is_test_mode")
            .putInt("price_rubles", priceRubles.coerceAtLeast(1))
            .apply()
    }

    suspend fun createPayment(
        fighterCallsign: String,
        fighterEmail: String,
        returnUrl: String = "kapterka://payment_success"
    ): PaymentInitResult = withContext(Dispatchers.IO) {
        val baseUrl = backendBaseUrl()
        if (baseUrl.isBlank()) {
            return@withContext PaymentInitResult(
                success = false,
                errorMessage = "Сервер оплаты пока не настроен для этой тестовой сборки."
            )
        }

        try {
            val response = requestBackend(
                action = "create",
                payload = JSONObject().apply {
                    put("callsign", fighterCallsign)
                    put("email", fighterEmail.trim())
                    put("return_url", returnUrl)
                    put("idempotence_key", UUID.randomUUID().toString())
                }
            )
            val ok = response.optBoolean("ok", false)
            val paymentId = response.optString("payment_id")
            val confirmationUrl = response.optString("confirmation_url")

            if (ok && paymentId.isNotBlank() && confirmationUrl.isNotBlank()) {
                PaymentInitResult(
                    success = true,
                    paymentId = paymentId,
                    confirmationUrl = confirmationUrl
                )
            } else {
                PaymentInitResult(
                    success = false,
                    errorMessage = backendErrorMessage(response)
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "Payment backend create error", e)
            PaymentInitResult(
                success = false,
                errorMessage = "Не удалось связаться с сервером оплаты. Попробуйте позже."
            )
        }
    }

    fun openPaymentUrl(confirmationUrl: String) {
        try {
            val uri = Uri.parse(confirmationUrl)
            if (uri.scheme != "https") {
                Log.e(tag, "Blocked non-HTTPS payment URL")
                return
            }
            context.startActivity(
                Intent(Intent.ACTION_VIEW, uri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        } catch (e: Exception) {
            Log.e(tag, "Error opening payment link", e)
        }
    }

    suspend fun verifyPaymentStatus(paymentId: String): Pair<Boolean, String> =
        withContext(Dispatchers.IO) {
            if (paymentId.isBlank()) {
                return@withContext Pair(
                    false,
                    "Счёт на оплату не найден. Сначала нажмите «Оплатить через ЮKassa»."
                )
            }

            if (!paymentId.matches(Regex("^[A-Za-z0-9_-]{6,100}$"))) {
                return@withContext Pair(false, "Некорректный идентификатор платежа.")
            }

            val baseUrl = backendBaseUrl()
            if (baseUrl.isBlank()) {
                return@withContext Pair(
                    false,
                    "Сервер проверки оплаты пока не настроен для этой тестовой сборки."
                )
            }

            try {
                val response = requestBackend(
                    action = "check",
                    payload = JSONObject().apply { put("payment_id", paymentId) }
                )
                val paid = response.optBoolean("paid", false)
                val status = response.optString("status", "unknown")

                when {
                    paid && status == "succeeded" ->
                        Pair(true, "Оплата подтверждена сервером.")
                    status == "pending" ->
                        Pair(false, "Платёж ожидает оплаты.")
                    status == "waiting_for_capture" ->
                        Pair(false, "Платёж авторизован, но ещё не завершён.")
                    status == "canceled" ->
                        Pair(false, "Платёж отменён без активации.")
                    else ->
                        Pair(false, backendErrorMessage(response))
                }
            } catch (e: Exception) {
                Log.e(tag, "Payment backend check error", e)
                Pair(false, "Не удалось проверить оплату через сервер. Попробуйте позже.")
            }
        }

    private fun requestBackend(action: String, payload: JSONObject): JSONObject {
        val endpoint = URL(backendBaseUrl() + "?action=" + Uri.encode(action))
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
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use {
                it.write(payload.toString())
            }

            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val text = stream?.let {
                BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            }.orEmpty()

            val json = if (text.isBlank()) JSONObject() else JSONObject(text)
            if (responseCode !in 200..299 && !json.has("ok")) {
                json.put("ok", false)
                json.put("error", "HTTP_$responseCode")
            }
            return json
        } finally {
            connection.disconnect()
        }
    }

    private fun backendErrorMessage(response: JSONObject): String {
        return when (response.optString("error")) {
            "INVALID_EMAIL" -> "Укажите корректный Email для оплаты."
            "MISSING_PAYMENT_ID" -> "Не найден идентификатор платежа."
            "PAYMENT_NOT_CONFIRMED" -> "Оплата ещё не подтверждена."
            "UPSTREAM_ERROR" -> "Сервис оплаты временно недоступен."
            else -> "Оплата не подтверждена сервером."
        }
    }
}
