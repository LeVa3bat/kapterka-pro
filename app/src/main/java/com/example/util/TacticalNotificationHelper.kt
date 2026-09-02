package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.RequestStatus
import com.example.data.model.RequisitionRequest

object TacticalNotificationHelper {
    private const val CHANNEL_ID = "kapterka_tactical_channel"
    private const val CHANNEL_NAME = "Оповещения каптёрки (Боевые)"
    private const val CHANNEL_DESC = "Уведомления о перемещениях и выдаче имущества"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                setShowBadge(true)
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun getPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getActivity(context, 0, intent, flags)
    }

    fun notifyRequisitionStatus(
        context: Context,
        req: RequisitionRequest,
        status: RequestStatus
    ) {
        if (!hasNotificationPermission(context)) return
        createNotificationChannel(context)

        val (title, priorityText) = when (status) {
            RequestStatus.COLLECTED -> Pair(
                "📦 Заявка СОБРАНА для ${req.pointName}!",
                "Готова к выдаче и отправке на ${req.pointName}"
            )
            RequestStatus.ISSUED -> Pair(
                "🚚 Заявка ВЫДАНА (${req.pointName})",
                "Имущество выдано подразделению (${req.applicantName})"
            )
            RequestStatus.PENDING -> Pair(
                "📋 Новая заявка на снабжение",
                "Заявка для ${req.pointName} от ${req.applicantName}"
            )
        }

        val contentText = "$priorityText • ${req.itemsSummary.take(60)}"
        val bigText = buildString {
            append("Статус: ${status.titleRu.uppercase()}\n")
            append("Точка назначения: ${req.pointName}\n")
            append("Заявитель: ${req.applicantName}\n")
            append("Состав: ${req.itemsSummary}\n")
            if (req.comment.isNotEmpty()) {
                append("Примечание: ${req.comment}")
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(getPendingIntent(context))
            .setAutoCancel(true)
            .setColor(0xFF8DAA59.toInt()) // Tactical Sage Green
            .build()

        try {
            NotificationManagerCompat.from(context).notify(req.id.hashCode(), notification)
        } catch (_: SecurityException) {
        }
    }

    fun notifyTransfer(
        context: Context,
        fromPoint: String,
        toPoint: String,
        itemsSummary: String,
        baseWarehouseStockSummary: String
    ) {
        if (!hasNotificationPermission(context)) return
        createNotificationChannel(context)

        val title = "🔄 Перемещение: $fromPoint ➔ $toPoint"
        val contentText = "Перемещено: ${itemsSummary.take(45)} • Базовый: $baseWarehouseStockSummary"

        val bigText = buildString {
            append("Маршрут: $fromPoint ➔ $toPoint\n")
            append("Перемещенное имущество: $itemsSummary\n\n")
            append("📦 Остатки на Базовом складе:\n")
            append(baseWarehouseStockSummary.ifEmpty { "Учет синхронизирован по подразделению" })
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(getPendingIntent(context))
            .setAutoCancel(true)
            .setColor(0xFF4DB6AC.toInt()) // Tactical Teal
            .build()

        try {
            val notifId = (System.currentTimeMillis() % 100000).toInt()
            NotificationManagerCompat.from(context).notify(notifId, notification)
        } catch (_: SecurityException) {
        }
    }

    fun notifyIncome(
        context: Context,
        toPoint: String,
        supplier: String,
        itemsSummary: String,
        baseWarehouseStockSummary: String
    ) {
        if (!hasNotificationPermission(context)) return
        createNotificationChannel(context)

        val title = "📥 Приход на склад: $toPoint"
        val contentText = "От: $supplier • $itemsSummary"

        val bigText = buildString {
            append("Поставщик / Источник: $supplier\n")
            append("Принято на склад: $itemsSummary\n\n")
            if (baseWarehouseStockSummary.isNotEmpty()) {
                append("📦 Текущий остаток склада:\n$baseWarehouseStockSummary")
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(getPendingIntent(context))
            .setAutoCancel(true)
            .setColor(0xFF8DAA59.toInt())
            .build()

        try {
            val notifId = (System.currentTimeMillis() % 100000).toInt()
            NotificationManagerCompat.from(context).notify(notifId, notification)
        } catch (_: SecurityException) {
        }
    }

    fun notifyIssue(
        context: Context,
        fromPoint: String,
        toPoint: String,
        itemsSummary: String,
        baseWarehouseStockSummary: String
    ) {
        if (!hasNotificationPermission(context)) return
        createNotificationChannel(context)

        val title = "⬆️ Выдача (Поднятие) имущества"
        val contentText = "$fromPoint ➔ $toPoint • $itemsSummary"

        val bigText = buildString {
            append("Маршрут: $fromPoint ➔ $toPoint\n")
            append("Выдано: $itemsSummary\n\n")
            if (baseWarehouseStockSummary.isNotEmpty()) {
                append("📦 Текущий остаток склада:\n$baseWarehouseStockSummary")
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(getPendingIntent(context))
            .setAutoCancel(true)
            .setColor(0xFFFFB300.toInt()) // Tactical Gold
            .build()

        try {
            val notifId = (System.currentTimeMillis() % 100000).toInt()
            NotificationManagerCompat.from(context).notify(notifId, notification)
        } catch (_: SecurityException) {
        }
    }

    fun notifyExpenditure(
        context: Context,
        pointName: String,
        docNumber: String,
        itemsSummary: String,
        reason: String
    ) {
        if (!hasNotificationPermission(context)) return
        createNotificationChannel(context)

        val title = "💥 Расход имущества (Акт ф.8 № $docNumber)"
        val contentText = "Позиция: $pointName • Списано: $itemsSummary"

        val bigText = buildString {
            append("Точка списания: $pointName\n")
            append("Акт расхода: № $docNumber\n")
            append("Списано: $itemsSummary\n")
            append("Задача/Причина: ${if(reason.isEmpty()) "Боевая работа" else reason}")
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(getPendingIntent(context))
            .setAutoCancel(true)
            .setColor(0xFFE57373.toInt()) // Tactical Red
            .build()

        try {
            val notifId = (System.currentTimeMillis() % 100000).toInt()
            NotificationManagerCompat.from(context).notify(notifId, notification)
        } catch (_: SecurityException) {
        }
    }
}
