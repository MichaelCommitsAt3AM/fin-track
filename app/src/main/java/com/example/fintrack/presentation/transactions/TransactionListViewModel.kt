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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
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

    private val _currentLimit = MutableStateFlow(20)
    val currentLimit: StateFlow<Int> = _currentLimit

    // We need categories to map icons/colors correctly
    private val categoriesFlow = categoryRepository.getAllCategories()

    // Dynamic transaction flow based on Search, Filter, and Limit
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val transactionsFlow = combine(
        _searchQuery,
        _selectedFilter,
        _currentLimit
    ) { query, filter, limit ->
        Triple(query, filter, limit)
    }.flatMapLatest { (query, filter, limit) ->
        if (query.isNotEmpty()) {
            transactionRepository.searchTransactions(query)
        } else {
            if (filter == "All") {
                transactionRepository.getAllTransactionsPaged(limit)
            } else {
                transactionRepository.getTransactionsByTypePaged(filter.uppercase(), limit)
            }
        }
    }

    // Planned transactions flow - always fetch planned transactions
    val plannedTransactions = transactionRepository.getPlannedTransactions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Planned transactions UI State
    val plannedUiState = combine(
        plannedTransactions,
        categoriesFlow
    ) { transactions, categories ->
        // Create category cache
        val categoryCache = categories.associateBy({ it.name }, { category ->
            val icon = category.iconName?.let { getIconByName(it) }
                ?: com.example.fintrack.presentation.settings.getIconByName("wallet")
            
            val color = try { 
                Color(android.graphics.Color.parseColor(category.colorHex ?: "#CCCCCC")) 
            } catch(e: Exception) { 
                Color.Gray 
            }
            
            CategoryUiCache(icon, color)
        })

        // Map to UI Model
        transactions.map { transaction ->
            val cachedCategory = categoryCache[transaction.category]
            
            val icon = cachedCategory?.icon ?: com.example.fintrack.presentation.settings.getIconByName("wallet")
            val color = cachedCategory?.color ?: Color.Gray

            val displayAmount = if (transaction.type == TransactionType.INCOME) transaction.amount else -kotlin.math.abs(transaction.amount)

            TransactionItemData(
                id = transaction.id,
                title = transaction.notes ?: transaction.category,
                category = transaction.category,
                amount = displayAmount,
                icon = icon,
                color = color,
                dateMillis = transaction.date
            )
        }
    }
    .flowOn(kotlinx.coroutines.Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Combine transactions, categories => UI State
    val uiState = combine(
        transactionsFlow,
        categoriesFlow,
        _searchQuery,
        _selectedFilter
    ) { transactions, categories, query, filter ->

        // Optimization 1: Create a generic map for O(1) lookups & Pre-calculate UI attributes
        // This avoids parsing colors and looking up icons for every single transaction
        val categoryCache = categories.associateBy({ it.name }, { category ->
            val icon = category.iconName?.let { getIconByName(it) }
                ?: com.example.fintrack.presentation.settings.getIconByName("wallet")
            
            val color = try { 
                Color(android.graphics.Color.parseColor(category.colorHex ?: "#CCCCCC")) 
            } catch(e: Exception) { 
                Color.Gray 
            }
            
            CategoryUiCache(icon, color)
        })

        // Optimization 2: Map to UI Model using the cache
        val uiList = transactions.map { transaction ->
            val cachedCategory = categoryCache[transaction.category]
            
            // Fallbacks if category not found
            val icon = cachedCategory?.icon ?: com.example.fintrack.presentation.settings.getIconByName("wallet")
            val color = cachedCategory?.color ?: Color.Gray

            // Adjust amount sign for display (- for expense)
            val displayAmount = if (transaction.type == TransactionType.INCOME) transaction.amount else -kotlin.math.abs(transaction.amount)

            TransactionItemData(
                id = transaction.id,
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

    }
    .flowOn(kotlinx.coroutines.Dispatchers.Default) // Optimization 3: Run on background thread
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    fun loadMore() {
        if (_searchQuery.value.isEmpty()) {
            _currentLimit.value += 20 // Load next 20 items
        }
    }
    
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        // Reset limit on search clear? Not strictly necessary as we switch flows, but good practice
        if (query.isEmpty() && _currentLimit.value > 20) {
           // _currentLimit.value = 20 // Optional: reset limit when clearing search
        }
    }

    fun onFilterSelected(filter: String) {
        _selectedFilter.value = filter
        _currentLimit.value = 20 // Reset limit when changing filters
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
    val id: String,
    val title: String,
    val category: String,
    val amount: Double,
    val icon: ImageVector,
    val color: Color,
    val dateMillis: Long
)

// Helper class for caching UI resources
data class CategoryUiCache(
    val icon: ImageVector,
    val color: Color
)
