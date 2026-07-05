package com.valsi.invoicesystem.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valsi.invoicesystem.data.entity.AppSettings
import com.valsi.invoicesystem.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsFormState(
    val companyName: String = "",
    val companyLogoUri: String? = null,
    val companyAddress: String = "",
    val companyPhone: String = "",
    val companyEmail: String = "",
    val vatNumber: String = "",
    val currencySymbol: String = "£",
    val defaultVatPercent: String = "20",
    val invoicePrefix: String = "VLS-",
    val nextInvoiceNumber: Int = 1,
    val isSaving: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsFormState())
    val state: StateFlow<SettingsFormState> = _state.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    init {
        viewModelScope.launch {
            val s = repository.getOnce()
            _state.value = SettingsFormState(
                companyName = s.companyName,
                companyLogoUri = s.companyLogoUri,
                companyAddress = s.companyAddress,
                companyPhone = s.companyPhone,
                companyEmail = s.companyEmail,
                vatNumber = s.vatNumber,
                currencySymbol = s.currencySymbol,
                defaultVatPercent = trimTrailingZero(s.defaultVatPercent),
                invoicePrefix = s.invoicePrefix,
                nextInvoiceNumber = s.nextInvoiceNumber,
            )
        }
    }

    fun onCompanyName(v: String) = _state.update { it.copy(companyName = v) }
    fun onAddress(v: String) = _state.update { it.copy(companyAddress = v) }
    fun onPhone(v: String) = _state.update { it.copy(companyPhone = v) }
    fun onEmail(v: String) = _state.update { it.copy(companyEmail = v) }
    fun onVatNumber(v: String) = _state.update { it.copy(vatNumber = v) }
    fun onCurrency(v: String) = _state.update { it.copy(currencySymbol = v) }
    fun onDefaultVat(v: String) = _state.update { it.copy(defaultVatPercent = v) }
    fun onInvoicePrefix(v: String) = _state.update { it.copy(invoicePrefix = v) }
    fun setLogoUri(uri: String?) = _state.update { it.copy(companyLogoUri = uri) }

    fun save() {
        val current = _state.value
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val settings = AppSettings(
                companyName = current.companyName.trim(),
                companyLogoUri = current.companyLogoUri,
                companyAddress = current.companyAddress.trim(),
                companyPhone = current.companyPhone.trim(),
                companyEmail = current.companyEmail.trim(),
                vatNumber = current.vatNumber.trim(),
                currencySymbol = current.currencySymbol.trim().ifBlank { "£" },
                defaultVatPercent = current.defaultVatPercent.toDoubleOrNull() ?: 20.0,
                invoicePrefix = current.invoicePrefix.trim().ifBlank { "VLS-" },
                // Preserve the counter — never reset it from the UI.
                nextInvoiceNumber = current.nextInvoiceNumber,
            )
            repository.save(settings)
            _state.update { it.copy(isSaving = false) }
            _messages.send("Settings saved.")
        }
    }

    private fun trimTrailingZero(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
}
