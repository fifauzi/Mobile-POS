package com.example.service

import com.example.data.local.model.OrderItemEntity
import com.example.domain.model.CartItem
import com.example.domain.model.VariantOption
import com.example.ui.components.CurrencyUtils

data class ParsedVariantAttributes(
    val size: String = "-",
    val type: String = "-",
    val beansType: String = "-",
    val grindSize: String = "-",
    val sugarLevel: String = "-",
    val addOns: String = "-",
    val extraPriceTotal: Double = 0.0
)

/**
 * Robust Variant Data Mapping Engine:
 * Accurately extracts Size, Type, Beans Type, Grind Size, Sugar Level, Add-ons,
 * and Extra Price Total from CartItems, selected variant maps, or summary strings.
 * Ensures CSV exports and transaction logs never drop variant attributes.
 */
object VariantDataMapper {

    fun extractFromCartItem(cartItem: CartItem): ParsedVariantAttributes {
        val map = cartItem.selectedVariants
        val summary = cartItem.variantSummary

        var size = "-"
        var type = "-"
        var beans = "-"
        var grind = "-"
        var sugar = "-"
        val addonsList = mutableListOf<String>()
        var extraPrice = cartItem.variantPriceDelta

        // 1. Extract from selectedVariants Map
        for ((groupKey, options) in map) {
            val nonBlankOpts = options.filter { it.name.isNotBlank() }
            if (nonBlankOpts.isEmpty()) continue

            val keyLower = groupKey.lowercase().trim()
            val optStrings = nonBlankOpts.map { opt ->
                if (opt.priceAdjustment > 0) "${opt.name} (+${CurrencyUtils.formatIdr(opt.priceAdjustment)})" else opt.name
            }
            val formatted = optStrings.joinToString(", ")

            when {
                // 1. Grind Size (must check before Size because "Grind Size" contains "size")
                keyLower.contains("grind") || keyLower.contains("giling") || keyLower.contains("bubuk") || keyLower.contains("kehalusan") -> {
                    grind = formatted
                }
                // 2. Beans Type (must check before Type because "Beans Type" contains "type")
                keyLower.contains("bean") || keyLower.contains("biji") || keyLower.contains("blend") || keyLower.contains("origin") -> {
                    beans = formatted
                }
                // 3. Size (Ukuran / Cup / Porsi) - strictly not grind size
                !keyLower.contains("grind") && (keyLower.contains("size") || keyLower.contains("ukuran") || keyLower.contains("cup") || keyLower.contains("porsi") || keyLower.contains("takaran")) -> {
                    size = formatted
                }
                // 4. Type / Temperature (Tipe / Suhu / Penyajian) - strictly not beans type
                !keyLower.contains("bean") && !keyLower.contains("biji") && (keyLower.contains("type") || keyLower.contains("tipe") || keyLower.contains("suhu") || keyLower.contains("penyajian") || keyLower.contains("serving") || keyLower.contains("temperature")) -> {
                    type = formatted
                }
                // 5. Sugar Level (Tingkat Gula / Manis)
                keyLower.contains("sugar") || keyLower.contains("gula") || keyLower.contains("manis") || keyLower.contains("sweet") -> {
                    sugar = formatted
                }
                // 6. Add-ons / Topping / Tambahan / Ekstra
                keyLower.contains("add") || keyLower.contains("topping") || keyLower.contains("tambahan") || keyLower.contains("extra") || keyLower.contains("ekstra") -> {
                    addonsList.add(formatted)
                }
                else -> {
                    // Semantic detection based on option names
                    val firstOptName = nonBlankOpts.first().name.lowercase()
                    when {
                        firstOptName.contains("whole bean") || firstOptName.contains("espresso") || firstOptName.contains("coarse") || firstOptName.contains("fine") || firstOptName.contains("v60") -> {
                            if (grind == "-") grind = formatted else addonsList.add(formatted)
                        }
                        firstOptName.contains("house blend") || firstOptName.contains("ethiopia") || firstOptName.contains("decaf") || firstOptName.contains("arabica") || firstOptName.contains("robusta") -> {
                            if (beans == "-") beans = formatted else addonsList.add(formatted)
                        }
                        firstOptName.contains("regular") || firstOptName.contains("large") || firstOptName.contains("small") || firstOptName.contains("medium") || firstOptName.contains("venti") || firstOptName.contains("grande") || firstOptName.contains("tall") -> {
                            if (size == "-") size = formatted else addonsList.add(formatted)
                        }
                        firstOptName.contains("iced") || firstOptName.contains("hot") || firstOptName.contains("dingin") || firstOptName.contains("panas") -> {
                            if (type == "-") type = formatted else addonsList.add(formatted)
                        }
                        firstOptName.contains("normal") || firstOptName.contains("less sugar") || firstOptName.contains("no sugar") || firstOptName.contains("%") -> {
                            if (sugar == "-") sugar = formatted else addonsList.add(formatted)
                        }
                        else -> {
                            addonsList.add(formatted)
                        }
                    }
                }
            }
        }

        // 2. Fallback check into summary string if any primary field is missing
        if (summary.isNotBlank() && !summary.equals("Standard", ignoreCase = true)) {
            val fromSummary = parseFromSummaryString(summary)
            if (size == "-") size = fromSummary.size
            if (type == "-") type = fromSummary.type
            if (beans == "-") beans = fromSummary.beansType
            if (grind == "-") grind = fromSummary.grindSize
            if (sugar == "-") sugar = fromSummary.sugarLevel
            if (addonsList.isEmpty() && fromSummary.addOns != "-") {
                addonsList.add(fromSummary.addOns)
            }
            if (extraPrice == 0.0 && fromSummary.extraPriceTotal > 0.0) {
                extraPrice = fromSummary.extraPriceTotal
            }
        }

        val addons = if (addonsList.isNotEmpty()) addonsList.joinToString(", ") else "-"

        return ParsedVariantAttributes(
            size = size,
            type = type,
            beansType = beans,
            grindSize = grind,
            sugarLevel = sugar,
            addOns = addons,
            extraPriceTotal = extraPrice
        )
    }

    /**
     * Resolves all variant attributes from an existing OrderItemEntity.
     * If individual columns are already populated, uses them; otherwise falls back
     * to parsing from variantSummary and calculating extraPriceTotal accurately.
     */
    fun resolveOrderItemVariants(item: OrderItemEntity): ParsedVariantAttributes {
        val sizeFromItem = item.variantSize.trim().takeIf { it.isNotBlank() && it != "-" }
        val typeFromItem = item.variantType.trim().takeIf { it.isNotBlank() && it != "-" }
        val beansFromItem = item.variantBeans.trim().takeIf { it.isNotBlank() && it != "-" }
        val grindFromItem = item.variantGrind.trim().takeIf { it.isNotBlank() && it != "-" }
        val sugarFromItem = item.variantSugar.trim().takeIf { it.isNotBlank() && it != "-" }
        val addonsFromItem = item.variantAddons.trim().takeIf { it.isNotBlank() && it != "-" }
        var extraTotal = item.variantExtraTotal

        val parsedSummary = parseFromSummaryString(item.variantSummary)

        val finalSize = sizeFromItem ?: parsedSummary.size
        val finalType = typeFromItem ?: parsedSummary.type
        val finalBeans = beansFromItem ?: parsedSummary.beansType
        val finalGrind = grindFromItem ?: parsedSummary.grindSize
        val finalSugar = sugarFromItem ?: parsedSummary.sugarLevel
        val finalAddons = addonsFromItem ?: parsedSummary.addOns

        if (extraTotal == 0.0) {
            extraTotal = if (parsedSummary.extraPriceTotal > 0.0) {
                parsedSummary.extraPriceTotal
            } else {
                (item.unitPrice - item.basePrice).coerceAtLeast(0.0)
            }
        }

        return ParsedVariantAttributes(
            size = finalSize.ifBlank { "-" },
            type = finalType.ifBlank { "-" },
            beansType = finalBeans.ifBlank { "-" },
            grindSize = finalGrind.ifBlank { "-" },
            sugarLevel = finalSugar.ifBlank { "-" },
            addOns = finalAddons.ifBlank { "-" },
            extraPriceTotal = extraTotal
        )
    }

    /**
     * Intelligent parser that reconstructs variant attributes from formatted summary strings
     * such as "Large (+Rp 5.000), Iced, Less Sugar 50%, Extra Espresso Shot (+Rp 6.000)".
     */
    fun parseFromSummaryString(summary: String): ParsedVariantAttributes {
        if (summary.isBlank() || summary.equals("Standard", ignoreCase = true)) {
            return ParsedVariantAttributes()
        }

        val tokens = summary.split(",").map { it.trim() }.filter { it.isNotBlank() }
        var size = "-"
        var type = "-"
        var beans = "-"
        var grind = "-"
        var sugar = "-"
        val addons = mutableListOf<String>()
        var extraPrice = 0.0

        for (token in tokens) {
            val lower = token.lowercase()

            // Calculate extra price if token contains (+Rp ...)
            if (token.contains("(+Rp", ignoreCase = true)) {
                try {
                    val priceStr = token.substringAfter("(+Rp")
                        .substringBefore(")")
                        .replace(".", "")
                        .replace(",", "")
                        .replace(" ", "")
                        .trim()
                    val p = priceStr.toDoubleOrNull() ?: 0.0
                    extraPrice += p
                } catch (_: Exception) {}
            }

            // Skip B2B badges from variant classification
            if (lower.contains("b2b")) {
                continue
            }

            when {
                // Size
                (lower.contains("large") || lower.contains("regular") || lower.contains("small") ||
                        lower.contains("medium") || lower.contains("tall") || lower.contains("grande") ||
                        lower.contains("venti") || lower.contains("single") || lower.contains("double") ||
                        lower.contains("12oz") || lower.contains("16oz") || lower.contains("250g") || lower.contains("500g") || lower.contains("1kg")) &&
                        !lower.contains("beans") && !lower.contains("house blend") -> {
                    if (size == "-") size = token else addons.add(token)
                }
                // Type / Temperature
                lower.contains("iced") || lower.contains("hot") || lower.contains("dingin") ||
                        lower.contains("panas") || lower.contains("ice") || lower.contains("warm") -> {
                    if (type == "-") type = token else addons.add(token)
                }
                // Beans Type vs Grind Size
                lower.contains("whole bean") || lower.contains("fine") || lower.contains("coarse") ||
                        lower.contains("v60") || lower.contains("tubruk") ||
                        lower.contains("french press") || lower.contains("cold brew") -> {
                    if (grind == "-") grind = token else addons.add(token)
                }
                lower.contains("house blend") || lower.contains("ethiopia") || lower.contains("decaf") ||
                        lower.contains("sumatra") || lower.contains("brazil") || lower.contains("arabica") ||
                        lower.contains("robusta") || lower.contains("toraja") || lower.contains("gayo") ||
                        lower.contains("single origin") || lower.contains("mandheling") || lower.contains("blend") -> {
                    if (beans == "-") beans = token else addons.add(token)
                }
                lower.contains("grind") || lower.contains("giling") || lower.contains("filter") -> {
                    if (grind == "-") grind = token else addons.add(token)
                }
                lower.contains("bean") || lower.contains("biji") -> {
                    if (beans == "-") beans = token else addons.add(token)
                }
                // Sugar Level
                lower.contains("normal") || lower.contains("less sugar") || lower.contains("no sugar") ||
                        lower.contains("gula") || lower.contains("sugar") || lower.contains("%") -> {
                    if (sugar == "-") sugar = token else addons.add(token)
                }
                // Add-ons / Toppings
                else -> {
                    addons.add(token)
                }
            }
        }

        return ParsedVariantAttributes(
            size = size,
            type = type,
            beansType = beans,
            grindSize = grind,
            sugarLevel = sugar,
            addOns = if (addons.isNotEmpty()) addons.joinToString(", ") else "-",
            extraPriceTotal = extraPrice
        )
    }
}
