package com.example.cargo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cargo.data.Contact
import com.example.cargo.ui.theme.Purple
import com.example.cargo.viewmodel.ShipmentViewModel

/**
 * دفترچه تلفن — مخاطب‌های ثابت برای انتخاب سریع شماره
 * onPick: وقتی از حالت انتخاب فراخوانی می‌شه (name, phone) برمی‌گردونه
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    viewModel: ShipmentViewModel,
    onBack: () -> Unit,
    pickMode: Boolean = false,
    onPick: ((name: String, phone: String) -> Unit)? = null
) {
    var search by remember { mutableStateOf("") }
    val contacts by (if (search.isBlank()) viewModel.contactRepo.allContacts else viewModel.contactRepo.search(search))
        .collectAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Contact?>(null) }
    var deleting by remember { mutableStateOf<Contact?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📇 دفترچه تلفن") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "افزودن مخاطب")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text("جستجوی مخاطب...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))

            if (pickMode) {
                Text(
                    "برای انتخاب، روی مخاطب بزن",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (contacts.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📇", fontSize = 56.sp)
                        Text("هیچ مخاطبی نیست", color = Color.Gray)
                        Text("با دکمه + اضافه کن", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(contacts) { c ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                if (pickMode) onPick?.invoke(c.name, c.phone)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Person, null,
                                    tint = Purple,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(c.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Phone, null, tint = Color.Gray, modifier = Modifier.size(13.dp))
                                        Spacer(Modifier.width(3.dp))
                                        Text(c.phone, fontSize = 13.sp, color = Color.Gray)
                                    }
                                }
                                if (!pickMode) {
                                    IconButton(onClick = { editing = c }) {
                                        Icon(Icons.Default.Edit, "ویرایش", tint = Purple, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(onClick = { deleting = c }) {
                                        Icon(Icons.Default.Delete, "حذف", tint = Color.Red, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / edit dialog
    if (showAddDialog || editing != null) {
        ContactDialog(
            initial = editing,
            onDismiss = {
                showAddDialog = false
                editing = null
            },
            onSave = { name, phone ->
                if (editing != null) {
                    viewModel.saveContact(editing!!.copy(name = name, phone = phone))
                } else {
                    viewModel.saveContact(Contact(name = name, phone = phone))
                }
                showAddDialog = false
                editing = null
            }
        )
    }

    // Delete confirm
    if (deleting != null) {
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("حذف مخاطب") },
            text = { Text("«${deleting!!.name}» حذف شود؟") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteContact(deleting!!)
                    deleting = null
                }) { Text("حذف", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("انصراف") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactDialog(
    initial: Contact?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var phone by remember { mutableStateOf(initial?.phone ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "مخاطب جدید" else "ویرایش مخاطب") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("شماره تلفن") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank() && phone.isNotBlank()) onSave(name.trim(), phone.trim()) },
                enabled = name.isNotBlank() && phone.isNotBlank()
            ) { Text("ذخیره") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}
