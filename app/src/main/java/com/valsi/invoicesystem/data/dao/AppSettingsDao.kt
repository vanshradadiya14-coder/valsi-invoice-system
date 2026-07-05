package com.valsi.invoicesystem.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.valsi.invoicesystem.data.entity.AppSettings
import kotlinx.coroutines.flow.Flow

@Dao
abstract class AppSettingsDao {

    @Query("SELECT * FROM app_settings WHERE id = ${AppSettings.SINGLETON_ID}")
    abstract fun observe(): Flow<AppSettings?>

    @Query("SELECT * FROM app_settings WHERE id = ${AppSettings.SINGLETON_ID}")
    abstract suspend fun getOnce(): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsert(settings: AppSettings)

    @Query("SELECT nextInvoiceNumber FROM app_settings WHERE id = ${AppSettings.SINGLETON_ID}")
    protected abstract suspend fun peekNextNumber(): Int

    @Query("UPDATE app_settings SET nextInvoiceNumber = :value WHERE id = ${AppSettings.SINGLETON_ID}")
    protected abstract suspend fun setNextNumber(value: Int)

    /**
     * Atomically returns the current next invoice number and increments the stored counter.
     * Runs in its own transaction, and is also safe when nested inside a larger
     * finalize-invoice transaction (SQLite savepoints), so two concurrent callers can never
     * receive the same number.
     */
    @Transaction
    open suspend fun reserveNextInvoiceNumber(): Int {
        val current = peekNextNumber()
        setNextNumber(current + 1)
        return current
    }
}
