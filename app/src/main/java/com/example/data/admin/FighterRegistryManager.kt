package com.example.data.admin

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FighterAdminRecord(
    val id: String,
    val callsign: String = "",
    val role: String = "Старшина / Боец",
    val unitName: String = "",
    val unitKey: String = "",
    val licenseKey: String = "",
    val isProActive: Boolean = false,
    val licenseDaysLeft: Int = 0,
    val licenseExpiresFormatted: String = "",
    val registeredAtMillis: Long = 0L,
    val registeredAtFormatted: String = "",
    val lastSeenMillis: Long = 0L,
    val lastSeenFormatted: String = "",
    val isOnline: Boolean = false,
    val email: String = "",
    val deviceModel: String = ""
)

class FighterRegistryManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val tag = "FighterRegistryManager"
    private val prefsName = "kapterka_fighters_registry_cache"
    private val keyFightersJson = "cached_fighters_list"
    private val backend = FighterBackendService()

    private val _fighters = MutableStateFlow<List<FighterAdminRecord>>(emptyList())
    val fighters: StateFlow<List<FighterAdminRecord>> = _fighters.asStateFlow()

    init {
        loadCachedFighters()
    }

    private fun loadCachedFighters() {
        val raw = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .getString(keyFightersJson, null)
        if (raw.isNullOrBlank()) {
            _fighters.value = emptyList()
            return
        }

        try {
            val list = parseFightersJson(raw).filterNot {
                it.id.contains("SOKOL") ||
                    it.id.contains("GROM") ||
                    it.id.contains("SEVER") ||
                    it.callsign.lowercase(Locale.ROOT) in listOf("сокол", "гром", "север")
            }
            _fighters.value = list
            saveFightersToCache(list)
        } catch (e: Exception) {
            Log.w(tag, "Failed reading fighter cache", e)
            _fighters.value = emptyList()
        }
    }

    private fun saveFightersToCache(list: List<FighterAdminRecord>) {
        try {
            val arr = JSONArray()
            list.forEach { fighter ->
                arr.put(JSONObject().apply {
                    put("id", fighter.id)
                    put("callsign", fighter.callsign)
                    put("role", fighter.role)
                    put("unitName", fighter.unitName)
                    put("unitKey", fighter.unitKey)
                    put("licenseKey", fighter.licenseKey)
                    put("isProActive", fighter.isProActive)
                    put("licenseDaysLeft", fighter.licenseDaysLeft)
                    put("licenseExpiresFormatted", fighter.licenseExpiresFormatted)
                    put("registeredAtMillis", fighter.registeredAtMillis)
                    put("registeredAtFormatted", fighter.registeredAtFormatted)
                    put("lastSeenMillis", fighter.lastSeenMillis)
                    put("lastSeenFormatted", fighter.lastSeenFormatted)
                    put("isOnline", fighter.isOnline)
                    put("email", fighter.email)
                    put("deviceModel", fighter.deviceModel)
                })
            }
            context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                .edit()
                .putString(keyFightersJson, arr.toString())
                .apply()
        } catch (e: Exception) {
            Log.w(tag, "Failed saving fighter cache", e)
        }
    }

    private fun parseFightersJson(raw: String): List<FighterAdminRecord> {
        val arr = JSONArray(raw)
        return buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                add(
                    FighterAdminRecord(
                        id = obj.optString("id"),
                        callsign = obj.optString("callsign"),
                        role = obj.optString("role", "Старшина / Боец"),
                        unitName = obj.optString("unitName"),
                        unitKey = obj.optString("unitKey"),
                        licenseKey = obj.optString("licenseKey"),
                        isProActive = obj.optBoolean("isProActive", false),
                        licenseDaysLeft = obj.optInt("licenseDaysLeft", 0),
                        licenseExpiresFormatted = obj.optString("licenseExpiresFormatted"),
                        registeredAtMillis = obj.optLong("registeredAtMillis", 0L),
                        registeredAtFormatted = obj.optString("registeredAtFormatted"),
                        lastSeenMillis = obj.optLong("lastSeenMillis", 0L),
                        lastSeenFormatted = obj.optString("lastSeenFormatted"),
                        isOnline = obj.optBoolean("isOnline", false),
                        email = obj.optString("email"),
                        deviceModel = obj.optString("deviceModel")
                    )
                )
            }
        }
    }

    /**
     * Admin-only result replacement. The global fighter list is never fetched
     * directly from Firestore by Android.
     */
    fun replaceCachedFighters(fighters: List<FighterAdminRecord>) {
        _fighters.value = fighters
        saveFightersToCache(fighters)
    }

    fun removeCachedFighter(fighterId: String) {
        val next = _fighters.value.filterNot { it.id == fighterId }
        _fighters.value = next
        saveFightersToCache(next)
    }

    /**
     * Ordinary profile registration/update. License state is deliberately not
     * trusted from Android; the server derives it from the license registry.
     */
    fun registerOrUpdateFighter(
        fighterId: String,
        callsign: String,
        unitName: String,
        unitKey: String,
        email: String,
        licenseKey: String = "",
        isProActive: Boolean = false,
        expiresAt: Long = 0L,
        role: String = "Старшина склада"
    ) {
        val now = System.currentTimeMillis()
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val daysLeft = if (expiresAt > now) {
            ((expiresAt - now) / 86400000L).toInt().coerceAtLeast(1)
        } else if (isProActive) {
            30
        } else {
            0
        }

        val record = FighterAdminRecord(
            id = fighterId,
            callsign = callsign.ifBlank { "Боец" },
            role = role,
            unitName = unitName.ifBlank { "Подразделение" },
            unitKey = unitKey.trim(),
            licenseKey = licenseKey,
            isProActive = isProActive,
            licenseDaysLeft = daysLeft,
            licenseExpiresFormatted = if (expiresAt > 0L) sdf.format(Date(expiresAt)) else "",
            registeredAtMillis = now,
            registeredAtFormatted = sdf.format(Date(now)),
            lastSeenMillis = now,
            lastSeenFormatted = "В сети",
            isOnline = true,
            email = email.trim(),
            deviceModel = (Build.MANUFACTURER + " " + Build.MODEL).trim()
        )

        val current = _fighters.value.toMutableList()
        val index = current.indexOfFirst { it.id == fighterId }
        if (index >= 0) {
            val existing = current[index]
            current[index] = record.copy(
                registeredAtMillis = existing.registeredAtMillis,
                registeredAtFormatted = existing.registeredAtFormatted
            )
        } else {
            current.add(0, record)
        }
        _fighters.value = current
        saveFightersToCache(current)

        scope.launch(Dispatchers.IO) {
            val ok = backend.upsertFighter(
                fighterId = fighterId,
                callsign = callsign,
                unitName = unitName,
                unitKey = unitKey,
                email = email
            )
            if (!ok) {
                Log.w(tag, "Fighter server upsert deferred; local profile remains intact")
            }
        }
    }

    suspend fun lookupFighter(
        email: String = "",
        callsign: String = "",
        fighterId: String = ""
    ): FighterAdminRecord? = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase(Locale.ROOT)
        val cleanCallsign = callsign.trim().lowercase(Locale.ROOT)
        val cleanId = fighterId.trim()

        // Local admin cache must never act as a weaker identity-recovery path.
        // Unit-key recovery from cache is allowed only for the exact persistent
        // fighter id. Email/callsign alone are not sufficient.
        val local = if (cleanId.isNotBlank() && cleanEmail.isNotBlank()) {
            _fighters.value.firstOrNull {
                it.id == cleanId &&
                    it.email.trim().lowercase(Locale.ROOT) == cleanEmail
            }
        } else {
            null
        }
        if (local != null && local.unitKey.isNotBlank()) {
            return@withContext local
        }

        backend.lookupFighter(
            email = cleanEmail,
            callsign = cleanCallsign,
            fighterId = cleanId
        )
    }

    @Deprecated("Global registry refresh requires AdminBackendService.listFighters()")
    suspend fun fetchFightersFromCloud() {
        Log.w(tag, "Blocked direct global fighter-list read from Android")
    }

    @Deprecated("Privileged deletes must go through AdminBackendService")
    fun deleteFighter(fighterId: String) {
        Log.w(tag, "Blocked client-side fighter deletion")
    }

    @Deprecated("Privileged license grants must go through AdminBackendService")
    fun grantLicense(fighterId: String, days: Int = 30): String {
        Log.w(tag, "Blocked client-side license grant")
        return ""
    }
}
