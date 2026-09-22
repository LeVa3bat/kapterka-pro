package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class TacticalColors(
    val isDark: Boolean,
    val bg: Color,
    val surface: Color,
    val surfaceLight: Color,
    val surfaceElevated: Color,
    val border: Color,
    val borderSubtle: Color,
    val sageGreenPrimary: Color,
    val sageGreenBright: Color,
    val sageGreenDark: Color,
    val sageGreenContainer: Color,
    val gold: Color,
    val goldDark: Color,
    val goldText: Color,
    val red: Color,
    val redDark: Color,
    val redText: Color,
    val teal: Color,
    val tealDark: Color,
    val tealText: Color,
    val greenBtn: Color,
    val greenBtnText: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textDim: Color,
    val demoBanner: Color,
    val demoBannerBorder: Color,
    val demoBannerText: Color,
    val proBanner: Color,
    val proBannerBorder: Color,
    val proBannerText: Color
)

// LIGHT TACTICAL PALETTE (Светлая дневная тема: мягкий оливковый/шалфейный фон, чёткий текст)
val LightTacticalColors = TacticalColors(
    isDark = false,
    bg = Color(0xFFF6F8FB),
    surface = Color(0xFFFFFFFF),
    surfaceLight = Color(0xFFF1F4F7),
    surfaceElevated = Color(0xFFFFFFFF),
    border = Color(0xFFDCE3EA),
    borderSubtle = Color(0xFFE9EEF3),
    sageGreenPrimary = Color(0xFF5B5CE2),
    sageGreenBright = Color(0xFF4849C8),
    sageGreenDark = Color(0xFFEEEEFF),
    sageGreenContainer = Color(0xFFDADBFF),
    gold = Color(0xFFD6922E),
    goldDark = Color(0xFFFFF3D9),
    goldText = Color(0xFF875A11),
    red = Color(0xFFD84A4A),
    redDark = Color(0xFFFFE8E8),
    redText = Color(0xFF9F2E2E),
    teal = Color(0xFF287DB8),
    tealDark = Color(0xFFE8F3FA),
    tealText = Color(0xFF1E638F),
    greenBtn = Color(0xFF5B5CE2),
    greenBtnText = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF172027),
    textSecondary = Color(0xFF44515B),
    textMuted = Color(0xFF71808B),
    textDim = Color(0xFF9AA6AF),
    demoBanner = Color(0xFFFFF6DD),
    demoBannerBorder = Color(0xFFE2B24F),
    demoBannerText = Color(0xFF7A5717),
    proBanner = Color(0xFFF0F0FF),
    proBannerBorder = Color(0xFF8D8EFF),
    proBannerText = Color(0xFF4445B8)
)

// DARK TACTICAL PALETTE (Боевая ночная / Тёмная тактическая тема: глубокий матовый фон, фосфорные акценты)
// DARK MODERN PALETTE
val DarkTacticalColors = TacticalColors(
    isDark = true,
    bg = Color(0xFF0F1216),
    surface = Color(0xFF171B20),
    surfaceLight = Color(0xFF20262D),
    surfaceElevated = Color(0xFF262D35),
    border = Color(0xFF343C45),
    borderSubtle = Color(0xFF292F36),
    sageGreenPrimary = Color(0xFF9294FF),
    sageGreenBright = Color(0xFFB1B2FF),
    sageGreenDark = Color(0xFF292B62),
    sageGreenContainer = Color(0xFF383B80),
    gold = Color(0xFFF0B457),
    goldDark = Color(0xFF4A3516),
    goldText = Color(0xFFFFD991),
    red = Color(0xFFF16C6C),
    redDark = Color(0xFF482222),
    redText = Color(0xFFFFB0B0),
    teal = Color(0xFF61B6E8),
    tealDark = Color(0xFF173647),
    tealText = Color(0xFFA9DCF7),
    greenBtn = Color(0xFF9294FF),
    greenBtnText = Color(0xFF11132F),
    textPrimary = Color(0xFFF1F5F7),
    textSecondary = Color(0xFFC1CBD2),
    textMuted = Color(0xFF8F9BA5),
    textDim = Color(0xFF64717C),
    demoBanner = Color(0xFF413319),
    demoBannerBorder = Color(0xFFD8A64F),
    demoBannerText = Color(0xFFFFD891),
    proBanner = Color(0xFF242653),
    proBannerBorder = Color(0xFF9294FF),
    proBannerText = Color(0xFFC5C6FF)
)

val LocalTacticalColors = staticCompositionLocalOf { LightTacticalColors }

// Dynamic properties resolving via current LocalTacticalColors
val TacticalBg: Color @Composable @ReadOnlyComposable get() = LocalTacticalColors.current.bg
val TacticalSurface: Color @Composable @ReadOnlyComposable get() = LocalTacticalColors.current.surface
val TacticalSurfaceLight: Color @Composable @ReadOnlyComposable get() = LocalTacticalColors.current.surfaceLight
val TacticalSurfaceElevated: Color @Composable @ReadOnlyComposable get() = LocalTacticalColors.current.surfaceElevated
val TacticalBorder: Color @Composable @ReadOnlyComposable get() = LocalTacticalColors.current.border
val TacticalBorderSubtle: Color @Composable @ReadOnlyComposable get() = LocalTacticalColors.current.borderSubtle

val SageGreenPrimary: Color @Composable @ReadOnlyComposable get() = LocalTacticalColors.current.sageGreenPrimary
val SageGreenBright: Color @Composable @ReadOnlyComposable get() = LocalTacticalColors.current.sageGreenBright
val SageGreenDark: Color @Composable @ReadOnlyComposable get() = LocalTacticalColors.current.sageGreenDark
val SageGreenContainer: Color @Composable @ReadOnlyComposable get() = LocalTacticalColors.current.sageGreenContainer

val TacticalGold: Color @Composable @ReadOnlyComposable get() = LocalTacticalColors.current.gold
val TacticalGoldDark: Color @Composable @ReadOnlyComposable get() = LocalTacticalColors.current.goldDark
val TacticalGoldText: Color @Composable @ReadOnlyComposable get() = LocalTacticalColors.current.goldText

val TacticalRed: Color @Composable @ReadOnlyComposable get() = LocalTacticalColors.current.red
val TacticalRedDark: Color @Composable @ReadOnlyComposable get() = LocalTacticalColors.current.redDark
val TacticalRedText: Color @Composable @ReadOnlyComposable get() = LocalTacticalColors.current.redText

val TacticalTeal: Color @Composable @ReadOnlyComposable get() = LocalTacticalColors.current.teal
val TacticalTealDark: Color @Composable @ReadOnlyComposable get() = LocalTacticalColors.current.tealDark
val TacticalTealText: Color @Composable @ReadOnlyComposable get() = LocalTacticalColors.current.tealText

val TacticalGreenBtn: Color @Composable @ReadOnlyComposable get() = LocalTacticalColors.current.greenBtn
val TacticalGreenBtnText: Color @Composable @ReadOnlyComposable get() = LocalTacticalColors.current.greenBtnText

val TacticalTextPrimary: Color @Composable @ReadOnlyComposable get() = LocalTacticalColors.current.textPrimary
val TacticalTextSecondary: Color @Composable @ReadOnlyComposable get() = LocalTacticalColors.current.textSecondary
val TacticalTextMuted: Color @Composable @ReadOnlyComposable get() = LocalTacticalColors.current.textMuted
val TacticalTextDim: Color @Composable @ReadOnlyComposable get() = LocalTacticalColors.current.textDim

val TacticalDemoBanner: Color @Composable @ReadOnlyComposable get() = LocalTacticalColors.current.demoBanner
val TacticalDemoBannerBorder: Color @Composable @ReadOnlyComposable get() = LocalTacticalColors.current.demoBannerBorder
val TacticalDemoBannerText: Color @Composable @ReadOnlyComposable get() = LocalTacticalColors.current.demoBannerText

val TacticalProBanner: Color @Composable @ReadOnlyComposable get() = LocalTacticalColors.current.proBanner
val TacticalProBannerBorder: Color @Composable @ReadOnlyComposable get() = LocalTacticalColors.current.proBannerBorder
val TacticalProBannerText: Color @Composable @ReadOnlyComposable get() = LocalTacticalColors.current.proBannerText
