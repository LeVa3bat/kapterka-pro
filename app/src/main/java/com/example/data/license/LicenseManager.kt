package com.example.data.license

import android.content.Context
import android.util.Log
import com.example.data.local.KapterkaDao
import com.example.data.model.PersonalLicense
import com.example.data.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class FighterLicenseStatus(
    val licenseKey: String = "",
    val isProActive: Boolean = false,
    val daysRemaining: Int = 0,
    val expiresAtDateFormatted: String = "",
    val fighterId: String = "",
    val activationSource: String = "Демо-период",
    val lastSavedKey: String = "",
    val savedKeys: List<String> = emptyList()
)

class LicenseManager(
    private val context: Context,
    private val dao: KapterkaDao,
    private val scope: CoroutineScope
) {
    private val TAG = "LicenseManager"
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val PREFS_NAME = "kapterka_fighter_license_prefs"
    private val PERMANENT_VAULT = "kapterka_license_permanent_vault"

    private val _licenseStatus = MutableStateFlow(FighterLicenseStatus())
    val licenseStatus: StateFlow<FighterLicenseStatus> = _licenseStatus.asStateFlow()

    init {
        refreshLicenseStatus()
    }

    fun getFighterPersonalId(): String {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var id = sp.getString("fighter_personal_id", null)
        if (id == null) {
            val randomDigits = (1000..9999).random()
            val randomChars = UUID.randomUUID().toString().take(4).uppercase(Locale.ROOT)
            id = "БОЕЦ-$randomDigits-$randomChars"
            sp.edit().putString("fighter_personal_id", id).apply()
        }
        return id
    }

    private fun saveToPermanentVault(key: String, expiresAt: Long) {
        val vault = context.getSharedPreferences(PERMANENT_VAULT, Context.MODE_PRIVATE)
        val historySet = vault.getStringSet("vault_keys_history", emptySet())?.toMutableSet() ?: mutableSetOf()
        if (key.isNotBlank()) {
            historySet.add(key)
        }
        vault.edit()
            .putString("vault_active_key", key)
            .putLong("vault_expires_at", expiresAt)
            .putStringSet("vault_keys_history", historySet)
            .apply()
    }

    fun getAllSavedKeys(): List<String> {
        val vault = context.getSharedPreferences(PERMANENT_VAULT, Context.MODE_PRIVATE)
        val historySet = vault.getStringSet("vault_keys_history", emptySet()) ?: emptySet()
        val currentKey = vault.getString("vault_active_key", "") ?: ""
        val list = historySet.toMutableList()
        if (currentKey.isNotBlank() && !list.contains(currentKey)) {
            list.add(0, currentKey)
        }
        return list.filter { it.isNotBlank() }
    }

    /**
     * Вычисляет криптографическую контрольную сумму для 4-го сегмента ключа
     */
    fun computeKeyChecksum(p1: String, p2: String): String {
        val chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
        val s = "KAPT-$p1-$p2-KAPT3RKA_881_MILITARY"
        var h1 = 0x811c9dc5L
        var h2 = 0x5a2d1e39L
        for (ch in s) {
            val code = ch.code.toLong()
            h1 = ((h1 xor code) * 0x01000193L) and 0xFFFFFFFFL
            h2 = (((h2 + code) * 31L) + 0x45L) and 0xFFFFFFFFL
        }
        val c0 = chars[((h1 ushr 24) and 0x1FL).toInt()]
        val c1 = chars[((h1 ushr 16) and 0x1FL).toInt()]
        val c2 = chars[((h2 ushr 24) and 0x1FL).toInt()]
        val c3 = chars[((h2 ushr 16) and 0x1FL).toInt()]
        return "$c0$c1$c2$c3"
    }

    /**
     * Проверяет математическую и криптографическую подлинность ключа
     */
    fun verifyKeyChecksum(key: String): Boolean {
        val clean = key.uppercase(Locale.ROOT)
            .replace(Regex("[^A-Z0-9-]"), "")
        val parts = clean.split("-")
        if (parts.size != 4 || (parts[0] != "KAPT" && parts[0] != "KPT") || parts[1].length != 4 || parts[2].length != 4 || parts[3].length != 4) {
            return false
        }
        val expected = computeKeyChecksum(parts[1], parts[2])
        return parts[3] == expected
    }

    /**
     * Генерирует уникальный 16-значный персональный военный ключ лицензии с криптографической подписью:
     * Формат: KAPT-XXXX-XXXX-ZZZZ (где ZZZZ - верификационная контрольная сумма)
     */
    fun generateLicenseKey(): String {
        val chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ" // без похожих 0/O, 1/I
        val random = SecureRandom()
        fun part(): String = (1..4).map { chars[random.nextInt(chars.length)] }.joinToString("")
        val p1 = part()
        val p2 = part()
        val checksum = computeKeyChecksum(p1, p2)
        return "KAPT-$p1-$p2-$checksum"
    }

    /**
     * Обновляет локальный статус лицензии бойца с авто-восстановлением из вечного сейфа
     */
    fun refreshLicenseStatus() {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val vault = context.getSharedPreferences(PERMANENT_VAULT, Context.MODE_PRIVATE)
        
        var key = sp.getString("active_license_key", "") ?: ""
        var expiresAt = sp.getLong("license_expires_at", 0L)
        val now = System.currentTimeMillis()
        val fighterId = getFighterPersonalId()

        // Если в основных prefs ключ пропал, пробуем восстановить из вечного сейфа
        if (key.isBlank() || expiresAt <= now) {
            val vaultKey = vault.getString("vault_active_key", "") ?: ""
            val vaultExpires = vault.getLong("vault_expires_at", 0L)
            if (vaultKey.isNotBlank() && vaultExpires > now) {
                key = vaultKey
                expiresAt = vaultExpires
                sp.edit()
                    .putString("active_license_key", key)
                    .putLong("license_expires_at", expiresAt)
                    .apply()
            }
        }

        val lastSaved = vault.getString("vault_active_key", "") ?: key
        val allSaved = getAllSavedKeys()

        if (expiresAt > now && key.isNotBlank()) {
            val days = ((expiresAt - now) / (1000L * 60 * 60 * 24)).toInt().coerceAtLeast(1)
            val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            _licenseStatus.value = FighterLicenseStatus(
                licenseKey = key,
                isProActive = true,
                daysRemaining = days,
                expiresAtDateFormatted = sdf.format(Date(expiresAt)),
                fighterId = fighterId,
                activationSource = "Персональная лицензия ПРО (30 дн.)",
                lastSavedKey = key,
                savedKeys = allSaved
            )
        } else {
            // Лицензия истекла или не активирована, но показываем сохраненный ключ бойца
            _licenseStatus.value = FighterLicenseStatus(
                licenseKey = "",
                isProActive = false,
                daysRemaining = 0,
                expiresAtDateFormatted = if (expiresAt > 0) "Истекла" else "Не активирована",
                fighterId = fighterId,
                activationSource = "Базовый доступ",
                lastSavedKey = lastSaved,
                savedKeys = allSaved
            )
        }
    }

    /**
     * Восстанавливает ранее сохраненный ключ бойца из вечного хранилища устройства
     */
    fun restoreSavedLicense(): Pair<Boolean, String> {
        val vault = context.getSharedPreferences(PERMANENT_VAULT, Context.MODE_PRIVATE)
        val vaultKey = vault.getString("vault_active_key", "") ?: ""
        val vaultExpires = vault.getLong("vault_expires_at", 0L)
        val now = System.currentTimeMillis()

        if (vaultKey.isBlank()) {
            return Pair(false, "На этом устройстве нет ранее сохраненных лицензионных ключей.")
        }

        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val finalExpires = if (vaultExpires > now) vaultExpires else (now + 30L * 86400000L)
        sp.edit()
            .putString("active_license_key", vaultKey)
            .putLong("license_expires_at", finalExpires)
            .apply()
        saveToPermanentVault(vaultKey, finalExpires)
        refreshLicenseStatus()
        return Pair(true, "Лицензия бойца успешно восстановлена: $vaultKey")
    }

    /**
     * Запрашивает облачную базу Google Firebase для восстановления оплаченной лицензии
     * по Email, позывному, номеру подразделения или аккаунту покупателя
     */
    suspend fun restoreLicenseFromCloud(
        email: String,
        callsign: String,
        unitKey: String = ""
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase(Locale.ROOT)
        val cleanCallsign = callsign.trim()
        val currentFighterId = getFighterPersonalId()
        val now = System.currentTimeMillis()

        var foundKey = ""
        var foundExpiresAt = 0L

        // 1. Поиск в реестре лицензий Firestore по email
        if (cleanEmail.isNotBlank()) {
            try {
                val byEmail = firestore.collection("licenses")
                    .whereEqualTo("email", cleanEmail)
                    .get()
                    .await()
                for (doc in byEmail.documents) {
                    val key = doc.getString("licenseKey") ?: doc.id
                    val exp = doc.getLong("expiresAt") ?: 0L
                    val status = doc.getString("status") ?: "ACTIVE"
                    if (key.isNotBlank() && status == "ACTIVE") {
                        foundKey = key
                        foundExpiresAt = exp
                        break
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed searching licenses by email", e)
            }
        }

        // 2. Поиск по оригинальному регистру email
        if (foundKey.isBlank() && email.isNotBlank() && email.trim() != cleanEmail) {
            try {
                val byEmailOrig = firestore.collection("licenses")
                    .whereEqualTo("email", email.trim())
                    .get()
                    .await()
                for (doc in byEmailOrig.documents) {
                    val key = doc.getString("licenseKey") ?: doc.id
                    val exp = doc.getLong("expiresAt") ?: 0L
                    val status = doc.getString("status") ?: "ACTIVE"
                    if (key.isNotBlank() && status == "ACTIVE") {
                        foundKey = key
                        foundExpiresAt = exp
                        break
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed searching licenses by orig email", e)
            }
        }

        // 3. Поиск по позывному
        if (foundKey.isBlank() && cleanCallsign.isNotBlank()) {
            try {
                val byCallsign = firestore.collection("licenses")
                    .whereEqualTo("callsign", cleanCallsign)
                    .get()
                    .await()
                for (doc in byCallsign.documents) {
                    val key = doc.getString("licenseKey") ?: doc.id
                    val exp = doc.getLong("expiresAt") ?: 0L
                    val status = doc.getString("status") ?: "ACTIVE"
                    if (key.isNotBlank() && status == "ACTIVE") {
                        foundKey = key
                        foundExpiresAt = exp
                        break
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed searching licenses by callsign", e)
            }
        }

        // 4. Поиск в общем реестре бойцов 'fighters_registry'
        if (foundKey.isBlank() && cleanEmail.isNotBlank()) {
            try {
                val regDoc = firestore.collection("fighters_registry")
                    .whereEqualTo("email", cleanEmail)
                    .get()
                    .await()
                for (doc in regDoc.documents) {
                    val key = doc.getString("licenseKey") ?: ""
                    val isPro = doc.getBoolean("isProActive") ?: false
                    if (key.isNotBlank() && isPro) {
                        foundKey = key
                        foundExpiresAt = now + 30L * 86400000L
                        break
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed searching fighters_registry", e)
            }
        }

        // 5. Применение найденного в облаке ключа
        if (foundKey.isNotBlank()) {
            val finalExpires = if (foundExpiresAt > now) foundExpiresAt else (now + 30L * 86400000L)
            val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            sp.edit()
                .putString("active_license_key", foundKey)
                .putLong("license_expires_at", finalExpires)
                .apply()
            saveToPermanentVault(foundKey, finalExpires)

            val daysLeft = ((finalExpires - now) / (1000L * 60 * 60 * 24)).toInt().coerceAtLeast(1)
            updateRoomProfilePro(daysLeft)
            refreshLicenseStatus()

            Pair(true, "Лицензия бойца успешно восстановлена из базы! Ключ: $foundKey (на $daysLeft дн.)")
        } else {
            // Пробуем сейф устройства
            val (vaultSuccess, vaultMsg) = restoreSavedLicense()
            if (vaultSuccess) {
                Pair(true, "Лицензия восстановлена из сейфа устройства: $vaultMsg")
            } else {
                Pair(false, "Оплаченная лицензия для «$cleanEmail» не найдена в реестре. Проверьте правильность email или введите ключ вручную.")
            }
        }
    }

    /**
     * Сброс / отзыв лицензии при отмене оплаты или по запросу пользователя/разработчика
     */
    fun resetLicense() {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit().clear().apply()
        val vault = context.getSharedPreferences(PERMANENT_VAULT, Context.MODE_PRIVATE)
        vault.edit().remove("vault_active_key").remove("vault_expires_at").apply()
        scope.launch(Dispatchers.IO) {
            try {
                val profile = dao.getUserProfile().first()
                if (profile != null) {
                    dao.saveUserProfile(
                        profile.copy(
                            isProActive = false,
                            proDaysLeft = 0,
                            demoDaysLeft = 3
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error resetting profile", e)
            }
            refreshLicenseStatus()
        }
    }

    /**
     * Активирует 30-дневную персональную лицензию после успешной оплаты в ЮKassa
     */
    suspend fun activateLicenseAfterPayment(
        fighterCallsign: String,
        fighterEmail: String,
        paymentId: String
    ): String = withContext(Dispatchers.IO) {
        val newKey = generateLicenseKey()
        val now = System.currentTimeMillis()
        val durationMillis = 30L * 24L * 60L * 60L * 1000L // ровно 30 дней
        val expiresAt = now + durationMillis
        val fighterId = getFighterPersonalId()

        // 1. Сохраняем локально на устройстве бойца
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit()
            .putString("active_license_key", newKey)
            .putLong("license_expires_at", expiresAt)
            .putLong("license_activated_at", now)
            .putString("license_payment_id", paymentId)
            .apply()

        saveToPermanentVault(newKey, expiresAt)

        // 2. Обновляем статус в профиле бойца Room
        try {
            val profile = dao.getUserProfile().first()
            if (profile != null) {
                dao.saveUserProfile(
                    profile.copy(
                        isProActive = true,
                        proDaysLeft = 30,
                        demoDaysLeft = 0
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating profile with new license", e)
        }

        // 3. Записываем в общую защищенную коллекцию Firebase Firestore
        try {
            val licenseData = hashMapOf(
                "licenseKey" to newKey,
                "fighterId" to fighterId,
                "callsign" to fighterCallsign,
                "email" to fighterEmail,
                "paymentId" to paymentId,
                "activatedAt" to now,
                "expiresAt" to expiresAt,
                "durationDays" to 30,
                "status" to "ACTIVE"
            )
            firestore.collection("licenses").document(newKey)
                .set(licenseData, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to upload license to Firestore immediately, saved locally", e)
        }

        refreshLicenseStatus()
        newKey
    }

    private suspend fun updateRoomProfilePro(days: Int) {
        try {
            val profile = dao.getUserProfile().first()
            if (profile != null) {
                dao.saveUserProfile(
                    profile.copy(
                        isProActive = true,
                        proDaysLeft = days.coerceAtLeast(1),
                        demoDaysLeft = 0
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating Room profile with PRO status", e)
        }
    }

    /**
     * Ручная активация существующего ключа (если боец получил ключ с сайта или от командира)
     */
    suspend fun activateKeyManually(enteredKey: String, fighterCallsign: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val cleanKey = enteredKey.uppercase(Locale.ROOT)
            .replace(Regex("[^A-Z0-9-]"), "")

        if (cleanKey.length < 12 || (!cleanKey.startsWith("KAPT-") && !cleanKey.startsWith("KPT-"))) {
            return@withContext Pair(false, "Неверный формат ключа. Формат: KAPT-XXXX-XXXX-ZZZZ или KPT-XXXX-XXXX-ZZZZ")
        }

        val isLocalValid = verifyKeyChecksum(cleanKey)

        try {
            val doc = firestore.collection("licenses").document(cleanKey).get().await()
            if (doc.exists()) {
                val expiresAt = doc.getLong("expiresAt") ?: 0L
                val status = doc.getString("status") ?: "ACTIVE"
                val boundFighter = doc.getString("fighterId")

                val now = System.currentTimeMillis()
                if (expiresAt < now) {
                    return@withContext Pair(false, "Срок действия данного ключа уже истек.")
                }
                if (status != "ACTIVE") {
                    return@withContext Pair(false, "Данный ключ лицензии деактивирован.")
                }
                val currentFighterId = getFighterPersonalId()
                if (!boundFighter.isNullOrEmpty() && boundFighter != currentFighterId) {
                    return@withContext Pair(false, "Ключ уже привязан к другому бойцу ($boundFighter).")
                }

                // Успешно активируем
                val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                sp.edit()
                    .putString("active_license_key", cleanKey)
                    .putLong("license_expires_at", expiresAt)
                    .apply()
                saveToPermanentVault(cleanKey, expiresAt)

                val daysLeft = ((expiresAt - now) / (1000L * 60 * 60 * 24)).toInt().coerceAtLeast(1)
                updateRoomProfilePro(daysLeft)

                // Привязываем к текущему бойцу в Firestore
                try {
                    firestore.collection("licenses").document(cleanKey).set(
                        hashMapOf(
                            "fighterId" to currentFighterId,
                            "callsign" to fighterCallsign
                        ),
                        SetOptions.merge()
                    )
                } catch (_: Exception) {}

                refreshLicenseStatus()
                Pair(true, "Лицензия успешно активирована на $daysLeft дн.!")
            } else {
                if (!isLocalValid) {
                    return@withContext Pair(false, "❌ Недействительный ключ! Не найден в базе и не прошел проверку подлинности.")
                }
                // Ключ имеет верную подпись, но еще не зарегистрирован в облаке (выдан оффлайн)
                val now = System.currentTimeMillis()
                val expiresAt = now + (30L * 24L * 60L * 60L * 1000L)
                val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                sp.edit()
                    .putString("active_license_key", cleanKey)
                    .putLong("license_expires_at", expiresAt)
                    .apply()
                saveToPermanentVault(cleanKey, expiresAt)
                updateRoomProfilePro(30)
                refreshLicenseStatus()

                // Фоновая регистрация ключа в облаке
                try {
                    val currentFighterId = getFighterPersonalId()
                    val licenseData = hashMapOf(
                        "licenseKey" to cleanKey,
                        "fighterId" to currentFighterId,
                        "callsign" to fighterCallsign,
                        "activatedAt" to now,
                        "expiresAt" to expiresAt,
                        "durationDays" to 30,
                        "status" to "ACTIVE",
                        "source" to "Активация проверенного военного ключа"
                    )
                    firestore.collection("licenses").document(cleanKey)
                        .set(licenseData, SetOptions.merge())
                } catch (_: Exception) {}

                Pair(true, "Ключ успешно активирован! Доступ открыт на 30 дней.")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Network exception verifying key in Firestore, falling back to offline validation", e)
            if (!isLocalValid) {
                return@withContext Pair(false, "❌ Недействительный ключ (отсутствует сеть для проверки в базе)!")
            }
            val now = System.currentTimeMillis()
            val expiresAt = now + (30L * 24L * 60L * 60L * 1000L)
            val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            sp.edit()
                .putString("active_license_key", cleanKey)
                .putLong("license_expires_at", expiresAt)
                .apply()
            saveToPermanentVault(cleanKey, expiresAt)
            updateRoomProfilePro(30)
            refreshLicenseStatus()
            Pair(true, "Ключ подтвержден цифровой подписью в оффлайн-режиме (активен 30 дней)")
        }
    }
}
