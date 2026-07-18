package com.valsi.invoicesystem.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "invoices",
    foreignKeys = [
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("customerId"),
        Index(value = ["invoiceNumber"], unique = true),
        Index("createdAt"),
    ],
)
data class Invoice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Human-facing sequential number, e.g. "VLS-0001". Unique. */
    val invoiceNumber: String,
    val customerId: Long,
    val status: InvoiceStatus = InvoiceStatus.FINALIZED,
    val paymentStatus: PaymentStatus = PaymentStatus.UNPAID,
    val subtotal: Double,
    val discountAmount: Double,
    val vatAmount: Double,
    val grandTotal: Double,
    val amountPaid: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val notes: String? = null,
) {
    /** User-facing number: real invoices show their number; drafts just show "Draft". */
    val displayNumber: String
        get() = if (status == InvoiceStatus.DRAFT) "Draft" else invoiceNumber
}
