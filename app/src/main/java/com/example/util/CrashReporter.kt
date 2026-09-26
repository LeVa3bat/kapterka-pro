package com.example.util

import android.content.Context
import android.os.Build
import com.example.BuildConfig
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Локальный отчёт о сбое: версия, модель телефона и стек ошибки. Ни ФИО, ни почты, ни
 * данных склада в отчёт не попадает. Пользователь сам решает, отправлять ли файл.
 */
object CrashReporter {
    private const val FILE = "last_crash.txt"
    private const val MAX_CHARS = 12_000

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                File(appContext.filesDir, FILE).writeText(build(throwable, thread.name), Charsets.UTF_8)
            } catch (_: Throwable) {
                // Сбой записи отчёта не должен мешать штатной обработке.
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun build(throwable: Throwable, threadName: String, now: Long = System.currentTimeMillis()): String {
        val trace = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString()
        val header = buildString {
            appendLine("Каптёрка PRO ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("Дата: " + SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(now)))
            appendLine("Телефон: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}")
            appendLine("Поток: $threadName")
            appendLine()
        }
        return (header + trace).take(MAX_CHARS)
    }

    fun read(context: Context): String? =
        File(context.filesDir, FILE).takeIf { it.exists() }?.readText(Charsets.UTF_8)

    fun clear(context: Context) {
        File(context.filesDir, FILE).delete()
    }
}
