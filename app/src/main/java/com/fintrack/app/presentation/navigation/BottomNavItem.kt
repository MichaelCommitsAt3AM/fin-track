package com.fintrack.app.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.ui.graphics.vector.ImageVector
import com.fintrack.app.presentation.navigation.AppRoutes

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

    object Transactions : BottomNavItem(
        route = AppRoutes.TransactionList.route,
        title = "Transactions",
        icon = Icons.Default.Receipt
    )

    object Goals : BottomNavItem(
        route = AppRoutes.Goals.route,
        title = "Goals",
        icon = Icons.Default.Flag
    )

    object Reports : BottomNavItem(
        route = AppRoutes.Reports.route,
        title = "Reports",
        icon = Icons.Default.BarChart
    )
}