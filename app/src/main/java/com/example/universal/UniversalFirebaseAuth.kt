package com.example.universal

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest

sealed interface UniversalAuthResult {
    data class Authenticated(
        val email: String,
        val displayName: String
    ) : UniversalAuthResult

    data class VerificationRequired(
        val email: String,
        val message: String
    ) : UniversalAuthResult

    data class Error(val message: String) : UniversalAuthResult
}

class UniversalFirebaseAuth(context: Context) {
    private val auth = FirebaseAuth.getInstance()
    private val prefs = context.getSharedPreferences("sklad_pro_firebase_auth", Context.MODE_PRIVATE)

    fun currentVerifiedAccount(): UniversalAuthResult.Authenticated? {
        val user = auth.currentUser ?: return null
        if (!user.isEmailVerified) return null
        return UniversalAuthResult.Authenticated(
            email = user.email.orEmpty().trim().lowercase(),
            displayName = user.displayName.orEmpty().trim()
        )
    }

    fun currentUnverifiedEmail(): String? {
        val user = auth.currentUser ?: return null
        return user.email?.takeIf { !user.isEmailVerified }
    }

    fun savedEmail(): String =
        auth.currentUser?.email
            ?: prefs.getString("last_email", "").orEmpty()

    fun register(
        name: String,
        email: String,
        password: String,
        callback: (UniversalAuthResult) -> Unit
    ) {
        val cleanName = name.trim()
        val normalizedEmail = email.trim().lowercase()

        if (cleanName.isBlank()) {
            callback(UniversalAuthResult.Error("Укажите ваше имя"))
            return
        }
        if (!isEmailValid(normalizedEmail)) {
            callback(UniversalAuthResult.Error("Укажите корректный email"))
            return
        }
        if (password.length < 8) {
            callback(UniversalAuthResult.Error("Пароль должен содержать минимум 8 символов"))
            return
        }

        auth.createUserWithEmailAndPassword(normalizedEmail, password)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    callback(UniversalAuthResult.Error(mapError(task.exception)))
                    return@addOnCompleteListener
                }

                val user = auth.currentUser
                if (user == null) {
                    callback(UniversalAuthResult.Error("Не удалось создать аккаунт. Попробуйте ещё раз."))
                    return@addOnCompleteListener
                }

                prefs.edit()
                    .putString("last_email", normalizedEmail)
                    .putString("pending_name", cleanName)
                    .apply()

                val profileRequest = UserProfileChangeRequest.Builder()
                    .setDisplayName(cleanName)
                    .build()

                user.updateProfile(profileRequest).addOnCompleteListener {
                    user.sendEmailVerification().addOnCompleteListener { verifyTask ->
                        if (verifyTask.isSuccessful) {
                            callback(
                                UniversalAuthResult.VerificationRequired(
                                    email = normalizedEmail,
                                    message = "Мы отправили письмо для подтверждения на $normalizedEmail"
                                )
                            )
                        } else {
                            callback(
                                UniversalAuthResult.VerificationRequired(
                                    email = normalizedEmail,
                                    message = "Аккаунт создан. Письмо не отправилось — нажмите «Отправить ещё раз»."
                                )
                            )
                        }
                    }
                }
            }
    }

    fun login(
        email: String,
        password: String,
        callback: (UniversalAuthResult) -> Unit
    ) {
        val normalizedEmail = email.trim().lowercase()
        if (!isEmailValid(normalizedEmail)) {
            callback(UniversalAuthResult.Error("Укажите корректный email"))
            return
        }
        if (password.isBlank()) {
            callback(UniversalAuthResult.Error("Введите пароль"))
            return
        }

        auth.signInWithEmailAndPassword(normalizedEmail, password)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    callback(UniversalAuthResult.Error(mapError(task.exception)))
                    return@addOnCompleteListener
                }

                prefs.edit().putString("last_email", normalizedEmail).apply()
                callback(resultForCurrentUser())
            }
    }

    fun refreshVerification(callback: (UniversalAuthResult) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            callback(UniversalAuthResult.Error("Сначала войдите в аккаунт"))
            return
        }

        user.reload().addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                callback(UniversalAuthResult.Error(mapError(task.exception)))
                return@addOnCompleteListener
            }
            callback(resultForCurrentUser())
        }
    }

    fun resendVerification(callback: (String?) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            callback("Сначала войдите в аккаунт")
            return
        }
        if (user.isEmailVerified) {
            callback(null)
            return
        }

        user.sendEmailVerification().addOnCompleteListener { task ->
            callback(if (task.isSuccessful) null else mapError(task.exception))
        }
    }

    fun sendPasswordReset(email: String, callback: (String?) -> Unit) {
        val normalizedEmail = email.trim().lowercase()
        if (!isEmailValid(normalizedEmail)) {
            callback("Укажите корректный email")
            return
        }

        auth.sendPasswordResetEmail(normalizedEmail).addOnCompleteListener { task ->
            callback(if (task.isSuccessful) null else mapError(task.exception))
        }
    }

    fun signOut() {
        auth.signOut()
    }

    private fun resultForCurrentUser(): UniversalAuthResult {
        val user = auth.currentUser
            ?: return UniversalAuthResult.Error("Не удалось получить данные аккаунта")

        val email = user.email.orEmpty().trim().lowercase()
        return if (user.isEmailVerified) {
            val savedName = prefs.getString("pending_name", "").orEmpty().trim()
            val name = user.displayName.orEmpty().trim().ifBlank { savedName }
            prefs.edit()
                .putString("last_email", email)
                .remove("pending_name")
                .apply()
            UniversalAuthResult.Authenticated(email = email, displayName = name)
        } else {
            UniversalAuthResult.VerificationRequired(
                email = email,
                message = "Подтвердите email по ссылке из письма и вернитесь в приложение."
            )
        }
    }

    private fun isEmailValid(email: String): Boolean =
        email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

    private fun mapError(error: Throwable?): String = when (error) {
        is FirebaseAuthUserCollisionException -> "Аккаунт с таким email уже существует"
        is FirebaseAuthWeakPasswordException -> "Пароль слишком простой"
        is FirebaseAuthInvalidUserException -> "Аккаунт не найден или отключён"
        is FirebaseAuthInvalidCredentialsException -> "Неверный email или пароль"
        is FirebaseAuthException -> when (error.errorCode) {
            "ERROR_TOO_MANY_REQUESTS" -> "Слишком много попыток. Попробуйте немного позже"
            "ERROR_NETWORK_REQUEST_FAILED" -> "Нет соединения с интернетом"
            else -> "Ошибка авторизации. Попробуйте ещё раз"
        }
        else -> "Ошибка соединения с Firebase. Попробуйте ещё раз"
    }
}
