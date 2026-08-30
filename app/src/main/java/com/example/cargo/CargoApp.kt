package com.example.cargo

import android.app.Application
import com.example.cargo.data.AppDatabase
import com.example.cargo.data.ShipmentRepository

class CargoApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val repository: ShipmentRepository by lazy { ShipmentRepository(database.shipmentDao()) }
}
