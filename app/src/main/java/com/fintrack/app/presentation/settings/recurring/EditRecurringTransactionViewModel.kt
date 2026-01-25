package com.fintrack.app.presentation.settings.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.app.core.domain.model.Category
import com.fintrack.app.core.domain.model.RecurrenceFrequency
import com.fintrack.app.core.domain.model.RecurringTransaction
import com.fintrack.app.core.domain.model.TransactionType
import com.fintrack.app.core.domain.repository.TransactionRepository
import com.fintrack.app.core.domain.use_case.GetCategoriesUseCase
import com.google.firebase.auth.FirebaseAuth // ADD THIS
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class EditRecurringTransactionUiState(
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val categories: List<Category> = emptyList(),
    val amount: String = "",
    val description: String = "",
    val selectedCategory: String? = null,
    val startDate: Long = System.currentTimeMillis(),
    val frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class EditRecurringTransactionUiEvent {
    data class OnTypeChange(val type: TransactionType) : EditRecurringTransactionUiEvent()
    data class OnAmountChange(val amount: String) : EditRecurringTransactionUiEvent()
    data class OnDescriptionChange(val description: String) : EditRecurringTransactionUiEvent()
    data class OnCategoryChange(val category: String) : EditRecurringTransactionUiEvent()
    data class OnDateChange(val date: Long) : EditRecurringTransactionUiEvent()
    data class OnFrequencyChange(val frequency: RecurrenceFrequency) : EditRecurringTransactionUiEvent()
    object OnSaveTransaction : EditRecurringTransactionUiEvent()
}

sealed class EditRecurringTransactionEvent {
    object NavigateBack : EditRecurringTransactionEvent()
    object ShowSuccess : EditRecurringTransactionEvent()
}

@HiltViewModel
class EditRecurringTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val firebaseAuth: FirebaseAuth // ADD THIS
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditRecurringTransactionUiState())
    val uiState: StateFlow<EditRecurringTransactionUiState> = _uiState.asStateFlow()

    private val _eventChannel = Channel<EditRecurringTransactionEvent>()
    val events = _eventChannel.receiveAsFlow()

    private var originalTransaction: RecurringTransaction? = null

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            getCategoriesUseCase().collect { categories ->
                _uiState.value = _uiState.value.copy(categories = categories)
            }
        }
    }

    fun loadTransaction(transactionId: String) {
        viewModelScope.launch {
            try {
                val transactions = transactionRepository.getAllRecurringTransactions().first()
                val transaction = transactions.find { it.id.toString() == transactionId }

                if (transaction != null) {
                    originalTransaction = transaction
                    _uiState.value = _uiState.value.copy(
                        transactionType = transaction.type,
                        selectedCategory = transaction.category,
                        amount = transaction.amount.toString(),
                        description = transaction.notes ?: "",
                        startDate = transaction.startDate,
                        frequency = transaction.frequency
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to load transaction")
            }
        }
    }

    fun onEvent(event: EditRecurringTransactionUiEvent) {
        when (event) {
            is EditRecurringTransactionUiEvent.OnTypeChange -> {
                _uiState.value = _uiState.value.copy(
                    transactionType = event.type,
                    selectedCategory = null
                )
            }
            is EditRecurringTransactionUiEvent.OnAmountChange -> _uiState.value = _uiState.value.copy(amount = event.amount)
            is EditRecurringTransactionUiEvent.OnDescriptionChange -> _uiState.value = _uiState.value.copy(description = event.description)
            is EditRecurringTransactionUiEvent.OnCategoryChange -> _uiState.value = _uiState.value.copy(selectedCategory = event.category)
            is EditRecurringTransactionUiEvent.OnDateChange -> _uiState.value = _uiState.value.copy(startDate = event.date)
            is EditRecurringTransactionUiEvent.OnFrequencyChange -> _uiState.value = _uiState.value.copy(frequency = event.frequency)
            is EditRecurringTransactionUiEvent.OnSaveTransaction -> saveTransaction()
        }
    }

    private fun saveTransaction() {
        val state = _uiState.value
        val amountDouble = state.amount.toDoubleOrNull()
        val userId = firebaseAuth.currentUser?.uid // ADD THIS

        if (userId == null) {
            _uiState.value = _uiState.value.copy(error = "User not logged in")
            return
        }
        if (state.selectedCategory == null) {
            _uiState.value = _uiState.value.copy(error = "Please select a category")
            return
        }
        if (amountDouble == null || amountDouble <= 0) {
            _uiState.value = _uiState.value.copy(error = "Please enter a valid amount")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            try {
                // Delete old transaction
                originalTransaction?.let {
                    transactionRepository.deleteRecurringTransaction(it)
                }

                // Insert updated transaction
                val updatedTransaction = RecurringTransaction(
                    id = originalTransaction?.id ?: 0,
                    userId = userId, // ADD THIS
                    type = state.transactionType,
                    amount = amountDouble,
                    category = state.selectedCategory,
                    startDate = state.startDate,
                    frequency = state.frequency,
                    notes = state.description.ifBlank { null }
                )
                transactionRepository.insertRecurringTransaction(updatedTransaction)

                _uiState.value = _uiState.value.copy(isLoading = false)
                _eventChannel.send(EditRecurringTransactionEvent.ShowSuccess)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to update transaction: ${e.message}"
                )
            }
        }
    }

    fun formatMillisToDate(millis: Long): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(Date(millis))
    }
}
