package com.example.cargo.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardOptions
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
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddShipmentScreen(
    viewModel: ShipmentViewModel,
    onBack: () -> Unit,
    initialShipment: Shipment? = null
) {
    val context = LocalContext.current
    val scroll = rememberScrollState()
    val isEditing = initialShipment != null

    var description by remember { mutableStateOf(initialShipment?.cargoDescription ?: "") }
    var sender by remember { mutableStateOf(initialShipment?.senderName ?: "") }
    var receiver by remember { mutableStateOf(initialShipment?.receiverName ?: "") }
    var destination by remember { mutableStateOf(initialShipment?.destination ?: "") }
    var notes by remember { mutableStateOf(initialShipment?.notes ?: "") }
    var status by remember { mutableStateOf(initialShipment?.status ?: Shipment.STATUS_IN_TRANSIT) }
    var imagePaths by remember { mutableStateOf(initialShipment?.imagePaths ?: "") }
    var imageUris by remember { mutableStateOf<MutableList<Uri>>(mutableListOf()) }
    var statusMenuOpen by remember { mutableStateOf(false) }

    // Load existing images
    val paths = imagePaths.split("|").filter { it.isNotBlank() }
    if (isEditing && imageUris.isEmpty()) {
        imageUris.addAll(paths.map { File(it) }.filter { it.exists() }.map { Uri.fromFile(it) })
    }

    // Camera permission
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
            val uri = Uri.fromFile(file)
            val newPaths = if (imagePaths.isEmpty()) file.absolutePath else "$imagePaths|${file.absolutePath}"
            imagePaths = newPaths
            imageUris.add(uri)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.forEach { uri ->
            val target = ImageHelper.createImageFile(context)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(target).use { output ->
                    input.copyTo(output)
                }
            }
            val newPaths = if (imagePaths.isEmpty()) target.absolutePath else "$imagePaths|${target.absolutePath}"
            imagePaths = newPaths
            imageUris.add(Uri.fromFile(target))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "ویرایش بار" else "ثبت بار جدید") },
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

            // Images grid
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("📷 عکس‌های بار", fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        if (imageUris.size < 5) {
                            Text("${imageUris.size}/5", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    if (imageUris.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(imageUris) { uri ->
                                val index = imageUris.indexOf(uri)
                                Box(Modifier.size(100.dp).clip(RoundedCornerShape(8.dp))) {
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = null,
                                        contentScale = androidx.compose.foundation.layout.ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    // Delete button on image
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        IconButton(onClick = {
                                            imageUris.removeAt(index)
                                            val newPaths = imageUris.map { it.path }.joinToString("|")
                                            imagePaths = newPaths
                                        }) {
                                            Icon(Icons.Default.Close, "حذف", tint = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (imageUris.size < 5) {
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
                                modifier = Modifier.weight(1f),
                                enabled = imageUris.size < 5
                            ) {
                                Icon(Icons.Default.PhotoCamera, null)
                                Spacer(Modifier.width(4.dp))
                                Text("دوربین")
                            }
                            OutlinedButton(
                                onClick = { galleryLauncher.launch("image/*") },
                                modifier = Modifier.weight(1f),
                                enabled = imageUris.size < 5
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
                    if (description.isBlank() || sender.isBlank() || receiver.isBlank()) {
                        Toast.makeText(context, "فیلدهای ستاره‌دار الزامی هستند", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (isEditing && initialShipment != null) {
                        val updated = initialShipment.copy(
                            cargoDescription = description.trim(),
                            senderName = sender.trim(),
                            receiverName = receiver.trim(),
                            destination = destination.trim(),
                            notes = notes.trim(),
                            status = status,
                            imagePath = imagePaths.split("|").firstOrNull { it.isNotBlank() },
                            imagePaths = imagePaths
                        )
                        viewModel.update(updated)
                        onBack()
                    } else {
                        viewModel.insert(
                            description = description,
                            sender = sender,
                            receiver = receiver,
                            destination = destination,
                            notes = notes,
                            status = status,
                            imagePaths = imagePaths
                        ) {
                            onBack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isEditing) "به‌روزرسانی" else "ذخیره")
            }
        }
    }
}