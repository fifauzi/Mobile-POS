package com.example.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.ui.components.CashierShiftSwitcherModal
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.model.OrderEntity
import com.example.ui.PosViewModel
import com.example.ui.components.CurrencyUtils
import com.example.ui.components.DateTimeUtils
import com.example.ui.components.PosTab
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandBlueLight
import com.example.ui.theme.CardBg
import com.example.ui.theme.PastelBlueBg
import com.example.ui.theme.PastelBlueIcon
import com.example.ui.theme.PastelGreenBg
import com.example.ui.theme.PastelGreenIcon
import com.example.ui.theme.PastelOrangeBg
import com.example.ui.theme.PastelOrangeIcon
import com.example.ui.theme.PastelPurpleBg
import com.example.ui.theme.PastelPurpleIcon
import com.example.ui.theme.ScaffoldBg
import com.example.ui.theme.StockOrangeBg
import com.example.ui.theme.StockOrangeText
import com.example.ui.theme.SurfaceSubtle
import com.example.ui.theme.TextDarkMuted
import com.example.ui.theme.TextDarkPrimary
import com.example.ui.theme.TextDarkSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Beranda / Dashboard Overview Screen:
 * - Quick stats: Today's Revenue, Order count, Average ticket
 * - Quick Action Buttons: Mulai Kasir, Kelola Stok, Unduh CSV, Cek Printer
 * - Top Selling Items Today with progress metrics
 * - Low Stock Alerts
 * - Recent Invoices Feed
 */
@Composable
fun DashboardScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val allOrderItems by viewModel.allOrderItems.collectAsStateWithLifecycle()
    val activeEmployee by viewModel.activeEmployee.collectAsStateWithLifecycle()
    val activeShift by viewModel.activeShift.collectAsStateWithLifecycle()
    val employees by viewModel.employees.collectAsStateWithLifecycle()

    var isShiftModalOpen by remember { mutableStateOf(false) }

    val todayOrders = remember(orders) {
        val now = System.currentTimeMillis()
        val startOfToday = DateTimeUtils.getStartOfDayWib(now)
        val endOfToday = DateTimeUtils.getEndOfDayWib(now)
        orders.filter { order ->
            val effTime = DateTimeUtils.getOrderEffectiveTimestamp(order)
            effTime in startOfToday..endOfToday
        }
    }

    val totalRevenue = remember(todayOrders) {
        todayOrders.sumOf { it.grandTotal }
    }

    val todayOrdersCount = todayOrders.size
    val averageTicket = remember(totalRevenue, todayOrdersCount) {
        if (todayOrdersCount > 0) totalRevenue / todayOrdersCount else 0.0
    }

    val todayCogsHpp = remember(todayOrders, allOrderItems) {
        val invoiceSet = todayOrders.map { it.invoiceNumber }.toSet()
        allOrderItems.filter { it.orderInvoiceNumber in invoiceSet }
            .sumOf { it.costPrice * it.quantity }
    }
    val todayNetProfit = remember(totalRevenue, todayCogsHpp) {
        (totalRevenue - todayCogsHpp).coerceAtLeast(0.0)
    }

    val lowStockProducts = remember(products) {
        products.filter { it.stockQuantity <= 10 }.take(4)
    }

    val todayTopProducts = remember(todayOrders, allOrderItems) {
        val todayInvoices = todayOrders.map { it.invoiceNumber }.toSet()
        val itemsForToday = allOrderItems.filter { it.orderInvoiceNumber in todayInvoices }
        val sourceItems = if (itemsForToday.isNotEmpty()) itemsForToday else allOrderItems

        sourceItems.groupBy { it.productName }
            .map { (name, items) ->
                val qty = items.sumOf { it.quantity }
                val rev = items.sumOf { it.lineTotal }
                Triple(name, qty, rev)
            }
            .sortedByDescending { it.second }
            .take(5)
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
            // Header Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "BDS Coffee Company",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlue,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Dashboard",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextDarkPrimary
                    )
                }

                // Active Shift Status Pill (Clickable to switch cashier & shift)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFE8F8F0))
                        .border(1.dp, Color(0xFF86EFAC), RoundedCornerShape(20.dp))
                        .clickable { isShiftModalOpen = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("dashboard_shift_pill_btn")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF22C55E))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${activeEmployee.name} • ${activeShift.split(" ").firstOrNull() ?: "Shift"} ⌵",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF15803D)
                        )
                    }
                }
            }

            // Hero Sales Card (Clean modern white card with soft shadow)
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_hero_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TOTAL PENDAPATAN HARI INI",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextDarkSecondary,
                            letterSpacing = 0.8.sp
                        )

                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = Color(0xFF22C55E),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = CurrencyUtils.formatIdr(totalRevenue),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextDarkPrimary
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE8F8F0))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Est. Laba Bersih",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF166534)
                                )
                                Text(
                                    text = CurrencyUtils.formatIdr(todayNetProfit),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF15803D)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(thickness = 0.5.dp, color = BorderSubtle)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Secondary Metrics in Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Total Transaksi", fontSize = 11.sp, color = TextDarkSecondary)
                            Text(
                                text = "$todayOrdersCount Pesanan",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDarkPrimary
                            )
                        }

                        Column {
                            Text(text = "Rata-rata / Nota", fontSize = 11.sp, color = TextDarkSecondary)
                            Text(
                                text = CurrencyUtils.formatIdr(averageTicket),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandBlue
                            )
                        }

                        Column {
                            Text(text = "Metode Populer", fontSize = 11.sp, color = TextDarkSecondary)
                            Text(
                                text = "QRIS Dinamis",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Quick Actions Section Title
            Text(
                text = "Aksi Cepat",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // 4 Grid Quick Action Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Action 1: Buka Kasir
                QuickActionButton(
                    title = "Buka Kasir",
                    subtitle = "Mulai order",
                    icon = Icons.Default.PointOfSale,
                    iconBg = BrandBlueLight,
                    iconTint = BrandBlue,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.setCurrentTab(PosTab.KASIR) }
                )

                // Action 2: Kelola Stok
                QuickActionButton(
                    title = "Katalog & Stok",
                    subtitle = "${products.size} produk",
                    icon = Icons.Default.Inventory2,
                    iconBg = PastelOrangeBg,
                    iconTint = PastelOrangeIcon,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.setCurrentTab(PosTab.KATALOG) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Action 3: Riwayat Transaksi
                QuickActionButton(
                    title = "Riwayat Nota",
                    subtitle = "$todayOrdersCount nota selesai",
                    icon = Icons.Default.ReceiptLong,
                    iconBg = PastelGreenBg,
                    iconTint = PastelGreenIcon,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.setCurrentTab(PosTab.TRANSAKSI) }
                )

                // Action 4: Unduh CSV
                QuickActionButton(
                    title = "Ekspor CSV",
                    subtitle = "Laporan Excel",
                    icon = Icons.Default.FileDownload,
                    iconBg = PastelPurpleBg,
                    iconTint = PastelPurpleIcon,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.exportSalesCsv(context) }
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Low Stock Warning Card (if applicable)
            if (lowStockProducts.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = StockOrangeBg),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
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
                                    text = "Perhatian Stok Menipis (${lowStockProducts.size})",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StockOrangeText
                                )
                            }

                            Text(
                                text = "Restok ➔",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = StockOrangeText,
                                modifier = Modifier.clickable { viewModel.setCurrentTab(PosTab.KATALOG) }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        lowStockProducts.forEach { item ->
                            Text(
                                text = "• ${item.name} — Sisa ${item.stockQuantity} unit",
                                fontSize = 12.sp,
                                color = Color(0xFF9A3412),
                                modifier = Modifier.padding(vertical = 1.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Top Products Section: Menu Terlaris Hari Ini
            if (todayTopProducts.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🏆 Menu Terlaris Hari Ini",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkPrimary
                    )
                    Text(
                        text = "Lihat Semua ➔",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlue,
                        modifier = Modifier.clickable { viewModel.setCurrentTab(PosTab.TRANSAKSI) }
                    )
                }

                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        todayTopProducts.forEachIndexed { index, (name, qty, rev) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(if (index == 0) BrandBlue else SurfaceSubtle),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "#${index + 1}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (index == 0) Color.White else TextDarkSecondary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = name,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextDarkPrimary
                                        )
                                        Text(
                                            text = CurrencyUtils.formatIdr(rev),
                                            fontSize = 11.sp,
                                            color = TextDarkSecondary
                                        )
                                    }
                                }

                                Text(
                                    text = "$qty terjual",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BrandBlue
                                )
                            }

                            if (index < todayTopProducts.size - 1) {
                                HorizontalDivider(thickness = 0.5.dp, color = BorderSubtle, modifier = Modifier.padding(start = 56.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    if (isShiftModalOpen) {
        CashierShiftSwitcherModal(
            employees = employees,
            currentEmployee = activeEmployee,
            currentShift = activeShift,
            onDismiss = { isShiftModalOpen = false },
            onConfirmSwitch = { emp, shift ->
                viewModel.setActiveEmployee(emp)
                viewModel.setActiveShift(shift)
            }
        )
    }
}

@Composable
fun QuickActionButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkPrimary
            )

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = TextDarkSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
