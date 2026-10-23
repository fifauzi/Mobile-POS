package com.example.ui.orders

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import com.example.data.local.model.OrderEntity
import com.example.data.local.model.OrderItemEntity
import com.example.ui.PosViewModel
import com.example.ui.checkout.ReceiptSuccessModal
import com.example.ui.components.CurrencyUtils
import com.example.ui.components.IosSegmentedControl
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandBlueLight
import com.example.ui.theme.CardBg
import com.example.ui.theme.ScaffoldBg
import com.example.ui.theme.StockGreenBg
import com.example.ui.theme.StockGreenText
import com.example.ui.theme.TextDarkMuted
import com.example.ui.theme.TextDarkPrimary
import com.example.ui.theme.TextDarkSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Transaksi Screen matching image_afa560ee.png and image_711ee7d3.png:
 * - Defaults to today's transactions ("Hari ini")
 * - Expandable/Collapsible Accordion date cards with smooth animated visibility & arrow icons
 * - Responsive header and export triggers
 * - Shows variant summary and profit per transaction
 */
@Composable
fun OrdersHistoryScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val allOrderItems by viewModel.allOrderItems.collectAsStateWithLifecycle()

    var selectedTabIdx by remember { mutableIntStateOf(0) } // 0: Pendapatan, 1: Pencairan
    // Default to today's transactions ("Hari ini") as strictly required
    var filterDaysLabel by remember { mutableStateOf("Hari ini") }
    var isFilterDropdownOpen by remember { mutableStateOf(false) }

    var selectedOrderForDetail by remember { mutableStateOf<Pair<OrderEntity, List<OrderItemEntity>>?>(null) }
    var showFullAnalytics by remember { mutableStateOf(false) }

    val filteredOrders = remember(orders, filterDaysLabel) {
        val now = System.currentTimeMillis()
        val oneDayMs = 24L * 60 * 60 * 1000
        when (filterDaysLabel) {
            "Hari ini" -> orders.filter { now - it.timestamp <= oneDayMs }
            "7 hari terakhir" -> orders.filter { now - it.timestamp <= 7 * oneDayMs }
            "14 hari terakhir" -> orders.filter { now - it.timestamp <= 14 * oneDayMs }
            "30 hari terakhir" -> orders.filter { now - it.timestamp <= 30 * oneDayMs }
            else -> orders
        }
    }

    val orderItemsGrouped = remember(allOrderItems) {
        allOrderItems.groupBy { it.orderInvoiceNumber }
    }

    // Group orders by date formatted "EEEE, dd MMM yyyy"
    val ordersByDate = remember(filteredOrders) {
        val dateFormat = SimpleDateFormat("EEEE, dd MMM yyyy", Locale("id", "ID"))
        filteredOrders.groupBy { dateFormat.format(Date(it.timestamp)) }
    }

    BackHandler(enabled = showFullAnalytics || selectedOrderForDetail != null) {
        when {
            showFullAnalytics -> showFullAnalytics = false
            selectedOrderForDetail != null -> selectedOrderForDetail = null
        }
    }

    if (showFullAnalytics) {
        com.example.ui.analytics.AnalyticsScreen(
            viewModel = viewModel,
            onBack = { showFullAnalytics = false }
        )
    } else {
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
                // Header: Responsive "Transaksi" & "Laporan" pill button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = "Transaksi",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextDarkPrimary
                        )
                    }

                    // "Laporan" action pill (responsive sizing)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(BrandBlueLight)
                            .clickable { showFullAnalytics = true }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("open_analytics_report_btn"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = null,
                            tint = BrandBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Laporan Analitik",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandBlue
                        )
                    }
                }

                // Tabs: "Pendapatan" / "Pencairan"
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    IosSegmentedControl(
                        items = listOf("Pendapatan", "Pencairan"),
                        selectedIndex = selectedTabIdx,
                        onItemSelected = { idx, _ -> selectedTabIdx = idx }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Calendar Filter Pill ("📅 Hari ini ⌵")
                Box(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(CardBg)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                            .clickable { isFilterDropdownOpen = true }
                            .padding(horizontal = 16.dp),
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
                            Text(
                                text = filterDaysLabel,
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

                    DropdownMenu(
                        expanded = isFilterDropdownOpen,
                        onDismissRequest = { isFilterDropdownOpen = false },
                        modifier = Modifier.background(CardBg)
                    ) {
                        listOf("Hari ini", "7 hari terakhir", "14 hari terakhir", "30 hari terakhir", "Semua waktu").forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt, fontWeight = if (filterDaysLabel == opt) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    filterDaysLabel = opt
                                    isFilterDropdownOpen = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Grouped Transactions List by Date
                if (filteredOrders.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = Color(0xFFCBD5E1),
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Tidak ada transaksi ($filterDaysLabel)",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDarkPrimary
                            )
                            Text(
                                text = "Pilih rentang tanggal lain untuk melihat riwayat",
                                fontSize = 13.sp,
                                color = TextDarkSecondary
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ordersByDate.forEach { (dateHeader, dateOrders) ->
                            item(key = dateHeader) {
                                DateGroupAccordionCard(
                                    dateHeader = dateHeader,
                                    orders = dateOrders,
                                    itemsGrouped = orderItemsGrouped,
                                    onOrderClick = { ord, itms ->
                                        selectedOrderForDetail = Pair(ord, itms)
                                    },
                                    onReprint = { ord, itms ->
                                        viewModel.printCurrentReceipt(context, ord, itms)
                                    }
                                )
                            }
                        }
                        item {
                            Spacer(modifier = Modifier.height(100.dp))
                        }
                    }
                }
            }
        }
    }

    selectedOrderForDetail?.let { (order, items) ->
        ReceiptSuccessModal(
            order = order,
            items = items,
            viewModel = viewModel,
            onDismiss = { selectedOrderForDetail = null }
        )
    }
}

/**
 * Interactive Accordion Date Group Card:
 * Smooth up/down arrow button toggles the hidden transaction breakdown list smoothly without jumps.
 */
@Composable
fun DateGroupAccordionCard(
    dateHeader: String,
    orders: List<OrderEntity>,
    itemsGrouped: Map<String, List<OrderItemEntity>>,
    onOrderClick: (OrderEntity, List<OrderItemEntity>) -> Unit,
    onReprint: (OrderEntity, List<OrderItemEntity>) -> Unit
) {
    val totalAmount = remember(orders) { orders.sumOf { it.grandTotal } }
    var isExpanded by remember { mutableStateOf(true) }

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
            // Interactive Accordion Header Row (Clicking toggles collapse/expand)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = dateHeader,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkPrimary
                    )
                    Text(
                        text = "${orders.size} transaksi • ${CurrencyUtils.formatIdr(totalAmount)}",
                        fontSize = 12.sp,
                        color = TextDarkSecondary
                    )
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = BrandBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Smooth Animated Accordion Body
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Text(
                        text = "Rincian Transaksi:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDarkMuted
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Sub-Cards for each transaction
                    orders.forEachIndexed { index, order ->
                        val timeStr = SimpleDateFormat("HH:mm:ss 'WIB'", Locale("id", "ID")).format(Date(order.timestamp))
                        val items = itemsGrouped[order.invoiceNumber] ?: emptyList()
                        val itemCount = items.sumOf { it.quantity }
                        val isDineIn = order.orderType != "TAKEAWAY"

                        val variantsSummary = items.mapNotNull {
                            if (it.variantSummary.isNotBlank() && it.variantSummary != "Standard") "${it.productName} (${it.variantSummary})" else null
                        }.joinToString("; ")

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                                .background(CardBg)
                                .clickable { onOrderClick(order, items) }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f, fill = false)
                                    ) {
                                        Text(
                                            text = "Transaksi #${index + 1} (${order.paymentMethod})",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextDarkPrimary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isDineIn) BrandBlueLight else Color(0xFFFEF3C7))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (isDineIn) "🍽️ Dine-in" else "🥡 Bungkus",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDineIn) BrandBlue else Color(0xFFD97706)
                                            )
                                        }
                                    }
                                    Text(
                                        text = CurrencyUtils.formatIdr(order.grandTotal),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandBlue
                                    )
                                }

                                if (variantsSummary.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "↳ $variantsSummary",
                                        fontSize = 11.sp,
                                        color = BrandBlue,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Jam Bayar: $timeStr",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextDarkMuted
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "$itemCount items",
                                            fontSize = 11.sp,
                                            color = TextDarkSecondary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = null,
                                            tint = BrandBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    HorizontalDivider(thickness = 0.5.dp, color = BorderSubtle, modifier = Modifier.padding(vertical = 6.dp))

                    // Total Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Hari Ini",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDarkPrimary
                        )
                        Text(
                            text = CurrencyUtils.formatIdr(totalAmount),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextDarkPrimary
                        )
                    }
                }
            }
        }
    }
}
