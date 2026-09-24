package com.example.data.sync

import android.net.Uri
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/** Result of registering this device as a member of a unit. */
sealed class UnitAccessResult {
    object Member : UnitAccessResult()
    /** Server or Firebase Auth unreachable / not configured — retry later. */
    data class Unavailable(val reason: String) : UnitAccessResult()
    /** The server refused (wrong key, too many attempts). */
    data class Denied(val reason: String) : UnitAccessResult()
}

/**
 * Proves this device's identity to the backend and registers it as a member
 * of a unit (`unit_join`). Firestore rules let only members read and write a
 * unit, so a guessed unit key is useless without passing this rate-limited
 * server check.
 *
 * The identity is an anonymous Firebase Auth account: nothing for the user to
 * type, and it survives app restarts.
 */
class UnitAccessService(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val tag = "UnitAccess"

    private val joinedUnits = mutableSetOf<String>()

    suspend fun ensureMembership(
        unitKey: String,
        fighterId: String,
        callsign: String,
        deviceId: String
    ): UnitAccessResult = withContext(Dispatchers.IO) {
        val cleanKey = unitKey.trim()
        val uid = auth.currentUser?.uid
        if (uid != null && joinedUnits.contains("$uid/$cleanKey")) return@withContext UnitAccessResult.Member

        val base = BuildConfig.PAYMENT_API_URL.trim().trimEnd('/')
        if (base.isBlank()) return@withContext UnitAccessResult.Unavailable("backend not configured")

        val idToken = try {
            withTimeout(15_000) {
                val user = auth.currentUser ?: auth.signInAnonymously().await().user
                user?.getIdToken(false)?.await()?.token
            }
        } catch (e: Exception) {
            Log.w(tag, "Anonymous sign-in failed: ${e.javaClass.simpleName}")
            null
        } ?: return@withContext UnitAccessResult.Unavailable("sign-in failed")

        val payload = JSONObject().apply {
            put("id_token", idToken)
            put("unit_key", cleanKey)
            put("create", true)
            put("fighter_id", fighterId)
            put("callsign", callsign)
            put("device_id", deviceId)
        }

        try {
            val connection = (URL(base + "?action=" + Uri.encode("unit_join")).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 12_000
                readTimeout = 12_000
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
            }
            try {
                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(payload.toString()) }
                val code = connection.responseCode
                val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                val json = runCatching { JSONObject(body) }.getOrDefault(JSONObject())
                when {
                    code in 200..299 && json.optBoolean("member", false) -> {
                        auth.currentUser?.uid?.let { joinedUnits.add("$it/$cleanKey") }
                        UnitAccessResult.Member
                    }
                    code == 429 -> UnitAccessResult.Denied("Слишком много попыток подключения. Подождите минуту.")
                    code == 400 && json.optString("error") == "INVALID_UNIT_KEY" ->
                        UnitAccessResult.Denied("Неверный формат ключа подразделения.")
                    code == 404 -> UnitAccessResult.Denied("Подразделение с таким ключом не найдено.")
                    else -> UnitAccessResult.Unavailable("HTTP $code ${json.optString("error")}")
                }
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            Log.w(tag, "unit_join failed: ${e.javaClass.simpleName}")
            UnitAccessResult.Unavailable(e.javaClass.simpleName)
        }
    }
}
