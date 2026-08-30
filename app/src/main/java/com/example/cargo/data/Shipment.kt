package com.example.cargo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * مخاطب دفترچه تلفن - مشتری‌های ثابت
 */
@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "shipments")
data class Shipment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cargoDescription: String,
    val senderName: String,
    val senderPhone: String = "",      // شماره صاحب بار (فرستنده)
    val receiverName: String,
    val receiverPhone: String = "",    // شماره گیرنده
    val destination: String,
    val notes: String,
    val status: String,           // در حال ارسال، تحویل شده، برگشتی
    val imagePath: String?,       // مسیر عکس اول (سازگاری با نسخه قبل)
    val imagePaths: String = "",  // مسیرهای همه عکس‌ها جدا شده با |
    val smsSent: Boolean = false, // آیا پیامک خودکار ارسال شد؟
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
