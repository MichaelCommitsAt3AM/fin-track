package com.example.fintrack.presentation.navigation

import androidx.biometric.BiometricManager
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.example.fintrack.presentation.settings.CategoryDetailScreen
import com.example.fintrack.presentation.settings.security.ManageSignInMethodsScreen
import com.example.fintrack.presentation.settings.categories.ManageCategoriesScreen
import com.example.fintrack.presentation.settings.SettingsScreen
import com.example.fintrack.presentation.settings.biometric.BiometricSetupScreen
import com.example.fintrack.presentation.settings.biometric.FingerprintSetupScreen
import com.example.fintrack.presentation.settings.profile.ManageProfileScreen
import com.example.fintrack.presentation.settings.recurring.EditRecurringTransactionScreen
import com.example.fintrack.presentation.settings.recurring.RecurringTransactionsScreen
import com.example.fintrack.presentation.settings.security.SetPasswordScreen
import com.example.fintrack.presentation.settings.notifications.NotificationSettingsScreen
import com.example.fintrack.presentation.settings.payment_methods.PaymentMethodsScreen
import com.example.fintrack.presentation.settings.support.HelpSupportScreen
import com.example.fintrack.presentation.settings.support.PrivacyPolicyScreen
import com.example.fintrack.presentation.settings.support.TermsOfServiceScreen

fun NavGraphBuilder.settingsNavGraph(
    navController: NavHostController,
    paddingValues: PaddingValues,
) {
    navigation(
        route = AppRoutes.SettingsGraph.route,
        startDestination = AppRoutes.Settings.route
    ) {
        // 1. Main Settings Screen
        composable(route = AppRoutes.Settings.route) {
            SettingsScreen(
                navController = navController,
                onNavigateToLogin = {
                    // Instead of navigating, restart the activity to clear all ViewModels
                    // This prevents Firestore permission errors from active listeners
                    (navController.context as? android.app.Activity)?.apply {
                        finish()
                        startActivity(intent)
                    }
                },
                onNavigateBack = { navController.popBackStack() },
                onNavigateToManageCategories = {
                    navController.navigate(AppRoutes.ManageCategories.route)
                },
                onNavigateToSignInMethods = { // <--- Pass the navigation call
                    navController.navigate(AppRoutes.ManageSignInMethods.route)
                },
                onNavigateToPaymentMethods = {
                    navController.navigate(AppRoutes.PaymentMethods.route)
                },
                paddingValues = paddingValues
            )
        }

        // 2. Manage Profile Screen
        composable(
            route = AppRoutes.ManageProfile.route,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { 1000 },
                    animationSpec = tween(300)
                )
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { 1000 },
                    animationSpec = tween(300)
                )
            }
        ) {
            ManageProfileScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 3: Manage Sign-in Methods Screen
        composable(
            route = AppRoutes.ManageSignInMethods.route,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { 1000 },
                    animationSpec = tween(300)
                )
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { 1000 },
                    animationSpec = tween(300)
                )
            }
        ) {
            ManageSignInMethodsScreen(
                onNavigateBack = { navController.popBackStack() },
                navController = navController
            )
        }

        // 4: Set Password Screen
        composable(
            route = AppRoutes.SetPassword.route,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { 1000 },
                    animationSpec = tween(300)
                )
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { 1000 },
                    animationSpec = tween(300)
                )
            }
        ) {
            SetPasswordScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 4a: Notification Settings Screen
        composable(
            route = AppRoutes.NotificationSettings.route,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { 1000 },
                    animationSpec = tween(300)
                )
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { 1000 },
                    animationSpec = tween(300)
                )
            }
        ) {
            NotificationSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 4b: Payment Methods Screen
        composable(
            route = AppRoutes.PaymentMethods.route,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { 1000 },
                    animationSpec = tween(300)
                )
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { 1000 },
                    animationSpec = tween(300)
                )
            }
        ) {
            PaymentMethodsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 5. Manage Categories Screen
        composable(
            route = AppRoutes.ManageCategories.route,
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { 1000 },
                    animationSpec = tween(300)
                )
            }
        ) {
            ManageCategoriesScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddCategory = {
                    navController.navigate(AppRoutes.AddCategory.route)
                },
                onNavigateToEditCategory = { categoryName ->
                    navController.navigate(
                        AppRoutes.EditCategory.createRoute(
                            categoryName
                        )
                    )
                }
            )
        }

        // 6. Recurring Transactions Screen
        composable(
            route = AppRoutes.RecurringTransactions.route,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { 1000 },
                    animationSpec = tween(300)
                )
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { 1000 },
                    animationSpec = tween(300)
                )
            }
        ) {
            RecurringTransactionsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditRecurringTransaction = { transactionId ->
                    navController.navigate(
                        AppRoutes.EditRecurringTransaction.createRoute(
                            transactionId
                        )
                    )
                }
            )
        }

        // 7. Add Category Screen (Slide Up)
        composable(
            route = AppRoutes.AddCategory.route,
            enterTransition = {
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(300)
                )
            }
        ) {
            CategoryDetailScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 8. Edit Category Screen (Slide Over)
        composable(
            route = AppRoutes.EditCategory.route,
            arguments = listOf(navArgument("categoryName") { type = NavType.StringType }),
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { 1000 },
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { 1000 },
                    animationSpec = tween(300)
                )
            }
        ) {
            CategoryDetailScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 9. Edit Recurring Transaction Screen (Slide Over)
        composable(
            route = AppRoutes.EditRecurringTransaction.route,
            arguments = listOf(navArgument("transactionId") { type = NavType.StringType }),
            enterTransition = {
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(300)
                )
            }
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getString("transactionId") ?: ""
            EditRecurringTransactionScreen(
                recurringTransactionId = transactionId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToManageCategories = { navController.navigate(AppRoutes.ManageCategories.route) }
            )
        }

        // 10. Biometric Setup Screen (PIN)
        composable(
            route = AppRoutes.BiometricSetup.route,
            enterTransition = {
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(300)
                )
            }
        ) {
            val context = LocalContext.current

            BiometricSetupScreen(
                onSetupComplete = { _ ->
                    // PIN is saved. Now check if we can offer Fingerprint.
                    val biometricManager = BiometricManager.from(context)
                    val canAuthenticate =
                        biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)

                    if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
                        // Has hardware -> Go to Fingerprint Setup
                        navController.navigate(AppRoutes.FingerprintSetup.route)
                    } else {
                        // No hardware -> Finish (PIN is the only option)
                        // You might want to enable the biometric flag here if you treat PIN as "Biometric Login" feature
                        // For now, simply closing means PIN is saved but flag IS_BIOMETRIC_ENABLED is false (Settings toggle will look OFF)
                        navController.popBackStack()
                    }
                },
                onCancel = { navController.popBackStack() }
            )
        }

        // 11. Fingerprint Setup Screen (NEW)
        composable(
            route = AppRoutes.FingerprintSetup.route,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { 1000 },
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { 1000 },
                    animationSpec = tween(300)
                )
            }
        ) {
            FingerprintSetupScreen(
                onSetupComplete = {
                    // Success! Pop back to Settings (skipping PIN screen in backstack usually preferred, but popBackStack works)
                    navController.popBackStack(AppRoutes.Settings.route, inclusive = false)
                },
                onSkip = {
                    // User skipped fingerprint. Pop back to Settings.
                    navController.popBackStack(AppRoutes.Settings.route, inclusive = false)
                }
            )
        }

        // 12. Privacy Policy Screen
        composable(
            route = AppRoutes.PrivacyPolicy.route,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { 1000 },
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { 1000 },
                    animationSpec = tween(300)
                )
            }
        ) {
            PrivacyPolicyScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        // 13. Terms of Service Screen
        composable(
            route = AppRoutes.TermsOfService.route,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { 1000 },
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { 1000 },
                    animationSpec = tween(300)
                )
            }
        ) {
            TermsOfServiceScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        // 14. Help & Support Screen
        composable(
            route = AppRoutes.HelpSupport.route,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { 1000 },
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { 1000 },
                    animationSpec = tween(300)
                )
            }
        ) {
            HelpSupportScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}