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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

// Helper for combining 4 flows
private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

// Helper for combining 5 flows
private data class Tuple5<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)

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
    
    // Secondary filter for transaction source
    private val _selectedSourceFilter = MutableStateFlow("All") // "All", "Manual", "M-Pesa"
    val selectedSourceFilter: StateFlow<String> = _selectedSourceFilter
    
    // View More dropdown state
    private val _isSourceFilterExpanded = MutableStateFlow(false)
    val isSourceFilterExpanded: StateFlow<Boolean> = _isSourceFilterExpanded

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

    // Dynamic transaction flow based on Search, Filter, Source Filter, and Limit
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val transactionsFlow = combine(
        _searchQuery,
        _selectedFilter,
        _selectedSourceFilter,
        _currentLimit
    ) { query, filter, sourceFilter, limit ->
        Tuple4(query, filter, sourceFilter, limit)
    }.flatMapLatest { (query, filter, sourceFilter, limit) ->
        val baseFlow = if (query.isNotEmpty()) {
            transactionRepository.searchTransactions(query)
        } else {
            if (filter == "All") {
                transactionRepository.getAllTransactionsPaged(limit)
            } else {
                transactionRepository.getTransactionsByTypePaged(filter.uppercase(), limit)
            }
        }
        
        // Apply source filter on top of base flow
        baseFlow.map { transactions ->
            when (sourceFilter) {
                "Manual" -> transactions.filter { transaction ->
                    // Manual transactions: do NOT have M-Pesa or Auto-imported tags
                    !(transaction.tags?.any { it == "M-Pesa" || it == "Auto-imported" } ?: false)
                }
                "M-Pesa" -> transactions.filter { transaction ->
                    // M-Pesa transactions: have M-Pesa tag
                    transaction.tags?.contains("M-Pesa") ?: false
                }
                else -> transactions // "All" - no filtering
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
    
    fun onSourceFilterSelected(source: String) {
        _selectedSourceFilter.value = source
        _currentLimit.value = 20 // Reset pagination
    }
    
    fun toggleSourceFilterExpanded() {
        _isSourceFilterExpanded.value = !_isSourceFilterExpanded.value
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
