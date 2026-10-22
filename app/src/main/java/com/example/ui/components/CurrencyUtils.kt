package com.example.ui.components

import java.util.Locale

object CurrencyUtils {
    fun formatIdr(amount: Double): String {
        return "Rp " + String.format(Locale.GERMAN, "%,.0f", amount)
    }

    fun formatIdrShort(amount: Double): String {
        return when {
            amount >= 1_000_000_000 -> String.format(Locale.US, "%.1fB", amount / 1_000_000_000)
            amount >= 1_000_000 -> String.format(Locale.US, "%.1fM", amount / 1_000_000)
            amount >= 1_000 -> String.format(Locale.US, "%.0fK", amount / 1_000)
            else -> String.format(Locale.US, "%.0f", amount)
        }
    }
}
