package com.valsi.invoicesystem.data.repository

import androidx.room.withTransaction
import com.valsi.invoicesystem.data.ValsiDatabase
import com.valsi.invoicesystem.data.dao.AppSettingsDao
import com.valsi.invoicesystem.data.dao.CustomerDao
import com.valsi.invoicesystem.data.dao.InvoiceDao
import com.valsi.invoicesystem.data.dao.InvoiceItemDao
import com.valsi.invoicesystem.data.entity.AppSettings
import com.valsi.invoicesystem.data.entity.Invoice
import com.valsi.invoicesystem.data.entity.InvoiceDetail
import com.valsi.invoicesystem.data.entity.InvoiceItem
import com.valsi.invoicesystem.data.entity.InvoiceStatus
import com.valsi.invoicesystem.data.entity.InvoiceWithCustomer
import com.valsi.invoicesystem.data.entity.InvoiceWithItems
import com.valsi.invoicesystem.data.entity.PaymentStatus
import com.valsi.invoicesystem.data.model.NewInvoiceRequest
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InvoiceRepository @Inject constructor(
    private val db: ValsiDatabase,
    private val invoiceDao: InvoiceDao,
    private val invoiceItemDao: InvoiceItemDao,
    private val appSettingsDao: AppSettingsDao,
    private val customerDao: CustomerDao,
) {
    // ---- Reads ----

    fun observeAll(): Flow<List<InvoiceWithCustomer>> = invoiceDao.observeAllWithCustomer()

    fun observeFiltered(
        query: String,
        customerId: Long?,
        paymentStatus: PaymentStatus?,
        startDate: Long?,
        endDate: Long?,
    ): Flow<List<InvoiceWithCustomer>> =
        invoiceDao.observeFiltered(query.trim(), customerId, paymentStatus?.name, startDate, endDate)

    fun observeDetail(id: Long): Flow<InvoiceDetail?> = invoiceDao.observeDetail(id)

    suspend fun getWithItems(id: Long): InvoiceWithItems? = invoiceDao.getWithItems(id)

    // ---- Dashboard ----

    fun observeCountBetween(start: Long, end: Long): Flow<Int> =
        invoiceDao.observeCountBetween(start, end)

    fun observeSalesBetween(start: Long, end: Long): Flow<Double> =
        invoiceDao.observeSalesBetween(start, end)

    fun observePendingCount(): Flow<Int> = invoiceDao.observePendingCount()

    // ---- Writes ----

    /**
     * Persists a new invoice and its items atomically. When [NewInvoiceRequest.status] is
     * FINALIZED, a sequential invoice number is reserved from settings inside the same
     * transaction, guaranteeing no two invoices ever share a number even under concurrency.
     * Returns the new invoice id. If [replaceDraftId] refers to an existing DRAFT invoice, that
     * draft is deleted in the same transaction (used when finishing or re-saving a draft).
     */
    suspend fun createInvoice(request: NewInvoiceRequest, replaceDraftId: Long? = null): Long =
        db.withTransaction {
        // Remove the draft being resumed so it isn't duplicated.
        if (replaceDraftId != null) {
            invoiceDao.getById(replaceDraftId)?.let { old ->
                if (old.status == InvoiceStatus.DRAFT) invoiceDao.delete(old)
            }
        }
        val subtotal = request.lines.sumOf { it.lineTotal }
        val grandTotal = subtotal - request.discountAmount + request.vatAmount
        val amountPaid = normalizeAmountPaid(request.paymentStatus, request.amountPaid, grandTotal)

        val invoiceNumber = if (request.status == InvoiceStatus.FINALIZED) {
            val reserved = appSettingsDao.reserveNextInvoiceNumber()
            val settings = appSettingsDao.getOnce() ?: AppSettings()
            formatInvoiceNumber(settings.invoicePrefix, reserved)
        } else {
            // Drafts don't consume a real number; keep it unique so the index isn't violated.
            "DRAFT-${System.currentTimeMillis()}"
        }

        val invoiceId = invoiceDao.insert(
            Invoice(
                invoiceNumber = invoiceNumber,
                customerId = request.customerId,
                status = request.status,
                paymentStatus = request.paymentStatus,
                subtotal = subtotal,
                discountAmount = request.discountAmount,
                vatAmount = request.vatAmount,
                grandTotal = grandTotal,
                amountPaid = amountPaid,
                notes = request.notes,
            )
        )

        invoiceItemDao.insertAll(
            request.lines.map { line ->
                InvoiceItem(
                    invoiceId = invoiceId,
                    productId = line.productId,
                    productNameSnapshot = line.productName,
                    quantity = line.quantity,
                    unitPriceSnapshot = line.unitPrice,
                    lineTotal = line.lineTotal,
                )
            }
        )

        recalcOutstanding(request.customerId)
        invoiceId
    }

    /** Updates only the payment status/amount of a finalized invoice and refreshes the balance. */
    suspend fun updatePaymentStatus(
        invoiceId: Long,
        paymentStatus: PaymentStatus,
        amountPaidInput: Double,
    ) = db.withTransaction {
        val invoice = invoiceDao.getById(invoiceId) ?: return@withTransaction
        val amountPaid = normalizeAmountPaid(paymentStatus, amountPaidInput, invoice.grandTotal)
        invoiceDao.updatePayment(invoiceId, paymentStatus, amountPaid)
        recalcOutstanding(invoice.customerId)
    }

    /** Voids a finalized invoice (kept for numbering integrity) and refreshes the balance. */
    suspend fun voidInvoice(invoiceId: Long) = db.withTransaction {
        val invoice = invoiceDao.getById(invoiceId) ?: return@withTransaction
        invoiceDao.updateStatus(invoiceId, InvoiceStatus.VOID)
        recalcOutstanding(invoice.customerId)
    }

    /**
     * Permanently deletes an invoice (its items cascade) and refreshes the customer's balance.
     * Used for drafts, and for finalized invoices the rep confirms deleting outright (as opposed
     * to [voidInvoice], which keeps the record and invoice number).
     */
    suspend fun deleteInvoice(invoice: Invoice) = db.withTransaction {
        invoiceDao.delete(invoice)
        recalcOutstanding(invoice.customerId)
    }

    private suspend fun recalcOutstanding(customerId: Long) {
        val balance = invoiceDao.outstandingForCustomer(customerId)
        customerDao.updateOutstandingBalance(customerId, balance)
    }

    companion object {
        fun formatInvoiceNumber(prefix: String, number: Int): String =
            "$prefix%04d".format(number)

        fun normalizeAmountPaid(
            status: PaymentStatus,
            input: Double,
            grandTotal: Double,
        ): Double = when (status) {
            PaymentStatus.PAID -> grandTotal
            PaymentStatus.UNPAID -> 0.0
            PaymentStatus.PARTIAL -> input.coerceIn(0.0, grandTotal)
        }
    }
}
