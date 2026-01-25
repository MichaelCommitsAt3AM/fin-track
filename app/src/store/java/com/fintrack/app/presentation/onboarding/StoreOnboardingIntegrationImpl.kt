package com.fintrack.app.presentation.onboarding

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.fintrack.app.presentation.navigation.OnboardingIntegration
import javax.inject.Inject

/**
 * Empty OnboardingIntegration implementation for the store build flavor.
 * Store flavor does not have M-Pesa or any variant-specific onboarding.
 */
class StoreOnboardingIntegrationImpl @Inject constructor() : OnboardingIntegration {
    
    override suspend fun getStartDestinationIfRequired(): String? {
        // No onboarding required for store flavor
        return null
    }

    override val isEnabled: Boolean = false

    override fun NavGraphBuilder.addOnboardingRoutes(
        navController: NavHostController,
        onOnboardingComplete: () -> Unit
    ) {
        // No onboarding routes for store flavor
    }
}
