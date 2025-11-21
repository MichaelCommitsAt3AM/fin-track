package com.example.fintrack.presentation.settings.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fintrack.core.domain.model.RecurringTransaction
import com.example.fintrack.core.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecurringTransactionsUiState(
    val recurringTransactions: List<RecurringTransaction> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class RecurringTransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecurringTransactionsUiState())
    val uiState: StateFlow<RecurringTransactionsUiState> = _uiState.asStateFlow()

    init {
        loadRecurringTransactions()
    }

    private fun loadRecurringTransactions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            transactionRepository.getAllRecurringTransactions().collect { transactions ->
                _uiState.value = _uiState.value.copy(
                    recurringTransactions = transactions,
                    isLoading = false
                )
            }
        }
    }

    fun deleteRecurringTransaction(transaction: RecurringTransaction) {
        viewModelScope.launch {
            transactionRepository.deleteRecurringTransaction(transaction)
        }
    }
}
