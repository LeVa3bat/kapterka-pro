package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalSurfaceLight
import com.example.ui.theme.TacticalTextMuted
import com.example.ui.theme.TacticalTextPrimary

@Composable
fun ReviewRequestCard(
    onOpenReview: () -> Unit,
    onLater: () -> Unit,
    onStopAsking: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = TacticalSurfaceLight),
        border = BorderStroke(1.dp, TacticalBorder)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text("Отчёт сохранён", color = TacticalTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(
                "Поделитесь честным опытом в RuStore: что удобно и что стоит улучшить. Это добровольно.",
                color = TacticalTextMuted,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(REVIEW_STORE_URL))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }.onSuccess { onOpenReview() }
                        .onFailure {
                            Toast.makeText(context, "Не удалось открыть RuStore. Попробуйте позже.", Toast.LENGTH_SHORT).show()
                        }
                }) {
                    Text("Оставить отзыв", fontSize = 12.sp)
                }
                TextButton(onClick = onLater) { Text("Позже", fontSize = 12.sp) }
            }
            TextButton(onClick = onStopAsking) { Text("Больше не спрашивать", fontSize = 11.sp) }
        }
    }
}

private const val REVIEW_STORE_URL = "https://www.rustore.ru/catalog/app/com.aistudio.kapterka.jmwqve"
