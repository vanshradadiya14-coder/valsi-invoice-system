package com.valsi.invoicesystem.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.valsi.invoicesystem.data.entity.InvoiceItem

@Dao
interface InvoiceItemDao {

    @Insert
    suspend fun insertAll(items: List<InvoiceItem>): List<Long>

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun getForInvoice(invoiceId: Long): List<InvoiceItem>
}
