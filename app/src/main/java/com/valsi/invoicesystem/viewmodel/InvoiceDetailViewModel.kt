package com.valsi.invoicesystem.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valsi.invoicesystem.data.entity.AppSettings
import com.valsi.invoicesystem.data.entity.InvoiceDetail
import com.valsi.invoicesystem.data.entity.PaymentStatus
import com.valsi.invoicesystem.data.repository.InvoiceRepository
import com.valsi.invoicesystem.data.repository.SettingsRepository
import com.valsi.invoicesystem.navigation.Routes
import com.valsi.invoicesystem.pdf.PdfInvoiceGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

enum class PdfAction { SHARE, PRINT, SAVE, VIEW }

sealed interface DetailEvent {
    data class PdfReady(val file: File, val action: PdfAction, val invoiceNumber: String) : DetailEvent
    data class Message(val text: String) : DetailEvent
}

data class InvoiceDetailUiState(
    val detail: InvoiceDetail? = null,
    val settings: AppSettings = AppSettings(),
    val loading: Boolean = true,
    val pdfInProgress: Boolean = false,
)

@HiltViewModel
class InvoiceDetailViewModel @Inject constructor(
    private val invoiceRepository: InvoiceRepository,
    private val settingsRepository: SettingsRepository,
    private val pdfGenerator: PdfInvoiceGenerator,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val invoiceId: Long = savedStateHandle[Routes.ARG_INVOICE_ID] ?: 0L

    val state: StateFlow<InvoiceDetailUiState> = combine(
        invoiceRepository.observeDetail(invoiceId),
        settingsRepository.observe(),
    ) { detail, settings ->
        InvoiceDetailUiState(detail = detail, settings = settings, loading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InvoiceDetailUiState())

    private val _events = Channel<DetailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun preparePdf(action: PdfAction) {
        val current = state.value
        val detail = current.detail ?: return
        if (current.pdfInProgress) return
        viewModelScope.launch {
            try {
                val file = pdfGenerator.generate(detail, current.settings)
                _events.send(DetailEvent.PdfReady(file, action, detail.invoice.invoiceNumber))
            } catch (e: Exception) {
                _events.send(DetailEvent.Message("Could not generate PDF: ${e.message}"))
            }
        }
    }

    fun updatePaymentStatus(status: PaymentStatus, amountPaid: Double) {
        viewModelScope.launch {
            invoiceRepository.updatePaymentStatus(invoiceId, status, amountPaid)
            _events.send(DetailEvent.Message("Payment status updated."))
        }
    }

    fun voidInvoice() {
        viewModelScope.launch {
            invoiceRepository.voidInvoice(invoiceId)
            _events.send(DetailEvent.Message("Invoice voided."))
        }
    }

    /**
     * Permanently deletes this invoice and its line items, then refreshes the customer's
     * outstanding balance. Works for drafts and for finalized invoices entered by mistake —
     * deleting a finalized one leaves a gap in the invoice numbering (the screen warns first).
     */
    fun deleteInvoice(onDeleted: () -> Unit) {
        val invoice = state.value.detail?.invoice ?: return
        viewModelScope.launch {
            try {
                invoiceRepository.deleteInvoice(invoice)
                onDeleted()
            } catch (e: Exception) {
                _events.send(DetailEvent.Message("Could not delete invoice: ${e.message}"))
            }
        }
    }
}
