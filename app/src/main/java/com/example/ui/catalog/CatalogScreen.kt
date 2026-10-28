package com.example.ui.catalog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ReceiptLong
import com.example.ui.cart.OpenBillsListModal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.model.CategoryEntity
import com.example.data.local.model.ProductEntity
import com.example.ui.PosViewModel
import com.example.ui.components.CashierShiftSwitcherModal
import com.example.ui.components.CurrencyUtils
import com.example.ui.components.ProductFallbackAvatar
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandBlueLight
import com.example.ui.theme.CardBg
import com.example.ui.theme.ScaffoldBg
import com.example.ui.theme.StockGreenBg
import com.example.ui.theme.StockGreenText
import com.example.ui.theme.StockOrangeBg
import com.example.ui.theme.StockOrangeText
import com.example.ui.theme.StockRedBg
import com.example.ui.theme.StockRedText
import com.example.ui.theme.SurfaceSubtle
import com.example.ui.theme.TextDarkMuted
import com.example.ui.theme.TextDarkPrimary
import com.example.ui.theme.TextDarkSecondary

@Composable
fun CatalogScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val filteredProducts by viewModel.filteredProducts.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val cartSummary by viewModel.cartSummary.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val selectedProductForVariant by viewModel.selectedProductForVariant.collectAsStateWithLifecycle()
    val variantGroups by viewModel.variantGroups.collectAsStateWithLifecycle()
    val activeOpenBills by viewModel.activeOpenBills.collectAsStateWithLifecycle()
    val activeEmployee by viewModel.activeEmployee.collectAsStateWithLifecycle()
    val activeShift by viewModel.activeShift.collectAsStateWithLifecycle()
    val employees by viewModel.employees.collectAsStateWithLifecycle()
    val wasDraftRestored by viewModel.wasDraftRestored.collectAsStateWithLifecycle()

    var isCategoryDropdownOpen by remember { mutableStateOf(false) }
    var isManualInputSheetOpen by remember { mutableStateOf(false) }
    var isOpenBillsModalOpen by remember { mutableStateOf(false) }
    var isCashierShiftModalOpen by remember { mutableStateOf(false) }

    // Map product ID to current quantity in cart
    val cartQtyMap = remember(cartItems) {
        cartItems.groupBy { it.product.id }.mapValues { (_, items) -> items.sumOf { it.quantity } }
    }

    val selectedCategoryName = remember(categories, selectedCategory) {
        if (selectedCategory == "all") "Semua" else categories.find { it.id == selectedCategory }?.name ?: "Semua"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ScaffoldBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header (Kasir, BDS Coffee Company, Shift info, + Stacked Open Bill & Input Manual)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "Kasir",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextDarkPrimary
                    )
                    Text(
                        text = "BDS Coffee Company",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0099FF),
                        modifier = Modifier.padding(top = 1.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // Active Cashier & Shift interactive badge pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(BrandBlueLight.copy(alpha = 0.5f))
                            .border(1.dp, BrandBlue.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                            .clickable { isCashierShiftModalOpen = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("cashier_shift_pill_btn"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF22C55E))
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "${activeEmployee.name} • ${activeShift.split(" ").firstOrNull() ?: "Shift"}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandBlue,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Switch Cashier",
                            tint = BrandBlue,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Vertically stacked Open Bill & Input Manual controls with safe padding
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Open Bills Counter Badge Button (Stacked Top)
                    if (activeOpenBills.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFFEF3C7))
                                .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(20.dp))
                                .clickable { isOpenBillsModalOpen = true }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                .testTag("open_bills_badge_btn"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = Color(0xFFB45309),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Open Bill (${activeOpenBills.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309)
                            )
                        }
                    }

                    // "+ Input Manual" action button (Stacked Bottom)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(BrandBlueLight.copy(alpha = 0.5f))
                            .border(1.dp, BrandBlue.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                            .clickable { isManualInputSheetOpen = true }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("input_manual_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(BrandBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Input Manual",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandBlue
                        )
                    }
                }
            }

            // 🔄 Draft Restored Notification Banner
            AnimatedVisibility(
                visible = wasDraftRestored && cartItems.isNotEmpty()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEFF6FF))
                        .border(1.dp, Color(0xFF93C5FD), RoundedCornerShape(12.dp))
                        .clickable { viewModel.openCartSheet() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔄", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Draft Pesanan Dipulihkan (${cartSummary.totalItemCount} item)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandBlue
                            )
                            Text(
                                text = "Data keranjang dari sesi sebelumnya berhasil dimuat otomatis.",
                                fontSize = 10.sp,
                                color = TextDarkSecondary
                            )
                        }
                    }
                    IconButton(
                        onClick = { viewModel.dismissDraftRestoredBanner() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = BrandBlue,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Search Bar & Category Dropdown (image_53be3fd4.png style)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Search Pill Input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Cari produk...", fontSize = 13.sp, color = TextDarkMuted) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = BrandBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = TextDarkMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("search_input"),
                    shape = RoundedCornerShape(27.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardBg,
                        unfocusedContainerColor = CardBg,
                        focusedBorderColor = BrandBlue,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextDarkPrimary,
                        unfocusedTextColor = TextDarkPrimary
                    ),
                    singleLine = true
                )

                // Category Dropdown Pill ("Semua ⌵")
                Box {
                    Row(
                        modifier = Modifier
                            .height(54.dp)
                            .clip(RoundedCornerShape(27.dp))
                            .background(CardBg)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(27.dp))
                            .clickable { isCategoryDropdownOpen = true }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedCategoryName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDarkPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Filter",
                            tint = BrandBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = isCategoryDropdownOpen,
                        onDismissRequest = { isCategoryDropdownOpen = false },
                        modifier = Modifier.background(CardBg)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Semua Kategori", fontWeight = if (selectedCategory == "all") FontWeight.Bold else FontWeight.Normal) },
                            onClick = {
                                viewModel.selectCategory("all")
                                isCategoryDropdownOpen = false
                            }
                        )
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name, fontWeight = if (selectedCategory == cat.id) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    viewModel.selectCategory(cat.id)
                                    isCategoryDropdownOpen = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Product Cards List (image_53be3fd4.png layout)
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Coffee,
                            contentDescription = null,
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Produk tidak ditemukan",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDarkPrimary
                        )
                        Text(
                            text = "Coba kata kunci atau kategori lain",
                            fontSize = 13.sp,
                            color = TextDarkSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 4.dp,
                        bottom = if (cartSummary.items.isNotEmpty()) 80.dp else 20.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        val qty = cartQtyMap[product.id] ?: 0
                        ProductCardYokke(
                            product = product,
                            currentQty = qty,
                            onCardClick = { viewModel.openVariantPicker(product) },
                            onIncrement = { viewModel.openVariantPicker(product) },
                            onDecrement = {
                                // Find any item in cart for this product and decrement
                                val matching = cartItems.find { it.product.id == product.id }
                                if (matching != null) {
                                    viewModel.updateCartItemQuantity(matching.cartItemId, -1)
                                }
                            }
                        )
                    }
                }
            }
        }

        // Floating Solid Blue Order Summary Bar (image_53be3fd4.png)
        AnimatedVisibility(
            visible = cartSummary.items.isNotEmpty(),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp, start = 16.dp, end = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(28.dp),
                        spotColor = BrandBlue.copy(alpha = 0.4f)
                    )
                    .clip(RoundedCornerShape(28.dp))
                    .background(BrandBlue)
                    .clickable { viewModel.openCartSheet() }
                    .padding(horizontal = 22.dp)
                    .testTag("floating_cart_bar")
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${cartSummary.totalItemCount} Produk",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = CurrencyUtils.formatIdr(cartSummary.grandTotal),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = "Cart",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    // Variant Picker Bottom Sheet
    selectedProductForVariant?.let { product ->
        VariantPickerBottomSheet(
            product = product,
            globalVariantGroups = variantGroups,
            onDismiss = { viewModel.closeVariantPicker() },
            onAddToCart = { prod, variants, qty, discType, discVal, notes ->
                viewModel.addToCart(prod, variants, qty, discType, discVal, notes)
            }
        )
    }

    // Manual Input Custom Product Bottom Sheet
    if (isManualInputSheetOpen) {
        ManualInputBottomSheet(
            categories = categories,
            onDismiss = { isManualInputSheetOpen = false },
            onAddToCart = { prod, qty, notes ->
                viewModel.addToCart(prod, emptyMap(), qty, notes = notes)
            }
        )
    }

    // Active Open Bills Modal List
    if (isOpenBillsModalOpen) {
        OpenBillsListModal(
            openBills = activeOpenBills,
            onDismiss = { isOpenBillsModalOpen = false },
            onRecallBill = { bill ->
                viewModel.recallOpenBill(bill)
                isOpenBillsModalOpen = false
            },
            onDeleteBill = { billId ->
                viewModel.deleteOpenBill(billId)
            }
        )
    }

    // Cashier & Shift Switcher Modal
    if (isCashierShiftModalOpen) {
        CashierShiftSwitcherModal(
            employees = employees,
            currentEmployee = activeEmployee,
            currentShift = activeShift,
            onDismiss = { isCashierShiftModalOpen = false },
            onConfirmSwitch = { emp, shift ->
                viewModel.setActiveEmployee(emp)
                viewModel.setActiveShift(shift)
            }
        )
    }
}

/**
 * Product Card exact reproduction of image_53be3fd4.png:
 * White background, 20.dp rounded corners, thumbnail with conditional "B2B" badge,
 * Title, Price, "Stok X" pill, Wholesale text, and solid blue circular stepper buttons!
 */
@Composable
fun ProductCardYokke(
    product: ProductEntity,
    currentQty: Int,
    onCardClick: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Card(
        onClick = onCardClick,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product_card_${product.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Product Thumbnail with conditional B2B Badge & 2-letter capital initial avatar
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    ProductFallbackAvatar(
                        productName = product.name,
                        size = 80.dp,
                        cornerRadius = 16.dp,
                        fontSize = 24.sp,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Conditional B2B badge in top-left
                    if (product.hasB2bPricing) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(bottomEnd = 10.dp, topStart = 16.dp))
                                .background(Color(0xFF2563EB))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "B2B",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Product Details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = CurrencyUtils.formatIdr(product.basePrice),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextDarkPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Stock Status Pill ("Stok 100" or "Hampir habis (5)")
                    val isLowStock = product.stockQuantity <= 10
                    val stockBg = if (isLowStock) StockOrangeBg else StockGreenBg
                    val stockColor = if (isLowStock) StockOrangeText else StockGreenText
                    val stockLabel = if (isLowStock) "Hampir habis (${product.stockQuantity})" else "Stok ${product.stockQuantity}"

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(stockBg)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stockLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = stockColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Subtext & Blue Circular Stepper
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (product.hasB2bPricing && product.b2bTier1Price > 0) {
                    val tier1Str = "≥${product.b2bTier1MinQty}: ${CurrencyUtils.formatIdrShort(product.b2bTier1Price)}"
                    val tier2Str = if (product.b2bTier2Price > 0) " • ≥${product.b2bTier2MinQty}: ${CurrencyUtils.formatIdrShort(product.b2bTier2Price)}" else ""
                    Text(
                        text = "B2B: $tier1Str$tier2Str",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2563EB),
                        modifier = Modifier.weight(1f, fill = false)
                    )
                } else {
                    Spacer(modifier = Modifier.width(4.dp))
                }

                // Solid Blue Circular Quantity Adjustment Control
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Minus Button
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (currentQty > 0) BrandBlue else Color(0xFFE2E8F0))
                            .clickable(enabled = currentQty > 0) { onDecrement() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease",
                            tint = if (currentQty > 0) Color.White else Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Quantity Number
                    Text(
                        text = "$currentQty",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkPrimary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    // Solid Blue Plus Button
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(BrandBlue)
                            .clickable { onIncrement() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

fun getCategoryIcon(name: String): ImageVector {
    return when (name.lowercase()) {
        "coffee" -> Icons.Default.Coffee
        "local_cafe" -> Icons.Default.LocalCafe
        "grain" -> Icons.Default.Grain
        "bakery_dining" -> Icons.Default.BakeryDining
        "inventory_2" -> Icons.Default.Inventory2
        else -> Icons.Default.Coffee
    }
}
