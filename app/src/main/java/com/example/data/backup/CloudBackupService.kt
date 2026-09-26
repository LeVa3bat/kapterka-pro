package com.example.data.backup

import android.util.Base64
import com.example.BuildConfig
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

data class CloudBackup(val createdAt: Long, val json: String)

/**
 * Облачная копия склада. Лежит в поле `cloudBackup` документа подразделения
 * `units/{ключ}` (сжатый gzip + base64), поэтому правила Firestore не меняются, а старые
 * версии приложения лишнее поле просто не читают. Хранится одна последняя копия.
 */
class CloudBackupService {
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private fun unitRef(unitKey: String) = firestore.collection("units").document(unitKey)

    suspend fun upload(unitKey: String, json: String, createdAt: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            check(!BuildConfig.IS_NEXT_SAFE_TEST) { "В тестовой сборке облако отключено" }
            require(unitKey.isNotBlank()) { "Не указан код подразделения" }
            val packed = gzipBase64(json)
            check(packed.length <= MAX_PACKED_CHARS) { "Копия слишком большая для облака. Сохраните её в файл" }
            withTimeout(30_000) {
                unitRef(unitKey).set(
                    mapOf("cloudBackup" to mapOf("createdAt" to createdAt, "data" to packed)),
                    SetOptions.merge()
                ).await()
            }
            Unit
        }
    }

    /** null, если облачной копии ещё нет. */
    suspend fun download(unitKey: String): Result<CloudBackup?> = withContext(Dispatchers.IO) {
        runCatching {
            check(!BuildConfig.IS_NEXT_SAFE_TEST) { "В тестовой сборке облако отключено" }
            require(unitKey.isNotBlank()) { "Не указан код подразделения" }
            val snap = withTimeout(30_000) { unitRef(unitKey).get(Source.SERVER).await() }
            @Suppress("UNCHECKED_CAST")
            val map = snap.get("cloudBackup") as? Map<String, Any?> ?: return@runCatching null
            val data = map["data"] as? String ?: return@runCatching null
            CloudBackup((map["createdAt"] as? Number)?.toLong() ?: 0L, gunzipBase64(data))
        }
    }

    companion object {
        // Документ Firestore не больше 1 МБ; оставляем запас на служебные поля.
        const val MAX_PACKED_CHARS = 900_000

        fun gzipBase64(text: String): String {
            val out = ByteArrayOutputStream()
            GZIPOutputStream(out).use { it.write(text.toByteArray(Charsets.UTF_8)) }
            return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        }

        fun gunzipBase64(packed: String): String {
            val bytes = Base64.decode(packed, Base64.NO_WRAP)
            return GZIPInputStream(ByteArrayInputStream(bytes)).use { String(it.readBytes(), Charsets.UTF_8) }
        }
    }
}
