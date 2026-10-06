package com.alkendi.quran.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ألوان جميلة: زمردي عميق + ذهبي + كريمي
val EmeraldDark = Color(0xFF0B3D2E)
val Emerald = Color(0xFF146B4D)
val EmeraldLight = Color(0xFF1F8A63)
val Gold = Color(0xFFC9A227)
val GoldLight = Color(0xFFE9D189)
val Cream = Color(0xFFFAF6EE)
val CreamDark = Color(0xFFF0E6D2)
val Ink = Color(0xFF1C2420)
val InkSoft = Color(0xFF4A5A52)

private val LightScheme = lightColorScheme(
    primary = EmeraldDark,
    onPrimary = Color.White,
    secondary = Emerald,
    onSecondary = Color.White,
    tertiary = Gold,
    background = Cream,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = CreamDark,
    onSurfaceVariant = InkSoft
)

private val DarkScheme = darkColorScheme(
    primary = GoldLight,
    onPrimary = EmeraldDark,
    secondary = EmeraldLight,
    background = Color(0xFF0E1B16),
    onBackground = Color(0xFFF3EEDF),
    surface = Color(0xFF152822),
    onSurface = Color(0xFFF3EEDF),
    surfaceVariant = Color(0xFF1E352C),
    tertiary = Gold
)

@Composable
fun QuranTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        content = content
    )
}
