package com.example.cargo.util

import android.telephony.SmsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * ارسال پیامک — سه روش:
 * 1. از سیم‌کارت خود گوشی (SmsManager) — رایگان از شارژ
 * 2. API با GET: هر سرویس پیامکی با قالب URL
 *    مثال کاوه‌نگار:
 *    https://api.kavenegar.com/v1/{api_key}/sms/send.json?receptor={phone}&message={message}&sender={sender}
 * 3. API با POST JSON: مثلا SMS.ir
 *    POST https://api.sms.ir/v1/send/bulk
 *    هدر: X-API-KEY: {api_key}
 *    بدنه: {"lineNumber": {sender}, "messageText": "{message}", "mobiles": ["{phone}"]}
 *
 *    placeholder ها: {api_key} {sender} {phone} {message}
 */
object SmsSender {

    const val DEFAULT_MESSAGE = "سفارش شما در حال بسته بندی و ارسال میباشد"

    // ===== Preset templates =====
    const val KAVENEGAR_URL =
        "https://api.kavenegar.com/v1/{api_key}/sms/send.json?receptor={phone}&message={message}&sender={sender}"

    const val SMSIR_URL = "https://api.sms.ir/v1/send/bulk"
    const val SMSIR_BODY =
        "{\"lineNumber\": {sender}, \"messageText\": \"{message}\", \"mobiles\": [\"{phone}\"]}"
    const val SMSIR_HEADERS = "X-API-KEY: {api_key}"

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
     * ارسال از API عمومی — GET (query params) یا POST (JSON body + هدر)
     */
    suspend fun sendViaApi(
        method: String,
        urlTemplate: String,
        bodyTemplate: String,
        headersTemplate: String,
        apiKey: String,
        sender: String,
        phone: String,
        message: String
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            if (urlTemplate.isBlank()) return@withContext Pair(false, "آدرس API تنظیم نشده")

            val isPost = method.equals("POST", ignoreCase = true)

            fun jsonEscape(s: String): String =
                s.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "")
                    .replace("\t", "\\t")

            var requestUrl: String
            var body: String? = null

            if (isPost) {
                requestUrl = urlTemplate
                body = bodyTemplate
                    .replace("{api_key}", apiKey)
                    .replace("{sender}", sender)
                    .replace("{phone}", phone)
                    .replace("{message}", jsonEscape(message))
            } else {
                requestUrl = urlTemplate
                    .replace("{api_key}", URLEncoder.encode(apiKey, "UTF-8"))
                    .replace("{sender}", URLEncoder.encode(sender, "UTF-8"))
                    .replace("{phone}", URLEncoder.encode(phone, "UTF-8"))
                    .replace("{message}", URLEncoder.encode(message, "UTF-8"))
            }

            val conn = URL(requestUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.requestMethod = if (isPost) "POST" else "GET"

            // هدرهای سفارشی (برای GET هم اعمال می‌شه)
            if (headersTemplate.isNotBlank()) {
                headersTemplate.split("\n").forEach { line ->
                    val idx = line.indexOf(":")
                    if (idx > 0) {
                        val name = line.substring(0, idx).trim()
                        val value = line.substring(idx + 1)
                            .replace("{api_key}", apiKey)
                            .replace("{sender}", sender)
                            .replace("{phone}", phone)
                            .trim()
                        if (name.isNotBlank() && value.isNotBlank()) {
                            conn.setRequestProperty(name, value)
                        }
                    }
                }
            }

            if (isPost) {
                conn.doOutput = true
                if (conn.getRequestProperty("Content-Type") == null) {
                    conn.setRequestProperty("Content-Type", "application/json")
                }
                body?.let { b ->
                    conn.outputStream.use { os ->
                        os.write(b.toByteArray(Charsets.UTF_8))
                        os.flush()
                    }
                }
            }

            val code = conn.responseCode
            val response = try {
                val stream = if (code in 200..399) conn.inputStream else conn.errorStream
                stream?.bufferedReader()?.readText() ?: ""
            } catch (e: Exception) {
                ""
            }
            conn.disconnect()

            if (code in 200..299) {
                // بررسی status داخل JSON (سبک SMS.ir: status=1 موفق)
                val statusMatch = Regex("\"status\"\\s*:\\s*(-?\\d+)").find(response)
                if (statusMatch != null) {
                    val st = statusMatch.groupValues[1].toIntOrNull() ?: -1
                    if (st == 1) {
                        Pair(true, "OK")
                    } else {
                        val msgMatch = Regex("\"message\"\\s*:\\s*\"([^\"]*)\"").find(response)
                        Pair(false, "کد $st: ${msgMatch?.groupValues?.get(1) ?: response.take(120)}")
                    }
                } else {
                    Pair(true, "OK")
                }
            } else {
                Pair(false, "HTTP $code: ${response.take(150)}")
            }
        } catch (e: Exception) {
            Pair(false, e.message ?: "خطای نامشخص")
        }
    }
}
