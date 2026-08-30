package com.example.cargo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shipments")
data class Shipment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cargoDescription: String,
    val senderName: String,
    val receiverName: String,
    val destination: String,
    val notes: String,
    val status: String,           // IN_TRANSIT, DELIVERED, RETURNED
    val imagePath: String?,       // مسیر فایل عکس
    val jalaliYear: Int,
    val jalaliMonth: Int,
    val jalaliDay: Int,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_IN_TRANSIT = "در حال ارسال"
        const val STATUS_DELIVERED = "تحویل شده"
        const val STATUS_RETURNED = "برگشتی"

        val ALL_STATUSES = listOf(STATUS_IN_TRANSIT, STATUS_DELIVERED, STATUS_RETURNED)
    }
}
