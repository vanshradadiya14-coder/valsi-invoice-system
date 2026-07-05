package com.valsi.invoicesystem.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "invoice_items",
    foreignKeys = [
        ForeignKey(
            entity = Invoice::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("invoiceId"), Index("productId")],
)
data class InvoiceItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long,
    val productId: Long,
    /** Product name captured at time of sale so history stays accurate if the product changes. */
    val productNameSnapshot: String,
    val quantity: Int,
    /**
     * CRITICAL: the price at the moment this line was created. Displaying old invoices must
     * never reference [Product.currentPrice].
     */
    val unitPriceSnapshot: Double,
    val lineTotal: Double,
)
