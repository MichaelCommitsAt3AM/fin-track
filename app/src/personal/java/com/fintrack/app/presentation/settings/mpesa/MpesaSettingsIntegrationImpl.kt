package com.fintrack.app.presentation.settings.mpesa

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.runtime.Composable
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.fintrack.app.presentation.navigation.SettingsIntegration
import com.fintrack.app.presentation.settings.SettingsItem
import javax.inject.Inject

class MpesaSettingsIntegrationImpl @Inject constructor() : SettingsIntegration {

    companion object {
        const val ROUTE_MPESA_SETTINGS = "mpesa_settings_screen"
        const val ROUTE_MERCHANT_MAPPING = "merchant_mapping_screen"
        const val ROUTE_MPESA_BACKUP = "mpesa_backup_screen"
    }

    override fun NavGraphBuilder.addSettingsRoutes(navController: NavHostController) {
        composable(
            route = ROUTE_MPESA_SETTINGS,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { 1000 },
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -1000 },
                    animationSpec = tween(300)
                )
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -1000 },
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
            MpesaSettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToMerchantMapping = { navController.navigate(ROUTE_MERCHANT_MAPPING) },
                onNavigateToBackup = { navController.navigate(ROUTE_MPESA_BACKUP) }
            )
        }

        composable(route = ROUTE_MPESA_BACKUP) {
            MpesaBackupScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenAccountSettings = {
                    // Account & sync lives on the main Settings screen
                    navController.popBackStack(com.fintrack.app.presentation.navigation.AppRoutes.Settings.route, false)
                }
            )
        }
        
        composable(
            route = ROUTE_MERCHANT_MAPPING,
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
            MerchantMappingScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddCategory = { navController.navigate(com.fintrack.app.presentation.navigation.AppRoutes.AddCategory.route) }
            )
        }
    }

    @Composable
    override fun SettingsEntryPoint(navController: NavHostController) {
        // Divider provided by parent container usually, but we can just render the item
        androidx.compose.material3.HorizontalDivider(
            color = androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
        SettingsItem(
            icon = Icons.Default.PhoneAndroid,
            title = "M-Pesa Settings",
            onClick = { navController.navigate(ROUTE_MPESA_SETTINGS) }
        )
    }

    @Composable
    override fun PrivacyPolicyExtras() {
        com.fintrack.app.presentation.settings.support.PrivacySection(
            title = "M-Pesa Messages and Cloud Backup",
            content = "M-Pesa SMS messages are read and parsed on your phone. The text of your messages is never uploaded, and your M-Pesa data is not uploaded unless you turn on Cloud backup in the M-Pesa settings.\n\nCloud backup is off by default and needs an account. If you turn it on, the parsed details of your M-Pesa transactions (receipt number, amount, date, type, merchant name, paybill or till number, account number, transaction cost) and your category choices are encrypted on your phone with a passphrase that only you know, and only the encrypted data is uploaded. We cannot read it. Your SMS text, M-Pesa balances and phone numbers are never included.\n\nIf you forget the passphrase, the backup cannot be restored. You can delete the backup from the cloud at any time by turning Cloud backup off."
        )
    }
}
