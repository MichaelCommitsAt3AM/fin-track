package com.example.fintrack.presentation.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.fintrack.presentation.navigation.AppRoutes
import com.example.fintrack.presentation.navigation.BottomNavItem

@Composable
fun BottomNavBar(navController: NavController) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Reports,
        BottomNavItem.Placeholder, // The empty space for the FAB
        BottomNavItem.Budgets,
        BottomNavItem.Settings
    )

    // Observe the current back stack to know which tab to select
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.8f), // Frosted look
        tonalElevation = 0.dp
    ) {
        items.forEach { item ->
            if (item == BottomNavItem.Placeholder) {
                // Render a disabled, empty item to create space for the FAB
                NavigationBarItem(
                    selected = false,
                    onClick = { },
                    icon = { },
                    enabled = false
                )
            } else {
                // Check if current route matches the item or is part of its graph
                val isSelected = when (item) {
                    BottomNavItem.Settings -> isSettingsRoute(currentRoute)
                    else -> currentRoute == item.route
                }

                NavigationBarItem(
                    selected = isSelected,
                    onClick = {
                        if (currentRoute != item.route) {
                            navController.navigate(item.route) {
                                // Pop up to the start destination of the graph to
                                // avoid building up a large stack of destinations
                                navController.graph.startDestinationRoute?.let { route ->
                                    popUpTo(route) {
                                        saveState = true
                                    }
                                }
                                // Avoid multiple copies of the same destination
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
                                restoreState = true
                            }
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title
                        )
                    },
                    label = {
                        Text(
                            text = item.title,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                )
            }
        }
    }
}

/**
 * Helper function to check if a route belongs to the settings navigation graph.
 * Add any new settings-related routes here to keep the Settings tab highlighted.
 */
private fun isSettingsRoute(route: String?): Boolean {
    return route == AppRoutes.SettingsGraph.route ||
            route == AppRoutes.Settings.route ||
            route == AppRoutes.ManageCategories.route
    // Add future settings routes here (e.g., Profile, Security, etc.)
}
