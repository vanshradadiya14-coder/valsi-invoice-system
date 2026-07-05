package com.valsi.invoicesystem.viewmodel

import android.util.Patterns
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valsi.invoicesystem.data.entity.Customer
import com.valsi.invoicesystem.data.repository.CustomerRepository
import com.valsi.invoicesystem.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomerFormState(
    val storeName: String = "",
    val ownerName: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val address: String = "",
    val paymentTerms: String = "Net 7",
    val notes: String = "",
    val storeNameError: String? = null,
    val ownerNameError: String? = null,
    val phoneError: String? = null,
    val emailError: String? = null,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
) {
    companion object {
        val PAYMENT_TERMS = listOf("Cash on Delivery", "Net 7", "Net 15", "Net 30")
    }
}

@HiltViewModel
class CustomerEditViewModel @Inject constructor(
    private val repository: CustomerRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val customerId: Long = savedStateHandle[Routes.ARG_CUSTOMER_ID] ?: 0L
    val isEditing: Boolean = customerId != 0L

    private var loadedCustomer: Customer? = null

    private val _state = MutableStateFlow(CustomerFormState())
    val state: StateFlow<CustomerFormState> = _state.asStateFlow()

    init {
        if (isEditing) {
            viewModelScope.launch {
                repository.getById(customerId)?.let { c ->
                    loadedCustomer = c
                    _state.value = CustomerFormState(
                        storeName = c.storeName,
                        ownerName = c.ownerName,
                        phoneNumber = c.phoneNumber,
                        email = c.email.orEmpty(),
                        address = c.address,
                        paymentTerms = c.paymentTerms,
                        notes = c.notes.orEmpty(),
                    )
                }
            }
        }
    }

    fun onStoreName(v: String) = _state.update { it.copy(storeName = v, storeNameError = null) }
    fun onOwnerName(v: String) = _state.update { it.copy(ownerName = v, ownerNameError = null) }
    fun onPhone(v: String) = _state.update { it.copy(phoneNumber = v, phoneError = null) }
    fun onEmail(v: String) = _state.update { it.copy(email = v, emailError = null) }
    fun onAddress(v: String) = _state.update { it.copy(address = v) }
    fun onPaymentTerms(v: String) = _state.update { it.copy(paymentTerms = v) }
    fun onNotes(v: String) = _state.update { it.copy(notes = v) }

    fun save() {
        val current = _state.value
        val storeNameError = if (current.storeName.isBlank()) "Store name is required" else null
        val ownerNameError = if (current.ownerName.isBlank()) "Owner name is required" else null
        val phoneError = when {
            current.phoneNumber.isBlank() -> "Phone number is required"
            !isValidPhone(current.phoneNumber) -> "Enter a valid phone number"
            else -> null
        }
        val emailError = if (current.email.isNotBlank() && !isValidEmail(current.email)) {
            "Enter a valid email"
        } else {
            null
        }

        if (storeNameError != null || ownerNameError != null || phoneError != null || emailError != null) {
            _state.update {
                it.copy(
                    storeNameError = storeNameError,
                    ownerNameError = ownerNameError,
                    phoneError = phoneError,
                    emailError = emailError,
                )
            }
            return
        }

        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val customer = (loadedCustomer ?: Customer(
                storeName = "",
                ownerName = "",
                phoneNumber = "",
            )).copy(
                storeName = current.storeName.trim(),
                ownerName = current.ownerName.trim(),
                phoneNumber = current.phoneNumber.trim(),
                email = current.email.trim().ifBlank { null },
                address = current.address.trim(),
                paymentTerms = current.paymentTerms,
                notes = current.notes.trim().ifBlank { null },
            )
            if (isEditing) repository.update(customer) else repository.add(customer)
            _state.update { it.copy(isSaving = false, saved = true) }
        }
    }

    private fun isValidPhone(phone: String): Boolean {
        val digits = phone.count { it.isDigit() }
        return digits in 7..15
    }

    private fun isValidEmail(email: String): Boolean =
        Patterns.EMAIL_ADDRESS.matcher(email).matches()
}
