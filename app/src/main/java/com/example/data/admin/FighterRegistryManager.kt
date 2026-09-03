package com.example.data.admin

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.data.license.LicenseManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FighterAdminRecord(
    val id: String, // Уникальный идентификатор бойца
    val callsign: String, // Позывной (например "сокол", "буран")
    val role: String = "Старшина / Боец", // Должность
    val unitName: String = "1-е Подразделение", // Название подразделения
    val unitKey: String = "kapt_59e13b", // Секретный ключ подразделения
    val licenseKey: String = "", // Ключ лицензии (KAPT-XXXX-XXXX-XXXX)
    val isProActive: Boolean = false, // Статус лицензии
    val licenseDaysLeft: Int = 0, // Оставшиеся дни
    val licenseExpiresFormatted: String = "", // Дата окончания
    val registeredAtMillis: Long = 0L, // Дата регистрации (таймстамп)
    val registeredAtFormatted: String = "", // Дата регистрации (строка)
    val lastSeenMillis: Long = 0L, // Время последней активности
    val lastSeenFormatted: String = "", // Время последней активности (строка)
    val isOnline: Boolean = false, // Онлайн прямо сейчас
    val email: String = "", // Почта
    val deviceModel: String = "" // Модель устройства
)

class FighterRegistryManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val TAG = "FighterRegistryManager"
    private val PREFS_NAME = "kapterka_fighters_registry_cache"
    private val KEY_FIGHTERS_JSON = "cached_fighters_list"
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private val _fighters = MutableStateFlow<List<FighterAdminRecord>>(emptyList())
    val fighters: StateFlow<List<FighterAdminRecord>> = _fighters.asStateFlow()

    init {
        loadCachedFighters()
        // Синхронизируем с Firestore в фоновом режиме
        scope.launch {
            fetchFightersFromCloud()
        }
    }

    private fun loadCachedFighters() {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = sp.getString(KEY_FIGHTERS_JSON, null)
        if (!jsonStr.isNullOrBlank()) {
            try {
                val list = parseFightersJson(jsonStr)
                    // Удаляем любые остаточные тестовые записи
                    .filterNot { 
                        it.id.contains("SOKOL") || 
                        it.id.contains("GROM") || 
                        it.id.contains("SEVER") ||
                        it.callsign in listOf("сокол", "гром", "север")
                    }
                _fighters.value = list
                saveFightersToCache(list)
                return
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse cached fighters", e)
            }
        }

        // Изначально список пуст — наполняется только реальными бойцами при регистрации
        _fighters.value = emptyList()
        saveFightersToCache(emptyList())
    }

    private fun saveFightersToCache(list: List<FighterAdminRecord>) {
        try {
            val jsonArr = JSONArray()
            val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            for (f in list) {
                val obj = JSONObject().apply {
                    put("id", f.id)
                    put("callsign", f.callsign)
                    put("role", f.role)
                    put("unitName", f.unitName)
                    put("unitKey", f.unitKey)
                    put("licenseKey", f.licenseKey)
                    put("isProActive", f.isProActive)
                    put("licenseDaysLeft", f.licenseDaysLeft)
                    put("licenseExpiresFormatted", f.licenseExpiresFormatted)
                    put("registeredAtMillis", f.registeredAtMillis)
                    put("registeredAtFormatted", f.registeredAtFormatted)
                    put("lastSeenMillis", f.lastSeenMillis)
                    put("lastSeenFormatted", f.lastSeenFormatted)
                    put("isOnline", f.isOnline)
                    put("email", f.email)
                    put("deviceModel", f.deviceModel)
                }
                jsonArr.put(obj)
            }
            val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            sp.edit().putString(KEY_FIGHTERS_JSON, jsonArr.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving fighters to cache", e)
        }
    }

    private fun parseFightersJson(jsonStr: String): List<FighterAdminRecord> {
        val list = mutableListOf<FighterAdminRecord>()
        val arr = JSONArray(jsonStr)
        val now = System.currentTimeMillis()
        val fifteenMinutes = 15 * 60 * 1000L

        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val lastSeen = obj.optLong("lastSeenMillis", 0L)
            val isOnline = (now - lastSeen) < fifteenMinutes || obj.optBoolean("isOnline", false)

            list.add(
                FighterAdminRecord(
                    id = obj.optString("id"),
                    callsign = obj.optString("callsign"),
                    role = obj.optString("role", "Боец"),
                    unitName = obj.optString("unitName", "Подразделение"),
                    unitKey = obj.optString("unitKey", "kapt_default"),
                    licenseKey = obj.optString("licenseKey", ""),
                    isProActive = obj.optBoolean("isProActive", false),
                    licenseDaysLeft = obj.optInt("licenseDaysLeft", 0),
                    licenseExpiresFormatted = obj.optString("licenseExpiresFormatted", ""),
                    registeredAtMillis = obj.optLong("registeredAtMillis", 0L),
                    registeredAtFormatted = obj.optString("registeredAtFormatted", ""),
                    lastSeenMillis = lastSeen,
                    lastSeenFormatted = obj.optString("lastSeenFormatted", ""),
                    isOnline = isOnline,
                    email = obj.optString("email", ""),
                    deviceModel = obj.optString("deviceModel", "Android")
                )
            )
        }
        return list
    }

    /**
     * Загружает бойцов из всех подразделений Firebase Firestore
     */
    suspend fun fetchFightersFromCloud() = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("fighters").get().await()
            val now = System.currentTimeMillis()
            val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            val fifteenMinutes = 15 * 60 * 1000L

            val cloudList = mutableListOf<FighterAdminRecord>()
            for (doc in snapshot.documents) {
                val id = doc.id
                val callsign = doc.getString("callsign") ?: "Боец"
                val role = doc.getString("role") ?: "Старшина / Боец"
                val unitName = doc.getString("unitName") ?: "1-е Подразделение"
                val unitKey = doc.getString("unitKey") ?: "kapt_default"
                val licenseKey = doc.getString("licenseKey") ?: ""
                val expiresAt = doc.getLong("expiresAt") ?: 0L
                val isProActive = expiresAt > now
                val daysLeft = if (isProActive) ((expiresAt - now) / 86400000L).toInt().coerceAtLeast(1) else 0
                val regAt = doc.getLong("registeredAt") ?: (now - 86400000L)
                val lastSeen = doc.getLong("lastSeenAt") ?: now
                val isOnline = (now - lastSeen) < fifteenMinutes
                val email = doc.getString("email") ?: ""
                val deviceModel = doc.getString("deviceModel") ?: "Android"

                cloudList.add(
                    FighterAdminRecord(
                        id = id,
                        callsign = callsign,
                        role = role,
                        unitName = unitName,
                        unitKey = unitKey,
                        licenseKey = licenseKey,
                        isProActive = isProActive,
                        licenseDaysLeft = daysLeft,
                        licenseExpiresFormatted = if (expiresAt > 0) sdf.format(Date(expiresAt)) else "Нет",
                        registeredAtMillis = regAt,
                        registeredAtFormatted = sdf.format(Date(regAt)),
                        lastSeenMillis = lastSeen,
                        lastSeenFormatted = if (isOnline) "В сети" else sdf.format(Date(lastSeen)),
                        isOnline = isOnline,
                        email = email,
                        deviceModel = deviceModel
                    )
                )
            }

            if (cloudList.isNotEmpty()) {
                // Объединяем с локальным списком (приоритет облака)
                val currentLocal = _fighters.value
                val merged = (cloudList + currentLocal).distinctBy { it.id }
                _fighters.value = merged
                saveFightersToCache(merged)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch fighters from Firestore, using cached", e)
        }
    }

    /**
     * Регистрирует или обновляет данные бойца во всех реестрах
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
        val daysLeft = if (expiresAt > now) ((expiresAt - now) / 86400000L).toInt().coerceAtLeast(1) else (if (isProActive) 30 else 0)

        val updatedRecord = FighterAdminRecord(
            id = fighterId,
            callsign = callsign.ifEmpty { "Боец" },
            role = role,
            unitName = unitName.ifEmpty { "1-е Подразделение" },
            unitKey = unitKey.ifEmpty { "kapt_default" },
            licenseKey = licenseKey,
            isProActive = isProActive || expiresAt > now,
            licenseDaysLeft = daysLeft,
            licenseExpiresFormatted = if (expiresAt > 0) sdf.format(Date(expiresAt)) else "30 суток",
            registeredAtMillis = now,
            registeredAtFormatted = sdf.format(Date(now)),
            lastSeenMillis = now,
            lastSeenFormatted = "В сети",
            isOnline = true,
            email = email,
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
        )

        val current = _fighters.value.toMutableList()
        val idx = current.indexOfFirst { it.id == fighterId || it.callsign.equals(callsign, ignoreCase = true) }
        if (idx >= 0) {
            val existing = current[idx]
            current[idx] = updatedRecord.copy(
                registeredAtMillis = existing.registeredAtMillis,
                registeredAtFormatted = existing.registeredAtFormatted,
                licenseKey = if (licenseKey.isNotEmpty()) licenseKey else existing.licenseKey
            )
        } else {
            current.add(0, updatedRecord)
        }

        _fighters.value = current
        saveFightersToCache(current)

        // Асинхронно пушим в Firestore
        scope.launch(Dispatchers.IO) {
            try {
                val data = hashMapOf(
                    "fighterId" to fighterId,
                    "callsign" to callsign,
                    "role" to role,
                    "unitName" to unitName,
                    "unitKey" to unitKey,
                    "licenseKey" to licenseKey,
                    "expiresAt" to expiresAt,
                    "registeredAt" to updatedRecord.registeredAtMillis,
                    "lastSeenAt" to now,
                    "email" to email,
                    "deviceModel" to updatedRecord.deviceModel
                )
                firestore.collection("fighters").document(fighterId)
                    .set(data, SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                Log.w(TAG, "Could not upload fighter to Firestore", e)
            }
        }
    }

    /**
     * Удаляет бойца из всех подразделений
     */
    fun deleteFighter(fighterId: String) {
        val current = _fighters.value.toMutableList()
        current.removeAll { it.id == fighterId }
        _fighters.value = current
        saveFightersToCache(current)

        scope.launch(Dispatchers.IO) {
            try {
                firestore.collection("fighters").document(fighterId).delete().await()
            } catch (e: Exception) {
                Log.w(TAG, "Error deleting fighter from Firestore", e)
            }
        }
    }

    /**
     * Выдает бойцу новую лицензию на 30 дней прямо из панели разработчика
     */
    fun grantLicense(fighterId: String, days: Int = 30): String {
        val chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
        fun part(): String = (1..4).map { chars.random() }.joinToString("")
        val newKey = "KAPT-${part()}-${part()}-${part()}"
        val now = System.currentTimeMillis()
        val expiresAt = now + (days.toLong() * 86400000L)
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

        val current = _fighters.value.toMutableList()
        val idx = current.indexOfFirst { it.id == fighterId }
        if (idx >= 0) {
            val old = current[idx]
            current[idx] = old.copy(
                licenseKey = newKey,
                isProActive = true,
                licenseDaysLeft = days,
                licenseExpiresFormatted = sdf.format(Date(expiresAt))
            )
            _fighters.value = current
            saveFightersToCache(current)
        }

        scope.launch(Dispatchers.IO) {
            try {
                val data = hashMapOf(
                    "licenseKey" to newKey,
                    "fighterId" to fighterId,
                    "expiresAt" to expiresAt,
                    "activatedAt" to now,
                    "durationDays" to days,
                    "status" to "ACTIVE"
                )
                firestore.collection("licenses").document(newKey).set(data).await()
                firestore.collection("fighters").document(fighterId)
                    .set(hashMapOf("licenseKey" to newKey, "expiresAt" to expiresAt), SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                Log.w(TAG, "Error granting license in Firestore", e)
            }
        }
        return newKey
    }

    suspend fun lookupFighter(query: String): FighterAdminRecord? {
        val q = query.trim().lowercase(Locale.ROOT)
        if (q.isBlank()) return null

        // 1. Поиск в локальном кэше
        val localMatch = _fighters.value.find {
            it.callsign.lowercase(Locale.ROOT) == q ||
            (it.email.isNotBlank() && it.email.lowercase(Locale.ROOT) == q) ||
            (it.unitKey.isNotBlank() && it.unitKey.lowercase(Locale.ROOT) == q)
        }
        if (localMatch != null) return localMatch

        // 2. Поиск в облаке Firestore
        return withContext(Dispatchers.IO) {
            try {
                val snap = firestore.collection("fighters").get().await()
                for (doc in snap.documents) {
                    val cs = doc.getString("callsign")?.lowercase(Locale.ROOT) ?: ""
                    val em = doc.getString("email")?.lowercase(Locale.ROOT) ?: ""
                    val uk = doc.getString("unitKey")?.lowercase(Locale.ROOT) ?: ""
                    if (cs == q || (em.isNotBlank() && em == q) || (uk.isNotBlank() && uk == q)) {
                        val exp = doc.getLong("expiresAt") ?: 0L
                        val daysLeft = if (exp > System.currentTimeMillis()) {
                            ((exp - System.currentTimeMillis()) / 86400000L).toInt()
                        } else 0
                        return@withContext FighterAdminRecord(
                            id = doc.getString("fighterId") ?: doc.id,
                            callsign = doc.getString("callsign") ?: "",
                            unitName = doc.getString("unitName") ?: "",
                            unitKey = doc.getString("unitKey") ?: "",
                            email = doc.getString("email") ?: "",
                            licenseKey = doc.getString("licenseKey") ?: "",
                            isProActive = doc.getBoolean("isProActive") ?: false,
                            licenseDaysLeft = daysLeft,
                            role = doc.getString("role") ?: "Старшина подразделения"
                        )
                    }
                }
                null
            } catch (e: Exception) {
                Log.w(TAG, "Error looking up fighter in cloud", e)
                null
            }
        }
    }
}
