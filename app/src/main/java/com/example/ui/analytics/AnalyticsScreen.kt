package com.example.ui.analytics

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.PosViewModel
import com.example.ui.components.CurrencyUtils
import com.example.ui.components.DateTimeUtils
import com.example.ui.components.IosSegmentedControl
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandBlueLight
import com.example.ui.theme.CardBg
import com.example.ui.theme.ScaffoldBg
import com.example.ui.theme.SurfaceSubtle
import com.example.ui.theme.TextDarkMuted
import com.example.ui.theme.TextDarkPrimary
import com.example.ui.theme.TextDarkSecondary

/**
 * Analytics & Profit/Loss (Laba Rugi) Report Screen matching image_711ee7d3.png:
 * - Responsive Export CSV button fitting mobile viewports cleanly
 * - Comprehensive Profit & Loss (Laba Rugi) breakdown: Omzet, HPP, Laba Kotor, Pajak, Laba Bersih, Margin %
 * - Top Selling Products with exact variant-aware revenue
 * - Payment breakdown (Cash, Dynamic QRIS, Bank Transfer)
 * - Session & Shift summary
 */
@Composable
fun AnalyticsScreen(
    viewModel: PosViewModel,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val allOrderItems by viewModel.allOrderItems.collectAsStateWithLifecycle()
    val outletInfo by viewModel.outletInfo.collectAsStateWithLifecycle()

    var selectedTabIdx by remember { mutableIntStateOf(0) }
    var filterLabel by remember { mutableStateOf("Hari ini") }
    var isDateDropdownOpen by remember { mutableStateOf(false) }
    var isEmailDialogVisible by remember { mutableStateOf(false) }

    val filteredOrders = remember(orders, filterLabel) {
        val now = System.currentTimeMillis()
        when (filterLabel) {
            "Hari ini" -> {
                val startOfToday = DateTimeUtils.getStartOfDayWib(now)
                val endOfToday = DateTimeUtils.getEndOfDayWib(now)
                orders.filter { DateTimeUtils.getOrderEffectiveTimestamp(it) in startOfToday..endOfToday }
            }
            "7 hari terakhir" -> {
                val start = DateTimeUtils.getStartOfDayDaysAgoWib(6, now)
                orders.filter { DateTimeUtils.getOrderEffectiveTimestamp(it) >= start }
            }
            "14 hari terakhir" -> {
                val start = DateTimeUtils.getStartOfDayDaysAgoWib(13, now)
                orders.filter { DateTimeUtils.getOrderEffectiveTimestamp(it) >= start }
            }
            "30 hari terakhir" -> {
                val start = DateTimeUtils.getStartOfDayDaysAgoWib(29, now)
                orders.filter { DateTimeUtils.getOrderEffectiveTimestamp(it) >= start }
            }
            "Bulan ini" -> {
                val cal = java.util.Calendar.getInstance(DateTimeUtils.WIB_ZONE).apply {
                    timeInMillis = now
                    set(java.util.Calendar.DAY_OF_MONTH, 1)
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                orders.filter { DateTimeUtils.getOrderEffectiveTimestamp(it) >= cal.timeInMillis }
            }
            else -> orders
        }
    }

    val totalRevenue = remember(filteredOrders) {
        filteredOrders.sumOf { it.grandTotal }
    }

    val totalSubtotal = remember(filteredOrders) {
        filteredOrders.sumOf { it.subtotal }
    }

    val totalDiscounts = remember(filteredOrders) {
        filteredOrders.sumOf { it.itemDiscountTotal + it.orderDiscountAmount }
    }

    val totalTax = remember(filteredOrders) {
        filteredOrders.sumOf { it.taxAmount }
    }

    val totalOrdersCount = filteredOrders.size

    // Calculate Total HPP (Cost of Goods Sold) for filtered orders
    val totalCogsHpp = remember(filteredOrders, allOrderItems) {
        val invoiceSet = filteredOrders.map { it.invoiceNumber }.toSet()
        allOrderItems.filter { it.orderInvoiceNumber in invoiceSet }
            .sumOf { it.costPrice * it.quantity }
    }

    val grossProfit = remember(totalSubtotal, totalDiscounts, totalCogsHpp) {
        (totalSubtotal - totalDiscounts) - totalCogsHpp
    }

    val netProfit = remember(grossProfit) {
        grossProfit
    }

    val profitMarginPct = remember(totalRevenue, netProfit) {
        if (totalRevenue > 0) (netProfit / totalRevenue) * 100.0 else 0.0
    }

    val shift1Orders = remember(filteredOrders) {
        filteredOrders.filter { com.example.data.local.SettingsStorageService.normalizeShift(it.shiftName) == "Shift 1" }
    }
    val shift2Orders = remember(filteredOrders) {
        filteredOrders.filter { com.example.data.local.SettingsStorageService.normalizeShift(it.shiftName) == "Shift 2" }
    }
    val eventOrders = remember(filteredOrders) {
        filteredOrders.filter { com.example.data.local.SettingsStorageService.normalizeShift(it.shiftName) == "Event" }
    }

    val paymentBreakdown = remember(filteredOrders) {
        val map = mutableMapOf("CASH" to 0.0, "QRIS" to 0.0, "BANK_TRANSFER" to 0.0)
        filteredOrders.forEach { ord ->
            map[ord.paymentMethod] = (map[ord.paymentMethod] ?: 0.0) + ord.grandTotal
        }
        map
    }

    val topProducts = remember(allOrderItems, filteredOrders) {
        val filteredInvoices = filteredOrders.map { it.invoiceNumber }.toSet()
        allOrderItems.filter { it.orderInvoiceNumber in filteredInvoices }
            .groupBy { it.productName }
            .map { (name, items) ->
                val qty = items.sumOf { it.quantity }
                val rev = items.sumOf { it.lineTotal }
                Triple(name, qty, rev)
            }
            .sortedByDescending { it.second }
            .take(5)
    }

    val employeeSalesBreakdown = remember(filteredOrders) {
        filteredOrders.groupBy { it.cashierName.ifBlank { "Rian Pratama" } }
            .map { (name, list) ->
                val count = list.size
                val rev = list.sumOf { it.grandTotal }
                Triple(name, count, rev)
            }
            .sortedByDescending { it.third }
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // Header: Responsive Title & Responsive Export CSV Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali",
                                tint = TextDarkPrimary
                            )
                        }
                    }
                    Text(
                        text = "Laporan Transaksi",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextDarkPrimary
                    )
                }

                // Action Buttons: Send to Email & Download CSV
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { isEmailDialogVisible = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("send_email_report_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Email",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Button(
                        onClick = { viewModel.exportSalesCsv(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("export_csv_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Unduh CSV",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Tabs: "Pendapatan" / "Pencairan"
            IosSegmentedControl(
                items = listOf("Pendapatan", "Pencairan"),
                selectedIndex = selectedTabIdx,
                onItemSelected = { idx, _ -> selectedTabIdx = idx }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Store Selector Pill ("🏪 Caffeinery Senopati ⌵")
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = null,
                            tint = BrandBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "${outletInfo.name} (${outletInfo.branch})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDarkPrimary
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = BrandBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Inline Date Range Filter ("📅 Periode Laporan: Hari ini ⌵")
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isDateDropdownOpen = true }
            ) {
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = BrandBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Periode Laporan",
                                    fontSize = 11.sp,
                                    color = TextDarkSecondary
                                )
                                Text(
                                    text = filterLabel,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDarkPrimary
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = BrandBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = isDateDropdownOpen,
                        onDismissRequest = { isDateDropdownOpen = false },
                        modifier = Modifier.background(CardBg)
                    ) {
                        listOf("Hari ini", "7 hari terakhir", "14 hari terakhir", "30 hari terakhir", "Bulan ini", "Semua waktu").forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt, fontWeight = if (filterLabel == opt) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    filterLabel = opt
                                    isDateDropdownOpen = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 💰 REKAP LABA RUGI (PROFIT & LOSS CARD)
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE8F8F0)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MonetizationOn,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Rekap Laba Rugi (P&L)",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDarkPrimary
                            )
                        }

                        // Margin Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE8F8F0))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Margin: ${String.format(java.util.Locale.US, "%.1f", profitMarginPct)}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Laba Bersih Highlight
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFF0FDF4))
                            .border(1.dp, Color(0xFFBBF7D0), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "TOTAL LABA BERSIH",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF166534),
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CurrencyUtils.formatIdr(netProfit),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF15803D)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Breakdown Rows
                    ProfitLossRow("Total Omzet Penjualan", CurrencyUtils.formatIdr(totalRevenue), TextDarkPrimary, true)
                    Spacer(modifier = Modifier.height(6.dp))
                    ProfitLossRow("Total Diskon Pelanggan", "-${CurrencyUtils.formatIdr(totalDiscounts)}", Color(0xFFDC2626), false)
                    Spacer(modifier = Modifier.height(6.dp))
                    ProfitLossRow("Harga Pokok Penjualan (HPP / Modal)", "-${CurrencyUtils.formatIdr(totalCogsHpp)}", Color(0xFFEA580C), false)
                    Spacer(modifier = Modifier.height(6.dp))
                    ProfitLossRow("Pajak PB1 Restoran (10%)", CurrencyUtils.formatIdr(totalTax), TextDarkSecondary, false)
                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(thickness = 0.5.dp, color = BorderSubtle)
                    Spacer(modifier = Modifier.height(6.dp))
                    ProfitLossRow("Laba Kotor (Gross Profit)", CurrencyUtils.formatIdr(grossProfit), BrandBlue, true)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Schedule / Session Cards (image_711ee7d3.png layout)
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Ringkasan Shift & Sesi",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkSecondary
                    )

                    val shift1Cashier = shift1Orders.map { it.cashierName }.distinct().joinToString().ifBlank { "Rian Pratama" }
                    LaporanScheduleRow(
                        title = "Shift 1 - Kasir: $shift1Cashier",
                        subtitle = "Sesi Pagi / Utama",
                        transCount = "${shift1Orders.size} transaksi",
                        amount = CurrencyUtils.formatIdr(shift1Orders.sumOf { it.grandTotal })
                    )

                    val shift2Cashier = shift2Orders.map { it.cashierName }.distinct().joinToString().ifBlank { "Maya Sari" }
                    LaporanScheduleRow(
                        title = "Shift 2 - Kasir: $shift2Cashier",
                        subtitle = "Sesi Sore / Penutupan",
                        transCount = "${shift2Orders.size} transaksi",
                        amount = CurrencyUtils.formatIdr(shift2Orders.sumOf { it.grandTotal })
                    )

                    if (eventOrders.isNotEmpty()) {
                        val eventCashier = eventOrders.map { it.cashierName }.distinct().joinToString().ifBlank { "Tim Event" }
                        LaporanScheduleRow(
                            title = "Event - Kasir: $eventCashier",
                            subtitle = "Sesi Khusus / Event",
                            transCount = "${eventOrders.size} transaksi",
                            amount = CurrencyUtils.formatIdr(eventOrders.sumOf { it.grandTotal })
                        )
                    }

                    // "Penarikan Instan >" box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.5.dp, BrandBlue, RoundedCornerShape(16.dp))
                            .background(BrandBlueLight.copy(alpha = 0.4f))
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Pencairan Saldo (QRIS & EDC)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDarkPrimary
                                )
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = BrandBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Saldo Siap Cair",
                                    fontSize = 12.sp,
                                    color = TextDarkSecondary
                                )
                                Text(
                                    text = CurrencyUtils.formatIdr(totalRevenue * 0.7),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF16A34A)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Omnibus / Per-Employee Sales Summary Card
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("employee_sales_summary_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Kinerja Kasir & Karyawan",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDarkPrimary
                            )
                            Text(
                                text = "Breakdown omzet & transaksi per pegawai",
                                fontSize = 11.sp,
                                color = TextDarkSecondary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(BrandBlueLight.copy(alpha = 0.5f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${employeeSalesBreakdown.size} Kasir",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandBlue
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (employeeSalesBreakdown.isEmpty()) {
                        Text(
                            text = "Belum ada transaksi tercatat",
                            fontSize = 12.sp,
                            color = TextDarkMuted
                        )
                    } else {
                        employeeSalesBreakdown.forEachIndexed { idx, (empName, txCount, empRev) ->
                            val percentage = if (totalRevenue > 0) (empRev / totalRevenue).toFloat() else 0f
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(if (idx == 0) BrandBlue else Color(0xFFE2E8F0)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = empName.take(1).uppercase(),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (idx == 0) Color.White else TextDarkPrimary
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = empName,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextDarkPrimary
                                            )
                                            Text(
                                                text = "$txCount transaksi • ${(percentage * 100).toInt()}% kontribusi",
                                                fontSize = 11.sp,
                                                color = TextDarkSecondary
                                            )
                                        }
                                    }

                                    Text(
                                        text = CurrencyUtils.formatIdr(empRev),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandBlue
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { percentage },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (idx == 0) BrandBlue else Color(0xFF38BDF8),
                                    trackColor = SurfaceSubtle
                                )
                            }
                            if (idx < employeeSalesBreakdown.size - 1) {
                                HorizontalDivider(
                                    thickness = 0.5.dp,
                                    color = BorderSubtle,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Payment Methods Breakdown Card
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(
                        text = "Metode Pembayaran",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkPrimary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val cashRev = paymentBreakdown["CASH"] ?: 0.0
                    val qrisRev = paymentBreakdown["QRIS"] ?: 0.0
                    val bankRev = paymentBreakdown["BANK_TRANSFER"] ?: 0.0
                    val total = (cashRev + qrisRev + bankRev).coerceAtLeast(1.0)

                    PaymentBarRowClean("Tunai (Cash)", cashRev, (cashRev / total).toFloat(), Color(0xFF0052CC))
                    Spacer(modifier = Modifier.height(10.dp))
                    PaymentBarRowClean("QRIS Dinamis", qrisRev, (qrisRev / total).toFloat(), Color(0xFFE11D48))
                    Spacer(modifier = Modifier.height(10.dp))
                    PaymentBarRowClean("Transfer Bank", bankRev, (bankRev / total).toFloat(), Color(0xFF059669))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Top Selling Products Card (Rank #1, #2, etc.)
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(
                        text = "🏆 Menu Terlaris ($filterLabel)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkPrimary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (topProducts.isEmpty()) {
                        Text(
                            text = "Belum ada item terjual",
                            fontSize = 13.sp,
                            color = TextDarkMuted
                        )
                    } else {
                        topProducts.forEachIndexed { index, (name, qty, rev) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(if (index == 0) BrandBlue else SurfaceSubtle),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "#${index + 1}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (index == 0) Color.White else TextDarkSecondary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(text = name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDarkPrimary)
                                        Text(text = "$qty terjual", fontSize = 12.sp, color = TextDarkSecondary)
                                    }
                                }

                                Text(
                                    text = CurrencyUtils.formatIdr(rev),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandBlue
                                )
                            }
                            if (index < topProducts.size - 1) {
                                HorizontalDivider(thickness = 0.5.dp, color = BorderSubtle, modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    if (isEmailDialogVisible) {
        var emailInput by remember { mutableStateOf("bdscoffeecompany@gmail.com") }
        AlertDialog(
            onDismissRequest = { isEmailDialogVisible = false },
            title = {
                Text(
                    text = "Kirim Laporan ke Email",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextDarkPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "File spreadsheet CSV laporan transaksi penjualan lengkap akan dikirim langsung ke alamat email:",
                        fontSize = 13.sp,
                        color = TextDarkSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Email Penerima") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandBlue,
                            unfocusedBorderColor = BorderSubtle
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (emailInput.isNotBlank()) {
                            viewModel.sendSalesReportEmail(context, emailInput.trim())
                            isEmailDialogVisible = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Kirim Sekarang", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isEmailDialogVisible = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun ProfitLossRow(label: String, value: String, valueColor: Color, isBold: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = if (isBold) TextDarkPrimary else TextDarkSecondary
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@Composable
fun LaporanScheduleRow(title: String, subtitle: String, transCount: String, amount: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceSubtle)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDarkPrimary)
            Text(text = subtitle, fontSize = 11.sp, color = TextDarkSecondary)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = amount, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
            Text(text = transCount, fontSize = 11.sp, color = TextDarkMuted)
        }
    }
}

@Composable
fun PaymentBarRowClean(method: String, amount: Double, progress: Float, color: Color) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = method, fontSize = 13.sp, color = TextDarkSecondary)
            Text(text = CurrencyUtils.formatIdr(amount), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDarkPrimary)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = color,
            trackColor = SurfaceSubtle,
        )
    }
}
