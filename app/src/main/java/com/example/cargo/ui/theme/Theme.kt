package com.example.cargo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Brand palette: Sharifan (emerald green + gold, from the shop logo) ──
val Emerald = Color(0xFF0E7A46)       // main green from the شریفان calligraphy
val EmeraldDark = Color(0xFF0B5C35)
val EmeraldDeep = Color(0xFF07402A)
val EmeraldGlow = Color(0xFF34D399)   // bright green for dark-mode accent
val Gold = Color(0xFFD4AF37)          // logo ornament gold
val GoldSoft = Color(0xFFE6C757)
val Blue = Color(0xFF60A5FA)
val Amber = Color(0xFFFBBF24)
val Green = Color(0xFF34D399)
val Red = Color(0xFFF87171)
val Cyan = Color(0xFF22D3EE)

// Gradient stops for hero cards / buttons
val GradTop = Color(0xFF128A50)
val GradBottom = Color(0xFF0A5C3C)

private val LightColors = lightColorScheme(
    primary = EmeraldDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7F2E2),
    onPrimaryContainer = Color(0xFF06301C),
    secondary = Color(0xFF9A7B1C),
    tertiary = Color(0xFF0E7A46),
    background = Color(0xFFF6FBF7),
    surface = Color.White,
    surfaceVariant = Color(0xFFE8F2EB),
    onSurfaceVariant = Color(0xFF3F5A4B),
    outline = Color(0xFFBFD4C6)
)

private val DarkColors = darkColorScheme(
    primary = EmeraldGlow,
    onPrimary = Color(0xFF00210F),
    primaryContainer = Color(0xFF0B4A2E),
    onPrimaryContainer = Color(0xFFB9F2D0),
    secondary = GoldSoft,
    tertiary = Color(0xFF6EE7B7),
    background = Color(0xFF070D0A),   // near-black with green undertone
    surface = Color(0xFF0F1713),      // card surface
    surfaceVariant = Color(0xFF17241D),
    onSurfaceVariant = Color(0xFFB4C8BB),
    outline = Color(0xFF2B3D33)
)

@Composable
fun CargoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}