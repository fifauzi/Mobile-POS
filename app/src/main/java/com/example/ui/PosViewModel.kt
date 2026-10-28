package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsStorageService
import com.example.data.local.model.CategoryEntity
import com.example.data.local.model.OrderEntity
import com.example.data.local.model.OrderItemEntity
import com.example.data.local.model.ProductEntity
import com.example.data.local.model.OpenBillEntity
import com.example.data.repository.PosRepository
import org.json.JSONArray
import org.json.JSONObject
import com.example.domain.model.CartItem
import com.example.domain.model.DiscountType
import com.example.domain.model.AdditionalFeesConfig
import com.example.domain.model.EmployeeItem
import com.example.domain.model.OrderMethodsConfig
import com.example.domain.model.OutletInfo
import com.example.domain.model.PaymentGatewaysConfig
import com.example.domain.model.ReceiptCustomizerConfig
import com.example.domain.model.VariantOption
import com.example.service.BluetoothPrinterDevice
import com.example.service.CsvExportService
import com.example.service.EscPosPrinterService
import com.example.service.QrisPayloadGenerator
import com.example.ui.components.CurrencyUtils
import com.example.ui.components.PosTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class CartSummary(
    val items: List<CartItem> = emptyList(),
    val subtotal: Double = 0.0,
    val itemDiscountTotal: Double = 0.0,
    val orderDiscountType: DiscountType = DiscountType.NONE,
    val orderDiscountValue: Double = 0.0,
    val orderDiscountAmount: Double = 0.0,
    val serviceFeePercentage: Double = 0.0,
    val serviceFeeAmount: Double = 0.0,
    val taxPercentage: Double = 10.0, // 10% PB1
    val taxAmount: Double = 0.0,
    val grandTotal: Double = 0.0,
    val totalItemCount: Int = 0
)

class PosViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PosRepository
    private val settingsStorage: SettingsStorageService = SettingsStorageService(application)

    // Draft Restoration Banner
    private val _wasDraftRestored = MutableStateFlow(false)
    val wasDraftRestored: StateFlow<Boolean> = _wasDraftRestored.asStateFlow()

    fun dismissDraftRestoredBanner() {
        _wasDraftRestored.value = false
    }

    init {
        val database = AppDatabase.getDatabase(application)
        repository = PosRepository(database.posDao())
        settingsStorage.initDefaultsIfEmpty()

        viewModelScope.launch(Dispatchers.IO) {
            com.example.data.local.DatabaseSeeder.seedIfEmpty(database.posDao())
            restoreActiveCartDraft()
        }
    }

    // Active Navigation Tab
    private val _currentTab = MutableStateFlow<PosTab>(PosTab.DASHBOARD)
    val currentTab: StateFlow<PosTab> = _currentTab.asStateFlow()

    fun setCurrentTab(tab: PosTab) {
        _currentTab.value = tab
    }

    // Search and Category Filter
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow("all")
    val selectedCategoryId: StateFlow<String> = _selectedCategoryId.asStateFlow()

    val categories: StateFlow<List<CategoryEntity>> = repository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<ProductEntity>> = repository.products
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val orders: StateFlow<List<OrderEntity>> = repository.orders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allOrderItems: StateFlow<List<OrderItemEntity>> = repository.allOrderItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val variantGroups: StateFlow<List<com.example.data.local.model.VariantGroupEntity>> = repository.variantGroups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeOpenBills: StateFlow<List<OpenBillEntity>> = repository.activeOpenBills
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addVariantGroup(group: com.example.data.local.model.VariantGroupEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addVariantGroup(group)
        }
    }

    fun updateVariantGroup(group: com.example.data.local.model.VariantGroupEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateVariantGroup(group)
        }
    }

    fun deleteVariantGroup(group: com.example.data.local.model.VariantGroupEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteVariantGroup(group)
        }
    }

    // Filtered Products for Catalog
    val filteredProducts: StateFlow<List<ProductEntity>> = combine(
        products,
        _selectedCategoryId,
        _searchQuery
    ) { all, catId, query ->
        all.filter { prod ->
            val matchesCategory = catId == "all" || prod.categoryId == catId
            val matchesSearch = query.isBlank() || prod.name.contains(query, ignoreCase = true) || prod.sku.contains(query, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectCategory(categoryId: String) {
        _selectedCategoryId.value = categoryId
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Cart State
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _orderDiscountType = MutableStateFlow(DiscountType.NONE)
    val orderDiscountType: StateFlow<DiscountType> = _orderDiscountType.asStateFlow()

    private val _orderDiscountValue = MutableStateFlow(0.0)
    val orderDiscountValue: StateFlow<Double> = _orderDiscountValue.asStateFlow()

    private val _customerName = MutableStateFlow("")
    val customerName: StateFlow<String> = _customerName.asStateFlow()

    fun setCustomerName(name: String) {
        _customerName.value = name
        persistCurrentCartState()
    }

    private val _orderType = MutableStateFlow("DINE_IN") // "DINE_IN" or "TAKEAWAY"
    val orderType: StateFlow<String> = _orderType.asStateFlow()

    fun setOrderType(type: String) {
        _orderType.value = type
        persistCurrentCartState()
    }

    // Business Settings Management (Persisted via SettingsStorageService)
    private val _outletInfo = MutableStateFlow(settingsStorage.loadOutletInfo())
    val outletInfo: StateFlow<OutletInfo> = _outletInfo.asStateFlow()

    fun updateOutletInfo(info: OutletInfo) {
        _outletInfo.value = info
        settingsStorage.saveOutletInfo(info)
    }

    private val _employees = MutableStateFlow(settingsStorage.loadEmployees())
    val employees: StateFlow<List<EmployeeItem>> = _employees.asStateFlow()

    // Active Logged-in Cashier & Shift Tracking
    private val _activeEmployee = MutableStateFlow(settingsStorage.loadActiveEmployee())
    val activeEmployee: StateFlow<EmployeeItem> = _activeEmployee.asStateFlow()

    fun setActiveEmployee(emp: EmployeeItem) {
        _activeEmployee.value = emp
        settingsStorage.saveActiveEmployee(emp)
    }

    private val _activeShift = MutableStateFlow(settingsStorage.loadActiveShift())
    val activeShift: StateFlow<String> = _activeShift.asStateFlow()

    fun setActiveShift(shift: String) {
        _activeShift.value = shift
        settingsStorage.saveActiveShift(shift)
    }

    fun addEmployee(name: String, role: String, pin: String) {
        val newItem = EmployeeItem(name = name, role = role, pin = pin)
        val updated = _employees.value + newItem
        _employees.value = updated
        settingsStorage.saveEmployees(updated)
    }

    fun removeEmployee(id: String) {
        val updated = _employees.value.filter { it.id != id }
        _employees.value = updated
        settingsStorage.saveEmployees(updated)
    }

    private val _orderMethods = MutableStateFlow(settingsStorage.loadOrderMethods())
    val orderMethods: StateFlow<OrderMethodsConfig> = _orderMethods.asStateFlow()

    fun updateOrderMethods(config: OrderMethodsConfig) {
        _orderMethods.value = config
        settingsStorage.saveOrderMethods(config)
    }

    private val _paymentGateways = MutableStateFlow(settingsStorage.loadPaymentGateways())
    val paymentGateways: StateFlow<PaymentGatewaysConfig> = _paymentGateways.asStateFlow()

    fun updatePaymentGateways(config: PaymentGatewaysConfig) {
        _paymentGateways.value = config
        settingsStorage.savePaymentGateways(config)
    }

    private val _additionalFees = MutableStateFlow(settingsStorage.loadAdditionalFees())
    val additionalFees: StateFlow<AdditionalFeesConfig> = _additionalFees.asStateFlow()

    fun updateAdditionalFees(config: AdditionalFeesConfig) {
        _additionalFees.value = config
        settingsStorage.saveAdditionalFees(config)
    }

    private val _receiptCustomizer = MutableStateFlow(settingsStorage.loadReceiptCustomizer())
    val receiptCustomizer: StateFlow<ReceiptCustomizerConfig> = _receiptCustomizer.asStateFlow()

    fun updateReceiptCustomizer(config: ReceiptCustomizerConfig) {
        _receiptCustomizer.value = config
        settingsStorage.saveReceiptCustomizer(config)
    }

    val cartSummary: StateFlow<CartSummary> = combine(
        _cartItems,
        _orderDiscountType,
        _orderDiscountValue,
        _additionalFees
    ) { items, discType, discVal, feesConfig ->
        val subtotal = items.sumOf { it.lineTotal }
        val itemDiscTotal = items.sumOf { it.totalDiscountAmount }
        val totalCount = items.sumOf { it.quantity }

        val orderDiscAmount = when (discType) {
            DiscountType.PERCENTAGE -> (subtotal * (discVal / 100.0)).coerceAtLeast(0.0)
            DiscountType.FIXED_IDR -> discVal.coerceAtMost(subtotal).coerceAtLeast(0.0)
            DiscountType.NONE -> 0.0
        }

        val afterDiscount = (subtotal - orderDiscAmount).coerceAtLeast(0.0)

        val serviceFeePct = if (feesConfig.isServiceFeeEnabled) feesConfig.serviceFeePercentage else 0.0
        val serviceFeeAmount = (afterDiscount * (serviceFeePct / 100.0)).coerceAtLeast(0.0)

        val taxableBase = afterDiscount + serviceFeeAmount
        val taxPct = if (feesConfig.isTaxEnabled) feesConfig.taxPercentage else 0.0
        val taxAmount = (taxableBase * (taxPct / 100.0)).coerceAtLeast(0.0)

        var total = afterDiscount + serviceFeeAmount + taxAmount
        if (feesConfig.isRoundingEnabled) {
            total = kotlin.math.round(total / 100.0) * 100.0
        }

        CartSummary(
            items = items,
            subtotal = subtotal,
            itemDiscountTotal = itemDiscTotal,
            orderDiscountType = discType,
            orderDiscountValue = discVal,
            orderDiscountAmount = orderDiscAmount,
            serviceFeePercentage = serviceFeePct,
            serviceFeeAmount = serviceFeeAmount,
            taxPercentage = taxPct,
            taxAmount = taxAmount,
            grandTotal = total,
            totalItemCount = totalCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CartSummary())

    // Modals & Sheets
    private val _selectedProductForVariant = MutableStateFlow<ProductEntity?>(null)
    val selectedProductForVariant: StateFlow<ProductEntity?> = _selectedProductForVariant.asStateFlow()

    private val _isCartSheetOpen = MutableStateFlow(false)
    val isCartSheetOpen: StateFlow<Boolean> = _isCartSheetOpen.asStateFlow()

    private val _isCheckoutModalOpen = MutableStateFlow(false)
    val isCheckoutModalOpen: StateFlow<Boolean> = _isCheckoutModalOpen.asStateFlow()

    private val _isReceiptModalOpen = MutableStateFlow(false)
    val isReceiptModalOpen: StateFlow<Boolean> = _isReceiptModalOpen.asStateFlow()

    private val _activeCompletedOrder = MutableStateFlow<Pair<OrderEntity, List<OrderItemEntity>>?>(null)
    val activeCompletedOrder: StateFlow<Pair<OrderEntity, List<OrderItemEntity>>?> = _activeCompletedOrder.asStateFlow()

    // Printer settings
    private val _pairedPrinters = MutableStateFlow<List<BluetoothPrinterDevice>>(emptyList())
    val pairedPrinters: StateFlow<List<BluetoothPrinterDevice>> = _pairedPrinters.asStateFlow()

    private val _selectedPrinterAddress = MutableStateFlow(settingsStorage.loadSelectedPrinter().ifBlank { "00:11:22:33:44:55" })
    val selectedPrinterAddress: StateFlow<String> = _selectedPrinterAddress.asStateFlow()

    private val _printerStatusMessage = MutableStateFlow("Ready to print")
    val printerStatusMessage: StateFlow<String> = _printerStatusMessage.asStateFlow()

    private val _isPrintingAnimation = MutableStateFlow(false)
    val isPrintingAnimation: StateFlow<Boolean> = _isPrintingAnimation.asStateFlow()

    private val _showPrinterDisconnectedAlert = MutableStateFlow(false)
    val showPrinterDisconnectedAlert: StateFlow<Boolean> = _showPrinterDisconnectedAlert.asStateFlow()

    fun dismissPrinterDisconnectedAlert() {
        _showPrinterDisconnectedAlert.value = false
    }

    fun triggerPrinterDisconnectedAlert() {
        _showPrinterDisconnectedAlert.value = true
    }

    fun openVariantPicker(product: ProductEntity) {
        _selectedProductForVariant.value = product
    }

    fun closeVariantPicker() {
        _selectedProductForVariant.value = null
    }

    fun openCartSheet() {
        _isCartSheetOpen.value = true
    }

    fun closeCartSheet() {
        _isCartSheetOpen.value = false
    }

    fun openCheckoutModal() {
        _isCartSheetOpen.value = false
        _isCheckoutModalOpen.value = true
    }

    fun closeCheckoutModal() {
        _isCheckoutModalOpen.value = false
    }

    fun closeReceiptModal() {
        _isReceiptModalOpen.value = false
        _activeCompletedOrder.value = null
    }

    fun addToCart(
        product: ProductEntity,
        selectedVariants: Map<String, List<VariantOption>>,
        quantity: Int = 1,
        discountType: DiscountType = DiscountType.NONE,
        discountValue: Double = 0.0,
        notes: String = ""
    ) {
        val current = _cartItems.value.toMutableList()
        // Check if an identical item (same product and exact same variants and discount) already exists
        val existingIndex = current.indexOfFirst {
            it.product.id == product.id &&
                    it.selectedVariants == selectedVariants &&
                    it.discountType == discountType &&
                    it.discountValue == discountValue
        }

        if (existingIndex >= 0) {
            val oldItem = current[existingIndex]
            current[existingIndex] = oldItem.copy(quantity = oldItem.quantity + quantity)
        } else {
            current.add(
                CartItem(
                    product = product,
                    selectedVariants = selectedVariants,
                    quantity = quantity,
                    discountType = discountType,
                    discountValue = discountValue,
                    notes = notes
                )
            )
        }
        _cartItems.value = current
        closeVariantPicker()
        persistCurrentCartState()
    }

    fun updateCartItemQuantity(cartItemId: String, delta: Int) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.cartItemId == cartItemId }
        if (index >= 0) {
            val item = current[index]
            val newQty = item.quantity + delta
            if (newQty <= 0) {
                current.removeAt(index)
            } else {
                current[index] = item.copy(quantity = newQty)
            }
            _cartItems.value = current
            persistCurrentCartState()
        }
    }

    fun removeCartItem(cartItemId: String) {
        _cartItems.value = _cartItems.value.filter { it.cartItemId != cartItemId }
        persistCurrentCartState()
    }

    fun setOrderDiscount(type: DiscountType, value: Double) {
        _orderDiscountType.value = type
        _orderDiscountValue.value = value
        persistCurrentCartState()
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _orderDiscountType.value = DiscountType.NONE
        _orderDiscountValue.value = 0.0
        _customerName.value = ""
        _orderType.value = "DINE_IN"
        settingsStorage.clearActiveCartDraft()
    }

    // Process Sale Checkout
    fun completeCheckout(
        paymentMethod: String,
        tenderedAmount: Double,
        notes: String = ""
    ) {
        val summary = cartSummary.value
        val items = summary.items
        if (items.isEmpty()) return

        val now = System.currentTimeMillis()
        val invoice = "INV-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date(now))
        val change = (tenderedAmount - summary.grandTotal).coerceAtLeast(0.0)

        val order = OrderEntity(
            invoiceNumber = invoice,
            timestamp = now,
            customerName = _customerName.value.trim().ifBlank { "Pelanggan Walk-in" },
            orderType = _orderType.value,
            subtotal = summary.subtotal,
            itemDiscountTotal = summary.itemDiscountTotal,
            orderDiscountPercentage = if (summary.orderDiscountType == DiscountType.PERCENTAGE) summary.orderDiscountValue else 0.0,
            orderDiscountAmount = summary.orderDiscountAmount,
            serviceFeePercentage = summary.serviceFeePercentage,
            serviceFeeAmount = summary.serviceFeeAmount,
            taxPercentage = summary.taxPercentage,
            taxAmount = summary.taxAmount,
            grandTotal = summary.grandTotal,
            paymentMethod = paymentMethod,
            tenderedAmount = tenderedAmount,
            changeAmount = change,
            paymentStatus = "PAID",
            cashierName = _activeEmployee.value.name,
            shiftName = _activeShift.value,
            notes = notes
        )

        val orderItems = items.map { cartItem ->
            val catName = categories.value.find { it.id == cartItem.product.categoryId }?.name ?: "Coffee"
            val varSize = cartItem.selectedVariants.entries.firstOrNull { it.key.contains("Size", ignoreCase = true) }
                ?.value?.joinToString(", ") { opt -> if (opt.priceAdjustment > 0) "${opt.name} (+${CurrencyUtils.formatIdr(opt.priceAdjustment)})" else opt.name } ?: "-"
            val varType = cartItem.selectedVariants.entries.firstOrNull { it.key.contains("Type", ignoreCase = true) }
                ?.value?.joinToString(", ") { opt -> if (opt.priceAdjustment > 0) "${opt.name} (+${CurrencyUtils.formatIdr(opt.priceAdjustment)})" else opt.name } ?: "-"
            val varBeans = cartItem.selectedVariants.entries.firstOrNull { it.key.contains("Beans", ignoreCase = true) }
                ?.value?.joinToString(", ") { opt -> if (opt.priceAdjustment > 0) "${opt.name} (+${CurrencyUtils.formatIdr(opt.priceAdjustment)})" else opt.name } ?: "-"
            val varGrind = cartItem.selectedVariants.entries.firstOrNull { it.key.contains("Grind", ignoreCase = true) }
                ?.value?.joinToString(", ") { opt -> if (opt.priceAdjustment > 0) "${opt.name} (+${CurrencyUtils.formatIdr(opt.priceAdjustment)})" else opt.name } ?: "-"
            val varSugar = cartItem.selectedVariants.entries.firstOrNull { it.key.contains("Sugar", ignoreCase = true) }
                ?.value?.joinToString(", ") { opt -> if (opt.priceAdjustment > 0) "${opt.name} (+${CurrencyUtils.formatIdr(opt.priceAdjustment)})" else opt.name } ?: "-"
            val varAddons = cartItem.selectedVariants.entries.firstOrNull { it.key.contains("Add", ignoreCase = true) }
                ?.value?.joinToString(", ") { opt -> if (opt.priceAdjustment > 0) "${opt.name} (+${CurrencyUtils.formatIdr(opt.priceAdjustment)})" else opt.name } ?: "-"

            val b2bTag = if (cartItem.isB2bPriceApplied) " [B2B]" else ""
            val displayName = cartItem.product.name + b2bTag
            val itemVariantSummary = if (cartItem.isB2bPriceApplied) {
                val tierStr = cartItem.b2bTierLabel ?: "Harga B2B"
                if (cartItem.variantSummary == "Standard" || cartItem.variantSummary.isBlank()) tierStr else "${cartItem.variantSummary}, $tierStr"
            } else {
                cartItem.variantSummary
            }

            OrderItemEntity(
                orderInvoiceNumber = invoice,
                productId = cartItem.product.id,
                productName = displayName,
                sku = cartItem.product.sku,
                basePrice = cartItem.activeBasePrice,
                costPrice = cartItem.product.costPrice,
                variantSummary = itemVariantSummary,
                unitPrice = cartItem.finalUnitPrice,
                quantity = cartItem.quantity,
                itemDiscount = cartItem.totalDiscountAmount,
                lineTotal = cartItem.lineTotal,
                categoryName = catName,
                variantSize = varSize,
                variantType = varType,
                variantBeans = varBeans,
                variantGrind = varGrind,
                variantSugar = varSugar,
                variantAddons = varAddons,
                variantExtraTotal = cartItem.variantPriceDelta
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            repository.completeSale(order, orderItems)
            _activeCompletedOrder.value = Pair(order, orderItems)
            _isCheckoutModalOpen.value = false
            _isReceiptModalOpen.value = true
            clearCart()
            // Auto-Print Trigger upon completing payment
            attemptPrintReceipt(getApplication(), order, orderItems)
        }
    }

    // Bluetooth Printer Methods
    fun refreshBluetoothPrinters(context: Context) {
        _pairedPrinters.value = EscPosPrinterService.getPairedBluetoothPrinters(context)
    }

    fun selectPrinter(address: String) {
        _selectedPrinterAddress.value = address
        settingsStorage.saveSelectedPrinter(address)
    }

    /**
     * Guards print execution: checks if printer is connected.
     * If disconnected, sets showPrinterDisconnectedAlert to true.
     */
    fun attemptPrintReceipt(context: Context, order: OrderEntity, items: List<OrderItemEntity>) {
        val address = _selectedPrinterAddress.value
        val isConnected = EscPosPrinterService.isPrinterConnected(context, address)
        if (!isConnected) {
            _showPrinterDisconnectedAlert.value = true
            _printerStatusMessage.value = "Printer belum terhubung. Silakan sambungkan printer Bluetooth terlebih dahulu."
            return
        }
        printCurrentReceipt(context, order, items)
    }

    fun printCurrentReceipt(context: Context, order: OrderEntity, items: List<OrderItemEntity>) {
        viewModelScope.launch {
            _isPrintingAnimation.value = true
            _printerStatusMessage.value = "Transmitting ESC/POS bytes..."
            val info = _outletInfo.value
            val rc = _receiptCustomizer.value
            val bytes = EscPosPrinterService.buildReceiptBytes(
                order = order,
                items = items,
                storeName = info.name,
                storeAddress = "${info.address} (${info.branch})",
                storePhone = "Telp: ${info.phone}",
                footerMessage = rc.footerNote,
                showTax = rc.showTax,
                paperWidth = if (rc.paperSize == "80mm") 48 else 32
            )
            val result = EscPosPrinterService.printBytes(_selectedPrinterAddress.value, bytes)
            result.onSuccess { msg ->
                _printerStatusMessage.value = msg
            }.onFailure { err ->
                _printerStatusMessage.value = "Print error: ${err.localizedMessage}"
            }
            _isPrintingAnimation.value = false
        }
    }

    // Inventory Management
    fun addProduct(product: ProductEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addProduct(product)
        }
    }

    fun updateProduct(product: ProductEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateProduct(product)
        }
    }

    fun updateStock(productId: Long, newStock: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateStock(productId, newStock)
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteProduct(product)
        }
    }

    // Category Management
    fun addCategory(category: CategoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addCategory(category)
        }
    }

    fun updateCategory(category: CategoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateCategory(category)
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCategory(category)
        }
    }

    // CSV Sales Export
    fun exportSalesCsv(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val allOrders = orders.value
            val allItems = allOrderItems.value.groupBy { it.orderInvoiceNumber }
            val csv = CsvExportService.generateSalesCsv(allOrders, allItems)
            launch(Dispatchers.Main) {
                CsvExportService.shareCsvReport(context, csv)
            }
        }
    }

    // 📧 Email Report Dispatcher
    fun sendSalesReportEmail(
        context: Context,
        targetEmail: String = "bdscoffeecompany@gmail.com"
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val allOrders = orders.value
            val allItems = allOrderItems.value.groupBy { it.orderInvoiceNumber }
            val csv = CsvExportService.generateSalesCsv(allOrders, allItems)

            val totalRevenue = allOrders.sumOf { it.grandTotal }
            val totalHpp = allOrderItems.value.sumOf { it.costPrice * it.quantity }
            val totalProfit = (totalRevenue - totalHpp).coerceAtLeast(0.0)

            val summaryStats = """
                • Total Transaksi: ${allOrders.size} pesanan
                • Total Omzet (Revenue): ${com.example.ui.components.CurrencyUtils.formatIdr(totalRevenue)}
                • Estimasi Laba Bersih: ${com.example.ui.components.CurrencyUtils.formatIdr(totalProfit)}
                • Tanggal Laporan: ${SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID")).format(Date())}
            """.trimIndent()

            launch(Dispatchers.Main) {
                CsvExportService.sendCsvReportViaEmail(
                    context = context,
                    csvContent = csv,
                    targetEmail = targetEmail,
                    outletName = _outletInfo.value.name,
                    summaryStats = summaryStats
                )
            }
        }
    }

    // 📝 Open Bill / Hold Order Actions
    fun saveCurrentCartAsOpenBill(
        customerName: String,
        tableNumber: String,
        orderType: String,
        notes: String = ""
    ) {
        val summary = cartSummary.value
        val items = summary.items
        if (items.isEmpty()) return

        val now = System.currentTimeMillis()
        val billNum = "OB-" + SimpleDateFormat("HHmmss", Locale.US).format(Date(now))

        val jsonArray = serializeCartItemsToJson(items)

        val openBill = OpenBillEntity(
            billNumber = billNum,
            customerName = customerName.ifBlank { "Tamu" },
            tableNumber = tableNumber,
            orderType = orderType,
            itemsJson = jsonArray.toString(),
            totalItemCount = summary.totalItemCount,
            subtotal = summary.subtotal,
            taxAmount = summary.taxAmount,
            serviceFeeAmount = summary.serviceFeeAmount,
            grandTotal = summary.grandTotal,
            notes = notes,
            cashierName = _activeEmployee.value.name,
            createdAt = now,
            status = "ACTIVE"
        )

        viewModelScope.launch(Dispatchers.IO) {
            repository.saveOpenBill(openBill)
            launch(Dispatchers.Main) {
                clearCart()
                closeCartSheet()
            }
        }
    }

    fun recallOpenBill(bill: OpenBillEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val restoredItems = mutableListOf<CartItem>()
            try {
                val array = JSONArray(bill.itemsJson)
                val allProds = products.value.associateBy { it.id }

                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val cId = obj.optString("cartItemId", UUID.randomUUID().toString())
                    val pId = obj.optLong("productId", 0L)
                    val pName = obj.optString("productName", "Produk")
                    val pPrice = obj.optDouble("productPrice", 0.0)
                    val pCost = obj.optDouble("productCost", 0.0)
                    val pCat = obj.optString("productCategory", "coffee")
                    val pStock = obj.optInt("productStock", 999)
                    val pUnit = obj.optString("productUnit", "cup")
                    val hasB2b = obj.optBoolean("hasB2bPricing", false)
                    val b2bT1Qty = obj.optInt("b2bTier1MinQty", 5)
                    val b2bT1Price = obj.optDouble("b2bTier1Price", 0.0)
                    val b2bT2Qty = obj.optInt("b2bTier2MinQty", 10)
                    val b2bT2Price = obj.optDouble("b2bTier2Price", 0.0)
                    val qty = obj.optInt("quantity", 1)
                    val dTypeStr = obj.optString("discountType", "NONE")
                    val dVal = obj.optDouble("discountValue", 0.0)
                    val notes = obj.optString("notes", "")

                    val product = allProds[pId] ?: ProductEntity(
                        id = pId,
                        sku = obj.optString("sku", "ITEM-$pId"),
                        name = pName,
                        categoryId = pCat,
                        basePrice = pPrice,
                        costPrice = pCost,
                        stockQuantity = pStock,
                        unit = pUnit,
                        hasB2bPricing = hasB2b,
                        b2bTier1MinQty = b2bT1Qty,
                        b2bTier1Price = b2bT1Price,
                        b2bTier2MinQty = b2bT2Qty,
                        b2bTier2Price = b2bT2Price
                    )

                    val variantsMap = mutableMapOf<String, List<VariantOption>>()
                    val vObj = obj.optJSONObject("selectedVariants")
                    if (vObj != null) {
                        val keys = vObj.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            val oArr = vObj.optJSONArray(k)
                            val oList = mutableListOf<VariantOption>()
                            if (oArr != null) {
                                for (j in 0 until oArr.length()) {
                                    val itemObj = oArr.getJSONObject(j)
                                    oList.add(
                                        VariantOption(
                                            name = itemObj.optString("name", ""),
                                            priceAdjustment = itemObj.optDouble("price", 0.0)
                                        )
                                    )
                                }
                            }
                            variantsMap[k] = oList
                        }
                    }

                    restoredItems.add(
                        CartItem(
                            cartItemId = cId,
                            product = product,
                            selectedVariants = variantsMap,
                            quantity = qty,
                            discountType = try { DiscountType.valueOf(dTypeStr) } catch (_: Exception) { DiscountType.NONE },
                            discountValue = dVal,
                            notes = notes
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            repository.deleteOpenBill(bill.id)

            launch(Dispatchers.Main) {
                _cartItems.value = restoredItems
                _orderType.value = bill.orderType
                _customerName.value = if (bill.tableNumber.isNotBlank()) "${bill.customerName} (${bill.tableNumber})" else bill.customerName
                persistCurrentCartState()
                openCartSheet()
            }
        }
    }

    fun deleteOpenBill(billId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteOpenBill(billId)
        }
    }

    // ==========================================
    // 💾 CART STATE PERSISTENCE & RESTORATION
    // ==========================================
    private fun serializeCartItemsToJson(items: List<CartItem>): JSONArray {
        val jsonArray = JSONArray()
        for (item in items) {
            val obj = JSONObject().apply {
                put("cartItemId", item.cartItemId)
                put("productId", item.product.id)
                put("sku", item.product.sku)
                put("productName", item.product.name)
                put("productPrice", item.product.basePrice)
                put("productCost", item.product.costPrice)
                put("productCategory", item.product.categoryId)
                put("productStock", item.product.stockQuantity)
                put("productUnit", item.product.unit)
                put("hasB2bPricing", item.product.hasB2bPricing)
                put("b2bTier1MinQty", item.product.b2bTier1MinQty)
                put("b2bTier1Price", item.product.b2bTier1Price)
                put("b2bTier2MinQty", item.product.b2bTier2MinQty)
                put("b2bTier2Price", item.product.b2bTier2Price)
                put("quantity", item.quantity)
                put("discountType", item.discountType.name)
                put("discountValue", item.discountValue)
                put("notes", item.notes)

                val variantsObj = JSONObject()
                for ((groupName, options) in item.selectedVariants) {
                    val optsArray = JSONArray()
                    for (opt in options) {
                        val o = JSONObject().apply {
                            put("name", opt.name)
                            put("price", opt.priceAdjustment)
                        }
                        optsArray.put(o)
                    }
                    variantsObj.put(groupName, optsArray)
                }
                put("selectedVariants", variantsObj)
            }
            jsonArray.put(obj)
        }
        return jsonArray
    }

    private fun persistCurrentCartState() {
        val items = _cartItems.value
        if (items.isEmpty()) {
            settingsStorage.clearActiveCartDraft()
        } else {
            val jsonArray = serializeCartItemsToJson(items)
            settingsStorage.saveActiveCartDraft(
                itemsJson = jsonArray.toString(),
                customerName = _customerName.value,
                orderType = _orderType.value,
                discountType = _orderDiscountType.value.name,
                discountValue = _orderDiscountValue.value
            )
        }
    }

    private suspend fun restoreActiveCartDraft() {
        val draft = settingsStorage.loadActiveCartDraft() ?: return
        if (draft.itemsJson.isBlank() || draft.itemsJson == "[]") return

        try {
            val array = JSONArray(draft.itemsJson)
            if (array.length() == 0) return

            val liveProducts = repository.getAllProductsSync().associateBy { it.id }
            val restoredItems = mutableListOf<CartItem>()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val cId = obj.optString("cartItemId", UUID.randomUUID().toString())
                val pId = obj.optLong("productId", 0L)
                val sku = obj.optString("sku", "ITEM-$pId")
                val pName = obj.optString("productName", "Produk")
                val pPrice = obj.optDouble("productPrice", 0.0)
                val pCost = obj.optDouble("productCost", 0.0)
                val pCat = obj.optString("productCategory", "coffee")
                val pStock = obj.optInt("productStock", 999)
                val pUnit = obj.optString("productUnit", "cup")
                val hasB2b = obj.optBoolean("hasB2bPricing", false)
                val b2bT1Qty = obj.optInt("b2bTier1MinQty", 5)
                val b2bT1Price = obj.optDouble("b2bTier1Price", 0.0)
                val b2bT2Qty = obj.optInt("b2bTier2MinQty", 10)
                val b2bT2Price = obj.optDouble("b2bTier2Price", 0.0)
                val qty = obj.optInt("quantity", 1)
                val dTypeStr = obj.optString("discountType", "NONE")
                val dVal = obj.optDouble("discountValue", 0.0)
                val notes = obj.optString("notes", "")

                val product = liveProducts[pId] ?: ProductEntity(
                    id = pId,
                    sku = sku,
                    name = pName,
                    categoryId = pCat,
                    basePrice = pPrice,
                    costPrice = pCost,
                    stockQuantity = pStock,
                    unit = pUnit,
                    hasB2bPricing = hasB2b,
                    b2bTier1MinQty = b2bT1Qty,
                    b2bTier1Price = b2bT1Price,
                    b2bTier2MinQty = b2bT2Qty,
                    b2bTier2Price = b2bT2Price
                )

                val variantsMap = mutableMapOf<String, List<VariantOption>>()
                val vObj = obj.optJSONObject("selectedVariants")
                if (vObj != null) {
                    val keys = vObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        val oArr = vObj.optJSONArray(k)
                        val oList = mutableListOf<VariantOption>()
                        if (oArr != null) {
                            for (j in 0 until oArr.length()) {
                                val itemObj = oArr.getJSONObject(j)
                                oList.add(
                                    VariantOption(
                                        name = itemObj.optString("name", ""),
                                        priceAdjustment = itemObj.optDouble("price", 0.0)
                                    )
                                )
                            }
                        }
                        variantsMap[k] = oList
                    }
                }

                restoredItems.add(
                    CartItem(
                        cartItemId = cId,
                        product = product,
                        selectedVariants = variantsMap,
                        quantity = qty,
                        discountType = try { DiscountType.valueOf(dTypeStr) } catch (_: Exception) { DiscountType.NONE },
                        discountValue = dVal,
                        notes = notes
                    )
                )
            }

            if (restoredItems.isNotEmpty()) {
                _cartItems.value = restoredItems
                _customerName.value = draft.customerName
                _orderType.value = draft.orderType
                _orderDiscountType.value = try { DiscountType.valueOf(draft.discountType) } catch (_: Exception) { DiscountType.NONE }
                _orderDiscountValue.value = draft.discountValue
                _wasDraftRestored.value = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
