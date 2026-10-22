package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.PosViewModel
import com.example.ui.analytics.AnalyticsScreen
import com.example.ui.cart.CartBottomSheet
import com.example.ui.catalog.CatalogScreen
import com.example.ui.checkout.CheckoutModal
import com.example.ui.checkout.ReceiptSuccessModal
import com.example.ui.components.IosBottomBar
import com.example.ui.components.PosTab
import com.example.ui.inventory.InventoryScreen
import com.example.ui.orders.OrdersHistoryScreen
import com.example.ui.printer.ThermalPrinterScreen
import com.example.ui.theme.MyApplicationTheme

import com.example.ui.dashboard.DashboardScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.ScaffoldBg

class MainActivity : ComponentActivity() {

    private val posViewModel: PosViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                PosAppRoot(viewModel = posViewModel)
            }
        }
    }
}

@Composable
fun PosAppRoot(viewModel: PosViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val isCartSheetOpen by viewModel.isCartSheetOpen.collectAsStateWithLifecycle()
    val isCheckoutModalOpen by viewModel.isCheckoutModalOpen.collectAsStateWithLifecycle()
    val isReceiptModalOpen by viewModel.isReceiptModalOpen.collectAsStateWithLifecycle()
    val activeCompletedOrder by viewModel.activeCompletedOrder.collectAsStateWithLifecycle()
    val cartSummary by viewModel.cartSummary.collectAsStateWithLifecycle()

    // Back handling
    BackHandler(enabled = isReceiptModalOpen || isCheckoutModalOpen || isCartSheetOpen || currentTab != PosTab.BERANDA) {
        when {
            isReceiptModalOpen -> viewModel.closeReceiptModal()
            isCheckoutModalOpen -> viewModel.closeCheckoutModal()
            isCartSheetOpen -> viewModel.closeCartSheet()
            currentTab != PosTab.BERANDA -> viewModel.setCurrentTab(PosTab.BERANDA)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ScaffoldBg,
        bottomBar = {
            IosBottomBar(
                currentTab = currentTab,
                onTabSelected = { viewModel.setCurrentTab(it) },
                cartItemCount = cartSummary.totalItemCount
            )
        }
    ) { innerPadding ->
        Crossfade(
            targetState = currentTab,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
            label = "TabCrossfade"
        ) { tab ->
            when (tab) {
                PosTab.BERANDA -> DashboardScreen(viewModel = viewModel)
                PosTab.KASIR -> CatalogScreen(viewModel = viewModel)
                PosTab.KATALOG -> InventoryScreen(viewModel = viewModel)
                PosTab.TRANSAKSI -> OrdersHistoryScreen(viewModel = viewModel)
                PosTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
            }
        }
    }

    // Modal Overlays
    if (isCartSheetOpen) {
        CartBottomSheet(
            viewModel = viewModel,
            onDismiss = { viewModel.closeCartSheet() }
        )
    }

    if (isCheckoutModalOpen) {
        CheckoutModal(
            viewModel = viewModel,
            onDismiss = { viewModel.closeCheckoutModal() }
        )
    }

    if (isReceiptModalOpen && activeCompletedOrder != null) {
        ReceiptSuccessModal(
            order = activeCompletedOrder!!.first,
            items = activeCompletedOrder!!.second,
            viewModel = viewModel,
            onDismiss = { viewModel.closeReceiptModal() }
        )
    }
}
