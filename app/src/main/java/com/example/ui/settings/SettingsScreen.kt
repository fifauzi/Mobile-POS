package com.example.ui.settings

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.PosViewModel
import com.example.ui.components.PosTab
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.CardBg
import com.example.ui.theme.PastelBlueBg
import com.example.ui.theme.PastelBlueIcon
import com.example.ui.theme.PastelGreenBg
import com.example.ui.theme.PastelGreenIcon
import com.example.ui.theme.PastelOrangeBg
import com.example.ui.theme.PastelOrangeIcon
import com.example.ui.theme.PastelPinkBg
import com.example.ui.theme.PastelPinkIcon
import com.example.ui.theme.PastelPurpleBg
import com.example.ui.theme.PastelPurpleIcon
import com.example.ui.theme.PastelYellowBg
import com.example.ui.theme.PastelYellowIcon
import com.example.ui.theme.ScaffoldBg
import com.example.ui.theme.TextDarkPrimary
import com.example.ui.theme.TextDarkSecondary

/**
 * Settings Screen matching image_318c0090.png:
 * Organized into white card groups (Pengaturan Usaha),
 * Distinct pastel rounded icons, titles, subtitles, and clean blue right chevrons.
 */
enum class SettingsModalType {
    OUTLET,
    PEGAWAI,
    METODE_PEMESANAN,
    METODE_PEMBAYARAN,
    BIAYA_TAMBAHAN,
    TAMPILAN_STRUK,
    KATEGORI_PRODUK,
    MANAJEMEN_VARIAN
}

@Composable
fun SettingsScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var activeModal by remember { mutableStateOf<SettingsModalType?>(null) }

    androidx.activity.compose.BackHandler(enabled = activeModal != null) {
        activeModal = null
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
            // Screen Header: Bold "Settings"
            Text(
                text = "Settings",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextDarkPrimary,
                modifier = Modifier.padding(top = 14.dp, bottom = 18.dp)
            )

            // Section 1: "Pengaturan Usaha"
            Text(
                text = "Pengaturan Usaha",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // White Card Group
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Item 1: Outlet
                    SettingsItemRow(
                        icon = Icons.Default.Store,
                        iconBg = PastelOrangeBg,
                        iconTint = PastelOrangeIcon,
                        title = "Outlet",
                        subtitle = "Atur informasi outlet usahamu.",
                        onClick = { activeModal = SettingsModalType.OUTLET }
                    )

                    HorizontalDivider(thickness = 0.5.dp, color = BorderSubtle, modifier = Modifier.padding(start = 72.dp))

                    // Item 2: Pegawai
                    SettingsItemRow(
                        icon = Icons.Default.GroupAdd,
                        iconBg = PastelBlueBg,
                        iconTint = PastelBlueIcon,
                        title = "Pegawai",
                        subtitle = "Pengaturan seputar pegawaimu.",
                        onClick = { activeModal = SettingsModalType.PEGAWAI }
                    )

                    HorizontalDivider(thickness = 0.5.dp, color = BorderSubtle, modifier = Modifier.padding(start = 72.dp))

                    // Item 3: Metode Pemesanan
                    SettingsItemRow(
                        icon = Icons.Default.ShoppingCart,
                        iconBg = PastelOrangeBg,
                        iconTint = PastelOrangeIcon,
                        title = "Metode Pemesanan",
                        subtitle = "Atur metode dan jam pemesanan.",
                        onClick = { activeModal = SettingsModalType.METODE_PEMESANAN }
                    )

                    HorizontalDivider(thickness = 0.5.dp, color = BorderSubtle, modifier = Modifier.padding(start = 72.dp))

                    // Item 4: Metode Pembayaran
                    SettingsItemRow(
                        icon = Icons.Default.CreditCard,
                        iconBg = PastelYellowBg,
                        iconTint = PastelYellowIcon,
                        title = "Metode Pembayaran",
                        subtitle = "Bisa atur cara pencairan QRIS juga.",
                        badgeText = "BARU",
                        badgeBg = Color(0xFF22C55E),
                        onClick = { activeModal = SettingsModalType.METODE_PEMBAYARAN }
                    )

                    HorizontalDivider(thickness = 0.5.dp, color = BorderSubtle, modifier = Modifier.padding(start = 72.dp))

                    // Item 5: Biaya Tambahan
                    SettingsItemRow(
                        icon = Icons.Default.Sell,
                        iconBg = PastelPinkBg,
                        iconTint = PastelPinkIcon,
                        title = "Biaya Tambahan",
                        subtitle = "Atur pajak PB1 10% dan biaya layanan.",
                        onClick = { activeModal = SettingsModalType.BIAYA_TAMBAHAN }
                    )

                    HorizontalDivider(thickness = 0.5.dp, color = BorderSubtle, modifier = Modifier.padding(start = 72.dp))

                    // Item 6: Tampilan Struk & Printer
                    SettingsItemRow(
                        icon = Icons.Default.Print,
                        iconBg = PastelPurpleBg,
                        iconTint = PastelPurpleIcon,
                        title = "Tampilan Struk & Printer",
                        subtitle = "Koneksi Bluetooth ESC/POS & layout struk.",
                        onClick = { activeModal = SettingsModalType.TAMPILAN_STRUK }
                    )

                    HorizontalDivider(thickness = 0.5.dp, color = BorderSubtle, modifier = Modifier.padding(start = 72.dp))

                    // Item 7: Kategori Produk
                    SettingsItemRow(
                        icon = Icons.Default.Category,
                        iconBg = PastelGreenBg,
                        iconTint = PastelGreenIcon,
                        title = "Kategori Produk",
                        subtitle = "Kelola kategori Coffee, Bahan Baku, Beans, Pastry.",
                        onClick = { activeModal = SettingsModalType.KATEGORI_PRODUK }
                    )

                    HorizontalDivider(thickness = 0.5.dp, color = BorderSubtle, modifier = Modifier.padding(start = 72.dp))

                    // Item 8: Manajemen Varian
                    SettingsItemRow(
                        icon = Icons.Default.Tune,
                        iconBg = PastelPurpleBg,
                        iconTint = PastelPurpleIcon,
                        title = "Manajemen Varian",
                        subtitle = "Atur opsi Size, Beans, Type, Grind, Add-ons.",
                        onClick = { activeModal = SettingsModalType.MANAJEMEN_VARIAN }
                    )

                    HorizontalDivider(thickness = 0.5.dp, color = BorderSubtle, modifier = Modifier.padding(start = 72.dp))

                    // Item 9: Ekspor Data CSV
                    SettingsItemRow(
                        icon = Icons.Default.FileDownload,
                        iconBg = PastelGreenBg,
                        iconTint = PastelGreenIcon,
                        title = "Ekspor Laporan Penjualan",
                        subtitle = "Simpan atau bagikan laporan CSV transaksi.",
                        onClick = { viewModel.exportSalesCsv(context) }
                    )

                    HorizontalDivider(thickness = 0.5.dp, color = BorderSubtle, modifier = Modifier.padding(start = 72.dp))

                    // Item 10: Kirim Laporan ke Email
                    SettingsItemRow(
                        icon = Icons.Default.Email,
                        iconBg = PastelBlueBg,
                        iconTint = PastelBlueIcon,
                        title = "Kirim Laporan ke Email",
                        subtitle = "Kirim spreadsheet penjualan ke bdscoffeecompany@gmail.com.",
                        onClick = { viewModel.sendSalesReportEmail(context) }
                    )
                }
            }

            // Render Sub-screen Sheets when active
            when (activeModal) {
                SettingsModalType.OUTLET -> OutletSettingsSheet(viewModel = viewModel, onDismiss = { activeModal = null })
                SettingsModalType.PEGAWAI -> EmployeeSettingsSheet(viewModel = viewModel, onDismiss = { activeModal = null })
                SettingsModalType.METODE_PEMESANAN -> OrderMethodsSettingsSheet(viewModel = viewModel, onDismiss = { activeModal = null })
                SettingsModalType.METODE_PEMBAYARAN -> PaymentGatewaysSettingsSheet(viewModel = viewModel, onDismiss = { activeModal = null })
                SettingsModalType.BIAYA_TAMBAHAN -> AdditionalFeesSettingsSheet(viewModel = viewModel, onDismiss = { activeModal = null })
                SettingsModalType.TAMPILAN_STRUK -> ReceiptAndPrinterSettingsSheet(viewModel = viewModel, onDismiss = { activeModal = null })
                SettingsModalType.KATEGORI_PRODUK -> CategoryManagementSheet(viewModel = viewModel, onDismiss = { activeModal = null })
                SettingsModalType.MANAJEMEN_VARIAN -> com.example.ui.variants.VariantManagementModalSheet(viewModel = viewModel, onDismiss = { activeModal = null })
                null -> {}
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App version info
            Text(
                text = "Caffeinery POS v3.0.0 • Powered by BDS Tech",
                fontSize = 12.sp,
                color = Color(0xFF94A3B8),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun SettingsItemRow(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    badgeText: String? = null,
    badgeBg: Color = Color(0xFF22C55E),
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pastel Icon Box (~44x44dp with 14dp rounded corners)
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
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

        Spacer(modifier = Modifier.width(14.dp))

        // Titles
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkPrimary
                )
                if (badgeText != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextDarkSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // Simple Blue Right Chevron (">")
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = BrandBlue,
            modifier = Modifier.size(20.dp)
        )
    }
}
