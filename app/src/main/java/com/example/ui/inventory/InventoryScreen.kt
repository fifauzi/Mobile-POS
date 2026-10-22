package com.example.ui.inventory

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import com.example.ui.components.ProductFallbackAvatar
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.model.CategoryEntity
import com.example.data.local.model.ProductEntity
import com.example.data.local.model.VariantGroupEntity
import com.example.ui.PosViewModel
import com.example.ui.catalog.getCategoryIcon
import com.example.ui.components.CurrencyUtils
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
import com.example.ui.variants.VariantManagementScreen
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun InventoryScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val variantGroups by viewModel.variantGroups.collectAsStateWithLifecycle()

    var selectedCategoryId by remember { mutableStateOf("all") }
    var isAddProductSheetOpen by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var productToDelete by remember { mutableStateOf<ProductEntity?>(null) }

    val filtered = remember(products, selectedCategoryId) {
        if (selectedCategoryId == "all") products else products.filter { it.categoryId == selectedCategoryId }
    }

    val lowStockCount = remember(products) {
        products.count { it.stockQuantity <= 15 }
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
            // Header: Responsive Title & + Tambah Button (image_618d62.png)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                ) {
                    Text(
                        text = "Katalog Produk",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextDarkPrimary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Manajemen stok & modal HPP produk",
                        fontSize = 12.sp,
                        color = TextDarkSecondary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = { isAddProductSheetOpen = true },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("add_new_product_btn")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tambah", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

                // Low Stock Warning Banner
                if (lowStockCount > 0) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(StockOrangeBg)
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = StockOrangeText,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$lowStockCount produk membutuhkan restok segera!",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = StockOrangeText
                            )
                        }
                    }
                }

                // Category Tabs (Horizontal Scroll)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isAllSelected = selectedCategoryId == "all"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isAllSelected) BrandBlue else CardBg)
                            .border(1.dp, if (isAllSelected) BrandBlue else BorderSubtle, RoundedCornerShape(18.dp))
                            .clickable { selectedCategoryId = "all" }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Semua (${products.size})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAllSelected) Color.White else TextDarkPrimary
                        )
                    }

                    categories.forEach { cat ->
                        val isSelected = selectedCategoryId == cat.id
                        val count = products.count { it.categoryId == cat.id }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isSelected) BrandBlue else CardBg)
                                .border(1.dp, if (isSelected) BrandBlue else BorderSubtle, RoundedCornerShape(18.dp))
                                .clickable { selectedCategoryId = cat.id }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "${cat.name} ($count)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else TextDarkPrimary
                            )
                        }
                    }
                }

                // Products List
                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Tidak ada produk dalam kategori ini",
                            fontSize = 14.sp,
                            color = TextDarkSecondary
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filtered, key = { it.id }) { product ->
                            InventoryItemCardClean(
                                product = product,
                                onStockAdjust = { delta ->
                                    val newStock = (product.stockQuantity + delta).coerceAtLeast(0)
                                    viewModel.updateStock(product.id, newStock)
                                },
                                onEdit = { editingProduct = product },
                                onDelete = { productToDelete = product }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(100.dp))
                        }
                    }
            }
        }
    }

    // Add / Edit Modal Sheet
    if (isAddProductSheetOpen) {
        AddEditProductSheetClean(
            initialProduct = null,
            categories = categories,
            availableVariantGroups = variantGroups,
            onDismiss = { isAddProductSheetOpen = false },
            onSave = { newProd ->
                viewModel.addProduct(newProd)
                isAddProductSheetOpen = false
            }
        )
    }

    editingProduct?.let { prod ->
        AddEditProductSheetClean(
            initialProduct = prod,
            categories = categories,
            availableVariantGroups = variantGroups,
            onDismiss = { editingProduct = null },
            onSave = { updated ->
                viewModel.updateProduct(updated)
                editingProduct = null
            }
        )
    }

    // Delete Confirmation Dialog
    productToDelete?.let { prod ->
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("Hapus Produk", fontWeight = FontWeight.Bold, color = TextDarkPrimary) },
            text = { Text("Yakin ingin menghapus '${prod.name}' dari katalog?", color = TextDarkSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProduct(prod)
                        productToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Hapus", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text("Batal", color = TextDarkSecondary)
                }
            }
        )
    }
}

@Composable
fun InventoryItemCardClean(
    product: ProductEntity,
    onStockAdjust: (Int) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    ProductFallbackAvatar(
                        productName = product.name,
                        size = 48.dp,
                        cornerRadius = 14.dp,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = product.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDarkPrimary
                        )
                        Text(
                            text = "SKU: ${product.sku} • Jual: ${CurrencyUtils.formatIdr(product.basePrice)}",
                            fontSize = 12.sp,
                            color = TextDarkSecondary
                        )
                        if (product.costPrice > 0) {
                            Text(
                                text = "HPP: ${CurrencyUtils.formatIdr(product.costPrice)} • Untung: ${CurrencyUtils.formatIdr(product.basePrice - product.costPrice)}",
                                fontSize = 11.sp,
                                color = Color(0xFF15803D),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = TextDarkSecondary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                    }
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = BorderSubtle, modifier = Modifier.padding(vertical = 10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Stok: ",
                        fontSize = 13.sp,
                        color = TextDarkSecondary
                    )
                    Text(
                        text = "${product.stockQuantity} ${product.unit}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (product.stockQuantity <= 15) StockOrangeText else StockGreenText
                    )
                }

                // Quick Stock Adjustment Buttons
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceSubtle)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CardBg)
                            .clickable { onStockAdjust(-1) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("-1", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDarkPrimary)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CardBg)
                            .clickable { onStockAdjust(5) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("+5", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CardBg)
                            .clickable { onStockAdjust(20) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("+20", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditProductSheetClean(
    initialProduct: ProductEntity?,
    categories: List<CategoryEntity>,
    availableVariantGroups: List<VariantGroupEntity>,
    onDismiss: () -> Unit,
    onSave: (ProductEntity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    var sku by remember { mutableStateOf(initialProduct?.sku ?: ("CF-" + (100..999).random())) }
    var selectedCategoryId by remember { mutableStateOf(initialProduct?.categoryId ?: (categories.firstOrNull()?.id ?: "coffee")) }
    var basePriceStr by remember { mutableStateOf(initialProduct?.basePrice?.toLong()?.toString() ?: "35000") }
    var costPriceStr by remember { mutableStateOf(initialProduct?.costPrice?.toLong()?.toString() ?: "14000") }
    var stockQuantityStr by remember { mutableStateOf(initialProduct?.stockQuantity?.toString() ?: "50") }
    var unit by remember { mutableStateOf(initialProduct?.unit ?: "cup") }
    var description by remember { mutableStateOf(initialProduct?.description ?: "") }

    var isCategoryDropdownOpen by remember { mutableStateOf(false) }

    // Track which variant group names are linked to this product
    val linkedVariantGroupNames = remember {
        mutableStateListOf<String>().apply {
            if (initialProduct != null && initialProduct.variantsJson.isNotBlank()) {
                val parsed = com.example.domain.model.VariantParser.parseVariantsJson(initialProduct.variantsJson)
                addAll(parsed.map { it.name })
            } else {
                // Default coffee options linked
                addAll(listOf("Size", "Type", "Beans Type", "Sugar Level", "Add-ons"))
            }
        }
    }

    val selectedCategoryName = remember(categories, selectedCategoryId) {
        categories.find { it.id == selectedCategoryId }?.name ?: "Pilih Kategori"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = CardBg,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 38.dp, height = 5.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFCBD5E1))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialProduct != null) "Edit Produk" else "Tambah Produk Baru",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkPrimary
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SurfaceSubtle)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextDarkPrimary, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Nama Produk
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nama Produk") },
                placeholder = { Text("Contoh: Spanish Latte, Croissant") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandBlue,
                    unfocusedBorderColor = BorderSubtle
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Functional Category Dropdown Selector
            Text("Kategori Produk", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDarkPrimary)
            Spacer(modifier = Modifier.height(6.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                        .clickable { isCategoryDropdownOpen = true }
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedCategoryName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDarkPrimary
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Dropdown",
                        tint = BrandBlue
                    )
                }

                DropdownMenu(
                    expanded = isCategoryDropdownOpen,
                    onDismissRequest = { isCategoryDropdownOpen = false },
                    modifier = Modifier.background(CardBg)
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.name, fontWeight = if (selectedCategoryId == cat.id) FontWeight.Bold else FontWeight.Normal) },
                            onClick = {
                                selectedCategoryId = cat.id
                                isCategoryDropdownOpen = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = sku,
                    onValueChange = { sku = it },
                    label = { Text("SKU / Kode") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandBlue, unfocusedBorderColor = BorderSubtle)
                )

                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Satuan (cup/pcs/bag)") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandBlue, unfocusedBorderColor = BorderSubtle)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = basePriceStr,
                    onValueChange = { basePriceStr = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Harga Jual (Rp)") },
                    prefix = { Text("Rp ", fontWeight = FontWeight.Bold, color = BrandBlue) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandBlue, unfocusedBorderColor = BorderSubtle)
                )

                OutlinedTextField(
                    value = costPriceStr,
                    onValueChange = { costPriceStr = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Harga Modal / HPP") },
                    prefix = { Text("Rp ", fontWeight = FontWeight.Bold, color = TextDarkSecondary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandBlue, unfocusedBorderColor = BorderSubtle)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = stockQuantityStr,
                onValueChange = { stockQuantityStr = it.filter { ch -> ch.isDigit() } },
                label = { Text("Jumlah Stok") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandBlue, unfocusedBorderColor = BorderSubtle)
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Deskripsi Singkat") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandBlue, unfocusedBorderColor = BorderSubtle)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Link Variant Groups Section
            Text(
                text = "Tautkan Varian Produk:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkPrimary
            )
            Text(
                text = "Pilih varian yang berlaku untuk produk ini (misal Size, Beans, Grind)",
                fontSize = 11.sp,
                color = TextDarkSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableVariantGroups.forEach { grp ->
                    val isLinked = grp.name in linkedVariantGroupNames
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isLinked) BrandBlueLight else SurfaceSubtle)
                            .border(1.dp, if (isLinked) BrandBlue else BorderSubtle, RoundedCornerShape(14.dp))
                            .clickable {
                                if (isLinked) {
                                    linkedVariantGroupNames.remove(grp.name)
                                } else {
                                    linkedVariantGroupNames.add(grp.name)
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isLinked) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = BrandBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = grp.name,
                                fontSize = 12.sp,
                                fontWeight = if (isLinked) FontWeight.Bold else FontWeight.Medium,
                                color = if (isLinked) BrandBlue else TextDarkPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val price = basePriceStr.toDoubleOrNull() ?: 35000.0
                    val cost = costPriceStr.toDoubleOrNull() ?: (price * 0.4)
                    val stock = stockQuantityStr.toIntOrNull() ?: 50

                    // Construct variantsJson from linked groups
                    val selectedGroups = availableVariantGroups.filter { it.name in linkedVariantGroupNames }
                    val jsonArray = JSONArray()
                    for (grp in selectedGroups) {
                        val grpObj = JSONObject()
                        grpObj.put("name", grp.name)
                        grpObj.put("isMultiSelect", grp.isMultiSelect)
                        val opts = com.example.ui.variants.parseOptionsFromJson(grp.optionsJson)
                        val optsArray = JSONArray()
                        for (opt in opts) {
                            val optObj = JSONObject()
                            optObj.put("name", opt.name)
                            optObj.put("price", opt.priceAdjustment)
                            optsArray.put(optObj)
                        }
                        grpObj.put("options", optsArray)
                        jsonArray.put(grpObj)
                    }

                    val updated = (initialProduct ?: ProductEntity(
                        sku = sku,
                        name = name.trim(),
                        categoryId = selectedCategoryId,
                        basePrice = price,
                        costPrice = cost,
                        stockQuantity = stock,
                        unit = unit,
                        description = description,
                        variantsJson = jsonArray.toString()
                    )).copy(
                        name = name.trim(),
                        sku = sku.trim(),
                        categoryId = selectedCategoryId,
                        basePrice = price,
                        costPrice = cost,
                        stockQuantity = stock,
                        unit = unit,
                        description = description,
                        variantsJson = jsonArray.toString()
                    )
                    onSave(updated)
                },
                enabled = name.isNotBlank() && sku.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (initialProduct != null) "Simpan Perubahan" else "Tambah Produk",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
