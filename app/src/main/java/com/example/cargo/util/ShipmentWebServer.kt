package com.example.cargo.util

import fi.iki.elonen.NanoHTTPD
import com.example.cargo.data.Shipment
import com.example.cargo.data.ShipmentRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.FileInputStream
import java.io.File

/**
 * وب‌سرور محلی برای مشاهده ارسالی‌ها از سیستم شرکت (همان وایفای)
 * آدرس: http://IP-گوشی:8080
 */
class ShipmentWebServer(
    private val repository: ShipmentRepository,
    port: Int = 8080
) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri ?: "/"

        return when {
            uri == "/" || uri == "/index.html" -> htmlResponse(renderHomePage())
            uri.startsWith("/image/") -> serveImage(uri.removePrefix("/image/"))
            uri == "/api/shipments" -> jsonResponse(renderJson())
            else -> htmlResponse("<h1>404 - یافت نشد</h1>", 404)
        }
    }

    private fun renderHomePage(): String {
        val shipments = runBlocking { repository.allShipments.first() }

        val rows = shipments.joinToString("\n") { s ->
            val statusColor = when (s.status) {
                Shipment.STATUS_DELIVERED -> "#43A047"
                Shipment.STATUS_RETURNED -> "#E53935"
                else -> "#FFB300"
            }
            val img = if (!s.imagePath.isNullOrBlank() && File(s.imagePath).exists()) {
                """<img src="/image${s.id}" class="thumb" loading="lazy">"""
            } else ""

            """
            <tr>
                <td>$img</td>
                <td>${esc(s.cargoDescription)}</td>
                <td>${esc(s.senderName)}</td>
                <td>${esc(s.receiverName)}</td>
                <td>${esc(s.destination)}</td>
                <td><span class="badge" style="background:$statusColor">${esc(s.status)}</span></td>
                <td>${s.jalaliYear}/${"%02d".format(s.jalaliMonth)}/${"%02d".format(s.jalaliDay)}</td>
                <td>${esc(s.notes)}</td>
            </tr>
            """
        }

        return """
        <!DOCTYPE html>
        <html dir="rtl" lang="fa">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>باربری - لیست ارسالی‌ها</title>
            <style>
                * { margin:0; padding:0; box-sizing:border-box; font-family: Tahoma, Arial; }
                body { background:#121016; color:#fff; padding:20px; }
                h1 { color:#B388FF; margin-bottom:5px; }
                .sub { color:#888; margin-bottom:20px; font-size:14px; }
                table { width:100%; border-collapse:collapse; background:#1D1B22; border-radius:8px; overflow:hidden; }
                th { background:#4527A0; padding:12px 8px; font-size:13px; text-align:right; }
                td { padding:10px 8px; border-bottom:1px solid #2A2733; font-size:13px; }
                tr:hover td { background:#2A2733; }
                .badge { padding:4px 10px; border-radius:12px; color:#fff; font-size:11px; white-space:nowrap; }
                .thumb { width:60px; height:60px; object-fit:cover; border-radius:6px; }
                .count { color:#B388FF; font-size:16px; margin-bottom:12px; }
            </style>
        </head>
        <body>
            <h1>📦 باربری</h1>
            <div class="sub">لیست زنده ارسالی‌ها از اپلیکیشن گوشی</div>
            <div class="count">تعداد کل: ${shipments.size}</div>
            <table>
                <tr>
                    <th>عکس</th>
                    <th>توضیحات</th>
                    <th>فرستنده</th>
                    <th>گیرنده</th>
                    <th>مقصد</th>
                    <th>وضعیت</th>
                    <th>تاریخ</th>
                    <th>یادداشت</th>
                </tr>
                $rows
            </table>
            <script>setTimeout(()=>location.reload(), 15000);</script>
        </body>
        </html>
        """.trimIndent()
    }

    private fun renderJson(): String {
        val shipments = runBlocking { repository.allShipments.first() }
        val json = shipments.joinToString(",") { s ->
            """{"id":${s.id},"description":"${esc(s.cargoDescription)}","sender":"${esc(s.senderName)}","receiver":"${esc(s.receiverName)}","destination":"${esc(s.destination)}","status":"${esc(s.status)}","date":"${s.jalaliYear}/${s.jalaliMonth}/${s.jalaliDay}","notes":"${esc(s.notes)}"}"""
        }
        return "[$json]"
    }

    private fun serveImage(shipmentIdStr: String): Response {
        val id = shipmentIdStr.toIntOrNull() ?: return notFound()
        val shipment = runBlocking { repository.getById(id).first() } ?: return notFound()
        val path = shipment.imagePath ?: return notFound()
        val file = File(path)
        if (!file.exists()) return notFound()

        return newChunkedResponse(Response.Status.OK, "image/jpeg", FileInputStream(file))
    }

    private fun htmlResponse(body: String, code: Int = 200): Response =
        newFixedLengthResponse(
            if (code == 200) Response.Status.OK else Response.Status.NOT_FOUND,
            "text/html; charset=utf-8",
            body
        )

    private fun jsonResponse(body: String): Response =
        newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", body)

    private fun notFound(): Response =
        newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not found")

    private fun esc(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    companion object {
        fun getLocalIpAddress(): String? {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                val addrs = intf.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
            return null
        }
    }
}
