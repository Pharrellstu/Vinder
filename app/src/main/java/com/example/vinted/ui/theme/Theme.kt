package com.example.vinted.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = VinderAzure,
    secondary = VinderAmber,
    onSecondary = Color(0xFF1C1C1E),
    background = Color(0xFFF5F6F8),
    onBackground = Color(0xFF1C1C1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1C1E),
    outline = Color(0xFFE5E5EA),
)

private val DarkColorScheme = darkColorScheme(
    primary = VinderAzure,
    secondary = VinderAmber,
    onSecondary = Color.White,
    background = Color(0xFF000000),
    onBackground = Color.White,
    surface = Color(0xFF121212),
    onSurface = Color.White,
    outline = Color(0xFF2C2C2E),
)

@Composable
fun VintedTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (VinderPalette.dark) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}
