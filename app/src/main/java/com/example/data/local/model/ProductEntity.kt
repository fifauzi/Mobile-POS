package com.example.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sku: String,
    val name: String,
    val categoryId: String,
    val basePrice: Double,
    val costPrice: Double = 0.0, // HPP / Modal Awal
    val stockQuantity: Int,
    val unit: String = "cup",
    val description: String = "",
    val iconName: String = "coffee",
    val variantsJson: String = "",
    val isActive: Boolean = true
)
