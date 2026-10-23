package com.example.ui.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.CartItem
import com.example.domain.model.DiscountType
import com.example.ui.PosViewModel
import com.example.ui.components.CurrencyUtils
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandBlueLight
import com.example.ui.theme.CardBg
import com.example.ui.theme.SurfaceSubtle
import com.example.ui.theme.TextDarkMuted
import com.example.ui.theme.TextDarkPrimary
import com.example.ui.theme.TextDarkSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartBottomSheet(
    viewModel: PosViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val cartSummary by viewModel.cartSummary.collectAsStateWithLifecycle()
    val customerName by viewModel.customerName.collectAsStateWithLifecycle()
    val orderType by viewModel.orderType.collectAsStateWithLifecycle()
    var isOpenBillModalOpen by remember { mutableStateOf(false) }

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
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Ringkasan Pesanan",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkPrimary
                    )
                    Text(
                        text = "${cartSummary.totalItemCount} item dipilih",
                        fontSize = 12.sp,
                        color = BrandBlue
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (cartSummary.items.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.clearCart() }
                        ) {
                            Text("Reset", color = Color(0xFFDC2626), fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Order Type Segmented Selector (Dine-in vs Takeaway)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceSubtle)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Dine-in Pill
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (orderType == "DINE_IN") BrandBlue else Color.Transparent)
                        .clickable { viewModel.setOrderType("DINE_IN") }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🍽️ Makan di Tempat",
                        fontSize = 13.sp,
                        fontWeight = if (orderType == "DINE_IN") FontWeight.Bold else FontWeight.Medium,
                        color = if (orderType == "DINE_IN") Color.White else TextDarkPrimary
                    )
                }

                // Takeaway Pill
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (orderType == "TAKEAWAY") BrandBlue else Color.Transparent)
                        .clickable { viewModel.setOrderType("TAKEAWAY") }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🥡 Bungkus (Takeaway)",
                        fontSize = 13.sp,
                        fontWeight = if (orderType == "TAKEAWAY") FontWeight.Bold else FontWeight.Medium,
                        color = if (orderType == "TAKEAWAY") Color.White else TextDarkPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Customer Name / Table Input
            OutlinedTextField(
                value = customerName,
                onValueChange = { viewModel.setCustomerName(it) },
                placeholder = { Text("Nama Pelanggan / Nomor Meja", fontSize = 13.sp, color = TextDarkMuted) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = BrandBlue,
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardBg,
                    unfocusedContainerColor = CardBg,
                    focusedBorderColor = BrandBlue
                ),
                singleLine = true
            )

            HorizontalDivider(
                thickness = 0.5.dp,
                color = BorderSubtle,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Items List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(cartSummary.items, key = { it.cartItemId }) { cartItem ->
                    CartItemRowClean(
                        item = cartItem,
                        onQuantityChange = { delta -> viewModel.updateCartItemQuantity(cartItem.cartItemId, delta) },
                        onRemove = { viewModel.removeCartItem(cartItem.cartItemId) }
                    )
                }
            }

            HorizontalDivider(
                thickness = 0.5.dp,
                color = BorderSubtle,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Order-Level Discount Selector
            Text(
                text = "DISKON TOTAL",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkSecondary,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple(DiscountType.NONE, 0.0, "0%"),
                    Triple(DiscountType.PERCENTAGE, 5.0, "5%"),
                    Triple(DiscountType.PERCENTAGE, 10.0, "10%"),
                    Triple(DiscountType.PERCENTAGE, 15.0, "15%"),
                    Triple(DiscountType.FIXED_IDR, 10000.0, "-10K")
                ).forEach { (type, value, label) ->
                    val isSelected = cartSummary.orderDiscountType == type &&
                            (type == DiscountType.NONE || cartSummary.orderDiscountValue == value)

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) BrandBlue else SurfaceSubtle)
                            .clickable { viewModel.setOrderDiscount(type, value) }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else TextDarkPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bill Breakdown Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceSubtle)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SummaryLineClean("Subtotal", CurrencyUtils.formatIdr(cartSummary.subtotal))
                if (cartSummary.itemDiscountTotal > 0) {
                    SummaryLineClean("Diskon Item", "-${CurrencyUtils.formatIdr(cartSummary.itemDiscountTotal)}", Color(0xFF16A34A))
                }
                if (cartSummary.orderDiscountAmount > 0) {
                    SummaryLineClean("Diskon Pesanan", "-${CurrencyUtils.formatIdr(cartSummary.orderDiscountAmount)}", Color(0xFF16A34A))
                }
                if (cartSummary.serviceFeeAmount > 0) {
                    SummaryLineClean("Biaya Layanan (${cartSummary.serviceFeePercentage.toInt()}%)", CurrencyUtils.formatIdr(cartSummary.serviceFeeAmount))
                }
                if (cartSummary.taxAmount > 0) {
                    SummaryLineClean("PB1 Pajak (${cartSummary.taxPercentage.toInt()}%)", CurrencyUtils.formatIdr(cartSummary.taxAmount))
                }

                HorizontalDivider(thickness = 0.5.dp, color = BorderSubtle, modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total Pembayaran",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkPrimary
                    )
                    Text(
                        text = CurrencyUtils.formatIdr(cartSummary.grandTotal),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BrandBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons: Open Bill and Proceed to Payment
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { isOpenBillModalOpen = true },
                    enabled = cartSummary.items.isNotEmpty(),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, if (cartSummary.items.isNotEmpty()) BrandBlue else BorderSubtle),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("open_bill_hold_btn"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandBlue)
                ) {
                    Icon(imageVector = Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Open Bill",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = { viewModel.openCheckoutModal() },
                    enabled = cartSummary.items.isNotEmpty(),
                    modifier = Modifier
                        .weight(1.8f)
                        .height(52.dp)
                        .testTag("proceed_checkout_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Bayar Sekarang",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (isOpenBillModalOpen) {
        SaveOpenBillModal(
            initialOrderType = orderType,
            onDismiss = { isOpenBillModalOpen = false },
            onSave = { cust, tbl, oType, notes ->
                viewModel.saveCurrentCartAsOpenBill(
                    customerName = cust,
                    tableNumber = tbl,
                    orderType = oType,
                    notes = notes
                )
                isOpenBillModalOpen = false
            }
        )
    }
}

@Composable
fun CartItemRowClean(
    item: CartItem,
    onQuantityChange: (Int) -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.product.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkPrimary
            )
            if (item.variantSummary.isNotBlank()) {
                Text(
                    text = item.variantSummary,
                    fontSize = 12.sp,
                    color = TextDarkSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Text(
                text = CurrencyUtils.formatIdr(item.finalUnitPrice),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = BrandBlue,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceSubtle)
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onQuantityChange(-1) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (item.quantity == 1) Icons.Default.DeleteOutline else Icons.Default.Remove,
                        contentDescription = "Decrease",
                        tint = if (item.quantity == 1) Color(0xFFDC2626) else TextDarkPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = "${item.quantity}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(
                    onClick = { onQuantityChange(1) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase",
                        tint = BrandBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryLineClean(label: String, value: String, valueColor: Color = TextDarkPrimary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = TextDarkSecondary)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}
