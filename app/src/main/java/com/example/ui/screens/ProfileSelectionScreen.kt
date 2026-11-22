package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddBox
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WorkspaceProfileMode
import com.example.ui.theme.*

@Composable
fun ProfileSelectionScreen(
    initialSelection: WorkspaceProfileMode = WorkspaceProfileMode.CIVILIAN,
    onConfirmProfile: (WorkspaceProfileMode) -> Unit,
    isReconfigureMode: Boolean = false,
    hasExistingData: Boolean = false,
    onCancel: (() -> Unit)? = null,
    onConfirmReconfigure: ((WorkspaceProfileMode, Boolean) -> Unit)? = null
) {
    var selected by remember(initialSelection) { mutableStateOf(initialSelection) }
    var applyStarterSet by remember(isReconfigureMode) { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TacticalBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(modifier = Modifier.height(6.dp))

        // Top badge + optional back button in reconfigure mode
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(SageGreenDark)
                    .border(1.dp, SageGreenPrimary.copy(alpha = 0.3f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    text = if (isReconfigureMode) "СМЕНА ПРОФИЛЯ И СКЛАДОВ" else "ПЕРВЫЙ ЗАПУСК • ВЫБОР ПРОФИЛЯ",
                    color = SageGreenBright,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
            }
            if (isReconfigureMode && onCancel != null) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(TacticalSurfaceLight)
                        .border(1.dp, TacticalBorderSubtle, RoundedCornerShape(12.dp))
                        .clickable(onClick = onCancel)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Назад",
                        tint = TacticalTextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Назад", color = TacticalTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Text(
            text = if (isReconfigureMode) "Выберите профиль склада" else "Как вы будете вести склад?",
            color = TacticalTextPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 31.sp
        )
        Text(
            text = if (isReconfigureMode) {
                "Выберите режим учёта — подстроим склады, кнопки операций, справочник и формы Excel."
            } else {
                "Подберём готовый справочник, стартовые склады и нужные бланки отчётов Excel. Профиль можно переключить позже в настройках."
            },
            color = TacticalTextSecondary,
            fontSize = 13.5.sp,
            lineHeight = 19.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        ProfileOptionCard(
            mode = WorkspaceProfileMode.CIVILIAN,
            selected = selected == WorkspaceProfileMode.CIVILIAN,
            title = "Гражданский склад / Мастерская",
            subtitle = "Универсальный учёт инструмента, расходников, СИЗ и ТМЦ",
            description = "Готовый справочник (64 позиции в 8 категориях), 3 склада и официальные бланки: накладная М-11 / ТОРГ-13, ведомость выдачи МБ-7, акт МБ-8.",
            chips = listOf("64 позиции", "8 категорий", "М-11 / МБ-7 / МБ-8"),
            icon = Icons.Rounded.Build,
            accent = TacticalEmerald,
            accentBg = TacticalEmeraldDark,
            onClick = { selected = WorkspaceProfileMode.CIVILIAN },
            testTag = "profile_option_civilian"
        )

        ProfileOptionCard(
            mode = WorkspaceProfileMode.MILITARY,
            selected = selected == WorkspaceProfileMode.MILITARY,
            title = "Воинский учёт / Подразделение",
            subtitle = "Штатные службы МТО и уставная отчётность МО РФ",
            description = "Полный каталог по службам (РАВ, БПЛА, Связь, Вещевая, ГСМ, Мед), КЗ и точки, точные бланки Форма № 8 (ОКУД 6002203) и Форма № 18 (ОКУД 0504042).",
            chips = listOf("Все службы", "Форма № 8", "Форма № 18"),
            icon = Icons.Rounded.MilitaryTech,
            accent = TacticalGold,
            accentBg = TacticalGoldDark,
            onClick = { selected = WorkspaceProfileMode.MILITARY },
            testTag = "profile_option_military"
        )

        ProfileOptionCard(
            mode = WorkspaceProfileMode.EMPTY,
            selected = selected == WorkspaceProfileMode.EMPTY,
            title = "Пустой склад (с нуля)",
            subtitle = "Чистый лист — всё на ваше усмотрение",
            description = "Один чистый «Основной склад» без предустановленных товаров и чужих категорий. Удобно, если хотите завести только свою номенклатуру.",
            chips = listOf("1 склад", "0 лишних строк", "Свободный учёт"),
            icon = Icons.Rounded.AddBox,
            accent = TacticalTeal,
            accentBg = TacticalTealDark,
            onClick = { selected = WorkspaceProfileMode.EMPTY },
            testTag = "profile_option_empty"
        )

        if (isReconfigureMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(TacticalSurface)
                    .border(1.dp, TacticalBorderSubtle, RoundedCornerShape(14.dp))
                    .clickable { applyStarterSet = !applyStarterSet }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Применить стартовые склады и каталог",
                        color = TacticalTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (hasExistingData) {
                            "Ваши остатки, операции и заявки сохранятся — обновятся только стартовые шаблоны"
                        } else {
                            "Сразу загрузит склады, категории и справочник выбранного профиля"
                        },
                        color = TacticalTextMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = applyStarterSet,
                    onCheckedChange = { applyStarterSet = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = SageGreenPrimary
                    )
                )
            }
        } else {
            // Upgrade safety note
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(TacticalSurfaceLight)
                    .border(1.dp, TacticalBorderSubtle, RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shield,
                    contentDescription = null,
                    tint = SageGreenPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "При обновлении уже установленной версии старые склады, остатки и журнал никогда не меняются.",
                    color = TacticalTextSecondary,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        com.example.ui.components.Volumetric3DButton(
            text = if (isReconfigureMode) "Применить профиль" else "Начать работу",
            onClick = {
                if (isReconfigureMode && onConfirmReconfigure != null) {
                    onConfirmReconfigure(selected, applyStarterSet)
                } else {
                    onConfirmProfile(selected)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("confirm_profile_button"),
            height = 56.dp,
            cornerRadius = 18.dp
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ProfileOptionCard(
    mode: WorkspaceProfileMode,
    selected: Boolean,
    title: String,
    subtitle: String,
    description: String,
    chips: List<String>,
    icon: ImageVector,
    accent: Color,
    accentBg: Color,
    onClick: () -> Unit,
    testTag: String
) {
    com.example.ui.components.Card3DSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag),
        cornerRadius = 22.dp,
        accentBorder = if (selected) accent else null,
        elevated = selected
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (selected) Modifier.background(
                        Brush.horizontalGradient(
                            listOf(SageGreenPrimary.copy(alpha = 0.06f), Color.Transparent)
                        )
                    ) else Modifier
                )
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accentBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = TacticalTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        color = TacticalTextMuted,
                        fontSize = 11.5.sp
                    )
                }
                if (selected) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Выбрано",
                        tint = SageGreenPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, TacticalBorder, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = description,
                color = TacticalTextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                chips.forEach { chip ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) accentBg else TacticalSurfaceLight)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = chip,
                            color = if (selected) accent else TacticalTextSecondary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
