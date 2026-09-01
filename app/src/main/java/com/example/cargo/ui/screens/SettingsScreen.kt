package com.example.cargo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/** فیلد متنی با دکمه پاک کردن (✕) */
@Composable
fun ClearableTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardNumber: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else minLines,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (keyboardNumber) KeyboardType.Phone else KeyboardType.Text
        ),
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Default.Close, "پاک کردن", tint = Color.Gray)
                }
            }
        }
    )
}

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
    val smsApiUrl by viewModel.settings.smsApiUrl.collectAsState(initial = SmsSender.KAVENEGAR_URL)
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

    // چک دوباره پرمیشن وقتی از تنظیمات گوشی برمی‌گردیم
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasSmsPerm = context.checkSelfPermission(android.Manifest.permission.SEND_SMS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
                            Text("پس از ثبت بار به شماره گیرنده پیام بده", fontSize = 12.sp, color = Color.Gray)
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
                                value = when (smsMethod) {
                                    "sim" -> "📱 از سیم‌کارت خودم (رایگان)"
                                    "api_get" -> "🌐 API - لینک مستقیم (GET)"
                                    else -> "🌐 API - پیامک.آی‌آر و مشابه (POST)"
                                },
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
                                    text = { Text("🌐 API با لینک مستقیم (GET) — کاوه‌نگار و مشابه") },
                                    onClick = {
                                        scope.launch {
                                            viewModel.settings.setSmsMethod("api_get")
                                            viewModel.settings.setSmsApiUrl(SmsSender.KAVENEGAR_URL)
                                            viewModel.settings.setSmsApiBody("")
                                            viewModel.settings.setSmsApiHeaders("")
                                        }
                                        methodMenuOpen = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("🌐 API با درخواست POST — SMS.ir و مشابه") },
                                    onClick = {
                                        scope.launch {
                                            viewModel.settings.setSmsMethod("api_post")
                                            viewModel.settings.setSmsApiUrl(SmsSender.SMSIR_URL)
                                            viewModel.settings.setSmsApiBody(SmsSender.SMSIR_BODY)
                                            viewModel.settings.setSmsApiHeaders(SmsSender.SMSIR_HEADERS)
                                        }
                                        methodMenuOpen = false
                                    }
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        if (smsMethod.startsWith("api")) {
                            ClearableTextField(
                                value = smsApiUrl,
                                onValueChange = { scope.launch { viewModel.settings.setSmsApiUrl(it) } },
                                label = "آدرس API",
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            ClearableTextField(
                                value = smsApiKey,
                                onValueChange = { scope.launch { viewModel.settings.setSmsApiKey(it) } },
                                label = "API Key (کلید از پنل سایت)",
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            ClearableTextField(
                                value = smsSenderNum,
                                onValueChange = { scope.launch { viewModel.settings.setSmsSender(it) } },
                                label = "شماره خط ارسال (مثلا 3000...) ",
                                modifier = Modifier.fillMaxWidth(),
                                keyboardNumber = true
                            )
                            if (smsMethod == "api_post") {
                                Spacer(Modifier.height(8.dp))
                                // Body + headers prefilled with SMS.ir template - editable for advanced use
                                val bodyFlow = viewModel.settings.smsApiBody
                                val headersFlow = viewModel.settings.smsApiHeaders
                                val bodyVal by bodyFlow.collectAsState(initial = SmsSender.SMSIR_BODY)
                                val headersVal by headersFlow.collectAsState(initial = SmsSender.SMSIR_HEADERS)
                                ClearableTextField(
                                    value = bodyVal,
                                    onValueChange = { scope.launch { viewModel.settings.setSmsApiBody(it) } },
                                    label = "بدنه درخواست JSON (پیشرفته)",
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = false,
                                    minLines = 2
                                )
                                Spacer(Modifier.height(8.dp))
                                ClearableTextField(
                                    value = headersVal,
                                    onValueChange = { scope.launch { viewModel.settings.setSmsApiHeaders(it) } },
                                    label = "هدرها (پیشرفته)",
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "placeholder ها: {api_key} {sender} {phone} {message}\n" +
                                    if (smsMethod == "api_post")
                                        "SMS.ir: پنل ← برنامه‌نویسان ← کلید API — شماره خط را وارد کن"
                                    else
                                        "کاوه‌نگار: پنل ← تنظیمات ← API Key",
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
                        }

                        Spacer(Modifier.height(8.dp))

                        // Test SMS
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            ClearableTextField(
                                value = testPhone,
                                onValueChange = { testPhone = it },
                                label = "شماره برای تست",
                                modifier = Modifier.weight(1f),
                                keyboardNumber = true
                            )
                            Button(onClick = {
                                testResult = null
                                scope.launch {
                                    viewModel.sendTestSms(testPhone) { ok, msg ->
                                        testResult = if (ok) "✅ $msg" else "❌ $msg"
                                    }
                                }
                            }) {
                                Text("📤 تست")
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

                        Spacer(Modifier.height(8.dp))

                        // Message template
                        ClearableTextField(
                            value = smsTemplate,
                            onValueChange = { scope.launch { viewModel.settings.setSmsTemplate(it) } },
                            label = "متن پیامک",
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = false,
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
                    Text("نسخه ۳.۴", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}