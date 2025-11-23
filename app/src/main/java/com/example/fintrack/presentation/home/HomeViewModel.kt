package com.example.fintrack.presentation.home

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.material.icons.filled.Category
import com.example.fintrack.core.domain.model.TransactionType
import com.example.fintrack.core.domain.model.CategoryType
import com.example.fintrack.core.domain.repository.CategoryRepository
import com.example.fintrack.core.domain.repository.TransactionRepository
import com.example.fintrack.presentation.settings.getIconByName
import com.example.fintrack.presentation.ui.theme.FinTrackGreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    // Recent Transactions
    val recentTransactions: StateFlow<List<TransactionUiModel>> = combine(
        transactionRepository.getRecentTransactions(limit = 3),
        categoryRepository.getAllCategories()
    ) { transactions, categories ->
        transactions.map { transaction ->
            val category = categories.find { it.name == transaction.category }

            val icon = category?.iconName?.let { getIconByName(it) }
                ?: androidx.compose.material.icons.Icons.Default.Category

            val color = try {
                Color(android.graphics.Color.parseColor(category?.colorHex ?: "#CCCCCC"))
            } catch (e: Exception) {
                Color.Gray
            }

            val displayAmount = if (transaction.type == TransactionType.EXPENSE) {
                -transaction.amount
            } else {
                transaction.amount
            }

            TransactionUiModel(
                name = transaction.notes ?: transaction.category,
                category = transaction.category,
                date = formatDate(transaction.date),
                amount = displayAmount,
                icon = icon,
                color = color
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Spending Categories (Current Month)
    val spendingCategories: StateFlow<List<SpendingCategoryUiModel>> = combine(
        transactionRepository.getAllTransactions(),
        categoryRepository.getAllCategories()
    ) { transactions, categories ->
        // Get the start and end of current month
        val calendar = Calendar.getInstance()
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
        val monthEnd = calendar.timeInMillis

        // Filter expenses for current month
        val monthlyExpenses = transactions.filter { transaction ->
            transaction.type == TransactionType.EXPENSE &&
                    transaction.date >= monthStart &&
                    transaction.date <= monthEnd
        }

        // Group by category and sum amounts
        val categoryTotals = monthlyExpenses
            .groupBy { it.category }
            .mapValues { (_, transactions) -> transactions.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second } // Sort by amount descending
            .take(4) // Take top 4 categories

        // Map to UI models
        categoryTotals.mapNotNull { (categoryName, totalAmount) ->
            val category = categories.find { it.name == categoryName && it.type == CategoryType.EXPENSE }

            if (category != null) {
                val icon = getIconByName(category.iconName)
                val color = try {
                    Color(android.graphics.Color.parseColor(category.colorHex))
                } catch (e: Exception) {
                    Color.Gray
                }

                SpendingCategoryUiModel(
                    name = categoryName,
                    amount = "Ksh ${String.format("%.2f", totalAmount)}",
                    icon = icon,
                    color = color
                )
            } else {
                null
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Calculates weekly and last week's spending
    val weeklySpending: StateFlow<Pair<Double, Double>> = transactionRepository.getAllTransactions()
        .map { transactions ->
            val calendar = Calendar.getInstance()

            // This week's range
            calendar.timeInMillis = System.currentTimeMillis()
            // Set to start of week (typically Sunday - adjust if you want Monday)
            calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val weekStart = calendar.timeInMillis

            // End of this week (coming Saturday or Sunday)
            calendar.add(Calendar.DAY_OF_WEEK, 6)
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val weekEnd = calendar.timeInMillis

            // Last week's range
            calendar.timeInMillis = weekStart
            calendar.add(Calendar.DAY_OF_YEAR, -7)
            val lastWeekStart = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, 6)
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val lastWeekEnd = calendar.timeInMillis

            // Compute totals
            val thisWeekTotal = transactions
                .filter { it.type == TransactionType.EXPENSE && it.date in weekStart..weekEnd }
                .sumOf { it.amount }

            val lastWeekTotal = transactions
                .filter { it.type == TransactionType.EXPENSE && it.date in lastWeekStart..lastWeekEnd }
                .sumOf { it.amount }

            Pair(thisWeekTotal, lastWeekTotal)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Pair(0.0, 0.0)
        )


    private fun formatDate(dateMillis: Long): String {
        val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
        return sdf.format(Date(dateMillis))
    }
}

// UI Models
data class TransactionUiModel(
    val name: String,
    val category: String,
    val date: String,
    val amount: Double,
    val icon: ImageVector,
    val color: Color
)

data class SpendingCategoryUiModel(
    val name: String,
    val amount: String,
    val icon: ImageVector,
    val color: Color
)
