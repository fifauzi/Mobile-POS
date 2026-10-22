package com.example.ui.checkout

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode2
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.service.QrisPayloadGenerator
import com.example.ui.PosViewModel
import com.example.ui.components.CurrencyUtils
import com.example.ui.components.IosSegmentedControl
import com.example.ui.components.QrCodeCanvas
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandBlueLight
import com.example.ui.theme.CardBg
import com.example.ui.theme.SurfaceSubtle
import com.example.ui.theme.TextDarkMuted
import com.example.ui.theme.TextDarkPrimary
import com.example.ui.theme.TextDarkSecondary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class PaymentTab(val label: String) {
    CASH("Tunai"),
    QRIS("QRIS"),
    TRANSFER("Transfer Bank")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CheckoutModal(
    viewModel: PosViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val cartSummary by viewModel.cartSummary.collectAsStateWithLifecycle()
    val grandTotal = cartSummary.grandTotal

    var selectedTab by remember { mutableStateOf(PaymentTab.CASH) }

    // Cash states
    var cashTenderedInput by remember { mutableStateOf("") }
    val cashTendered = remember(cashTenderedInput, grandTotal) {
        if (cashTenderedInput.isBlank()) grandTotal else (cashTenderedInput.toDoubleOrNull() ?: grandTotal)
    }
    val changeAmount by remember {
        derivedStateOf {
            (cashTendered - grandTotal).coerceAtLeast(0.0)
        }
    }

    // Dynamic QRIS states
    val tempInvoiceNumber = remember {
        "INV-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
    }
    val qrisPayload = remember(grandTotal) {
        QrisPayloadGenerator.generateDynamicQris(
            invoiceNumber = tempInvoiceNumber,
            amount = grandTotal,
            merchantName = "CAFFEINERY COFFEE",
            merchantCity = "JAKARTA",
            postalCode = "12190"
        )
    }
    var qrisSecondsRemaining by remember { mutableIntStateOf(300) }

    LaunchedEffect(selectedTab) {
        if (selectedTab == PaymentTab.QRIS) {
            while (qrisSecondsRemaining > 0) {
                delay(1000)
                qrisSecondsRemaining--
            }
        }
    }

    // Bank Transfer states
    var selectedBank by remember { mutableStateOf("BCA") }
    var isTransferConfirmed by remember { mutableStateOf(false) }

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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Tagihan",
                        fontSize = 12.sp,
                        color = TextDarkSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = CurrencyUtils.formatIdr(grandTotal),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BrandBlue
                    )
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

            Spacer(modifier = Modifier.height(14.dp))

            // Segmented Control
            IosSegmentedControl(
                items = PaymentTab.entries,
                selectedIndex = PaymentTab.entries.indexOf(selectedTab),
                onItemSelected = { _, tab -> selectedTab = tab },
                itemLabel = { it.label }
            )

            HorizontalDivider(
                thickness = 0.5.dp,
                color = BorderSubtle,
                modifier = Modifier.padding(vertical = 14.dp)
            )

            // Content per tab
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "PaymentTabAnimation"
            ) { tab ->
                when (tab) {
                    PaymentTab.CASH -> {
                        CashPaymentClean(
                            grandTotal = grandTotal,
                            cashTendered = cashTendered,
                            changeAmount = changeAmount,
                            customInput = cashTenderedInput,
                            onCustomInputChange = { cashTenderedInput = it },
                            onPresetSelected = { preset -> cashTenderedInput = preset.toLong().toString() },
                            onComplete = {
                                viewModel.completeCheckout("CASH", cashTendered)
                            }
                        )
                    }

                    PaymentTab.QRIS -> {
                        DynamicQrisClean(
                            grandTotal = grandTotal,
                            payload = qrisPayload,
                            invoice = tempInvoiceNumber,
                            secondsLeft = qrisSecondsRemaining,
                            onCopyPayload = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("QRIS Payload", qrisPayload)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "QRIS EMVCo payload berhasil disalin", Toast.LENGTH_SHORT).show()
                            },
                            onConfirmPayment = {
                                viewModel.completeCheckout("QRIS", grandTotal)
                            }
                        )
                    }

                    PaymentTab.TRANSFER -> {
                        BankTransferClean(
                            grandTotal = grandTotal,
                            selectedBank = selectedBank,
                            onSelectBank = { selectedBank = it },
                            isConfirmed = isTransferConfirmed,
                            onToggleConfirmed = { isTransferConfirmed = !isTransferConfirmed },
                            onCopyAccount = { accountNum ->
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Bank Account", accountNum)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Nomor rekening berhasil disalin", Toast.LENGTH_SHORT).show()
                            },
                            onComplete = {
                                viewModel.completeCheckout("BANK_TRANSFER", grandTotal, notes = "Bank: $selectedBank")
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CashPaymentClean(
    grandTotal: Double,
    cashTendered: Double,
    changeAmount: Double,
    customInput: String,
    onCustomInputChange: (String) -> Unit,
    onPresetSelected: (Double) -> Unit,
    onComplete: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "UANG PAS & PECAHAN CEPAT",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextDarkSecondary,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        val presets = remember(grandTotal) {
            val list = mutableListOf(grandTotal)
            val rounded50k = (Math.ceil(grandTotal / 50000.0) * 50000.0).coerceAtLeast(50000.0)
            if (!list.contains(rounded50k)) list.add(rounded50k)
            listOf(100000.0, 200000.0, 500000.0).forEach { p ->
                if (p > grandTotal && !list.contains(p)) list.add(p)
            }
            list.sorted()
        }

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { preset ->
                val isSelected = cashTendered == preset
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) BrandBlueLight else SurfaceSubtle)
                        .border(1.dp, if (isSelected) BrandBlue else BorderSubtle, RoundedCornerShape(12.dp))
                        .clickable { onPresetSelected(preset) }
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = if (preset == grandTotal) "Uang Pas (${CurrencyUtils.formatIdrShort(preset)})" else CurrencyUtils.formatIdr(preset),
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) BrandBlue else TextDarkPrimary
                    )
                }
            }
        }

        Text(
            text = "ATAU KETIK JUMLAH UANG TUNAI",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextDarkSecondary,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        OutlinedTextField(
            value = customInput,
            onValueChange = { onCustomInputChange(it.filter { ch -> ch.isDigit() }) },
            placeholder = { Text("cth. 100000", fontSize = 13.sp, color = TextDarkMuted) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Payments,
                    contentDescription = null,
                    tint = BrandBlue,
                    modifier = Modifier.size(20.dp)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("cash_tendered_input"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CardBg,
                unfocusedContainerColor = CardBg,
                focusedBorderColor = BrandBlue
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Change Calculation Card
        val isSufficient = cashTendered >= grandTotal
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (isSufficient) Color(0xFFDCFCE7) else Color(0xFFFEE2E2))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isSufficient) "KEMBALIAN" else "UANG KURANG",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSufficient) Color(0xFF15803D) else Color(0xFFB91C1C),
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = if (isSufficient) CurrencyUtils.formatIdr(changeAmount) else "Kurang ${CurrencyUtils.formatIdr(grandTotal - cashTendered)}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isSufficient) Color(0xFF14532D) else Color(0xFF7F1D1D)
                )
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isSufficient) Color(0xFF15803D) else Color(0xFFB91C1C)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSufficient) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = onComplete,
            enabled = isSufficient,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("complete_cash_sale_btn"),
            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Selesaikan Pembayaran Tunai",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun DynamicQrisClean(
    grandTotal: Double,
    payload: String,
    invoice: String,
    secondsLeft: Int,
    onCopyPayload: () -> Unit,
    onConfirmPayment: () -> Unit
) {
    val minutes = secondsLeft / 60
    val seconds = secondsLeft % 60
    val timerStr = String.format(Locale.US, "%02d:%02d", minutes, seconds)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(BrandBlueLight)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.QrCode2,
                contentDescription = null,
                tint = BrandBlue,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "QRIS DINAMIS • OTOMATIS TERCATAT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = BrandBlue
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .size(220.dp)
                .testTag("dynamic_qris_qr_container"),
            contentAlignment = Alignment.Center
        ) {
            QrCodeCanvas(payload = payload)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "CAFFEINERY SPECIALTY",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = TextDarkPrimary
        )
        Text(
            text = "NMID: ID1024356789012 • $invoice",
            fontSize = 11.sp,
            color = TextDarkSecondary
        )

        Text(
            text = "Berlaku hingga $timerStr",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (secondsLeft < 60) Color(0xFFDC2626) else BrandBlue,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceSubtle)
                .clickable(onClick = onCopyPayload)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = null,
                tint = BrandBlue,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Salin String QRIS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextDarkPrimary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onConfirmPayment,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("confirm_qris_payment_btn"),
            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Simulasi Pembayaran Berhasil • ${CurrencyUtils.formatIdr(grandTotal)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun BankTransferClean(
    grandTotal: Double,
    selectedBank: String,
    onSelectBank: (String) -> Unit,
    isConfirmed: Boolean,
    onToggleConfirmed: () -> Unit,
    onCopyAccount: (String) -> Unit,
    onComplete: () -> Unit
) {
    val bankAccounts = remember {
        mapOf(
            "BCA" to "8271 0029 4821",
            "Mandiri" to "137 00 1928374 1",
            "BRI" to "0192 01 002938 50 2"
        )
    }
    val currentAccount = bankAccounts[selectedBank] ?: "8271 0029 4821"

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "PILIH BANK TUJUAN",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextDarkSecondary,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("BCA", "Mandiri", "BRI").forEach { bank ->
                val isSelected = bank == selectedBank
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) BrandBlueLight else SurfaceSubtle)
                        .border(1.dp, if (isSelected) BrandBlue else BorderSubtle, RoundedCornerShape(12.dp))
                        .clickable { onSelectBank(bank) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = bank,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) BrandBlue else TextDarkPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Account Details Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceSubtle)
                .padding(16.dp)
        ) {
            Text(
                text = "Nama Penerima: PT CAFFEINERY INDONESIA",
                fontSize = 12.sp,
                color = TextDarkSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentAccount,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = BrandBlue
                )

                IconButton(
                    onClick = { onCopyAccount(currentAccount) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = BrandBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (isConfirmed) Color(0xFFDCFCE7) else SurfaceSubtle)
                .clickable(onClick = onToggleConfirmed)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isConfirmed) Color(0xFF15803D) else Color(0xFFCBD5E1)),
                contentAlignment = Alignment.Center
            ) {
                if (isConfirmed) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Kasir telah memeriksa mutasi / bukti transfer",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (isConfirmed) Color(0xFF14532D) else TextDarkPrimary
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = onComplete,
            enabled = isConfirmed,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("complete_transfer_sale_btn"),
            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Konfirmasi Pembayaran Transfer",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
