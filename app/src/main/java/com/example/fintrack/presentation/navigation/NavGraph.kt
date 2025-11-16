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
import com.example.fintrack.presentation.home.HomeScreen
import com.example.fintrack.presentation.reports.ReportsScreen
import com.example.fintrack.presentation.settings.SettingsScreen
import com.example.fintrack.presentation.navigation.BottomNavItem



@Composable
fun NavGraph(
    navController: NavHostController,
    paddingValues: PaddingValues, // Receive padding from the main Scaffold
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.padding(paddingValues) // Apply padding here
    ) {
        // --- Auth Routes ---
        composable(
            route = AppRoutes.Login.route,
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -1000 }, animationSpec = tween(300)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -1000 }, animationSpec = tween(300)) }
        ) {
            LoginScreen(
                onNavigateToHome = {
                    navController.navigate(BottomNavItem.Home.route) {
                        popUpTo(AppRoutes.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(AppRoutes.Register.route) },
                onNavigateToForgotPassword = { navController.navigate(AppRoutes.ForgotPassword.route) },
                onNavigateToEmailVerification = {
                    navController.navigate(AppRoutes.VerifyEmail.route) { popUpTo(AppRoutes.Login.route) { inclusive = true } }
                },
                onNavigateToLogin = { navController.navigate(AppRoutes.Login.route)}
            )
        }

        composable(
            route = AppRoutes.Register.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { 1000 }, animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { 1000 }, animationSpec = tween(300)) }
        ) {
            RegistrationScreen(
                onNavigateToHome = {
                    navController.navigate(BottomNavItem.Home.route) {
                        popUpTo(AppRoutes.Login.route) { inclusive = true }
                    }
                },
                onNavigateBackToLogin = { navController.popBackStack() },
                onNavigateToEmailVerification ={
                    navController.navigate(AppRoutes.VerifyEmail.route) { popUpTo(AppRoutes.Login.route) { inclusive = true } }
                }
            )
        }

        composable(AppRoutes.ForgotPassword.route) {
            ForgotPasswordScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(AppRoutes.VerifyEmail.route) {
            EmailVerificationScreen(
                onNavigateToHome = {
                    navController.navigate(AppRoutes.Home.route) {
                        popUpTo(AppRoutes.VerifyEmail.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        // --- Main App Routes ---
        composable(
            route = BottomNavItem.Home.route,
            enterTransition = { fadeIn(animationSpec = tween(500)) },
            exitTransition = { fadeOut(animationSpec = tween(500)) }
        ) {
            HomeScreen(navController = navController)
        }

        // TODO: Add animations in between screens
        composable(BottomNavItem.Reports.route) {
            ReportsScreen(navController = navController)
        }

        composable(BottomNavItem.Budgets.route) {
            Text("Budgets Screen (Coming Soon)")
        }

        composable(
            route = AppRoutes.Settings.route,
            enterTransition = { slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) },
            exitTransition = { slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) }
        ) {
            AddTransactionScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        settingsNavGraph(navController = navController)

        // Add Transaction
        composable(
            route = AppRoutes.AddTransaction.route,
            // Slides up from the bottom
            enterTransition = { slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) },
            // Slides back down
            exitTransition = { slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) }
        ) {
            AddTransactionScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}