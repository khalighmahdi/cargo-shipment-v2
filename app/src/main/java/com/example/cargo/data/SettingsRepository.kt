package com.example.cargo.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.cargo.util.SmsSender
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    companion object {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        // SMS settings - generic
        val SMS_ENABLED = booleanPreferencesKey("sms_enabled")
        val SMS_METHOD = stringPreferencesKey("sms_method")       // "sim", "api_get", "api_post"
        val SMS_API_URL = stringPreferencesKey("sms_api_url")     // generic URL template
        val SMS_API_BODY = stringPreferencesKey("sms_api_body")   // POST body template
        val SMS_API_HEADERS = stringPreferencesKey("sms_api_headers") // POST headers template
        val SMS_API_KEY = stringPreferencesKey("sms_api_key")     // API key for the service
        val SMS_SENDER = stringPreferencesKey("sms_sender")       // sender line number
        val SMS_TEMPLATE = stringPreferencesKey("sms_template")
    }

    val darkMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[DARK_MODE] ?: true
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[DARK_MODE] = enabled
        }
    }

    // ===== SMS =====
    val smsEnabled: Flow<Boolean> = context.dataStore.data.map { it[SMS_ENABLED] ?: true }
    val smsMethod: Flow<String> = context.dataStore.data.map { it[SMS_METHOD] ?: "sim" }
    val smsApiUrl: Flow<String> = context.dataStore.data.map { it[SMS_API_URL] ?: SmsSender.KAVENEGAR_URL }
    val smsApiBody: Flow<String> = context.dataStore.data.map { it[SMS_API_BODY] ?: SmsSender.SMSIR_BODY }
    val smsApiHeaders: Flow<String> = context.dataStore.data.map { it[SMS_API_HEADERS] ?: SmsSender.SMSIR_HEADERS }
    val smsApiKey: Flow<String> = context.dataStore.data.map { it[SMS_API_KEY] ?: "" }
    val smsSender: Flow<String> = context.dataStore.data.map { it[SMS_SENDER] ?: "" }
    val smsTemplate: Flow<String> = context.dataStore.data.map { it[SMS_TEMPLATE] ?: SmsSender.DEFAULT_MESSAGE }

    suspend fun setSmsEnabled(enabled: Boolean) =
        context.dataStore.edit { it[SMS_ENABLED] = enabled }

    suspend fun setSmsMethod(method: String) =
        context.dataStore.edit { it[SMS_METHOD] = method }

    suspend fun setSmsApiUrl(url: String) =
        context.dataStore.edit { it[SMS_API_URL] = url }

    suspend fun setSmsApiBody(body: String) =
        context.dataStore.edit { it[SMS_API_BODY] = body }

    suspend fun setSmsApiHeaders(headers: String) =
        context.dataStore.edit { it[SMS_API_HEADERS] = headers }

    suspend fun setSmsApiKey(key: String) =
        context.dataStore.edit { it[SMS_API_KEY] = key }

    suspend fun setSmsSender(sender: String) =
        context.dataStore.edit { it[SMS_SENDER] = sender }

    suspend fun setSmsTemplate(template: String) =
        context.dataStore.edit { it[SMS_TEMPLATE] = template }

    /**
     * اسنپ‌شات یک‌جا از همه تنظیمات پیامک — یک بار از DataStore می‌خواند تا
     * خواندن‌های تکه‌تکه (که ممکن است بین‌شان مقدار عوض شود) اتفاق نیفتد.
     */
    data class SmsSettings(
        val enabled: Boolean,
        val method: String,
        val apiUrl: String,
        val apiBody: String,
        val apiHeaders: String,
        val apiKey: String,
        val sender: String,
        val template: String
    )

    suspend fun smsSettingsSnapshot(): SmsSettings {
        val prefs = context.dataStore.data.first()
        return SmsSettings(
            enabled = prefs[SMS_ENABLED] ?: true,
            method = prefs[SMS_METHOD] ?: "sim",
            apiUrl = prefs[SMS_API_URL] ?: SmsSender.KAVENEGAR_URL,
            apiBody = prefs[SMS_API_BODY] ?: SmsSender.SMSIR_BODY,
            apiHeaders = prefs[SMS_API_HEADERS] ?: SmsSender.SMSIR_HEADERS,
            apiKey = prefs[SMS_API_KEY] ?: "",
            sender = prefs[SMS_SENDER] ?: "",
            template = prefs[SMS_TEMPLATE] ?: SmsSender.DEFAULT_MESSAGE
        )
    }
}

object SmsSenderTemplate {
    const val DEFAULT = "سفارش شما در حال بسته بندی و ارسال میباشد"
}