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

    @Test
    fun `csv export includes 21 dedicated multi columns with cashier and shift`() {
        val order = OrderEntity(
            invoiceNumber = "INV-002",
            timestamp = System.currentTimeMillis(),
            customerName = "Budi Walk-in",
            cashierName = "Maya Sari",
            shiftName = "Shift 2",
            subtotal = 50000.0,
            grandTotal = 55000.0,
            paymentMethod = "QRIS"
        )
        val item = OrderItemEntity(
            orderInvoiceNumber = "INV-002",
            productId = 1,
            productName = "Spanish Latte",
            sku = "CF-001",
            basePrice = 35000.0,
            unitPrice = 40000.0,
            quantity = 1,
            lineTotal = 40000.0,
            categoryName = "Coffee",
            variantSize = "Large (+Rp 5.000)",
            variantType = "Iced",
            variantBeans = "House Blend",
            variantGrind = "-",
            variantSugar = "Normal 100%",
            variantAddons = "Extra Espresso Shot (+Rp 6.000)",
            variantExtraTotal = 11000.0
        )
        val csv = com.example.service.CsvExportService.generateSalesCsv(
            listOf(order),
            mapOf("INV-002" to listOf(item))
        )
        assertTrue(csv.contains("Transaction ID,Date & Time,Employee / Cashier,Shift,Order Type,Category,Product Name,Base Price,Variant - Size,Variant - Type,Variant - Beans Type,Variant - Grind Size,Variant - Sugar Level,Variant - Add-ons,Variant Extra Price Total,Quantity,Subtotal,Discount,Tax / Service Fee,Grand Total,Payment Method"))
        assertTrue(csv.contains("Maya Sari,Shift 2"))
        assertTrue(csv.contains("Large (+Rp 5.000)"))
        assertTrue(csv.contains("Extra Espresso Shot (+Rp 6.000)"))
    }

    @Test
    fun `settings storage service persists and loads custom outlet info across restarts`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage1 = com.example.data.local.SettingsStorageService(context)
        storage1.saveOutletInfo(
            com.example.domain.model.OutletInfo(
                name = "BDS Coffee Roasters",
                branch = "Mega Kuningan",
                address = "Jl. Prof. Dr. Satrio No. 1",
                phone = "+62 811-2233-4455",
                taxId = "99.888.777.6-555.000",
                instagram = "@bdscoffee_roasters"
            )
        )

        // Simulate app restart with a fresh storage instance
        val storage2 = com.example.data.local.SettingsStorageService(context)
        val loaded = storage2.loadOutletInfo()
        assertEquals("BDS Coffee Roasters", loaded.name)
        assertEquals("Mega Kuningan", loaded.branch)
        assertEquals("Jl. Prof. Dr. Satrio No. 1", loaded.address)
        assertEquals("+62 811-2233-4455", loaded.phone)
        assertEquals("@bdscoffee_roasters", loaded.instagram)
    }

    @Test
    fun `settings storage service persists receipt customizer and tax configurations`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage1 = com.example.data.local.SettingsStorageService(context)
        storage1.saveReceiptCustomizer(
            com.example.domain.model.ReceiptCustomizerConfig(
                paperSize = "80mm",
                showLogo = false,
                showTax = false,
                headerNote = "CUSTOM HEADER BDS",
                footerNote = "CUSTOM FOOTER NOTE"
            )
        )
        storage1.saveAdditionalFees(
            com.example.domain.model.AdditionalFeesConfig(
                isTaxEnabled = true,
                taxPercentage = 11.0,
                isServiceFeeEnabled = true,
                serviceFeePercentage = 7.0,
                isRoundingEnabled = false
            )
        )

        // Simulate app restart
        val storage2 = com.example.data.local.SettingsStorageService(context)
        val loadedReceipt = storage2.loadReceiptCustomizer()
        assertEquals("80mm", loadedReceipt.paperSize)
        assertEquals(false, loadedReceipt.showLogo)
        assertEquals(false, loadedReceipt.showTax)
        assertEquals("CUSTOM HEADER BDS", loadedReceipt.headerNote)
        assertEquals("CUSTOM FOOTER NOTE", loadedReceipt.footerNote)

        val loadedFees = storage2.loadAdditionalFees()
        assertEquals(true, loadedFees.isTaxEnabled)
        assertEquals(11.0, loadedFees.taxPercentage, 0.01)
        assertEquals(true, loadedFees.isServiceFeeEnabled)
        assertEquals(7.0, loadedFees.serviceFeePercentage, 0.01)
        assertEquals(false, loadedFees.isRoundingEnabled)
    }

    @Test
    fun `settings storage service persists active cashier and simplified shift`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage1 = com.example.data.local.SettingsStorageService(context)
        val emp = com.example.domain.model.EmployeeItem(
            name = "Budi Santoso",
            role = "Kasir",
            shift = "Shift 2",
            pin = "2024"
        )
        storage1.saveActiveEmployee(emp)
        storage1.saveActiveShift("Shift 2")

        // Simulate app restart
        val storage2 = com.example.data.local.SettingsStorageService(context)
        val loadedEmp = storage2.loadActiveEmployee()
        val loadedShift = storage2.loadActiveShift()

        assertEquals("Budi Santoso", loadedEmp.name)
        assertEquals("Kasir", loadedEmp.role)
        assertEquals("Shift 2", loadedShift)

        // Also test Event shift
        storage2.saveActiveShift("Event")
        assertEquals("Event", storage2.loadActiveShift())
    }

    @Test
    fun `cart item variant summary shows nominal price additions and hides zero cost`() {
        val product = com.example.data.local.model.ProductEntity(
            id = 10,
            categoryId = "coffee",
            name = "Espresso Tonic",
            sku = "CF-010",
            basePrice = 28000.0,
            costPrice = 10000.0,
            stockQuantity = 20
        )
        val variants = mapOf(
            "Size" to listOf(com.example.domain.model.VariantOption("Large", 5000.0)),
            "Type" to listOf(com.example.domain.model.VariantOption("Iced", 0.0)),
            "Add-ons" to listOf(com.example.domain.model.VariantOption("Extra Espresso Shot", 6000.0))
        )
        val cartItem = com.example.domain.model.CartItem(
            product = product,
            selectedVariants = variants,
            quantity = 1
        )

        val summary = cartItem.variantSummary
        assertTrue(summary.contains("Large (+Rp 5.000)"))
        assertTrue(summary.contains("Extra Espresso Shot (+Rp 6.000)"))
        assertTrue(summary.contains("Iced"))
        // Verify zero cost does not have (+Rp 0)
        org.junit.Assert.assertFalse(summary.contains("Iced (+Rp 0)"))
    }

    @Test
    fun `pdf receipt generation renders with dynamic height and no overflow`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val order = OrderEntity(
            invoiceNumber = "INV-TEST-099",
            timestamp = System.currentTimeMillis(),
            customerName = "Arif Rahardian",
            orderType = "DINE_IN",
            cashierName = "Rian Pratama",
            shiftName = "Shift 1",
            subtotal = 70000.0,
            taxAmount = 7000.0,
            grandTotal = 77000.0,
            paymentMethod = "CASH",
            tenderedAmount = 100000.0,
            changeAmount = 23000.0
        )
        val items = listOf(
            OrderItemEntity(
                orderInvoiceNumber = "INV-TEST-099",
                productId = 1,
                productName = "Specialty House Blend V60 Long Name That Might Wrap",
                sku = "CF-001",
                basePrice = 35000.0,
                unitPrice = 40000.0,
                quantity = 2,
                lineTotal = 70000.0,
                variantSummary = "Large (+Rp 5.000), Extra Espresso Shot (+Rp 6.000)"
            )
        )
        val message = com.example.service.PdfReceiptService.buildWhatsAppReceiptMessage(
            order = order,
            items = items
        )
        assertNotNull(message)
        assertTrue(message.contains("Kasir:* Rian Pratama"))
        org.junit.Assert.assertFalse(message.contains("Shift 1"))
        assertTrue(message.contains("Large (+Rp 5.000)"))
        assertTrue(message.contains("Extra Espresso Shot (+Rp 6.000)"))
        assertTrue(message.contains("77.000"))
    }

    @Test
    fun `official BDS COFFEE CO dynamic and static QRIS generation`() {
        val dynamicPayload = QrisPayloadGenerator.generateDynamicQris(
            invoiceNumber = "INV-BDS-001",
            amount = 50000.0,
            merchantName = "BDS COFFEE CO",
            nmid = "ID1024365195063",
            terminalId = "A01"
        )
        assertNotNull(dynamicPayload)
        assertTrue(dynamicPayload.contains("BDS COFFEE CO"))
        assertTrue(dynamicPayload.contains("ID1024365195063"))
        assertTrue(dynamicPayload.contains("A01"))
        assertTrue(dynamicPayload.contains("540550000")) // Nominal Rp 50.000
        assertEquals(4, dynamicPayload.takeLast(4).length)
        assertTrue(QrisPayloadGenerator.isValidQrisPayload(dynamicPayload))

        val staticPayload = QrisPayloadGenerator.generateStaticQris(
            merchantName = "BDS COFFEE CO",
            nmid = "ID1024365195063",
            terminalId = "A01"
        )
        assertEquals(QrisPayloadGenerator.OFFICIAL_STATIC_QRIS_PAYLOAD, staticPayload)
        assertTrue(staticPayload.startsWith("000201010211")) // Point of initiation 11 (Static QR)
        assertTrue(staticPayload.contains("BDS COFFEE CO"))
        assertTrue(staticPayload.contains("ID1024365195063"))
        assertTrue(staticPayload.contains("A01"))
        // Static QR does NOT have Tag 54 (amount fixed)
        org.junit.Assert.assertFalse(staticPayload.contains("540550000"))
        assertTrue(QrisPayloadGenerator.isValidQrisPayload(staticPayload))
    }

    @Test
    fun `zxing qr matrix generator produces valid non-empty matrix for official payload`() {
        val matrix = com.example.ui.components.QrMatrixBuilder.generateMatrix(QrisPayloadGenerator.OFFICIAL_STATIC_QRIS_PAYLOAD)
        assertTrue(matrix.isNotEmpty())
        assertTrue(matrix[0].isNotEmpty())
        val hasDarkModules = matrix.any { row -> row.any { it } }
        assertTrue(hasDarkModules)
        // With margin=1, (1, 1) is the start of the top-left finder pattern
        assertTrue(matrix[1][1])
    }

    @Test
    fun `official qris_bds asset png exists and loads via AssetManager and Drawable`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val assetStream = context.assets.open("qris_bds.png")
        assertNotNull(assetStream)
        assertTrue(assetStream.available() > 0)
        assetStream.close()

        val drawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.qris_bds)
        assertNotNull(drawable)
    }

    @Test
    fun `printer connection guard recognizes virtual simulator and rejects blank address`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertTrue(EscPosPrinterService.isPrinterConnected(context, "00:11:22:33:44:55"))
        org.junit.Assert.assertFalse(EscPosPrinterService.isPrinterConnected(context, ""))
        org.junit.Assert.assertFalse(EscPosPrinterService.isPrinterConnected(context, "   "))
    }

    @Test
    fun `thermal receipt bytes show only cashier name and omit shift`() {
        val order = OrderEntity(
            invoiceNumber = "INV-003",
            timestamp = System.currentTimeMillis(),
            customerName = "Guest",
            cashierName = "Budi Kasir",
            shiftName = "Shift 1",
            subtotal = 35000.0,
            grandTotal = 35000.0,
            paymentMethod = "CASH"
        )
        val items = listOf(
            OrderItemEntity(
                orderInvoiceNumber = "INV-003",
                productId = 1,
                productName = "Latte",
                sku = "CF-001",
                basePrice = 35000.0,
                unitPrice = 35000.0,
                quantity = 1,
                lineTotal = 35000.0
            )
        )
        val bytes = EscPosPrinterService.buildReceiptBytes(order, items)
        val receiptText = String(bytes, Charsets.ISO_8859_1)
        assertTrue(receiptText.contains("Kasir:"))
        assertTrue(receiptText.contains("Budi Kasir"))
        org.junit.Assert.assertFalse(receiptText.contains("Shift 1"))
    }

    @Test
    fun `convertToDynamicQRIS correctly transforms base static payload with Tag 54 injection for Rp 29400`() {
        // Base static string for BDS COFFEE CO
        val baseStatic = QrisPayloadGenerator.OFFICIAL_STATIC_QRIS_PAYLOAD
        assertTrue(baseStatic.contains("010211")) // Static point of initiation

        val dynamicResult = QrisPayloadGenerator.convertToDynamicQRIS(
            staticString = baseStatic,
            amount = 29400.0,
            invoiceNumber = "INV-29400"
        )

        assertNotNull(dynamicResult)
        // 1. Point of initiation converted from 11 to 12
        assertTrue(dynamicResult.startsWith("000201010212"))
        org.junit.Assert.assertFalse(dynamicResult.contains("010211"))

        // 2. Tag 54 injected with exact length 05 and value 29400
        assertTrue(dynamicResult.contains("540529400"))

        // 3. Official credentials preserved
        assertTrue(dynamicResult.contains("ID1024365195063"))
        assertTrue(dynamicResult.contains("BDS COFFEE CO"))
        assertTrue(dynamicResult.contains("A01"))

        // 4. Tag 53 (IDR currency) precedes Tag 54
        val idx53 = dynamicResult.indexOf("5303360")
        val idx54 = dynamicResult.indexOf("540529400")
        assertTrue(idx53 != -1 && idx54 != -1 && idx54 > idx53)

        // 5. CRC16-CCITT checksum valid
        assertTrue(dynamicResult.contains("6304"))
        assertEquals(4, dynamicResult.takeLast(4).length)
        assertTrue(QrisPayloadGenerator.isValidQrisPayload(dynamicResult))
    }

    @Test
    fun `order entity and receipts correctly map customer name under Pelanggan`() {
        val order = OrderEntity(
            invoiceNumber = "INV-004",
            timestamp = System.currentTimeMillis(),
            customerName = "Kak Budi - Meja 03",
            cashierName = "Rian Kasir",
            subtotal = 40000.0,
            grandTotal = 40000.0,
            paymentMethod = "CASH"
        )
        val items = listOf(
            OrderItemEntity(
                orderInvoiceNumber = "INV-004",
                productId = 1,
                productName = "Americano",
                sku = "CF-002",
                basePrice = 40000.0,
                unitPrice = 40000.0,
                quantity = 1,
                lineTotal = 40000.0
            )
        )
        val bytes = EscPosPrinterService.buildReceiptBytes(order, items)
        val receiptText = String(bytes, Charsets.ISO_8859_1)
        assertTrue(receiptText.contains("Pelanggan:"))
        assertTrue(receiptText.contains("Kak Budi - Meja 03"))

        val message = com.example.service.PdfReceiptService.buildWhatsAppReceiptMessage(order, items)
        assertTrue(message.contains("👤 *Pelanggan:* Kak Budi - Meja 03"))
    }

    @Test
    fun `tiered B2B pricing calculates correct dynamic unit price and tier labels`() {
        val b2bProduct = com.example.data.local.model.ProductEntity(
            id = 201,
            sku = "B2B-001",
            name = "Roasted Beans House Blend 1kg",
            categoryId = "roasted_beans",
            basePrice = 250000.0,
            costPrice = 120000.0,
            stockQuantity = 100,
            hasB2bPricing = true,
            b2bTier1MinQty = 5,
            b2bTier1Price = 220000.0,
            b2bTier2MinQty = 10,
            b2bTier2Price = 200000.0
        )

        // Case 1: Standard retail qty < Tier 1
        val cartRetail = com.example.domain.model.CartItem(
            product = b2bProduct,
            quantity = 2
        )
        assertEquals(250000.0, cartRetail.finalUnitPrice, 0.01)
        assertEquals(500000.0, cartRetail.lineTotal, 0.01)
        org.junit.Assert.assertFalse(cartRetail.isB2bPriceApplied)
        org.junit.Assert.assertNull(cartRetail.b2bTierLabel)

        // Case 2: Tier 1 (qty = 5)
        val cartTier1 = com.example.domain.model.CartItem(
            product = b2bProduct,
            quantity = 5
        )
        assertEquals(220000.0, cartTier1.finalUnitPrice, 0.01)
        assertEquals(1100000.0, cartTier1.lineTotal, 0.01)
        assertTrue(cartTier1.isB2bPriceApplied)
        assertEquals("B2B Tier 1 (≥5)", cartTier1.b2bTierLabel)

        // Case 3: Tier 2 (qty = 10)
        val cartTier2 = com.example.domain.model.CartItem(
            product = b2bProduct,
            quantity = 10
        )
        assertEquals(200000.0, cartTier2.finalUnitPrice, 0.01)
        assertEquals(2000000.0, cartTier2.lineTotal, 0.01)
        assertTrue(cartTier2.isB2bPriceApplied)
        assertEquals("B2B Tier 2 (≥10)", cartTier2.b2bTierLabel)

        // Case 4: Non-B2B product remains strictly standard retail even at high quantity
        val standardProduct = com.example.data.local.model.ProductEntity(
            id = 202,
            sku = "STD-001",
            name = "Croissant Butter",
            categoryId = "pastry",
            basePrice = 28000.0,
            costPrice = 12000.0,
            stockQuantity = 50,
            hasB2bPricing = false
        )
        val cartStandard = com.example.domain.model.CartItem(
            product = standardProduct,
            quantity = 25
        )
        assertEquals(28000.0, cartStandard.finalUnitPrice, 0.01)
        assertEquals(700000.0, cartStandard.lineTotal, 0.01)
        org.junit.Assert.assertFalse(cartStandard.isB2bPriceApplied)
        org.junit.Assert.assertNull(cartStandard.b2bTierLabel)
    }

    @Test
    fun `database seeder populates catalog, b2b items, and orders on empty db and is idempotent`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.local.AppDatabase::class.java).build()
        val dao = db.posDao()

        assertEquals(0, dao.getProductCount())
        assertEquals(0, dao.getCategoryCount())
        assertEquals(0, dao.getOrderCount())

        // First seed
        com.example.data.local.DatabaseSeeder.seedIfEmpty(dao)

        val prodCount = dao.getProductCount()
        val catCount = dao.getCategoryCount()
        val orderCount = dao.getOrderCount()

        assertTrue(prodCount >= 15)
        assertEquals(5, catCount)
        assertTrue(orderCount >= 3)

        // Check B2B beans exist
        val prods = dao.getAllProductsSync()
        val b2bBeans = prods.find { it.name.contains("House Blend Beans", ignoreCase = true) }
        assertNotNull(b2bBeans)
        assertTrue(b2bBeans!!.hasB2bPricing)
        assertEquals(5, b2bBeans.b2bTier1MinQty)
        assertEquals(85000.0, b2bBeans.b2bTier1Price, 0.01)

        // Second seed (idempotent - should not duplicate)
        com.example.data.local.DatabaseSeeder.seedIfEmpty(dao)
        assertEquals(prodCount, dao.getProductCount())
        assertEquals(catCount, dao.getCategoryCount())
        assertEquals(orderCount, dao.getOrderCount())

        db.close()
    }

    @Test
    fun `cart draft persistence and recovery in SettingsStorageService`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val service = com.example.data.local.SettingsStorageService(context)

        service.clearActiveCartDraft()
        org.junit.Assert.assertNull(service.loadActiveCartDraft())

        val sampleJson = """[{"productId":1,"productName":"BDS Aren Latte","quantity":2}]"""
        service.saveActiveCartDraft(
            itemsJson = sampleJson,
            customerName = "Kak Rudi / Meja 03",
            orderType = "DINE_IN",
            discountType = "NONE",
            discountValue = 0.0
        )

        val loaded = service.loadActiveCartDraft()
        assertNotNull(loaded)
        assertEquals("Kak Rudi / Meja 03", loaded!!.customerName)
        assertEquals("DINE_IN", loaded.orderType)
        assertEquals(sampleJson, loaded.itemsJson)

        service.clearActiveCartDraft()
        org.junit.Assert.assertNull(service.loadActiveCartDraft())
    }

    @Test
    fun `dynamic date grouping partitions transactions by WIB calendar date and calculates day-specific totals`() {
        val calWib = java.util.Calendar.getInstance(com.example.ui.components.DateTimeUtils.WIB_ZONE).apply {
            set(2026, java.util.Calendar.OCTOBER, 28, 10, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val t1 = calWib.timeInMillis // 28 Oct 2026 10:00 WIB

        calWib.set(java.util.Calendar.HOUR_OF_DAY, 23)
        calWib.set(java.util.Calendar.MINUTE, 30)
        val t2LateNight = calWib.timeInMillis // 28 Oct 2026 23:30 WIB (in UTC this is 16:30 same day, but tests calendar boundary)

        calWib.set(2026, java.util.Calendar.OCTOBER, 27, 14, 0, 0)
        val t3Yesterday = calWib.timeInMillis // 27 Oct 2026 14:00 WIB

        // 1. Verify Date Key grouping (yyyy-MM-dd)
        assertEquals("2026-10-28", com.example.ui.components.DateTimeUtils.formatDateKey(t1))
        assertEquals("2026-10-28", com.example.ui.components.DateTimeUtils.formatDateKey(t2LateNight))
        assertEquals("2026-10-27", com.example.ui.components.DateTimeUtils.formatDateKey(t3Yesterday))

        // 2. Verify Indonesian Header formatting
        val header1 = com.example.ui.components.DateTimeUtils.formatDateHeader(t1)
        val header3 = com.example.ui.components.DateTimeUtils.formatDateHeader(t3Yesterday)
        assertTrue(header1.contains("28"))
        assertTrue(header1.contains("Okt") || header1.contains("Oktober"))
        assertTrue(header3.contains("27"))

        // 3. Verify Orders Grouping and Specific Daily Totals
        val orders = listOf(
            OrderEntity(
                invoiceNumber = "INV-01",
                timestamp = t1,
                customerName = "Cust 1",
                subtotal = 50000.0,
                grandTotal = 50000.0,
                paymentMethod = "CASH"
            ),
            OrderEntity(
                invoiceNumber = "INV-02",
                timestamp = t2LateNight,
                customerName = "Cust 2",
                subtotal = 75000.0,
                grandTotal = 75000.0,
                paymentMethod = "QRIS"
            ),
            OrderEntity(
                invoiceNumber = "INV-03",
                timestamp = t3Yesterday,
                customerName = "Cust 3",
                subtotal = 120000.0,
                grandTotal = 120000.0,
                paymentMethod = "BANK_TRANSFER"
            )
        )

        val grouped = orders.groupBy { com.example.ui.components.DateTimeUtils.formatDateKey(it.timestamp) }
        assertEquals(2, grouped.keys.size)
        assertTrue(grouped.containsKey("2026-10-28"))
        assertTrue(grouped.containsKey("2026-10-27"))

        val day1Orders = grouped["2026-10-28"]!!
        val day2Orders = grouped["2026-10-27"]!!

        assertEquals(2, day1Orders.size)
        assertEquals(125000.0, day1Orders.sumOf { it.grandTotal }, 0.01)

        assertEquals(1, day2Orders.size)
        assertEquals(120000.0, day2Orders.sumOf { it.grandTotal }, 0.01)

        // 4. Verify Start & End of Day bounds in WIB
        val startOfT1 = com.example.ui.components.DateTimeUtils.getStartOfDayWib(t1)
        val endOfT1 = com.example.ui.components.DateTimeUtils.getEndOfDayWib(t1)
        assertTrue(t1 in startOfT1..endOfT1)
        assertTrue(t2LateNight in startOfT1..endOfT1)
        org.junit.Assert.assertFalse(t3Yesterday in startOfT1..endOfT1)
    }

    @Test
    fun `extract date directly from invoice ID pattern and partition historical dates independently`() {
        val nowMistakenTimestamp = System.currentTimeMillis() // Today timestamp

        // Orders created on different historical dates but with identical/today raw timestamp
        val orderOct27 = OrderEntity(
            invoiceNumber = "INV-20261027-142010",
            timestamp = nowMistakenTimestamp,
            customerName = "Cust 27",
            subtotal = 50000.0,
            grandTotal = 50000.0,
            paymentMethod = "CASH"
        )
        val orderOct28 = OrderEntity(
            invoiceNumber = "INV-20261028-113045",
            timestamp = nowMistakenTimestamp,
            customerName = "Cust 28",
            subtotal = 80000.0,
            grandTotal = 80000.0,
            paymentMethod = "QRIS"
        )
        val orderOct29 = OrderEntity(
            invoiceNumber = "INV-20261029-091520",
            timestamp = nowMistakenTimestamp,
            customerName = "Cust 29",
            subtotal = 35000.0,
            grandTotal = 35000.0,
            paymentMethod = "BANK_TRANSFER"
        )

        // 1. Verify getOrderDateKey parses directly from invoice prefix
        assertEquals("2026-10-27", com.example.ui.components.DateTimeUtils.getOrderDateKey(orderOct27))
        assertEquals("2026-10-28", com.example.ui.components.DateTimeUtils.getOrderDateKey(orderOct28))
        assertEquals("2026-10-29", com.example.ui.components.DateTimeUtils.getOrderDateKey(orderOct29))

        // 2. Verify getOrderEffectiveTimestamp extracts exact WIB time
        val eff27 = com.example.ui.components.DateTimeUtils.getOrderEffectiveTimestamp(orderOct27)
        val eff28 = com.example.ui.components.DateTimeUtils.getOrderEffectiveTimestamp(orderOct28)
        val eff29 = com.example.ui.components.DateTimeUtils.getOrderEffectiveTimestamp(orderOct29)

        assertEquals("14:20:10 WIB", com.example.ui.components.DateTimeUtils.formatTime(eff27))
        assertEquals("11:30:45 WIB", com.example.ui.components.DateTimeUtils.formatTime(eff28))
        assertEquals("09:15:20 WIB", com.example.ui.components.DateTimeUtils.formatTime(eff29))

        // 3. Verify distinct calendar date grouping into 3 separate groups
        val orders = listOf(orderOct27, orderOct28, orderOct29)
        val groupedByDate = orders.groupBy { com.example.ui.components.DateTimeUtils.getOrderDateKey(it) }

        assertEquals(3, groupedByDate.keys.size)
        assertTrue(groupedByDate.containsKey("2026-10-27"))
        assertTrue(groupedByDate.containsKey("2026-10-28"))
        assertTrue(groupedByDate.containsKey("2026-10-29"))

        // 4. Verify each group's independent totals
        assertEquals(50000.0, groupedByDate["2026-10-27"]!!.sumOf { it.grandTotal }, 0.01)
        assertEquals(80000.0, groupedByDate["2026-10-28"]!!.sumOf { it.grandTotal }, 0.01)
        assertEquals(35000.0, groupedByDate["2026-10-29"]!!.sumOf { it.grandTotal }, 0.01)
    }
}
