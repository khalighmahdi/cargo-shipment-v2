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
    Brush.verticalGradient(listOf(Color(0xFF0B0910), Color(0xFF141020), Color(0xFF0B0910)))
} else {
    Brush.verticalGradient(listOf(Color(0xFFF7F6FB), Color(0xFFF0EDFA), Color(0xFFF7F6FB)))
}

/**
 * پس‌زمینه صفحه با هاله‌های نور بنفش (aurora glow) — حس پریمیوم
 */
@Composable
fun AuroraBackground(content: @Composable () -> Unit) {
    val dark = MaterialTheme.colorScheme.background == Color(0xFF0B0910)
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
                    .background(Color(0xFF6D28D9).copy(alpha = 0.35f), RoundedCornerShape(50))
            )
            Box(
                Modifier
                    .size(260.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 70.dp, y = 180.dp)
                    .blur(100.dp)
                    .background(Color(0xFF4338CA).copy(alpha = 0.30f), RoundedCornerShape(50))
            )
        }
        content()
    }
}
