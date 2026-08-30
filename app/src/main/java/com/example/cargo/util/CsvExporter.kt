package com.example.cargo.util

import android.content.Context
import android.os.Environment
import com.example.cargo.data.Shipment
import java.io.File
import java.io.FileWriter

object CsvExporter {

    fun export(context: Context, shipments: List<Shipment>): String? {
        return try {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            if (dir != null && !dir.exists()) dir.mkdirs()
            val file = File(dir, "shipments_${System.currentTimeMillis()}.csv")

            FileWriter(file).use { writer ->
                writer.write("\uFEFF")
                writer.write("ID,توضیحات,فرستنده,گیرنده,مقصد,وضعیت,تاریخ,یادداشت\n")

                shipments.forEach { s ->
                    val row = listOf(
                        s.id.toString(),
                        esc(s.cargoDescription),
                        esc(s.senderName),
                        esc(s.receiverName),
                        esc(s.destination),
                        esc(s.status),
                        "${s.jalaliYear}/${s.jalaliMonth.toString().padStart(2, '0')}/${s.jalaliDay.toString().padStart(2, '0')}",
                        esc(s.notes)
                    ).joinToString(",")
                    writer.write("$row\n")
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun esc(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else value
    }
}
