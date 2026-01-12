package com.example.fintrack.presentation.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fintrack.core.domain.model.Budget
import com.example.fintrack.core.domain.repository.BudgetRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val userId: String?
        get() = firebaseAuth.currentUser?.uid

    // Current month and year
    private val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1 // 1-12
    private val currentYear = Calendar.getInstance().get(Calendar.YEAR)

    // State for budgets
    private val _budgets = MutableStateFlow<List<Budget>>(emptyList())
    val budgets: StateFlow<List<Budget>> = _budgets.asStateFlow()

    // Loading and error states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadBudgets()
    }

    fun loadBudgets(month: Int = currentMonth, year: Int = currentYear) {
        viewModelScope.launch {
            _isLoading.value = true
            budgetRepository.getAllBudgetsForMonth(month, year)
                .catch { e ->
                    _error.value = e.message
                    _isLoading.value = false
                }
                .collect { budgetsList ->
                    _budgets.value = budgetsList
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

    fun getCurrentMonth(): Int = currentMonth
    fun getCurrentYear(): Int = currentYear

    fun clearError() {
        _error.value = null
    }
}
