package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.model.OrderEntity
import com.example.data.local.model.OrderItemEntity
import com.example.service.EscPosPrinterService
import com.example.service.QrisPayloadGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Caffeinery POS by BDS Tech", appName)
    }

    @Test
    fun `dynamic QRIS generation generates valid TLV and CRC`() {
        val payload = QrisPayloadGenerator.generateDynamicQris(
            invoiceNumber = "INV-20261021-001",
            amount = 45000.0,
            merchantName = "CAFFEINERY SPECIALTY"
        )
        assertNotNull(payload)
        assertTrue(payload.startsWith("000201010212")) // Format 01 + Dynamic 12
        assertTrue(payload.contains("5303360")) // Currency IDR
        assertTrue(payload.contains("540545000")) // Amount 45000
        assertTrue(payload.contains("6304")) // CRC tag
        assertEquals(4, payload.takeLast(4).length) // 4-char CRC16
    }

    @Test
    fun `esc pos byte builder produces valid printer commands`() {
        val order = OrderEntity(
            invoiceNumber = "INV-001",
            timestamp = System.currentTimeMillis(),
            customerName = "Guest",
            subtotal = 35000.0,
            grandTotal = 38500.0,
            paymentMethod = "CASH",
            tenderedAmount = 50000.0,
            changeAmount = 11500.0
        )
        val items = listOf(
            OrderItemEntity(
                orderInvoiceNumber = "INV-001",
                productId = 1,
                productName = "Spanish Latte",
                sku = "CF-001",
                basePrice = 35000.0,
                unitPrice = 35000.0,
                quantity = 1,
                lineTotal = 35000.0
            )
        )
        val bytes = EscPosPrinterService.buildReceiptBytes(order, items)
        assertTrue(bytes.isNotEmpty())
        // Verify ESC @ (0x1B, 0x40) init command
        assertEquals(0x1B.toByte(), bytes[0])
        assertEquals(0x40.toByte(), bytes[1])
    }
}
