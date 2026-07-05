package com.valsi.invoicesystem.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valsi.invoicesystem.data.entity.Customer
import com.valsi.invoicesystem.data.entity.InvoiceWithCustomer
import com.valsi.invoicesystem.data.repository.CustomerRepository
import com.valsi.invoicesystem.data.repository.SettingsRepository
import com.valsi.invoicesystem.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class CustomerDetailState(
    val customer: Customer? = null,
    val invoices: List<InvoiceWithCustomer> = emptyList(),
    val currencySymbol: String = "£",
    val loading: Boolean = true,
)

@HiltViewModel
class CustomerDetailViewModel @Inject constructor(
    customerRepository: CustomerRepository,
    settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val customerId: Long = savedStateHandle[Routes.ARG_CUSTOMER_ID] ?: 0L

    val state: StateFlow<CustomerDetailState> = combineDetail(
        customerRepository, settingsRepository, customerId,
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CustomerDetailState())

    private fun combineDetail(
        customerRepository: CustomerRepository,
        settingsRepository: SettingsRepository,
        id: Long,
    ) = kotlinx.coroutines.flow.combine(
        customerRepository.observeById(id),
        customerRepository.observeInvoices(id),
        settingsRepository.observe(),
    ) { customer, invoices, settings ->
        CustomerDetailState(
            customer = customer,
            invoices = invoices,
            currencySymbol = settings.currencySymbol,
            loading = false,
        )
    }
}
