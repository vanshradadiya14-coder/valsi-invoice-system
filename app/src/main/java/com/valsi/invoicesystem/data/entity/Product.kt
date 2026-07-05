package com.valsi.invoicesystem.data.entity

import androidx.annotation.DrawableRes
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A sellable product. The product LIST is fixed (seeded on first launch) but [currentPrice]
 * is freely editable over time. Existing invoices never read [currentPrice] — they keep their
 * own price snapshot on [InvoiceItem].
 */
@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Local drawable reference, since the product catalogue is fixed. */
    @DrawableRes val imageResId: Int,
    val currentPrice: Double = 0.0,
    val unit: String = "pack",
    /** Allows hiding a product from invoice creation without deleting its history. */
    val isActive: Boolean = true,
)
