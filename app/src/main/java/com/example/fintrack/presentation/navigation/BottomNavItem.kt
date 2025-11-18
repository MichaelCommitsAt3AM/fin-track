package com.example.fintrack.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : BottomNavItem(
        route = AppRoutes.Home.route,
        title = "Home",
        icon = Icons.Default.Home
    )

    object Reports : BottomNavItem(
        route = AppRoutes.Reports.route,
        title = "Reports",
        icon = Icons.Default.BarChart
    )

    // This is a placeholder for the middle FAB space
//    object Placeholder : BottomNavItem(
//        route = "",
//        title = "",
//        icon = Icons.Default.Home // Icon doesn't matter, won't be shown
//    )

    object Budgets : BottomNavItem(
        route = AppRoutes.Budgets.route,
        title = "Budgets",
        icon = Icons.Outlined.AccountBalanceWallet
    )

    object Settings : BottomNavItem(
        route = AppRoutes.SettingsGraph.route,
        title = "Settings",
        icon = Icons.Default.Settings
    )
}