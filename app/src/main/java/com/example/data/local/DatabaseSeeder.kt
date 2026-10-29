package com.example.data.local

import com.example.data.local.dao.PosDao
import com.example.data.local.model.CategoryEntity
import com.example.data.local.model.OrderEntity
import com.example.data.local.model.OrderItemEntity
import com.example.data.local.model.ProductEntity
import com.example.data.local.model.VariantGroupEntity

/**
 * Idempotent Database Seeder for BDS Coffee Company POS.
 * Ensures initial catalog, categories, variant groups, and sample orders
 * exist on fresh install or empty database without overwriting user data.
 */
object DatabaseSeeder {

    suspend fun seedIfEmpty(dao: PosDao) {
        // 1. Seed Categories if empty
        if (dao.getCategoryCount() == 0) {
            val categories = listOf(
                CategoryEntity("coffee", "Coffee", "coffee", "#7C4A27", 1),
                CategoryEntity("non_coffee", "Non-Coffee", "local_cafe", "#2563EB", 2),
                CategoryEntity("roasted_beans", "Roasted Beans", "grain", "#92400E", 3),
                CategoryEntity("pastry", "Pastry & Bakery", "bakery_dining", "#D97706", 4),
                CategoryEntity("bahan_baku", "Bahan Baku", "inventory_2", "#059669", 5)
            )
            dao.insertCategories(categories)
        }

        // 2. Seed Variant Groups if empty
        if (dao.getVariantGroupCount() == 0) {
            val initialVariantGroups = listOf(
                VariantGroupEntity(
                    id = "size",
                    name = "Size",
                    isMultiSelect = false,
                    optionsJson = """[{"name":"Regular","price":0.0},{"name":"Large","price":5000.0}]""",
                    applicableCategoryIds = "coffee,non_coffee",
                    sortOrder = 1
                ),
                VariantGroupEntity(
                    id = "type",
                    name = "Type",
                    isMultiSelect = false,
                    optionsJson = """[{"name":"Iced","price":0.0},{"name":"Hot","price":0.0}]""",
                    applicableCategoryIds = "coffee,non_coffee",
                    sortOrder = 2
                ),
                VariantGroupEntity(
                    id = "beans_type",
                    name = "Beans Type",
                    isMultiSelect = false,
                    optionsJson = """[{"name":"House Blend (Brazil/Sumatra)","price":0.0},{"name":"Single Origin Ethiopia","price":8000.0},{"name":"Swiss Decaf","price":5000.0}]""",
                    applicableCategoryIds = "coffee",
                    sortOrder = 3
                ),
                VariantGroupEntity(
                    id = "grind_size",
                    name = "Grind Size",
                    isMultiSelect = false,
                    optionsJson = """[{"name":"Whole Bean","price":0.0},{"name":"Espresso (Fine)","price":0.0},{"name":"V60 / Medium Filter","price":0.0},{"name":"Cold Brew / Coarse","price":0.0}]""",
                    applicableCategoryIds = "roasted_beans",
                    sortOrder = 4
                ),
                VariantGroupEntity(
                    id = "sugar_level",
                    name = "Sugar Level",
                    isMultiSelect = false,
                    optionsJson = """[{"name":"Normal 100%","price":0.0},{"name":"Less Sugar 50%","price":0.0},{"name":"No Sugar 0%","price":0.0}]""",
                    applicableCategoryIds = "coffee,non_coffee",
                    sortOrder = 5
                ),
                VariantGroupEntity(
                    id = "addons",
                    name = "Add-ons",
                    isMultiSelect = true,
                    optionsJson = """[{"name":"Extra Espresso Shot","price":6000.0},{"name":"Oat Milk Sub","price":8000.0}]""",
                    applicableCategoryIds = "coffee,non_coffee",
                    sortOrder = 6
                )
            )
            dao.insertVariantGroups(initialVariantGroups)
        }

        // 3. Seed Products if empty
        if (dao.getProductCount() == 0) {
            val initialProducts = listOf(
                // Coffee
                ProductEntity(
                    sku = "CF-001",
                    name = "BDS Aren Latte",
                    categoryId = "coffee",
                    basePrice = 28000.0,
                    costPrice = 11000.0,
                    stockQuantity = 120,
                    unit = "cup",
                    description = "Signature house blend, susu segar, dan gula aren organik khas BDS",
                    iconName = "coffee",
                    isActive = true
                ),
                ProductEntity(
                    sku = "CF-002",
                    name = "Americano",
                    categoryId = "coffee",
                    basePrice = 24000.0,
                    costPrice = 5500.0,
                    stockQuantity = 150,
                    unit = "cup",
                    description = "Double shot espresso dengan air murni, bold dan menyegarkan",
                    iconName = "coffee",
                    isActive = true
                ),
                ProductEntity(
                    sku = "CF-003",
                    name = "Caffe Latte",
                    categoryId = "coffee",
                    basePrice = 28000.0,
                    costPrice = 10000.0,
                    stockQuantity = 80,
                    unit = "cup",
                    description = "Espresso seimbang dengan microfoam susu segar yang lembut",
                    iconName = "coffee",
                    isActive = true
                ),
                ProductEntity(
                    sku = "CF-004",
                    name = "Cappuccino",
                    categoryId = "coffee",
                    basePrice = 28000.0,
                    costPrice = 10000.0,
                    stockQuantity = 75,
                    unit = "cup",
                    description = "Foam tebal khas Italia dengan taburan dark cocoa dusting",
                    iconName = "coffee",
                    isActive = true
                ),
                ProductEntity(
                    sku = "CF-005",
                    name = "Caramel Macchiato",
                    categoryId = "coffee",
                    basePrice = 34000.0,
                    costPrice = 13000.0,
                    stockQuantity = 60,
                    unit = "cup",
                    description = "Vanilla milk, rich espresso, dan drizzle saus karamel premium",
                    iconName = "coffee",
                    isActive = true
                ),
                ProductEntity(
                    sku = "CF-006",
                    name = "V60 Manual Brew",
                    categoryId = "coffee",
                    basePrice = 32000.0,
                    costPrice = 12000.0,
                    stockQuantity = 50,
                    unit = "cup",
                    description = "Seduhan manual pour over dengan pilihan specialty single origin",
                    iconName = "coffee",
                    isActive = true
                ),

                // Non-Coffee
                ProductEntity(
                    sku = "NC-001",
                    name = "Matcha Fusion Latte",
                    categoryId = "non_coffee",
                    basePrice = 32000.0,
                    costPrice = 12500.0,
                    stockQuantity = 65,
                    unit = "cup",
                    description = "Pure Kyoto Uji matcha dengan susu segar manis seimbang",
                    iconName = "local_cafe",
                    isActive = true
                ),
                ProductEntity(
                    sku = "NC-002",
                    name = "Belgian Dark Chocolate",
                    categoryId = "non_coffee",
                    basePrice = 30000.0,
                    costPrice = 11000.0,
                    stockQuantity = 70,
                    unit = "cup",
                    description = "Cokelat Belgia hitam 70% kaya rasa dengan tekstur lembut",
                    iconName = "local_cafe",
                    isActive = true
                ),
                ProductEntity(
                    sku = "NC-003",
                    name = "Earl Grey Artisan Tea",
                    categoryId = "non_coffee",
                    basePrice = 24000.0,
                    costPrice = 6000.0,
                    stockQuantity = 50,
                    unit = "cup",
                    description = "Teh hitam artisan beraroma minyak bergamot Italia",
                    iconName = "local_cafe",
                    isActive = true
                ),

                // Roasted Beans (WITH B2B TIERED PRICING)
                ProductEntity(
                    sku = "RB-001",
                    name = "House Blend Beans 250g",
                    categoryId = "roasted_beans",
                    basePrice = 95000.0,
                    costPrice = 55000.0,
                    stockQuantity = 40,
                    unit = "pack",
                    description = "Komposisi 50% Brazil Cerrado & 50% Aceh Gayo. Tasting notes: Chocolate, Nutty, Brown Sugar.",
                    iconName = "grain",
                    isActive = true,
                    hasB2bPricing = true,
                    b2bTier1MinQty = 5,
                    b2bTier1Price = 85000.0,
                    b2bTier2MinQty = 10,
                    b2bTier2Price = 75000.0
                ),
                ProductEntity(
                    sku = "RB-002",
                    name = "Single Origin Ethiopia 200g",
                    categoryId = "roasted_beans",
                    basePrice = 135000.0,
                    costPrice = 80000.0,
                    stockQuantity = 25,
                    unit = "pack",
                    description = "Yirgacheffe Washed Grade 1. Tasting notes: Jasmine, Bergamot, Peach.",
                    iconName = "grain",
                    isActive = true,
                    hasB2bPricing = true,
                    b2bTier1MinQty = 5,
                    b2bTier1Price = 120000.0,
                    b2bTier2MinQty = 10,
                    b2bTier2Price = 110000.0
                ),
                ProductEntity(
                    sku = "RB-003",
                    name = "Aceh Gayo Winey 250g",
                    categoryId = "roasted_beans",
                    basePrice = 110000.0,
                    costPrice = 65000.0,
                    stockQuantity = 30,
                    unit = "pack",
                    description = "Natural Anaerobic Fermentation. Tasting notes: Winey, Raisin, Dark Cherry.",
                    iconName = "grain",
                    isActive = true,
                    hasB2bPricing = true,
                    b2bTier1MinQty = 5,
                    b2bTier1Price = 98000.0,
                    b2bTier2MinQty = 10,
                    b2bTier2Price = 88000.0
                ),

                // Pastry & Bakery
                ProductEntity(
                    sku = "PS-001",
                    name = "French Butter Croissant",
                    categoryId = "pastry",
                    basePrice = 24000.0,
                    costPrice = 10000.0,
                    stockQuantity = 25,
                    unit = "pcs",
                    description = "Croissant renyah berlapis dengan aroma French AOP Butter",
                    iconName = "bakery_dining",
                    isActive = true
                ),
                ProductEntity(
                    sku = "PS-002",
                    name = "Pain au Chocolat",
                    categoryId = "pastry",
                    basePrice = 28000.0,
                    costPrice = 12000.0,
                    stockQuantity = 20,
                    unit = "pcs",
                    description = "Pastry mentega renyah dengan isi batang dark chocolate ganda",
                    iconName = "bakery_dining",
                    isActive = true
                ),
                ProductEntity(
                    sku = "PS-003",
                    name = "Cinnamon Swirl Roll",
                    categoryId = "pastry",
                    basePrice = 26000.0,
                    costPrice = 11000.0,
                    stockQuantity = 18,
                    unit = "pcs",
                    description = "Roti gulung kayu manis harum dengan glaze cream cheese",
                    iconName = "bakery_dining",
                    isActive = true
                ),
                ProductEntity(
                    sku = "PS-004",
                    name = "Almond Croissant",
                    categoryId = "pastry",
                    basePrice = 32000.0,
                    costPrice = 14000.0,
                    stockQuantity = 15,
                    unit = "pcs",
                    description = "Double-baked croissant dengan isian pasta almond dan taburan almond slice",
                    iconName = "bakery_dining",
                    isActive = true
                ),

                // Bahan Baku (WITH B2B TIERED PRICING)
                ProductEntity(
                    sku = "BB-001",
                    name = "Fresh Milk Pasteurisasi 1L",
                    categoryId = "bahan_baku",
                    basePrice = 22000.0,
                    costPrice = 17000.0,
                    stockQuantity = 50,
                    unit = "liter",
                    description = "Susu segar pasteurisasi standard barista coffee shop",
                    iconName = "inventory_2",
                    isActive = true,
                    hasB2bPricing = true,
                    b2bTier1MinQty = 10,
                    b2bTier1Price = 20000.0,
                    b2bTier2MinQty = 24,
                    b2bTier2Price = 18500.0
                ),
                ProductEntity(
                    sku = "BB-002",
                    name = "Sirup Gula Aren Organik 1L",
                    categoryId = "bahan_baku",
                    basePrice = 45000.0,
                    costPrice = 30000.0,
                    stockQuantity = 30,
                    unit = "botol",
                    description = "Gula aren cair asli tanpa pengawet kemasan 1 Liter",
                    iconName = "inventory_2",
                    isActive = true,
                    hasB2bPricing = true,
                    b2bTier1MinQty = 6,
                    b2bTier1Price = 40000.0,
                    b2bTier2MinQty = 12,
                    b2bTier2Price = 36000.0
                )
            )
            dao.insertProducts(initialProducts)
        }

        // 4. Seed Initial Completed Orders if empty (so dashboard, history & shift reports are ready)
        if (dao.getOrderCount() == 0) {
            val sampleOrders = listOf(
                // Oct 29, 2026 (Today) Order 1
                OrderEntity(
                    invoiceNumber = "INV-20261029-142010",
                    timestamp = com.example.ui.components.DateTimeUtils.getOrderEffectiveTimestamp("INV-20261029-142010", 0),
                    customerName = "Kak Maya / Takeaway",
                    orderType = "TAKEAWAY",
                    subtotal = 58000.0,
                    taxPercentage = 10.0,
                    taxAmount = 5800.0,
                    grandTotal = 63800.0,
                    paymentMethod = "CASH",
                    tenderedAmount = 100000.0,
                    changeAmount = 36200.0,
                    paymentStatus = "PAID",
                    cashierName = "Maya Sari",
                    shiftName = "Shift 2",
                    notes = "Less ice"
                ),
                // Oct 29, 2026 (Today) Order 2 (B2B order)
                OrderEntity(
                    invoiceNumber = "INV-20261029-113045",
                    timestamp = com.example.ui.components.DateTimeUtils.getOrderEffectiveTimestamp("INV-20261029-113045", 0),
                    customerName = "PT Kopi Mandiri [B2B]",
                    orderType = "TAKEAWAY",
                    subtotal = 510000.0,
                    taxPercentage = 10.0,
                    taxAmount = 51000.0,
                    grandTotal = 561000.0,
                    paymentMethod = "BANK_TRANSFER",
                    tenderedAmount = 561000.0,
                    changeAmount = 0.0,
                    paymentStatus = "PAID",
                    cashierName = "Rian Pratama",
                    shiftName = "Shift 1",
                    notes = "Invoice B2B pesanan 6 pack beans Tier 1"
                ),
                // Oct 28, 2026 (Yesterday) Order
                OrderEntity(
                    invoiceNumber = "INV-20261028-161520",
                    timestamp = com.example.ui.components.DateTimeUtils.getOrderEffectiveTimestamp("INV-20261028-161520", 0),
                    customerName = "Kak Rudi (Meja 02)",
                    orderType = "DINE_IN",
                    subtotal = 80000.0,
                    taxPercentage = 10.0,
                    taxAmount = 8000.0,
                    grandTotal = 88000.0,
                    paymentMethod = "QRIS",
                    tenderedAmount = 88000.0,
                    changeAmount = 0.0,
                    paymentStatus = "PAID",
                    cashierName = "Rian Pratama",
                    shiftName = "Shift 1",
                    notes = "Gula aren dipisah"
                ),
                // Oct 27, 2026 (2 Days Ago) Order
                OrderEntity(
                    invoiceNumber = "INV-20261027-091520",
                    timestamp = com.example.ui.components.DateTimeUtils.getOrderEffectiveTimestamp("INV-20261027-091520", 0),
                    customerName = "Mas Dimas / Takeaway",
                    orderType = "TAKEAWAY",
                    subtotal = 34000.0,
                    taxPercentage = 10.0,
                    taxAmount = 3400.0,
                    grandTotal = 37400.0,
                    paymentMethod = "CASH",
                    tenderedAmount = 50000.0,
                    changeAmount = 12600.0,
                    paymentStatus = "PAID",
                    cashierName = "Rian Pratama",
                    shiftName = "Shift 2",
                    notes = "Extra hot"
                )
            )
            dao.insertOrders(sampleOrders)

            val sampleOrderItems = listOf(
                // Items for INV-20261029-142010 (Oct 29)
                OrderItemEntity(
                    orderInvoiceNumber = "INV-20261029-142010",
                    productId = 7L,
                    productName = "Matcha Fusion Latte",
                    sku = "NC-001",
                    basePrice = 32000.0,
                    costPrice = 12500.0,
                    variantSummary = "Iced, Normal 100%",
                    unitPrice = 32000.0,
                    quantity = 1,
                    lineTotal = 32000.0,
                    categoryName = "Non-Coffee"
                ),
                OrderItemEntity(
                    orderInvoiceNumber = "INV-20261029-142010",
                    productId = 16L,
                    productName = "Cinnamon Swirl Roll",
                    sku = "PS-003",
                    basePrice = 26000.0,
                    costPrice = 11000.0,
                    variantSummary = "Standard",
                    unitPrice = 26000.0,
                    quantity = 1,
                    lineTotal = 26000.0,
                    categoryName = "Pastry & Bakery"
                ),

                // Items for INV-20261029-113045 (Oct 29 B2B Tier 1 order)
                OrderItemEntity(
                    orderInvoiceNumber = "INV-20261029-113045",
                    productId = 10L,
                    productName = "House Blend Beans 250g [B2B]",
                    sku = "RB-001",
                    basePrice = 85000.0,
                    costPrice = 55000.0,
                    variantSummary = "Whole Bean, B2B Tier 1 (≥5)",
                    unitPrice = 85000.0,
                    quantity = 6,
                    lineTotal = 510000.0,
                    categoryName = "Roasted Beans"
                ),

                // Items for INV-20261028-161520 (Oct 28)
                OrderItemEntity(
                    orderInvoiceNumber = "INV-20261028-161520",
                    productId = 1L,
                    productName = "BDS Aren Latte",
                    sku = "CF-001",
                    basePrice = 28000.0,
                    costPrice = 11000.0,
                    variantSummary = "Iced, Less Sugar 50%",
                    unitPrice = 28000.0,
                    quantity = 2,
                    lineTotal = 56000.0,
                    categoryName = "Coffee"
                ),
                OrderItemEntity(
                    orderInvoiceNumber = "INV-20261028-161520",
                    productId = 14L,
                    productName = "French Butter Croissant",
                    sku = "PS-001",
                    basePrice = 24000.0,
                    costPrice = 10000.0,
                    variantSummary = "Standard",
                    unitPrice = 24000.0,
                    quantity = 1,
                    lineTotal = 24000.0,
                    categoryName = "Pastry & Bakery"
                ),

                // Items for INV-20261027-091520 (Oct 27)
                OrderItemEntity(
                    orderInvoiceNumber = "INV-20261027-091520",
                    productId = 5L,
                    productName = "Caramel Macchiato",
                    sku = "CF-005",
                    basePrice = 34000.0,
                    costPrice = 13000.0,
                    variantSummary = "Hot, Normal 100%",
                    unitPrice = 34000.0,
                    quantity = 1,
                    lineTotal = 34000.0,
                    categoryName = "Coffee"
                )
            )
            dao.insertOrderItems(sampleOrderItems)
        } else {
            // 5. Sanitize any existing orders whose stored timestamp doesn't match their encoded invoice date
            try {
                val existingOrders = dao.getAllOrdersSync()
                val ordersToFix = existingOrders.filter { order ->
                    com.example.ui.components.DateTimeUtils.hasDateEncodedInInvoice(order.invoiceNumber) &&
                        order.timestamp != com.example.ui.components.DateTimeUtils.getOrderEffectiveTimestamp(order)
                }
                if (ordersToFix.isNotEmpty()) {
                    val fixed = ordersToFix.map {
                        it.copy(timestamp = com.example.ui.components.DateTimeUtils.getOrderEffectiveTimestamp(it))
                    }
                    dao.updateOrders(fixed)
                }
            } catch (_: Exception) {}
        }
    }
}
