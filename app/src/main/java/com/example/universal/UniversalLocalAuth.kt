package com.example.universal

import android.content.Context
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class UniversalLocalAuth(context: Context) {
    private val prefs = context.getSharedPreferences("sklad_pro_local_auth", Context.MODE_PRIVATE)

    fun hasAccount(): Boolean =
        !prefs.getString("email", null).isNullOrBlank() &&
        !prefs.getString("password_hash", null).isNullOrBlank() &&
        !prefs.getString("password_salt", null).isNullOrBlank()

    fun registeredEmail(): String = prefs.getString("email", "").orEmpty()

    fun register(email: String, password: String): String? {
        val normalized = email.trim().lowercase()
        if (normalized.isBlank() || !normalized.contains("@")) return "Укажите корректный email"
        if (password.length < 8) return "Пароль должен содержать минимум 8 символов"

        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = derive(password, salt)
        prefs.edit()
            .putString("email", normalized)
            .putString("password_salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString("password_hash", Base64.encodeToString(hash, Base64.NO_WRAP))
            .apply()
        return null
    }

    fun verify(email: String, password: String): Boolean {
        val normalized = email.trim().lowercase()
        if (normalized != registeredEmail()) return false

        val saltRaw = prefs.getString("password_salt", null) ?: return false
        val expectedRaw = prefs.getString("password_hash", null) ?: return false
        return try {
            val salt = Base64.decode(saltRaw, Base64.NO_WRAP)
            val expected = Base64.decode(expectedRaw, Base64.NO_WRAP)
            val actual = derive(password, salt)
            java.security.MessageDigest.isEqual(expected, actual)
        } catch (_: Throwable) {
            false
        }
    }

    private fun derive(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, 120_000, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(spec)
            .encoded
    }
}
