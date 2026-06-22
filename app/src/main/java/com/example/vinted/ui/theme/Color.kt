package com.example.vinted.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Holds the active light/dark flag. It is a Compose [mutableStateOf], so the semantic colour tokens
 * below (which read it via their getters) recompose every consumer the instant the flag flips —
 * even though screens reference the tokens directly rather than through `MaterialTheme`.
 */
object VinderPalette {
    var dark by mutableStateOf(false)
}

private fun themed(light: Color, dark: Color): Color = if (VinderPalette.dark) dark else light

// Brand colours — identical in both themes.
val VinderAzure = Color(0xFF4E8098)
val VinderAzureLight = Color(0xFFD6E8F5)
val VinderGreen = Color(0xFF4CAF50)
val VinderAmber = Color(0xFFFFC107)
val VinderError = Color(0xFFD32F2F)

// Semantic tokens. Light value first, dark value second. Dark mode = black screens, white text.
/** App background (screens). */
val Grey97: Color get() = themed(Color(0xFFF5F6F8), Color(0xFF000000))

/** Card / sheet / app-bar / input surface that sits on the background. */
val SurfaceColor: Color get() = themed(Color(0xFFFFFFFF), Color(0xFF121212))

val Grey95: Color get() = themed(Color(0xFFF0F0F5), Color(0xFF1C1C1E))
val Grey94: Color get() = themed(Color(0xFFEFEFF2), Color(0xFF1C1C1E))
val Grey91: Color get() = themed(Color(0xFFE5E5EA), Color(0xFF2C2C2E))
val Grey82: Color get() = themed(Color(0xFFD1D1D6), Color(0xFF3A3A3C))

/** Mid-grey secondary text — legible on both backgrounds, so it stays put. */
val Grey57 = Color(0xFF8E8E93)

/** Darker secondary text — lightened in dark mode so it stays legible on black. */
val Grey36: Color get() = themed(Color(0xFF5A5A60), Color(0xFFC7C7CC))

/** Primary text — near-black on light, white on dark. */
val Grey11: Color get() = themed(Color(0xFF1C1C1E), Color(0xFFFFFFFF))

val inputColor: Color get() = themed(Color(0xFFE5EEF3), Color(0xFF1C1C1E))
val grayColor: Color get() = themed(Color(0xFFD9D9D9), Color(0xFF2C2C2E))
val boxDivColor: Color get() = themed(Color(0xFFFDF8F8), Color(0xFF1C1C1E))

// Status badge palette — kept constant in both themes.
val StatusPendingBg = Color(0xFFFFF8E1)
val StatusPendingText = Color(0xFFF57F17)
val StatusAcceptedBg = Color(0xFFE8F5E9)
val StatusAcceptedText = Color(0xFF2E7D32)
val StatusRejectedBg = Color(0xFFFFEBEE)
val StatusRejectedText = Color(0xFFC62828)
val StatusCancelledBg = Color(0xFFF5F5F5)
