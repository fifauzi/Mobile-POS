package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.PosDao
import com.example.data.local.model.CategoryEntity
import com.example.data.local.model.OrderEntity
import com.example.data.local.model.OrderItemEntity
import com.example.data.local.model.ProductEntity
import com.example.data.local.model.VariantGroupEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CategoryEntity::class,
        ProductEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        VariantGroupEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun posDao(): PosDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "caffeinery_pos.db"
                )
                    .addCallback(AppDatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class AppDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.posDao())
                    }
                }
            }

            private suspend fun populateInitialData(dao: PosDao) {
                val categories = listOf(
                    CategoryEntity("coffee", "Coffee", "coffee", "#7C4A27", 1),
                    CategoryEntity("non_coffee", "Non-Coffee", "local_cafe", "#2563EB", 2),
                    CategoryEntity("roasted_beans", "Roasted Beans", "grain", "#92400E", 3),
                    CategoryEntity("pastry", "Pastry & Bakery", "bakery_dining", "#D97706", 4),
                    CategoryEntity("bahan_baku", "Bahan Baku", "inventory_2", "#059669", 5)
                )
                dao.insertCategories(categories)

                // Seed Global Variant Groups (Matching Reference & Strict Requirements)
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

                val defaultCoffeeVariants = """[
                    {"name":"Size","isMultiSelect":false,"options":[{"name":"Regular","price":0.0},{"name":"Large","price":5000.0}]},
                    {"name":"Type","isMultiSelect":false,"options":[{"name":"Iced","price":0.0},{"name":"Hot","price":0.0}]},
                    {"name":"Beans Type","isMultiSelect":false,"options":[{"name":"House Blend (Brazil/Sumatra)","price":0.0},{"name":"Single Origin Ethiopia","price":8000.0},{"name":"Swiss Decaf","price":5000.0}]},
                    {"name":"Sugar Level","isMultiSelect":false,"options":[{"name":"Normal 100%","price":0.0},{"name":"Less Sugar 50%","price":0.0},{"name":"No Sugar 0%","price":0.0}]},
                    {"name":"Add-ons","isMultiSelect":true,"options":[{"name":"Extra Espresso Shot","price":6000.0},{"name":"Oat Milk Sub","price":8000.0}]}
                ]""".trimIndent()

                val products = listOf(
                    ProductEntity(
                        sku = "CF-001",
                        name = "Spanish Latte",
                        categoryId = "coffee",
                        basePrice = 35000.0,
                        costPrice = 14000.0,
                        stockQuantity = 85,
                        unit = "cup",
                        description = "Espresso with sweet textured condensed milk and fresh milk",
                        iconName = "coffee",
                        variantsJson = defaultCoffeeVariants
                    ),
                    ProductEntity(
                        sku = "CF-002",
                        name = "Caramel Macchiato",
                        categoryId = "coffee",
                        basePrice = 38000.0,
                        costPrice = 16000.0,
                        stockQuantity = 64,
                        unit = "cup",
                        description = "Layered espresso with vanilla syrup, steamed milk and artisan caramel drizzle",
                        iconName = "coffee",
                        variantsJson = defaultCoffeeVariants
                    ),
                    ProductEntity(
                        sku = "CF-003",
                        name = "Espresso Romano",
                        categoryId = "coffee",
                        basePrice = 25000.0,
                        costPrice = 8000.0,
                        stockQuantity = 90,
                        unit = "cup",
                        description = "Classic double ristretto espresso served with fresh lemon peel garnish",
                        iconName = "coffee",
                        variantsJson = defaultCoffeeVariants
                    ),
                    ProductEntity(
                        sku = "CF-004",
                        name = "V60 Hand Drip Pour Over",
                        categoryId = "coffee",
                        basePrice = 34000.0,
                        costPrice = 12000.0,
                        stockQuantity = 50,
                        unit = "cup",
                        description = "Clean floral pour-over with balanced acidity and crisp finish",
                        iconName = "coffee",
                        variantsJson = defaultCoffeeVariants
                    ),
                    ProductEntity(
                        sku = "NC-001",
                        name = "Matcha Oat Latte",
                        categoryId = "non_coffee",
                        basePrice = 36000.0,
                        costPrice = 15000.0,
                        stockQuantity = 45,
                        unit = "cup",
                        description = "Ceremonial grade Uji matcha with velvety barista oat milk",
                        iconName = "local_cafe",
                        variantsJson = """[
                            {"name":"Size","isMultiSelect":false,"options":[{"name":"Regular","price":0.0},{"name":"Large","price":5000.0}]},
                            {"name":"Type","isMultiSelect":false,"options":[{"name":"Iced","price":0.0},{"name":"Hot","price":0.0}]},
                            {"name":"Sugar Level","isMultiSelect":false,"options":[{"name":"Normal 100%","price":0.0},{"name":"Less Sugar 50%","price":0.0},{"name":"No Sugar 0%","price":0.0}]}
                        ]""".trimIndent()
                    ),
                    ProductEntity(
                        sku = "NC-002",
                        name = "Dark Belgian Chocolate",
                        categoryId = "non_coffee",
                        basePrice = 34000.0,
                        costPrice = 13000.0,
                        stockQuantity = 40,
                        unit = "cup",
                        description = "Rich 70% dark Belgian cocoa blended with steamed whole milk",
                        iconName = "local_cafe",
                        variantsJson = """[
                            {"name":"Size","isMultiSelect":false,"options":[{"name":"Regular","price":0.0},{"name":"Large","price":5000.0}]},
                            {"name":"Type","isMultiSelect":false,"options":[{"name":"Iced","price":0.0},{"name":"Hot","price":0.0}]}
                        ]""".trimIndent()
                    ),
                    ProductEntity(
                        sku = "RB-001",
                        name = "Ethiopia Yirgacheffe G1 250g",
                        categoryId = "roasted_beans",
                        basePrice = 125000.0,
                        costPrice = 70000.0,
                        stockQuantity = 22,
                        unit = "bag",
                        description = "Washed process. Notes of Jasmine, Bergamot, and Peach tea.",
                        iconName = "grain",
                        variantsJson = """[
                            {"name":"Grind Size","isMultiSelect":false,"options":[{"name":"Whole Bean","price":0.0},{"name":"Espresso (Fine)","price":0.0},{"name":"V60 / Medium Filter","price":0.0},{"name":"Cold Brew / Coarse","price":0.0}]}
                        ]""".trimIndent()
                    ),
                    ProductEntity(
                        sku = "RB-002",
                        name = "Aceh Gayo Wine Process 250g",
                        categoryId = "roasted_beans",
                        basePrice = 115000.0,
                        costPrice = 65000.0,
                        stockQuantity = 18,
                        unit = "bag",
                        description = "Fermented anaerobic natural. Intense grape, dark rum, and cacao.",
                        iconName = "grain",
                        variantsJson = """[
                            {"name":"Grind Size","isMultiSelect":false,"options":[{"name":"Whole Bean","price":0.0},{"name":"Espresso (Fine)","price":0.0},{"name":"V60 / Medium Filter","price":0.0},{"name":"Cold Brew / Coarse","price":0.0}]}
                        ]""".trimIndent()
                    ),
                    ProductEntity(
                        sku = "PS-001",
                        name = "Butter Croissant",
                        categoryId = "pastry",
                        basePrice = 28000.0,
                        costPrice = 10000.0,
                        stockQuantity = 32,
                        unit = "pcs",
                        description = "French cultured butter flaky croissant, freshly baked daily",
                        iconName = "bakery_dining",
                        variantsJson = """[
                            {"name":"Serving","isMultiSelect":false,"options":[{"name":"Warmed Up","price":0.0},{"name":"Standard","price":0.0}]}
                        ]""".trimIndent()
                    ),
                    ProductEntity(
                        sku = "PS-002",
                        name = "Pain Au Chocolat",
                        categoryId = "pastry",
                        basePrice = 32000.0,
                        costPrice = 12000.0,
                        stockQuantity = 26,
                        unit = "pcs",
                        description = "Double chocolate batons in golden laminated puff pastry",
                        iconName = "bakery_dining",
                        variantsJson = """[
                            {"name":"Serving","isMultiSelect":false,"options":[{"name":"Warmed Up","price":0.0},{"name":"Standard","price":0.0}]}
                        ]""".trimIndent()
                    ),
                    ProductEntity(
                        sku = "BB-001",
                        name = "Fresh Milk Pasteurized 1L",
                        categoryId = "bahan_baku",
                        basePrice = 24000.0,
                        costPrice = 18000.0,
                        stockQuantity = 48,
                        unit = "bottle",
                        description = "Raw material dairy supply for barista station",
                        iconName = "inventory_2",
                        variantsJson = ""
                    ),
                    ProductEntity(
                        sku = "BB-002",
                        name = "Oat Milk Barista Edition 1L",
                        categoryId = "bahan_baku",
                        basePrice = 45000.0,
                        costPrice = 32000.0,
                        stockQuantity = 30,
                        unit = "bottle",
                        description = "Non-dairy micro-foam milk stock",
                        iconName = "inventory_2",
                        variantsJson = ""
                    )
                )
                dao.insertProducts(products)

                // Add 2 initial completed orders for immediate analytics & P&L demonstration
                val now = System.currentTimeMillis()
                val order1 = OrderEntity(
                    invoiceNumber = "INV-${System.currentTimeMillis() - 7200000}",
                    timestamp = now - 7200000,
                    customerName = "Budi Santoso",
                    subtotal = 73000.0,
                    itemDiscountTotal = 0.0,
                    orderDiscountPercentage = 0.0,
                    orderDiscountAmount = 0.0,
                    taxPercentage = 10.0,
                    taxAmount = 7300.0,
                    grandTotal = 80300.0,
                    paymentMethod = "QRIS",
                    tenderedAmount = 80300.0,
                    changeAmount = 0.0,
                    paymentStatus = "PAID",
                    notes = "Dine-in Table 4"
                )
                val order2 = OrderEntity(
                    invoiceNumber = "INV-${System.currentTimeMillis() - 3600000}",
                    timestamp = now - 3600000,
                    customerName = "Sarah L.",
                    subtotal = 67000.0,
                    itemDiscountTotal = 5000.0,
                    orderDiscountPercentage = 0.0,
                    orderDiscountAmount = 0.0,
                    taxPercentage = 10.0,
                    taxAmount = 6200.0,
                    grandTotal = 68200.0,
                    paymentMethod = "CASH",
                    tenderedAmount = 100000.0,
                    changeAmount = 31800.0,
                    paymentStatus = "PAID",
                    notes = "Takeaway"
                )
                dao.insertOrders(listOf(order1, order2))

                val item1 = OrderItemEntity(
                    orderInvoiceNumber = order1.invoiceNumber,
                    productId = 1,
                    productName = "Spanish Latte",
                    sku = "CF-001",
                    basePrice = 35000.0,
                    costPrice = 14000.0,
                    variantSummary = "Large, Iced, Less Sugar",
                    unitPrice = 40000.0,
                    quantity = 1,
                    itemDiscount = 0.0,
                    lineTotal = 40000.0
                )
                val item2 = OrderItemEntity(
                    orderInvoiceNumber = order1.invoiceNumber,
                    productId = 2,
                    productName = "Caramel Macchiato",
                    sku = "CF-002",
                    basePrice = 38000.0,
                    costPrice = 16000.0,
                    variantSummary = "Regular, Hot",
                    unitPrice = 38000.0,
                    quantity = 1,
                    itemDiscount = 5000.0,
                    lineTotal = 33000.0
                )
                val item3 = OrderItemEntity(
                    orderInvoiceNumber = order2.invoiceNumber,
                    productId = 10,
                    productName = "Pain Au Chocolat",
                    sku = "PS-002",
                    basePrice = 32000.0,
                    costPrice = 12000.0,
                    variantSummary = "Warmed Up",
                    unitPrice = 32000.0,
                    quantity = 1,
                    itemDiscount = 0.0,
                    lineTotal = 32000.0
                )
                val item4 = OrderItemEntity(
                    orderInvoiceNumber = order2.invoiceNumber,
                    productId = 1,
                    productName = "Spanish Latte",
                    sku = "CF-001",
                    basePrice = 35000.0,
                    costPrice = 14000.0,
                    variantSummary = "Regular, Iced",
                    unitPrice = 35000.0,
                    quantity = 1,
                    itemDiscount = 0.0,
                    lineTotal = 35000.0
                )
                dao.insertOrderItems(listOf(item1, item2, item3, item4))
            }
        }
    }
}
