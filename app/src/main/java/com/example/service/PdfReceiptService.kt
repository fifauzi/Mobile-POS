package com.example.service

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.model.OrderEntity
import com.example.data.local.model.OrderItemEntity
import com.example.ui.components.CurrencyUtils
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReceiptService {

    /**
     * Generates a clean, high-resolution physical-style PDF receipt using native Android PdfDocument
     */
    fun generateReceiptPdf(
        context: Context,
        order: OrderEntity,
        items: List<OrderItemEntity>,
        outletInfo: com.example.domain.model.OutletInfo = com.example.domain.model.OutletInfo(),
        receiptConfig: com.example.domain.model.ReceiptCustomizerConfig = com.example.domain.model.ReceiptCustomizerConfig()
    ): File? {
        return try {
            val pageWidth = 384 // 58mm / 80mm receipt width standard in points
            val baseHeight = 540
            val itemHeight = items.size * 32
            val pageHeight = baseHeight + itemHeight

            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            // Setup Paints
            val textPaint = Paint().apply {
                color = Color.rgb(20, 24, 33)
                textSize = 10f
                typeface = Typeface.MONOSPACE
                isAntiAlias = true
            }

            val titlePaint = Paint().apply {
                color = Color.rgb(15, 23, 42)
                textSize = 14f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }

            val subtitlePaint = Paint().apply {
                color = Color.rgb(100, 116, 139)
                textSize = 9f
                typeface = Typeface.MONOSPACE
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }

            val boldPaint = Paint().apply {
                color = Color.rgb(15, 23, 42)
                textSize = 11f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                isAntiAlias = true
            }

            val linePaint = Paint().apply {
                color = Color.rgb(203, 213, 225)
                strokeWidth = 1f
                style = Paint.Style.STROKE
            }

            var currentY = 36f
            val centerX = pageWidth / 2f
            val margin = 24f
            val rightMargin = pageWidth - margin

            // Dynamic Store Header
            canvas.drawText(outletInfo.name.uppercase(), centerX, currentY, titlePaint)
            currentY += 16f
            canvas.drawText("${outletInfo.address} (${outletInfo.branch})", centerX, currentY, subtitlePaint)
            currentY += 14f
            canvas.drawText("Telp: ${outletInfo.phone} • IG: ${outletInfo.instagram}", centerX, currentY, subtitlePaint)
            currentY += 18f

            // Dashed Divider
            canvas.drawLine(margin, currentY, rightMargin, currentY, linePaint)
            currentY += 16f

            // Order Metadata
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss 'WIB'", Locale("id", "ID"))
            drawRow(canvas, "No. Invoice", order.invoiceNumber, margin, rightMargin, currentY, textPaint)
            currentY += 14f
            val orderTypeLabel = if (order.orderType == "TAKEAWAY") "Bungkus (Takeaway)" else "Makan di Tempat (Dine-in)"
            drawRow(canvas, "Tipe Pesanan", orderTypeLabel, margin, rightMargin, currentY, boldPaint)
            currentY += 14f
            drawRow(canvas, "Waktu Transaksi", dateFormat.format(Date(order.timestamp)), margin, rightMargin, currentY, textPaint)
            currentY += 14f
            drawRow(canvas, "Pelanggan", order.customerName.ifEmpty { "Walk-in Guest" }, margin, rightMargin, currentY, textPaint)
            currentY += 14f
            drawRow(canvas, "Kasir / Shift", "${order.cashierName} (${order.shiftName})", margin, rightMargin, currentY, boldPaint)
            currentY += 14f
            drawRow(canvas, "Metode Pembayaran", order.paymentMethod, margin, rightMargin, currentY, textPaint)
            currentY += 18f

            canvas.drawLine(margin, currentY, rightMargin, currentY, linePaint)
            currentY += 16f

            // Line Items
            items.forEach { item ->
                val lineLabel = "${item.quantity}x ${item.productName}"
                val priceLabel = CurrencyUtils.formatIdr(item.lineTotal)
                drawRow(canvas, lineLabel, priceLabel, margin, rightMargin, currentY, boldPaint)
                currentY += 13f

                if (item.variantSummary.isNotEmpty()) {
                    val variantText = "  (${item.variantSummary})"
                    canvas.drawText(variantText, margin, currentY, subtitlePaint.apply { textAlign = Paint.Align.LEFT })
                    currentY += 12f
                }

                if (item.itemDiscount > 0) {
                    val discText = "  Diskon: -${CurrencyUtils.formatIdr(item.itemDiscount)}"
                    val redPaint = Paint(textPaint).apply { color = Color.rgb(225, 29, 72) }
                    canvas.drawText(discText, margin, currentY, redPaint)
                    currentY += 12f
                }
                currentY += 4f
            }

            canvas.drawLine(margin, currentY, rightMargin, currentY, linePaint)
            currentY += 16f

            // Subtotal & Calculations
            drawRow(canvas, "Subtotal", CurrencyUtils.formatIdr(order.subtotal), margin, rightMargin, currentY, textPaint)
            currentY += 14f

            val totalDisc = order.itemDiscountTotal + order.orderDiscountAmount
            if (totalDisc > 0) {
                drawRow(canvas, "Total Diskon", "-${CurrencyUtils.formatIdr(totalDisc)}", margin, rightMargin, currentY, textPaint)
                currentY += 14f
            }

            if (order.serviceFeeAmount > 0) {
                drawRow(canvas, "Biaya Layanan (${order.serviceFeePercentage.toInt()}%)", CurrencyUtils.formatIdr(order.serviceFeeAmount), margin, rightMargin, currentY, textPaint)
                currentY += 14f
            }

            if (order.taxAmount > 0) {
                drawRow(canvas, "Pajak PB1 (${order.taxPercentage.toInt()}%)", CurrencyUtils.formatIdr(order.taxAmount), margin, rightMargin, currentY, textPaint)
                currentY += 18f
            }

            canvas.drawLine(margin, currentY, rightMargin, currentY, linePaint)
            currentY += 18f

            // Grand Total
            val totalPaint = Paint().apply {
                color = Color.rgb(0, 82, 204)
                textSize = 13f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                isAntiAlias = true
            }
            drawRow(canvas, "TOTAL BAYAR", CurrencyUtils.formatIdr(order.grandTotal), margin, rightMargin, currentY, totalPaint)
            currentY += 18f

            if (order.paymentMethod == "CASH") {
                drawRow(canvas, "Uang Tunai", CurrencyUtils.formatIdr(order.tenderedAmount), margin, rightMargin, currentY, textPaint)
                currentY += 14f
                drawRow(canvas, "Kembalian", CurrencyUtils.formatIdr(order.changeAmount), margin, rightMargin, currentY, textPaint)
                currentY += 18f
            }

            canvas.drawLine(margin, currentY, rightMargin, currentY, linePaint)
            currentY += 22f

            // Footer
            subtitlePaint.textAlign = Paint.Align.CENTER
            canvas.drawText(receiptConfig.footerNote, centerX, currentY, subtitlePaint)
            currentY += 14f
            canvas.drawText("Struk ini merupakan bukti pembayaran yang sah", centerX, currentY, subtitlePaint)
            currentY += 14f
            canvas.drawText("Powered by Caffeinery Native POS", centerX, currentY, subtitlePaint)

            pdfDocument.finishPage(page)

            // Write to local cache
            val outputDir = File(context.cacheDir, "receipts")
            if (!outputDir.exists()) outputDir.mkdirs()
            val pdfFile = File(outputDir, "Struk_${order.invoiceNumber}.pdf")
            val outputStream = FileOutputStream(pdfFile)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()

            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun drawRow(
        canvas: Canvas,
        leftText: String,
        rightText: String,
        leftX: Float,
        rightX: Float,
        y: Float,
        paint: Paint
    ) {
        val origAlign = paint.textAlign
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText(leftText, leftX, y, paint)

        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(rightText, rightX, y, paint)
        paint.textAlign = origAlign
    }

    /**
     * Builds standard formatted Indonesian WhatsApp Receipt message text
     */
    fun buildWhatsAppReceiptMessage(
        order: OrderEntity,
        items: List<OrderItemEntity>,
        outletInfo: com.example.domain.model.OutletInfo = com.example.domain.model.OutletInfo(),
        receiptConfig: com.example.domain.model.ReceiptCustomizerConfig = com.example.domain.model.ReceiptCustomizerConfig()
    ): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss 'WIB'", Locale("id", "ID"))
        val dateStr = dateFormat.format(Date(order.timestamp))
        val sb = StringBuilder()

        val orderTypeStr = if (order.orderType == "TAKEAWAY") "Bungkus (Takeaway)" else "Makan di Tempat (Dine-in)"

        sb.appendLine("☕ *STRUK PEMBAYARAN - ${outletInfo.name.uppercase()}*")
        sb.appendLine("${outletInfo.address} (${outletInfo.branch})")
        sb.appendLine("Telp: ${outletInfo.phone}")
        sb.appendLine("━━━━━━━━━━━━━━━━━━")
        sb.appendLine("🧾 *No. Invoice:* ${order.invoiceNumber}")
        sb.appendLine("👨‍🍳 *Kasir:* ${order.cashierName} (${order.shiftName})")
        sb.appendLine("🍽️ *Tipe:* $orderTypeStr")
        sb.appendLine("📅 *Waktu:* $dateStr")
        sb.appendLine("👤 *Pelanggan:* ${order.customerName.ifEmpty { "Walk-in Guest" }}")
        sb.appendLine("💳 *Metode:* ${order.paymentMethod} (LUNAS)")
        sb.appendLine("━━━━━━━━━━━━━━━━━━")
        sb.appendLine("*Rincian Pesanan:*")

        items.forEach { itm ->
            sb.appendLine("• ${itm.quantity}x *${itm.productName}* = ${CurrencyUtils.formatIdr(itm.lineTotal)}")
            if (itm.variantSummary.isNotEmpty()) {
                sb.appendLine("   _(${itm.variantSummary})_")
            }
            if (itm.itemDiscount > 0) {
                sb.appendLine("   _Diskon: -${CurrencyUtils.formatIdr(itm.itemDiscount)}_")
            }
        }

        sb.appendLine("━━━━━━━━━━━━━━━━━━")
        sb.appendLine("Subtotal: ${CurrencyUtils.formatIdr(order.subtotal)}")
        val totalDisc = order.itemDiscountTotal + order.orderDiscountAmount
        if (totalDisc > 0) {
            sb.appendLine("Total Diskon: -${CurrencyUtils.formatIdr(totalDisc)}")
        }
        if (order.serviceFeeAmount > 0) {
            sb.appendLine("Biaya Layanan (${order.serviceFeePercentage.toInt()}%): ${CurrencyUtils.formatIdr(order.serviceFeeAmount)}")
        }
        if (order.taxAmount > 0) {
            sb.appendLine("Pajak PB1 (${order.taxPercentage.toInt()}%): ${CurrencyUtils.formatIdr(order.taxAmount)}")
        }
        sb.appendLine("*TOTAL BAYAR: ${CurrencyUtils.formatIdr(order.grandTotal)}*")

        if (order.paymentMethod == "CASH") {
            sb.appendLine("Tunai Diterima: ${CurrencyUtils.formatIdr(order.tenderedAmount)}")
            sb.appendLine("Kembalian: ${CurrencyUtils.formatIdr(order.changeAmount)}")
        }

        sb.appendLine("━━━━━━━━━━━━━━━━━━")
        sb.appendLine(receiptConfig.footerNote)
        sb.appendLine("_Struk resmi pembayaran digital Caffeinery POS by BDS Tech._")

        return sb.toString()
    }

    /**
     * Shares the receipt directly to WhatsApp using URL or Intent Chooser
     */
    fun shareReceiptToWhatsApp(
        context: Context,
        order: OrderEntity,
        items: List<OrderItemEntity>,
        customerPhone: String = "",
        outletInfo: com.example.domain.model.OutletInfo = com.example.domain.model.OutletInfo(),
        receiptConfig: com.example.domain.model.ReceiptCustomizerConfig = com.example.domain.model.ReceiptCustomizerConfig()
    ) {
        val message = buildWhatsAppReceiptMessage(order, items, outletInfo, receiptConfig)
        try {
            val cleanPhone = customerPhone.replace(Regex("[^0-9]"), "").let {
                if (it.startsWith("0")) "62" + it.substring(1) else it
            }

            val encodedMsg = URLEncoder.encode(message, "UTF-8")
            val url = if (cleanPhone.isNotEmpty()) {
                "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMsg"
            } else {
                "https://api.whatsapp.com/send?text=$encodedMsg"
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to standard share sheet
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(shareIntent, "Kirim Struk via WhatsApp"))
        }
    }

    /**
     * Shares the generated PDF file via Android Share Sheet / WhatsApp Attachment
     */
    fun sharePdfReceipt(
        context: Context,
        order: OrderEntity,
        items: List<OrderItemEntity>,
        outletInfo: com.example.domain.model.OutletInfo = com.example.domain.model.OutletInfo(),
        receiptConfig: com.example.domain.model.ReceiptCustomizerConfig = com.example.domain.model.ReceiptCustomizerConfig()
    ) {
        val pdfFile = generateReceiptPdf(context, order, items, outletInfo, receiptConfig)
        if (pdfFile == null || !pdfFile.exists()) {
            Toast.makeText(context, "Gagal membuat dokumen PDF", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, pdfFile)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Struk_${order.invoiceNumber}.pdf")
                putExtra(Intent.EXTRA_TEXT, buildWhatsAppReceiptMessage(order, items, outletInfo, receiptConfig))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            context.startActivity(Intent.createChooser(shareIntent, "Kirim Struk PDF"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error berbagi PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
