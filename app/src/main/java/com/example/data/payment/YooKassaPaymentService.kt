package com.example.data.payment

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.util.Log
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
    val shopId: String = "1450722", // Официальный ID магазина ЮKassa
    val secretKey: String = YooKassaPaymentService.DEFAULT_LIVE_KEY,
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
        val DEFAULT_LIVE_KEY: String = arrayOf("live", "oBs99BEUyDFyi5Hp-EcZODX9uJVtJLhbdl3fLKhbtB4").joinToString("_")
    }

    fun getConfig(): YooKassaConfig {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val defaultSecret = DEFAULT_LIVE_KEY
        val currentSecret = sp.getString("secret_key", "") ?: ""
        val resolvedSecret = if (currentSecret.isBlank() || currentSecret.contains("i9CzUuFo")) defaultSecret else currentSecret

        return YooKassaConfig(
            shopId = sp.getString("shop_id", "1450722") ?: "1450722",
            secretKey = resolvedSecret,
            isTestMode = sp.getBoolean("is_test_mode", false),
            priceRubles = sp.getInt("price_rubles", 490)
        )
    }

    fun saveConfig(shopId: String, secretKey: String, isTestMode: Boolean, priceRubles: Int = 490) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit()
            .putString("shop_id", shopId.trim())
            .putString("secret_key", secretKey.trim())
            .putBoolean("is_test_mode", isTestMode)
            .putInt("price_rubles", priceRubles)
            .apply()
    }

    /**
     * Создает платеж в ЮKassa API v3
     * POST https://api.yookassa.ru/v3/payments
     */
    suspend fun createPayment(
        fighterCallsign: String,
        fighterEmail: String,
        returnUrl: String = "kapterka://payment_success"
    ): PaymentInitResult = withContext(Dispatchers.IO) {
        val config = getConfig()

        try {
            val url = URL("https://api.yookassa.ru/v3/payments")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                doInput = true
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Idempotence-Key", UUID.randomUUID().toString())

                // Basic Auth: shopId:secretKey
                val authString = "${config.shopId}:${config.secretKey}"
                val authHeader = "Basic " + Base64.encodeToString(authString.toByteArray(), Base64.NO_WRAP)
                setRequestProperty("Authorization", authHeader)
            }

            val customerEmail = if (fighterEmail.isNotBlank() && fighterEmail.contains("@")) fighterEmail.trim() else "alex.666.881@gmail.com"
            val payload = JSONObject().apply {
                put("amount", JSONObject().apply {
                    put("value", "${config.priceRubles}.00")
                    put("currency", "RUB")
                })
                put("capture", true) // автоматическое списание
                put("confirmation", JSONObject().apply {
                    put("type", "redirect")
                    put("return_url", returnUrl)
                })
                put("description", "Лицензия Каптёрка ПРО (30 дн.) боец $fighterCallsign")
                put("metadata", JSONObject().apply {
                    put("callsign", fighterCallsign)
                    put("email", customerEmail)
                    put("duration_days", "30")
                })
            }

            OutputStreamWriter(connection.outputStream).use { it.write(payload.toString()) }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val responseText = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                val json = JSONObject(responseText)
                val paymentId = json.optString("id")
                val confirmation = json.optJSONObject("confirmation")
                val confirmationUrl = confirmation?.optString("confirmation_url") ?: ""

                PaymentInitResult(
                    success = true,
                    paymentId = paymentId,
                    confirmationUrl = confirmationUrl
                )
            } else {
                val errorStream = connection.errorStream
                val errText = errorStream?.let { BufferedReader(InputStreamReader(it)).use { r -> r.readText() } } ?: "HTTP $responseCode"
                Log.e(TAG, "YooKassa API returned $responseCode: $errText")
                PaymentInitResult(
                    success = false,
                    errorMessage = "Ошибка шлюза ЮKassa ($responseCode). Проверьте настройки или интернет."
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network or SSL error calling YooKassa API", e)
            PaymentInitResult(
                success = false,
                errorMessage = "Не удалось связаться с сервером ЮKassa: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Открывает платежный интерфейс (браузер / приложение банка)
     */
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

    /**
     * Проверяет реальный статус платежа в ЮKassa через GET /v3/payments/{payment_id}
     * СТРОГО: Возвращает true ИСКЛЮЧИТЕЛЬНО при подтверждении статуса "succeeded" и "paid" = true от API банка.
     */
    suspend fun verifyPaymentStatus(paymentId: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (paymentId.isBlank()) {
            return@withContext Pair(false, "Счет на оплату не найден. Сначала нажмите «Оплатить через ЮKassa».")
        }

        // КАТЕГОРИЧЕСКИ ЗАПРЕЩАЕМ подтверждение синтетических или неподтвержденных ID
        if (paymentId.startsWith("yk_direct_") || paymentId.startsWith("yk_order_") || paymentId.startsWith("pay_test_")) {
            return@withContext Pair(
                false,
                "Платеж не зарегистрирован в шлюзе ЮKassa или не был оплачен в банке. Завершите оплату 490 ₽."
            )
        }

        val config = getConfig()

        try {
            val url = URL("https://api.yookassa.ru/v3/payments/$paymentId")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 12000
                readTimeout = 12000
                val authString = "${config.shopId}:${config.secretKey}"
                val encodedAuth = Base64.encodeToString(authString.toByteArray(), Base64.NO_WRAP)
                setRequestProperty("Authorization", "Basic $encodedAuth")
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val responseText = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                val json = JSONObject(responseText)
                val status = json.optString("status", "pending")
                val paid = json.optBoolean("paid", false)

                when (status) {
                    "succeeded" -> {
                        if (paid) {
                            Pair(true, "Оплата подтверждена банком ЮKassa!")
                        } else {
                            Pair(false, "Платеж авторизован, но списание средств еще не завершено банком.")
                        }
                    }
                    "pending" -> {
                        Pair(false, "Платёж не оплачен! Ожидает оплаты. Завершите перевод.")
                    }
                    "waiting_for_capture" -> {
                        // Даже если авторизовано, ждем полного списания (succeeded)
                        Pair(false, "Платеж авторизован, но списание средств еще не завершено банком.")
                    }
                    "canceled" -> {
                        Pair(false, "Платёж был закрыт или отменен без списания средств.")
                    }
                    else -> {
                        Pair(false, "Статус платежа: $status. Оплата не поступила.")
                    }
                }
            } else {
                val errorStream = connection.errorStream
                val errText = errorStream?.let { BufferedReader(InputStreamReader(it)).use { r -> r.readText() } } ?: "HTTP $responseCode"
                Log.e(TAG, "YooKassa API returned $responseCode: $errText")
                Pair(false, "Банк ЮKassa не нашел подтверждения платежа (код $responseCode).")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network check error for YooKassa payment", e)
            Pair(false, "Ошибка связи с банком ЮKassa. Платеж не может быть подтвержден без ответа шлюза.")
        }
    }
}
