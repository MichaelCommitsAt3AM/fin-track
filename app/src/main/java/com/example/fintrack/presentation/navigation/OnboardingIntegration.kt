package com.example.fintrack.presentation.navigation

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController

/**
 * Interface to allow different build variants to inject their own onboarding flows
 * into the main application navigation.
 */
interface OnboardingIntegration {
    
    /**
     * returns the route that should be used as the start destination if onboarding is required.
     * Returns null if no variant-specific onboarding is active/needed.
     */
    suspend fun getStartDestinationIfRequired(): String?

    /**
     * Whether the onboarding integration is enabled/active for this variant.
     * Use this to check if variant-specific onboarding routes are actually available.
     */
    val isEnabled: Boolean

    /**
     * Registers the onboarding routes into the main NavGraph.
     * 
     * @param navController The navigation controller.
     * @param onOnboardingComplete Callback to invoke when onboarding is finished, usually to navigate to Home.
     */
    fun NavGraphBuilder.addOnboardingRoutes(
        navController: NavHostController,
        onOnboardingComplete: () -> Unit
    )
}
