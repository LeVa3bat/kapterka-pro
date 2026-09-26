package com.example.data.backup

import android.content.Context
import android.net.Uri
import com.example.BuildConfig
import com.example.data.local.KapterkaDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Резервная копия склада в файл: ручная (в выбранное место) и автоматическая раз в неделю
 * (в папку приложения). Восстановление заменяет складские данные целиком, но перед этим
 * сохраняет страховочную копию текущих.
 */
class BackupManager(private val context: Context, private val dao: KapterkaDao) {

    private val prefs = context.getSharedPreferences("kapterka_backup_prefs", Context.MODE_PRIVATE)

    val autoBackupDir: File get() = File(context.getExternalFilesDir(null) ?: context.filesDir, "backups")

    val lastAutoBackupAt: Long get() = prefs.getLong("last_auto_backup", 0L)

    suspend fun buildJson(now: Long = System.currentTimeMillis()): String {
        val snapshot = BackupSnapshot(
            items = dao.getAllItems().first(),
            points = dao.getAllPoints().first(),
            stocks = dao.getAllStockRecords().first(),
            operations = dao.getAllOperations().first(),
            requisitions = dao.getAllRequisitions().first()
        )
        return BackupCodec.encode(snapshot, BuildConfig.VERSION_NAME, now)
    }

    fun suggestedFileName(now: Long = System.currentTimeMillis()): String =
        "kapterka-kopiya-" + SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.US).format(Date(now)) + ".json"

    /** Возвращает число операций в сохранённой копии. */
    suspend fun exportTo(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val json = buildJson()
            context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                ?: error("Не удалось открыть файл для записи")
            BackupCodec.decode(json).operations.size
        }
    }

    /** Возвращает число восстановленных операций. Если файл плохой, база не меняется. */
    suspend fun restoreFrom(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val text = context.contentResolver.openInputStream(uri)?.use { String(it.readBytes(), Charsets.UTF_8) }
                ?: error("Не удалось прочитать файл")
            restoreFromText(text)
        }
    }

    private suspend fun restoreFromText(text: String): Int {
        val snapshot = BackupCodec.decode(text)
        // Страховка: текущие данные остаются в папке приложения на случай ошибочного выбора копии.
        writeFile("before-restore", buildJson())
        dao.replaceAllData(snapshot.items, snapshot.points, snapshot.stocks, snapshot.operations, snapshot.requisitions)
        return snapshot.operations.size
    }

    private val cloud = CloudBackupService()

    val lastCloudBackupAt: Long get() = prefs.getLong("last_cloud_backup", 0L)

    private suspend fun unitKey(): String = dao.getUserProfile().first()?.unitKey?.trim().orEmpty()

    /** Отправляет копию в облако подразделения. */
    suspend fun uploadToCloud(now: Long = System.currentTimeMillis()): Result<Unit> {
        val key = unitKey()
        if (key.isEmpty()) return Result.failure(IllegalStateException("Сначала подключите общий учёт (код подразделения)"))
        return cloud.upload(key, buildJson(now), now).onSuccess {
            prefs.edit().putLong("last_cloud_backup", now).apply()
        }
    }

    /** Возвращает число восстановленных операций и время копии. */
    suspend fun restoreFromCloud(): Result<Pair<Int, Long>> = runCatching {
        val key = unitKey()
        if (key.isEmpty()) error("Сначала подключите общий учёт (код подразделения)")
        val backup = cloud.download(key).getOrThrow() ?: error("В облаке ещё нет копии этого подразделения")
        withContext(Dispatchers.IO) { restoreFromText(backup.json) } to backup.createdAt
    }

    /** Раз в сутки, если подключён общий учёт, обновляет облачную копию. Тихо, без сообщений. */
    suspend fun cloudBackupIfDue(now: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        try {
            if (now - lastCloudBackupAt < DAY_MS) return@withContext
            if (unitKey().isEmpty()) return@withContext
            if (dao.getAllOperations().first().isEmpty() && dao.getAllStockRecords().first().isEmpty()) return@withContext
            uploadToCloud(now)
        } catch (e: Exception) {
            android.util.Log.w("KapterkaBackup", "Cloud backup skipped: ${e.message}")
        }
    }

    /** Раз в 7 дней сохраняет копию в папку приложения (хранятся три последние). */
    suspend fun autoBackupIfDue(now: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        try {
            if (now - lastAutoBackupAt < WEEK_MS) return@withContext
            if (dao.getAllOperations().first().isEmpty() && dao.getAllStockRecords().first().isEmpty()) return@withContext
            writeFile("auto", buildJson(now))
            prefs.edit().putLong("last_auto_backup", now).apply()
            autoBackupDir.listFiles { f -> f.name.startsWith("auto-") }
                ?.sortedByDescending { it.name }?.drop(3)?.forEach { it.delete() }
        } catch (e: Exception) {
            android.util.Log.w("KapterkaBackup", "Auto backup skipped: ${e.message}")
        }
    }

    private fun writeFile(prefix: String, json: String): File {
        autoBackupDir.mkdirs()
        val f = File(autoBackupDir, "$prefix-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date()) + ".json")
        f.writeText(json, Charsets.UTF_8)
        return f
    }

    companion object {
        const val WEEK_MS = 7L * 24 * 60 * 60 * 1000
        const val DAY_MS = 24L * 60 * 60 * 1000
    }
}
