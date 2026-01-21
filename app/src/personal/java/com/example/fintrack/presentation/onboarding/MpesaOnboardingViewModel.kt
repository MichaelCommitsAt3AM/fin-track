package com.example.fintrack.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fintrack.core.analytics.MpesaAnalyticsEngine
import com.example.fintrack.core.data.preferences.MpesaOnboardingPreferences
import com.example.fintrack.core.domain.model.LookbackPeriod
import com.example.fintrack.core.domain.model.onboarding.OnboardingInsights
import com.example.fintrack.core.domain.model.onboarding.OnboardingStep
import com.example.fintrack.core.domain.model.onboarding.PermissionState
import com.example.fintrack.core.domain.model.onboarding.SyncProgress
import com.example.fintrack.core.domain.repository.MpesaTransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for M-Pesa onboarding flow.
 * Manages navigation between onboarding steps and permission state.
 */
@HiltViewModel
class MpesaOnboardingViewModel @Inject constructor(
    private val onboardingPrefs: MpesaOnboardingPreferences,
    private val mpesaRepository: MpesaTransactionRepository,
    private val analyticsEngine: MpesaAnalyticsEngine
) : ViewModel() {
    
    private val _currentStep = MutableStateFlow<OnboardingStep>(OnboardingStep.Welcome)
    val currentStep: StateFlow<OnboardingStep> = _currentStep
    
    private val _permissionState = MutableStateFlow(PermissionState())
    val permissionState: StateFlow<PermissionState> = _permissionState
    
    private val _syncProgress = MutableStateFlow(SyncProgress())
    val syncProgress: StateFlow<SyncProgress> = _syncProgress
    
    private val _insights = MutableStateFlow(OnboardingInsights())
    val insights: StateFlow<OnboardingInsights> = _insights
    
    private val _selectedLookbackPeriod = MutableStateFlow(LookbackPeriod.THREE_MONTHS)
    val selectedLookbackPeriod: StateFlow<LookbackPeriod> = _selectedLookbackPeriod
    
    /**
     * Navigate to the next step in the onboarding flow.
     */
    fun nextStep() {
        _currentStep.value = when (_currentStep.value) {
            OnboardingStep.Welcome -> OnboardingStep.Permissions
            OnboardingStep.Permissions -> OnboardingStep.Syncing
            OnboardingStep.Syncing -> OnboardingStep.Insights
            OnboardingStep.Insights -> OnboardingStep.Completion // Start with skipping category step for now
            OnboardingStep.CategorySuggestions -> OnboardingStep.Completion
            OnboardingStep.RealTimeSetup -> OnboardingStep.Completion
            OnboardingStep.Completion -> OnboardingStep.Completion
        }
    }
    
    /**
     * Navigate to the previous step.
     */
    fun previousStep() {
        _currentStep.value = when (_currentStep.value) {
            OnboardingStep.Permissions -> OnboardingStep.Welcome
            OnboardingStep.Syncing -> OnboardingStep.Permissions
            OnboardingStep.Insights -> OnboardingStep.Syncing
            OnboardingStep.Completion -> OnboardingStep.Insights
            else -> OnboardingStep.Welcome
        }
    }
    
    /**
     * Update permission state when permissions are granted/denied.
     */
    fun onPermissionsGranted(readSms: Boolean, receiveSms: Boolean) {
        _permissionState.value = PermissionState(readSms, receiveSms)
        
        // Auto-advance if all permissions granted
        if (readSms && receiveSms) {
            nextStep()
        }
    }
    
    /**
     * Set the lookback period for SMS scanning.
     */
    fun setLookbackPeriod(period: LookbackPeriod) {
        _selectedLookbackPeriod.value = period
    }
    
    /**
     * Start initial M-Pesa SMS sync with progress tracking.
     */
    fun startInitialSync(lookbackPeriod: LookbackPeriod) {
        viewModelScope.launch {
            try {
                _syncProgress.value = SyncProgress(
                    status = "Starting SMS scan...",
                    isComplete = false
                )
                
                // Delay for UI feedback
                delay(500)
                
                _syncProgress.value = SyncProgress(
                    status = "Scanning M-Pesa messages...",
                    isComplete = false
                )
                
                // Perform actual sync
                mpesaRepository.syncMpesaSms(lookbackPeriod)
                
                // Generate insights
                _syncProgress.value = _syncProgress.value.copy(
                    status = "Analyzing spending patterns...",
                    parsedTransactions = mpesaRepository.getTransactionCount()
                )
                
                // Run analytics
                val analysis = analyticsEngine.generateInsights()
                _insights.value = analysis

                _syncProgress.value = SyncProgress(
                    status = "Analysis complete!",
                    parsedTransactions = analysis.totalTransactions,
                    isComplete = true
                )
                
                // Save lookback period preference
                onboardingPrefs.setLookbackPeriod(
                    when (lookbackPeriod) {
                        LookbackPeriod.ONE_MONTH -> 1
                        LookbackPeriod.THREE_MONTHS -> 3
                        LookbackPeriod.SIX_MONTHS -> 6
                        LookbackPeriod.ONE_YEAR -> 12
                    }
                )
                
                // Auto-advance after 1.5 seconds
                delay(1500)
                nextStep() // Go to Insights
                
            } catch (e: Exception) {
                _syncProgress.value = SyncProgress(
                    status = "Error: ${e.message ?: "Unknown error"}",
                    isComplete = false
                )
            }
        }
    }
    
    /**
     * Mark onboarding as complete and save to preferences.
     */
    fun completeOnboarding() {
        viewModelScope.launch {
            onboardingPrefs.markOnboardingComplete()
        }
    }
}
