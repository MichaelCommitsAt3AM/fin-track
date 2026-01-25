package com.fintrack.app.core.domain.model.onboarding

/**
 * Represents the different steps in the M-Pesa onboarding flow.
 */
sealed class OnboardingStep {
    data object Welcome : OnboardingStep()
    data object Permissions : OnboardingStep()
    data object Syncing : OnboardingStep()
    data object Insights : OnboardingStep()
    data object CategorySuggestions : OnboardingStep()
    data object RealTimeSetup : OnboardingStep()
    data object Completion : OnboardingStep()
}

/**
 * Represents the state of SMS permissions.
 */
data class PermissionState(
    val readSmsGranted: Boolean = false,
    val receiveSmsGranted: Boolean = false
) {
    /**
     * Returns true if all required permissions are granted.
     */
    val allGranted: Boolean
        get() = readSmsGranted && receiveSmsGranted
}

/**
 * Represents the progress of SMS scanning/syncing.
 */
data class SyncProgress(
    val current: Int = 0,
    val total: Int = 0,
    val status: String = "Preparing...",
    val parsedTransactions: Int = 0,
    val isComplete: Boolean = false
)

/**
 * Represents analysis of a frequent merchant.
 */
data class MerchantFrequency(
    val merchantName: String,
    val transactionCount: Int,
    val totalAmount: Double,
    val suggestedCategory: String?,
    val recentTransactions: List<com.fintrack.app.core.domain.model.MpesaTransaction> = emptyList()
)

/**
 * Represents a detected recurring bill payment.
 */
data class RecurringPaybill(
    val paybillNumber: String,
    val merchantName: String?,
    val frequency: Int, // Number of times detected
    val averageAmount: Double,
    val suggestedCategory: String?
)

/**
 * Container for all onboarding insights.
 */
data class OnboardingInsights(
    val totalTransactions: Int = 0,
    val frequentMerchants: List<MerchantFrequency> = emptyList(),
    val recurringPaybills: List<RecurringPaybill> = emptyList(),
    val categorySuggestions: List<com.fintrack.app.core.domain.model.CategorySuggestion> = emptyList(),
    val isLoading: Boolean = false
)
