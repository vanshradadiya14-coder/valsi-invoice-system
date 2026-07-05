package com.valsi.invoicesystem.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "customers",
    indices = [Index("storeName"), Index("phoneNumber")],
)
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val storeName: String,
    val ownerName: String,
    val phoneNumber: String,
    val email: String? = null,
    val address: String = "",
    val paymentTerms: String = "Net 7",
    val notes: String? = null,
    /** Cached running balance = sum of (grandTotal - amountPaid) over their non-void invoices. */
    val outstandingBalance: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
)
