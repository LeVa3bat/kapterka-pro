package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.universal.UniversalEntitlement

@Composable
fun UniversalProDialog(
    entitlement: UniversalEntitlement?,
    backendConfigured: Boolean,
    hasPendingPayment: Boolean,
    loading: Boolean,
    message: String?,
    onPay: () -> Unit,
    onCheckPayment: () -> Unit,
    onDismiss: () -> Unit
) {
    val statusTitle = when {
        entitlement?.isProActive == true ->
            "PRO активен • ${entitlement.daysRemaining()} дн."
        entitlement?.isTrialActive == true ->
            "Демо • ${entitlement.daysRemaining()} дн."
        entitlement?.isExpired == true ->
            "Демо завершено"
        else ->
            "Подписка"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        containerColor = Color.White,
        title = {
            Column {
                Text(
                    text = "Склад ПРО",
                    color = Color(0xFF111827),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = statusTitle,
                    color = when {
                        entitlement?.isProActive == true -> Color(0xFF16803A)
                        entitlement?.isTrialActive == true -> Color(0xFF5B5CE2)
                        else -> Color(0xFFB42318)
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color(0xFFF4F3FF),
                            RoundedCornerShape(18.dp)
                        )
                        .padding(14.dp)
                ) {
                    Text(
                        text = "500 ₽ / 30 дней",
                        color = Color(0xFF20265C),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Продление добавляет 30 дней к уже оплаченному сроку.",
                        color = Color(0xFF667085),
                        fontSize = 10.5.sp,
                        lineHeight = 15.sp
                    )
                }

                if (entitlement?.isProActive == true) {
                    Text(
                        text = "PRO активирован",
                        color = Color(0xFF16803A),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Платёж подтверждён сервером. Осталось " + entitlement.daysRemaining() + " дн.",
                        color = Color(0xFF596273),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                } else {
                    Text(
                        text = "Демо на 3 дня",
                        color = Color(0xFF111827),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "В демо доступны базовые локальные возможности. PRO открывает функции, отмеченные в приложении как PRO, включая облачную синхронизацию между вашими устройствами.",
                        color = Color(0xFF596273),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                    Text(
                        text = "После окончания демо данные не удаляются: просмотр остаётся доступен, а изменение данных включается после активации PRO.",
                        color = Color(0xFF7C8392),
                        fontSize = 10.5.sp,
                        lineHeight = 15.sp
                    )
                }

                if (!backendConfigured) {
                    Text(
                        text = "Оплата временно недоступна. Данные и демо-доступ сохраняются; попробуйте открыть подписку позже.",
                        color = Color(0xFFB54708),
                        fontSize = 10.5.sp,
                        lineHeight = 15.sp
                    )
                }

                if (!message.isNullOrBlank()) {
                    Text(
                        text = message,
                        color = Color(0xFFB42318),
                        fontSize = 10.5.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        },
        confirmButton = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (hasPendingPayment) {
                    TextButton(
                        onClick = onCheckPayment,
                        enabled = !loading && backendConfigured
                    ) {
                        Text("Проверить оплату")
                    }
                }

                Button(
                    onClick = onPay,
                    enabled = !loading && backendConfigured,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF5B5CE2),
                        contentColor = Color.White
                    )
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = Color.White,
                            modifier = Modifier.height(18.dp)
                        )
                    } else {
                        Text(
                            text = if (entitlement?.isProActive == true)
                                "Продлить 500 ₽"
                            else
                                "Оплатить 500 ₽",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !loading) {
                Text("Закрыть", color = Color(0xFF667085))
            }
        }
    )
}
