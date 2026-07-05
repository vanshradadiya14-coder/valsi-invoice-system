package com.valsi.invoicesystem.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valsi.invoicesystem.data.repository.InvoiceRepository
import com.valsi.invoicesystem.data.repository.SettingsRepository
import com.valsi.invoicesystem.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeUiState(
    val invoicesToday: Int = 0,
    val salesToday: Double = 0.0,
    val pendingPayments: Int = 0,
    val currencySymbol: String = "£",
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    invoiceRepository: InvoiceRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    // "Today" window is captured when the dashboard is first observed.
    private val startOfToday = DateUtils.startOfDay()
    private val endOfToday = DateUtils.endOfDay()

    val uiState: StateFlow<HomeUiState> = combine(
        invoiceRepository.observeCountBetween(startOfToday, endOfToday),
        invoiceRepository.observeSalesBetween(startOfToday, endOfToday),
        invoiceRepository.observePendingCount(),
        settingsRepository.observe(),
    ) { count, sales, pending, settings ->
        HomeUiState(
            invoicesToday = count,
            salesToday = sales,
            pendingPayments = pending,
            currencySymbol = settings.currencySymbol,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
