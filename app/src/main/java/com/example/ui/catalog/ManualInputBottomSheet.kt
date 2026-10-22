package com.example.ui.catalog

import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.local.model.CategoryEntity
import com.example.data.local.model.ProductEntity
import com.example.ui.components.CurrencyUtils
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.CardBg
import com.example.ui.theme.SurfaceSubtle
import com.example.ui.theme.TextDarkMuted
import com.example.ui.theme.TextDarkPrimary
import com.example.ui.theme.TextDarkSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ManualInputBottomSheet(
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onAddToCart: (ProductEntity, Int, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var itemName by remember { mutableStateOf("") }
    var itemPriceText by remember { mutableStateOf("") }
    var itemCostPriceText by remember { mutableStateOf("") }
    var quantity by remember { mutableIntStateOf(1) }
    var selectedCategoryId by remember {
        mutableStateOf(categories.firstOrNull()?.id ?: "coffee")
    }
    var itemNotes by remember { mutableStateOf("") }

    val price = itemPriceText.toDoubleOrNull() ?: 0.0
    val costPrice = itemCostPriceText.toDoubleOrNull() ?: (price * 0.4) // default 40% if left empty
    val totalAmount = price * quantity
    val isValid = itemName.isNotBlank() && price > 0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = CardBg,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(BorderSubtle)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .verticalScroll(rememberScrollState())
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
                        text = "Input Pesanan Manual",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextDarkPrimary
                    )
                    Text(
                        text = "Tambahkan item non-katalog atau harga kustom",
                        fontSize = 12.sp,
                        color = TextDarkSecondary
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextDarkSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Nama Produk / Item
            Text(
                text = "Nama Menu / Item",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = itemName,
                onValueChange = { itemName = it },
                placeholder = { Text("Contoh: Biji Kopi Gayo 200g, Custom Pastry", fontSize = 13.sp, color = TextDarkMuted) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandBlue,
                    unfocusedBorderColor = BorderSubtle
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("manual_item_name_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Harga Jual (Rp)
            Text(
                text = "Harga Jual (Rp)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = itemPriceText,
                onValueChange = { itemPriceText = it.filter { char -> char.isDigit() } },
                placeholder = { Text("0", fontSize = 13.sp, color = TextDarkMuted) },
                prefix = { Text("Rp ", fontWeight = FontWeight.Bold, color = BrandBlue) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandBlue,
                    unfocusedBorderColor = BorderSubtle
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("manual_price_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Harga Modal / HPP (Rp) - Opsional untuk Laba Rugi
            Text(
                text = "Harga Modal / HPP (Opsional untuk Laba Rugi)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = itemCostPriceText,
                onValueChange = { itemCostPriceText = it.filter { char -> char.isDigit() } },
                placeholder = { Text("Otomatis 40% jika kosong", fontSize = 13.sp, color = TextDarkMuted) },
                prefix = { Text("Rp ", fontWeight = FontWeight.Bold, color = TextDarkSecondary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandBlue,
                    unfocusedBorderColor = BorderSubtle
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Responsive FlowRow for Category Chips Selection
            Text(
                text = "Pilih Kategori",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = selectedCategoryId == cat.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) BrandBlue else SurfaceSubtle)
                            .clickable { selectedCategoryId = cat.id }
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                    ) {
                        Text(
                            text = cat.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else TextDarkSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Quantity Stepper
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Jumlah (Qty)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkPrimary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (quantity > 1) BrandBlue else BorderSubtle)
                            .clickable(enabled = quantity > 1) { quantity-- },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Minus", tint = Color.White, modifier = Modifier.size(18.dp))
                    }

                    Text(
                        text = "$quantity",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkPrimary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(BrandBlue)
                            .clickable { quantity++ },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Plus", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Notes
            Text(
                text = "Catatan Pesanan (Opsional)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = itemNotes,
                onValueChange = { itemNotes = it },
                placeholder = { Text("Contoh: Gula pisah, kemasan takeaway", fontSize = 13.sp, color = TextDarkMuted) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandBlue,
                    unfocusedBorderColor = BorderSubtle
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Add to Cart Button
            Button(
                onClick = {
                    if (isValid) {
                        val customProduct = ProductEntity(
                            id = System.currentTimeMillis() * -1,
                            sku = "MAN-${System.currentTimeMillis() % 10000}",
                            name = itemName.trim(),
                            categoryId = selectedCategoryId,
                            basePrice = price,
                            costPrice = costPrice,
                            stockQuantity = 999,
                            iconName = "coffee",
                            variantsJson = "[]"
                        )
                        onAddToCart(customProduct, quantity, itemNotes)
                        onDismiss()
                    }
                },
                enabled = isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_manual_item_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandBlue,
                    disabledContainerColor = BorderSubtle
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tambahkan ke Keranjang", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Text(
                        text = CurrencyUtils.formatIdr(totalAmount),
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
