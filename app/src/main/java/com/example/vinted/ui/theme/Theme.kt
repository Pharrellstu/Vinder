package com.example.vinted.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VinderColorScheme = lightColorScheme(
    primary = VinderAzure,
    onPrimary = Color.White,
    secondary = VinderAmber,
    onSecondary = Grey11,
    background = Grey97,
    onBackground = Grey11,
    surface = Color.White,
    onSurface = Grey11,
    outline = Grey91,
)

@Composable
fun VintedTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VinderColorScheme,
        typography = Typography,
        content = content,
    )
}
