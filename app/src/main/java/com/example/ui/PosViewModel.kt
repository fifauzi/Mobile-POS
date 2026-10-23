package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
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

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = PosRepository(database.posDao())
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

    private val _customerName = MutableStateFlow("Guest")
    val customerName: StateFlow<String> = _customerName.asStateFlow()

    fun setCustomerName(name: String) {
        _customerName.value = name
    }

    private val _orderType = MutableStateFlow("DINE_IN") // "DINE_IN" or "TAKEAWAY"
    val orderType: StateFlow<String> = _orderType.asStateFlow()

    fun setOrderType(type: String) {
        _orderType.value = type
    }

    // Business Settings Management
    private val _outletInfo = MutableStateFlow(OutletInfo())
    val outletInfo: StateFlow<OutletInfo> = _outletInfo.asStateFlow()

    fun updateOutletInfo(info: OutletInfo) {
        _outletInfo.value = info
    }

    private val _employees = MutableStateFlow(
        listOf(
            EmployeeItem(name = "Rian Pratama", role = "Manager / Owner", pin = "8899"),
            EmployeeItem(name = "Maya Sari", role = "Head Barista", pin = "1234"),
            EmployeeItem(name = "Budi Santoso", role = "Kasir", pin = "2024"),
            EmployeeItem(name = "Dimas Anggara", role = "Barista", pin = "1122")
        )
    )
    val employees: StateFlow<List<EmployeeItem>> = _employees.asStateFlow()

    // Active Logged-in Cashier & Shift Tracking
    private val _activeEmployee = MutableStateFlow(
        EmployeeItem(name = "Rian Pratama", role = "Manager / Owner", shift = "Shift 1 (Pagi)", pin = "8899")
    )
    val activeEmployee: StateFlow<EmployeeItem> = _activeEmployee.asStateFlow()

    fun setActiveEmployee(emp: EmployeeItem) {
        _activeEmployee.value = emp
    }

    private val _activeShift = MutableStateFlow("Shift 1 (08:00 - 16:00)")
    val activeShift: StateFlow<String> = _activeShift.asStateFlow()

    fun setActiveShift(shift: String) {
        _activeShift.value = shift
    }

    fun addEmployee(name: String, role: String, pin: String) {
        val newItem = EmployeeItem(name = name, role = role, pin = pin)
        _employees.value = _employees.value + newItem
    }

    fun removeEmployee(id: String) {
        _employees.value = _employees.value.filter { it.id != id }
    }

    private val _orderMethods = MutableStateFlow(OrderMethodsConfig())
    val orderMethods: StateFlow<OrderMethodsConfig> = _orderMethods.asStateFlow()

    fun updateOrderMethods(config: OrderMethodsConfig) {
        _orderMethods.value = config
    }

    private val _paymentGateways = MutableStateFlow(PaymentGatewaysConfig())
    val paymentGateways: StateFlow<PaymentGatewaysConfig> = _paymentGateways.asStateFlow()

    fun updatePaymentGateways(config: PaymentGatewaysConfig) {
        _paymentGateways.value = config
    }

    private val _additionalFees = MutableStateFlow(AdditionalFeesConfig())
    val additionalFees: StateFlow<AdditionalFeesConfig> = _additionalFees.asStateFlow()

    fun updateAdditionalFees(config: AdditionalFeesConfig) {
        _additionalFees.value = config
    }

    private val _receiptCustomizer = MutableStateFlow(ReceiptCustomizerConfig())
    val receiptCustomizer: StateFlow<ReceiptCustomizerConfig> = _receiptCustomizer.asStateFlow()

    fun updateReceiptCustomizer(config: ReceiptCustomizerConfig) {
        _receiptCustomizer.value = config
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

    private val _selectedPrinterAddress = MutableStateFlow("00:11:22:33:44:55")
    val selectedPrinterAddress: StateFlow<String> = _selectedPrinterAddress.asStateFlow()

    private val _printerStatusMessage = MutableStateFlow("Ready to print")
    val printerStatusMessage: StateFlow<String> = _printerStatusMessage.asStateFlow()

    private val _isPrintingAnimation = MutableStateFlow(false)
    val isPrintingAnimation: StateFlow<Boolean> = _isPrintingAnimation.asStateFlow()

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
        }
    }

    fun removeCartItem(cartItemId: String) {
        _cartItems.value = _cartItems.value.filter { it.cartItemId != cartItemId }
    }

    fun setOrderDiscount(type: DiscountType, value: Double) {
        _orderDiscountType.value = type
        _orderDiscountValue.value = value
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _orderDiscountType.value = DiscountType.NONE
        _orderDiscountValue.value = 0.0
        _customerName.value = "Guest"
        _orderType.value = "DINE_IN"
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
            customerName = _customerName.value.ifBlank { "Walk-in Guest" },
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
            OrderItemEntity(
                orderInvoiceNumber = invoice,
                productId = cartItem.product.id,
                productName = cartItem.product.name,
                sku = cartItem.product.sku,
                basePrice = cartItem.product.basePrice,
                costPrice = cartItem.product.costPrice,
                variantSummary = cartItem.variantSummary,
                unitPrice = cartItem.finalUnitPrice,
                quantity = cartItem.quantity,
                itemDiscount = cartItem.totalDiscountAmount,
                lineTotal = cartItem.lineTotal
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            repository.completeSale(order, orderItems)
            _activeCompletedOrder.value = Pair(order, orderItems)
            _isCheckoutModalOpen.value = false
            _isReceiptModalOpen.value = true
            clearCart()
        }
    }

    // Bluetooth Printer Methods
    fun refreshBluetoothPrinters(context: Context) {
        _pairedPrinters.value = EscPosPrinterService.getPairedBluetoothPrinters(context)
    }

    fun selectPrinter(address: String) {
        _selectedPrinterAddress.value = address
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
                footerMessage = rc.footerNote
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

        val jsonArray = JSONArray()
        for (item in items) {
            val obj = JSONObject().apply {
                put("productId", item.product.id)
                put("productName", item.product.name)
                put("productPrice", item.product.basePrice)
                put("productCost", item.product.costPrice)
                put("productCategory", item.product.categoryId)
                put("productUnit", item.product.unit)
                put("quantity", item.quantity)
                put("discountType", item.discountType.name)
                put("discountValue", item.discountValue)
                put("notes", item.notes)

                val variantsObj = JSONObject()
                for ((groupName, options) in item.selectedVariants) {
                    val optsArray = JSONArray()
                    for (opt in options) {
                        val o = JSONObject()
                        o.put("name", opt.name)
                        o.put("price", opt.priceAdjustment)
                        optsArray.put(o)
                    }
                    variantsObj.put(groupName, optsArray)
                }
                put("selectedVariants", variantsObj)
            }
            jsonArray.put(obj)
        }

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
                    val pId = obj.optLong("productId", 0L)
                    val pName = obj.optString("productName", "Produk")
                    val pPrice = obj.optDouble("productPrice", 0.0)
                    val pCost = obj.optDouble("productCost", 0.0)
                    val pCat = obj.optString("productCategory", "coffee")
                    val pUnit = obj.optString("productUnit", "cup")
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
                        stockQuantity = 999,
                        unit = pUnit
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
                openCartSheet()
            }
        }
    }

    fun deleteOpenBill(billId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteOpenBill(billId)
        }
    }
}
