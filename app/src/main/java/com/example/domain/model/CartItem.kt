package com.example.domain.model

import com.example.data.local.model.ProductEntity
import com.example.ui.components.CurrencyUtils
import java.util.UUID

enum class DiscountType {
    NONE,
    PERCENTAGE,
    FIXED_IDR
}

data class CartItem(
    val cartItemId: String = UUID.randomUUID().toString(),
    val product: ProductEntity,
    val selectedVariants: Map<String, List<VariantOption>> = emptyMap(),
    val quantity: Int = 1,
    val discountType: DiscountType = DiscountType.NONE,
    val discountValue: Double = 0.0,
    val notes: String = ""
) {
    val variantPriceDelta: Double
        get() = selectedVariants.values.flatten().sumOf { it.priceAdjustment }

    val activeBasePrice: Double
        get() = if (product.hasB2bPricing) product.getUnitPriceForQuantity(quantity) else product.basePrice

    val isB2bPriceApplied: Boolean
        get() = product.hasB2bPricing && product.isB2bTierApplied(quantity)

    val b2bTierLabel: String?
        get() = if (product.hasB2bPricing) product.getB2bTierLabel(quantity) else null

    val baseUnitWithVariants: Double
        get() = activeBasePrice + variantPriceDelta

    val unitDiscountAmount: Double
        get() = when (discountType) {
            DiscountType.PERCENTAGE -> (baseUnitWithVariants * (discountValue / 100.0)).coerceAtLeast(0.0)
            DiscountType.FIXED_IDR -> discountValue.coerceAtMost(baseUnitWithVariants).coerceAtLeast(0.0)
            DiscountType.NONE -> 0.0
        }

    val finalUnitPrice: Double
        get() = (baseUnitWithVariants - unitDiscountAmount).coerceAtLeast(0.0)

    val lineTotal: Double
        get() = finalUnitPrice * quantity

    val totalDiscountAmount: Double
        get() = unitDiscountAmount * quantity

    val variantSummary: String
        get() {
            val items = selectedVariants.values.flatten().map { opt ->
                if (opt.priceAdjustment > 0) {
                    "${opt.name} (+${CurrencyUtils.formatIdr(opt.priceAdjustment)})"
                } else {
                    opt.name
                }
            }
            return if (items.isEmpty()) "Standard" else items.joinToString(", ")
        }
}
