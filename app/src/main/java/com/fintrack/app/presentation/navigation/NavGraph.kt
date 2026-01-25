package com.fintrack.app.presentation.navigation

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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.fintrack.app.presentation.add_transaction.AddTransactionScreen
import com.fintrack.app.presentation.auth.BiometricLoginScreen
import com.fintrack.app.presentation.auth.EmailVerificationScreen
import com.fintrack.app.presentation.auth.ForgotPasswordScreen
import com.fintrack.app.presentation.auth.LoginScreen
import com.fintrack.app.presentation.auth.pin.PinLoginScreen
import com.fintrack.app.presentation.auth.RegistrationScreen
import com.fintrack.app.presentation.goals.budgets.AddBudgetScreen
import com.fintrack.app.presentation.goals.debt.AddDebtScreen
import com.fintrack.app.presentation.goals.saving.AddSavingScreen
import com.fintrack.app.presentation.goals.GoalsScreen
import com.fintrack.app.presentation.goals.saving.ManageSavingScreen
import com.fintrack.app.presentation.goals.debt.ManageDebtScreen
import com.fintrack.app.presentation.goals.budgets.ManageBudgetScreen
import com.fintrack.app.presentation.home.HomeScreen
import com.fintrack.app.presentation.navigation.AppRoutes
import com.fintrack.app.presentation.notifications.NotificationScreen
import com.fintrack.app.presentation.profile_setup.ProfileSetupScreen
import com.fintrack.app.presentation.reports.ReportsScreen
import com.fintrack.app.presentation.setup.SetupScreen
import com.fintrack.app.presentation.transactions.TransactionListScreen
import com.fintrack.app.presentation.transactions.ManageTransactionScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    paddingValues: PaddingValues,
    startDestination: String,
    onOpenDrawer: () -> Unit = {},
    settingsIntegration: SettingsIntegration? = null,
    onboardingIntegration: OnboardingIntegration? = null
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
        // --- Variant Specific Onboarding Routes ---
        onboardingIntegration?.apply {
            addOnboardingRoutes(navController) {
                // On complete, navigate to Home and clear back stack
                navController.navigate(BottomNavItem.Home.route) {
                    popUpTo(0) // clear all
                }
            }
        }
        // --- Auth Routes ---
        composable(
            route = AppRoutes.Login.route,
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = authSlideSpec) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = authSlideSpec) }
        ) {
            LoginScreen(
                onNavigateToSetup = {
                    val route = if (onboardingIntegration != null && onboardingIntegration.isEnabled) AppRoutes.MpesaOnboarding.route else AppRoutes.Setup.route
                    navController.navigate(route) {
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
                },
                onNavigateToSetup = {
                    val route = if (onboardingIntegration != null && onboardingIntegration.isEnabled) AppRoutes.MpesaOnboarding.route else AppRoutes.Setup.route
                    navController.navigate(route) {
                        popUpTo(AppRoutes.Register.route) { inclusive = true }
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
                    val route = if (onboardingIntegration != null && onboardingIntegration.isEnabled) AppRoutes.MpesaOnboarding.route else AppRoutes.Setup.route
                    navController.navigate(route) {
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
                    val route = if (onboardingIntegration != null && onboardingIntegration.isEnabled) AppRoutes.MpesaOnboarding.route else AppRoutes.Setup.route
                    navController.navigate(route) {
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
                paddingValues = paddingValues,
                onOpenDrawer = onOpenDrawer
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
            route = BottomNavItem.Goals.route,
            enterTransition = { fadeIn(animationSpec = tabCrossfadeSpec) },
            exitTransition = { fadeOut(animationSpec = tabCrossfadeSpec) }
        ) {
            GoalsScreen(
                navController = navController,
                paddingValues = paddingValues
            )
        }

        composable(
            route = BottomNavItem.Transactions.route,
            enterTransition = { fadeIn(animationSpec = tabCrossfadeSpec) },
            exitTransition = { fadeOut(animationSpec = tabCrossfadeSpec) }
        ) {
            TransactionListScreen(
                onNavigateToTransaction = { transactionId ->
                    navController.navigate(AppRoutes.ManageTransaction.createRoute(transactionId))
                }
            )
        }

        // --- Detail / Feature Screens ---
        // Using slide animations (IntOffset)

        composable(
            route = AppRoutes.AddBudget.route,
            arguments = listOf(
                navArgument("categoryName") { nullable = true },
                navArgument("month") { type = NavType.IntType; defaultValue = -1 },
                navArgument("year") { type = NavType.IntType; defaultValue = -1 }
            ),
            enterTransition = { slideInVertically(initialOffsetY = { it }, animationSpec = detailSlideSpec) },
            exitTransition = { slideOutVertically(targetOffsetY = { it }, animationSpec = detailSlideSpec) }
        ) { backStackEntry ->
            val categoryName = backStackEntry.arguments?.getString("categoryName")
            val month = backStackEntry.arguments?.getInt("month").takeIf { it != -1 }
            val year = backStackEntry.arguments?.getInt("year").takeIf { it != -1 }

            AddBudgetScreen(
                onNavigateBack = { navController.popBackStack() },
                editCategoryName = categoryName,
                editMonth = month,
                editYear = year
            )
        }

        composable(
            route = AppRoutes.AddDebt.route,
            enterTransition = { slideInVertically(initialOffsetY = { it }, animationSpec = detailSlideSpec) },
            exitTransition = { slideOutVertically(targetOffsetY = { it }, animationSpec = detailSlideSpec) }
        ) {
            AddDebtScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = AppRoutes.AddSaving.route,
            enterTransition = { slideInVertically(initialOffsetY = { it }, animationSpec = detailSlideSpec) },
            exitTransition = { slideOutVertically(targetOffsetY = { it }, animationSpec = detailSlideSpec) }
        ) {
            AddSavingScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = AppRoutes.ManageSaving.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = detailSlideSpec) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = detailSlideSpec) }
        ) { backStackEntry ->
            val savingId = backStackEntry.arguments?.getString("savingId") ?: ""
            ManageSavingScreen(
                savingId = savingId,
                onNavigateBack = { navController.popBackStack() },
                onEdit = { 
                    // Navigate to AddSaving screen in edit mode
                    navController.navigate(AppRoutes.AddSaving.route)
                }
            )
        }

        composable(
            route = AppRoutes.ManageDebt.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = detailSlideSpec) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = detailSlideSpec) }
        ) { backStackEntry ->
            val debtId = backStackEntry.arguments?.getString("debtId") ?: ""
            ManageDebtScreen(
                debtId = debtId,
                onNavigateBack = { navController.popBackStack() },
                onEdit = { 
                    // Navigate to AddDebt screen in edit mode
                    navController.navigate(AppRoutes.AddDebt.route)
                }
            )
        }

        composable(
            route = AppRoutes.ManageBudget.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = detailSlideSpec) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = detailSlideSpec) }
        ) { backStackEntry ->
            val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""
            val month = backStackEntry.arguments?.getString("month")?.toIntOrNull() ?: 1
            val year = backStackEntry.arguments?.getString("year")?.toIntOrNull() ?: 2024
            ManageBudgetScreen(
                categoryName = categoryName,
                month = month,
                year = year,
                onNavigateBack = { navController.popBackStack() },
                onEdit = { 
                    // Navigate to AddBudget screen in edit mode
                    navController.navigate(AppRoutes.AddBudget.createRoute(categoryName, month, year))
                }
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
            paddingValues  = paddingValues,
            settingsIntegration = settingsIntegration
        )

        composable(
            route = AppRoutes.ManageTransaction.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = detailSlideSpec) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = detailSlideSpec) }
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getString("transactionId") ?: ""
            ManageTransactionScreen(
                transactionId = transactionId,
                onNavigateBack = { navController.popBackStack() },
                onEdit = {
                    navController.navigate(AppRoutes.AddTransaction.createRoute(transactionId))
                }
            )
        }

        composable(
            route = AppRoutes.AddTransaction.route,
            arguments = listOf(
                navArgument("transactionId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            ),
            enterTransition = { slideInVertically(initialOffsetY = { it }, animationSpec = detailSlideSpec) },
            exitTransition = { slideOutVertically(targetOffsetY = { it }, animationSpec = detailSlideSpec) }
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getString("transactionId")
            AddTransactionScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToManageCategories = { navController.navigate(AppRoutes.ManageCategories.route) },
                onNavigateToPaymentMethods = { navController.navigate(AppRoutes.PaymentMethods.route) },
                transactionId = transactionId
            )
        }
    }
}