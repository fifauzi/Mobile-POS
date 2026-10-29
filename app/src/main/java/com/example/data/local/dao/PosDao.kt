package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.model.CategoryEntity
import com.example.data.local.model.OrderEntity
import com.example.data.local.model.OrderItemEntity
import com.example.data.local.model.ProductEntity
import com.example.data.local.model.VariantGroupEntity
import com.example.data.local.model.OpenBillEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PosDao {

    // Categories
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int

    // Products
    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE categoryId = :categoryId AND isActive = 1 ORDER BY name ASC")
    fun getProductsByCategory(categoryId: String): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    @Query("UPDATE products SET stockQuantity = stockQuantity - :qty WHERE id = :productId")
    suspend fun decreaseStock(productId: Long, qty: Int)

    @Query("UPDATE products SET stockQuantity = :newStock WHERE id = :productId")
    suspend fun setStock(productId: Long, newStock: Int)

    @Query("SELECT COUNT(*) FROM products")
    suspend fun getProductCount(): Int

    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY name ASC")
    suspend fun getAllProductsSync(): List<ProductEntity>

    // Orders
    @Query("SELECT * FROM orders ORDER BY timestamp DESC")
    fun getAllOrders(): Flow<List<OrderEntity>>

    @Query("SELECT COUNT(*) FROM orders")
    suspend fun getOrderCount(): Int

    @Query("SELECT * FROM orders WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getOrdersBetween(startTime: Long, endTime: Long): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE invoiceNumber = :invoiceNumber LIMIT 1")
    suspend fun getOrderByInvoice(invoiceNumber: String): OrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrders(orders: List<OrderEntity>)

    @Query("SELECT * FROM orders")
    suspend fun getAllOrdersSync(): List<OrderEntity>

    @Update
    suspend fun updateOrders(orders: List<OrderEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<OrderItemEntity>)

    @Query("SELECT * FROM order_items WHERE orderInvoiceNumber = :invoiceNumber")
    fun getOrderItemsForInvoice(invoiceNumber: String): Flow<List<OrderItemEntity>>

    @Query("SELECT * FROM order_items WHERE orderInvoiceNumber = :invoiceNumber")
    suspend fun getOrderItemsForInvoiceSync(invoiceNumber: String): List<OrderItemEntity>

    @Query("SELECT * FROM order_items")
    fun getAllOrderItems(): Flow<List<OrderItemEntity>>

    @Transaction
    suspend fun recordSale(order: OrderEntity, items: List<OrderItemEntity>) {
        insertOrder(order)
        insertOrderItems(items)
        for (item in items) {
            decreaseStock(item.productId, item.quantity)
        }
    }

    // Variant Groups
    @Query("SELECT * FROM variant_groups ORDER BY sortOrder ASC, name ASC")
    fun getAllVariantGroups(): Flow<List<VariantGroupEntity>>

    @Query("SELECT COUNT(*) FROM variant_groups")
    suspend fun getVariantGroupCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVariantGroup(group: VariantGroupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVariantGroups(groups: List<VariantGroupEntity>)

    @Update
    suspend fun updateVariantGroup(group: VariantGroupEntity)

    @Delete
    suspend fun deleteVariantGroup(group: VariantGroupEntity)

    // Open Bills (Hold Orders)
    @Query("SELECT * FROM open_bills WHERE status = 'ACTIVE' ORDER BY createdAt DESC")
    fun getActiveOpenBills(): Flow<List<OpenBillEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOpenBill(openBill: OpenBillEntity)

    @Update
    suspend fun updateOpenBill(openBill: OpenBillEntity)

    @Delete
    suspend fun deleteOpenBill(openBill: OpenBillEntity)

    @Query("DELETE FROM open_bills WHERE id = :id")
    suspend fun deleteOpenBillById(id: String)
}
