package com.valsi.invoicesystem.data.repository

import com.valsi.invoicesystem.data.dao.ProductDao
import com.valsi.invoicesystem.data.entity.Product
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepository @Inject constructor(
    private val productDao: ProductDao,
) {
    fun observeAll(): Flow<List<Product>> = productDao.observeAll()

    fun observeActive(): Flow<List<Product>> = productDao.observeActive()

    fun search(query: String): Flow<List<Product>> =
        if (query.isBlank()) observeAll() else productDao.search(query.trim())

    suspend fun getById(id: Long): Product? = productDao.getById(id)

    suspend fun updatePrice(productId: Long, price: Double) = productDao.updatePrice(productId, price)

    suspend fun setActive(productId: Long, active: Boolean) = productDao.updateActive(productId, active)
}
