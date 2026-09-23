package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.model.UserProfile
import com.example.data.model.WarehousePoint
import com.example.universal.WarehouseProfileCatalog

@Composable
fun UniversalMoreScreen(
    userProfile: UserProfile?,
    warehouseProfileId: String?,
    points: List<WarehousePoint>,
    onAddWarehouse: () -> Unit,
    onEditWarehouse: (WarehousePoint) -> Unit,
    onChangeProfile: () -> Unit,
    onLogout: () -> Unit
) {
    val profile = WarehouseProfileCatalog.find(warehouseProfileId)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FB))
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        item {
            Text(
                text = "Профиль",
                color = Color(0xFF111827),
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Аккаунт и настройки рабочего пространства",
                color = Color(0xFF6B7280),
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF20265C))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(17.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                    Spacer(modifier = Modifier.size(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userProfile?.callsign?.takeIf { it.isNotBlank() } ?: "Пользователь",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = Color(0xFFC9CCFF),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.size(5.dp))
                            Text(
                                text = userProfile?.email?.takeIf { it.isNotBlank() } ?: "Email не указан",
                                color = Color(0xFFC9CCFF),
                                fontSize = 10.5.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            SettingsRow(
                emoji = profile.emoji,
                title = "Тип склада",
                subtitle = profile.title,
                onClick = onChangeProfile
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        item {
            WarehouseSettingsCard(
                points = points,
                onAddWarehouse = onAddWarehouse,
                onEditWarehouse = onEditWarehouse
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        item {
            SettingsRow(
                emoji = "☁️",
                title = "Синхронизация",
                subtitle = "Аккаунт подключён; складские данные Alpha пока хранятся локально",
                onClick = {}
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (profile.id == "military") {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F2F5))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Военный профиль",
                            color = Color(0xFF303846),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Камера и фотографии отключены. Специализированные категории доступны только в этом профиле.",
                            color = Color(0xFF687181),
                            fontSize = 10.5.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(15.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF5B5CE2),
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "Склад ПРО",
                            color = Color(0xFF111827),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Версия ${BuildConfig.VERSION_NAME}",
                        color = Color(0xFF596273),
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Отдельное приложение. Оно не использует рабочую базу, лицензию или серверы «Каптёрки ПРО».",
                        color = Color(0xFF858D9B),
                        fontSize = 10.5.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFFFEEEE))
                    .clickable(onClick = onLogout)
                    .padding(horizontal = 15.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = null,
                    tint = Color(0xFFB42318),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    text = "Выйти из аккаунта",
                    color = Color(0xFFB42318),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun WarehouseSettingsCard(
    points: List<WarehousePoint>,
    onAddWarehouse: () -> Unit,
    onEditWarehouse: (WarehousePoint) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Склады",
                        color = Color(0xFF111827),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (points.isEmpty()) "Добавьте первый склад" else "Всего: ${points.size}",
                        color = Color(0xFF747D8C),
                        fontSize = 10.sp
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF2F3FF))
                        .clickable(onClick = onAddWarehouse)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color(0xFF5B5CE2),
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = "Добавить",
                        color = Color(0xFF5B5CE2),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (points.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
            }

            points.forEachIndexed { index, point ->
                if (index > 0) {
                    Spacer(modifier = Modifier.height(7.dp))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(15.dp))
                        .background(Color(0xFFF8F9FC))
                        .clickable { onEditWarehouse(point) }
                        .padding(horizontal = 11.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(Color(0xFFEFF1FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warehouse,
                            contentDescription = null,
                            tint = Color(0xFF5B5CE2),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.size(9.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = point.name,
                                color = Color(0xFF111827),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            if (point.isBase) {
                                Text(
                                    text = "ОСНОВНОЙ",
                                    color = Color(0xFF5B5CE2),
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(100.dp))
                                        .background(Color(0xFFEFF1FF))
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                        if (point.description.isNotBlank()) {
                            Text(
                                text = point.description,
                                color = Color(0xFF7B8493),
                                fontSize = 9.5.sp,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.size(6.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Редактировать",
                        tint = Color(0xFFB3BAC6),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(
    emoji: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(43.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFF2F3FF)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 21.sp)
        }

        Spacer(modifier = Modifier.size(11.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color(0xFF111827),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = Color(0xFF747D8C),
                fontSize = 10.5.sp,
                lineHeight = 14.sp,
                maxLines = 2
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFFB3BAC6),
            modifier = Modifier.size(20.dp)
        )
    }
}
