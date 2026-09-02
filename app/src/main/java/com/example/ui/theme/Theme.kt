package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = SageGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = SageGreenDark,
    onPrimaryContainer = SageGreenBright,
    secondary = TacticalTeal,
    onSecondary = Color.White,
    secondaryContainer = TacticalTealDark,
    onSecondaryContainer = TacticalTealText,
    tertiary = TacticalGold,
    onTertiary = Color.White,
    tertiaryContainer = TacticalGoldDark,
    onTertiaryContainer = TacticalGoldText,
    error = TacticalRed,
    onError = Color.White,
    errorContainer = TacticalRedDark,
    onErrorContainer = TacticalRedText,
    background = TacticalBg,
    onBackground = TacticalTextPrimary,
    surface = TacticalSurface,
    onSurface = TacticalTextPrimary,
    surfaceVariant = TacticalSurfaceLight,
    onSurfaceVariant = TacticalTextSecondary,
    outline = TacticalBorder,
    outlineVariant = TacticalBorderSubtle
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}

