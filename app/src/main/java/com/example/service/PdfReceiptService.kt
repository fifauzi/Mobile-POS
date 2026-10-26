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
import com.example.domain.model.OutletInfo
import com.example.domain.model.ReceiptCustomizerConfig
import com.example.ui.components.CurrencyUtils
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReceiptService {

    /**
     * Generates a clean, physical-style PDF receipt with dynamic page height and
     * multi-line text wrapping so no element ever overflows, clips, or collides.
     */
    fun generateReceiptPdf(
        context: Context,
        order: OrderEntity,
        items: List<OrderItemEntity>,
        outletInfo: OutletInfo = OutletInfo(),
        receiptConfig: ReceiptCustomizerConfig = ReceiptCustomizerConfig()
    ): File? {
        return try {
            val is80mm = receiptConfig.paperSize == "80mm"
            val pageWidth = if (is80mm) 560 else 384
            val margin = 20f

            // Pass 1: Measure content height dynamically
            val measuredContentHeight = renderReceipt(
                canvas = null,
                order = order,
                items = items,
                outletInfo = outletInfo,
                receiptConfig = receiptConfig,
                pageWidth = pageWidth,
                margin = margin
            )

            // Add bottom safe margin so nothing cuts off at the bottom
            val pageHeight = (measuredContentHeight + 40f).toInt().coerceAtLeast(300)

            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)

            // Pass 2: Draw the receipt content
            renderReceipt(
                canvas = page.canvas,
                order = order,
                items = items,
                outletInfo = outletInfo,
                receiptConfig = receiptConfig,
                pageWidth = pageWidth,
                margin = margin
            )

            pdfDocument.finishPage(page)

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

    private fun renderReceipt(
        canvas: Canvas?,
        order: OrderEntity,
        items: List<OrderItemEntity>,
        outletInfo: OutletInfo,
        receiptConfig: ReceiptCustomizerConfig,
        pageWidth: Int,
        margin: Float
    ): Float {
        val rightMargin = pageWidth - margin
        val contentWidth = rightMargin - margin
        val centerX = pageWidth / 2f

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

        val textPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 10f
            typeface = Typeface.MONOSPACE
            isAntiAlias = true
        }

        val boldPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 10.5f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = true
        }

        val totalPaint = Paint().apply {
            color = Color.rgb(0, 82, 204)
            textSize = 13f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = true
        }

        val redPaint = Paint().apply {
            color = Color.rgb(225, 29, 72)
            textSize = 9.5f
            typeface = Typeface.MONOSPACE
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }

        var currentY = 32f

        // 1. Store Header (Centered & Wrapped)
        currentY = drawCenteredWrapped(canvas, outletInfo.name.uppercase(), centerX, contentWidth, currentY, titlePaint, 18f)
        currentY += 4f
        val addressLine = "${outletInfo.address} (${outletInfo.branch})"
        currentY = drawCenteredWrapped(canvas, addressLine, centerX, contentWidth, currentY, subtitlePaint, 13f)
        currentY += 2f
        val contactLine = "Telp: ${outletInfo.phone} • IG: ${outletInfo.instagram}"
        currentY = drawCenteredWrapped(canvas, contactLine, centerX, contentWidth, currentY, subtitlePaint, 13f)
        currentY += 8f

        // Divider
        canvas?.drawLine(margin, currentY, rightMargin, currentY, linePaint)
        currentY += 14f

        // 2. Metadata Rows
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss 'WIB'", Locale("id", "ID"))
        val dateStr = dateFormat.format(Date(order.timestamp))
        val orderTypeLabel = if (order.orderType == "TAKEAWAY") "Takeaway (Bungkus)" else "Dine-in (Makan di Tempat)"
        val cashierName = order.cashierName.ifBlank { "Rian Pratama" }

        currentY = drawTwoColumnRow(canvas, "No. Invoice", order.invoiceNumber, margin, rightMargin, currentY, textPaint, textPaint)
        currentY = drawTwoColumnRow(canvas, "Tipe Pesanan", orderTypeLabel, margin, rightMargin, currentY, textPaint, boldPaint)
        currentY = drawTwoColumnRow(canvas, "Waktu Transaksi", dateStr, margin, rightMargin, currentY, textPaint, textPaint)
        currentY = drawTwoColumnRow(canvas, "Pelanggan", order.customerName.ifEmpty { "Walk-in Guest" }, margin, rightMargin, currentY, textPaint, textPaint)
        currentY = drawTwoColumnRow(canvas, "Kasir", cashierName, margin, rightMargin, currentY, textPaint, boldPaint)
        currentY = drawTwoColumnRow(canvas, "Metode Bayar", order.paymentMethod, margin, rightMargin, currentY, textPaint, textPaint)
        currentY += 4f

        // Divider
        canvas?.drawLine(margin, currentY, rightMargin, currentY, linePaint)
        currentY += 14f

        // 3. Itemized Products with Variants and Discounts
        for (item in items) {
            val itemLabel = "${item.quantity}x ${item.productName}"
            val priceLabel = CurrencyUtils.formatIdr(item.lineTotal)
            currentY = drawTwoColumnRow(canvas, itemLabel, priceLabel, margin, rightMargin, currentY, boldPaint, boldPaint)

            if (item.variantSummary.isNotBlank() && item.variantSummary != "Standard") {
                val variantPaint = Paint(subtitlePaint).apply { textAlign = Paint.Align.LEFT }
                currentY = drawIndentedWrapped(canvas, "↳ ${item.variantSummary}", margin + 8f, contentWidth - 12f, currentY, variantPaint, 12f)
            }

            if (item.itemDiscount > 0) {
                currentY = drawIndentedWrapped(canvas, "↳ Diskon: -${CurrencyUtils.formatIdr(item.itemDiscount)}", margin + 8f, contentWidth - 12f, currentY, redPaint, 12f)
            }
            currentY += 3f
        }

        // Divider
        canvas?.drawLine(margin, currentY, rightMargin, currentY, linePaint)
        currentY += 14f

        // 4. Financial Calculations
        currentY = drawTwoColumnRow(canvas, "Subtotal", CurrencyUtils.formatIdr(order.subtotal), margin, rightMargin, currentY, textPaint, textPaint)

        val totalDisc = order.itemDiscountTotal + order.orderDiscountAmount
        if (totalDisc > 0) {
            currentY = drawTwoColumnRow(canvas, "Total Diskon", "-${CurrencyUtils.formatIdr(totalDisc)}", margin, rightMargin, currentY, redPaint, redPaint)
        }

        if (order.serviceFeeAmount > 0) {
            currentY = drawTwoColumnRow(canvas, "Biaya Layanan (${order.serviceFeePercentage.toInt()}%)", CurrencyUtils.formatIdr(order.serviceFeeAmount), margin, rightMargin, currentY, textPaint, textPaint)
        }

        if (receiptConfig.showTax && order.taxAmount > 0) {
            currentY = drawTwoColumnRow(canvas, "Pajak PB1 (${order.taxPercentage.toInt()}%)", CurrencyUtils.formatIdr(order.taxAmount), margin, rightMargin, currentY, textPaint, textPaint)
        }

        currentY += 4f
        // Divider
        canvas?.drawLine(margin, currentY, rightMargin, currentY, linePaint)
        currentY += 16f

        // Grand Total
        currentY = drawTwoColumnRow(canvas, "TOTAL BAYAR", CurrencyUtils.formatIdr(order.grandTotal), margin, rightMargin, currentY, totalPaint, totalPaint, 16f)
        currentY += 4f

        if (order.paymentMethod == "CASH") {
            currentY = drawTwoColumnRow(canvas, "Uang Tunai", CurrencyUtils.formatIdr(order.tenderedAmount), margin, rightMargin, currentY, textPaint, textPaint)
            currentY = drawTwoColumnRow(canvas, "Kembalian", CurrencyUtils.formatIdr(order.changeAmount), margin, rightMargin, currentY, textPaint, textPaint)
            currentY += 4f
        }

        // Divider
        canvas?.drawLine(margin, currentY, rightMargin, currentY, linePaint)
        currentY += 16f

        // 5. Footer Notes (Wrapped & Centered)
        currentY = drawCenteredWrapped(canvas, receiptConfig.footerNote, centerX, contentWidth, currentY, subtitlePaint, 13f)
        currentY += 3f
        currentY = drawCenteredWrapped(canvas, "Struk ini merupakan bukti pembayaran yang sah", centerX, contentWidth, currentY, subtitlePaint, 13f)
        currentY += 3f
        currentY = drawCenteredWrapped(canvas, "Powered by Caffeinery POS by BDS Tech", centerX, contentWidth, currentY, subtitlePaint, 13f)

        return currentY
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return emptyList()
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            val candidate = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(candidate) <= maxWidth) {
                currentLine = candidate
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                }
                if (paint.measureText(word) > maxWidth) {
                    var sub = ""
                    for (ch in word) {
                        if (paint.measureText(sub + ch) <= maxWidth) {
                            sub += ch
                        } else {
                            if (sub.isNotEmpty()) lines.add(sub)
                            sub = ch.toString()
                        }
                    }
                    currentLine = sub
                } else {
                    currentLine = word
                }
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }
        return lines
    }

    private fun drawCenteredWrapped(
        canvas: Canvas?,
        text: String,
        centerX: Float,
        maxWidth: Float,
        startY: Float,
        paint: Paint,
        lineSpacing: Float
    ): Float {
        var y = startY
        val rawLines = text.split("\n")
        for (raw in rawLines) {
            val lines = wrapText(raw, paint, maxWidth)
            for (line in lines) {
                canvas?.let {
                    val origAlign = paint.textAlign
                    paint.textAlign = Paint.Align.CENTER
                    it.drawText(line, centerX, y, paint)
                    paint.textAlign = origAlign
                }
                y += lineSpacing
            }
        }
        return y
    }

    private fun drawIndentedWrapped(
        canvas: Canvas?,
        text: String,
        leftX: Float,
        maxWidth: Float,
        startY: Float,
        paint: Paint,
        lineSpacing: Float
    ): Float {
        var y = startY
        val lines = wrapText(text, paint, maxWidth)
        for (line in lines) {
            canvas?.let {
                val origAlign = paint.textAlign
                paint.textAlign = Paint.Align.LEFT
                it.drawText(line, leftX, y, paint)
                paint.textAlign = origAlign
            }
            y += lineSpacing
        }
        return y
    }

    private fun drawTwoColumnRow(
        canvas: Canvas?,
        leftText: String,
        rightText: String,
        leftX: Float,
        rightX: Float,
        startY: Float,
        leftPaint: Paint,
        rightPaint: Paint,
        lineSpacing: Float = 14f
    ): Float {
        val rightWidth = rightPaint.measureText(rightText)
        val gap = 8f
        val maxLeftWidth = ((rightX - leftX) - rightWidth - gap).coerceAtLeast(60f)
        val lines = wrapText(leftText, leftPaint, maxLeftWidth)

        canvas?.let {
            val origAlign = rightPaint.textAlign
            rightPaint.textAlign = Paint.Align.RIGHT
            it.drawText(rightText, rightX, startY, rightPaint)
            rightPaint.textAlign = origAlign

            val origLeftAlign = leftPaint.textAlign
            leftPaint.textAlign = Paint.Align.LEFT
            for ((index, line) in lines.withIndex()) {
                it.drawText(line, leftX, startY + (index * lineSpacing), leftPaint)
            }
            leftPaint.textAlign = origLeftAlign
        }

        val totalLines = lines.size.coerceAtLeast(1)
        return startY + (totalLines * lineSpacing)
    }

    /**
     * Builds standard formatted Indonesian WhatsApp Receipt message text
     */
    fun buildWhatsAppReceiptMessage(
        order: OrderEntity,
        items: List<OrderItemEntity>,
        outletInfo: OutletInfo = OutletInfo(),
        receiptConfig: ReceiptCustomizerConfig = ReceiptCustomizerConfig()
    ): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss 'WIB'", Locale("id", "ID"))
        val dateStr = dateFormat.format(Date(order.timestamp))
        val sb = StringBuilder()

        val orderTypeStr = if (order.orderType == "TAKEAWAY") "Bungkus (Takeaway)" else "Makan di Tempat (Dine-in)"
        val cashierName = order.cashierName.ifBlank { "Rian Pratama" }

        sb.appendLine("☕ *STRUK PEMBAYARAN - ${outletInfo.name.uppercase()}*")
        sb.appendLine("${outletInfo.address} (${outletInfo.branch})")
        sb.appendLine("Telp: ${outletInfo.phone}")
        sb.appendLine("━━━━━━━━━━━━━━━━━━")
        sb.appendLine("🧾 *No. Invoice:* ${order.invoiceNumber}")
        sb.appendLine("👨‍🍳 *Kasir:* $cashierName")
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
        if (receiptConfig.showTax && order.taxAmount > 0) {
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
        outletInfo: OutletInfo = OutletInfo(),
        receiptConfig: ReceiptCustomizerConfig = ReceiptCustomizerConfig()
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
        outletInfo: OutletInfo = OutletInfo(),
        receiptConfig: ReceiptCustomizerConfig = ReceiptCustomizerConfig()
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
