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
    val proBannerText: Color,
    val income: Color,
    val incomeDark: Color,
    val incomeText: Color,
    val heroGradientStart: Color,
    val heroGradientEnd: Color
)

// LIGHT EMERALD-SAGE PALETTE (Светлая тема первого варианта: чистый светлый фон, изумрудные акценты)
val LightTacticalColors = TacticalColors(
    isDark = false,
    bg = Color(0xFFF4F7F6),
    surface = Color(0xFFFFFFFF),
    surfaceLight = Color(0xFFEBF1EE),
    surfaceElevated = Color(0xFFFFFFFF),
    border = Color(0xFFD5E1DC),
    borderSubtle = Color(0xFFE4ECE8),
    sageGreenPrimary = Color(0xFF108565),
    sageGreenBright = Color(0xFF0A664D),
    sageGreenDark = Color(0xFFE3F5EE),
    sageGreenContainer = Color(0xFFC8ECDE),
    gold = Color(0xFFD98B1F),
    goldDark = Color(0xFFFFF4DC),
    goldText = Color(0xFF8C560B),
    red = Color(0xFFD64545),
    redDark = Color(0xFFFFEAEA),
    redText = Color(0xFF9F2626),
    teal = Color(0xFF0F766E),
    tealDark = Color(0xFFE3F5F3),
    tealText = Color(0xFF0B5C56),
    greenBtn = Color(0xFF108565),
    greenBtnText = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF13201B),
    textSecondary = Color(0xFF3E5049),
    textMuted = Color(0xFF667A72),
    textDim = Color(0xFF93A59E),
    demoBanner = Color(0xFFFFF6DE),
    demoBannerBorder = Color(0xFFE4B44C),
    demoBannerText = Color(0xFF7D5310),
    proBanner = Color(0xFFE5F7F0),
    proBannerBorder = Color(0xFF4BB493),
    proBannerText = Color(0xFF0B664D),
    income = Color(0xFF059669),
    incomeDark = Color(0xFFE1F7EE),
    incomeText = Color(0xFF046C4E),
    heroGradientStart = Color(0xFF0E6E52),
    heroGradientEnd = Color(0xFF0F766E)
)

// DARK EMERALD-GRAPHITE PALETTE (Основной первый вариант: глубокий графит #091014 + изумруд #10B981 + золото #F59E0B)
val DarkTacticalColors = TacticalColors(
    isDark = true,
    bg = Color(0xFF091014),
    surface = Color(0xFF101A20),
    surfaceLight = Color(0xFF17242C),
    surfaceElevated = Color(0xFF1E2D36),
    border = Color(0xFF283B45),
    borderSubtle = Color(0xFF1E2E36),
    sageGreenPrimary = Color(0xFF10B981),
    sageGreenBright = Color(0xFF34D399),
    sageGreenDark = Color(0xFF102F26),
    sageGreenContainer = Color(0xFF154336),
    gold = Color(0xFFF59E0B),
    goldDark = Color(0xFF382812),
    goldText = Color(0xFFFCD34D),
    red = Color(0xFFF87171),
    redDark = Color(0xFF3A1A20),
    redText = Color(0xFFFCA5A5),
    teal = Color(0xFF2DD4BF),
    tealDark = Color(0xFF102E33),
    tealText = Color(0xFF67E8F9),
    greenBtn = Color(0xFF10B981),
    greenBtnText = Color(0xFF04160F),
    textPrimary = Color(0xFFF1F7F5),
    textSecondary = Color(0xFFB8CCD5),
    textMuted = Color(0xFF8CA4B0),
    textDim = Color(0xFF5D7480),
    demoBanner = Color(0xFF2F2310),
    demoBannerBorder = Color(0xFFD99B26),
    demoBannerText = Color(0xFFFDE68A),
    proBanner = Color(0xFF0F2D24),
    proBannerBorder = Color(0xFF34D399),
    proBannerText = Color(0xFFA7F3D0),
    income = Color(0xFF10B981),
    incomeDark = Color(0xFF102F26),
    incomeText = Color(0xFF34D399),
    heroGradientStart = Color(0xFF0D4234),
    heroGradientEnd = Color(0xFF0F5E56)
)

val LocalTacticalColors = staticCompositionLocalOf { DarkTacticalColors }

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

val TacticalIncome: Color @Composable @ReadOnlyComposable get() = LocalTacticalColors.current.income
val TacticalIncomeDark: Color @Composable @ReadOnlyComposable get() = LocalTacticalColors.current.incomeDark
val TacticalIncomeText: Color @Composable @ReadOnlyComposable get() = LocalTacticalColors.current.incomeText
val TacticalEmerald: Color @Composable @ReadOnlyComposable get() = LocalTacticalColors.current.income
val TacticalEmeraldDark: Color @Composable @ReadOnlyComposable get() = LocalTacticalColors.current.incomeDark
val TacticalEmeraldText: Color @Composable @ReadOnlyComposable get() = LocalTacticalColors.current.incomeText
val HeroGradientStart: Color @Composable @ReadOnlyComposable get() = LocalTacticalColors.current.heroGradientStart
val HeroGradientEnd: Color @Composable @ReadOnlyComposable get() = LocalTacticalColors.current.heroGradientEnd
