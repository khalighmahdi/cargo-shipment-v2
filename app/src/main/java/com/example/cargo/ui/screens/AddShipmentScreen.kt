package com.example.cargo.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.cargo.CargoApp
import com.example.cargo.data.Shipment
import com.example.cargo.util.ImageHelper
import com.example.cargo.viewmodel.ShipmentViewModel
import java.io.File
import java.io.FileOutputStream
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddShipmentScreen(
    viewModel: ShipmentViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scroll = rememberScrollState()

    var description by remember { mutableStateOf("") }
    var sender by remember { mutableStateOf("") }
    var receiver by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(Shipment.STATUS_IN_TRANSIT) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var imagePath by remember { mutableStateOf<String?>(null) }
    var statusMenuOpen by remember { mutableStateOf(false) }

    // Camera
    val cameraPermission = Manifest.permission.CAMERA
    var hasCameraPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, cameraPermission) == PackageManager.PERMISSION_GRANTED
        )
    }
    val cameraPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPerm = granted }

    var pendingCameraFile by remember { mutableStateOf<File?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val file = pendingCameraFile
        if (success && file != null && file.exists()) {
            imagePath = file.absolutePath
            imageUri = Uri.fromFile(file)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            // copy to app filesDir
            val target = ImageHelper.createImageFile(context)
            context.contentResolver.openInputStream(it)?.use { input ->
                FileOutputStream(target).use { output ->
                    input.copyTo(output)
                }
            }
            imagePath = target.absolutePath
            imageUri = Uri.fromFile(target)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ثبت بار جدید") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // Image section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("📷 عکس بار", fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    if (imageUri != null) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().height(200.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { imageUri = null; imagePath = null },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("حذف عکس") }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    if (hasCameraPerm) {
                                        val file = ImageHelper.createImageFile(context)
                                        pendingCameraFile = file
                                        val uri = ImageHelper.getUriForFile(context, file)
                                        cameraLauncher.launch(uri)
                                    } else {
                                        cameraPermLauncher.launch(cameraPermission)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PhotoCamera, null)
                                Spacer(Modifier.width(4.dp))
                                Text("دوربین")
                            }
                            OutlinedButton(
                                onClick = { galleryLauncher.launch("image/*") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, null)
                                Spacer(Modifier.width(4.dp))
                                Text("گالری")
                            }
                        }
                    }
                }
            }

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("توضیحات بار") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            // Sender
            OutlinedTextField(
                value = sender,
                onValueChange = { sender = it },
                label = { Text("فرستنده") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Person, null) }
            )

            // Receiver
            OutlinedTextField(
                value = receiver,
                onValueChange = { receiver = it },
                label = { Text("گیرنده") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Person, null) }
            )

            // Destination
            OutlinedTextField(
                value = destination,
                onValueChange = { destination = it },
                label = { Text("مقصد") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Place, null) }
            )

            // Status dropdown
            ExposedDropdownMenuBox(
                expanded = statusMenuOpen,
                onExpandedChange = { statusMenuOpen = it }
            ) {
                OutlinedTextField(
                    value = status,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("وضعیت") },
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
                                status = st
                                statusMenuOpen = false
                            }
                        )
                    }
                }
            }

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("یادداشت (اختیاری)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            // Save button
            Button(
                onClick = {
                    viewModel.insert(
                        description = description,
                        sender = sender,
                        receiver = receiver,
                        destination = destination,
                        notes = notes,
                        status = status,
                        imagePath = imagePath
                    ) {
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = description.isNotBlank() && sender.isNotBlank() && receiver.isNotBlank()
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(4.dp))
                Text("ذخیره")
            }
        }
    }
}
