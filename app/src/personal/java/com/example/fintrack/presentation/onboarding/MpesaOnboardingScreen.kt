package com.example.fintrack.presentation.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fintrack.core.domain.model.onboarding.OnboardingStep
import com.example.fintrack.presentation.onboarding.steps.CompletionStep
import com.example.fintrack.presentation.onboarding.steps.InsightsStep
import com.example.fintrack.presentation.onboarding.steps.PermissionsStep
import com.example.fintrack.presentation.onboarding.steps.SyncingStep
import com.example.fintrack.presentation.onboarding.steps.WelcomeStep

/**
 * Main M-Pesa onboarding screen container.
 * Displays different steps based on current onboarding state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MpesaOnboardingScreen(
    onComplete: () -> Unit,
    onSkip: () -> Unit,
    viewModel: MpesaOnboardingViewModel = hiltViewModel()
) {
    val currentStep by viewModel.currentStep.collectAsState()
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .statusBarsPadding()
        ) {
            // Progress indicator
            val targetProgress = when (currentStep) {
                OnboardingStep.Welcome -> 0.15f
                OnboardingStep.Permissions -> 0.30f
                OnboardingStep.Syncing -> 0.45f
                OnboardingStep.Insights -> 0.60f
                OnboardingStep.CategorySuggestions -> 0.80f
                OnboardingStep.Completion -> 1f
                else -> 0f
            }

            val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
                targetValue = targetProgress,
                label = "OnboardingProgress"
            )

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth()
            )
            
            // Step content
            when (currentStep) {
                OnboardingStep.Welcome -> WelcomeStep(
                    onNext = { viewModel.nextStep() },
                    onSkip = onSkip
                )
                
                OnboardingStep.Permissions -> PermissionsStep(
                    onPermissionsGranted = { readSms, receiveSms ->
                        viewModel.onPermissionsGranted(readSms, receiveSms)
                    },
                    onBack = { viewModel.previousStep() },
                    onSkip = onSkip
                )
                
                OnboardingStep.Syncing -> SyncingStep(
                    viewModel = viewModel
                )
                
                OnboardingStep.Insights -> InsightsStep(
                    viewModel = viewModel,
                    onNext = { viewModel.nextStep() }
                )
                
                OnboardingStep.CategorySuggestions -> com.example.fintrack.presentation.onboarding.steps.CategorySuggestionsStep(
                    viewModel = viewModel,
                    onNext = { viewModel.nextStep() } // Used for skipping
                )
                
                OnboardingStep.Completion -> CompletionStep(
                    onComplete = {
                        viewModel.completeOnboarding()
                        onComplete()
                    }
                )
                
                else -> {
                    // Future steps (Insights, CategorySuggestions, etc.)
                }
            }
        }
    }
}
