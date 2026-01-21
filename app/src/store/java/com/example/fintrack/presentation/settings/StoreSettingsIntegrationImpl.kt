package com.example.fintrack.presentation.settings

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.example.fintrack.presentation.navigation.SettingsIntegration
import javax.inject.Inject

class StoreSettingsIntegrationImpl @Inject constructor() : SettingsIntegration {
    override fun NavGraphBuilder.addSettingsRoutes(navController: NavHostController) {
        // No extra routes for store variant
    }

    @Composable
    override fun SettingsEntryPoint(navController: NavHostController) {
        // No extra settings item for store variant
    }
}
