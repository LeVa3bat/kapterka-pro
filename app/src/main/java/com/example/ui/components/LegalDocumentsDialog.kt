package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.SageGreenBright
import com.example.ui.theme.SageGreenPrimary
import com.example.ui.theme.TacticalBg
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalSurfaceLight
import com.example.ui.theme.TacticalTextDim
import com.example.ui.theme.TacticalTextMuted
import com.example.ui.theme.TacticalTextPrimary
import com.example.ui.theme.TacticalTextSecondary

enum class LegalDocumentTab(val title: String) {
    PRIVACY("Политика конфиденциальности"),
    TERMS("Пользовательское соглашение"),
    CONSENT("Согласие 152-ФЗ")
}

@Composable
fun LegalDocumentsDialog(
    initialTab: LegalDocumentTab = LegalDocumentTab.PRIVACY,
    onDismiss: () -> Unit,
    onAccept: () -> Unit = onDismiss
) {
    val context = LocalContext.current
    var selectedIndex by remember {
        mutableIntStateOf(
            when (initialTab) {
                LegalDocumentTab.PRIVACY -> 0
                LegalDocumentTab.TERMS -> 1
                LegalDocumentTab.CONSENT -> 2
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, SageGreenPrimary.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SageGreenPrimary.copy(alpha = 0.15f))
                                .border(1.dp, SageGreenPrimary, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = SageGreenBright,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ПРАВОВЫЕ ДОКУМЕНТЫ",
                                color = TacticalTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "ПО «Каптёрка Про» • Федеральный закон № 152-ФЗ",
                                color = TacticalTextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = TacticalTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Navigation Tabs
                TabRow(
                    selectedTabIndex = selectedIndex,
                    containerColor = TacticalBg,
                    contentColor = SageGreenBright,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                            color = SageGreenPrimary
                        )
                    },
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                ) {
                    Tab(
                        selected = selectedIndex == 0,
                        onClick = { selectedIndex = 0 },
                        text = {
                            Text(
                                text = "Конфиденциальность",
                                fontSize = 11.sp,
                                fontWeight = if (selectedIndex == 0) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1
                            )
                        }
                    )
                    Tab(
                        selected = selectedIndex == 1,
                        onClick = { selectedIndex = 1 },
                        text = {
                            Text(
                                text = "Соглашение",
                                fontSize = 11.sp,
                                fontWeight = if (selectedIndex == 1) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1
                            )
                        }
                    )
                    Tab(
                        selected = selectedIndex == 2,
                        onClick = { selectedIndex = 2 },
                        text = {
                            Text(
                                text = "152-ФЗ",
                                fontSize = 11.sp,
                                fontWeight = if (selectedIndex == 2) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Document Content Scrollable Body
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(TacticalSurfaceLight)
                        .border(1.dp, TacticalBorder, RoundedCornerShape(8.dp))
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    when (selectedIndex) {
                        0 -> PrivacyPolicyContent()
                        1 -> TermsOfServiceContent()
                        2 -> PersonalDataConsentContent()
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Actions: Open on Web & Accept Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            val url = when (selectedIndex) {
                                0 -> "https://kapterka-pro.ru/privacy.html"
                                1 -> "https://kapterka-pro.ru/terms.html"
                                else -> "https://kapterka-pro.ru/privacy.html#consent"
                            }
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TacticalSurfaceLight,
                            contentColor = SageGreenBright
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = "На сайте",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("На сайте", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            onAccept()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SageGreenPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = "Принять",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Принимаю условия", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacyPolicyContent() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "ПОЛИТИКА КОНФИДЕНЦИАЛЬНОСТИ И ОБРАБОТКИ ДАННЫХ",
            color = SageGreenBright,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Дата вступления в силу: 1 января 2025 г. (в редакции от 15.09.2026 г.)\nОператор: разработчик программного обеспечения «Каптёрка Про» (ИНН 250104230398, email: alex.666.881@gmail.com).",
            color = TacticalTextMuted,
            fontSize = 10.sp
        )

        SectionHeader("1. Общие положения")
        LegalText(
            "1.1. Настоящая Политика конфиденциальности определяет порядок обработки и защиты информации о физических лицах, использующих мобильное приложение «Каптёрка Про» и веб-сайт kapterka-pro.ru.\n" +
            "1.2. Соблюдение конфиденциальности и защита информации пользователей является безусловным приоритетом разработчика. Приложение разработано с учетом автономной работы в полевых условиях с повышенными требованиями к информационной безопасности."
        )

        SectionHeader("2. Состав и объем обрабатываемых данных")
        LegalText(
            "2.1. При регистрации и использовании сервиса Пользователь по своему усмотрению указывает следующие данные:\n" +
            " • Позывной / псевдоним пользователя (для отображения в проводках учета);\n" +
            " • Условное наименование подразделения;\n" +
            " • Адрес электронной почты (Email) — используется исключительно для отправки фискальных чеков при оплате и восстановления доступа;\n" +
            " • Идентификационный код связки подразделения (Unit Key).\n\n" +
            "2.2. ВОИНСКИЕ И СКЛАДСКИЕ ДАННЫЕ:\n" +
            "Все списки материальных средств, боеприпасов, военного имущества, акты списания (Форма № 8) и ведомости учета (Форма № 18) хранятся исключительно в изолированной зашифрованной локальной базе данных SQLite на вашем мобильном устройстве (Offline-First). Разработчик не осуществляет сбор, передачу или анализ содержимого ваших складских книг третьим лицам."
        )

        SectionHeader("3. Цели обработки информации")
        LegalText(
            "Обработка минимального набора данных осуществляется исключительно в целях:\n" +
            " • Идентификации пользователя и связки устройств одного подразделения по защищенному коду;\n" +
            " • Обеспечения работоспособности функций многопользовательского учета;\n" +
            " • Предоставления технической поддержки по запросу пользователя."
        )

        SectionHeader("4. Безопасность и защита данных")
        LegalText(
            "4.1. Приложение поддерживает 100% автономный режим работы без необходимости подключения к сети Интернет.\n" +
            "4.2. Персональные данные Пользователя никогда не продаются, не передаются рекламным сетям и не предоставляются третьим лицам, за исключением случаев, прямо предусмотренных действующим законодательством Российской Федерации."
        )

        SectionHeader("5. Изменение и удаление данных")
        LegalText(
            "Пользователь имеет право в любой момент очистить базу данных в настройках приложения или удалить аккаунт, направив запрос по адресу alex.666.881@gmail.com или в Telegram @kapterka_help_bot."
        )
    }
}

@Composable
private fun TermsOfServiceContent() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "ПОЛЬЗОВАТЕЛЬСКОЕ СОГЛАШЕНИЕ (ЛИЦЕНЗИОННЫЙ ДОГОВОР)",
            color = SageGreenBright,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Публичная оферта на использование программного продукта «Каптёрка Про»\nРазработчик / Правообладатель: ИНН 250104230398",
            color = TacticalTextMuted,
            fontSize = 10.sp
        )

        SectionHeader("1. Предмет соглашения")
        LegalText(
            "1.1. Правообладатель предоставляет Пользователю неисключительную лицензию (право использования) программного обеспечения «Каптёрка Про» для ведения складского и материального учета имущества на смартфонах и планшетах под управлением ОС Android.\n" +
            "1.2. Регистрация в приложении, авторизация или нажатие кнопки «Начать работу» означает полное и безоговорочное принятие (акцепт) условий настоящего Соглашения."
        )

        SectionHeader("2. Условия использования и тарифы")
        LegalText(
            "2.1. Базовые функции складского учета (приход, расход, остатки, работа без интернета) предоставляются на безвозмездной основе.\n" +
            "2.2. Дополнительный функционал расширенной версии ПРО (многоскладская онлайн-синхронизация, неограниченная выгрузка сводных отчетов и ведомостей в формат Excel) активируется по подписке или лицензионному ключу.\n" +
            "2.3. В приложении отсутствуют скрытые списания и автопродления без согласия пользователя."
        )

        SectionHeader("3. Ограничения использования")
        LegalText(
            "Пользователю запрещается осуществлять декомпиляцию, модификацию или взлом программных средств защиты приложения, а также использовать приложение в противоправных целях."
        )

        SectionHeader("4. Ответственность и гарантии")
        LegalText(
            "4.1. Программное обеспечение предоставляется по принципу «как есть» (AS IS).\n" +
            "4.2. Правообладатель принимает все разумные меры для обеспечения надежности сохранения данных и функционирования автономного режима."
        )
    }
}

@Composable
private fun PersonalDataConsentContent() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "СОГЛАСИЕ НА ОБРАБОТКУ ПЕРСОНАЛЬНЫХ ДАННЫХ",
            color = SageGreenBright,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "В соответствии с Федеральным законом от 27.07.2006 № 152-ФЗ «О персональных данных»",
            color = TacticalTextMuted,
            fontSize = 10.sp
        )

        SectionHeader("Текст согласия субъекта персональных данных")
        LegalText(
            "Регистрируясь в мобильном приложении «Каптёрка Про», Пользователь свободно, своей волей и в своем интересе дает согласие Оператору (ИНН 250104230398) на обработку следующих данных:\n\n" +
            " • Псевдоним / Позывной;\n" +
            " • Адрес электронной почты (Email);\n" +
            " • Пользовательский идентификатор ключа подразделения.\n\n" +
            "Согласие дается на совершение следующих действий с персональными данными: сбор, запись, систематизация, накопление, хранение, уточнение (обновление, изменение), извлечение, использование, блокирование, удаление, уничтожение.\n\n" +
            "Обработка персональных данных осуществляется с использованием средств автоматизации и без использования таких средств.\n\n" +
            "Настоящее согласие действует бессрочно с момента предоставления и может быть отозвано в любой момент путем направления письменного уведомления Оператору на электронную почту: alex.666.881@gmail.com."
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = TacticalTextPrimary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun LegalText(text: String) {
    Text(
        text = text,
        color = TacticalTextSecondary,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        textAlign = TextAlign.Start
    )
}
