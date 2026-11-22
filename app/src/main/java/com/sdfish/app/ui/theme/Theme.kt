package com.sdfish.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF00696B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9CF1F1),
    onPrimaryContainer = Color(0xFF002020),
    secondary = Color(0xFF4A6363),
    secondaryContainer = Color(0xFFCCE8E7),
    onSecondaryContainer = Color(0xFF051F1F),
    tertiary = Color(0xFF4B607C),
    background = Color(0xFFF4FBFA),
    surface = Color(0xFFF4FBFA),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF80D5D4),
    onPrimary = Color(0xFF003737),
    primaryContainer = Color(0xFF004F50),
    onPrimaryContainer = Color(0xFF9CF1F1),
    secondary = Color(0xFFB0CCCB),
    secondaryContainer = Color(0xFF324B4B),
    onSecondaryContainer = Color(0xFFCCE8E7),
    tertiary = Color(0xFFB3C8E8),
    background = Color(0xFF0E1415),
    surface = Color(0xFF0E1415),
)

@Composable
fun SdFishTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
