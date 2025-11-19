package com.example.fintrack.presentation.setup

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fintrack.core.domain.repository.CategoryRepository
import com.example.fintrack.core.domain.repository.TransactionRepository
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
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    private val _eventChannel = Channel<SetupEvent>()
    val events = _eventChannel.receiveAsFlow()

    fun startSetup() {
        viewModelScope.launch {
            try {
                Log.d("SetupViewModel", "Starting setup process")

                // Step 1: Check if data already exists locally
                _uiState.value = _uiState.value.copy(
                    currentStep = "Checking local data...",
                    progress = 0.1f
                )
                delay(300) // Small delay for UX

                val hasLocalData = checkLocalData()

                if (hasLocalData) {
                    Log.d("SetupViewModel", "Local data found, skipping sync")
                    _uiState.value = _uiState.value.copy(
                        currentStep = "Ready!",
                        progress = 1.0f
                    )
                    delay(500)
                    _eventChannel.send(SetupEvent.NavigateToHome)
                    return@launch
                }

                // Step 2: Sync categories
                _uiState.value = _uiState.value.copy(
                    currentStep = "Syncing categories...",
                    progress = 0.3f
                )

                val categoriesJob = async {
                    categoryRepository.syncCategoriesFromCloud()
                }

                categoriesJob.await()
                Log.d("SetupViewModel", "Categories synced")

                // Step 3: Sync transactions
                _uiState.value = _uiState.value.copy(
                    currentStep = "Syncing transactions...",
                    progress = 0.6f
                )

                val transactionsJob = async {
                    transactionRepository.syncTransactionsFromCloud()
                }

                transactionsJob.await()
                Log.d("SetupViewModel", "Transactions synced")

                // Step 4: Verify sync
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
                    Log.w("SetupViewModel", "Sync verification failed, but proceeding anyway")
                    // Still navigate to home even if verification fails
                    _eventChannel.send(SetupEvent.NavigateToHome)
                }

            } catch (e: Exception) {
                Log.e("SetupViewModel", "Setup failed: ${e.message}", e)
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
            val transactions = transactionRepository.getAllTransactions().first()

            val hasData = categories.isNotEmpty() || transactions.isNotEmpty()
            Log.d("SetupViewModel", "Local data check: ${categories.size} categories, ${transactions.size} transactions")
            hasData
        } catch (e: Exception) {
            Log.e("SetupViewModel", "Error checking local data: ${e.message}")
            false
        }
    }

    private suspend fun verifySync(): Boolean {
        return try {
            val categories = categoryRepository.getAllCategories().first()
            val transactions = transactionRepository.getAllTransactions().first()

            Log.d("SetupViewModel", "Verification: ${categories.size} categories, ${transactions.size} transactions")

            // Consider sync successful if we have at least default categories
            categories.isNotEmpty()
        } catch (e: Exception) {
            Log.e("SetupViewModel", "Verification failed: ${e.message}")
            false
        }
    }

    fun retrySetup() {
        _uiState.value = SetupUiState() // Reset state
        startSetup()
    }
}
