package com.example.cargo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cargo.data.Shipment
import com.example.cargo.ui.theme.Purple
import com.example.cargo.viewmodel.ShipmentViewModel
import com.example.cargo.util.JalaliDate

private val monthNames = listOf(
    "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
    "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
)

/**
 * بایگانی پوشه‌ای: سال → ماه → روز → لیست بارها
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    viewModel: ShipmentViewModel,
    onShipmentClick: (Int) -> Unit
) {
    // Navigation state inside archive: null = year list, else month list, else day list
    var selectedYear by remember { mutableStateOf<Int?>(null) }
    var selectedMonth by remember { mutableStateOf<Int?>(null) }

    val allShipments by viewModel.filteredShipments.collectAsState()

    // Group by year > month > day
    val grouped = remember(allShipments) {
        allShipments.groupBy { it.jalaliYear }
            .mapValues { (_, yearList) ->
                yearList.groupBy { it.jalaliMonth }
                    .mapValues { (_, monthList) ->
                        monthList.groupBy { it.jalaliDay }
                    }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            selectedYear == null -> "📁 بایگانی"
                            selectedMonth == null -> "📁 ${selectedYear}"
                            else -> "📁 ${monthNames[selectedMonth!! - 1]} ${selectedYear}"
                        }
                    )
                },
                navigationIcon = {
                    if (selectedYear != null) {
                        IconButton(onClick = {
                            if (selectedMonth != null) selectedMonth = null else selectedYear = null
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت")
                        }
                    }
                }
            )
        }
    ) { padding ->
        when {
            // Year list
            selectedYear == null -> {
                val years = grouped.keys.sortedDescending()
                if (years.isEmpty()) {
                    EmptyArchive("هیچ بایگانی‌ای وجود ندارد")
                } else {
                    LazyColumn(Modifier.padding(padding).padding(8.dp)) {
                        items(years) { year ->
                            val count = grouped[year]?.values?.sumOf { it.size } ?: 0
                            FolderCard(
                                icon = Icons.Default.Folder,
                                title = "سال ${year}",
                                subtitle = "$count بار",
                                count = count,
                                onClick = { selectedYear = year }
                            )
                        }
                    }
                }
            }

            // Month list
            selectedMonth == null -> {
                val year = selectedYear!!
                val months = grouped[year]?.keys?.sortedDescending() ?: emptyList()
                if (months.isEmpty()) {
                    EmptyArchive("هیچ بایگانی‌ای در این سال نیست")
                } else {
                    LazyColumn(Modifier.padding(padding).padding(8.dp)) {
                        items(months) { month ->
                            val count = grouped[year]?.get(month)?.values?.sumOf { it.size } ?: 0
                            FolderCard(
                                icon = Icons.Default.FolderOpen,
                                title = monthNames[month - 1],
                                subtitle = "$count بار",
                                count = count,
                                onClick = { selectedMonth = month }
                            )
                        }
                    }
                }
            }

            // Day list
            else -> {
                val year = selectedYear!!
                val month = selectedMonth!!
                val days = grouped[year]?.get(month)?.keys?.sortedDescending() ?: emptyList()
                if (days.isEmpty()) {
                    EmptyArchive("هیچ بایگانی‌ای در این ماه نیست")
                } else {
                    LazyColumn(Modifier.padding(padding).padding(8.dp)) {
                        items(days) { day ->
                            val list = grouped[year]?.get(month)?.get(day) ?: emptyList()
                            FolderCard(
                                icon = Icons.Default.CalendarMonth,
                                title = "روز $day",
                                subtitle = "${list.size} بار",
                                count = list.size,
                                onClick = {
                                    // Show shipments of this day inline (expandable list)
                                }
                            )
                            // Inline list of shipments for that day
                            list.forEach { s ->
                                DayShipmentRow(s, onClick = { onShipmentClick(s.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    count: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Purple,
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, fontSize = 12.sp, color = Color.Gray)
            }
            Badge(
                containerColor = Purple.copy(alpha = 0.2f),
                contentColor = Purple
            ) {
                Text("$count")
            }
        }
    }
}

@Composable
private fun DayShipmentRow(shipment: Shipment, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, top = 2.dp, bottom = 2.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val statusColor = when (shipment.status) {
                Shipment.STATUS_DELIVERED -> Color(0xFF43A047)
                Shipment.STATUS_RETURNED -> Color(0xFFE53935)
                else -> Color(0xFFFFB300)
            }
            Box(
                Modifier.size(10.dp).background(statusColor, RoundedCornerShape(50))
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    shipment.cargoDescription.ifBlank { "(بدون توضیح)" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "${shipment.senderName} → ${shipment.receiverName}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
        }
    }
}

@Composable
private fun EmptyArchive(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📂", fontSize = 64.sp)
            Text(message, color = Color.Gray)
        }
    }
}
