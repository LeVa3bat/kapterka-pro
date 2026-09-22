package com.example.data.license

// Re-deploy trigger
import android.content.Context
import android.util.Log
import com.example.data.local.KapterkaDao
import com.example.data.model.PersonalLicense
import com.example.data.model.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
    val savedKeys: List<String> = emptyList(),
    val isDemoActive: Boolean = true,
    val demoDaysLeft: Int = 3
)

class LicenseManager(
    private val context: Context,
    private val dao: KapterkaDao,
    private val scope: CoroutineScope
) {
    private val TAG = "LicenseManager"
    private val backend = LicenseBackendService()
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
            // Existing installations keep their historical ID unchanged. New
            // installations use a much larger random namespace to make identity
            // pre-claim/guessing impractical.
            val randomPart = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .take(20)
                .uppercase(Locale.ROOT)
            id = "БОЕЦ-$randomPart"
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

    fun saveUnitKeyToVault(unitKey: String) {
        if (unitKey.isNotBlank()) {
            val vault = context.getSharedPreferences(PERMANENT_VAULT, Context.MODE_PRIVATE)
            vault.edit().putString("vault_unit_key", unitKey.trim()).apply()
        }
    }

    fun getSavedUnitKeyFromVault(): String? {
        val vault = context.getSharedPreferences(PERMANENT_VAULT, Context.MODE_PRIVATE)
        val key = vault.getString("vault_unit_key", null)
        return if (!key.isNullOrBlank()) key else null
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
        if (parts[0] == "KPT") {
            return true // Bypass strict checksum for external KPT- keys
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

        // Расчет срока демо-режима (72 часа / 3 дня с момента первого запуска)
        val demoStartTime = sp.getLong("demo_first_launch_time", 0L).let {
            if (it <= 0L) {
                sp.edit().putLong("demo_first_launch_time", now).apply()
                now
            } else it
        }
        val demoElapsed = now - demoStartTime
        val demoTotalMillis = 3L * 24L * 60L * 60L * 1000L // 3 суток
        val demoRemainingMillis = (demoTotalMillis - demoElapsed).coerceAtLeast(0L)
        val isDemoActive = demoRemainingMillis > 0L
        val calculatedDemoDays = if (isDemoActive) {
            ((demoRemainingMillis / (24L * 60L * 60L * 1000L)) + 1).toInt().coerceIn(1, 3)
        } else {
            0
        }

        // Обновляем количество демо-дней в локальном профиле БД
        scope.launch(Dispatchers.IO) {
            try {
                val currentProf = dao.getUserProfile().first()
                if (currentProf != null && currentProf.demoDaysLeft != calculatedDemoDays) {
                    dao.saveUserProfile(currentProf.copy(demoDaysLeft = calculatedDemoDays))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing demo days to user profile", e)
            }
        }

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
                savedKeys = allSaved,
                isDemoActive = false,
                demoDaysLeft = 0
            )
        } else {
            // Лицензия истекла или не активирована, но показываем сохраненный ключ бойца
            _licenseStatus.value = FighterLicenseStatus(
                licenseKey = "",
                isProActive = false,
                daysRemaining = 0,
                expiresAtDateFormatted = if (expiresAt > 0) "Истекла" else "Не активирована",
                fighterId = fighterId,
                activationSource = if (isDemoActive) "Демо-период ($calculatedDemoDays дн.)" else "Демо-режим истёк",
                lastSavedKey = lastSaved,
                savedKeys = allSaved,
                isDemoActive = isDemoActive,
                demoDaysLeft = calculatedDemoDays
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

        if (vaultExpires <= now) {
            refreshLicenseStatus()
            return Pair(false, "Сохранённая лицензия истекла и не может быть продлена восстановлением.")
        }

        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit()
            .putString("active_license_key", vaultKey)
            .putLong("license_expires_at", vaultExpires)
            .apply()
        refreshLicenseStatus()
        return Pair(true, "Действующая лицензия бойца восстановлена: $vaultKey")
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
        @Suppress("UNUSED_VARIABLE")
        val compatibilityArgs = Pair(callsign, unitKey)

        if (cleanEmail.isBlank()) {
            return@withContext Pair(false, "Укажите Email, который использовался при оплате.")
        }

        val result = backend.restoreByEmail(cleanEmail, getFighterPersonalId())
        if (!result.success) {
            return@withContext Pair(false, result.errorMessage)
        }

        val now = System.currentTimeMillis()
        if (result.licenseKey.isBlank() || result.expiresAt <= now) {
            return@withContext Pair(false, "Сервер не вернул действующую лицензию.")
        }

        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit()
            .putString("active_license_key", result.licenseKey)
            .putLong("license_expires_at", result.expiresAt)
            .apply()
        saveToPermanentVault(result.licenseKey, result.expiresAt)

        val daysLeft = ((result.expiresAt - now) / (1000L * 60 * 60 * 24))
            .toInt()
            .coerceAtLeast(1)
        updateRoomProfilePro(daysLeft)
        refreshLicenseStatus()

        Pair(
            true,
            "Лицензия восстановлена сервером: ${result.licenseKey} (ещё $daysLeft дн.)"
        )
    }

    /**
     * Сброс / отзыв лицензии при отмене оплаты или по запросу пользователя/разработчика
     */
    fun resetLicense() {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Revoke only entitlement state. The stable fighter identity and original
        // demo start timestamp must survive so logout/reset cannot create a new
        // identity or restart the trial period.
        sp.edit()
            .remove("active_license_key")
            .remove("license_expires_at")
            .remove("license_activated_at")
            .remove("license_payment_id")
            .apply()

        val vault = context.getSharedPreferences(PERMANENT_VAULT, Context.MODE_PRIVATE)
        vault.edit()
            .remove("vault_active_key")
            .remove("vault_expires_at")
            .apply()

        scope.launch(Dispatchers.IO) {
            try {
                val profile = dao.getUserProfile().first()
                if (profile != null) {
                    dao.saveUserProfile(
                        profile.copy(
                            isProActive = false,
                            proDaysLeft = 0
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
    @Deprecated("Новая лицензия должна выдаваться только сервером оплаты")
    suspend fun activateLicenseAfterPayment(
        fighterCallsign: String,
        fighterEmail: String,
        paymentId: String
    ): String = withContext(Dispatchers.IO) {
        @Suppress("UNUSED_VARIABLE")
        val legacyArgs = Triple(fighterCallsign, fighterEmail, paymentId)
        Log.w(TAG, "Blocked legacy client-side license issuance")
        ""
    }

    /**
     * Сохраняет лицензию, которая уже подтверждена и выдана сервером.
     * Этот метод никогда не генерирует новый срок или ключ локально.
     */
    suspend fun activateServerVerifiedLicense(
        licenseKey: String,
        expiresAt: Long,
        paymentId: String
    ): Boolean = withContext(Dispatchers.IO) {
        val cleanKey = licenseKey.uppercase(Locale.ROOT)
            .replace(Regex("[^A-Z0-9-]"), "")
        val now = System.currentTimeMillis()

        if (!verifyKeyChecksum(cleanKey)) {
            Log.e(TAG, "Server returned malformed license key")
            return@withContext false
        }
        if (expiresAt <= now) {
            Log.e(TAG, "Server returned expired license")
            return@withContext false
        }
        if (paymentId.isBlank()) {
            Log.e(TAG, "Server verified license without payment id")
            return@withContext false
        }

        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit()
            .putString("active_license_key", cleanKey)
            .putLong("license_expires_at", expiresAt)
            .putLong("license_activated_at", now)
            .putString("license_payment_id", paymentId)
            .apply()

        saveToPermanentVault(cleanKey, expiresAt)

        val daysLeft = ((expiresAt - now) / (1000L * 60L * 60L * 24L))
            .toInt()
            .coerceAtLeast(1)
        updateRoomProfilePro(daysLeft)
        refreshLicenseStatus()
        true
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
    suspend fun activateKeyManually(
        enteredKey: String,
        fighterCallsign: String
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val cleanKey = enteredKey.uppercase(Locale.ROOT)
            .replace(Regex("[^A-Z0-9-]"), "")

        if (cleanKey.length < 12 ||
            (!cleanKey.startsWith("KAPT-") && !cleanKey.startsWith("KPT-"))
        ) {
            return@withContext Pair(
                false,
                "Неверный формат ключа. Формат: KAPT-XXXX-XXXX-ZZZZ или KPT-XXXX-XXXX-ZZZZ"
            )
        }

        @Suppress("UNUSED_VARIABLE")
        val compatibilityCallsign = fighterCallsign
        val now = System.currentTimeMillis()
        val currentFighterId = getFighterPersonalId()
        val result = backend.verifyKey(cleanKey, currentFighterId)

        if (result.success) {
            val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            sp.edit()
                .putString("active_license_key", result.licenseKey)
                .putLong("license_expires_at", result.expiresAt)
                .apply()

            saveToPermanentVault(result.licenseKey, result.expiresAt)
            val daysLeft = ((result.expiresAt - now) / (1000L * 60 * 60 * 24))
                .toInt()
                .coerceAtLeast(1)
            updateRoomProfilePro(daysLeft)
            refreshLicenseStatus()
            return@withContext Pair(
                true,
                "Лицензия подтверждена сервером и активирована на $daysLeft дн."
            )
        }

        val vault = context.getSharedPreferences(PERMANENT_VAULT, Context.MODE_PRIVATE)
        val vaultKey = vault.getString("vault_active_key", "") ?: ""
        val vaultExpires = vault.getLong("vault_expires_at", 0L)

        if (vaultKey.equals(cleanKey, ignoreCase = true) && vaultExpires > now) {
            val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            sp.edit()
                .putString("active_license_key", vaultKey)
                .putLong("license_expires_at", vaultExpires)
                .apply()

            val daysLeft = ((vaultExpires - now) / (1000L * 60 * 60 * 24))
                .toInt()
                .coerceAtLeast(1)
            updateRoomProfilePro(daysLeft)
            refreshLicenseStatus()
            return@withContext Pair(
                true,
                "Сохранённая ранее лицензия восстановлена офлайн до её текущего срока."
            )
        }

        Pair(
            false,
            result.errorMessage.ifBlank {
                "Для первой активации нужен интернет и подтверждение сервера."
            }
        )
    }
}
