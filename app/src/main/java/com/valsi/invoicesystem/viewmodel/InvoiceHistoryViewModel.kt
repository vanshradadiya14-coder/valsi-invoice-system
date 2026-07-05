package com.valsi.invoicesystem.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valsi.invoicesystem.data.entity.Customer
import com.valsi.invoicesystem.data.entity.InvoiceWithCustomer
import com.valsi.invoicesystem.data.entity.PaymentStatus
import com.valsi.invoicesystem.data.repository.CustomerRepository
import com.valsi.invoicesystem.data.repository.InvoiceRepository
import com.valsi.invoicesystem.data.repository.SettingsRepository
import com.valsi.invoicesystem.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

enum class DatePreset { ALL, TODAY, WEEK, MONTH }

data class HistoryFilters(
    val query: String = "",
    val customerId: Long? = null,
    val paymentStatus: PaymentStatus? = null,
    val datePreset: DatePreset = DatePreset.ALL,
) {
    val startDate: Long?
        get() = when (datePreset) {
            DatePreset.ALL -> null
            DatePreset.TODAY -> DateUtils.startOfDay()
            DatePreset.WEEK -> DateUtils.startOfDay(System.currentTimeMillis() - 6L * DAY_MS)
            DatePreset.MONTH -> DateUtils.startOfDay(System.currentTimeMillis() - 29L * DAY_MS)
        }
    val endDate: Long? get() = if (datePreset == DatePreset.ALL) null else DateUtils.endOfDay()

    private companion object {
        const val DAY_MS = 24L * 60 * 60 * 1000
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InvoiceHistoryViewModel @Inject constructor(
    private val invoiceRepository: InvoiceRepository,
    customerRepository: CustomerRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _filters = MutableStateFlow(HistoryFilters())
    val filters: StateFlow<HistoryFilters> = _filters.asStateFlow()

    val invoices: StateFlow<List<InvoiceWithCustomer>> = _filters
        .flatMapLatest { f ->
            invoiceRepository.observeFiltered(f.query, f.customerId, f.paymentStatus, f.startDate, f.endDate)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val customers: StateFlow<List<Customer>> = customerRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val currencySymbol: StateFlow<String> = settingsRepository.observe()
        .map { it.currencySymbol }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "£")

    fun onQueryChange(v: String) = _filters.update { it.copy(query = v) }
    fun onPaymentFilter(status: PaymentStatus?) = _filters.update { it.copy(paymentStatus = status) }
    fun onCustomerFilter(customerId: Long?) = _filters.update { it.copy(customerId = customerId) }
    fun onDatePreset(preset: DatePreset) = _filters.update { it.copy(datePreset = preset) }
    fun clearFilters() = _filters.update { HistoryFilters(query = it.query) }
}
