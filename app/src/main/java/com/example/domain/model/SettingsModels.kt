package com.example.domain.model

import java.util.UUID

data class OutletInfo(
    val name: String = "Caffeinery Specialty Coffee",
    val branch: String = "Senopati Flagship",
    val address: String = "Jl. Senopati No. 42, Kebayoran Baru, Jakarta Selatan",
    val phone: String = "+62 812-9988-7766",
    val taxId: String = "01.234.567.8-012.000",
    val instagram: String = "@caffeinery.pos"
)

data class EmployeeItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val role: String, // Manager, Kasir, Barista
    val pin: String = "1234",
    val isActive: Boolean = true
)

data class OrderMethodsConfig(
    val isDineInEnabled: Boolean = true,
    val isTakeAwayEnabled: Boolean = true,
    val isOnlineDeliveryEnabled: Boolean = true,
    val isTableNumberRequired: Boolean = true
)

data class PaymentGatewaysConfig(
    val isCashEnabled: Boolean = true,
    val isQrisEnabled: Boolean = true,
    val qrisMerchantName: String = "CAFFEINERY SENOPATI",
    val qrisNmid: String = "ID1020039485721",
    val isBankTransferEnabled: Boolean = true,
    val bcaAccount: String = "BCA 827-019-2819 a/n PT Caffeinery Retail",
    val mandiriAccount: String = "Mandiri 137-00-1928374-1 a/n Caffeinery Coffee"
)

data class AdditionalFeesConfig(
    val isTaxEnabled: Boolean = true,
    val taxPercentage: Double = 10.0, // 10% PB1
    val isServiceFeeEnabled: Boolean = false,
    val serviceFeePercentage: Double = 5.0,
    val isRoundingEnabled: Boolean = true
)

data class ReceiptCustomizerConfig(
    val paperSize: String = "58mm", // 58mm or 80mm
    val showLogo: Boolean = true,
    val headerNote: String = "CAFFEINERY SPECIALTY COFFEE",
    val footerNote: String = "Terima kasih atas kunjungan Anda!",
    val printCopies: Int = 1
)
