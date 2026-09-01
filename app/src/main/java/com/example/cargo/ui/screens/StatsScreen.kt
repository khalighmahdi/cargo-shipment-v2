package com.example.cargo.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cargo.R
import com.example.cargo.data.Shipment
import com.example.cargo.ui.theme.AuroraBackground
import com.example.cargo.ui.theme.EmeraldGlow
import com.example.cargo.ui.theme.Gold
import com.example.cargo.ui.theme.brandGradient
import com.example.cargo.util.JalaliDate
import com.example.cargo.viewmodel.ShipmentViewModel

private val monthLabels = listOf(
    "فرو", "ارد", "خرد", "تیر", "مرد", "شهر",
    "مهر", "آبا", "آذر", "دی", "بهم", "اسف"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: ShipmentViewModel,
    onBack: () -> Unit
) {
    val all by viewModel.allShipments.collectAsState()
    val today = remember { JalaliDate.today() }
    var year by remember { mutableStateOf(today.year) }

    val yearShipments = remember(all, year) { all.filter { it.jalaliYear == year } }

    // monthly counts
    val monthly = remember(yearShipments) {
        val m = IntArray(12)
        yearShipments.forEach { if (it.jalaliMonth in 1..12) m[it.jalaliMonth - 1]++ }
        m.toList()
    }
    val maxMonthly = (monthly.maxOrNull() ?: 0).coerceAtLeast(1)

    // top customers (گیرنده — چون خود کاربر فرستنده است)
    val topSenders = remember(yearShipments) {
        yearShipments.groupBy { it.receiverName.trim() }
            .filter { it.key.isNotBlank() }
            .map { it.key to it.value.size }
            .sortedByDescending { it.second }
            .take(5)
    }
    val maxSender = (topSenders.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)

    // top destinations
    val topDests = remember(yearShipments) {
        yearShipments.groupBy { it.destination.trim() }
            .filter { it.key.isNotBlank() }
            .map { it.key to it.value.size }
            .sortedByDescending { it.second }
            .take(5)
    }
    val maxDest = (topDests.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)

    // status breakdown
    val statusCounts = remember(yearShipments) {
        mapOf(
            Shipment.STATUS_DELIVERED to yearShipments.count { it.status == Shipment.STATUS_DELIVERED },
            Shipment.STATUS_IN_TRANSIT to yearShipments.count { it.status == Shipment.STATUS_IN_TRANSIT },
            Shipment.STATUS_RETURNED to yearShipments.count { it.status == Shipment.STATUS_RETURNED }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                title = { Text("📊 داشبورد آماری", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت")
                    }
                },
                actions = {
                    // year switcher
                    IconButton(onClick = { year-- }) {
                        Icon(Icons.Default.ChevronRight, "سال قبل")
                    }
                    Text("$year", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    IconButton(onClick = { year++ }) {
                        Icon(Icons.Default.ChevronLeft, "سال بعد")
                    }
                }
            )
        }
    ) { padding ->
        AuroraBackground {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp)
            ) {
                // ===== Monthly bar chart =====
                StatsCard(title = "📈 روند ماهانه بارها — سال $year") {
                    MonthlyBarChart(monthly, maxMonthly)
                }

                // ===== Top customers =====
                StatsCard(title = "👥 پرتکرارترین مشتری‌ها (گیرنده)") {
                    if (topSenders.isEmpty()) EmptyHint() else {
                        topSenders.forEachIndexed { i, (name, count) ->
                            RankRow(
                                rank = i + 1,
                                title = name,
                                count = count,
                                fraction = count.toFloat() / maxSender,
                                color = EmeraldGlow
                            )
                        }
                    }
                }

                // ===== Top destinations =====
                StatsCard(title = "📍 پرتکرارترین مقصدها") {
                    if (topDests.isEmpty()) EmptyHint() else {
                        topDests.forEachIndexed { i, (name, count) ->
                            RankRow(
                                rank = i + 1,
                                title = name,
                                count = count,
                                fraction = count.toFloat() / maxDest,
                                color = Gold
                            )
                        }
                    }
                }

                // ===== Status donut-ish summary =====
                StatsCard(title = "🚚 وضعیت بارهای $year") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        statusCounts.forEach { (status, count) ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val color = when (status) {
                                    Shipment.STATUS_DELIVERED -> Color(0xFF34D399)
                                    Shipment.STATUS_RETURNED -> Color(0xFFF87171)
                                    else -> Color(0xFFFBBF24)
                                }
                                Text("$count", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
                                Text(status, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun StatsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        )
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun MonthlyBarChart(monthly: List<Int>, max: Int) {
    val barTop = EmeraldGlow
    val barBottom = Color(0xFF0A5C3C)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val labelPx = with(LocalDensity.current) { 9.sp.toPx() }.toInt()

    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
        ) {
            val n = monthly.size
            val gap = 8.dp.toPx()
            val labelSpace = labelPx + 14f
            val chartH = size.height - labelSpace
            val barW = (size.width - gap * (n - 1)) / n

            // horizontal gridlines (3)
            for (g in 1..3) {
                val y = chartH - chartH * g / 3
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.5f)
            }

            monthly.forEachIndexed { i, v ->
                val x = i * (barW + gap)
                val h = if (v == 0) 4f else chartH * (v.toFloat() / max)
                val top = chartH - h
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(barTop, barBottom), startY = top, endY = chartH),
                    topLeft = Offset(x, top),
                    size = Size(barW, h),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
                if (v > 0) {
                    drawContext.canvas.nativeCanvas.drawText(
                        v.toString(),
                        x + barW / 2,
                        top - 6f,
                        android.graphics.Paint().apply {
                            color = labelColor.toArgb()
                            textSize = labelPx * 1.1f
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                        }
                    )
                }
                drawContext.canvas.nativeCanvas.drawText(
                    monthLabels[i],
                    x + barW / 2,
                    size.height,
                    android.graphics.Paint().apply {
                        color = labelColor.toArgb()
                        textSize = labelPx.toFloat()
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                )
            }
        }
    }
}

@Composable
private fun RankRow(rank: Int, title: String, count: Int, fraction: Float, color: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // rank badge
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (rank == 1) Gold else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$rank",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (rank == 1) Color(0xFF3D2E00) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                title,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text("$count بار", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0.04f, 1f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.horizontalGradient(listOf(color.copy(alpha = 0.55f), color))
                    )
            )
        }
    }
}

@Composable
private fun EmptyHint() {
    Text(
        "در این سال داده‌ای نیست",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
    )
}