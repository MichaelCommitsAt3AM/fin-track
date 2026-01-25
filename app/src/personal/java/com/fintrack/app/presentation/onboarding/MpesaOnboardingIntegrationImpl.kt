package com.fintrack.app.presentation.onboarding

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.fintrack.app.core.data.preferences.MpesaOnboardingPreferences
import com.fintrack.app.presentation.navigation.OnboardingIntegration
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class MpesaOnboardingIntegrationImpl @Inject constructor(
    private val preferences: MpesaOnboardingPreferences
) : OnboardingIntegration {

    override suspend fun getStartDestinationIfRequired(): String? {
        val isComplete = preferences.isOnboardingCompleted.first()
        return if (!isComplete) com.fintrack.app.presentation.navigation.AppRoutes.MpesaOnboarding.route else null
    }

    override val isEnabled: Boolean = true

    override fun NavGraphBuilder.addOnboardingRoutes(
        navController: NavHostController,
        onOnboardingComplete: () -> Unit
    ) {
        composable(com.fintrack.app.presentation.navigation.AppRoutes.MpesaOnboarding.route) {
            MpesaOnboardingScreen(
                onComplete = onOnboardingComplete,
                onSkip = onOnboardingComplete
            )
        }
    }
}
