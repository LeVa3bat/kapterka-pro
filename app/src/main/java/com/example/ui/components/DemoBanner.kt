package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.ui.theme.TacticalDemoBanner
import com.example.ui.theme.TacticalDemoBannerBorder
import com.example.ui.theme.TacticalDemoBannerText
import com.example.ui.theme.TacticalGold
import com.example.ui.theme.TacticalProBanner
import com.example.ui.theme.TacticalProBannerBorder
import com.example.ui.theme.TacticalProBannerText

@Composable
fun DemoBanner(
    profile: UserProfile?,
    onBannerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPro = profile?.isProActive == true

    if (isPro) {
        // PRO ACTIVE BANNER
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(TacticalProBanner)
                .border(1.dp, TacticalProBannerBorder, RoundedCornerShape(8.dp))
                .clickable { onBannerClick() }
                .padding(horizontal = 12.dp, vertical = 9.dp)
                .testTag("pro_active_banner"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "PRO",
                    tint = TacticalProBannerText,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "ТАРИФ «КАПТЁРКА ПРО» АКТИВЕН",
                        color = TacticalProBannerText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Полный безлимит. До окончания: ${profile.proDaysLeft} дн.",
                        color = TacticalProBannerText.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Подробнее",
                tint = TacticalProBannerText,
                modifier = Modifier.size(18.dp)
            )
        }
    } else {
        // DEMO MODE BANNER (3 DAYS OR EXPIRED)
        val daysLeft = profile?.demoDaysLeft ?: 0
        val isExpired = daysLeft <= 0

        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isExpired) Color(0x33FF5252) else TacticalDemoBanner)
                .border(
                    1.dp, 
                    if (isExpired) Color(0xFFFF5252).copy(alpha = 0.6f) else TacticalDemoBannerBorder, 
                    RoundedCornerShape(10.dp)
                )
                .clickable { onBannerClick() }
                .padding(horizontal = 12.dp, vertical = 9.dp)
                .testTag("demo_mode_banner"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isExpired) Icons.Default.Lock else Icons.Default.AccessTime,
                    contentDescription = "Демо-режим",
                    tint = if (isExpired) Color(0xFFFF8B8B) else TacticalGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isExpired) "ДЕМО-РЕЖИМ ЗАВЕРШЁН" else "ДЕМО-РЕЖИМ (3 ДНЯ)",
                        color = if (isExpired) Color(0xFFFF8B8B) else TacticalDemoBannerText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = if (isExpired) {
                            "Для списания и выгрузки отчетов введите ключ PRO"
                        } else {
                            "Осталось: $daysLeft дн. Все функции и учет доступны."
                        },
                        color = if (isExpired) Color(0xFFFFD1D1) else TacticalDemoBannerText.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = if (isExpired) "Активировать" else "Продлить",
                tint = if (isExpired) Color(0xFFFF8B8B) else TacticalGold,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
