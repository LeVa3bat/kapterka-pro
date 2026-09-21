package com.example.data.payment

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class YooKassaConfig(
    val shopId: String = "1450722",
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

class YooKassaPaymentService(private val context: Context) {
    private val TAG = "YooKassaService"
    private val PREFS_NAME = "yookassa_settings_prefs"

    companion object {
        const val DIRECT_PAYMENT_URL = "https://yookassa.ru/my/i/apiQMG65ZHIE/l"

        // Public backend URL only. YooKassa secret credentials remain on the server.
        const val PAYMENT_API_URL =
            "https://script.google.com/macros/s/AKfycbwuwY74vD9El1R6ZVvO3DDpJ7BkY-wX0ljRphWRSA-jgB33-duUAqEp0g03D_7oFzjqmA/exec"
    }

    fun getConfig(): YooKassaConfig {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return YooKassaConfig(
            shopId = sp.getString("shop_id", "1450722") ?: "1450722",
            secretKey = "",
            isTestMode = false,
            priceRubles = sp.getInt("price_rubles", 490)
        )
    }

    /**
     * Kept for binary/source compatibility with the existing UI.
     * Secret keys are intentionally ignored: payment credentials must never be stored in the APK.
     */
    fun saveConfig(
        shopId: String,
        secretKey: String,
        isTestMode: Boolean,
        priceRubles: Int = 490
    ) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit()
            .putString("shop_id", shopId.trim().ifBlank { "1450722" })
            .remove("secret_key")
            .remove("is_test_mode")
            .putInt("price_rubles", priceRubles)
            .apply()

        if (secretKey.isNotBlank() || isTestMode) {
            Log.w(TAG, "Client-side YooKassa secret/test-mode settings are ignored by design")
        }
    }

    suspend fun createPayment(
        fighterCallsign: String,
        fighterEmail: String,
        returnUrl: String = "kapterka://payment_success"
    ): PaymentInitResult = withContext(Dispatchers.IO) {
        val cleanEmail = fighterEmail.trim().lowercase()
        if (!Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(cleanEmail)) {
            return@withContext PaymentInitResult(
                success = false,
                errorMessage = "Укажите корректный Email для оплаты и восстановления лицензии."
            )
        }

        val callsign = fighterCallsign.trim().ifBlank { "Пользователь" }
        val idempotenceKey = UUID.randomUUID().toString()

        try {
            val requestUrl = buildString {
                append(PAYMENT_API_URL)
                append("?action=pay")
                append("&email=").append(urlEncode(cleanEmail))
                append("&callsign=").append(urlEncode(callsign))
                append("&return_url=").append(urlEncode(returnUrl))
                append("&idempotence_key=").append(urlEncode(idempotenceKey))
            }

            val json = getJson(requestUrl)
            val paymentId = json.optString("payment_id")
            val confirmationUrl = json.optString("confirmation_url")
            val ok = json.optBoolean("ok", confirmationUrl.isNotBlank())

            if (ok && paymentId.isNotBlank() && confirmationUrl.isNotBlank()) {
                PaymentInitResult(
                    success = true,
                    paymentId = paymentId,
                    confirmationUrl = confirmationUrl
                )
            } else {
                PaymentInitResult(
                    success = false,
                    errorMessage = when (json.optString("error")) {
                        "INVALID_EMAIL" -> "Сервер оплаты отклонил Email."
                        "UPSTREAM_ERROR" -> "ЮKassa временно недоступна. Попробуйте позже."
                        else -> "Сервер не смог создать платёж ЮKassa."
                    }
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Payment backend create error", e)
            PaymentInitResult(
                success = false,
                errorMessage = "Не удалось связаться с сервером оплаты. Проверьте интернет и попробуйте позже."
            )
        }
    }

    suspend fun verifyPaymentStatus(paymentId: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val cleanId = paymentId.trim()
        if (cleanId.isBlank()) {
            return@withContext Pair(false, "Счёт на оплату не найден.")
        }

        if (cleanId.startsWith("yk_direct_") ||
            cleanId.startsWith("yk_order_") ||
            cleanId.startsWith("pay_test_")
        ) {
            return@withContext Pair(false, "Платёж не зарегистрирован сервером ЮKassa.")
        }

        try {
            val requestUrl =
                "${PAYMENT_API_URL}?action=check&payment_id=${urlEncode(cleanId)}"
            val json = getJson(requestUrl)
            val status = json.optString("status", "unknown")
            val paid = json.optBoolean("paid", false) ||
                (status == "succeeded" && json.optString("key").isNotBlank())

            when {
                status == "succeeded" && paid ->
                    Pair(true, "Оплата подтверждена сервером ЮKassa.")
                status == "pending" ->
                    Pair(false, "Платёж ожидает завершения.")
                status == "waiting_for_capture" ->
                    Pair(false, "Платёж авторизован, ожидается окончательное списание.")
                status == "canceled" ->
                    Pair(false, "Платёж отменён.")
                json.optString("error") == "UPSTREAM_ERROR" ->
                    Pair(false, "Сервер оплаты временно не получил ответ ЮKassa.")
                else ->
                    Pair(false, "Оплата пока не подтверждена. Статус: $status")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Payment backend verify error", e)
            Pair(false, "Не удалось проверить оплату через сервер. Попробуйте ещё раз позже.")
        }
    }

    fun openPaymentUrl(confirmationUrl: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(confirmationUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening payment link", e)
        }
    }

    private fun getJson(requestUrl: String): JSONObject {
        val connection = (URL(requestUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12000
            readTimeout = 12000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Cache-Control", "no-store")
        }

        return try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.let {
                BufferedReader(InputStreamReader(it)).use { reader -> reader.readText() }
            }.orEmpty()

            if (body.isBlank()) {
                throw IllegalStateException("Empty payment backend response (HTTP $code)")
            }

            val json = JSONObject(body)
            if (code !in 200..299) {
                Log.w(TAG, "Payment backend returned HTTP $code: ${json.optString("error")}")
            }
            json
        } finally {
            connection.disconnect()
        }
    }

    private fun urlEncode(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name())
}
