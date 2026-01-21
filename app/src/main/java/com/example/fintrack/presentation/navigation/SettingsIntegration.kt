package com.example.fintrack.presentation.navigation

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.compose.runtime.Composable

/**
 * Interface to allow different build variants (personal vs store) to inject
 * their own settings screens and navigation routes into the main Settings flow
 * without the main module having direct dependencies on them.
 */
interface SettingsIntegration {
    
    /**
     * Called within the settingsNavGraph builder to allow the variant to
     * register its own composable routes.
     */
    fun NavGraphBuilder.addSettingsRoutes(navController: NavHostController)

    /**
     * Returns a composable that renders the entry point (button) in the
     * main SettingsScreen list.
     * 
     * @param navController The controller to navigate to the variant's routes.
     */
    @Composable
    fun SettingsEntryPoint(navController: NavHostController)
}
