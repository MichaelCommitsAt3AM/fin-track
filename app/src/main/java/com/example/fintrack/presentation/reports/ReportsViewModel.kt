package com.example.fintrack.presentation.reports

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fintrack.core.domain.model.TransactionType
import com.example.fintrack.core.domain.repository.BudgetRepository
import com.example.fintrack.core.domain.repository.CategoryRepository
import com.example.fintrack.core.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _currentMonth = MutableStateFlow(YearMonth.now())

    val state: StateFlow<ReportsUiState> = combine(
        transactionRepository.getAllTransactions(),
        categoryRepository.getAllCategories(),
        _currentMonth
    ) { transactions, categories, currentMonth ->

        // 1. Prepare Pie Chart Data
        val currentMonthTransactions = transactions.filter {
            val date = Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate()
            YearMonth.from(date) == currentMonth && it.type == TransactionType.EXPENSE
        }

        val totalExpense = currentMonthTransactions.sumOf { it.amount }

        val breakdown = currentMonthTransactions
            .groupBy { it.category }
            .map { (categoryName, txList) ->
                val sum = txList.sumOf { it.amount }
                val categoryColor = categories.find { it.name == categoryName }?.colorHex?.toColor()
                    ?: Color.Gray

                CategoryReportData(
                    name = categoryName,
                    amount = sum,
                    percentage = if (totalExpense > 0) (sum / totalExpense).toFloat() else 0f,
                    color = categoryColor,
                    transactionCount = txList.size
                )
            }.sortedByDescending { it.percentage }

        // 2. Prepare Monthly Summary
        val income = transactions
            .filter {
                val date = Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate()
                YearMonth.from(date) == currentMonth && it.type == TransactionType.INCOME
            }
            .sumOf { it.amount }

        // 3. Prepare Bar Chart Data (Last 6 Months Trends)
        val sixMonthsAgo = currentMonth.minusMonths(5)

        // --- FIXED LOGIC START ---
        val trendData = transactions
            .filter {
                val date = Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate()
                val ym = YearMonth.from(date)
                !ym.isBefore(sixMonthsAgo) && !ym.isAfter(currentMonth)
            }
            .groupBy {
                YearMonth.from(Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate())
            }
            .entries
            .sortedBy { it.key } // Sort by YearMonth (Key) BEFORE mapping to strings
            .map { (ym, txs) ->
                MonthlyFinancials(
                    month = ym.month.name.take(3), // Now we can safely truncate to "Jan", "Feb"
                    year = ym.year,
                    income = txs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
                    expense = txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                )
            }
        // --- FIXED LOGIC END ---

        ReportsUiState(
            isLoading = false,
            selectedDate = currentMonth,
            totalIncome = income,
            totalExpense = totalExpense,
            categoryBreakdown = breakdown,
            monthlyTrends = trendData
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReportsUiState(isLoading = true)
    )

    fun previousMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        _currentMonth.value = _currentMonth.value.plusMonths(1)
    }
}

// --- Helper Data Classes & Extensions ---

data class ReportsUiState(
    val isLoading: Boolean = false,
    val selectedDate: YearMonth = YearMonth.now(),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val categoryBreakdown: List<CategoryReportData> = emptyList(),
    val monthlyTrends: List<MonthlyFinancials> = emptyList()
)

data class CategoryReportData(
    val name: String,
    val amount: Double,
    val percentage: Float,
    val color: Color,
    val transactionCount: Int
)

data class MonthlyFinancials(
    val month: String,
    val year: Int,
    val income: Double,
    val expense: Double
)

fun String.toColor(): Color {
    return try {
        Color(android.graphics.Color.parseColor(this))
    } catch (e: Exception) {
        Color.Gray
    }
}