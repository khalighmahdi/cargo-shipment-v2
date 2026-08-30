package com.example.cargo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cargo.ui.theme.Purple
import com.example.cargo.util.CsvExporter
import com.example.cargo.util.ShipmentWebServer
import com.example.cargo.util.SmsSender
import com.example.cargo.viewmodel.ShipmentViewModel
import kotlinx.coroutines.launch
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ShipmentViewModel,
    onOpenContacts: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val darkMode by viewModel.settings.darkMode.collectAsState(initial = true)

    // SMS settings state
    val smsEnabled by viewModel.settings.smsEnabled.collectAsState(initial = true)
    val smsMethod by viewModel.settings.smsMethod.collectAsState(initial = "sim")
    val smsApiKey by viewModel.settings.smsApiKey.collectAsState(initial = "")
    val smsSenderNum by viewModel.settings.smsSender.collectAsState(initial = "")
    val smsTemplate by viewModel.settings.smsTemplate.collectAsState(initial = SmsSender.DEFAULT_MESSAGE)

    var serverRunning by remember { mutableStateOf(false) }
    var serverIp by remember { mutableStateOf<String?>(null) }
    var server by remember { mutableStateOf<ShipmentWebServer?>(null) }

    // SMS permission (SEND_SMS for SIM method)
    var hasSmsPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }
    val smsPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasSmsPerm = granted }

    var methodMenuOpen by remember { mutableStateOf(false) }

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

            // ===== SMS settings =====
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Sms, null, tint = Purple)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("پیامک خودکار", fontWeight = FontWeight.Bold)
                            Text("پس از ثبت بار به صاحب بار پیام بده", fontSize = 12.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = smsEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch { viewModel.settings.setSmsEnabled(enabled) }
                            }
                        )
                    }

                    if (smsEnabled) {
                        Spacer(Modifier.height(12.dp))

                        // Method selector
                        ExposedDropdownMenuBox(
                            expanded = methodMenuOpen,
                            onExpandedChange = { methodMenuOpen = it }
                        ) {
                            OutlinedTextField(
                                value = if (smsMethod == "sim") "📱 از سیم‌کارت خودم" else "🌐 از API کاوه‌نگار",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("روش ارسال") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodMenuOpen) },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = methodMenuOpen,
                                onDismissRequest = { methodMenuOpen = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("📱 از سیم‌کارت خودم (رایگان از شارژ)") },
                                    onClick = {
                                        scope.launch { viewModel.settings.setSmsMethod("sim") }
                                        methodMenuOpen = false
                                        if (!hasSmsPerm) smsPermLauncher.launch(Manifest.permission.SEND_SMS)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("🌐 از API کاوه‌نگار (خط پیامکی)") },
                                    onClick = {
                                        scope.launch { viewModel.settings.setSmsMethod("kavenegar") }
                                        methodMenuOpen = false
                                    }
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        if (smsMethod == "kavenegar") {
                            OutlinedTextField(
                                value = smsApiKey,
                                onValueChange = { scope.launch { viewModel.settings.setSmsApiKey(it.trim()) } },
                                label = { Text("API Key کاوه‌نگار") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = smsSenderNum,
                                onValueChange = { scope.launch { viewModel.settings.setSmsSender(it.trim()) } },
                                label = { Text("شماره خط ارسال (اختیاری، مثلا 10008663)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "از kavenegar.com حساب بساز و API Key رو اینجا وارد کن",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        } else {
                            Text(
                                if (hasSmsPerm) "✅ پرمیشن پیامک داده شده" else "⚠️ پرمیشن پیامک لازم است",
                                fontSize = 12.sp,
                                color = if (hasSmsPerm) Color(0xFF43A047) else Color(0xFFFFB300)
                            )
                            if (!hasSmsPerm) {
                                TextButton(onClick = { smsPermLauncher.launch(Manifest.permission.SEND_SMS) }) {
                                    Text("دادن پرمیشن")
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Message template
                        OutlinedTextField(
                            value = smsTemplate,
                            onValueChange = { scope.launch { viewModel.settings.setSmsTemplate(it) } },
                            label = { Text("متن پیامک") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                    }
                }
            }

            // ===== Contacts book shortcut =====
            if (onOpenContacts != null) {
                Card(
                    Modifier.fillMaxWidth().clickable { onOpenContacts() }
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Contacts, null, tint = Purple)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("دفترچه تلفن", fontWeight = FontWeight.Bold)
                            Text("مدیریت مشتری‌های ثابت", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
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
                    Text("نسخه ۲.۳", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}