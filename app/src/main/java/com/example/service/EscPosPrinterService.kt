package com.example.service

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.example.data.local.model.OrderEntity
import com.example.data.local.model.OrderItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class BluetoothPrinterDevice(
    val name: String,
    val address: String,
    val isConnected: Boolean = false
)

object EscPosPrinterService {

    // Standard SPP (Serial Port Profile) UUID for Bluetooth Thermal Printers
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // ESC/POS Command Constants
    private val ESC_INIT = byteArrayOf(0x1B, 0x40) // ESC @
    private val ESC_ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00) // ESC a 0
    private val ESC_ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01) // ESC a 1
    private val ESC_ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 0x02) // ESC a 2
    private val ESC_BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01) // ESC E 1
    private val ESC_BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00) // ESC E 0
    private val GS_TEXT_DOUBLE = byteArrayOf(0x1D, 0x21, 0x11) // GS ! 0x11 (Double width + height)
    private val GS_TEXT_NORMAL = byteArrayOf(0x1D, 0x21, 0x00) // GS ! 0x00 (Normal)
    private val GS_CUT_PAPER = byteArrayOf(0x1D, 0x56, 0x42, 0x00) // GS V 66 0
    private val LF = byteArrayOf(0x0A)

    private const val PAPER_WIDTH_CHARS = 32 // Standard 58mm thermal paper column width

    /**
     * Builds standard ESC/POS byte array payload for 58mm / 80mm thermal printers.
     */
    fun buildReceiptBytes(
        order: OrderEntity,
        items: List<OrderItemEntity>,
        storeName: String = "CAFFEINERY SPECIALTY",
        storeAddress: String = "Jl. Senopati No. 42, Jakarta",
        storePhone: String = "Telp: +62 21 555-8900",
        footerMessage: String = "Thank you for your visit!\nFree Wi-Fi: caffeinery_guest (pass: espresso99)"
    ): ByteArray {
        val out = ByteArrayOutputStream()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val dateStr = dateFormat.format(Date(order.timestamp))

        // 1. Initialize
        out.write(ESC_INIT)

        // 2. Header (Center, Double Size)
        out.write(ESC_ALIGN_CENTER)
        out.write(ESC_BOLD_ON)
        out.write(GS_TEXT_DOUBLE)
        out.write("$storeName\n".toByteArray(Charsets.ISO_8859_1))
        out.write(GS_TEXT_NORMAL)
        out.write(ESC_BOLD_OFF)

        // Store Address & Contact
        out.write("$storeAddress\n".toByteArray(Charsets.ISO_8859_1))
        out.write("$storePhone\n".toByteArray(Charsets.ISO_8859_1))
        out.write(dashedLine().toByteArray(Charsets.ISO_8859_1))

        // 3. Invoice & Metadata (Left Aligned)
        out.write(ESC_ALIGN_LEFT)
        out.write(formatTwoColumns("Invoice:", order.invoiceNumber).toByteArray(Charsets.ISO_8859_1))
        out.write(formatTwoColumns("Date:", dateStr).toByteArray(Charsets.ISO_8859_1))
        out.write(formatTwoColumns("Customer:", order.customerName).toByteArray(Charsets.ISO_8859_1))
        out.write(formatTwoColumns("Payment:", order.paymentMethod).toByteArray(Charsets.ISO_8859_1))
        out.write(solidLine().toByteArray(Charsets.ISO_8859_1))

        // 4. Line Items
        out.write(ESC_BOLD_ON)
        out.write(formatTwoColumns("ITEMS", "TOTAL (IDR)").toByteArray(Charsets.ISO_8859_1))
        out.write(ESC_BOLD_OFF)
        out.write(dashedLine().toByteArray(Charsets.ISO_8859_1))

        for (item in items) {
            // Line 1: Item Name x Qty & Line Total
            val itemQty = "${item.productName} x${item.quantity}"
            val totalStr = formatIdr(item.lineTotal)
            out.write(formatTwoColumns(itemQty, totalStr).toByteArray(Charsets.ISO_8859_1))

            // Line 2: Variants/Notes if present
            if (item.variantSummary.isNotBlank() && item.variantSummary != "Standard") {
                out.write("  (${item.variantSummary})\n".toByteArray(Charsets.ISO_8859_1))
            }
            if (item.itemDiscount > 0) {
                out.write(formatTwoColumns("  *Item Discount", "-${formatIdr(item.itemDiscount)}").toByteArray(Charsets.ISO_8859_1))
            }
        }

        out.write(dashedLine().toByteArray(Charsets.ISO_8859_1))

        // 5. Totals & Tax
        out.write(formatTwoColumns("Subtotal:", formatIdr(order.subtotal)).toByteArray(Charsets.ISO_8859_1))
        if (order.orderDiscountAmount > 0) {
            out.write(formatTwoColumns("Order Discount:", "-${formatIdr(order.orderDiscountAmount)}").toByteArray(Charsets.ISO_8859_1))
        }
        if (order.taxAmount > 0) {
            out.write(formatTwoColumns("PB1 Tax (${order.taxPercentage.toInt()}%):", formatIdr(order.taxAmount)).toByteArray(Charsets.ISO_8859_1))
        }

        out.write(solidLine().toByteArray(Charsets.ISO_8859_1))

        // Grand Total (Bold, Large)
        out.write(ESC_BOLD_ON)
        out.write(formatTwoColumns("GRAND TOTAL:", formatIdr(order.grandTotal)).toByteArray(Charsets.ISO_8859_1))
        out.write(ESC_BOLD_OFF)

        if (order.paymentMethod == "CASH") {
            out.write(formatTwoColumns("Cash Tendered:", formatIdr(order.tenderedAmount)).toByteArray(Charsets.ISO_8859_1))
            out.write(formatTwoColumns("Change Due:", formatIdr(order.changeAmount)).toByteArray(Charsets.ISO_8859_1))
        }

        out.write(dashedLine().toByteArray(Charsets.ISO_8859_1))

        // 6. Footer (Center)
        out.write(ESC_ALIGN_CENTER)
        out.write("$footerMessage\n".toByteArray(Charsets.ISO_8859_1))
        out.write(LF)
        out.write(LF)
        out.write(LF)

        // 7. Cut paper
        out.write(GS_CUT_PAPER)

        return out.toByteArray()
    }

    fun dashedLine(): String = "-".repeat(PAPER_WIDTH_CHARS) + "\n"
    fun solidLine(): String = "=".repeat(PAPER_WIDTH_CHARS) + "\n"

    fun formatTwoColumns(left: String, right: String, maxWidth: Int = PAPER_WIDTH_CHARS): String {
        val totalLen = left.length + right.length
        return if (totalLen >= maxWidth) {
            val availableLeft = maxWidth - right.length - 1
            if (availableLeft > 0) {
                left.take(availableLeft) + " " + right + "\n"
            } else {
                left + "\n" + right.padStart(maxWidth) + "\n"
            }
        } else {
            val spaces = maxWidth - totalLen
            left + " ".repeat(spaces) + right + "\n"
        }
    }

    private fun formatIdr(amount: Double): String {
        return "Rp " + String.format(Locale.GERMAN, "%,.0f", amount)
    }

    /**
     * Retrieves paired Bluetooth devices for thermal printer connection.
     */
    @SuppressLint("MissingPermission")
    fun getPairedBluetoothPrinters(context: Context): List<BluetoothPrinterDevice> {
        val list = mutableListOf<BluetoothPrinterDevice>()
        try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            if (adapter != null && adapter.isEnabled) {
                val pairedDevices: Set<BluetoothDevice>? = adapter.bondedDevices
                pairedDevices?.forEach { device ->
                    list.add(
                        BluetoothPrinterDevice(
                            name = device.name ?: "Unknown Device",
                            address = device.address ?: "00:00:00:00:00:00"
                        )
                    )
                }
            }
        } catch (_: Exception) {
        }
        // Always include simulated virtual printer for testing/preview in emulator
        if (list.none { it.name.contains("Simulator") }) {
            list.add(0, BluetoothPrinterDevice("Virtual ESC/POS 58mm (Simulator)", "00:11:22:33:44:55", isConnected = true))
        }
        return list
    }

    /**
     * Connects to a physical or virtual Bluetooth thermal printer and transmits ESC/POS bytes.
     */
    @SuppressLint("MissingPermission")
    suspend fun printBytes(
        deviceAddress: String,
        payload: ByteArray
    ): Result<String> = withContext(Dispatchers.IO) {
        if (deviceAddress == "00:11:22:33:44:55") {
            // Virtual Simulator Success
            return@withContext Result.success("Printed successfully to Virtual Thermal Printer (${payload.size} bytes).")
        }

        var socket: BluetoothSocket? = null
        var outputStream: OutputStream? = null
        try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
                ?: return@withContext Result.failure(Exception("Bluetooth adapter not available on device"))

            val device = adapter.getRemoteDevice(deviceAddress)
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()
            outputStream = socket.outputStream
            outputStream.write(payload)
            outputStream.flush()
            Result.success("Print job sent successfully to ${device.name ?: deviceAddress}")
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try {
                outputStream?.close()
                socket?.close()
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Converts raw bytes to readable Hex dump string for debugging and protocol inspection.
     */
    fun bytesToHexDump(bytes: ByteArray): String {
        val hexChars = "0123456789ABCDEF"
        val sb = StringBuilder()
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            sb.append(hexChars[v ushr 4])
            sb.append(hexChars[v and 0x0F])
            sb.append(" ")
            if ((i + 1) % 16 == 0) sb.append("\n")
            else if ((i + 1) % 8 == 0) sb.append("  ")
        }
        return sb.toString()
    }
}
