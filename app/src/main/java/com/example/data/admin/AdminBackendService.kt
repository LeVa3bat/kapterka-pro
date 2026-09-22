package com.example.data.admin

import android.net.Uri
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class AdminAuthResult(
    val success: Boolean,
    val token: String = "",
    val errorMessage: String = ""
)

data class AdminActionResult(
    val success: Boolean,
    val licenseKey: String = "",
    val expiresAt: Long = 0L,
    val errorMessage: String = ""
)

data class AdminFightersResult(
    val success: Boolean,
    val fighters: List<FighterAdminRecord> = emptyList(),
    val errorMessage: String = ""
)

class AdminBackendService {
    private val tag = "AdminBackendService"

    private fun backendBaseUrl(): String =
        BuildConfig.PAYMENT_API_URL.trim().trimEnd('/')

    suspend fun authenticate(secret: String): AdminAuthResult = withContext(Dispatchers.IO) {
        if (secret.isBlank()) return@withContext AdminAuthResult(false, errorMessage = "Введите служебный ключ.")
        if (backendBaseUrl().isBlank()) {
            return@withContext AdminAuthResult(false, errorMessage = "Сервер администрирования не настроен.")
        }
        try {
            val response = request(
                "admin_auth",
                JSONObject().apply { put("secret", secret.trim()) }
            )
            if (response.optBoolean("ok", false)) {
                val token = response.optString("admin_token")
                if (token.isNotBlank()) AdminAuthResult(true, token = token)
                else AdminAuthResult(false, errorMessage = "Сервер не выдал служебную сессию.")
            } else {
                AdminAuthResult(false, errorMessage = errorText(response))
            }
        } catch (e: Exception) {
            Log.e(tag, "Admin auth failed", e)
            AdminAuthResult(false, errorMessage = "Не удалось связаться с сервером администрирования.")
        }
    }

    suspend fun listFighters(token: String): AdminFightersResult =
        withContext(Dispatchers.IO) {
            if (token.isBlank()) {
                return@withContext AdminFightersResult(
                    false,
                    errorMessage = "Служебная сессия истекла."
                )
            }
            try {
                val response = request(
                    "admin_list_fighters",
                    JSONObject().apply { put("admin_token", token) }
                )
                if (!response.optBoolean("ok", false)) {
                    return@withContext AdminFightersResult(
                        false,
                        errorMessage = errorText(response)
                    )
                }

                val now = System.currentTimeMillis()
                val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                val array = response.optJSONArray("fighters")
                val fighters = buildList {
                    if (array != null) {
                        for (i in 0 until array.length()) {
                            val obj = array.optJSONObject(i) ?: continue
                            val expiresAt = obj.optLong("expires_at", 0L)
                            val lastSeen = obj.optLong("last_seen_at", 0L)
                            val registeredAt = obj.optLong("registered_at", 0L)
                            val active = expiresAt > now
                            add(
                                FighterAdminRecord(
                                    id = obj.optString("id"),
                                    callsign = obj.optString("callsign"),
                                    role = obj.optString("role", "Старшина подразделения"),
                                    unitName = obj.optString("unit_name"),
                                    unitKey = obj.optString("unit_key"),
                                    licenseKey = obj.optString("license_key"),
                                    isProActive = active,
                                    licenseDaysLeft = if (active) {
                                        ((expiresAt - now) / 86400000L).toInt().coerceAtLeast(1)
                                    } else 0,
                                    licenseExpiresFormatted = if (expiresAt > 0L) sdf.format(Date(expiresAt)) else "Нет",
                                    registeredAtMillis = registeredAt,
                                    registeredAtFormatted = if (registeredAt > 0L) sdf.format(Date(registeredAt)) else "",
                                    lastSeenMillis = lastSeen,
                                    lastSeenFormatted = if (lastSeen > 0L) sdf.format(Date(lastSeen)) else "",
                                    isOnline = lastSeen > 0L && now - lastSeen < 15L * 60L * 1000L,
                                    email = obj.optString("email"),
                                    deviceModel = obj.optString("device_model")
                                )
                            )
                        }
                    }
                }
                AdminFightersResult(true, fighters = fighters)
            } catch (e: Exception) {
                Log.e(tag, "Admin list fighters failed", e)
                AdminFightersResult(
                    false,
                    errorMessage = "Не удалось загрузить реестр через сервер."
                )
            }
        }

    suspend fun grantLicense(token: String, fighterId: String, days: Int): AdminActionResult =
        withContext(Dispatchers.IO) {
            if (token.isBlank()) return@withContext AdminActionResult(false, errorMessage = "Служебная сессия истекла.")
            try {
                val response = request(
                    "admin_grant_license",
                    JSONObject().apply {
                        put("admin_token", token)
                        put("fighter_id", fighterId.trim())
                        put("days", days.coerceIn(1, 365))
                    }
                )
                if (response.optBoolean("ok", false)) {
                    AdminActionResult(
                        true,
                        licenseKey = response.optString("license_key"),
                        expiresAt = response.optLong("expires_at", 0L)
                    )
                } else {
                    AdminActionResult(false, errorMessage = errorText(response))
                }
            } catch (e: Exception) {
                Log.e(tag, "Admin grant failed", e)
                AdminActionResult(false, errorMessage = "Не удалось выдать лицензию через сервер.")
            }
        }

    suspend fun deleteFighter(token: String, fighterId: String): AdminActionResult =
        withContext(Dispatchers.IO) {
            if (token.isBlank()) return@withContext AdminActionResult(false, errorMessage = "Служебная сессия истекла.")
            try {
                val response = request(
                    "admin_delete_fighter",
                    JSONObject().apply {
                        put("admin_token", token)
                        put("fighter_id", fighterId.trim())
                    }
                )
                if (response.optBoolean("ok", false)) {
                    AdminActionResult(true)
                } else {
                    AdminActionResult(false, errorMessage = errorText(response))
                }
            } catch (e: Exception) {
                Log.e(tag, "Admin delete failed", e)
                AdminActionResult(false, errorMessage = "Не удалось удалить запись через сервер.")
            }
        }

    private fun request(action: String, payload: JSONObject): JSONObject {
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
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(payload.toString()) }
            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.let {
                BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { reader -> reader.readText() }
            }.orEmpty()
            return if (body.isBlank()) JSONObject() else JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun errorText(response: JSONObject): String = when (response.optString("error")) {
        "ADMIN_AUTH_FAILED" -> "Неверный служебный ключ."
        "ADMIN_AUTH_RATE_LIMITED" -> "Слишком много попыток входа. Повторите позже."
        "ADMIN_SESSION_INVALID" -> "Служебная сессия истекла. Войдите заново."
        "ADMIN_SESSION_NOT_CONFIGURED" -> "Серверная админ-сессия не настроена."
        "MISSING_FIGHTER_ID" -> "Не выбран пользователь."
        else -> "Сервер отклонил административную операцию."
    }
}
