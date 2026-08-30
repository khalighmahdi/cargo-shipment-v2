package com.example.cargo.util

import android.telephony.SmsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * ارسال پیامک — دو روش:
 * 1. از سیم‌کارت خود گوشی (SmsManager) — رایگان از شارژ
 * 2. API عمومی: هر سرویس پیامکی با قالب URL
 *    مثال کاوه‌نگار:
 *    https://api.kavenegar.com/v1/{api_key}/sms/send.json?receptor={phone}&message={message}&sender={sender}
 *
 *    placeholder ها: {api_key} {sender} {phone} {message}
 */
object SmsSender {

    const val DEFAULT_MESSAGE = "سفارش شما در حال بسته بندی و ارسال میباشد"

    const val KAVENEGAR_TEMPLATE =
        "https://api.kavenegar.com/v1/{api_key}/sms/send.json?receptor={phone}&message={message}&sender={sender}"

    /** ارسال از سیم‌کارت خود گوشی */
    fun sendViaSim(phone: String, message: String): Boolean {
        return try {
            @Suppress("DEPRECATION")
            val sm = SmsManager.getDefault()
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
     * ارسال از API عمومی — قالب URL با placeholder ها
     * هر سرویسی که GET با پارامتر داشته باشه کار می‌کنه
     */
    suspend fun sendViaApi(
        urlTemplate: String,
        apiKey: String,
        sender: String,
        phone: String,
        message: String
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            if (urlTemplate.isBlank()) return@withContext Pair(false, "آدرس API تنظیم نشده")

            val url = urlTemplate
                .replace("{api_key}", URLEncoder.encode(apiKey, "UTF-8"))
                .replace("{sender}", URLEncoder.encode(sender, "UTF-8"))
                .replace("{phone}", URLEncoder.encode(phone, "UTF-8"))
                .replace("{message}", URLEncoder.encode(message, "UTF-8"))

            val conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000

            val code = conn.responseCode
            val response = try {
                val stream = if (code in 200..399) conn.inputStream else conn.errorStream
                stream?.bufferedReader()?.readText() ?: ""
            } catch (e: Exception) {
                ""
            }
            conn.disconnect()

            if (code in 200..299) Pair(true, "OK")
            else Pair(false, "HTTP $code: ${response.take(150)}")
        } catch (e: Exception) {
            Pair(false, e.message ?: "خطای نامشخص")
        }
    }
}
