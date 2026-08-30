package com.example.cargo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cargo.data.Shipment
import com.example.cargo.viewmodel.ShipmentViewModel
import com.example.cargo.util.JalaliDate
import com.example.cargo.util.CsvExporter
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: ShipmentViewModel,
    onAdd: () -> Unit,
    onShipmentClick: (Int) -> Unit
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
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("📦 باربری", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(today.formatWithMonthName(), fontSize = 12.sp, color = Color.Gray)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // Stats cards
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatCard("کل", total.toString(), Color(0xFF1565C0), Modifier.weight(1f).padding(4.dp))
                StatCard("در حال ارسال", inTransit.toString(), Color(0xFFFFA000), Modifier.weight(1f).padding(4.dp))
                StatCard("تحویل شده", delivered.toString(), Color(0xFF2E7D32), Modifier.weight(1f).padding(4.dp))
                StatCard("برگشتی", returned.toString(), Color(0xFFC62828), Modifier.weight(1f).padding(4.dp))
            }

            // Search
            OutlinedTextField(
                value = search,
                onValueChange = { viewModel.setSearch(it) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
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
                modifier = Modifier.fillMaxWidth().padding(8.dp),
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

@Composable
private fun StatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 10.sp, color = Color.DarkGray)
        }
    }
}

@Composable
private fun ShipmentCard(shipment: Shipment, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    shipment.cargoDescription.ifBlank { "(بدون توضیح)" },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                StatusBadge(shipment.status)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("👤 ${shipment.senderName} → ${shipment.receiverName}", fontSize = 13.sp, color = Color.DarkGray)
            if (shipment.destination.isNotBlank()) {
                Text("📍 ${shipment.destination}", fontSize = 13.sp, color = Color.DarkGray)
            }
            Text("📅 ${shipment.jalaliYear}/${shipment.jalaliMonth.toString().padStart(2, '0')}/${shipment.jalaliDay.toString().padStart(2, '0')}", fontSize = 11.sp, color = Color.Gray)
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
