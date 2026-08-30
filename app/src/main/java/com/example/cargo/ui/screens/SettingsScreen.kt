package com.example.cargo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cargo.ui.theme.Purple
import com.example.cargo.util.CsvExporter
import com.example.cargo.util.ShipmentWebServer
import com.example.cargo.viewmodel.ShipmentViewModel
import kotlinx.coroutines.launch
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: ShipmentViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val darkMode by viewModel.settings.darkMode.collectAsState(initial = true)

    var serverRunning by remember { mutableStateOf(false) }
    var serverIp by remember { mutableStateOf<String?>(null) }
    var server by remember { mutableStateOf<ShipmentWebServer?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("⚙️ تنظیمات") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ===== Dark mode =====
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (darkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                        null,
                        tint = Purple
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("حالت تاریک", fontWeight = FontWeight.Bold)
                        Text(
                            if (darkMode) "فعال" else "غیرفعال",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = darkMode,
                        onCheckedChange = { enabled ->
                            scope.launch { viewModel.settings.setDarkMode(enabled) }
                        }
                    )
                }
            }

            // ===== Network sharing =====
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Wifi, null, tint = Purple)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("اشتراک در شبکه", fontWeight = FontWeight.Bold)
                            Text(
                                "مشاهده ارسالی‌ها از سیستم شرکت",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                        Switch(
                            checked = serverRunning,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    val ip = ShipmentWebServer.getLocalIpAddress()
                                    if (ip == null) {
                                        Toast.makeText(context, "به وایفای وصل نیستی", Toast.LENGTH_SHORT).show()
                                    } else {
                                        try {
                                            val app = context.applicationContext as com.example.cargo.CargoApp
                                            val s = ShipmentWebServer(app.repository)
                                            s.start(0, false)
                                            server = s
                                            serverRunning = true
                                            serverIp = ip
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "خطا: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    server?.stop()
                                    server = null
                                    serverRunning = false
                                    serverIp = null
                                }
                            }
                        )
                    }

                    if (serverRunning && serverIp != null) {
                        Spacer(Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Purple.copy(alpha = 0.15f)
                            )
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text("🌐 آدرس را در مرورگر سیستم بزن:", fontSize = 12.sp, color = Color.Gray)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "http://$serverIp:8080",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Purple
                                )
                                Text(
                                    "(هر دو دستگاه باید به یک وایفای وصل باشند)",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }

            // ===== Export CSV =====
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FileDownload, null, tint = Purple)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("خروجی اکسل (CSV)", fontWeight = FontWeight.Bold)
                            Text("ذخیره همه بارها در فایل", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                val shipments = viewModel.filteredShipments.value
                                val path = CsvExporter.export(context, shipments)
                                if (path != null) {
                                    Toast.makeText(context, "ذخیره شد ✓", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "خطا در ذخیره", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("📥 ذخیره فایل")
                    }
                }
            }

            // ===== About =====
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📦 باربری", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("نسخه ۲.۰", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}
