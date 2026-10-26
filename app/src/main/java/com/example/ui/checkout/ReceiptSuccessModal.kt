package com.example.ui.checkout

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.PrintDisabled
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.model.OrderEntity
import com.example.data.local.model.OrderItemEntity
import com.example.service.CsvExportService
import com.example.ui.PosViewModel
import com.example.ui.components.CurrencyUtils
import com.example.ui.components.PosTab
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandBlueLight
import com.example.ui.theme.CardBg
import com.example.ui.theme.ScaffoldBg
import com.example.ui.theme.TextDarkMuted
import com.example.ui.theme.TextDarkPrimary
import com.example.ui.theme.TextDarkSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptSuccessModal(
    order: OrderEntity,
    items: List<OrderItemEntity>,
    viewModel: PosViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isPrinting by viewModel.isPrintingAnimation.collectAsStateWithLifecycle()
    val printerStatus by viewModel.printerStatusMessage.collectAsStateWithLifecycle()
    val outletInfo by viewModel.outletInfo.collectAsStateWithLifecycle()
    val receiptConfig by viewModel.receiptCustomizer.collectAsStateWithLifecycle()
    val showPrinterDisconnectedAlert by viewModel.showPrinterDisconnectedAlert.collectAsStateWithLifecycle()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = CardBg,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 42.dp, height = 5.dp)
                    .clip(CircleShape)
                    .background(BorderSubtle)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Success Vibrant Badge
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF22C55E)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Success",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Pembayaran Berhasil! 🎉",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextDarkPrimary
            )

            Text(
                text = "${order.invoiceNumber} • ${order.paymentMethod}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrandBlue
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Thermal Paper Receipt View (Clean high contrast paper)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(18.dp), spotColor = Color.Black.copy(alpha = 0.08f))
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFFAFAFA))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
                    .padding(18.dp)
                    .testTag("thermal_receipt_preview")
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = outletInfo.name.uppercase(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "${outletInfo.address} (${outletInfo.branch})",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = "Telp: ${outletInfo.phone}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF64748B)
                    )

                    ReceiptDashedLine()

                    ReceiptRowMonospace("INVOICE", order.invoiceNumber)
                    val orderTypeLabel = if (order.orderType == "TAKEAWAY") "BUNGKUS (TAKEAWAY)" else "MAKAN DI TEMPAT (DINE-IN)"
                    ReceiptRowMonospace("TIPE PESANAN", orderTypeLabel)
                    val dateFormatted = SimpleDateFormat("dd/MM/yyyy HH:mm:ss 'WIB'", Locale("id", "ID")).format(Date(order.timestamp))
                    ReceiptRowMonospace("KASIR", order.cashierName.ifBlank { "Rian Pratama" })
                    ReceiptRowMonospace("PELANGGAN", order.customerName.ifEmpty { "Pelanggan Walk-in" })
                    ReceiptRowMonospace("METODE", order.paymentMethod)

                    ReceiptDashedLine()

                    // Line Items
                    items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${item.quantity}x ${item.productName}",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = CurrencyUtils.formatIdr(item.lineTotal),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        if (item.variantSummary.isNotEmpty()) {
                            Text(
                                text = "  ${item.variantSummary}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF64748B)
                            )
                        }

                        if (item.itemDiscount > 0) {
                            Text(
                                text = "  Diskon: -${CurrencyUtils.formatIdr(item.itemDiscount)}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFE11D48)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    ReceiptDashedLine()

                    // Subtotal & Breakdown
                    ReceiptRowMonospace("SUBTOTAL", CurrencyUtils.formatIdr(order.subtotal))
                    val totalDiscount = order.itemDiscountTotal + order.orderDiscountAmount
                    if (totalDiscount > 0) {
                        ReceiptRowMonospace("TOTAL DISKON", "-${CurrencyUtils.formatIdr(totalDiscount)}")
                    }
                    if (order.serviceFeeAmount > 0) {
                        ReceiptRowMonospace("BIAYA LAYANAN (${order.serviceFeePercentage.toInt()}%)", CurrencyUtils.formatIdr(order.serviceFeeAmount))
                    }
                    if (receiptConfig.showTax && order.taxAmount > 0) {
                        ReceiptRowMonospace("PAJAK PB1 (${order.taxPercentage.toInt()}%)", CurrencyUtils.formatIdr(order.taxAmount))
                    }

                    ReceiptDashedLine()

                    // Grand Total
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "TOTAL BAYAR",
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = CurrencyUtils.formatIdr(order.grandTotal),
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            color = BrandBlue
                        )
                    }

                    if (order.paymentMethod == "CASH") {
                        ReceiptRowMonospace("TUNAI", CurrencyUtils.formatIdr(order.tenderedAmount))
                        ReceiptRowMonospace("KEMBALI", CurrencyUtils.formatIdr(order.changeAmount))
                    }

                    ReceiptDashedLine()

                    Text(
                        text = receiptConfig.footerNote,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = "Simpan struk ini sebagai bukti pembayaran sah",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Printer Status feedback
            if (printerStatus.isNotEmpty()) {
                Text(
                    text = printerStatus,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (printerStatus.contains("error", ignoreCase = true)) Color(0xFFE11D48) else Color(0xFF16A34A)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // WhatsApp Receipt Button (Vibrant WhatsApp Green)
            Button(
                onClick = {
                    com.example.service.PdfReceiptService.shareReceiptToWhatsApp(
                        context = context,
                        order = order,
                        items = items,
                        outletInfo = outletInfo,
                        receiptConfig = receiptConfig
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("whatsapp_receipt_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Kirim Struk ke WA (WhatsApp)", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Print Thermal Receipt Button (BrandBlue Button)
            Button(
                onClick = {
                    viewModel.attemptPrintReceipt(context, order, items)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("print_receipt_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isPrinting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mencetak Struk...", color = Color.White, fontWeight = FontWeight.Bold)
                } else {
                    Icon(imageVector = Icons.Default.Print, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cetak Struk Fisik (ESC/POS)", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        com.example.service.PdfReceiptService.sharePdfReceipt(
                            context = context,
                            order = order,
                            items = items,
                            outletInfo = outletInfo,
                            receiptConfig = receiptConfig
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("share_pdf_btn"),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = TextDarkPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Unduh PDF", color = TextDarkPrimary, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("new_order_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlueLight),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Transaksi Baru", color = BrandBlue, fontWeight = FontWeight.ExtraBold)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    // Bluetooth Disconnected Alert Dialog Guard
    if (showPrinterDisconnectedAlert) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissPrinterDisconnectedAlert() },
            icon = {
                Icon(
                    imageVector = Icons.Default.PrintDisabled,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Printer Belum Terhubung",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = TextDarkPrimary
                )
            },
            text = {
                Text(
                    text = "Printer belum terhubung. Silakan sambungkan printer Bluetooth terlebih dahulu.",
                    fontSize = 14.sp,
                    color = TextDarkSecondary,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissPrinterDisconnectedAlert()
                        try {
                            val intent = Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            viewModel.setCurrentTab(PosTab.SETTINGS)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Pengaturan Bluetooth", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissPrinterDisconnectedAlert() }
                ) {
                    Text("Tutup", fontWeight = FontWeight.SemiBold, color = TextDarkSecondary)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = CardBg
        )
    }
}

@Composable
fun ReceiptRowMonospace(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF64748B))
        Text(text = value, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF0F172A))
    }
}

@Composable
fun ReceiptDashedLine() {
    Text(
        text = "- - - - - - - - - - - - - - - - - - - - - - - - - -",
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
        color = Color(0xFFCBD5E1),
        modifier = Modifier.padding(vertical = 4.dp),
        maxLines = 1
    )
}
