package com.valsi.invoicesystem.data

import androidx.room.TypeConverter
import com.valsi.invoicesystem.data.entity.InvoiceStatus
import com.valsi.invoicesystem.data.entity.PaymentStatus

/** Room stores enums as their String name. */
class Converters {
    @TypeConverter
    fun fromInvoiceStatus(status: InvoiceStatus): String = status.name

    @TypeConverter
    fun toInvoiceStatus(value: String): InvoiceStatus = InvoiceStatus.valueOf(value)

    @TypeConverter
    fun fromPaymentStatus(status: PaymentStatus): String = status.name

    @TypeConverter
    fun toPaymentStatus(value: String): PaymentStatus = PaymentStatus.valueOf(value)
}
