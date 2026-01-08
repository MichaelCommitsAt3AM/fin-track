package com.example.fintrack.presentation.transactions

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fintrack.core.data.local.LocalAuthManager
import com.example.fintrack.core.domain.model.Category
import com.example.fintrack.core.domain.model.Currency
import com.example.fintrack.core.domain.model.Transaction
import com.example.fintrack.core.domain.model.TransactionType
import com.example.fintrack.core.domain.repository.CategoryRepository
import com.example.fintrack.core.domain.repository.TransactionRepository
import com.example.fintrack.presentation.settings.getIconByName
import com.example.fintrack.presentation.ui.theme.FinTrackGreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class TransactionListViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val localAuthManager: LocalAuthManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedFilter = MutableStateFlow("All") // "All", "Income", "Expense"
    val selectedFilter: StateFlow<String> = _selectedFilter

    val currencyPreference = localAuthManager.currencyPreference
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Currency.KSH
        )

    // We need categories to map icons/colors correctly
    private val categoriesFlow = categoryRepository.getAllCategories()

    // Combine transactions, categories, search, and filter into one UI state
    val uiState = combine(
        transactionRepository.getAllTransactions(),
        categoriesFlow,
        _searchQuery,
        _selectedFilter
    ) { transactions, categories, query, filter ->

        // 1. Filter by Type and Search Query
        val filteredList = transactions.filter { transaction ->
            val matchesSearch = transaction.notes?.contains(query, ignoreCase = true) == true ||
                    transaction.category.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                "Income" -> transaction.type == TransactionType.INCOME
                "Expense" -> transaction.type == TransactionType.EXPENSE
                else -> true // "All"
            }

            matchesSearch && matchesFilter
        }

        // 2. Map to UI Model (Add Icons/Colors)
        val uiList = filteredList.map { transaction ->
            val categoryInfo = categories.find { it.name == transaction.category }

            val icon = categoryInfo?.iconName?.let { getIconByName(it) }
                ?: com.example.fintrack.presentation.settings.getIconByName("wallet") // Fallback

            val colorHex = categoryInfo?.colorHex ?: "#CCCCCC"
            val color = try { Color(android.graphics.Color.parseColor(colorHex)) } catch(e:Exception) { Color.Gray }

            // Adjust amount sign for display (- for expense)
            val displayAmount = if (transaction.type == TransactionType.EXPENSE) -transaction.amount else transaction.amount

            TransactionItemData(
                title = transaction.notes ?: transaction.category,
                category = transaction.category,
                amount = displayAmount,
                icon = icon,
                color = color,
                dateMillis = transaction.date // Pass raw date for grouping
            )
        }

        // 3. Group by Date
        uiList.groupBy { formatDate(it.dateMillis) }

    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onFilterSelected(filter: String) {
        _selectedFilter.value = filter
    }

    private fun formatDate(millis: Long): String {
        val formatter = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
        val todayFormatter = SimpleDateFormat("yyyyMMdd", Locale.getDefault())

        val date = Date(millis)
        val today = Date()

        // Simple Today/Yesterday check
        return when (todayFormatter.format(date)) {
            todayFormatter.format(today) -> "Today"
            todayFormatter.format(Date(today.time - 86400000)) -> "Yesterday"
            else -> formatter.format(date)
        }
    }
}

// Updated Data Class to include dateMillis for grouping logic
data class TransactionItemData(
    val title: String,
    val category: String,
    val amount: Double,
    val icon: ImageVector,
    val color: Color,
    val dateMillis: Long
)
