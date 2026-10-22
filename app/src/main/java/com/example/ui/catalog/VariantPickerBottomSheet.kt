package com.example.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.model.ProductEntity
import com.example.domain.model.DiscountType
import com.example.domain.model.VariantGroup
import com.example.domain.model.VariantOption
import com.example.domain.model.VariantParser
import com.example.ui.components.CurrencyUtils
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandBlueLight
import com.example.ui.theme.CardBg
import com.example.ui.theme.SurfaceSubtle
import com.example.ui.theme.TextDarkMuted
import com.example.ui.theme.TextDarkPrimary
import com.example.ui.theme.TextDarkSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VariantPickerBottomSheet(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onAddToCart: (
        product: ProductEntity,
        selectedVariants: Map<String, List<VariantOption>>,
        quantity: Int,
        discountType: DiscountType,
        discountValue: Double,
        notes: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val variantGroups = remember(product.variantsJson) {
        VariantParser.parseVariantsJson(product.variantsJson)
    }

    val selectedOptions = remember {
        mutableStateMapOf<String, MutableList<VariantOption>>().apply {
            variantGroups.forEach { group ->
                if (!group.isMultiSelect && group.options.isNotEmpty()) {
                    this[group.name] = mutableListOf(group.options.first())
                } else {
                    this[group.name] = mutableListOf()
                }
            }
        }
    }

    var quantity by remember { mutableIntStateOf(1) }
    var discountType by remember { mutableStateOf(DiscountType.NONE) }
    var discountInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }

    val discountVal = remember(discountInput) {
        discountInput.toDoubleOrNull() ?: 0.0
    }

    val variantDelta by remember {
        derivedStateOf {
            selectedOptions.values.flatten().sumOf { it.priceAdjustment }
        }
    }

    val unitBaseWithVariants by remember {
        derivedStateOf {
            product.basePrice + variantDelta
        }
    }

    val itemDiscountAmount by remember {
        derivedStateOf {
            when (discountType) {
                DiscountType.PERCENTAGE -> (unitBaseWithVariants * (discountVal / 100.0)).coerceAtLeast(0.0)
                DiscountType.FIXED_IDR -> discountVal.coerceAtMost(unitBaseWithVariants).coerceAtLeast(0.0)
                DiscountType.NONE -> 0.0
            }
        }
    }

    val finalUnitPrice by remember {
        derivedStateOf {
            (unitBaseWithVariants - itemDiscountAmount).coerceAtLeast(0.0)
        }
    }

    val lineTotal by remember {
        derivedStateOf {
            finalUnitPrice * quantity
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkPrimary
                    )
                    Text(
                        text = "SKU: ${product.sku} • Stok: ${product.stockQuantity} ${product.unit}",
                        fontSize = 12.sp,
                        color = BrandBlue,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    if (product.description.isNotBlank()) {
                        Text(
                            text = product.description,
                            fontSize = 13.sp,
                            color = TextDarkSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SurfaceSubtle)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextDarkPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            HorizontalDivider(
                thickness = 0.5.dp,
                color = BorderSubtle,
                modifier = Modifier.padding(vertical = 14.dp)
            )

            // Variant Groups
            variantGroups.forEach { group ->
                Text(
                    text = group.name.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkSecondary,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    group.options.forEach { option ->
                        val isSelected = selectedOptions[group.name]?.any { it.name == option.name } == true

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) BrandBlueLight else SurfaceSubtle)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) BrandBlue else BorderSubtle,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    val list = selectedOptions.getOrPut(group.name) { mutableListOf() }
                                    if (group.isMultiSelect) {
                                        if (isSelected) {
                                            list.removeAll { it.name == option.name }
                                        } else {
                                            list.add(option)
                                        }
                                    } else {
                                        list.clear()
                                        list.add(option)
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 9.dp)
                                .testTag("variant_${option.name}")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = option.name,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) BrandBlue else TextDarkPrimary
                                )
                                if (option.priceAdjustment > 0) {
                                    Text(
                                        text = " (+${CurrencyUtils.formatIdrShort(option.priceAdjustment)})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) BrandBlue else TextDarkSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Item-Level Discount
            Text(
                text = "DISKON ITEM",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkSecondary,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    DiscountType.NONE to "Tanpa Disc",
                    DiscountType.PERCENTAGE to "% Disc",
                    DiscountType.FIXED_IDR to "Rp Disc"
                ).forEach { (type, label) ->
                    val selected = discountType == type
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) BrandBlue else SurfaceSubtle)
                            .clickable { discountType = type }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) Color.White else TextDarkPrimary
                        )
                    }
                }

                if (discountType != DiscountType.NONE) {
                    OutlinedTextField(
                        value = discountInput,
                        onValueChange = { discountInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        placeholder = { Text(if (discountType == DiscountType.PERCENTAGE) "e.g. 10" else "e.g. 5000", fontSize = 12.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandBlue),
                        singleLine = true
                    )
                }
            }

            // Quantity Stepper
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Jumlah Pesanan",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkPrimary
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceSubtle)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (quantity > 1) quantity-- },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease",
                            tint = if (quantity > 1) TextDarkPrimary else TextDarkMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "$quantity",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    IconButton(
                        onClick = { quantity++ },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase",
                            tint = BrandBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Add to Cart Button (Solid Brand Blue)
            Button(
                onClick = {
                    onAddToCart(
                        product,
                        selectedOptions.toMap(),
                        quantity,
                        discountType,
                        discountVal,
                        notesInput
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("add_to_cart_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tambah ke Pesanan",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = CurrencyUtils.formatIdr(lineTotal),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
