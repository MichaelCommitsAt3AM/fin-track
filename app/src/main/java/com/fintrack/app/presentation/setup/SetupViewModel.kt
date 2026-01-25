package com.fintrack.app.presentation.setup

import com.fintrack.app.core.util.AppLogger
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.app.core.domain.repository.CategoryRepository
import com.fintrack.app.core.domain.repository.TransactionRepository
import com.fintrack.app.core.domain.repository.UserRepository // ADD THIS
import com.fintrack.app.core.domain.repository.BudgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SetupUiState(
    val isLoading: Boolean = true,
    val currentStep: String = "Initializing...",
    val progress: Float = 0f, // 0.0 to 1.0
    val error: String? = null
)

sealed class SetupEvent {
    object NavigateToHome : SetupEvent()
    data class ShowError(val message: String) : SetupEvent()
}

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val userRepository: UserRepository,
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    private val _eventChannel = Channel<SetupEvent>()
    val events = _eventChannel.receiveAsFlow()

    fun startSetup() {
        viewModelScope.launch {
            try {
                AppLogger.d("SetupViewModel", "Starting setup process")

                // Step 1: Sync user profile first
                _uiState.value = _uiState.value.copy(
                    currentStep = "Loading your profile...",
                    progress = 0.1f
                )
                delay(300)

                val userJob = async {
                    userRepository.syncUserFromCloud()
                }
                userJob.await()
                AppLogger.d("SetupViewModel", "User profile synced")

                // Step 2: Check if data already exists locally
                _uiState.value = _uiState.value.copy(
                    currentStep = "Checking local data...",
                    progress = 0.2f
                )
                delay(300)

                val hasLocalData = checkLocalData()

                if (hasLocalData) {
                    AppLogger.d("SetupViewModel", "Local data found, skipping sync")
                    _uiState.value = _uiState.value.copy(
                        currentStep = "Ready!",
                        progress = 1.0f
                    )
                    delay(500)
                    _eventChannel.send(SetupEvent.NavigateToHome)
                    return@launch
                }

                // Step 3: Sync categories
                _uiState.value = _uiState.value.copy(
                    currentStep = "Syncing categories...",
                    progress = 0.4f
                )

                val categoriesJob = async {
                    categoryRepository.syncCategoriesFromCloud()
                }

                categoriesJob.await()
                AppLogger.d("SetupViewModel", "Categories synced")

                // Step 4: Sync transactions
                _uiState.value = _uiState.value.copy(
                    currentStep = "Syncing transactions...",
                    progress = 0.7f
                )

                val transactionsJob = async {
                    transactionRepository.syncTransactionsFromCloud()
                }

                transactionsJob.await()
                AppLogger.d("SetupViewModel", "Transactions synced")

                // Step 5: Sync budgets
                _uiState.value = _uiState.value.copy(
                    currentStep = "Syncing budgets...",
                    progress = 0.8f
                )

                val budgetsJob = async {
                    budgetRepository.syncBudgetsFromCloud()
                }
                budgetsJob.await()
                AppLogger.d("SetupViewModel", "Budgets synced")

                // Step 6: Sync Recurring transactions
                _uiState.value = _uiState.value.copy(
                    currentStep = "Syncing recurring transactions...",
                    progress = 0.85f
                )

                val recurringJob = async {
                    transactionRepository.syncRecurringTransactionsFromCloud()
                }
                recurringJob.await()
                AppLogger.d("SetupViewModel", "Recurring transactions synced")

                // Step 7: Verify sync
                _uiState.value = _uiState.value.copy(
                    currentStep = "Verifying data...",
                    progress = 0.9f
                )
                delay(500)

                val syncSuccess = verifySync()

                if (syncSuccess) {
                    _uiState.value = _uiState.value.copy(
                        currentStep = "Setup complete!",
                        progress = 1.0f,
                        isLoading = false
                    )
                    delay(500)
                    _eventChannel.send(SetupEvent.NavigateToHome)
                } else {
                    AppLogger.w("SetupViewModel", "Sync verification failed, but proceeding anyway")
                    // Still navigate to home even if verification fails
                    _eventChannel.send(SetupEvent.NavigateToHome)
                }

            } catch (e: Exception) {
                AppLogger.e("SetupViewModel", "Setup failed: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Setup failed: ${e.message}"
                )
                _eventChannel.send(SetupEvent.ShowError(e.message ?: "Unknown error"))
            }
        }
    }

    private suspend fun checkLocalData(): Boolean {
        return try {
            val categories = categoryRepository.getAllCategories().first()
            val transactions = transactionRepository.getAllTransactionsPaged(Int.MAX_VALUE).first()

            val hasData = categories.isNotEmpty() || transactions.isNotEmpty()
            AppLogger.d("SetupViewModel", "Local data check: ${categories.size} categories, ${transactions.size} transactions")
            hasData
        } catch (e: Exception) {
            AppLogger.e("SetupViewModel", "Error checking local data: ${e.message}")
            false
        }
    }

    private suspend fun verifySync(): Boolean {
        return try {
            val categories = categoryRepository.getAllCategories().first()
            val transactions = transactionRepository.getAllTransactionsPaged(Int.MAX_VALUE).first()

            AppLogger.d("SetupViewModel", "Verification: ${categories.size} categories, ${transactions.size} transactions")

            // Consider sync successful if we have at least default categories
            categories.isNotEmpty()
        } catch (e: Exception) {
            AppLogger.e("SetupViewModel", "Verification failed: ${e.message}")
            false
        }
    }

    fun retrySetup() {
        _uiState.value = SetupUiState() // Reset state
        startSetup()
    }
}
