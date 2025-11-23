package com.example.fintrack.presentation.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.material.icons.filled.*
import com.example.fintrack.core.domain.model.CategoryType
import com.example.fintrack.core.domain.use_case.GetCategoriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

// UI State for Budget List Screen
data class BudgetListUiState(
    val budgets: List<BudgetItem> = emptyList(),
    val isLoading: Boolean = false
)

// UI State for Add/Edit Budget Screen
data class AddBudgetUiState(
    val amount: String = "",
    val selectedCategory: String? = null,
    val categories: List<String> = emptyList(), // Changed from hardcoded list to empty
    val month: String = getCurrentMonth(),
    val isLoading: Boolean = false
)

// Events for Add/Edit Screen
sealed class AddBudgetUiEvent {
    data class OnAmountChange(val amount: String) : AddBudgetUiEvent()
    data class OnCategoryChange(val category: String) : AddBudgetUiEvent()
    data class OnMonthChange(val month: String) : AddBudgetUiEvent()
    object OnSaveBudget : AddBudgetUiEvent()
}

// Events for List Screen
sealed class BudgetListUiEvent {
    data class OnDeleteBudget(val budget: BudgetItem) : BudgetListUiEvent()
    object OnRefresh : BudgetListUiEvent()
}

// Navigation Events
sealed class BudgetEvent {
    object NavigateBack : BudgetEvent()
    data class ShowError(val message: String) : BudgetEvent()
    data class ShowSuccess(val message: String) : BudgetEvent()
}

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val getCategoriesUseCase: GetCategoriesUseCase // Inject GetCategoriesUseCase
    // TODO: Inject BudgetRepository when created
    // private val budgetRepository: BudgetRepository
) : ViewModel() {

    // State for Budget List Screen
    private val _budgetListState = MutableStateFlow(BudgetListUiState())
    val budgetListState = _budgetListState.asStateFlow()

    // State for Add/Edit Budget Screen
    private val _addBudgetState = MutableStateFlow(AddBudgetUiState())
    val addBudgetState = _addBudgetState.asStateFlow()

    private val _eventChannel = Channel<BudgetEvent>()
    val events = _eventChannel.receiveAsFlow()

    init {
        loadBudgets()
        loadExpenseCategories() // Load categories on init
    }

    // ========== Load Expense Categories ==========

    private fun loadExpenseCategories() {
        viewModelScope.launch {
            getCategoriesUseCase().collect { categories ->
                // Filter only EXPENSE categories
                val expenseCategories = categories
                    .filter { it.type == CategoryType.EXPENSE }
                    .map { it.name }
                    .sorted() // Sort alphabetically

                _addBudgetState.value = _addBudgetState.value.copy(
                    categories = expenseCategories
                )
            }
        }
    }

    // ========== Budget List Screen Methods ==========

    private fun loadBudgets() {
        viewModelScope.launch {
            _budgetListState.value = _budgetListState.value.copy(isLoading = true)

            // TODO: Load from repository
            // For now, using dummy data
            val dummyBudgets = listOf(
                BudgetItem(
                    category = "Shopping",
                    spent = 250.0,
                    total = 1000.0,
                    icon = androidx.compose.material.icons.Icons.Default.ShoppingBag,
                    color = androidx.compose.ui.graphics.Color(0xFF6366F1)
                ),
                BudgetItem(
                    category = "Food & Dining",
                    spent = 420.0,
                    total = 600.0,
                    icon = androidx.compose.material.icons.Icons.Default.Restaurant,
                    color = androidx.compose.ui.graphics.Color(0xFFF97316),
                    warning = "You're nearing your budget!"
                ),
                BudgetItem(
                    category = "Transportation",
                    spent = 255.0,
                    total = 200.0,
                    icon = androidx.compose.material.icons.Icons.Default.DirectionsBus,
                    color = androidx.compose.ui.graphics.Color(0xFFEF4444),
                    warning = "You've exceeded your budget!",
                    isOverBudget = true
                ),
                BudgetItem(
                    category = "Utilities",
                    spent = 125.0,
                    total = 300.0,
                    icon = androidx.compose.material.icons.Icons.Default.Receipt,
                    color = androidx.compose.ui.graphics.Color(0xFF0EA5E9)
                )
            )

            _budgetListState.value = _budgetListState.value.copy(
                budgets = dummyBudgets,
                isLoading = false
            )
        }
    }

    fun onBudgetListEvent(event: BudgetListUiEvent) {
        when (event) {
            is BudgetListUiEvent.OnDeleteBudget -> deleteBudget(event.budget)
            is BudgetListUiEvent.OnRefresh -> loadBudgets()
        }
    }

    private fun deleteBudget(budget: BudgetItem) {
        viewModelScope.launch {
            try {
                // TODO: Delete from repository
                // budgetRepository.deleteBudget(budget)

                loadBudgets() // Reload list
                _eventChannel.send(BudgetEvent.ShowSuccess("Budget deleted"))
            } catch (e: Exception) {
                _eventChannel.send(BudgetEvent.ShowError("Failed to delete budget"))
            }
        }
    }

    // ========== Add/Edit Budget Screen Methods ==========

    fun onAddBudgetEvent(event: AddBudgetUiEvent) {
        when (event) {
            is AddBudgetUiEvent.OnAmountChange -> {
                val filtered = event.amount.filter { it.isDigit() || it == '.' }
                _addBudgetState.value = _addBudgetState.value.copy(amount = filtered)
            }
            is AddBudgetUiEvent.OnCategoryChange -> {
                _addBudgetState.value = _addBudgetState.value.copy(selectedCategory = event.category)
            }
            is AddBudgetUiEvent.OnMonthChange -> {
                _addBudgetState.value = _addBudgetState.value.copy(month = event.month)
            }
            is AddBudgetUiEvent.OnSaveBudget -> saveBudget()
        }
    }

    private fun saveBudget() {
        val state = _addBudgetState.value
        val amountDouble = state.amount.toDoubleOrNull()

        // Validation
        if (state.selectedCategory == null) {
            viewModelScope.launch {
                _eventChannel.send(BudgetEvent.ShowError("Please select a category"))
            }
            return
        }
        if (amountDouble == null || amountDouble <= 0) {
            viewModelScope.launch {
                _eventChannel.send(BudgetEvent.ShowError("Please enter a valid amount"))
            }
            return
        }
        if (state.month.isEmpty()) {
            viewModelScope.launch {
                _eventChannel.send(BudgetEvent.ShowError("Please enter a month"))
            }
            return
        }

        _addBudgetState.value = _addBudgetState.value.copy(isLoading = true)

        viewModelScope.launch {
            try {
                // TODO: Save to repository
                // val budget = Budget(
                //     category = state.selectedCategory!!,
                //     amount = amountDouble,
                //     month = state.month
                // )
                // budgetRepository.insertBudget(budget)

                _addBudgetState.value = _addBudgetState.value.copy(isLoading = false)

                // Reset form
                resetAddBudgetForm()

                // Reload budgets list
                loadBudgets()

                _eventChannel.send(BudgetEvent.NavigateBack)
            } catch (e: Exception) {
                _addBudgetState.value = _addBudgetState.value.copy(isLoading = false)
                _eventChannel.send(BudgetEvent.ShowError("Failed to save budget"))
            }
        }
    }

    private fun resetAddBudgetForm() {
        _addBudgetState.value = AddBudgetUiState()
    }
}

private fun getCurrentMonth(): String {
    val dateFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    return dateFormat.format(Date())
}
