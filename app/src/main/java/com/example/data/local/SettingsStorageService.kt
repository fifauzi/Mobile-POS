package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.domain.model.AdditionalFeesConfig
import com.example.domain.model.EmployeeItem
import com.example.domain.model.OrderMethodsConfig
import com.example.domain.model.OutletInfo
import com.example.domain.model.PaymentGatewaysConfig
import com.example.domain.model.ReceiptCustomizerConfig
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local Data Persistence Layer for all POS settings:
 * - Outlet Information (Name, Branch, Address, Phone, NPWP, Instagram/Social)
 * - Receipt Display Customization (Paper Size, Header, Footer, Logo, Tax Toggle)
 * - Tax & Service Charge (PB1 Tax %, Service Fee %, Rounding)
 * - Payment Gateways (Cash, QRIS NMID/Merchant, Bank Transfer accounts)
 * - Order Methods (Dine-in, Takeaway, Online Delivery, Table Number)
 * - Employee List, Active Logged-in Cashier, and Active Shift
 * - Selected Bluetooth Thermal Printer
 *
 * Persisted permanently across app restarts, reboots, and cold starts via SharedPreferences.
 */
class SettingsStorageService(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "caffeinery_pos_settings_prefs"

        private const val KEY_OUTLET_INFO = "key_outlet_info"
        private const val KEY_RECEIPT_CONFIG = "key_receipt_config"
        private const val KEY_ADDITIONAL_FEES = "key_additional_fees"
        private const val KEY_ORDER_METHODS = "key_order_methods"
        private const val KEY_PAYMENT_GATEWAYS = "key_payment_gateways"
        private const val KEY_EMPLOYEES = "key_employees"
        private const val KEY_ACTIVE_EMPLOYEE = "key_active_employee"
        private const val KEY_ACTIVE_SHIFT = "key_active_shift"
        private const val KEY_SELECTED_PRINTER = "key_selected_printer"
        private const val KEY_CART_DRAFT = "key_cart_draft"
        private const val KEY_IS_INITIALIZED = "key_is_initialized"

        fun normalizeShift(raw: String): String {
            return when {
                raw.contains("Shift 2", ignoreCase = true) || raw.contains("Sore", ignoreCase = true) -> "Shift 2"
                raw.contains("Event", ignoreCase = true) || raw.contains("Khusus", ignoreCase = true) -> "Event"
                else -> "Shift 1"
            }
        }
    }

    /**
     * Initializes default configuration only on fresh first-time installation.
     */
    fun initDefaultsIfEmpty() {
        if (!prefs.getBoolean(KEY_IS_INITIALIZED, false)) {
            saveOutletInfo(OutletInfo())
            saveReceiptCustomizer(ReceiptCustomizerConfig())
            saveAdditionalFees(AdditionalFeesConfig())
            saveOrderMethods(OrderMethodsConfig())
            savePaymentGateways(PaymentGatewaysConfig())

            val defaultEmployees = listOf(
                EmployeeItem(name = "Rian Pratama", role = "Manager / Owner", shift = "Shift 1", pin = "8899"),
                EmployeeItem(name = "Maya Sari", role = "Head Barista", shift = "Shift 2", pin = "1234"),
                EmployeeItem(name = "Budi Santoso", role = "Kasir", shift = "Shift 1", pin = "2024"),
                EmployeeItem(name = "Dimas Anggara", role = "Barista", shift = "Shift 2", pin = "1122")
            )
            saveEmployees(defaultEmployees)
            saveActiveEmployee(defaultEmployees.first())
            saveActiveShift("Shift 1")
            saveSelectedPrinter("")

            prefs.edit().putBoolean(KEY_IS_INITIALIZED, true).apply()
        }
    }

    // ==========================================
    // 1. OUTLET INFORMATION
    // ==========================================
    fun saveOutletInfo(info: OutletInfo) {
        val json = JSONObject().apply {
            put("name", info.name)
            put("branch", info.branch)
            put("address", info.address)
            put("phone", info.phone)
            put("taxId", info.taxId)
            put("instagram", info.instagram)
        }
        prefs.edit().putString(KEY_OUTLET_INFO, json.toString()).apply()
    }

    fun loadOutletInfo(): OutletInfo {
        val raw = prefs.getString(KEY_OUTLET_INFO, null) ?: return OutletInfo()
        return try {
            val json = JSONObject(raw)
            OutletInfo(
                name = json.optString("name", "BDS Coffee Company"),
                branch = json.optString("branch", "Senopati Roastery"),
                address = json.optString("address", "Jl. Senopati No. 42, Kebayoran Baru, Jakarta Selatan"),
                phone = json.optString("phone", "+62 812-9988-7766"),
                taxId = json.optString("taxId", "01.234.567.8-012.000"),
                instagram = json.optString("instagram", "@bdscoffeecompany")
            )
        } catch (e: Exception) {
            e.printStackTrace()
            OutletInfo()
        }
    }

    // ==========================================
    // 2. RECEIPT DISPLAY CUSTOMIZER
    // ==========================================
    fun saveReceiptCustomizer(config: ReceiptCustomizerConfig) {
        val json = JSONObject().apply {
            put("paperSize", config.paperSize)
            put("showLogo", config.showLogo)
            put("showTax", config.showTax)
            put("headerNote", config.headerNote)
            put("footerNote", config.footerNote)
            put("printCopies", config.printCopies)
        }
        prefs.edit().putString(KEY_RECEIPT_CONFIG, json.toString()).apply()
    }

    fun loadReceiptCustomizer(): ReceiptCustomizerConfig {
        val raw = prefs.getString(KEY_RECEIPT_CONFIG, null) ?: return ReceiptCustomizerConfig()
        return try {
            val json = JSONObject(raw)
            ReceiptCustomizerConfig(
                paperSize = json.optString("paperSize", "58mm"),
                showLogo = json.optBoolean("showLogo", true),
                showTax = json.optBoolean("showTax", true),
                headerNote = json.optString("headerNote", "BDS COFFEE COMPANY"),
                footerNote = json.optString("footerNote", "Terima kasih atas kunjungan Anda ke BDS Coffee Company! • Caffeinery POS by BDS Tech"),
                printCopies = json.optInt("printCopies", 1)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ReceiptCustomizerConfig()
        }
    }

    // ==========================================
    // 3. TAX & SERVICE CHARGE (ADDITIONAL FEES)
    // ==========================================
    fun saveAdditionalFees(config: AdditionalFeesConfig) {
        val json = JSONObject().apply {
            put("isTaxEnabled", config.isTaxEnabled)
            put("taxPercentage", config.taxPercentage)
            put("isServiceFeeEnabled", config.isServiceFeeEnabled)
            put("serviceFeePercentage", config.serviceFeePercentage)
            put("isRoundingEnabled", config.isRoundingEnabled)
        }
        prefs.edit().putString(KEY_ADDITIONAL_FEES, json.toString()).apply()
    }

    fun loadAdditionalFees(): AdditionalFeesConfig {
        val raw = prefs.getString(KEY_ADDITIONAL_FEES, null) ?: return AdditionalFeesConfig()
        return try {
            val json = JSONObject(raw)
            AdditionalFeesConfig(
                isTaxEnabled = json.optBoolean("isTaxEnabled", true),
                taxPercentage = json.optDouble("taxPercentage", 10.0),
                isServiceFeeEnabled = json.optBoolean("isServiceFeeEnabled", false),
                serviceFeePercentage = json.optDouble("serviceFeePercentage", 5.0),
                isRoundingEnabled = json.optBoolean("isRoundingEnabled", true)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            AdditionalFeesConfig()
        }
    }

    // ==========================================
    // 4. ORDER METHODS CONFIG
    // ==========================================
    fun saveOrderMethods(config: OrderMethodsConfig) {
        val json = JSONObject().apply {
            put("isDineInEnabled", config.isDineInEnabled)
            put("isTakeAwayEnabled", config.isTakeAwayEnabled)
            put("isOnlineDeliveryEnabled", config.isOnlineDeliveryEnabled)
            put("isTableNumberRequired", config.isTableNumberRequired)
        }
        prefs.edit().putString(KEY_ORDER_METHODS, json.toString()).apply()
    }

    fun loadOrderMethods(): OrderMethodsConfig {
        val raw = prefs.getString(KEY_ORDER_METHODS, null) ?: return OrderMethodsConfig()
        return try {
            val json = JSONObject(raw)
            OrderMethodsConfig(
                isDineInEnabled = json.optBoolean("isDineInEnabled", true),
                isTakeAwayEnabled = json.optBoolean("isTakeAwayEnabled", true),
                isOnlineDeliveryEnabled = json.optBoolean("isOnlineDeliveryEnabled", true),
                isTableNumberRequired = json.optBoolean("isTableNumberRequired", true)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            OrderMethodsConfig()
        }
    }

    // ==========================================
    // 5. PAYMENT GATEWAYS CONFIG
    // ==========================================
    fun savePaymentGateways(config: PaymentGatewaysConfig) {
        val json = JSONObject().apply {
            put("isCashEnabled", config.isCashEnabled)
            put("isQrisEnabled", config.isQrisEnabled)
            put("qrisMerchantName", config.qrisMerchantName)
            put("qrisNmid", config.qrisNmid)
            put("qrisTerminal", config.qrisTerminal)
            put("isBankTransferEnabled", config.isBankTransferEnabled)
            put("bcaAccount", config.bcaAccount)
            put("mandiriAccount", config.mandiriAccount)
        }
        prefs.edit().putString(KEY_PAYMENT_GATEWAYS, json.toString()).apply()
    }

    fun loadPaymentGateways(): PaymentGatewaysConfig {
        val raw = prefs.getString(KEY_PAYMENT_GATEWAYS, null) ?: return PaymentGatewaysConfig()
        return try {
            val json = JSONObject(raw)
            PaymentGatewaysConfig(
                isCashEnabled = json.optBoolean("isCashEnabled", true),
                isQrisEnabled = json.optBoolean("isQrisEnabled", true),
                qrisMerchantName = json.optString("qrisMerchantName", "BDS COFFEE CO"),
                qrisNmid = json.optString("qrisNmid", "ID1024365195063"),
                qrisTerminal = json.optString("qrisTerminal", "A01"),
                isBankTransferEnabled = json.optBoolean("isBankTransferEnabled", true),
                bcaAccount = json.optString("bcaAccount", "BCA 827-019-2819 a/n PT Caffeinery Retail"),
                mandiriAccount = json.optString("mandiriAccount", "Mandiri 137-00-1928374-1 a/n Caffeinery Coffee")
            )
        } catch (e: Exception) {
            e.printStackTrace()
            PaymentGatewaysConfig()
        }
    }

    // ==========================================
    // 6. EMPLOYEES & ACTIVE SHIFT
    // ==========================================
    fun saveEmployees(list: List<EmployeeItem>) {
        val arr = JSONArray()
        list.forEach { emp ->
            val obj = JSONObject().apply {
                put("id", emp.id)
                put("name", emp.name)
                put("role", emp.role)
                put("shift", emp.shift)
                put("pin", emp.pin)
                put("isActive", emp.isActive)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_EMPLOYEES, arr.toString()).apply()
    }

    fun loadEmployees(): List<EmployeeItem> {
        val raw = prefs.getString(KEY_EMPLOYEES, null)
        if (raw.isNullOrBlank()) {
            return listOf(
                EmployeeItem(name = "Rian Pratama", role = "Manager / Owner", shift = "Shift 1", pin = "8899"),
                EmployeeItem(name = "Maya Sari", role = "Head Barista", shift = "Shift 2", pin = "1234"),
                EmployeeItem(name = "Budi Santoso", role = "Kasir", shift = "Shift 1", pin = "2024"),
                EmployeeItem(name = "Dimas Anggara", role = "Barista", shift = "Shift 2", pin = "1122")
            )
        }
        return try {
            val arr = JSONArray(raw)
            val result = mutableListOf<EmployeeItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                result.add(
                    EmployeeItem(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        name = obj.optString("name", "Karyawan"),
                        role = obj.optString("role", "Kasir"),
                        shift = normalizeShift(obj.optString("shift", "Shift 1")),
                        pin = obj.optString("pin", "1234"),
                        isActive = obj.optBoolean("isActive", true)
                    )
                )
            }
            if (result.isEmpty()) {
                listOf(
                    EmployeeItem(name = "Rian Pratama", role = "Manager / Owner", shift = "Shift 1", pin = "8899")
                )
            } else {
                result
            }
        } catch (e: Exception) {
            e.printStackTrace()
            listOf(
                EmployeeItem(name = "Rian Pratama", role = "Manager / Owner", shift = "Shift 1", pin = "8899")
            )
        }
    }

    fun saveActiveEmployee(emp: EmployeeItem) {
        val json = JSONObject().apply {
            put("id", emp.id)
            put("name", emp.name)
            put("role", emp.role)
            put("shift", emp.shift)
            put("pin", emp.pin)
            put("isActive", emp.isActive)
        }
        prefs.edit().putString(KEY_ACTIVE_EMPLOYEE, json.toString()).apply()
    }

    fun loadActiveEmployee(): EmployeeItem {
        val raw = prefs.getString(KEY_ACTIVE_EMPLOYEE, null)
        if (raw.isNullOrBlank()) {
            return EmployeeItem(name = "Rian Pratama", role = "Manager / Owner", shift = "Shift 1", pin = "8899")
        }
        return try {
            val json = JSONObject(raw)
            EmployeeItem(
                id = json.optString("id", java.util.UUID.randomUUID().toString()),
                name = json.optString("name", "Rian Pratama"),
                role = json.optString("role", "Manager / Owner"),
                shift = normalizeShift(json.optString("shift", "Shift 1")),
                pin = json.optString("pin", "8899"),
                isActive = json.optBoolean("isActive", true)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            EmployeeItem(name = "Rian Pratama", role = "Manager / Owner", shift = "Shift 1", pin = "8899")
        }
    }

    fun saveActiveShift(shift: String) {
        prefs.edit().putString(KEY_ACTIVE_SHIFT, normalizeShift(shift)).apply()
    }

    fun loadActiveShift(): String {
        val raw = prefs.getString(KEY_ACTIVE_SHIFT, "Shift 1") ?: "Shift 1"
        return normalizeShift(raw)
    }

    // ==========================================
    // 7. SELECTED THERMAL PRINTER
    // ==========================================
    fun saveSelectedPrinter(address: String) {
        prefs.edit().putString(KEY_SELECTED_PRINTER, address).apply()
    }

    fun loadSelectedPrinter(): String {
        return prefs.getString(KEY_SELECTED_PRINTER, "") ?: ""
    }

    // ==========================================
    // 8. CART & DRAFT RECOVERY
    // ==========================================
    data class CartDraft(
        val itemsJson: String,
        val customerName: String,
        val orderType: String,
        val discountType: String,
        val discountValue: Double,
        val timestamp: Long = System.currentTimeMillis()
    )

    fun saveActiveCartDraft(
        itemsJson: String,
        customerName: String,
        orderType: String,
        discountType: String,
        discountValue: Double
    ) {
        val json = JSONObject().apply {
            put("itemsJson", itemsJson)
            put("customerName", customerName)
            put("orderType", orderType)
            put("discountType", discountType)
            put("discountValue", discountValue)
            put("timestamp", System.currentTimeMillis())
        }
        prefs.edit().putString(KEY_CART_DRAFT, json.toString()).apply()
    }

    fun loadActiveCartDraft(): CartDraft? {
        val raw = prefs.getString(KEY_CART_DRAFT, null) ?: return null
        return try {
            val json = JSONObject(raw)
            CartDraft(
                itemsJson = json.optString("itemsJson", "[]"),
                customerName = json.optString("customerName", ""),
                orderType = json.optString("orderType", "DINE_IN"),
                discountType = json.optString("discountType", "NONE"),
                discountValue = json.optDouble("discountValue", 0.0),
                timestamp = json.optLong("timestamp", System.currentTimeMillis())
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun clearActiveCartDraft() {
        prefs.edit().remove(KEY_CART_DRAFT).apply()
    }
}
