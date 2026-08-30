package com.example.cargo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand colors
val Purple = Color(0xFF7C4DFF)
val PurpleDark = Color(0xFF651FFF)
val Blue = Color(0xFF448AFF)
val Amber = Color(0xFFFFB300)
val Green = Color(0xFF43A047)
val Red = Color(0xFFE53935)

private val LightColors = lightColorScheme(
    primary = Purple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE1BEE7),
    onPrimaryContainer = Color(0xFF311B92),
    secondary = Blue,
    tertiary = Green,
    background = Color(0xFFF3F0FF),
    surface = Color.White,
    surfaceVariant = Color(0xFFEDE7F6),
    onSurfaceVariant = Color(0xFF49454F)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB388FF),
    onPrimary = Color(0xFF311B92),
    primaryContainer = Color(0xFF4527A0),
    onPrimaryContainer = Color(0xFFE1BEE7),
    secondary = Color(0xFF82B1FF),
    tertiary = Color(0xFFA5D6A7),
    background = Color(0xFF121016),
    surface = Color(0xFF1D1B22),
    surfaceVariant = Color(0xFF2A2733),
    onSurfaceVariant = Color(0xFFCAC4D0)
)

@Composable
fun CargoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
