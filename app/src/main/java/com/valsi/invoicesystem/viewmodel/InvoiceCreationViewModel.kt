package com.valsi.invoicesystem.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valsi.invoicesystem.data.entity.Customer
import com.valsi.invoicesystem.data.entity.InvoiceStatus
import com.valsi.invoicesystem.data.entity.PaymentStatus
import com.valsi.invoicesystem.data.entity.Product
import com.valsi.invoicesystem.data.model.NewInvoiceLine
import com.valsi.invoicesystem.data.model.NewInvoiceRequest
import com.valsi.invoicesystem.data.repository.CustomerRepository
import com.valsi.invoicesystem.data.repository.InvoiceRepository
import com.valsi.invoicesystem.data.repository.ProductRepository
import com.valsi.invoicesystem.data.repository.SettingsRepository
import com.valsi.invoicesystem.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class InvoiceStep { CUSTOMER, PRODUCTS, CART, SUMMARY }

enum class DiscountMode { FLAT, PERCENT }

data class CartLine(
    val productId: Long,
    val productName: String,
    val unit: String,
    val quantity: Int,
    val unitPrice: Double,
) {
    val lineTotal: Double get() = quantity * unitPrice
}

data class InvoiceCreationState(
    val step: InvoiceStep = InvoiceStep.CUSTOMER,
    val selectedCustomer: Customer? = null,
    val cart: List<CartLine> = emptyList(),
    val discountMode: DiscountMode = DiscountMode.FLAT,
    val discountInput: String = "",
    val vatPercentInput: String = "",
    val paymentStatus: PaymentStatus = PaymentStatus.UNPAID,
    val amountPaidInput: String = "",
    val notes: String = "",
    val currencySymbol: String = "£",
    val isSaving: Boolean = false,
    val generatedInvoiceId: Long? = null,
    val savedAsDraft: Boolean = false,
    val error: String? = null,
) {
    val itemCount: Int get() = cart.sumOf { it.quantity }
    val subtotal: Double get() = cart.sumOf { it.lineTotal }
    /** True once the rep has entered anything worth keeping. */
    val hasContent: Boolean get() = selectedCustomer != null || cart.isNotEmpty()

    val discountAmount: Double
        get() {
            val v = discountInput.toDoubleOrNull() ?: 0.0
            return when (discountMode) {
                DiscountMode.PERCENT -> (subtotal * v / 100.0).coerceIn(0.0, subtotal)
                DiscountMode.FLAT -> v.coerceIn(0.0, subtotal)
            }
        }

    val vatPercent: Double get() = vatPercentInput.toDoubleOrNull() ?: 0.0
    val vatAmount: Double get() = ((subtotal - discountAmount) * vatPercent / 100.0).coerceAtLeast(0.0)
    val grandTotal: Double get() = subtotal - discountAmount + vatAmount
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InvoiceCreationViewModel @Inject constructor(
    private val customerRepository: CustomerRepository,
    private val productRepository: ProductRepository,
    private val invoiceRepository: InvoiceRepository,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val duplicateFrom: Long = savedStateHandle[Routes.ARG_DUPLICATE_FROM] ?: Routes.NO_ID
    private val editDraftId: Long = savedStateHandle[Routes.ARG_EDIT_DRAFT] ?: Routes.NO_ID

    // Set when resuming a draft, so finishing/saving replaces it instead of duplicating.
    private var replaceDraftId: Long? = editDraftId.takeIf { it != Routes.NO_ID }

    private val _state = MutableStateFlow(InvoiceCreationState())
    val state: StateFlow<InvoiceCreationState> = _state.asStateFlow()

    private val _customerQuery = MutableStateFlow("")
    val customerQuery: StateFlow<String> = _customerQuery.asStateFlow()

    private val _productQuery = MutableStateFlow("")
    val productQuery: StateFlow<String> = _productQuery.asStateFlow()

    val customers: StateFlow<List<Customer>> = _customerQuery
        .flatMapLatest { customerRepository.search(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Active products filtered by the product search box. */
    val products: StateFlow<List<Product>> = combine(
        productRepository.observeActive(),
        _productQuery,
    ) { list, query ->
        if (query.isBlank()) list
        else list.filter { it.name.contains(query.trim(), ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            val settings = settingsRepository.getOnce()
            _state.update {
                it.copy(
                    currencySymbol = settings.currencySymbol,
                    vatPercentInput = trimTrailingZero(settings.defaultVatPercent),
                )
            }
            when {
                editDraftId != Routes.NO_ID -> prefillFromInvoice(editDraftId, useCurrentPrices = false)
                duplicateFrom != Routes.NO_ID -> prefillFromInvoice(duplicateFrom, useCurrentPrices = true)
            }
        }
    }

    /**
     * Pre-fill customer and lines from an existing invoice.
     * @param useCurrentPrices true for Duplicate (latest prices); false for resuming a draft
     *        (keep the prices the rep already entered).
     */
    private suspend fun prefillFromInvoice(invoiceId: Long, useCurrentPrices: Boolean) {
        val source = invoiceRepository.getWithItems(invoiceId) ?: return
        val customer = customerRepository.getById(source.invoice.customerId)
        val lines = source.items.map { item ->
            val product = productRepository.getById(item.productId)
            CartLine(
                productId = item.productId,
                productName = product?.name ?: item.productNameSnapshot,
                unit = product?.unit ?: "pack",
                quantity = item.quantity,
                unitPrice = if (useCurrentPrices) product?.currentPrice ?: item.unitPriceSnapshot
                else item.unitPriceSnapshot,
            )
        }
        _state.update {
            it.copy(
                selectedCustomer = customer,
                cart = lines,
                notes = source.invoice.notes.orEmpty(),
            )
        }
    }

    // ---- Step navigation ----
    fun goToStep(step: InvoiceStep) = _state.update { it.copy(step = step) }

    fun next() = _state.update { s ->
        val order = InvoiceStep.entries
        val nextIndex = (order.indexOf(s.step) + 1).coerceAtMost(order.lastIndex)
        s.copy(step = order[nextIndex])
    }

    fun back() = _state.update { s ->
        val order = InvoiceStep.entries
        val prevIndex = (order.indexOf(s.step) - 1).coerceAtLeast(0)
        s.copy(step = order[prevIndex])
    }

    // ---- Customer ----
    fun onCustomerQueryChange(v: String) { _customerQuery.value = v }

    fun selectCustomer(customer: Customer) =
        _state.update { it.copy(selectedCustomer = customer, step = InvoiceStep.PRODUCTS) }

    // ---- Products / cart ----
    fun onProductQueryChange(v: String) { _productQuery.value = v }

    fun addToCart(product: Product, quantity: Int) {
        if (quantity <= 0) return
        _state.update { s ->
            val existing = s.cart.find { it.productId == product.id }
            val newCart = if (existing != null) {
                s.cart.map {
                    if (it.productId == product.id) it.copy(quantity = it.quantity + quantity) else it
                }
            } else {
                s.cart + CartLine(
                    productId = product.id,
                    productName = product.name,
                    unit = product.unit,
                    quantity = quantity,
                    unitPrice = product.currentPrice,
                )
            }
            s.copy(cart = newCart)
        }
    }

    fun setLineQuantity(productId: Long, quantity: Int) {
        _state.update { s ->
            val newCart = if (quantity <= 0) {
                s.cart.filterNot { it.productId == productId }
            } else {
                s.cart.map { if (it.productId == productId) it.copy(quantity = quantity) else it }
            }
            s.copy(cart = newCart)
        }
    }

    fun setLineUnitPrice(productId: Long, unitPrice: Double) {
        _state.update { s ->
            s.copy(cart = s.cart.map {
                if (it.productId == productId) it.copy(unitPrice = unitPrice.coerceAtLeast(0.0)) else it
            })
        }
    }

    fun removeLine(productId: Long) =
        _state.update { s -> s.copy(cart = s.cart.filterNot { it.productId == productId }) }

    // ---- Summary ----
    fun setDiscountMode(mode: DiscountMode) = _state.update { it.copy(discountMode = mode) }
    fun onDiscountInput(v: String) = _state.update { it.copy(discountInput = v) }
    fun onVatInput(v: String) = _state.update { it.copy(vatPercentInput = v) }
    fun onNotes(v: String) = _state.update { it.copy(notes = v) }
    fun onAmountPaidInput(v: String) = _state.update { it.copy(amountPaidInput = v) }

    fun setPaymentStatus(status: PaymentStatus) = _state.update {
        // When switching to PAID pre-fill amount to grand total for clarity.
        val amount = when (status) {
            PaymentStatus.PAID -> trimTrailingZero(it.grandTotal)
            PaymentStatus.UNPAID -> ""
            PaymentStatus.PARTIAL -> it.amountPaidInput
        }
        it.copy(paymentStatus = status, amountPaidInput = amount)
    }

    fun clearError() = _state.update { it.copy(error = null) }

    private fun buildRequest(current: InvoiceCreationState, status: InvoiceStatus) =
        NewInvoiceRequest(
            customerId = current.selectedCustomer!!.id,
            lines = current.cart.map {
                NewInvoiceLine(it.productId, it.productName, it.quantity, it.unitPrice)
            },
            discountAmount = current.discountAmount,
            vatAmount = current.vatAmount,
            paymentStatus = current.paymentStatus,
            // Repository normalizes amountPaid against the final status and grand total.
            amountPaid = current.amountPaidInput.toDoubleOrNull() ?: 0.0,
            notes = current.notes.trim().ifBlank { null },
            status = status,
        )

    // ---- Generate (finalize) ----
    fun generateInvoice() {
        val current = _state.value
        when {
            current.selectedCustomer == null -> {
                _state.update { it.copy(error = "Please select a customer.", step = InvoiceStep.CUSTOMER) }
                return
            }
            current.cart.isEmpty() -> {
                _state.update { it.copy(error = "Add at least one product.", step = InvoiceStep.PRODUCTS) }
                return
            }
        }

        _state.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            try {
                val request = buildRequest(current, InvoiceStatus.FINALIZED)
                val id = invoiceRepository.createInvoice(request, replaceDraftId = replaceDraftId)
                _state.update { it.copy(isSaving = false, generatedInvoiceId = id) }
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, error = "Could not save invoice: ${e.message}") }
            }
        }
    }

    /** Saves the in-progress invoice as a DRAFT so nothing is lost. Requires a customer. */
    fun saveAsDraft() {
        val current = _state.value
        if (current.selectedCustomer == null) {
            _state.update { it.copy(error = "Pick a customer before saving a draft.", step = InvoiceStep.CUSTOMER) }
            return
        }
        _state.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            try {
                val request = buildRequest(current, InvoiceStatus.DRAFT)
                val newId = invoiceRepository.createInvoice(request, replaceDraftId = replaceDraftId)
                replaceDraftId = newId // in case the flow continues after saving
                _state.update { it.copy(isSaving = false, savedAsDraft = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, error = "Could not save draft: ${e.message}") }
            }
        }
    }

    private fun trimTrailingZero(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
}
