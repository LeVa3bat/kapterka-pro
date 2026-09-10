package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = LightTacticalColors.sageGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = LightTacticalColors.sageGreenDark,
    onPrimaryContainer = LightTacticalColors.sageGreenBright,
    secondary = LightTacticalColors.teal,
    onSecondary = Color.White,
    secondaryContainer = LightTacticalColors.tealDark,
    onSecondaryContainer = LightTacticalColors.tealText,
    tertiary = LightTacticalColors.gold,
    onTertiary = Color.White,
    tertiaryContainer = LightTacticalColors.goldDark,
    onTertiaryContainer = LightTacticalColors.goldText,
    error = LightTacticalColors.red,
    onError = Color.White,
    errorContainer = LightTacticalColors.redDark,
    onErrorContainer = LightTacticalColors.redText,
    background = LightTacticalColors.bg,
    onBackground = LightTacticalColors.textPrimary,
    surface = LightTacticalColors.surface,
    onSurface = LightTacticalColors.textPrimary,
    surfaceVariant = LightTacticalColors.surfaceLight,
    onSurfaceVariant = LightTacticalColors.textSecondary,
    outline = LightTacticalColors.border,
    outlineVariant = LightTacticalColors.borderSubtle
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkTacticalColors.sageGreenPrimary,
    onPrimary = DarkTacticalColors.greenBtnText,
    primaryContainer = DarkTacticalColors.sageGreenDark,
    onPrimaryContainer = DarkTacticalColors.sageGreenBright,
    secondary = DarkTacticalColors.teal,
    onSecondary = Color.Black,
    secondaryContainer = DarkTacticalColors.tealDark,
    onSecondaryContainer = DarkTacticalColors.tealText,
    tertiary = DarkTacticalColors.gold,
    onTertiary = Color.Black,
    tertiaryContainer = DarkTacticalColors.goldDark,
    onTertiaryContainer = DarkTacticalColors.goldText,
    error = DarkTacticalColors.red,
    onError = Color.Black,
    errorContainer = DarkTacticalColors.redDark,
    onErrorContainer = DarkTacticalColors.redText,
    background = DarkTacticalColors.bg,
    onBackground = DarkTacticalColors.textPrimary,
    surface = DarkTacticalColors.surface,
    onSurface = DarkTacticalColors.textPrimary,
    surfaceVariant = DarkTacticalColors.surfaceLight,
    onSurfaceVariant = DarkTacticalColors.textSecondary,
    outline = DarkTacticalColors.border,
    outlineVariant = DarkTacticalColors.borderSubtle
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val tacticalColors = if (darkTheme) DarkTacticalColors else LightTacticalColors
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalTacticalColors provides tacticalColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
