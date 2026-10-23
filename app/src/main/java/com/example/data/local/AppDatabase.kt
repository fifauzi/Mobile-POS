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
import com.example.data.local.model.OpenBillEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CategoryEntity::class,
        ProductEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        VariantGroupEntity::class,
        OpenBillEntity::class
    ],
    version = 5,
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
                // Seed Default Ready-to-Use Categories
                val categories = listOf(
                    CategoryEntity("coffee", "Coffee", "coffee", "#7C4A27", 1),
                    CategoryEntity("non_coffee", "Non-Coffee", "local_cafe", "#2563EB", 2),
                    CategoryEntity("roasted_beans", "Roasted Beans", "grain", "#92400E", 3),
                    CategoryEntity("pastry", "Pastry & Bakery", "bakery_dining", "#D97706", 4),
                    CategoryEntity("bahan_baku", "Bahan Baku", "inventory_2", "#059669", 5)
                )
                dao.insertCategories(categories)

                // Seed Default Ready-to-Use Variant Groups & Options Template
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
        }
    }
}
