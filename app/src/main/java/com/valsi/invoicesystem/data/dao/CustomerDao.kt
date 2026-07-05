package com.valsi.invoicesystem.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.valsi.invoicesystem.data.entity.Customer
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(customer: Customer): Long

    @Update
    suspend fun update(customer: Customer)

    @Delete
    suspend fun delete(customer: Customer)

    @Query("SELECT * FROM customers ORDER BY storeName COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id")
    fun observeById(id: Long): Flow<Customer?>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getById(id: Long): Customer?

    /** Search across store name, owner name, and phone number. */
    @Query(
        """
        SELECT * FROM customers
        WHERE storeName LIKE '%' || :query || '%'
           OR ownerName LIKE '%' || :query || '%'
           OR phoneNumber LIKE '%' || :query || '%'
        ORDER BY storeName COLLATE NOCASE ASC
        """
    )
    fun search(query: String): Flow<List<Customer>>

    @Query("SELECT COALESCE(SUM(outstandingBalance), 0.0) FROM customers")
    fun observeTotalOutstanding(): Flow<Double>

    @Query("UPDATE customers SET outstandingBalance = :balance WHERE id = :customerId")
    suspend fun updateOutstandingBalance(customerId: Long, balance: Double)
}
