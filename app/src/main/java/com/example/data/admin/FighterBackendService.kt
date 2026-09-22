package com.example.data.admin

import android.net.Uri
import android.os.Build
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

class FighterBackendService {
    private val tag = "FighterBackendService"

    private fun backendBaseUrl(): String =
        BuildConfig.PAYMENT_API_URL.trim().trimEnd('/')

    suspend fun upsertFighter(
        fighterId: String,
        callsign: String,
        unitName: String,
        unitKey: String,
        email: String
    ): Boolean = withContext(Dispatchers.IO) {
        if (fighterId.isBlank() || backendBaseUrl().isBlank()) return@withContext false
        try {
            val response = request(
                "fighter_upsert",
                JSONObject().apply {
                    put("fighter_id", fighterId.trim())
                    put("callsign", callsign.trim())
                    put("unit_name", unitName.trim())
                    put("unit_key", unitKey.trim())
                    put("email", email.trim().lowercase())
                    put("device_model", (Build.MANUFACTURER + " " + Build.MODEL).trim())
                }
            )
            response.optBoolean("ok", false)
        } catch (e: Exception) {
            Log.w(tag, "Fighter upsert backend unavailable", e)
            false
        }
    }

    suspend fun lookupFighter(
        email: String,
        callsign: String,
        fighterId: String
    ): FighterAdminRecord? = withContext(Dispatchers.IO) {
        if (backendBaseUrl().isBlank()) return@withContext null
        try {
            val response = request(
                "fighter_lookup",
                JSONObject().apply {
                    put("email", email.trim().lowercase())
                    put("callsign", callsign.trim())
                    put("fighter_id", fighterId.trim())
                }
            )
            if (!response.optBoolean("ok", false)) return@withContext null
            val fighter = response.optJSONObject("fighter") ?: return@withContext null
            FighterAdminRecord(
                id = fighter.optString("id"),
                unitName = fighter.optString("unit_name"),
                unitKey = fighter.optString("unit_key")
            )
        } catch (e: Exception) {
            Log.w(tag, "Fighter lookup backend unavailable", e)
            null
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
            return if (body.isBlank()) JSONObject() else JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }
}
