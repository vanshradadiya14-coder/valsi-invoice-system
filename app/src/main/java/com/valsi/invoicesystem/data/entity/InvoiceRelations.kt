package com.valsi.invoicesystem.data.entity

import androidx.room.Embedded
import androidx.room.Relation

/** Invoice plus its line items (used when rebuilding a PDF or viewing detail). */
data class InvoiceWithItems(
    @Embedded val invoice: Invoice,
    @Relation(parentColumn = "id", entityColumn = "invoiceId")
    val items: List<InvoiceItem>,
)

/** Full detail: invoice, its customer, and its line items. */
data class InvoiceDetail(
    @Embedded val invoice: Invoice,
    @Relation(parentColumn = "customerId", entityColumn = "id")
    val customer: Customer,
    @Relation(parentColumn = "id", entityColumn = "invoiceId")
    val items: List<InvoiceItem>,
)

/**
 * Lightweight row for invoice lists — the invoice plus just the customer's display names,
 * fetched via JOIN so the list doesn't need N extra queries.
 */
data class InvoiceWithCustomer(
    @Embedded val invoice: Invoice,
    val storeName: String,
    val ownerName: String,
)
