package com.example.cargo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cargo.CargoApp
import com.example.cargo.data.Contact
import com.example.cargo.data.SettingsRepository
import com.example.cargo.data.SmsSenderTemplate
import com.example.cargo.data.Shipment
import com.example.cargo.data.ShipmentRepository
import com.example.cargo.util.JalaliDate
import com.example.cargo.util.SmsSender
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ShipmentViewModel(application: Application) : AndroidViewModel(application) {

    private val repo: ShipmentRepository = (application as CargoApp).repository
    val contactRepo: com.example.cargo.data.ContactRepository = (application as CargoApp).contactRepository
    val settings: SettingsRepository = (application as CargoApp).settings

    // Search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Active filter (null = no filter, otherwise a status)
    private val _statusFilter = MutableStateFlow<String?>(null)
    val statusFilter: StateFlow<String?> = _statusFilter.asStateFlow()

    // Currently selected year
    private val _selectedYear = MutableStateFlow(JalaliDate.today().year)
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    // Currently selected month (0 = all)
    private val _selectedMonth = MutableStateFlow(0)
    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()

    val filteredShipments: StateFlow<List<Shipment>> = combine(
        _searchQuery,
        _statusFilter
    ) { q, status -> q to status }
        .flatMapLatest { (q, status) ->
            when {
                q.isNotBlank() -> repo.search(q)
                status != null -> repo.getByStatus(status)
                else -> repo.allShipments
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val countInTransit: StateFlow<Int> = repo.countByStatus(Shipment.STATUS_IN_TRANSIT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val countDelivered: StateFlow<Int> = repo.countByStatus(Shipment.STATUS_DELIVERED)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val countReturned: StateFlow<Int> = repo.countByStatus(Shipment.STATUS_RETURNED)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalCount: StateFlow<Int> = repo.count()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setSearch(q: String) { _searchQuery.value = q }
    fun clearSearch() { _searchQuery.value = "" }
    fun setStatusFilter(status: String?) { _statusFilter.value = status }
    fun setYear(year: Int) { _selectedYear.value = year }
    fun setMonth(month: Int) { _selectedMonth.value = month }

    fun getById(id: Int): Flow<Shipment?> = repo.getById(id)
    fun getByYear(year: Int): Flow<List<Shipment>> = repo.getByYear(year)
    fun getByMonth(year: Int, month: Int): Flow<List<Shipment>> = repo.getByMonth(year, month)
    fun getByDay(year: Int, month: Int, day: Int): Flow<List<Shipment>> = repo.getByDay(year, month, day)

    /** ارسال پیامک خودکار بر اساس تنظیمات (فقط فرستنده/صاحب بار) */
    fun sendAutoSms(shipment: Shipment, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            if (shipment.senderPhone.isBlank()) {
                onResult(false, "شماره صاحب بار ثبت نشده")
                return@launch
            }
            val enabled = firstOf(settings.smsEnabled, true)
            if (!enabled) {
                onResult(false, "پیامک خودکار خاموش است")
                return@launch
            }
            val method = firstOf(settings.smsMethod, "sim")
            val template = firstOf(settings.smsTemplate, SmsSenderTemplate.DEFAULT)

            if (method == "api") {
                val url = firstOf(settings.smsApiUrl, "")
                val apiKey = firstOf(settings.smsApiKey, "")
                if (url.isBlank()) {
                    onResult(false, "آدرس API تنظیم نشده")
                    return@launch
                }
                val sender = firstOf(settings.smsSender, "")
                val (ok, msg) = SmsSender.sendViaApi(url, apiKey, sender, shipment.senderPhone, template)
                if (ok) markSmsSent(shipment)
                onResult(ok, if (ok) "پیامک ارسال شد ✓" else "خطا: $msg")
            } else {
                val ok = SmsSender.sendViaSim(shipment.senderPhone, template)
                if (ok) markSmsSent(shipment)
                onResult(ok, if (ok) "پیامک ارسال شد ✓" else "خطا در ارسال پیامک (پرمیشن SMS؟)")
            }
        }
    }

    /** پیامک تست از صفحه تنظیمات */
    fun sendTestSms(phone: String, onResult: (Boolean, String) -> Unit) {
        sendAutoSms(Shipment(senderPhone = phone), onResult)
    }

    private fun markSmsSent(shipment: Shipment) {
        viewModelScope.launch { repo.update(shipment.copy(smsSent = true)) }
    }

    /** خواندن اولین مقدار از یک Flow (helper ساده) */
    private suspend fun <T> firstOf(flow: Flow<T>, default: T): T {
        var result = default
        flow.collect { result = it }
        return result
    }

    fun insert(
        description: String,
        sender: String,
        senderPhone: String,
        receiver: String,
        receiverPhone: String,
        destination: String,
        notes: String,
        status: String,
        imagePaths: String,
        sendSms: Boolean,
        onComplete: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            val today = JalaliDate.today()
            val s = Shipment(
                cargoDescription = description.trim(),
                senderName = sender.trim(),
                senderPhone = senderPhone.trim(),
                receiverName = receiver.trim(),
                receiverPhone = receiverPhone.trim(),
                destination = destination.trim(),
                notes = notes.trim(),
                status = status,
                imagePath = imagePaths.split("|").firstOrNull { it.isNotBlank() },
                imagePaths = imagePaths,
                jalaliYear = today.year,
                jalaliMonth = today.month,
                jalaliDay = today.day
            )
            val id = repo.insert(s)
            if (sendSms && senderPhone.isNotBlank()) {
                sendAutoSms(s.copy(id = id.toInt()))
            }
            // ذخیره خودکار در دفترچه تلفن اگر جدید باشد
            if (senderPhone.isNotBlank()) {
                val existing = contactRepo.getByPhone(senderPhone.trim())
                if (existing == null) {
                    contactRepo.insert(Contact(name = sender.trim(), phone = senderPhone.trim()))
                }
            }
            onComplete(id)
        }
    }

    fun update(shipment: Shipment) {
        viewModelScope.launch { repo.update(shipment) }
    }

    fun delete(shipment: Shipment) {
        viewModelScope.launch { repo.delete(shipment) }
    }

    fun deleteById(id: Int) {
        viewModelScope.launch { repo.deleteById(id) }
    }

    // Pending contact picks (from contacts book → Add screen)
    var pendingSenderName by mutableStateOf("")
    var pendingSenderPhone by mutableStateOf("")
    var pendingReceiverName by mutableStateOf("")
    var pendingReceiverPhone by mutableStateOf("")

    fun deleteContact(contact: Contact) {
        viewModelScope.launch { contactRepo.delete(contact) }
    }

    fun saveContact(contact: Contact) {
        viewModelScope.launch { contactRepo.insert(contact) }
    }
}