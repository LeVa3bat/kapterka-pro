package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.ui.theme.SageGreenBright
import com.example.ui.theme.SageGreenContainer
import com.example.ui.theme.SageGreenDark
import com.example.ui.theme.SageGreenPrimary
import com.example.ui.theme.TacticalBg
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalBorderSubtle
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalSurfaceLight
import com.example.ui.theme.TacticalTextDim
import com.example.ui.theme.TacticalTextMuted
import com.example.ui.theme.TacticalTextPrimary
import com.example.ui.theme.TacticalTextSecondary
import java.util.UUID

@Composable
fun AuthScreen(
    currentProfile: UserProfile?,
    onSaveProfile: (UserProfile) -> Unit,
    onContinue: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Регистрация, 1: Вход
    var callsign by remember { mutableStateOf(currentProfile?.callsign?.ifBlank { "" } ?: "") }
    var unitName by remember { mutableStateOf(currentProfile?.unitName?.ifBlank { "1-е Подразделение" } ?: "1-е Подразделение") }
    var unitKey by remember { mutableStateOf(currentProfile?.unitKey?.ifBlank { "kapt_59e13b" } ?: "kapt_59e13b") }
    var email by remember { mutableStateOf(currentProfile?.email?.ifBlank { "" } ?: "") }
    var password by remember { mutableStateOf("••••••••") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TacticalBg)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Emblem and Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SageGreenDark)
                        .border(1.dp, SageGreenPrimary, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MilitaryTech,
                        contentDescription = null,
                        tint = SageGreenBright,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "КАПТЁРКА",
                        color = TacticalTextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Воинский учет и снабжение подразделения",
                        color = TacticalTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Main Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, TacticalBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Quick Google Account Sign-In Button
                    Button(
                        onClick = {
                            val prof = (currentProfile ?: UserProfile()).copy(
                                callsign = callsign.ifEmpty { "лева" },
                                unitName = unitName.ifEmpty { "1-е Подразделение" },
                                unitKey = unitKey.ifEmpty { "kapt_59e13b" },
                                email = "alex.666.881@gmail.com",
                                isLoggedIn = true
                            )
                            onSaveProfile(prof)
                            onContinue()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("google_login_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TacticalSurfaceLight,
                            contentColor = TacticalTextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TacticalBorder)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Google",
                            tint = SageGreenBright,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Войти через Google аккаунт", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // "ИЛИ ПО ПОЧТЕ" Divider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(TacticalBorder)
                        )
                        Text(
                            text = "  ИЛИ ПО ПОЧТЕ И КЛЮЧУ  ",
                            color = TacticalTextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(TacticalBorder)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Tab Switcher: Регистрация / Вход
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = TacticalBg,
                        contentColor = SageGreenBright,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = SageGreenPrimary
                            )
                        },
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Регистрация", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Вход в подразделение", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Fields
                    OutlinedTextField(
                        value = callsign,
                        onValueChange = { callsign = it },
                        label = { Text("Позывной / Имя", color = TacticalTextSecondary, fontSize = 12.sp) },
                        placeholder = { Text("например: Лева / Ворон", color = TacticalTextDim, fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = TacticalSurfaceLight,
                            unfocusedContainerColor = TacticalSurfaceLight,
                            focusedBorderColor = SageGreenPrimary,
                            unfocusedBorderColor = TacticalBorder,
                            focusedTextColor = TacticalTextPrimary,
                            unfocusedTextColor = TacticalTextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = unitName,
                        onValueChange = { unitName = it },
                        label = { Text("Подразделение / Рота", color = TacticalTextSecondary, fontSize = 12.sp) },
                        placeholder = { Text("например: 1-е Подразделение / 3-й минбат", color = TacticalTextDim, fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = TacticalSurfaceLight,
                            unfocusedContainerColor = TacticalSurfaceLight,
                            focusedBorderColor = SageGreenPrimary,
                            unfocusedBorderColor = TacticalBorder,
                            focusedTextColor = TacticalTextPrimary,
                            unfocusedTextColor = TacticalTextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Unit Key Field with regenerate icon if registering
                    OutlinedTextField(
                        value = unitKey,
                        onValueChange = { unitKey = it },
                        label = { Text("Ключ подразделения", color = TacticalTextSecondary, fontSize = 12.sp) },
                        placeholder = { Text("например: kapt_59e13b", color = TacticalTextDim, fontSize = 12.sp) },
                        singleLine = true,
                        trailingIcon = {
                            if (selectedTab == 0) {
                                IconButton(onClick = { unitKey = "kapt_" + UUID.randomUUID().toString().take(6) }) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Сгенерировать ключ",
                                        tint = SageGreenBright
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = TacticalSurfaceLight,
                            unfocusedContainerColor = TacticalSurfaceLight,
                            focusedBorderColor = SageGreenPrimary,
                            unfocusedBorderColor = TacticalBorder,
                            focusedTextColor = SageGreenBright,
                            unfocusedTextColor = SageGreenBright
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Key helper note
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = SageGreenPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (selectedTab == 0)
                                "Ключ связывает все телефоны роты. Введите одинаковый ключ на всех устройствах."
                            else
                                "Введите ключ, выданный старшиной или командиром роты.",
                            color = TacticalTextMuted,
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Электронная почта (Email)", color = TacticalTextSecondary, fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = TacticalSurfaceLight,
                            unfocusedContainerColor = TacticalSurfaceLight,
                            focusedBorderColor = SageGreenPrimary,
                            unfocusedBorderColor = TacticalBorder,
                            focusedTextColor = TacticalTextPrimary,
                            unfocusedTextColor = TacticalTextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Primary Submit Button
                    Button(
                        onClick = {
                            val prof = (currentProfile ?: UserProfile()).copy(
                                callsign = callsign.ifEmpty { "пользователь" },
                                unitName = unitName.ifEmpty { "1-е Подразделение" },
                                unitKey = unitKey.ifEmpty { "kapt_59e13b" },
                                email = email,
                                isLoggedIn = true
                            )
                            onSaveProfile(prof)
                            onContinue()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("submit_auth_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SageGreenPrimary,
                            contentColor = Color(0xFF0F1B14)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (selectedTab == 0) "Зарегистрироваться и создать ключ" else "Войти в подразделение",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Offline Mode Entry Button
                    Button(
                        onClick = {
                            val prof = (currentProfile ?: UserProfile()).copy(
                                callsign = callsign.ifEmpty { "пользователь" },
                                unitName = unitName.ifEmpty { "1-е Подразделение" },
                                isLoggedIn = true
                            )
                            onSaveProfile(prof)
                            onContinue()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("offline_auth_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TacticalSurfaceLight,
                            contentColor = TacticalTextSecondary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = "Офлайн",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Автономный вход (Офлайн в поле)", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Offline-First Notice Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(TacticalSurface.copy(alpha = 0.8f))
                    .border(1.dp, TacticalBorderSubtle, RoundedCornerShape(8.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Безопасность",
                    tint = SageGreenPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Все данные сохраняются локально на устройстве (Offline-First) и автоматически синхронизируются при наличии сети.",
                    color = TacticalTextMuted,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}
