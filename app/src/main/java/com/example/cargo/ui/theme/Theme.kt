package com.example.cargo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Brand palette (Premium Dark / Revolut-Linear inspired) ──
val Purple = Color(0xFF8B5CF6)      // violet-500 — main accent
val PurpleDark = Color(0xFF6D28D9)  // violet-700
val PurpleDeep = Color(0xFF4C1D95)  // violet-900
val VioletGlow = Color(0xFFA78BFA)  // violet-400 — dark-mode accent
val Blue = Color(0xFF60A5FA)
val Amber = Color(0xFFFBBF24)
val Green = Color(0xFF34D399)
val Red = Color(0xFFF87171)
val Cyan = Color(0xFF22D3EE)

// Gradient stops for hero cards / buttons
val GradTop = Color(0xFF7C3AED)
val GradBottom = Color(0xFF4F46E5)

private val LightColors = lightColorScheme(
    primary = PurpleDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF2E1065),
    secondary = Color(0xFF4F46E5),
    tertiary = Color(0xFF059669),
    background = Color(0xFFF7F6FB),
    surface = Color.White,
    surfaceVariant = Color(0xFFEFEDF7),
    onSurfaceVariant = Color(0xFF4B4763),
    outline = Color(0xFFC9C4DE)
)

private val DarkColors = darkColorScheme(
    primary = VioletGlow,
    onPrimary = Color(0xFF1E1042),
    primaryContainer = Color(0xFF3B2A6B),
    onPrimaryContainer = Color(0xFFE4DBFF),
    secondary = Color(0xFF93C5FD),
    tertiary = Color(0xFF6EE7B7),
    background = Color(0xFF0B0910),   // near-black with violet undertone
    surface = Color(0xFF15121D),      // card surface
    surfaceVariant = Color(0xFF211D2E),
    onSurfaceVariant = Color(0xFFB9B3C9),
    outline = Color(0xFF35304A)
)

@Composable
fun CargoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
