package com.example.fintrack.presentation.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.example.fintrack.presentation.settings.ManageCategoriesScreen
import com.example.fintrack.presentation.settings.SettingsScreen

fun NavGraphBuilder.settingsNavGraph(navController: NavHostController) {
    navigation(
        route = AppRoutes.SettingsGraph.route,
        startDestination = AppRoutes.Settings.route
    ) {
        // 1. Main Settings Screen
        composable(route = AppRoutes.Settings.route) {
            SettingsScreen(
                navController = navController,
                onNavigateToLogin = {
                    navController.navigate(AppRoutes.Login.route) {
                        popUpTo(0) // Clear backstack on logout
                    }
                },
                onNavigateBack = { navController.popBackStack() },
                onNavigateToManageCategories = {
                    navController.navigate(AppRoutes.ManageCategories.route)
                }
            )
        }

        // 2. Manage Categories Screen
        composable(
            route = AppRoutes.ManageCategories.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { 1000 }, animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { 1000 }, animationSpec = tween(300)) }
        ) {
            ManageCategoriesScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 3. Add future settings screens here (e.g., Profile, Security)
    }
}