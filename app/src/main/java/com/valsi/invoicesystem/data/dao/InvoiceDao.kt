package com.valsi.invoicesystem.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.valsi.invoicesystem.data.entity.Invoice
import com.valsi.invoicesystem.data.entity.InvoiceDetail
import com.valsi.invoicesystem.data.entity.InvoiceStatus
import com.valsi.invoicesystem.data.entity.InvoiceWithCustomer
import com.valsi.invoicesystem.data.entity.InvoiceWithItems
import com.valsi.invoicesystem.data.entity.PaymentStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(invoice: Invoice): Long

    @Update
    suspend fun update(invoice: Invoice)

    @Delete
    suspend fun delete(invoice: Invoice)

    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun getById(id: Long): Invoice?

    // ---- Lists ----

    @Query(
        """
        SELECT invoices.*, customers.storeName AS storeName, customers.ownerName AS ownerName
        FROM invoices
        INNER JOIN customers ON customers.id = invoices.customerId
        ORDER BY invoices.createdAt DESC
        """
    )
    fun observeAllWithCustomer(): Flow<List<InvoiceWithCustomer>>

    /**
     * Flexible history filter. Any of [customerId], [paymentStatus], [startDate], [endDate]
     * may be null to skip that filter; [query] "" skips text search.
     * [paymentStatus] is the enum's stored name (e.g. "PAID") or null.
     */
    @Query(
        """
        SELECT invoices.*, customers.storeName AS storeName, customers.ownerName AS ownerName
        FROM invoices
        INNER JOIN customers ON customers.id = invoices.customerId
        WHERE (:query = ''
               OR invoices.invoiceNumber LIKE '%' || :query || '%'
               OR customers.storeName LIKE '%' || :query || '%'
               OR customers.ownerName LIKE '%' || :query || '%')
          AND (:customerId IS NULL OR invoices.customerId = :customerId)
          AND (:paymentStatus IS NULL OR invoices.paymentStatus = :paymentStatus)
          AND (:startDate IS NULL OR invoices.createdAt >= :startDate)
          AND (:endDate IS NULL OR invoices.createdAt <= :endDate)
        ORDER BY invoices.createdAt DESC
        """
    )
    fun observeFiltered(
        query: String,
        customerId: Long?,
        paymentStatus: String?,
        startDate: Long?,
        endDate: Long?,
    ): Flow<List<InvoiceWithCustomer>>

    @Query(
        """
        SELECT invoices.*, customers.storeName AS storeName, customers.ownerName AS ownerName
        FROM invoices
        INNER JOIN customers ON customers.id = invoices.customerId
        WHERE invoices.customerId = :customerId
        ORDER BY invoices.createdAt DESC
        """
    )
    fun observeForCustomer(customerId: Long): Flow<List<InvoiceWithCustomer>>

    // ---- Detail (with relations) ----

    @Transaction
    @Query("SELECT * FROM invoices WHERE id = :id")
    fun observeDetail(id: Long): Flow<InvoiceDetail?>

    @Transaction
    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun getWithItems(id: Long): InvoiceWithItems?

    // ---- Dashboard aggregates ----

    @Query("SELECT COUNT(*) FROM invoices WHERE status = 'FINALIZED' AND createdAt BETWEEN :start AND :end")
    fun observeCountBetween(start: Long, end: Long): Flow<Int>

    @Query(
        "SELECT COALESCE(SUM(grandTotal), 0.0) FROM invoices WHERE status = 'FINALIZED' AND createdAt BETWEEN :start AND :end"
    )
    fun observeSalesBetween(start: Long, end: Long): Flow<Double>

    @Query(
        "SELECT COUNT(*) FROM invoices WHERE status = 'FINALIZED' AND paymentStatus IN ('UNPAID', 'PARTIAL')"
    )
    fun observePendingCount(): Flow<Int>

    // ---- Balance & payment ----

    @Query(
        "SELECT COALESCE(SUM(grandTotal - amountPaid), 0.0) FROM invoices WHERE customerId = :customerId AND status = 'FINALIZED'"
    )
    suspend fun outstandingForCustomer(customerId: Long): Double

    @Query("SELECT COUNT(*) FROM invoices WHERE customerId = :customerId")
    suspend fun countForCustomer(customerId: Long): Int

    @Query("UPDATE invoices SET paymentStatus = :paymentStatus, amountPaid = :amountPaid WHERE id = :id")
    suspend fun updatePayment(id: Long, paymentStatus: PaymentStatus, amountPaid: Double)

    @Query("UPDATE invoices SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: InvoiceStatus)
}
