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
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
    val smsApiUrl by viewModel.settings.smsApiUrl.collectAsState(initial = SmsSender.KAVENEGAR_TEMPLATE)
    val smsApiKey by viewModel.settings.smsApiKey.collectAsState(initial = "")
    val smsSenderNum by viewModel.settings.smsSender.collectAsState(initial = "")
    val smsTemplate by viewModel.settings.smsTemplate.collectAsState(initial = SmsSender.DEFAULT_MESSAGE)

    var serverRunning by remember { mutableStateOf(false) }
    var serverIp by remember { mutableStateOf<String?>(null) }
    var server by remember { mutableStateOf<ShipmentWebServer?>(null) }

    // SMS permission for SIM method
    var hasSmsPerm by remember {
        mutableStateOf(
            context.checkSelfPermission(android.Manifest.permission.SEND_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    val smsPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasSmsPerm = granted }

    var methodMenuOpen by remember { mutableStateOf(false) }
    var testPhone by remember { mutableStateOf("") }
    var testResult by remember { mutableStateOf<String?>(null) }

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
                                value = if (smsMethod == "sim") "📱 از سیم‌کارت خودم (رایگان)" else "🌐 از API (هر سایت پیامکی)",
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
                                        if (!hasSmsPerm) smsPermLauncher.launch(android.Manifest.permission.SEND_SMS)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("🌐 از API (هر سایت پیامکی: کاوه‌نگار، فراپیامک، ...)") },
                                    onClick = {
                                        scope.launch { viewModel.settings.setSmsMethod("api") }
                                        methodMenuOpen = false
                                    }
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        if (smsMethod == "api") {
                            // API URL template
                            OutlinedTextField(
                                value = smsApiUrl,
                                onValueChange = { scope.launch { viewModel.settings.setSmsApiUrl(it.trim()) } },
                                label = { Text("آدرس API (قالب)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = smsApiKey,
                                onValueChange = { scope.launch { viewModel.settings.setSmsApiKey(it.trim()) } },
                                label = { Text("API Key") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = smsSenderNum,
                                onValueChange = { scope.launch { viewModel.settings.setSmsSender(it.trim()) } },
                                label = { Text("شماره خط ارسال (اختیاری)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "placeholders: {api_key} {sender} {phone} {message}\nمثال کاوه‌نگار: ${SmsSender.KAVENEGAR_TEMPLATE}",
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
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = { smsPermLauncher.launch(android.Manifest.permission.SEND_SMS) }) {
                                        Text("درخواست پرمیشن")
                                    }
                                    TextButton(onClick = {
                                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                        intent.data = Uri.parse("package:${context.packageName}")
                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        context.startActivity(intent)
                                    }) {
                                        Text("باز کردن تنظیمات اپ")
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            // Test SMS button
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = testPhone,
                                    onValueChange = { testPhone = it },
                                    label = { Text("شماره برای تست") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                Button(onClick = {
                                    testResult = null
                                    scope.launch { viewModel.sendTestSms(testPhone) { ok, msg ->
                                        testResult = if (ok) "✅ $msg" else "❌ $msg"
                                    } }
                                }) {
                                    Text("📤 پیامک تست")
                                }
                            }
                            if (testResult != null) {
                                Spacer(Modifier.height(4.dp))
                                val tr = testResult
                                Text(
                                    tr ?: "",
                                    fontSize = 12.sp,
                                    color = if (tr?.startsWith("✅") == true) Color(0xFF43A047) else Color(0xFFE53935)
                                )
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
                    Text("نسخه ۲.۴", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}