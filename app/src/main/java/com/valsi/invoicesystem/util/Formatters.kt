package com.valsi.invoicesystem.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Money formatting that honours the configurable currency symbol. */
object Money {
    fun format(amount: Double, symbol: String = "£"): String =
        symbol + "%,.2f".format(amount)
}

object DateUtils {
    private val dateFmt = SimpleDateFormat("dd MMM yyyy", Locale.UK)
    private val dateTimeFmt = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.UK)

    fun formatDate(millis: Long): String = dateFmt.format(Date(millis))

    fun formatDateTime(millis: Long): String = dateTimeFmt.format(Date(millis))

    /** Midnight at the start of the day containing [now]. */
    fun startOfDay(now: Long = System.currentTimeMillis()): Long = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    /** Last millisecond of the day containing [now]. */
    fun endOfDay(now: Long = System.currentTimeMillis()): Long = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis
}
