package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.CardBg
import com.example.ui.theme.TextDarkMuted
import com.example.ui.theme.TextDarkPrimary

enum class PosTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    BERANDA("Beranda", Icons.Filled.Home, Icons.Outlined.Home, "tab_beranda"),
    KASIR("Kasir", Icons.Filled.Storefront, Icons.Outlined.Storefront, "tab_kasir"),
    KATALOG("Katalog", Icons.Filled.Inventory2, Icons.Outlined.Inventory2, "tab_katalog"),
    TRANSAKSI("Transaksi", Icons.Filled.SwapHoriz, Icons.Outlined.SwapHoriz, "tab_transaksi"),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "tab_settings")
}

/**
 * Modern Gen-Z Aqua Glass Floating Bottom Navigation Bar:
 * - Frosted white translucent surface with subtle aqua glass gradient highlight
 * - Smooth rounded floating capsule (26.dp corner radius)
 * - Solid vibrant blue active indicators (matching image_0.png & image_1.png)
 * - Responsive 48dp+ interactive touch targets
 * - Interactive cart notification badge
 */
@Composable
fun IosBottomBar(
    currentTab: PosTab,
    onTabSelected: (PosTab) -> Unit,
    cartItemCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        // Floating Aqua Glass Capsule
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(26.dp),
                    spotColor = Color(0x330052CC),
                    ambientColor = Color(0x1A00E5FF)
                )
                .clip(RoundedCornerShape(26.dp))
                // Translucent Frosted Glass with subtle Aqua sheen
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFAFFFFFF), // Crisp pure frosted glass top
                            Color(0xF0F4FBFF)  // Soft subtle aqua glow bottom
                        )
                    )
                )
                .border(
                    width = 1.2.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0x80FFFFFF), // Glass reflection highlight
                            Color(0x4000C4DF)  // Subtle aqua glow border
                        )
                    ),
                    shape = RoundedCornerShape(26.dp)
                )
                .padding(vertical = 6.dp, horizontal = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PosTab.entries.forEach { tab ->
                    val isSelected = tab == currentTab

                    val iconTint by animateColorAsState(
                        targetValue = if (isSelected) BrandBlue else TextDarkMuted,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "IconTintAnimation"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isSelected) Color(0x140052CC) else Color.Transparent)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onTabSelected(tab)
                            }
                            .testTag(tab.tag),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label,
                                    tint = iconTint,
                                    modifier = Modifier.size(24.dp)
                                )

                                // Cart notification badge
                                if (tab == PosTab.KASIR && cartItemCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .offset(x = 12.dp, y = (-4).dp)
                                            .clip(CircleShape)
                                            .background(BrandBlue)
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = if (cartItemCount > 99) "99+" else cartItemCount.toString(),
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Text(
                                text = tab.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) BrandBlue else TextDarkMuted,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
