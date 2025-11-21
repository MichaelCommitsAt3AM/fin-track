package com.example.fintrack.presentation.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.example.fintrack.presentation.settings.CategoryDetailScreen
import com.example.fintrack.presentation.settings.categories.ManageCategoriesScreen
import com.example.fintrack.presentation.settings.SettingsScreen
import com.example.fintrack.presentation.settings.recurring.RecurringTransactionsScreen

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
            //enterTransition = { slideInHorizontally(initialOffsetX = { 1000 }, animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { 1000 }, animationSpec = tween(300)) }
        ) {
            ManageCategoriesScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddCategory = {
                    navController.navigate(AppRoutes.AddCategory.route)
                },
                onNavigateToEditCategory = { categoryName -> navController.navigate(AppRoutes.EditCategory.createRoute(categoryName))}
            )
        }

        composable(
            route = AppRoutes.RecurringTransactions.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { 1000 }, animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { 1000 }, animationSpec = tween(300)) }
        ) {
            RecurringTransactionsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 3. Add Category Screen (Slide Up)
        composable(
            route = AppRoutes.AddCategory.route,
            enterTransition = { slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) },
            exitTransition = { slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) }
        ) {
            CategoryDetailScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 4. Edit Category Screen (Slide Over)
        composable(
            route = AppRoutes.EditCategory.route,
            arguments = listOf(navArgument("categoryName") { type = NavType.StringType }),
            enterTransition = { slideInHorizontally(initialOffsetX = { 1000 }, animationSpec = tween(300)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { 1000 }, animationSpec = tween(300)) }
        ) {
            CategoryDetailScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}