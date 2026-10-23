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
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val timeFormat = SimpleDateFormat("HH:mm:ss 'WIB'", Locale("id", "ID"))
        val sb = StringBuilder()

        // Comprehensive CSV Header with Exact Time, Order Type, Cashier Name, Shift, Profit/Loss & Variant Columns
        sb.append("Invoice,Date,Exact_Time_WIB,Order_Type,Cashier_Name,Shift,Customer,Payment_Method,Items_Summary,Variants_Detail,Subtotal,Item_Discount,Order_Discount,Service_Fee,Tax,Grand_Total,Total_COGS_HPP,Gross_Profit_Laba_Kotor,Net_Profit,Status\n")

        for (order in orders) {
            val dateStr = dateFormat.format(Date(order.timestamp))
            val timeStr = timeFormat.format(Date(order.timestamp))
            val orderTypeStr = if (order.orderType == "TAKEAWAY") "Takeaway (Bungkus)" else "Dine-in (Makan di Tempat)"
            val items = orderItemsMap[order.invoiceNumber] ?: emptyList()

            val itemsSummary = items.joinToString(" | ") {
                "${it.productName} (x${it.quantity})"
            }.replace("\"", "\"\"")

            val variantsDetail = items.joinToString(" | ") {
                "${it.productName}: ${if (it.variantSummary.isNotBlank()) it.variantSummary else "Standard"}"
            }.replace("\"", "\"\"")

            // Calculate HPP and Profit for this order
            val totalCogsHpp = items.sumOf { it.costPrice * it.quantity }
            val grossProfit = (order.subtotal - order.itemDiscountTotal - order.orderDiscountAmount) - totalCogsHpp
            val netProfit = grossProfit // net operating margin before corporate taxes

            sb.append("\"${order.invoiceNumber}\",")
            sb.append("\"$dateStr\",")
            sb.append("\"$timeStr\",")
            sb.append("\"$orderTypeStr\",")
            sb.append("\"${order.cashierName.replace("\"", "\"\"")}\",")
            sb.append("\"${order.shiftName.replace("\"", "\"\"")}\",")
            sb.append("\"${order.customerName.replace("\"", "\"\"")}\",")
            sb.append("\"${order.paymentMethod}\",")
            sb.append("\"$itemsSummary\",")
            sb.append("\"$variantsDetail\",")
            sb.append("${order.subtotal},")
            sb.append("${order.itemDiscountTotal},")
            sb.append("${order.orderDiscountAmount},")
            sb.append("${order.serviceFeeAmount},")
            sb.append("${order.taxAmount},")
            sb.append("${order.grandTotal},")
            sb.append("$totalCogsHpp,")
            sb.append("$grossProfit,")
            sb.append("$netProfit,")
            sb.append("\"${order.paymentStatus}\"\n")
        }

        return sb.toString()
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
