package com.example.cargo.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.cargo.data.Shipment
import java.io.File
import java.io.FileOutputStream

/**
 * اشتراک‌گذاری بار: عکس‌ها + مشخصات به‌صورت متن — برای همه شبکه‌های اجتماعی
 * (واتساپ، تلگرام، ایمیل، ...) از طریق سیستم share اندروید.
 */
object ShipmentSharer {

    private fun loadBitmap(path: String): Bitmap? = try {
        BitmapFactory.decodeFile(path)
    } catch (e: Exception) {
        null
    }

    /** خواندن متن share برای نمایش قبل از ارسال */
    fun buildText(s: Shipment): String {
        val date = "${s.jalaliYear}/${s.jalaliMonth.toString().padStart(2, '0')}/${s.jalaliDay.toString().padStart(2, '0')}"
        return buildString {
            appendLine("📦 مشخصات بار")
            appendLine()
            if (s.cargoDescription.isNotBlank()) appendLine("▪️ بار: ${s.cargoDescription}")
            if (s.senderName.isNotBlank()) appendLine("👤 فرستنده: ${s.senderName}")
            if (s.receiverName.isNotBlank()) appendLine("👤 گیرنده: ${s.receiverName}")
            if (s.destination.isNotBlank()) appendLine("📍 مقصد: ${s.destination}")
            appendLine("🚚 وضعیت: ${s.status}")
            appendLine("📅 تاریخ: $date")
            if (s.notes.isNotBlank()) appendLine("📝 یادداشت: ${s.notes}")
        }
    }

    /**
     * اشتراک‌گذاری: عکس‌ها (تا N تا) + متن مشخصات.
     * عکس‌ها از مسیر داخلی به کاشی قابل‌اشتراک کپی می‌شوند تا همه اپ‌ها بتوانند بخوانند.
     */
    fun share(context: Context, shipment: Shipment, maxImages: Int = 9): Boolean {
        return try {
            val paths = shipment.imagePaths.split("|").filter { it.isNotBlank() }.take(maxImages)
            val shareDir = File(context.cacheDir, "shared_images").apply { mkdirs() }
            val uris = mutableListOf<Uri>()

            paths.forEachIndexed { idx, path ->
                val bmp = loadBitmap(path) ?: return@forEachIndexed
                val outFile = File(shareDir, "bar_${shipment.id}_${idx}.jpg")
                FileOutputStream(outFile).use { out ->
                    bmp.compress(Bitmap.CompressFormat.JPEG, 92, out)
                }
                uris.add(
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        outFile
                    )
                )
            }

            val text = buildText(shipment)

            val intent = Intent().apply {
                action = Intent.ACTION_SEND_MULTIPLE
                type = "image/jpeg"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            // اگر عکسی نبود، فقط متن
            if (uris.isEmpty()) {
                intent.action = Intent.ACTION_SEND
                intent.type = "text/plain"
                intent.removeExtra(Intent.EXTRA_STREAM)
            }

            val chooser = Intent.createChooser(intent, "اشتراک‌گذاری بار").apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
