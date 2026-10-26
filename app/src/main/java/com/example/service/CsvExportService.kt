package com.example.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.model.OrderEntity
import com.example.data.local.model.OrderItemEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExportService {

    fun generateSalesCsv(
        orders: List<OrderEntity>,
        orderItemsMap: Map<String, List<OrderItemEntity>>
    ): String {
        val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val sb = StringBuilder()

        // Dedicated 21-Column CSV Architecture with individual columns for each variant attribute
        sb.append("Transaction ID,Date & Time,Employee / Cashier,Shift,Order Type,Category,Product Name,Base Price,Variant - Size,Variant - Type,Variant - Beans Type,Variant - Grind Size,Variant - Sugar Level,Variant - Add-ons,Variant Extra Price Total,Quantity,Subtotal,Discount,Tax / Service Fee,Grand Total,Payment Method\n")

        for (order in orders) {
            val dateTimeStr = dateTimeFormat.format(Date(order.timestamp))
            val cashier = order.cashierName.ifBlank { "Rian Pratama" }
            val shift = com.example.data.local.SettingsStorageService.normalizeShift(order.shiftName)
            val orderType = if (order.orderType == "TAKEAWAY") "Takeaway" else "Dine-in"
            val taxServiceFee = order.taxAmount + order.serviceFeeAmount

            val items = orderItemsMap[order.invoiceNumber] ?: emptyList()
            if (items.isEmpty()) {
                val row = listOf(
                    escapeCsv(order.invoiceNumber),
                    escapeCsv(dateTimeStr),
                    escapeCsv(cashier),
                    escapeCsv(shift),
                    escapeCsv(orderType),
                    escapeCsv("Coffee"),
                    escapeCsv("-"),
                    "0.0",
                    "-",
                    "-",
                    "-",
                    "-",
                    "-",
                    "-",
                    "0.0",
                    "0",
                    order.subtotal.toString(),
                    (order.itemDiscountTotal + order.orderDiscountAmount).toString(),
                    taxServiceFee.toString(),
                    order.grandTotal.toString(),
                    escapeCsv(order.paymentMethod)
                )
                sb.append(row.joinToString(",")).append("\n")
            } else {
                for (item in items) {
                    val cat = item.categoryName.ifBlank { "Coffee" }
                    val vSize = item.variantSize.ifBlank { "-" }
                    val vType = item.variantType.ifBlank { "-" }
                    val vBeans = item.variantBeans.ifBlank { "-" }
                    val vGrind = item.variantGrind.ifBlank { "-" }
                    val vSugar = item.variantSugar.ifBlank { "-" }
                    val vAddons = item.variantAddons.ifBlank { "-" }

                    val row = listOf(
                        escapeCsv(order.invoiceNumber),
                        escapeCsv(dateTimeStr),
                        escapeCsv(cashier),
                        escapeCsv(shift),
                        escapeCsv(orderType),
                        escapeCsv(cat),
                        escapeCsv(item.productName),
                        item.basePrice.toString(),
                        escapeCsv(vSize),
                        escapeCsv(vType),
                        escapeCsv(vBeans),
                        escapeCsv(vGrind),
                        escapeCsv(vSugar),
                        escapeCsv(vAddons),
                        item.variantExtraTotal.toString(),
                        item.quantity.toString(),
                        item.lineTotal.toString(),
                        item.itemDiscount.toString(),
                        taxServiceFee.toString(),
                        order.grandTotal.toString(),
                        escapeCsv(order.paymentMethod)
                    )
                    sb.append(row.joinToString(",")).append("\n")
                }
            }
        }

        return sb.toString()
    }

    private fun escapeCsv(value: Any?): String {
        val s = value?.toString() ?: ""
        return if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            "\"${s.replace("\"", "\"\"")}\""
        } else {
            s
        }
    }

    /**
     * Native File Saver & Share Sheet:
     * Writes CSV file to local cache and shares via Android FileProvider,
     * allowing instant open in Google Sheets, Microsoft Excel, Drive, or WhatsApp.
     */
    fun shareCsvReport(
        context: Context,
        csvContent: String,
        reportTitle: String = "Laporan_Penjualan_Caffeinery"
    ) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "${reportTitle}_$timestamp.csv"
            val file = File(context.cacheDir, fileName)

            FileOutputStream(file).use { fos ->
                fos.write(csvContent.toByteArray(Charsets.UTF_8))
            }

            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, file)

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "$fileName - Ekspor Laporan POS")
                putExtra(Intent.EXTRA_TEXT, "Terlampir file laporan transaksi dan analisa laba rugi POS.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            val chooser = Intent.createChooser(sendIntent, "Buka atau Bagikan Laporan CSV")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal membuat file CSV: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Dispatch sales CSV report directly to the store owner's email address.
     */
    fun sendCsvReportViaEmail(
        context: Context,
        csvContent: String,
        targetEmail: String = "bdscoffeecompany@gmail.com",
        outletName: String = "BDS Coffee Company",
        summaryStats: String = ""
    ) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val dateLabel = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date())
            val fileName = "Laporan_Penjualan_${timestamp}.csv"
            val file = File(context.cacheDir, fileName)

            FileOutputStream(file).use { fos ->
                fos.write(csvContent.toByteArray(Charsets.UTF_8))
            }

            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, file)

            val subject = "[Laporan Penjualan] $outletName - $dateLabel"
            val body = buildString {
                append("Halo Tim Manajemen / Store Owner,\n\n")
                append("Berikut adalah ringkasan dan file spreadsheet laporan transaksi penjualan dari $outletName:\n\n")
                if (summaryStats.isNotBlank()) {
                    append(summaryStats)
                    append("\n\n")
                }
                append("File spreadsheet CSV terlampir dapat langsung dibuka menggunakan Google Sheets, Microsoft Excel, atau aplikasi spreadsheet lainnya.\n\n")
                append("Dikirim otomatis oleh Caffeinery POS by BDS Tech.")
            }

            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(targetEmail))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(emailIntent, "Kirim Laporan ke Email ($targetEmail)")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal mengirim email: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
