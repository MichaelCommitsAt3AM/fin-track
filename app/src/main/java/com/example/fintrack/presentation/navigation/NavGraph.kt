package com.example.fintrack.presentation.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.fintrack.presentation.add_transaction.AddTransactionScreen
import com.example.fintrack.presentation.auth.BiometricLoginScreen
import com.example.fintrack.presentation.auth.EmailVerificationScreen
import com.example.fintrack.presentation.auth.ForgotPasswordScreen
import com.example.fintrack.presentation.auth.LoginScreen
import com.example.fintrack.presentation.auth.pin.PinLoginScreen
import com.example.fintrack.presentation.auth.RegistrationScreen
import com.example.fintrack.presentation.budgets.AddBudgetScreen
import com.example.fintrack.presentation.budgets.BudgetsScreen
import com.example.fintrack.presentation.home.HomeScreen
import com.example.fintrack.presentation.notifications.NotificationScreen
import com.example.fintrack.presentation.profile_setup.ProfileSetupScreen
import com.example.fintrack.presentation.reports.ReportsScreen
import com.example.fintrack.presentation.setup.SetupScreen
import com.example.fintrack.presentation.transactions.TransactionListScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    paddingValues: PaddingValues,
    startDestination: String
) {
    // --- Animation Specs ---

    // 1. Auth & Full Screen SLIDE Spec (IntOffset)
    // Standard easing for entering/exiting setup flows (300ms)
    val authSlideSpec = remember { tween<IntOffset>(300, easing = FastOutSlowInEasing) }

    // 2. Auth & Full Screen FADE Spec (Float)
    // Used for screens that don't slide but fade in/out (300ms)
    val authFadeSpec = remember { tween<Float>(300, easing = FastOutSlowInEasing) }

    // 3. Bottom Nav Crossfade Spec (Float)
    // Fast 100ms Linear fade for snappy tab switching
    val tabCrossfadeSpec = remember { tween<Float>(100, easing = LinearEasing) }

    // 4. Modal/Detail Slide Spec (IntOffset)
    // Slightly faster (250ms) for details popping up/in
    val detailSlideSpec = remember { tween<IntOffset>(250, easing = FastOutSlowInEasing) }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier
    ) {
        // --- Auth Routes ---
        composable(
            route = AppRoutes.Login.route,
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = authSlideSpec) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = authSlideSpec) }
        ) {
            LoginScreen(
                onNavigateToSetup = {
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
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = authSlideSpec) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = authSlideSpec) }
        ) {
            RegistrationScreen(
                onNavigateToHome = {
                    navController.navigate(AppRoutes.ProfileSetup.route) {
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
                onNavigateToProfileSetup = {
                    navController.navigate(AppRoutes.ProfileSetup.route) {
                        popUpTo(AppRoutes.VerifyEmail.route) { inclusive = true }
                    }
                },
                onNavigateToSetup = {
                    navController.navigate(AppRoutes.Setup.route) {
                        popUpTo(AppRoutes.VerifyEmail.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        // --- Profile Setup Screen (New Users) ---
        composable(
            route = AppRoutes.ProfileSetup.route,
            enterTransition = { fadeIn(animationSpec = authFadeSpec) },
            exitTransition = { fadeOut(animationSpec = authFadeSpec) }
        ) {
            ProfileSetupScreen(
                onNavigateToHome = {
                    navController.navigate(AppRoutes.Setup.route) {
                        popUpTo(AppRoutes.ProfileSetup.route) { inclusive = true }
                    }
                }
            )
        }

        // --- Setup Screen ---
        composable(
            route = AppRoutes.Setup.route,
            enterTransition = { fadeIn(animationSpec = authFadeSpec) },
            exitTransition = { fadeOut(animationSpec = authFadeSpec) }
        ) {
            SetupScreen(
                onNavigateToHome = {
                    navController.navigate(BottomNavItem.Home.route) {
                        popUpTo(AppRoutes.Setup.route) { inclusive = true }
                        popUpTo(AppRoutes.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(AppRoutes.BiometricLock.route) {
            BiometricLoginScreen(
                onSuccess = { navController.popBackStack() },
                onUsePin = { navController.navigate(AppRoutes.PinLogin.route) }
            )
        }

        composable(AppRoutes.PinLogin.route) {
            val hasBiometrics = true
            PinLoginScreen(
                onPinVerified = {
                    navController.navigate(BottomNavItem.Home.route) {
                        popUpTo(AppRoutes.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onUseBiometrics = { navController.popBackStack() },
                onForgotPin = { navController.navigate(AppRoutes.ForgotPassword.route) },
                isBiometricAvailable = hasBiometrics
            )
        }

        // --- Main App Routes (Bottom Navigation) ---
        // Using fast crossfade (100ms)

        composable(
            route = BottomNavItem.Home.route,
            enterTransition = { fadeIn(animationSpec = tabCrossfadeSpec) },
            exitTransition = { fadeOut(animationSpec = tabCrossfadeSpec) }
        ) {
            HomeScreen(
                navController = navController,
                paddingValues = paddingValues
            )
        }

        composable(
            route = BottomNavItem.Reports.route,
            enterTransition = { fadeIn(animationSpec = tabCrossfadeSpec) },
            exitTransition = { fadeOut(animationSpec = tabCrossfadeSpec) }
        ) {
            ReportsScreen(
                navController = navController,
                paddingValues = paddingValues
            )
        }

        composable(
            route = BottomNavItem.Budgets.route,
            enterTransition = { fadeIn(animationSpec = tabCrossfadeSpec) },
            exitTransition = { fadeOut(animationSpec = tabCrossfadeSpec) }
        ) {
            BudgetsScreen(
                paddingValues = paddingValues,
                onNavigateToAddBudget = { navController.navigate(AppRoutes.AddBudget.route) }
            )
        }

        // --- Detail / Feature Screens ---
        // Using slide animations (IntOffset)

        composable(
            route = AppRoutes.AddBudget.route,
            enterTransition = { slideInVertically(initialOffsetY = { it }, animationSpec = detailSlideSpec) },
            exitTransition = { slideOutVertically(targetOffsetY = { it }, animationSpec = detailSlideSpec) }
        ) {
            AddBudgetScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = AppRoutes.Notifications.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = detailSlideSpec) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = detailSlideSpec) }
        ) {
            NotificationScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        settingsNavGraph(
            navController = navController,
            paddingValues  = paddingValues
        )

        composable(
            route = AppRoutes.AddTransaction.route,
            enterTransition = { slideInVertically(initialOffsetY = { it }, animationSpec = detailSlideSpec) },
            exitTransition = { slideOutVertically(targetOffsetY = { it }, animationSpec = detailSlideSpec) }
        ) {
            AddTransactionScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToManageCategories = { navController.navigate(AppRoutes.ManageCategories.route) }
            )
        }

        composable(
            route = AppRoutes.TransactionList.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = detailSlideSpec) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = detailSlideSpec) }
        ) {
            TransactionListScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}