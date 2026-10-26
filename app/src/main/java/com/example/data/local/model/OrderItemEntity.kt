package com.example.data.local.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "order_items",
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["invoiceNumber"],
            childColumns = ["orderInvoiceNumber"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["orderInvoiceNumber"])]
)
data class OrderItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderInvoiceNumber: String,
    val productId: Long,
    val productName: String,
    val sku: String,
    val basePrice: Double,
    val costPrice: Double = 0.0, // HPP per unit
    val variantSummary: String = "",
    val unitPrice: Double,
    val quantity: Int,
    val itemDiscount: Double = 0.0,
    val lineTotal: Double,
    val categoryName: String = "",
    val variantSize: String = "",
    val variantType: String = "",
    val variantBeans: String = "",
    val variantGrind: String = "",
    val variantSugar: String = "",
    val variantAddons: String = "",
    val variantExtraTotal: Double = 0.0
)
