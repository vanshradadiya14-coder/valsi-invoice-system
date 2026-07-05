package com.valsi.invoicesystem.data.entity

/**
 * Lifecycle of an invoice.
 * - [DRAFT]      – editable, deletable, does not consume an invoice number until finalized.
 * - [FINALIZED]  – issued financial document; immutable except for payment status.
 * - [VOID]       – cancelled after issue; kept to preserve invoice-number integrity (never deleted).
 */
enum class InvoiceStatus {
    DRAFT,
    FINALIZED,
    VOID,
}

/** Payment state of an invoice. */
enum class PaymentStatus {
    PAID,
    UNPAID,
    PARTIAL,
}
