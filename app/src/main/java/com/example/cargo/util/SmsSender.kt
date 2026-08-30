package com.example.cargo.util

import android.content.Context
import android.telephony.SmsManager
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * ارسال پیامک — دو روش:
 * 1. از سیم‌کارت خود گوشی (SmsManager) — رایگان از شارژ
 * 2. از API سرویس پیامکی (کاوه‌نگار) — برای ارسال انبوه
 */
object SmsSender {

    const val DEFAULT_MESSAGE = "سفارش شما در حال بسته بندی و ارسال میباشد"

    /** ارسال از سیم‌کارت خود گوشی */
    fun sendViaSim(phone: String, message: String): Boolean {
        return try {
            val sm = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val ctx = null // will use default
                SmsManager.getDefault()
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            // Long messages are split automatically
            val parts = sm.divideMessage(message)
            if (parts.size == 1) {
                sm.sendTextMessage(phone, null, message, null, null)
            } else {
                sm.sendMultipartTextMessage(phone, null, parts, null, null)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * ارسال از API کاوه‌نگار (kavenegar.com)
     * sender = شماره خط ارسال (مثلا 10008663) — خالی باشد = پیش‌فرض کاوه‌نگار
     */
    suspend fun sendViaKavenegar(apiKey: String, sender: String, phone: String, message: String): Pair<Boolean, String> =
        withContext(Dispatchers.IO) {
            try {
                val url = URL("https://api.kavenegar.com/v1/$apiKey/sms/send.json")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.connectTimeout = 10_000
                conn.readTimeout = 15_000

                val body = StringBuilder()
                body.append("receptor=").append(URLEncoder.encode(phone, "UTF-8"))
                body.append("&message=").append(URLEncoder.encode(message, "UTF-8"))
                if (sender.isNotBlank()) {
                    body.append("&sender=").append(URLEncoder.encode(sender, "UTF-8"))
                }

                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { w ->
                    w.write(body.toString())
                    w.flush()
                }

                val code = conn.responseCode
                val response = conn.inputStream.bufferedReader().readText()
                conn.disconnect()

                if (code in 200..299) Pair(true, "OK")
                else Pair(false, "HTTP $code: ${response.take(200)}")
            } catch (e: Exception) {
                Pair(false, e.message ?: "error")
            }
        }
}
