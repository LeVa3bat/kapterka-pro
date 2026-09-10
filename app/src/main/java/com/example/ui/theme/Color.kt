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
    bg = Color(0xFFE8EDE7),
    surface = Color(0xFFF3F7F2),
    surfaceLight = Color(0xFFDEE5DD),
    surfaceElevated = Color(0xFFFAFBF9),
    border = Color(0xFFBAC7B8),
    borderSubtle = Color(0xFFCDD8CC),
    sageGreenPrimary = Color(0xFF047857),
    sageGreenBright = Color(0xFF065F46),
    sageGreenDark = Color(0xFFD1FAE5),
    sageGreenContainer = Color(0xFFA7F3D0),
    gold = Color(0xFFB45309),
    goldDark = Color(0xFFFEF3C7),
    goldText = Color(0xFF78350F),
    red = Color(0xFFB91C1C),
    redDark = Color(0xFFFEE2E2),
    redText = Color(0xFF7F1D1D),
    teal = Color(0xFF0369A1),
    tealDark = Color(0xFFE0F2FE),
    tealText = Color(0xFF075985),
    greenBtn = Color(0xFF047857),
    greenBtnText = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF141F18),
    textSecondary = Color(0xFF2E3E34),
    textMuted = Color(0xFF5A6E62),
    textDim = Color(0xFF7E9386),
    demoBanner = Color(0xFFFEF3C7),
    demoBannerBorder = Color(0xFFF59E0B),
    demoBannerText = Color(0xFF78350F),
    proBanner = Color(0xFFD1FAE5),
    proBannerBorder = Color(0xFF059669),
    proBannerText = Color(0xFF064E3B)
)

// DARK TACTICAL PALETTE (Боевая ночная / Тёмная тактическая тема: глубокий матовый фон, фосфорные акценты)
val DarkTacticalColors = TacticalColors(
    isDark = true,
    bg = Color(0xFF0D1411),
    surface = Color(0xFF15201A),
    surfaceLight = Color(0xFF1C2C24),
    surfaceElevated = Color(0xFF24362C),
    border = Color(0xFF2D4236),
    borderSubtle = Color(0xFF1E2F26),
    sageGreenPrimary = Color(0xFF10B981),
    sageGreenBright = Color(0xFF34D399),
    sageGreenDark = Color(0xFF064E3B),
    sageGreenContainer = Color(0xFF047857),
    gold = Color(0xFFF59E0B),
    goldDark = Color(0xFF451A03),
    goldText = Color(0xFFFDE68A),
    red = Color(0xFFEF4444),
    redDark = Color(0xFF450A0A),
    redText = Color(0xFFFCA5A5),
    teal = Color(0xFF38BDF8),
    tealDark = Color(0xFF082F49),
    tealText = Color(0xFFBAE6FD),
    greenBtn = Color(0xFF10B981),
    greenBtnText = Color(0xFF022C22),
    textPrimary = Color(0xFFE2E8F0),
    textSecondary = Color(0xFFCBD5E1),
    textMuted = Color(0xFF94A3B8),
    textDim = Color(0xFF64748B),
    demoBanner = Color(0xFF451A03),
    demoBannerBorder = Color(0xFFF59E0B),
    demoBannerText = Color(0xFFFDE68A),
    proBanner = Color(0xFF064E3B),
    proBannerBorder = Color(0xFF10B981),
    proBannerText = Color(0xFFA7F3D0)
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
