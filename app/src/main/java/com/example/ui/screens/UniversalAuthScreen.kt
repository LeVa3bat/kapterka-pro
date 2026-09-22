package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SageGreenPrimary
import com.example.ui.theme.TacticalBg
import com.example.ui.theme.TacticalBorderSubtle
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalTextMuted
import com.example.ui.theme.TacticalTextPrimary

@Composable
fun UniversalAuthScreen(
    hasAccount: Boolean,
    savedEmail: String,
    onRegister: (name: String, email: String, password: String) -> String?,
    onLogin: (email: String, password: String) -> String?
) {
    var mode by remember { mutableStateOf(if (hasAccount) 1 else 0) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(savedEmail) }
    var password by remember { mutableStateOf("") }
    var passwordRepeat by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFF7F8FC),
                        Color(0xFFF1F4FA)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 28.dp)
        ) {
            Spacer(modifier = Modifier.height(18.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF5B5CE2)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "S",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Spacer(modifier = Modifier.size(12.dp))
                Column {
                    Text(
                        text = "Склад ПРО",
                        color = Color(0xFF111827),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Умный склад в вашем телефоне",
                        color = Color(0xFF6B7280),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(34.dp))

            Text(
                text = if (mode == 0) "Создайте аккаунт" else "С возвращением",
                color = Color(0xFF111827),
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(7.dp))
            Text(
                text = if (mode == 0)
                    "Сначала создадим ваш профиль. Тип склада выберете на следующем шаге."
                else
                    "Войдите в локальный аккаунт этой Alpha-версии.",
                color = Color(0xFF6B7280),
                fontSize = 13.sp,
                lineHeight = 19.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFF3F4F6))
                            .padding(4.dp)
                    ) {
                        AuthModeButton(
                            text = "Регистрация",
                            selected = mode == 0,
                            onClick = {
                                mode = 0
                                error = null
                            },
                            modifier = Modifier.weight(1f)
                        )
                        AuthModeButton(
                            text = "Вход",
                            selected = mode == 1,
                            onClick = {
                                mode = 1
                                error = null
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    AnimatedContent(
                        targetState = mode,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "AuthMode"
                    ) { selectedMode ->
                        Column {
                            if (selectedMode == 0) {
                                ModernAuthField(
                                    value = name,
                                    onValueChange = {
                                        name = it
                                        error = null
                                    },
                                    label = "Ваше имя",
                                    placeholder = "Алексей",
                                    icon = Icons.Default.Person
                                )
                                Spacer(modifier = Modifier.height(11.dp))
                            }

                            ModernAuthField(
                                value = email,
                                onValueChange = {
                                    email = it
                                    error = null
                                },
                                label = "Email",
                                placeholder = "name@example.com",
                                icon = Icons.Default.Email
                            )

                            Spacer(modifier = Modifier.height(11.dp))

                            ModernAuthField(
                                value = password,
                                onValueChange = {
                                    password = it
                                    error = null
                                },
                                label = "Пароль",
                                placeholder = "Минимум 8 символов",
                                icon = Icons.Default.Lock,
                                isPassword = true
                            )

                            if (selectedMode == 0) {
                                Spacer(modifier = Modifier.height(11.dp))
                                ModernAuthField(
                                    value = passwordRepeat,
                                    onValueChange = {
                                        passwordRepeat = it
                                        error = null
                                    },
                                    label = "Повторите пароль",
                                    placeholder = "Ещё раз пароль",
                                    icon = Icons.Default.Lock,
                                    isPassword = true
                                )
                            }
                        }
                    }

                    if (error != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = error!!,
                            color = Color(0xFFB42318),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            error = if (mode == 0) {
                                when {
                                    name.trim().isBlank() -> "Укажите ваше имя"
                                    password != passwordRepeat -> "Пароли не совпадают"
                                    else -> onRegister(name.trim(), email.trim(), password)
                                }
                            } else {
                                onLogin(email.trim(), password)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF5B5CE2),
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = if (mode == 0) "Создать аккаунт" else "Войти",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Alpha: аккаунт пока хранится только на этом устройстве. Рабочую «Каптёрку ПРО» и её серверы это приложение не использует.",
                color = Color(0xFF7C8392),
                fontSize = 10.5.sp,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun AuthModeButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(if (selected) Color.White else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Color(0xFF111827) else Color(0xFF6B7280),
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun ModernAuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(label, fontSize = 12.sp) },
        placeholder = { Text(placeholder, color = Color(0xFF9CA3AF), fontSize = 12.sp) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF5B5CE2),
                modifier = Modifier.size(19.dp)
            )
        },
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        shape = RoundedCornerShape(15.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xFFF9FAFB),
            unfocusedContainerColor = Color(0xFFF9FAFB),
            focusedBorderColor = Color(0xFF5B5CE2),
            unfocusedBorderColor = Color(0xFFE5E7EB),
            focusedTextColor = Color(0xFF111827),
            unfocusedTextColor = Color(0xFF111827),
            focusedLabelColor = Color(0xFF5B5CE2),
            unfocusedLabelColor = Color(0xFF6B7280)
        )
    )
}
