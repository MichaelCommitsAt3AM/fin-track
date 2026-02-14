package com.fintrack.app.presentation.goals.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import com.fintrack.app.core.data.local.LocalAuthManager
import com.fintrack.app.core.domain.model.Budget
import com.fintrack.app.core.domain.model.CategoryType
import com.fintrack.app.core.domain.model.Currency
import com.fintrack.app.core.domain.model.Transaction
import com.fintrack.app.core.domain.model.TransactionType
import com.fintrack.app.core.domain.repository.BudgetRepository
import com.fintrack.app.core.domain.repository.ExternalTransactionRepository
import com.fintrack.app.core.domain.repository.TransactionRepository
import com.fintrack.app.core.di.AppFlavorIntegration
import com.fintrack.app.core.domain.use_case.GetCategoriesUseCase
import com.fintrack.app.presentation.settings.getIconByName
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

// UI State for Budget List Screen
data class BudgetListUiState(
    val budgets: List<BudgetItem> = emptyList(),
    val isLoading: Boolean = false,
    val currency: Currency = Currency.KSH
)

// UI State for Add/Edit Budget Screen
data class AddBudgetUiState(
    val amount: String = "",
    val selectedCategory: String? = null,
    val categories: List<String> = emptyList(),
    val date: Long = System.currentTimeMillis(),
    val spentAmount: Double = 0.0,
    val isLoading: Boolean = false,
    val currency: Currency = Currency.KSH
)

// Events for Add/Edit Screen
sealed class AddBudgetUiEvent {
    data class OnAmountChange(val amount: String) : AddBudgetUiEvent()
    data class OnCategoryChange(val category: String) : AddBudgetUiEvent()
    data class OnDateChange(val date: Long) : AddBudgetUiEvent()
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

// Compatibility model for GoalsScreen (from old BudgetViewModel)
data class GoalBudgetUiModel(
    val budget: Budget,
    val spent: Double,
    val progress: Float
)

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val externalTransactionRepository: ExternalTransactionRepository,
    private val appFlavorIntegration: AppFlavorIntegration,
    private val localAuthManager: LocalAuthManager,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val userId: String?
        get() = firebaseAuth.currentUser?.uid

    // Current month and year
    private val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
    private val currentYear = Calendar.getInstance().get(Calendar.YEAR)

    // State for Budget List Screen (Budgets tab)
    private val _budgetListState = MutableStateFlow(BudgetListUiState())
    val budgetListState = _budgetListState.asStateFlow()

    // State for Add/Edit Budget Screen
    private val _addBudgetState = MutableStateFlow(AddBudgetUiState())
    val addBudgetState = _addBudgetState.asStateFlow()

    // State for GoalsScreen (from old BudgetViewModel)
    private val _budgets = MutableStateFlow<List<GoalBudgetUiModel>>(emptyList())
    val budgets: StateFlow<List<GoalBudgetUiModel>> = _budgets.asStateFlow()

    // Loading and error states (for GoalsScreen compatibility)
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Currency preference
    val currencyPreference = localAuthManager.currencyPreference
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            initialValue = Currency.KSH
        )

    // State for individual budget management (from old BudgetViewModel)
    private val _currentBudget = MutableStateFlow<GoalBudgetUiModel?>(null)
    val currentBudget: StateFlow<GoalBudgetUiModel?> = _currentBudget.asStateFlow()

    private val _budgetTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    val budgetTransactions: StateFlow<List<Transaction>> = _budgetTransactions.asStateFlow()

    private val _eventChannel = Channel<BudgetEvent>()
    val events = _eventChannel.receiveAsFlow()

    init {
        loadBudgetsForBudgetsScreen()
        loadBudgetsForGoalsScreen()
        loadExpenseCategories()
        observeCurrency()
    }

    private fun observeCurrency() {
        localAuthManager.currencyPreference.onEach { currency ->
            _budgetListState.value = _budgetListState.value.copy(currency = currency)
            _addBudgetState.value = _addBudgetState.value.copy(currency = currency)
        }.launchIn(viewModelScope)
    }

    private fun getAllTransactionsFlow(): Flow<List<Transaction>> {
        val internalFlow = transactionRepository.getAllTransactionsPaged(Int.MAX_VALUE)
        
        return if (appFlavorIntegration.supportsMpesa) {
            combine(
                internalFlow,
                externalTransactionRepository.getAllTransactions()
            ) { internal, external ->
                internal + external
            }
        } else {
            internalFlow
        }
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

    // ========== Budgets Screen Methods (from BudgetsViewModel) ==========

    private fun loadBudgetsForBudgetsScreen() {
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
                    getAllTransactionsFlow()
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
            is BudgetListUiEvent.OnDeleteBudget -> deleteBudgetFromList(event.budget)
            is BudgetListUiEvent.OnRefresh -> {
                loadBudgetsForBudgetsScreen()
                loadBudgetsForGoalsScreen()
            }
        }
    }

    private fun deleteBudgetFromList(budget: BudgetItem) {
        viewModelScope.launch {
            try {
                val calendar = Calendar.getInstance()
                val currentMonth = calendar.get(Calendar.MONTH) + 1
                val currentYear = calendar.get(Calendar.YEAR)

                budgetRepository.deleteBudget(budget.category, currentMonth, currentYear)

                loadBudgetsForBudgetsScreen()
                loadBudgetsForGoalsScreen()
                _eventChannel.send(BudgetEvent.ShowSuccess("Budget deleted"))
            } catch (e: Exception) {
                _eventChannel.send(BudgetEvent.ShowError("Failed to delete budget"))
            }
        }
    }

    // ========== Goals Screen Methods (from old BudgetViewModel) ==========

    fun loadBudgetsForGoalsScreen(month: Int = currentMonth, year: Int = currentYear) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                // Calculate month start/end for transaction filtering
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month - 1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfMonth = calendar.timeInMillis

                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                val endOfMonth = calendar.timeInMillis

                // Combine budgets and transactions
                combine(
                    budgetRepository.getAllBudgetsForMonth(month, year),
                    getAllTransactionsFlow()
                ) { budgetsList, transactionsList ->
                    budgetsList.map { budget ->
                        val spent = transactionsList
                            .filter { txn ->
                                txn.category == budget.categoryName &&
                                txn.type == TransactionType.EXPENSE &&
                                txn.date in startOfMonth..endOfMonth
                            }
                            .sumOf { it.amount }
                        
                        val progress = if (budget.amount > 0) (spent / budget.amount).toFloat() else 0f
                        
                        GoalBudgetUiModel(
                            budget = budget,
                            spent = spent,
                            progress = progress
                        )
                    }
                }
                .catch { e ->
                    _error.value = e.message
                    _isLoading.value = false
                }
                .collect { uiModels ->
                    _budgets.value = uiModels
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                 _error.value = e.message
                 _isLoading.value = false
            }
        }
    }

    fun addBudget(categoryName: String, amount: Double, month: Int = currentMonth, year: Int = currentYear) {
        userId?.let { uid ->
            viewModelScope.launch {
                try {
                    _isLoading.value = true
                    val budget = Budget(
                        categoryName = categoryName,
                        userId = uid,
                        amount = amount,
                        month = month,
                        year = year
                    )
                    budgetRepository.insertBudget(budget)
                    _isLoading.value = false
                    loadBudgetsForGoalsScreen(month, year)
                } catch (e: Exception) {
                    _error.value = e.message
                    _isLoading.value = false
                }
            }
        }
    }

    fun deleteBudget(categoryName: String, month: Int = currentMonth, year: Int = currentYear) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                budgetRepository.deleteBudget(categoryName, month, year)
                _isLoading.value = false
                loadBudgetsForGoalsScreen(month, year)
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun loadBudgetDetails(categoryName: String, month: Int = currentMonth, year: Int = currentYear) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                // Calculate month start/end for transaction filtering
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month - 1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfMonth = calendar.timeInMillis

                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                val endOfMonth = calendar.timeInMillis

                // Combine budget and transactions
                combine(
                    budgetRepository.getAllBudgetsForMonth(month, year),
                    getAllTransactionsFlow()
                ) { budgetsList, transactionsList ->
                    val budget = budgetsList.firstOrNull { it.categoryName == categoryName }
                    
                    if (budget != null) {
                        val categoryTransactions = transactionsList.filter { txn ->
                            txn.category == categoryName &&
                            txn.type == TransactionType.EXPENSE &&
                            txn.date in startOfMonth..endOfMonth
                        }
                        
                        val spent = categoryTransactions.sumOf { it.amount }
                        val progress = if (budget.amount > 0) (spent / budget.amount).toFloat() else 0f
                        
                        Pair(
                            GoalBudgetUiModel(
                                budget = budget,
                                spent = spent,
                                progress = progress
                            ),
                            categoryTransactions
                        )
                    } else {
                        null
                    }
                }
                .catch { e ->
                    _error.value = e.message
                    _isLoading.value = false
                }
                .collect { data ->
                    if (data != null) {
                        _currentBudget.value = data.first
                        _budgetTransactions.value = data.second
                    }
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun getCurrentMonth(): Int = currentMonth
    fun getCurrentYear(): Int = currentYear

    fun clearError() {
        _error.value = null
    }

    // ========== Add/Edit Budget Screen Methods ==========
    
    fun loadBudgetForEdit(categoryName: String, month: Int, year: Int) {
        viewModelScope.launch {
            _addBudgetState.value = _addBudgetState.value.copy(isLoading = true)
            try {
                // Get budgets for the month and find the specific one
                // We use first() to get the current state without observing indefinitely
                val budgets = budgetRepository.getAllBudgetsForMonth(month, year).first()
                val budget = budgets.find { it.categoryName == categoryName }

                if (budget != null) {
                    val amountString = if (budget.amount % 1.0 == 0.0) {
                        budget.amount.toInt().toString()
                    } else {
                        budget.amount.toString()
                    }

                    // Create calendar for the edit date
                    val calendar = Calendar.getInstance()
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month - 1)
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                    val dateMillis = calendar.timeInMillis

                    _addBudgetState.value = _addBudgetState.value.copy(
                        amount = amountString,
                        selectedCategory = budget.categoryName,
                        date = dateMillis,
                        isLoading = false
                    )
                    calculateSpentAmount()
                } else {
                    _eventChannel.send(BudgetEvent.ShowError("Budget not found"))
                    _addBudgetState.value = _addBudgetState.value.copy(isLoading = false)
                }
            } catch (e: Exception) {
                _eventChannel.send(BudgetEvent.ShowError("Failed to load budget: ${e.message}"))
                _addBudgetState.value = _addBudgetState.value.copy(isLoading = false)
            }
        }
    }

    fun onAddBudgetEvent(event: AddBudgetUiEvent) {
        when (event) {
            is AddBudgetUiEvent.OnAmountChange -> {
                val filtered = event.amount.filter { it.isDigit() || it == '.' }
                _addBudgetState.value = _addBudgetState.value.copy(amount = filtered)
            }
            is AddBudgetUiEvent.OnCategoryChange -> {
                _addBudgetState.value = _addBudgetState.value.copy(selectedCategory = event.category)
                calculateSpentAmount()
            }
            is AddBudgetUiEvent.OnDateChange -> {
                _addBudgetState.value = _addBudgetState.value.copy(date = event.date)
                calculateSpentAmount()
            }
            is AddBudgetUiEvent.OnSaveBudget -> saveBudget()
        }
    }

    private fun calculateSpentAmount() {
        val state = _addBudgetState.value
        val category = state.selectedCategory
        if (category == null) return

        viewModelScope.launch {
            try {
                // Determine month start/end from selected date
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = state.date
                
                // Reset to start of month
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfMonth = calendar.timeInMillis

                // Set to end of month
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val endOfMonth = calendar.timeInMillis

                // We can use transactionRepository directly. 
                // Since this is a simple sum, we can fetch all transactions (paged) and filter in memory 
                // OR add a specific usecase/repository method for efficiency.
                // For now, mirroring existing logic:
                getAllTransactionsFlow().collect { transactions ->
                     val spent = transactions
                        .filter { txn ->
                            txn.category == category &&
                            txn.type == TransactionType.EXPENSE &&
                            txn.date in startOfMonth..endOfMonth
                        }
                        .sumOf { it.amount }
                    
                    _addBudgetState.value = _addBudgetState.value.copy(spentAmount = spent)
                    // We only need the first emission for the calculation
                    return@collect 
                }
            } catch (e: Exception) {
                // Ignore error for visual indicator, or log it
            }
        }
    }

    private fun saveBudget() {
        val state = _addBudgetState.value
        val amountDouble = state.amount.toDoubleOrNull()
        val userId = firebaseAuth.currentUser?.uid

        // Validation
        if (userId == null) {
            viewModelScope.launch {
                _eventChannel.send(BudgetEvent.ShowError("User not logged in"))
            }
            return
        }
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

        _addBudgetState.value = _addBudgetState.value.copy(isLoading = true)

        viewModelScope.launch {
            try {
                // Extract month and year from selected date
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = state.date
                val month = calendar.get(Calendar.MONTH) + 1
                val year = calendar.get(Calendar.YEAR)

                // Create budget object
                val budget = Budget(
                    categoryName = state.selectedCategory!!,
                    userId = userId,
                    amount = amountDouble,
                    month = month,
                    year = year
                )

                // Save to repository
                budgetRepository.insertBudget(budget)

                _addBudgetState.value = _addBudgetState.value.copy(isLoading = false)

                // Reset form
                resetAddBudgetForm()

                // Reload budgets lists
                loadBudgetsForBudgetsScreen()
                loadBudgetsForGoalsScreen()

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

}


