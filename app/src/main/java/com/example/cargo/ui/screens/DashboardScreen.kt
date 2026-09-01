package com.example.cargo.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cargo.R
import com.example.cargo.data.Shipment
import com.example.cargo.ui.theme.AuroraBackground
import com.example.cargo.ui.theme.Gold
import com.example.cargo.ui.theme.brandGradient
import com.example.cargo.util.JalaliDate
import com.example.cargo.viewmodel.ShipmentViewModel
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.material.icons.outlined.Dashboard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: ShipmentViewModel,
    onAdd: () -> Unit,
    onShipmentClick: (Int) -> Unit,
    onOpenStats: () -> Unit
) {
    val context = LocalContext.current
    val shipments by viewModel.filteredShipments.collectAsState()
    val search by viewModel.searchQuery.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()
    val total by viewModel.totalCount.collectAsState()
    val inTransit by viewModel.countInTransit.collectAsState()
    val delivered by viewModel.countDelivered.collectAsState()
    val returned by viewModel.countReturned.collectAsState()
    val today = remember { JalaliDate.today() }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.logo_sharifan_white),
                                contentDescription = "شریفان",
                                modifier = Modifier.height(26.dp),
                                tint = Color.Unspecified
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("📦 باربری", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(today.formatWithMonthName(), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = onOpenStats) {
                        Icon(Icons.Outlined.Dashboard, "داشبورد آماری")
                    }
                }
            )
        }
    ) { padding ->
        AuroraBackground {
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // ===== Hero: brand gradient banner with logo + total =====
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(brandGradient())
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.logo_sharifan_white),
                            contentDescription = null,
                            modifier = Modifier.height(34.dp),
                            tint = Color.Unspecified
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "شرکت باربری شریفان",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                "$total بار ثبت شده تاکنون",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 12.sp
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        // gold sparkle dot
                        Box(
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Gold)
                        )
                    }
                }
            }

            // Stats cards — hero row with gradient accents
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard("کل", total.toString(), Color(0xFF60A5FA), Modifier.weight(1f))
                StatCard("در حال ارسال", inTransit.toString(), Color(0xFFFBBF24), Modifier.weight(1f))
                StatCard("تحویل شده", delivered.toString(), Color(0xFF34D399), Modifier.weight(1f))
                StatCard("برگشتی", returned.toString(), Color(0xFFF87171), Modifier.weight(1f))
            }

            // Search
            OutlinedTextField(
                value = search,
                onValueChange = { viewModel.setSearch(it) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                placeholder = { Text("🔍 جستجو...") },
                singleLine = true,
                trailingIcon = {
                    if (search.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearSearch() }) {
                            Icon(Icons.Default.Clear, "پاک کردن")
                        }
                    }
                }
            )

            // Filter chips
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = statusFilter == null,
                    onClick = { viewModel.setStatusFilter(null) },
                    label = { Text("همه") }
                )
                Shipment.ALL_STATUSES.forEach { st ->
                    FilterChip(
                        selected = statusFilter == st,
                        onClick = { viewModel.setStatusFilter(st) },
                        label = { Text(st) }
                    )
                }
            }

            // List
            if (shipments.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📭", fontSize = 64.sp)
                        Text("هیچ باری ثبت نشده", color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(shipments, key = { it.id }) { shipment ->
                        ShipmentCard(shipment, onClick = { onShipmentClick(shipment.id) })
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .size(width = 22.dp, height = 3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
            Spacer(Modifier.height(6.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ShipmentCard(shipment: Shipment, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp),
        shape = RoundedCornerShape(18.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    shipment.cargoDescription.ifBlank { "(بدون توضیح)" },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                StatusBadge(shipment.status)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text("👤 ${shipment.senderName} → ${shipment.receiverName}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (shipment.destination.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text("📍 ${shipment.destination}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(2.dp))
            Text("📅 ${shipment.jalaliYear}/${shipment.jalaliMonth.toString().padStart(2, '0')}/${shipment.jalaliDay.toString().padStart(2, '0')}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val color = when (status) {
        Shipment.STATUS_DELIVERED -> Color(0xFF2E7D32)
        Shipment.STATUS_RETURNED -> Color(0xFFC62828)
        else -> Color(0xFFFFA000)
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            status,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}