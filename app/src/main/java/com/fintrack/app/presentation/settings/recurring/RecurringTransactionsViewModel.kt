package com.fintrack.app.presentation.settings.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.app.core.data.local.LocalAuthManager
import com.fintrack.app.core.domain.model.Currency
import com.fintrack.app.core.domain.model.RecurringTransaction
import com.fintrack.app.core.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecurringTransactionsUiState(
    val recurringTransactions: List<RecurringTransaction> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class RecurringTransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val localAuthManager: LocalAuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecurringTransactionsUiState())
    val uiState: StateFlow<RecurringTransactionsUiState> = _uiState.asStateFlow()

    val currencyPreference = localAuthManager.currencyPreference
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Currency.KSH
        )

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
