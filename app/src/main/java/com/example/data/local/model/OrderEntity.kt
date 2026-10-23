package com.example.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val invoiceNumber: String,
    val timestamp: Long,
    val customerName: String = "Walk-in Guest",
    val orderType: String = "DINE_IN", // "DINE_IN" or "TAKEAWAY"
    val subtotal: Double,
    val itemDiscountTotal: Double = 0.0,
    val orderDiscountPercentage: Double = 0.0,
    val orderDiscountAmount: Double = 0.0,
    val serviceFeePercentage: Double = 0.0,
    val serviceFeeAmount: Double = 0.0,
    val taxPercentage: Double = 10.0, // 10% PB1
    val taxAmount: Double = 0.0,
    val grandTotal: Double,
    val paymentMethod: String, // CASH, QRIS, BANK_TRANSFER
    val tenderedAmount: Double = 0.0,
    val changeAmount: Double = 0.0,
    val paymentStatus: String = "PAID", // PAID, REFUNDED
    val cashierName: String = "Rian Pratama",
    val shiftName: String = "Shift 1 (Pagi)",
    val notes: String = ""
)
