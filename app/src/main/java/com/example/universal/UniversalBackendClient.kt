package com.example.universal

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

data class UniversalFeatureAccess(
    val coreInventory: Boolean = true,
    val localOperations: Boolean = true,
    val localCatalog: Boolean = true,
    val localWarehouses: Boolean = true,
    val cloudSync: Boolean = false,
    val multiDevice: Boolean = false,
    val exportReports: Boolean = false,
    val advancedRequisitions: Boolean = false
)

data class UniversalEntitlement(
    val status: String,
    val isProActive: Boolean,
    val isTrialActive: Boolean,
    val demoStartedAt: Long,
    val demoEndsAt: Long,
    val paidUntil: Long,
    val planId: String,
    val serverTime: Long,
    val features: UniversalFeatureAccess
) {
    val isExpired: Boolean get() = status == "expired"

    fun daysRemaining(): Int {
        val end = when {
            isProActive -> paidUntil
            isTrialActive -> demoEndsAt
            else -> 0L
        }
        if (end <= serverTime) return 0
        val remaining = end - serverTime
        return ((remaining + DAY_MS - 1L) / DAY_MS).toInt()
    }

    companion object {
        private const val DAY_MS = 24L * 60L * 60L * 1000L
    }
}

data class UniversalPaymentStart(
    val paymentId: String,
    val confirmationUrl: String
)

sealed interface UniversalBackendResult<out T> {
    data class Success<T>(val value: T) : UniversalBackendResult<T>
    data class Error(val message: String) : UniversalBackendResult<Nothing>
}

class UniversalBackendClient(context: Context) {
    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val auth = FirebaseAuth.getInstance()
    private val prefs = appContext.getSharedPreferences(
        "sklad_pro_subscription",
        Context.MODE_PRIVATE
    )
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    val isConfigured: Boolean
        get() = BuildConfig.PAYMENT_API_URL.trim().isNotBlank()

    fun cachedEntitlement(): UniversalEntitlement? {
        val raw = prefs.getString("entitlement_json", null) ?: return null
        return try {
            parseEntitlement(JSONObject(raw))
        } catch (_: Throwable) {
            null
        }
    }

    fun pendingPaymentId(): String =
        prefs.getString("pending_payment_id", "").orEmpty()

    fun bootstrap(callback: (UniversalBackendResult<UniversalEntitlement>) -> Unit) {
        authorizedRequest(
            method = "POST",
            path = "/v1/bootstrap",
            jsonBody = JSONObject()
        ) { result ->
            callback(result.mapJsonToEntitlement())
        }
    }

    fun refresh(callback: (UniversalBackendResult<UniversalEntitlement>) -> Unit) {
        authorizedRequest(
            method = "GET",
            path = "/v1/me"
        ) { result ->
            callback(result.mapJsonToEntitlement())
        }
    }

    fun startMonthlyPayment(
        callback: (UniversalBackendResult<UniversalPaymentStart>) -> Unit
    ) {
        val body = JSONObject().put("planId", "pro_month")
        authorizedRequest(
            method = "POST",
            path = "/v1/payments",
            jsonBody = body,
            idempotenceKey = UUID.randomUUID().toString()
        ) { result ->
            when (result) {
                is UniversalBackendResult.Error -> callback(result)
                is UniversalBackendResult.Success -> {
                    val json = result.value
                    val paymentId = json.optString("paymentId")
                    val confirmationUrl = json.optString("confirmationUrl")
                    if (paymentId.isBlank() || confirmationUrl.isBlank()) {
                        callback(UniversalBackendResult.Error("Сервер оплаты вернул неполный ответ"))
                    } else {
                        prefs.edit().putString("pending_payment_id", paymentId).apply()
                        callback(
                            UniversalBackendResult.Success(
                                UniversalPaymentStart(paymentId, confirmationUrl)
                            )
                        )
                    }
                }
            }
        }
    }

    fun checkPendingPayment(
        callback: (UniversalBackendResult<UniversalEntitlement>) -> Unit
    ) {
        val paymentId = pendingPaymentId()
        if (paymentId.isBlank()) {
            refresh(callback)
            return
        }

        val encoded = java.net.URLEncoder.encode(paymentId, "UTF-8")
        authorizedRequest(
            method = "GET",
            path = "/v1/payments/status?payment_id=$encoded"
        ) { result ->
            when (result) {
                is UniversalBackendResult.Error -> callback(result)
                is UniversalBackendResult.Success -> {
                    val entitlementObject = result.value.optJSONObject("entitlement")
                    if (entitlementObject == null) {
                        callback(UniversalBackendResult.Error("Не удалось получить статус подписки"))
                    } else {
                        val entitlement = parseEntitlement(entitlementObject)
                        cache(entitlement)
                        if (result.value.optString("status") == "succeeded") {
                            prefs.edit().remove("pending_payment_id").apply()
                        }
                        callback(UniversalBackendResult.Success(entitlement))
                    }
                }
            }
        }
    }

    private fun authorizedRequest(
        method: String,
        path: String,
        jsonBody: JSONObject? = null,
        idempotenceKey: String? = null,
        callback: (UniversalBackendResult<JSONObject>) -> Unit
    ) {
        val baseUrl = BuildConfig.PAYMENT_API_URL.trim().trimEnd('/')
        if (baseUrl.isBlank()) {
            deliver(callback, UniversalBackendResult.Error("Сервер Склад ПРО ещё не подключён к этой сборке"))
            return
        }

        val user = auth.currentUser
        if (user == null || !user.isEmailVerified) {
            deliver(callback, UniversalBackendResult.Error("Подтвердите email и войдите в аккаунт"))
            return
        }

        user.getIdToken(false).addOnCompleteListener { tokenTask ->
            val token = tokenTask.result?.token
            if (!tokenTask.isSuccessful || token.isNullOrBlank()) {
                deliver(callback, UniversalBackendResult.Error("Не удалось подтвердить аккаунт"))
                return@addOnCompleteListener
            }

            val requestBuilder = Request.Builder()
                .url(baseUrl + path)
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/json")

            if (!idempotenceKey.isNullOrBlank()) {
                requestBuilder.header("Idempotence-Key", idempotenceKey)
            }

            when (method) {
                "POST" -> {
                    val body = (jsonBody ?: JSONObject())
                        .toString()
                        .toRequestBody(JSON_MEDIA_TYPE)
                    requestBuilder.post(body)
                }
                else -> requestBuilder.get()
            }

            client.newCall(requestBuilder.build()).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    deliver(
                        callback,
                        UniversalBackendResult.Error("Нет соединения с сервером Склад ПРО")
                    )
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        val raw = it.body?.string().orEmpty()
                        val parsed = try {
                            if (raw.isBlank()) JSONObject() else JSONObject(raw)
                        } catch (_: Throwable) {
                            JSONObject()
                        }

                        if (!it.isSuccessful || parsed.optBoolean("ok", false).not()) {
                            val code = parsed.optString("error")
                            deliver(callback, UniversalBackendResult.Error(mapError(code)))
                            return
                        }

                        deliver(callback, UniversalBackendResult.Success(parsed))
                    }
                }
            })
        }
    }

    private fun UniversalBackendResult<JSONObject>.mapJsonToEntitlement():
        UniversalBackendResult<UniversalEntitlement> = when (this) {
        is UniversalBackendResult.Error -> this
        is UniversalBackendResult.Success -> {
            val entitlementObject = value.optJSONObject("entitlement")
            if (entitlementObject == null) {
                UniversalBackendResult.Error("Сервер не вернул данные подписки")
            } else {
                try {
                    val entitlement = parseEntitlement(entitlementObject)
                    cache(entitlement)
                    UniversalBackendResult.Success(entitlement)
                } catch (_: Throwable) {
                    UniversalBackendResult.Error("Не удалось прочитать данные подписки")
                }
            }
        }
    }

    private fun cache(entitlement: UniversalEntitlement) {
        val json = JSONObject()
            .put("status", entitlement.status)
            .put("isProActive", entitlement.isProActive)
            .put("isTrialActive", entitlement.isTrialActive)
            .put("demoStartedAt", entitlement.demoStartedAt)
            .put("demoEndsAt", entitlement.demoEndsAt)
            .put("paidUntil", entitlement.paidUntil)
            .put("planId", entitlement.planId)
            .put("serverTime", entitlement.serverTime)
            .put(
                "features",
                JSONObject()
                    .put("coreInventory", entitlement.features.coreInventory)
                    .put("localOperations", entitlement.features.localOperations)
                    .put("localCatalog", entitlement.features.localCatalog)
                    .put("localWarehouses", entitlement.features.localWarehouses)
                    .put("cloudSync", entitlement.features.cloudSync)
                    .put("multiDevice", entitlement.features.multiDevice)
                    .put("exportReports", entitlement.features.exportReports)
                    .put("advancedRequisitions", entitlement.features.advancedRequisitions)
            )
        prefs.edit().putString("entitlement_json", json.toString()).apply()
    }

    private fun parseEntitlement(json: JSONObject): UniversalEntitlement {
        val featuresJson = json.optJSONObject("features") ?: JSONObject()
        return UniversalEntitlement(
            status = json.optString("status", "expired"),
            isProActive = json.optBoolean("isProActive", false),
            isTrialActive = json.optBoolean("isTrialActive", false),
            demoStartedAt = json.optLong("demoStartedAt", 0L),
            demoEndsAt = json.optLong("demoEndsAt", 0L),
            paidUntil = json.optLong("paidUntil", 0L),
            planId = json.optString("planId", ""),
            serverTime = json.optLong("serverTime", System.currentTimeMillis()),
            features = UniversalFeatureAccess(
                coreInventory = featuresJson.optBoolean("coreInventory", true),
                localOperations = featuresJson.optBoolean("localOperations", false),
                localCatalog = featuresJson.optBoolean("localCatalog", false),
                localWarehouses = featuresJson.optBoolean("localWarehouses", false),
                cloudSync = featuresJson.optBoolean("cloudSync", false),
                multiDevice = featuresJson.optBoolean("multiDevice", false),
                exportReports = featuresJson.optBoolean("exportReports", false),
                advancedRequisitions = featuresJson.optBoolean("advancedRequisitions", false)
            )
        )
    }

    private fun mapError(code: String): String = when (code) {
        "AUTH_REQUIRED", "INVALID_AUTH_TOKEN" -> "Сессия истекла. Войдите в аккаунт ещё раз"
        "EMAIL_VERIFICATION_REQUIRED" -> "Сначала подтвердите email"
        "INVALID_PLAN" -> "Тариф временно недоступен"
        "PAYMENT_NOT_CONFIGURED" -> "Оплата ещё не настроена на сервере"
        "PAYMENT_PROVIDER_ERROR" -> "ЮKassa временно недоступна"
        "PAYMENT_BINDING_MISMATCH",
        "PAYMENT_AMOUNT_MISMATCH",
        "PAYMENT_MODE_MISMATCH" -> "Платёж не прошёл серверную проверку"
        else -> "Ошибка сервера Склад ПРО"
    }

    private fun <T> deliver(
        callback: (UniversalBackendResult<T>) -> Unit,
        result: UniversalBackendResult<T>
    ) {
        main.post { callback(result) }
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
