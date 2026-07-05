package com.valsi.invoicesystem.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.valsi.invoicesystem.data.SeedData
import com.valsi.invoicesystem.data.ValsiDatabase
import com.valsi.invoicesystem.data.dao.AppSettingsDao
import com.valsi.invoicesystem.data.dao.CustomerDao
import com.valsi.invoicesystem.data.dao.InvoiceDao
import com.valsi.invoicesystem.data.dao.InvoiceItemDao
import com.valsi.invoicesystem.data.dao.ProductDao
import com.valsi.invoicesystem.data.entity.AppSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        // Lazy self-reference so the seed callback can use the DAOs after the DB is built.
        databaseProvider: Provider<ValsiDatabase>,
    ): ValsiDatabase {
        return Room.databaseBuilder(context, ValsiDatabase::class.java, ValsiDatabase.DATABASE_NAME)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // Seed the fixed product catalogue and default settings on first launch.
                    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                        val database = databaseProvider.get()
                        database.productDao().insertAll(SeedData.products())
                        database.appSettingsDao().upsert(AppSettings())
                    }
                }
            })
            .build()
    }

    @Provides fun provideCustomerDao(db: ValsiDatabase): CustomerDao = db.customerDao()
    @Provides fun provideProductDao(db: ValsiDatabase): ProductDao = db.productDao()
    @Provides fun provideInvoiceDao(db: ValsiDatabase): InvoiceDao = db.invoiceDao()
    @Provides fun provideInvoiceItemDao(db: ValsiDatabase): InvoiceItemDao = db.invoiceItemDao()
    @Provides fun provideAppSettingsDao(db: ValsiDatabase): AppSettingsDao = db.appSettingsDao()
}
