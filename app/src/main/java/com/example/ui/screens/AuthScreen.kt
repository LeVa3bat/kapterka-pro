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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.ui.components.LegalDocumentTab
import com.example.ui.components.LegalDocumentsDialog
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
import androidx.compose.ui.platform.LocalContext

@Composable
fun AuthScreen(
    currentProfile: UserProfile?,
    onCompleteAuth: (UserProfile) -> Unit
) {
    val context = LocalContext.current
    

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Регистрация, 1: Вход
    var callsign by remember { mutableStateOf(currentProfile?.callsign?.ifBlank { "" } ?: "") }
    var unitName by remember { mutableStateOf(currentProfile?.unitName?.ifBlank { "" } ?: "") }
    var unitKey by remember {
        mutableStateOf(
            if (!currentProfile?.unitKey.isNullOrBlank()) currentProfile?.unitKey!!
            else ""
        )
    }
    var email by remember { mutableStateOf(currentProfile?.email?.ifBlank { "" } ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var consentAgreed by remember { mutableStateOf(false) }
    var showLegalDialog by remember { mutableStateOf(false) }
    var legalDialogTab by remember { mutableStateOf(LegalDocumentTab.PRIVACY) }

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
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "v${com.example.BuildConfig.VERSION_NAME} PRO (Сборка ${com.example.BuildConfig.VERSION_CODE})",
                        color = SageGreenBright,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
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
                    Text(
                        text = "Создайте новое подразделение или подключитесь к существующему.",
                        color = TacticalTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

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
                        placeholder = { Text("Введите свой позывной (например: Сокол, Буран)", color = TacticalTextDim, fontSize = 12.sp) },
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
                        placeholder = { Text("Введите название: ${"1-е Подразделение"}", color = TacticalTextDim, fontSize = 12.sp) },
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
                        label = { Text("Код подразделения", color = TacticalTextSecondary, fontSize = 12.sp) },
                        placeholder = { Text(if (selectedTab == 0) "Пусто = создать новый код" else "Введите код существующего подразделения", color = TacticalTextDim, fontSize = 12.sp) },
                        singleLine = true,
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
                                "Оставьте поле пустым — приложение безопасно создаст новый код. Для других телефонов используйте этот же код."
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
                        placeholder = { Text("Введите email для чеков (например: name@mail.ru)", color = TacticalTextDim, fontSize = 12.sp) },
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

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = errorMessage!!,
                            color = Color(0xFFEF5350),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // MANDATORY LEGAL CONSENT BLOCK BEFORE REGISTRATION (RuStore & 152-ФЗ Policy)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(TacticalSurfaceLight)
                            .border(
                                1.dp,
                                if (!consentAgreed && errorMessage != null) Color(0xFFEF5350) else TacticalBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Checkbox(
                            checked = consentAgreed,
                            onCheckedChange = { consentAgreed = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = SageGreenPrimary,
                                uncheckedColor = TacticalTextSecondary,
                                checkmarkColor = Color.White
                            ),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Перед регистрацией ознакомлен и принимаю:",
                                color = TacticalTextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Политику конфиденциальности",
                                    color = SageGreenBright,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    textDecoration = TextDecoration.Underline,
                                    modifier = Modifier.clickable {
                                        legalDialogTab = LegalDocumentTab.PRIVACY
                                        showLegalDialog = true
                                    }
                                )
                                Text(text = "•", color = TacticalTextMuted, fontSize = 11.sp)
                                Text(
                                    text = "Соглашение",
                                    color = SageGreenBright,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    textDecoration = TextDecoration.Underline,
                                    modifier = Modifier.clickable {
                                        legalDialogTab = LegalDocumentTab.TERMS
                                        showLegalDialog = true
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Согласие на обработку данных (152-ФЗ)",
                                color = SageGreenBright,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier.clickable {
                                    legalDialogTab = LegalDocumentTab.CONSENT
                                    showLegalDialog = true
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Primary Submit Button
                    Button(
                        onClick = {
                            if (!consentAgreed) {
                                errorMessage = "Для продолжения необходимо подтвердить согласие с Политикой конфиденциальности и Соглашением!"
                                return@Button
                            }
                            val cleanCallsign = callsign.trim()
                            if (cleanCallsign.isBlank()) {
                                errorMessage = "Пожалуйста, введите ваш позывной или имя!"
                                return@Button
                            }

                            val cleanEmail = email.trim().lowercase()
                            if (cleanEmail.isBlank() ||
                                !android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()
                            ) {
                                errorMessage = "Укажите корректный Email. Он нужен для лицензии и восстановления доступа."
                                return@Button
                            }

                            val cleanUnitName = unitName.trim().ifEmpty { "1-е Подразделение" }
                            val enteredKey = unitKey.trim()
                            if (selectedTab == 1 && enteredKey.isBlank()) {
                                errorMessage = "Для входа в существующее подразделение укажите его код."
                                return@Button
                            }

                            val resolvedKey = if (selectedTab == 0 && enteredKey.isBlank()) {
                                com.example.data.sync.SyncIdentityGenerator.newUnitKey()
                            } else {
                                enteredKey
                            }

                            errorMessage = null
                            val prof = (currentProfile ?: UserProfile()).copy(
                                callsign = cleanCallsign,
                                unitName = cleanUnitName,
                                unitKey = resolvedKey,
                                email = cleanEmail,
                                isLoggedIn = true
                            )
                            onCompleteAuth(prof)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("submit_auth_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SageGreenPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (selectedTab == 0) "Зарегистрироваться и создать ключ" else "Войти",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Offline Mode Entry Button
                    Button(
                        onClick = {
                            if (!consentAgreed) {
                                errorMessage = "Для входа необходимо подтвердить согласие с условиями!"
                                return@Button
                            }
                            val cleanCallsign = callsign.trim().ifEmpty { "Пользователь" }
                            val cleanUnitName = unitName.trim().ifEmpty { "1-е Подразделение" }
                            val cleanKey = unitKey.trim()
                            val cleanEmail = email.trim()

                            val prof = (currentProfile ?: UserProfile()).copy(
                                callsign = cleanCallsign,
                                unitName = cleanUnitName,
                                unitKey = cleanKey,
                                email = cleanEmail,
                                isLoggedIn = true
                            )
                            onCompleteAuth(prof)
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

            Spacer(modifier = Modifier.height(10.dp))

            // Footer Legal Document Links
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Политика конфиденциальности",
                    color = SageGreenBright,
                    fontSize = 11.sp,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable {
                        legalDialogTab = LegalDocumentTab.PRIVACY
                        showLegalDialog = true
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "•", color = TacticalTextMuted, fontSize = 11.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Пользовательское соглашение",
                    color = SageGreenBright,
                    fontSize = 11.sp,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable {
                        legalDialogTab = LegalDocumentTab.TERMS
                        showLegalDialog = true
                    }
                )
            }
        }

        // Render In-App Legal Documents Dialog
        if (showLegalDialog) {
            LegalDocumentsDialog(
                initialTab = legalDialogTab,
                onDismiss = { showLegalDialog = false },
                onAccept = {
                    consentAgreed = true
                    showLegalDialog = false
                }
            )
        }
    }
}

