package com.example.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "open_bills")
data class OpenBillEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val billNumber: String,
    val customerName: String = "",
    val tableNumber: String = "",
    val orderType: String = "DINE_IN", // DINE_IN or TAKEAWAY
    val itemsJson: String, // JSON array of cart items
    val totalItemCount: Int = 0,
    val subtotal: Double = 0.0,
    val taxAmount: Double = 0.0,
    val serviceFeeAmount: Double = 0.0,
    val grandTotal: Double = 0.0,
    val notes: String = "",
    val cashierName: String = "Rian Pratama",
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "ACTIVE" // ACTIVE, COMPLETED, CANCELLED
)
