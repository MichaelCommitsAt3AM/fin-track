package com.example.fintrack.presentation.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.fintrack.presentation.add_transaction.AddTransactionScreen
import com.example.fintrack.presentation.auth.EmailVerificationScreen
import com.example.fintrack.presentation.auth.ForgotPasswordScreen
import com.example.fintrack.presentation.auth.LoginScreen
import com.example.fintrack.presentation.auth.RegistrationScreen
import com.example.fintrack.presentation.budgets.AddBudgetScreen
import com.example.fintrack.presentation.budgets.BudgetsScreen
import com.example.fintrack.presentation.home.HomeScreen
import com.example.fintrack.presentation.reports.ReportsScreen
import com.example.fintrack.presentation.settings.SettingsScreen
import com.example.fintrack.presentation.navigation.BottomNavItem
import com.example.fintrack.presentation.transactions.TransactionListScreen
import com.example.fintrack.presentation.setup.SetupScreen // Add this import


@Composable
fun NavGraph(
    navController: NavHostController,
    paddingValues: PaddingValues,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier
    ) {
        // --- Auth Routes ---
        composable(
            route = AppRoutes.Login.route,
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -1000 }, animationSpec = tween(300)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -1000 }, animationSpec = tween(300)) }
        ) {
            LoginScreen(
                onNavigateToHome = {
                    // Navigate to setup screen instead of home
                    navController.navigate(AppRoutes.Setup.route) {
                        popUpTo(AppRoutes.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(AppRoutes.Register.route) },
                onNavigateToForgotPassword = { navController.navigate(AppRoutes.ForgotPassword.route) },
                onNavigateToEmailVerification = {
                    navController.navigate(AppRoutes.VerifyEmail.route) {
                        popUpTo(AppRoutes.Login.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.navigate(AppRoutes.Login.route) }
            )
        }

        composable(
            route = AppRoutes.Register.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { 1000 }, animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { 1000 }, animationSpec = tween(300)) }
        ) {
            RegistrationScreen(
                onNavigateToHome = {
                    // For new signups, go directly to home (they have no data to sync)
                    navController.navigate(BottomNavItem.Home.route) {
                        popUpTo(AppRoutes.Login.route) { inclusive = true }
                    }
                },
                onNavigateBackToLogin = { navController.popBackStack() },
                onNavigateToEmailVerification = {
                    navController.navigate(AppRoutes.VerifyEmail.route) {
                        popUpTo(AppRoutes.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(AppRoutes.ForgotPassword.route) {
            ForgotPasswordScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(AppRoutes.VerifyEmail.route) {
            EmailVerificationScreen(
                onNavigateToHome = {
                    // After email verification, go to setup for existing users
                    navController.navigate(AppRoutes.Setup.route) {
                        popUpTo(AppRoutes.VerifyEmail.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        // --- Setup Screen (New) ---
        composable(
            route = AppRoutes.Setup.route,
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            SetupScreen(
                onNavigateToHome = {
                    navController.navigate(BottomNavItem.Home.route) {
                        // Clear the entire back stack
                        popUpTo(AppRoutes.Setup.route) { inclusive = true }
                        popUpTo(AppRoutes.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // --- Main App Routes ---
        composable(
            route = BottomNavItem.Home.route,
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            HomeScreen(
                navController = navController,
                paddingValues = paddingValues
            )
        }

        composable(BottomNavItem.Reports.route) {
            ReportsScreen(
                navController = navController,
                paddingValues = paddingValues
            )
        }

        composable(
            route = BottomNavItem.Budgets.route,
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            BudgetsScreen(
                paddingValues = paddingValues,
                onNavigateToAddBudget = { navController.navigate(AppRoutes.AddBudget.route) }
            )
        }

        // Add AddBudget screen
        composable(
            route = AppRoutes.AddBudget.route,
            enterTransition = { slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) },
            exitTransition = { slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) }
        ) {
            AddBudgetScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }


        composable(
            route = AppRoutes.Settings.route,
            enterTransition = { slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) },
            exitTransition = { slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) }
        ) {
            AddTransactionScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToManageCategories = { navController.navigate(AppRoutes.ManageCategories.route) }
            )
        }

        settingsNavGraph(
            navController = navController,
            paddingValues  = paddingValues
        )

        // Add Transaction
        composable(
            route = AppRoutes.AddTransaction.route,
            enterTransition = { slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) },
            exitTransition = { slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) }
        ) {
            AddTransactionScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToManageCategories = { navController.navigate(AppRoutes.ManageCategories.route) }
            )
        }

        // --- Transaction List Screen ---
        composable(
            route = AppRoutes.TransactionList.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { 1000 }, animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { 1000 }, animationSpec = tween(300)) }
        ) {
            TransactionListScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
