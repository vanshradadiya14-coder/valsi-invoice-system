package com.valsi.invoicesystem.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.valsi.invoicesystem.data.dao.AppSettingsDao
import com.valsi.invoicesystem.data.dao.CustomerDao
import com.valsi.invoicesystem.data.dao.InvoiceDao
import com.valsi.invoicesystem.data.dao.InvoiceItemDao
import com.valsi.invoicesystem.data.dao.ProductDao
import com.valsi.invoicesystem.data.entity.AppSettings
import com.valsi.invoicesystem.data.entity.Customer
import com.valsi.invoicesystem.data.entity.Invoice
import com.valsi.invoicesystem.data.entity.InvoiceItem
import com.valsi.invoicesystem.data.entity.Product

@Database(
    entities = [
        Customer::class,
        Product::class,
        Invoice::class,
        InvoiceItem::class,
        AppSettings::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class ValsiDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun productDao(): ProductDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun invoiceItemDao(): InvoiceItemDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        const val DATABASE_NAME = "valsi.db"
    }
}
