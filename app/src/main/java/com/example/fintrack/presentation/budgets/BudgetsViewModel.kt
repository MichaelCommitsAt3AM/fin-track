package com.example.fintrack.presentation.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import com.example.fintrack.core.domain.model.Budget
import com.example.fintrack.core.domain.model.CategoryType
import com.example.fintrack.core.domain.model.TransactionType
import com.example.fintrack.core.domain.repository.BudgetRepository
import com.example.fintrack.core.domain.repository.TransactionRepository
import com.example.fintrack.core.domain.use_case.GetCategoriesUseCase
import com.example.fintrack.presentation.settings.getIconByName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    val categories: List<String> = emptyList(),
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
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val budgetRepository: BudgetRepository, // Inject BudgetRepository
    private val transactionRepository: TransactionRepository
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
        loadExpenseCategories()
    }

    // ========== Load Expense Categories ==========

    private fun loadExpenseCategories() {
        viewModelScope.launch {
            getCategoriesUseCase().collect { categories ->
                // Filter only EXPENSE categories
                val expenseCategories = categories
                    .filter { it.type == CategoryType.EXPENSE }
                    .map { it.name }
                    .sorted()

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

            try {
                // Get current month and year
                val calendar = Calendar.getInstance()
                val currentMonth = calendar.get(Calendar.MONTH) + 1 // Calendar.MONTH is 0-based
                val currentYear = calendar.get(Calendar.YEAR)

                // Calculate start and end timestamps for the current month
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val monthStart = calendar.timeInMillis

                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val monthEnd = calendar.timeInMillis

                // Combine budgets, categories, and transactions flows
                combine(
                    budgetRepository.getAllBudgetsForMonth(currentMonth, currentYear),
                    getCategoriesUseCase(),
                    transactionRepository.getAllTransactions()
                ) { budgets, categories, transactions ->
                    budgets.map { budget ->
                        // Find the matching category
                        val category = categories.find { it.name == budget.categoryName }

                        // Get icon and color
                        val icon = category?.iconName?.let { getIconByName(it) }
                            ?: Icons.Default.Category
                        val color = try {
                            Color(android.graphics.Color.parseColor(category?.colorHex ?: "#6366F1"))
                        } catch (e: Exception) {
                            Color(0xFF6366F1)
                        }

                        // Calculate spent amount from transactions
                        val spent = transactions
                            .filter { transaction ->
                                transaction.category == budget.categoryName &&
                                        transaction.type == TransactionType.EXPENSE &&
                                        transaction.date >= monthStart &&
                                        transaction.date <= monthEnd
                            }
                            .sumOf { it.amount }

                        val remaining = budget.amount - spent
                        val progress = if (budget.amount > 0) (spent / budget.amount) else 0.0

                        BudgetItem(
                            category = budget.categoryName,
                            spent = spent,
                            total = budget.amount,
                            icon = icon,
                            color = color,
                            warning = when {
                                spent > budget.amount -> "You've exceeded your budget!"
                                progress >= 0.7 -> "You're nearing your budget!"
                                else -> null
                            },
                            isOverBudget = spent > budget.amount
                        )
                    }
                }.collect { budgetItems ->
                    _budgetListState.value = _budgetListState.value.copy(
                        budgets = budgetItems,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _budgetListState.value = _budgetListState.value.copy(
                    budgets = emptyList(),
                    isLoading = false
                )
                _eventChannel.send(BudgetEvent.ShowError("Failed to load budgets: ${e.message}"))
            }
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
                // TODO: Add delete method to repository
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

        // Parse month string (YYYY-MM) to month and year
        val monthYear = parseMonthString(state.month)
        if (monthYear == null) {
            viewModelScope.launch {
                _eventChannel.send(BudgetEvent.ShowError("Invalid month format. Use YYYY-MM"))
            }
            return
        }

        _addBudgetState.value = _addBudgetState.value.copy(isLoading = true)

        viewModelScope.launch {
            try {
                // Create budget object
                val budget = Budget(
                    categoryName = state.selectedCategory!!,
                    amount = amountDouble,
                    month = monthYear.first,
                    year = monthYear.second
                )

                // Save to repository
                budgetRepository.insertBudget(budget)

                _addBudgetState.value = _addBudgetState.value.copy(isLoading = false)

                // Reset form
                resetAddBudgetForm()

                // Reload budgets list
                loadBudgets()

                _eventChannel.send(BudgetEvent.NavigateBack)
            } catch (e: Exception) {
                _addBudgetState.value = _addBudgetState.value.copy(isLoading = false)
                _eventChannel.send(BudgetEvent.ShowError("Failed to save budget: ${e.message}"))
            }
        }
    }

    private fun resetAddBudgetForm() {
        _addBudgetState.value = AddBudgetUiState()
    }

    // Helper function to parse "YYYY-MM" to (month, year)
    private fun parseMonthString(monthString: String): Pair<Int, Int>? {
        return try {
            val parts = monthString.split("-")
            if (parts.size == 2) {
                val year = parts[0].toInt()
                val month = parts[1].toInt()
                if (month in 1..12) {
                    Pair(month, year)
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }
}

private fun getCurrentMonth(): String {
    val dateFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    return dateFormat.format(Date())
}
