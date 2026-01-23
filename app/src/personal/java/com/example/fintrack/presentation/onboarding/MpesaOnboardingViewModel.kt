package com.example.fintrack.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fintrack.core.analytics.CategorySuggestionAnalyzer
import com.example.fintrack.core.analytics.MpesaAnalyticsEngine
import com.example.fintrack.core.data.local.dao.MpesaCategoryMappingDao
import com.example.fintrack.core.data.preferences.MpesaOnboardingPreferences
import com.example.fintrack.core.domain.model.LookbackPeriod
import com.example.fintrack.core.domain.model.onboarding.OnboardingInsights
import com.example.fintrack.core.domain.model.onboarding.OnboardingStep
import com.example.fintrack.core.domain.model.onboarding.PermissionState
import com.example.fintrack.core.domain.model.onboarding.SyncProgress
import com.example.fintrack.core.domain.repository.CategoryRepository
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
    private val analyticsEngine: MpesaAnalyticsEngine,
    private val categoryRepository: CategoryRepository,
    private val mappingDao: MpesaCategoryMappingDao,
    private val suggestionAnalyzer: CategorySuggestionAnalyzer,
    // [NEW] Cloud Sync Repositories
    private val userRepository: com.example.fintrack.core.domain.repository.UserRepository,
    private val transactionRepository: com.example.fintrack.core.domain.repository.TransactionRepository,
    private val budgetRepository: com.example.fintrack.core.domain.repository.BudgetRepository
) : ViewModel() {

    // Cache transactions for debug logging in CategorySuggestionsStep
    private var recentTransactions: List<com.example.fintrack.core.domain.model.MpesaTransaction> = emptyList()

    init {
        startBackgroundSync()
    }
    
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
            OnboardingStep.Insights -> {
                // Check if we have suggestions to show
                if (_insights.value.categorySuggestions.isNotEmpty()) {
                    OnboardingStep.CategorySuggestions
                } else {
                    OnboardingStep.Completion
                }
            }
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
            OnboardingStep.CategorySuggestions -> OnboardingStep.Insights
            OnboardingStep.Completion -> if (_insights.value.categorySuggestions.isNotEmpty()) OnboardingStep.CategorySuggestions else OnboardingStep.Insights
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
                val transactions = mpesaRepository.syncMpesaSms(lookbackPeriod)
                recentTransactions = transactions
                
                // Generate insights
                _syncProgress.value = _syncProgress.value.copy(
                    status = "Analyzing spending patterns...",
                    parsedTransactions = transactions.size
                )
                
                // Run analytics
                val analysis = analyticsEngine.generateInsights()
                
                // Generate category suggestions
                val suggestions = suggestionAnalyzer.analyzeSuggestions(transactions)
                
                _insights.value = analysis.copy(
                    categorySuggestions = suggestions
                )

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
     * User accepted category suggestions.
     * Creates categories and maps M-Pesa transactions to them.
     */
    fun acceptCategorySuggestions() {
        viewModelScope.launch {
            val suggestions = _insights.value.categorySuggestions
            
            // 1. Batch create categories
            // Note: In a real app we might check for existing dupes more carefully
            // but insertAllCategories generally handles conflicts depending on repo impl
            // Here we assume repo or room handles it. CategoryRepository doesn't specify behavior
            // but name is usually unique locally.
            // Let's create proper Category objects.
            val currentUser = userRepository.getCurrentUserOnce()
            val currentUserId = currentUser?.userId ?: "local"

            val categories = suggestions.map { suggestion ->
                com.example.fintrack.core.domain.model.Category(
                    name = suggestion.categoryName,
                    userId = currentUserId,
                    iconName = suggestion.iconName,
                    colorHex = suggestion.colorHex,
                    type = com.example.fintrack.core.domain.model.CategoryType.EXPENSE,
                    isDefault = false
                )
            }
            categoryRepository.insertAllCategories(categories)
            
            // 2. Create M-Pesa mappings
            val mappings = suggestions.flatMap { suggestion ->
                suggestion.mpesaReceiptNumbers.map { receipt ->
                    com.example.fintrack.core.data.local.model.MpesaCategoryMappingEntity(
                        mpesaReceiptNumber = receipt,
                        categoryName = suggestion.categoryName
                    )
                }
            }
            mappingDao.insertAll(mappings)
            
            // 3. Move to completion
            nextStep()
        }
    }
    
    /**
     * Log details of the first 5 transactions for each suggestion for verification.
     * Called when CategorySuggestionsStep is initialized.
     */
    fun logCategoryDebugInfo() {
        val suggestions = _insights.value.categorySuggestions
        if (suggestions.isEmpty()) return

        com.example.fintrack.core.util.AppLogger.d("CategoryDebug", "=== Category Suggestions Debug Info ===")
        
        // Index transactions for faster lookup
        val transactionMap = recentTransactions.associateBy { it.mpesaReceiptNumber }
        
        suggestions.forEach { suggestion ->
            com.example.fintrack.core.util.AppLogger.d("CategoryDebug", "Category: ${suggestion.categoryName} (${suggestion.transactionCount} txns)")
            
            // Log first 5 transactions
            val sampleTxns = suggestion.mpesaReceiptNumbers.take(5)
            sampleTxns.forEachIndexed { index, receipt ->
                val txn = transactionMap[receipt]
                if (txn != null) {
                    com.example.fintrack.core.util.AppLogger.d(
                        "CategoryDebug", 
                        "  [${index + 1}] $receipt | KES ${txn.amount} | ${txn.merchantName ?: txn.paybillNumber ?: "Unknown"} | ${java.text.SimpleDateFormat("dd/MM/yy", java.util.Locale.getDefault()).format(txn.timestamp)} | Clues: ${txn.smartClues}"
                    )
                } else {
                    com.example.fintrack.core.util.AppLogger.d("CategoryDebug", "  [${index + 1}] $receipt (Transaction not found in cache)")
                }
            }
            if (suggestion.transactionCount > 5) {
                com.example.fintrack.core.util.AppLogger.d("CategoryDebug", "  ... and ${suggestion.transactionCount - 5} more")
            }
        }
        com.example.fintrack.core.util.AppLogger.d("CategoryDebug", "=======================================")
    }

    /**
     * Mark onboarding as complete and save to preferences.
     */
    fun completeOnboarding() {
        viewModelScope.launch {
            onboardingPrefs.markOnboardingComplete()
        }
    }

    /**
     * Starts background synchronization of cloud data (User, Categories, Transactions, Budgets).
     * This replaces the logic previously visited in SetupScreen.
     */
    private fun startBackgroundSync() {
        viewModelScope.launch {
            try {
                com.example.fintrack.core.util.AppLogger.d("MpesaOnboardingViewModel", "Starting background cloud sync...")

                // 1. Sync User Profile
                launch {
                    try {
                        userRepository.syncUserFromCloud()
                        com.example.fintrack.core.util.AppLogger.d("MpesaOnboardingViewModel", "User profile synced in background")
                    } catch (e: Exception) {
                        com.example.fintrack.core.util.AppLogger.e("MpesaOnboardingViewModel", "Background user sync failed", e)
                    }
                }

                // 2. Sync Categories
                launch {
                    try {
                        categoryRepository.syncCategoriesFromCloud()
                        com.example.fintrack.core.util.AppLogger.d("MpesaOnboardingViewModel", "Categories synced in background")
                    } catch (e: Exception) {
                        com.example.fintrack.core.util.AppLogger.e("MpesaOnboardingViewModel", "Background category sync failed", e)
                    }
                }

                // 3. Sync Transactions & Budgets (Sequential or Parallel depending on dependency)
                launch {
                    try {
                        transactionRepository.syncTransactionsFromCloud()
                        com.example.fintrack.core.util.AppLogger.d("MpesaOnboardingViewModel", "Transactions synced in background")

                        // Recurring often depends on base transaction logic
                        transactionRepository.syncRecurringTransactionsFromCloud()
                        com.example.fintrack.core.util.AppLogger.d("MpesaOnboardingViewModel", "Recurring transactions synced in background")
                    } catch (e: Exception) {
                        com.example.fintrack.core.util.AppLogger.e("MpesaOnboardingViewModel", "Background transaction sync failed", e)
                    }
                }

                launch {
                    try {
                        budgetRepository.syncBudgetsFromCloud()
                        com.example.fintrack.core.util.AppLogger.d("MpesaOnboardingViewModel", "Budgets synced in background")
                    } catch (e: Exception) {
                        com.example.fintrack.core.util.AppLogger.e("MpesaOnboardingViewModel", "Background budget sync failed", e)
                    }
                }

            } catch (e: Exception) {
                com.example.fintrack.core.util.AppLogger.e("MpesaOnboardingViewModel", "Error initiating background sync", e)
            }
        }
    }
}
