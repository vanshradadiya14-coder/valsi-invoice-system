package com.valsi.invoicesystem.data.repository

import com.valsi.invoicesystem.data.dao.CustomerDao
import com.valsi.invoicesystem.data.dao.InvoiceDao
import com.valsi.invoicesystem.data.entity.Customer
import com.valsi.invoicesystem.data.entity.InvoiceWithCustomer
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomerRepository @Inject constructor(
    private val customerDao: CustomerDao,
    private val invoiceDao: InvoiceDao,
) {
    fun observeAll(): Flow<List<Customer>> = customerDao.observeAll()

    fun observeById(id: Long): Flow<Customer?> = customerDao.observeById(id)

    suspend fun getById(id: Long): Customer? = customerDao.getById(id)

    fun search(query: String): Flow<List<Customer>> =
        if (query.isBlank()) observeAll() else customerDao.search(query.trim())

    fun observeTotalOutstanding(): Flow<Double> = customerDao.observeTotalOutstanding()

    fun observeInvoices(customerId: Long): Flow<List<InvoiceWithCustomer>> =
        invoiceDao.observeForCustomer(customerId)

    suspend fun add(customer: Customer): Long = customerDao.insert(customer)

    suspend fun update(customer: Customer) = customerDao.update(customer)

    /**
     * Deletes a customer only if they have no invoices (finalized invoices must be preserved for
     * numbering integrity). Returns false without deleting if invoices exist.
     */
    suspend fun delete(customer: Customer): Boolean {
        if (invoiceDao.countForCustomer(customer.id) > 0) return false
        customerDao.delete(customer)
        return true
    }
}
