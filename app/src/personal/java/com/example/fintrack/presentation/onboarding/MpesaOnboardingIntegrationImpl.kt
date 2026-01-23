package com.example.fintrack.presentation.onboarding

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.fintrack.core.data.preferences.MpesaOnboardingPreferences
import com.example.fintrack.presentation.navigation.OnboardingIntegration
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class MpesaOnboardingIntegrationImpl @Inject constructor(
    private val preferences: MpesaOnboardingPreferences
) : OnboardingIntegration {

    override suspend fun getStartDestinationIfRequired(): String? {
        val isComplete = preferences.isOnboardingCompleted.first()
        return if (!isComplete) com.example.fintrack.presentation.navigation.AppRoutes.MpesaOnboarding.route else null
    }

    override fun NavGraphBuilder.addOnboardingRoutes(
        navController: NavHostController,
        onOnboardingComplete: () -> Unit
    ) {
        composable(com.example.fintrack.presentation.navigation.AppRoutes.MpesaOnboarding.route) {
            MpesaOnboardingScreen(
                onComplete = onOnboardingComplete,
                onSkip = onOnboardingComplete
            )
        }
    }
}
