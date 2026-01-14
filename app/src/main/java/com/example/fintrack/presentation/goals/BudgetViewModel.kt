package com.example.fintrack.presentation.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fintrack.core.domain.model.Budget
import com.example.fintrack.core.domain.repository.BudgetRepository
import com.example.fintrack.core.domain.repository.TransactionRepository
import com.example.fintrack.core.data.local.LocalAuthManager
import com.example.fintrack.core.domain.model.Currency
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val firebaseAuth: FirebaseAuth,
    private val localAuthManager: LocalAuthManager
) : ViewModel() {

    private val userId: String?
        get() = firebaseAuth.currentUser?.uid

    // Current month and year
    private val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1 // 1-12
    private val currentYear = Calendar.getInstance().get(Calendar.YEAR)

    // State for budgets
    private val _budgets = MutableStateFlow<List<GoalBudgetUiModel>>(emptyList())
    val budgets: StateFlow<List<GoalBudgetUiModel>> = _budgets.asStateFlow()

    // Loading and error states
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

    // State for individual budget management
    private val _currentBudget = MutableStateFlow<GoalBudgetUiModel?>(null)
    val currentBudget: StateFlow<GoalBudgetUiModel?> = _currentBudget.asStateFlow()

    private val _budgetTransactions = MutableStateFlow<List<com.example.fintrack.core.domain.model.Transaction>>(emptyList())
    val budgetTransactions: StateFlow<List<com.example.fintrack.core.domain.model.Transaction>> = _budgetTransactions.asStateFlow()

    init {
        loadBudgets()
    }

    fun loadBudgets(month: Int = currentMonth, year: Int = currentYear) {
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
                    transactionRepository.getAllTransactions()
                ) { budgetsList, transactionsList ->
                    budgetsList.map { budget ->
                        val spent = transactionsList
                            .filter { txn ->
                                txn.category == budget.categoryName &&
                                txn.type == com.example.fintrack.core.domain.model.TransactionType.EXPENSE &&
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
                    transactionRepository.getAllTransactions()
                ) { budgetsList, transactionsList ->
                    val budget = budgetsList.firstOrNull { it.categoryName == categoryName }
                    
                    if (budget != null) {
                        val categoryTransactions = transactionsList.filter { txn ->
                            txn.category == categoryName &&
                            txn.type == com.example.fintrack.core.domain.model.TransactionType.EXPENSE &&
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
}

data class GoalBudgetUiModel(
    val budget: Budget,
    val spent: Double,
    val progress: Float
)
