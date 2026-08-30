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
    val status: String,           // در حال ارسال، تحویل شده، برگشتی
    val imagePath: String?,       // مسیر عکس اول (سازگاری با نسخه قبل)
    val imagePaths: String = "",  // مسیرهای همه عکس‌ها جدا شده با |
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
