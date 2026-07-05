package com.valsi.invoicesystem.data.model

import com.valsi.invoicesystem.data.entity.InvoiceStatus
import com.valsi.invoicesystem.data.entity.PaymentStatus

/** One line of an in-progress invoice cart. */
data class NewInvoiceLine(
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
) {
    val lineTotal: Double get() = quantity * unitPrice
}

/**
 * Everything needed to persist a new invoice. Monetary totals (subtotal, grand total) are
 * recomputed authoritatively by the repository from [lines], [discountAmount] and [vatAmount]
 * so the stored document can never drift from its line items.
 */
data class NewInvoiceRequest(
    val customerId: Long,
    val lines: List<NewInvoiceLine>,
    val discountAmount: Double,
    val vatAmount: Double,
    val paymentStatus: PaymentStatus,
    val amountPaid: Double,
    val notes: String?,
    val status: InvoiceStatus = InvoiceStatus.FINALIZED,
)
