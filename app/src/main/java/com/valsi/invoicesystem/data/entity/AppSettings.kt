package com.valsi.invoicesystem.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Single-row settings table. [id] is pinned to [SINGLETON_ID] so there is only ever one row.
 */
@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val companyName: String = "Valsi Foods",
    val companyLogoUri: String? = null,
    val companyAddress: String = "",
    val companyPhone: String = "",
    val companyEmail: String = "",
    val vatNumber: String = "",
    val currencySymbol: String = "£",
    val defaultVatPercent: Double = 20.0,
    val invoicePrefix: String = "VLS-",
    val nextInvoiceNumber: Int = 1,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
