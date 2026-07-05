package com.valsi.invoicesystem.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.valsi.invoicesystem.data.entity.Product
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(products: List<Product>)

    @Update
    suspend fun update(product: Product)

    @Query("SELECT * FROM products ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY name COLLATE NOCASE ASC")
    fun observeActive(): Flow<List<Product>>

    @Query(
        "SELECT * FROM products WHERE name LIKE '%' || :query || '%' ORDER BY name COLLATE NOCASE ASC"
    )
    fun search(query: String): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: Long): Product?

    @Query("SELECT COUNT(*) FROM products")
    suspend fun count(): Int

    @Query("UPDATE products SET currentPrice = :price WHERE id = :productId")
    suspend fun updatePrice(productId: Long, price: Double)

    @Query("UPDATE products SET isActive = :active WHERE id = :productId")
    suspend fun updateActive(productId: Long, active: Boolean)
}
