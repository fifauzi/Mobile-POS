package com.example.ui.components

import com.example.data.local.model.OrderEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Utility for date parsing, formatting, and calendar calculations
 * strictly based on Indonesian Western Time (WIB / Asia/Jakarta).
 */
object DateTimeUtils {

    val WIB_ZONE: TimeZone = TimeZone.getTimeZone("Asia/Jakarta")
    val INDONESIA_LOCALE: Locale = Locale("id", "ID")

    private val INVOICE_FULL_REGEX = Regex("""^INV-(\d{4})(\d{2})(\d{2})-(\d{2})(\d{2})(\d{2})""")
    private val INVOICE_DATE_REGEX = Regex("""^INV-(\d{4})(\d{2})(\d{2})""")

    /**
     * Checks if the given invoice number contains an encoded date pattern "INV-YYYYMMDD..."
     */
    fun hasDateEncodedInInvoice(invoiceNumber: String): Boolean {
        return INVOICE_DATE_REGEX.containsMatchIn(invoiceNumber)
    }

    /**
     * Extracts calendar date key ("yyyy-MM-dd") from invoice ID pattern (INV-YYYYMMDD-XXXX)
     * or falls back to formatting the given timestamp in WIB.
     */
    fun getOrderDateKey(invoiceNumber: String, timestamp: Long): String {
        val match = INVOICE_DATE_REGEX.find(invoiceNumber)
        if (match != null) {
            val (year, month, day) = match.destructured
            return "$year-$month-$day"
        }
        return formatDateKey(timestamp)
    }

    fun getOrderDateKey(order: OrderEntity): String {
        return getOrderDateKey(order.invoiceNumber, order.timestamp)
    }

    /**
     * Extracts the real/effective timestamp for an order.
     * Priority:
     * 1. Parse date and time from invoice string "INV-yyyyMMdd-HHmmss" in WIB.
     * 2. Parse date from invoice string "INV-yyyyMMdd..." with noon 12:00:00 in WIB.
     * 3. Fallback to order's immutable timestamp (if valid > 0).
     * 4. Fallback to current time.
     */
    fun getOrderEffectiveTimestamp(invoiceNumber: String, timestamp: Long): Long {
        val fullMatch = INVOICE_FULL_REGEX.find(invoiceNumber)
        if (fullMatch != null) {
            val (yearStr, monthStr, dayStr, hourStr, minStr, secStr) = fullMatch.destructured
            try {
                val cal = Calendar.getInstance(WIB_ZONE).apply {
                    set(Calendar.YEAR, yearStr.toInt())
                    set(Calendar.MONTH, monthStr.toInt() - 1)
                    set(Calendar.DAY_OF_MONTH, dayStr.toInt())
                    set(Calendar.HOUR_OF_DAY, hourStr.toInt())
                    set(Calendar.MINUTE, minStr.toInt())
                    set(Calendar.SECOND, secStr.toInt())
                    set(Calendar.MILLISECOND, 0)
                }
                return cal.timeInMillis
            } catch (_: Exception) {}
        }

        val dateMatch = INVOICE_DATE_REGEX.find(invoiceNumber)
        if (dateMatch != null) {
            val (yearStr, monthStr, dayStr) = dateMatch.destructured
            try {
                val cal = Calendar.getInstance(WIB_ZONE).apply {
                    set(Calendar.YEAR, yearStr.toInt())
                    set(Calendar.MONTH, monthStr.toInt() - 1)
                    set(Calendar.DAY_OF_MONTH, dayStr.toInt())
                    set(Calendar.HOUR_OF_DAY, 12)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                return cal.timeInMillis
            } catch (_: Exception) {}
        }

        return if (timestamp > 0) timestamp else System.currentTimeMillis()
    }

    fun getOrderEffectiveTimestamp(order: OrderEntity): Long {
        return getOrderEffectiveTimestamp(order.invoiceNumber, order.timestamp)
    }

    /**
     * Formats timestamp into full day header: e.g. "Kamis, 29 Okt 2026"
     */
    fun formatDateHeader(timestamp: Long): String {
        val sdf = SimpleDateFormat("EEEE, dd MMM yyyy", INDONESIA_LOCALE).apply {
            timeZone = WIB_ZONE
        }
        return sdf.format(Date(timestamp))
    }

    /**
     * Formats timestamp into short day: e.g. "29 Okt 2026"
     */
    fun formatShortDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", INDONESIA_LOCALE).apply {
            timeZone = WIB_ZONE
        }
        return sdf.format(Date(timestamp))
    }

    /**
     * Formats timestamp into date key for sorting and grouping: "yyyy-MM-dd"
     */
    fun formatDateKey(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = WIB_ZONE
        }
        return sdf.format(Date(timestamp))
    }

    /**
     * Formats time string: "14:30:15 WIB"
     */
    fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm:ss 'WIB'", INDONESIA_LOCALE).apply {
            timeZone = WIB_ZONE
        }
        return sdf.format(Date(timestamp))
    }

    /**
     * Returns true if the given timestamp is on today's calendar date in WIB
     */
    fun isTodayWib(timestamp: Long, now: Long = System.currentTimeMillis()): Boolean {
        return formatDateKey(timestamp) == formatDateKey(now)
    }

    /**
     * Returns start of day (00:00:00.000) in WIB for the specified timestamp
     */
    fun getStartOfDayWib(timestamp: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance(WIB_ZONE).apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    /**
     * Returns end of day (23:59:59.999) in WIB for the specified timestamp
     */
    fun getEndOfDayWib(timestamp: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance(WIB_ZONE).apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }

    /**
     * Returns start of day N days ago in WIB
     */
    fun getStartOfDayDaysAgoWib(daysAgo: Int, now: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance(WIB_ZONE).apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, -daysAgo)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
