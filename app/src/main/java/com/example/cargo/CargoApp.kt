package com.example.cargo

import android.app.Application
import com.example.cargo.data.AppDatabase
import com.example.cargo.data.SettingsRepository
import com.example.cargo.data.ShipmentRepository

class CargoApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val repository: ShipmentRepository by lazy { ShipmentRepository(database.shipmentDao()) }
    val contactRepository: ContactRepository by lazy { ContactRepository(database.contactDao()) }
    val settings: SettingsRepository by lazy { SettingsRepository(this) }
}
