package com.fintrack.app.presentation.settings.mpesa

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
    }

    override fun NavGraphBuilder.addSettingsRoutes(navController: NavHostController) {
        composable(route = ROUTE_MPESA_SETTINGS) {
            MpesaSettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToMerchantMapping = { navController.navigate(ROUTE_MERCHANT_MAPPING) }
            )
        }
        
        composable(route = ROUTE_MERCHANT_MAPPING) {
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
}
