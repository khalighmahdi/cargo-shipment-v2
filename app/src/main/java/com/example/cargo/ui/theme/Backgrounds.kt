package com.example.cargo.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** گرادیان مارک — برای دکمه اصلی، هدر و کارت‌های قهرمان */
fun brandGradient(): Brush = Brush.linearGradient(
    colors = listOf(GradTop, GradBottom),
    start = Offset(0f, 0f),
    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
)

/** گرادیان پس‌زمینه صفحه */
fun bgGradient(dark: Boolean): Brush = if (dark) {
    Brush.verticalGradient(listOf(Color(0xFF070D0A), Color(0xFF0F1713), Color(0xFF070D0A)))
} else {
    Brush.verticalGradient(listOf(Color(0xFFF6FBF7), Color(0xFFE8F2EB), Color(0xFFF6FBF7)))
}

/**
 * پس‌زمینه صفحه با هاله‌های نور سبز/طلایی (aurora glow) — حس پریمیوم شریفان
 */
@Composable
fun AuroraBackground(content: @Composable () -> Unit) {
    val dark = MaterialTheme.colorScheme.background == Color(0xFF070D0A)
    Box(
        Modifier
            .fillMaxSize()
            .background(bgGradient(dark))
    ) {
        if (dark) {
            Box(
                Modifier
                    .size(280.dp)
                    .align(Alignment.TopStart)
                    .offset(x = (-80).dp, y = (-60).dp)
                    .blur(90.dp)
                    .background(Color(0xFF0E7A46).copy(alpha = 0.35f), RoundedCornerShape(50))
            )
            Box(
                Modifier
                    .size(220.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 70.dp, y = 180.dp)
                    .blur(100.dp)
                    .background(Color(0xFFD4AF37).copy(alpha = 0.22f), RoundedCornerShape(50))
            )
            Box(
                Modifier
                    .size(200.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = (-40).dp, y = 40.dp)
                    .blur(80.dp)
                    .background(Color(0xFF34D399).copy(alpha = 0.18f), RoundedCornerShape(50))
            )
        } else {
            Box(
                Modifier
                    .size(260.dp)
                    .align(Alignment.TopStart)
                    .offset(x = (-60).dp, y = (-50).dp)
                    .blur(100.dp)
                    .background(Color(0xFF0E7A46).copy(alpha = 0.12f), RoundedCornerShape(50))
            )
            Box(
                Modifier
                    .size(200.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 50.dp, y = 160.dp)
                    .blur(90.dp)
                    .background(Color(0xFFD4AF37).copy(alpha = 0.10f), RoundedCornerShape(50))
            )
        }
        content()
    }
}