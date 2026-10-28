package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.PosDao
import com.example.data.local.model.CategoryEntity
import com.example.data.local.model.OpenBillEntity
import com.example.data.local.model.OrderEntity
import com.example.data.local.model.OrderItemEntity
import com.example.data.local.model.ProductEntity
import com.example.data.local.model.VariantGroupEntity
import kotlinx.coroutines.CoroutineScope

@Database(
    entities = [
        CategoryEntity::class,
        ProductEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        VariantGroupEntity::class,
        OpenBillEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun posDao(): PosDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope? = null): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "caffeinery_pos.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
