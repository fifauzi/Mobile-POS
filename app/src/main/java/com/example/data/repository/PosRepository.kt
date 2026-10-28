package com.example.data.repository

import com.example.data.local.dao.PosDao
import com.example.data.local.model.CategoryEntity
import com.example.data.local.model.OrderEntity
import com.example.data.local.model.OrderItemEntity
import com.example.data.local.model.ProductEntity
import com.example.data.local.model.OpenBillEntity
import com.example.data.local.model.VariantGroupEntity
import kotlinx.coroutines.flow.Flow

class PosRepository(private val posDao: PosDao) {

    val categories: Flow<List<CategoryEntity>> = posDao.getAllCategories()
    val products: Flow<List<ProductEntity>> = posDao.getAllProducts()
    val orders: Flow<List<OrderEntity>> = posDao.getAllOrders()
    val allOrderItems: Flow<List<OrderItemEntity>> = posDao.getAllOrderItems()
    val variantGroups: Flow<List<VariantGroupEntity>> = posDao.getAllVariantGroups()
    val activeOpenBills: Flow<List<OpenBillEntity>> = posDao.getActiveOpenBills()

    fun getOrderItems(invoiceNumber: String): Flow<List<OrderItemEntity>> {
        return posDao.getOrderItemsForInvoice(invoiceNumber)
    }

    suspend fun getOrderItemsSync(invoiceNumber: String): List<OrderItemEntity> {
        return posDao.getOrderItemsForInvoiceSync(invoiceNumber)
    }

    suspend fun addCategory(category: CategoryEntity) {
        posDao.insertCategory(category)
    }

    suspend fun updateCategory(category: CategoryEntity) {
        posDao.updateCategory(category)
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        posDao.deleteCategory(category)
    }

    suspend fun addProduct(product: ProductEntity): Long {
        return posDao.insertProduct(product)
    }

    suspend fun updateProduct(product: ProductEntity) {
        posDao.updateProduct(product)
    }

    suspend fun deleteProduct(product: ProductEntity) {
        posDao.deleteProduct(product)
    }

    suspend fun updateStock(productId: Long, newStock: Int) {
        posDao.setStock(productId, newStock)
    }

    suspend fun addVariantGroup(group: VariantGroupEntity) {
        posDao.insertVariantGroup(group)
    }

    suspend fun updateVariantGroup(group: VariantGroupEntity) {
        posDao.updateVariantGroup(group)
    }

    suspend fun deleteVariantGroup(group: VariantGroupEntity) {
        posDao.deleteVariantGroup(group)
    }

    suspend fun completeSale(order: OrderEntity, items: List<OrderItemEntity>) {
        posDao.recordSale(order, items)
    }

    suspend fun saveOpenBill(bill: OpenBillEntity) {
        posDao.insertOpenBill(bill)
    }

    suspend fun deleteOpenBill(id: String) {
        posDao.deleteOpenBillById(id)
    }

    suspend fun getCategoryCount(): Int = posDao.getCategoryCount()
    suspend fun getProductCount(): Int = posDao.getProductCount()
    suspend fun getOrderCount(): Int = posDao.getOrderCount()
    suspend fun getVariantGroupCount(): Int = posDao.getVariantGroupCount()
    suspend fun getAllProductsSync(): List<ProductEntity> = posDao.getAllProductsSync()

    suspend fun insertCategories(categories: List<CategoryEntity>) = posDao.insertCategories(categories)
    suspend fun insertProducts(products: List<ProductEntity>) = posDao.insertProducts(products)
    suspend fun insertVariantGroups(groups: List<VariantGroupEntity>) = posDao.insertVariantGroups(groups)
    suspend fun insertOrders(orders: List<OrderEntity>) = posDao.insertOrders(orders)
    suspend fun insertOrderItems(items: List<OrderItemEntity>) = posDao.insertOrderItems(items)
}
