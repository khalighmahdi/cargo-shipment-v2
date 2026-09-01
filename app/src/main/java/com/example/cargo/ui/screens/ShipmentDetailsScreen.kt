package com.example.cargo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.cargo.data.Shipment
import com.example.cargo.viewmodel.ShipmentViewModel
import android.widget.Toast
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShipmentDetailsScreen(
    viewModel: ShipmentViewModel,
    shipmentId: Int,
    onBack: () -> Unit,
    onEdit: (Int) -> Unit
) {
    val context = LocalContext.current
    val shipment by viewModel.getById(shipmentId).collectAsState(initial = null)
    val s = shipment

    var statusMenuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("جزئیات بار") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت")
                    }
                },
                actions = {
                    if (s != null) {
                        IconButton(onClick = {
                            val ok = com.example.cargo.util.ShipmentSharer.share(context, s)
                            if (!ok) Toast.makeText(context, "خطا در اشتراک‌گذاری", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.Share, "اشتراک‌گذاری", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { onEdit(s.id) }) {
                            Icon(Icons.Default.Edit, "ویرایش", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Default.Delete, "حذف", tint = Color.Red)
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (s == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("در حال بارگذاری...")
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // All images
                val paths = s.imagePaths.split("|").filter { it.isNotBlank() }
                if (paths.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(paths) { path ->
                            AsyncImage(
                                model = File(path),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(220.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }
                    }
                }

                DetailRow("📦 توضیحات", s.cargoDescription.ifBlank { "-" })
                DetailRow("👤 فرستنده", s.senderName)
                DetailRow("👤 گیرنده", s.receiverName)
                if (s.senderPhone.isNotBlank()) DetailRow("📞 شماره گیرنده", s.senderPhone)
                if (s.destination.isNotBlank()) DetailRow("📍 مقصد", s.destination)
                DetailRow("📅 تاریخ", "${s.jalaliYear}/${s.jalaliMonth.toString().padStart(2, '0')}/${s.jalaliDay.toString().padStart(2, '0')}")
                if (s.notes.isNotBlank()) DetailRow("📝 یادداشت", s.notes)

                // Status changer
                ExposedDropdownMenuBox(
                    expanded = statusMenuOpen,
                    onExpandedChange = { statusMenuOpen = it }
                ) {
                    OutlinedTextField(
                        value = s.status,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("تغییر وضعیت") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusMenuOpen) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = statusMenuOpen,
                        onDismissRequest = { statusMenuOpen = false }
                    ) {
                        Shipment.ALL_STATUSES.forEach { st ->
                            DropdownMenuItem(
                                text = { Text(st) },
                                onClick = {
                                    viewModel.update(s.copy(status = st))
                                    statusMenuOpen = false
                                    Toast.makeText(context, "وضعیت به‌روزرسانی شد", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete && s != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("تأیید حذف") },
            text = { Text("آیا از حذف این بار مطمئن هستید؟") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(s)
                    confirmDelete = false
                    onBack()
                }) { Text("حذف", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("انصراف") }
            }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, fontSize = 12.sp, color = Color.Gray)
            Spacer(Modifier.height(2.dp))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}
