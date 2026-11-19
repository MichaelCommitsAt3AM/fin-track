package com.fintrack.app.presentation.transactions

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.app.core.di.AppFlavorIntegration
import com.fintrack.app.core.data.local.LocalAuthManager
import com.fintrack.app.core.domain.model.Category
import com.fintrack.app.core.domain.model.Currency
import com.fintrack.app.core.domain.model.Transaction
import com.fintrack.app.core.domain.model.TransactionType
import com.fintrack.app.core.domain.repository.CategoryRepository
import com.fintrack.app.core.domain.repository.ExternalTransactionRepository
import com.fintrack.app.core.domain.repository.TransactionRepository
import com.fintrack.app.core.domain.util.SpendingPeriods
import com.fintrack.app.core.domain.util.WeeklySpendingSummary
import com.fintrack.app.presentation.settings.getIconByName
import com.fintrack.app.presentation.ui.theme.FinTrackGreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
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
    private val externalTransactionRepository: ExternalTransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val localAuthManager: LocalAuthManager,
    private val flavorIntegration: AppFlavorIntegration
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

    val isMpesaSupported = MutableStateFlow(flavorIntegration.supportsMpesa).asStateFlow()

    val currencyPreference = localAuthManager.currencyPreference
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Currency.KSH
        )

    private val _currentLimit = MutableStateFlow(TransactionListFilter.PAGE_SIZE)
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
        // Get manual transactions
        val manualFlow = if (query.isNotEmpty()) {
            transactionRepository.searchTransactions(query)
        } else {
            if (filter == "All") {
                transactionRepository.getAllTransactionsPaged(limit)
            } else {
                transactionRepository.getTransactionsByTypePaged(filter.uppercase(), limit)
            }
        }
        
        // Get M-Pesa transactions (converted to Transaction model)
        val mpesaFlow = if (flavorIntegration.supportsMpesa) {
            externalTransactionRepository.observeNewTransactions()
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
        
        // Combine manual + M-Pesa transactions
        combine(manualFlow, mpesaFlow) { manual, mpesa ->
            TransactionListFilter.apply(manual, mpesa, filter, sourceFilter, query, limit)
        }
    }

    // --- Weekly Spending Summary ---
    // 0 = current week, -1 = last week, etc. Never positive (no future weeks).
    private val _weekOffset = MutableStateFlow(0)
    val weekOffset: StateFlow<Int> = _weekOffset.asStateFlow()

    private val firstDayOfWeek = SpendingPeriods.localeFirstDayOfWeek()

    private fun weekStartFor(offset: Int): LocalDate =
        SpendingPeriods.weekStartOf(LocalDate.now(), firstDayOfWeek).plusWeeks(offset.toLong())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val weeklySummary: StateFlow<WeeklySpendingSummary> = _weekOffset
        .flatMapLatest { offset ->
            val zone = ZoneId.systemDefault()
            val weekStart = weekStartFor(offset)
            // Load the previous week as well so we can show the comparison
            val rangeStart = weekStart.minusWeeks(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val rangeEnd = weekStart.plusWeeks(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

            val localFlow = transactionRepository.getTransactionsByDateRange(rangeStart, rangeEnd)
            val externalFlow = if (flavorIntegration.supportsMpesa) {
                externalTransactionRepository.getAllTransactions()
            } else {
                kotlinx.coroutines.flow.flowOf(emptyList())
            }

            combine(localFlow, externalFlow) { local, external ->
                // Planned (future-dated) transactions haven't happened yet, so they don't count as spending
                val now = System.currentTimeMillis()
                val relevant = (local + external).filter { it.date in rangeStart..minOf(rangeEnd, now) }
                SpendingPeriods.weeklySummary(relevant, weekStart, LocalDate.now(), zone)
            }
        }
        .flowOn(kotlinx.coroutines.Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = WeeklySpendingSummary.empty(weekStartFor(0), LocalDate.now())
        )

    fun previousWeek() {
        _weekOffset.value -= 1
    }

    fun nextWeek() {
        if (_weekOffset.value < 0) _weekOffset.value += 1
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
                ?: com.fintrack.app.presentation.settings.getIconByName("wallet")
            
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
            
            val icon = cachedCategory?.icon ?: com.fintrack.app.presentation.settings.getIconByName("wallet")
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
                ?: com.fintrack.app.presentation.settings.getIconByName("wallet")
            
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
            val icon = cachedCategory?.icon ?: com.fintrack.app.presentation.settings.getIconByName("wallet")
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
            _currentLimit.value += TransactionListFilter.PAGE_SIZE
        }
    }
    
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onFilterSelected(filter: String) {
        _selectedFilter.value = filter
        _currentLimit.value = TransactionListFilter.PAGE_SIZE // Back to the first page
    }
    
    fun onSourceFilterSelected(source: String) {
        _selectedSourceFilter.value = source
        _currentLimit.value = TransactionListFilter.PAGE_SIZE // Back to the first page
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
