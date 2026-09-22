package com.example.data.license

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

data class LicenseBackendResult(
    val success: Boolean,
    val licenseKey: String = "",
    val expiresAt: Long = 0L,
    val fighterId: String = "",
    val errorMessage: String = ""
)

class LicenseBackendService {
    private val tag = "LicenseBackendService"

    private fun backendBaseUrl(): String =
        BuildConfig.PAYMENT_API_URL.trim().trimEnd('/')

    suspend fun verifyKey(
        licenseKey: String,
        fighterId: String
    ): LicenseBackendResult = withContext(Dispatchers.IO) {
        requestLicense(
            action = "license_verify",
            payload = JSONObject().apply {
                put("license_key", licenseKey.trim().uppercase())
                put("fighter_id", fighterId.trim())
            }
        )
    }

    suspend fun restoreByEmail(
        email: String,
        fighterId: String
    ): LicenseBackendResult = withContext(Dispatchers.IO) {
        requestLicense(
            action = "license_restore",
            payload = JSONObject().apply {
                put("email", email.trim().lowercase())
                put("fighter_id", fighterId.trim())
            }
        )
    }

    private fun requestLicense(
        action: String,
        payload: JSONObject
    ): LicenseBackendResult {
        val base = backendBaseUrl()
        if (base.isBlank()) {
            return LicenseBackendResult(
                success = false,
                errorMessage = "Сервер лицензий пока не настроен для этой тестовой сборки."
            )
        }

        return try {
            val endpoint = URL(base + "?action=" + Uri.encode(action))
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
                val json = if (body.isBlank()) JSONObject() else JSONObject(body)

                if (connection.responseCode in 200..299 && json.optBoolean("ok", false)) {
                    val key = json.optString("license_key")
                    val expiresAt = json.optLong("expires_at", 0L)
                    if (key.isNotBlank() && expiresAt > System.currentTimeMillis()) {
                        LicenseBackendResult(
                            success = true,
                            licenseKey = key,
                            expiresAt = expiresAt,
                            fighterId = json.optString("fighter_id")
                        )
                    } else {
                        LicenseBackendResult(
                            success = false,
                            errorMessage = "Сервер вернул недействительные данные лицензии."
                        )
                    }
                } else {
                    LicenseBackendResult(
                        success = false,
                        errorMessage = errorText(json)
                    )
                }
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            Log.w(tag, "License backend unavailable", e)
            LicenseBackendResult(
                success = false,
                errorMessage = "Не удалось связаться с сервером лицензий."
            )
        }
    }

    private fun errorText(response: JSONObject): String = when (response.optString("error")) {
        "INVALID_LICENSE_KEY" -> "Неверный формат ключа."
        "LICENSE_NOT_ACTIVE" -> "Лицензия не найдена или срок её действия истёк."
        "LICENSE_NOT_FOUND" -> "Активная оплаченная лицензия для этого Email не найдена."
        "FIGHTER_MISMATCH" -> "Лицензия привязана к другому пользователю."
        "LICENSE_RESTORE_IDENTITY_MISMATCH" -> "Автоматическое восстановление не разрешено на этом профиле. Используйте сохранённую резервную копию или обратитесь в поддержку для безопасной перепривязки."
        "MISSING_FIGHTER_ID" -> "Не найден идентификатор пользователя для безопасного восстановления."
        "INVALID_EMAIL" -> "Укажите корректный Email."
        else -> "Сервер не подтвердил лицензию."
    }
}
