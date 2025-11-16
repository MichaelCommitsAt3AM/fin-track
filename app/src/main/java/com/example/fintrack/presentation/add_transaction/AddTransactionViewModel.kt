package com.example.fintrack.presentation.add_transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fintrack.core.domain.model.Category
import com.example.fintrack.core.domain.model.Transaction
import com.example.fintrack.core.domain.model.TransactionType
import com.example.fintrack.core.domain.use_case.AddTransactionUseCase
import com.example.fintrack.core.domain.use_case.GetCategoriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class AddTransactionUiState(
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val categories: List<Category> = emptyList(),
    val amount: String = "",
    val description: String = "",
    val selectedCategory: String? = null,
    val date: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class AddTransactionUiEvent {
    data class OnTypeChange(val type: TransactionType) : AddTransactionUiEvent()
    data class OnAmountChange(val amount: String) : AddTransactionUiEvent()
    data class OnDescriptionChange(val description: String) : AddTransactionUiEvent()
    data class OnCategoryChange(val category: String) : AddTransactionUiEvent()
    data class OnDateChange(val date: Long) : AddTransactionUiEvent()
    object OnSaveTransaction : AddTransactionUiEvent()
}

sealed class AddTransactionEvent {
    object NavigateBack : AddTransactionEvent()
}

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val addTransactionUseCase: AddTransactionUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddTransactionUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventChannel = Channel<AddTransactionEvent>()
    val events = _eventChannel.receiveAsFlow()

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

    fun onEvent(event: AddTransactionUiEvent) {
        when (event) {
            is AddTransactionUiEvent.OnTypeChange -> _uiState.value = _uiState.value.copy(transactionType = event.type)
            is AddTransactionUiEvent.OnAmountChange -> _uiState.value = _uiState.value.copy(amount = event.amount)
            is AddTransactionUiEvent.OnDescriptionChange -> _uiState.value = _uiState.value.copy(description = event.description)
            is AddTransactionUiEvent.OnCategoryChange -> _uiState.value = _uiState.value.copy(selectedCategory = event.category)
            is AddTransactionUiEvent.OnDateChange -> _uiState.value = _uiState.value.copy(date = event.date)
            is AddTransactionUiEvent.OnSaveTransaction -> saveTransaction()
        }
    }

    private fun saveTransaction() {
        val state = _uiState.value
        val amountDouble = state.amount.toDoubleOrNull()

        // --- Validation ---
        if (state.selectedCategory == null) {
            _uiState.value = _uiState.value.copy(error = "Please select a category")
            return
        }
        if (amountDouble == null || amountDouble <= 0) {
            _uiState.value = _uiState.value.copy(error = "Please enter a valid amount")
            return
        }
        // -----------------

        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            // Create a unique ID. We use timestamp + random to ensure uniqueness.
            // In a real app, you might let Room generate this, but we need it for Firestore.
            val uniqueId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt() + (0..1000).random()

            val transaction = Transaction(
                id = uniqueId,
                type = state.transactionType,
                amount = amountDouble,
                category = state.selectedCategory,
                date = state.date,
                notes = state.description,
                paymentMethod = "Card", // Hardcoded for now
                tags = null // Hardcoded for now
            )

            addTransactionUseCase(transaction)

            _uiState.value = _uiState.value.copy(isLoading = false)
            _eventChannel.send(AddTransactionEvent.NavigateBack)
        }
    }

    fun formatMillisToDate(millis: Long): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(Date(millis))
    }

    fun parseDateToMillis(dateString: String): Long {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.parse(dateString)?.time ?: System.currentTimeMillis()
    }
}