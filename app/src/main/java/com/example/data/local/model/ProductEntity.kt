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
    val isActive: Boolean = true,
    val hasB2bPricing: Boolean = false,
    val b2bTier1MinQty: Int = 5,
    val b2bTier1Price: Double = 0.0,
    val b2bTier2MinQty: Int = 10,
    val b2bTier2Price: Double = 0.0
) {
    fun getUnitPriceForQuantity(qty: Int): Double {
        if (!hasB2bPricing) return basePrice
        return when {
            b2bTier2Price > 0 && b2bTier2MinQty > 0 && qty >= b2bTier2MinQty -> b2bTier2Price
            b2bTier1Price > 0 && b2bTier1MinQty > 0 && qty >= b2bTier1MinQty -> b2bTier1Price
            else -> basePrice
        }
    }

    fun isB2bTierApplied(qty: Int): Boolean {
        if (!hasB2bPricing) return false
        return (b2bTier2Price > 0 && b2bTier2MinQty > 0 && qty >= b2bTier2MinQty) ||
                (b2bTier1Price > 0 && b2bTier1MinQty > 0 && qty >= b2bTier1MinQty)
    }

    fun getB2bTierLabel(qty: Int): String? {
        if (!hasB2bPricing) return null
        return when {
            b2bTier2Price > 0 && b2bTier2MinQty > 0 && qty >= b2bTier2MinQty -> "B2B Tier 2 (≥$b2bTier2MinQty)"
            b2bTier1Price > 0 && b2bTier1MinQty > 0 && qty >= b2bTier1MinQty -> "B2B Tier 1 (≥$b2bTier1MinQty)"
            else -> null
        }
    }
}
