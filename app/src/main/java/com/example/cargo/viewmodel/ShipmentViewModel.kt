package com.example.cargo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cargo.CargoApp
import com.example.cargo.data.SettingsRepository
import com.example.cargo.data.Shipment
import com.example.cargo.data.ShipmentRepository
import com.example.cargo.util.JalaliDate
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

    fun insert(
        description: String,
        sender: String,
        receiver: String,
        destination: String,
        notes: String,
        status: String,
        imagePaths: String,
        onComplete: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            val today = JalaliDate.today()
            val s = Shipment(
                cargoDescription = description.trim(),
                senderName = sender.trim(),
                receiverName = receiver.trim(),
                destination = destination.trim(),
                notes = notes.trim(),
                status = status,
                imagePath = imagePaths.split("|").firstOrNull() { it.isNotBlank() },
                imagePaths = imagePaths,
                jalaliYear = today.year,
                jalaliMonth = today.month,
                jalaliDay = today.day
            )
            val id = repo.insert(s)
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
}