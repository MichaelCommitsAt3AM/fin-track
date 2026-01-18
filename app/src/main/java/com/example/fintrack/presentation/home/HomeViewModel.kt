package com.example.fintrack.presentation.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fintrack.core.data.local.LocalAuthManager
import com.example.fintrack.core.domain.model.CategoryType
import com.example.fintrack.core.domain.model.Currency
import com.example.fintrack.core.domain.model.TransactionType
import com.example.fintrack.core.domain.repository.CategoryRepository
import com.example.fintrack.core.domain.repository.NetworkRepository
import com.example.fintrack.core.domain.repository.TransactionRepository
import com.example.fintrack.presentation.settings.getIconByName
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

// Sync status for UI feedback
sealed class SyncStatus {
    object Idle : SyncStatus()
    object Syncing : SyncStatus()
    object Success : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val networkRepository: NetworkRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val localAuthManager: LocalAuthManager
) : ViewModel() {

    // --- Sync Status ---
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    // --- Network Connectivity State ---
    val isOnline: StateFlow<Boolean> = networkRepository.observeNetworkConnectivity()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val currencyPreference = localAuthManager.currencyPreference
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Currency.KSH
        )

    // --- Real-time User Data (Firestore Listener) ---
    // This flow updates immediately when the user changes their profile in Settings
    val currentUser: StateFlow<UserUiModel?> = callbackFlow {
        val user = auth.currentUser
        if (user != null) {
            // Attach a real-time listener to the specific user document
            val listener = firestore.collection("users").document(user.uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        // In case of error, we can log it or just close the flow
                        close(error)
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        // 1. Get real-time data from Firestore
                        val avatarId = snapshot.getLong("avatarId")?.toInt() ?: 1
                        val fullName = snapshot.getString("fullName") ?: user.displayName ?: "User"
                        val email = user.email ?: ""

                        // 2. Emit the updated User model
                        trySend(
                            UserUiModel(
                                fullName = fullName,
                                email = email,
                                avatarId = avatarId
                            )
                        )
                    } else {
                        // Fallback if document doesn't exist yet (use Auth defaults)
                        trySend(
                            UserUiModel(
                                fullName = user.displayName ?: "User",
                                email = user.email ?: "",
                                avatarId = 1
                            )
                        )
                    }
                }

            // Clean up the listener when the ViewModel/Flow is cleared
            awaitClose { listener.remove() }
        } else {
            trySend(null)
            close()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // --- Recent Transactions ---
    val recentTransactions: StateFlow<List<TransactionUiModel>> = combine(
        transactionRepository.getRecentTransactions(limit = 3),
        categoryRepository.getAllCategories()
    ) { transactions, categories ->
        transactions.map { transaction ->
            val category = categories.find { it.name == transaction.category }

            val icon = category?.iconName?.let { getIconByName(it) } ?: Icons.Default.Category
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

    // --- Spending Categories (Current Month) ---
    val spendingCategories: StateFlow<List<SpendingCategoryUiModel>> = combine(
        transactionRepository.getAllTransactionsPaged(Int.MAX_VALUE),
        categoryRepository.getAllCategories(),
        currencyPreference
    ) { transactions, categories, currency ->
        val calendar = Calendar.getInstance()

        // Calculate Month Start
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val monthStart = calendar.timeInMillis

        // Calculate Month End
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val monthEnd = calendar.timeInMillis

        val monthlyExpenses = transactions.filter {
            it.type == TransactionType.EXPENSE && it.date >= monthStart && it.date <= monthEnd
        }

        val categoryTotals = monthlyExpenses
            .groupBy { it.category }
            .mapValues { (_, transactions) -> transactions.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
            .take(4)

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
                    amount = "${currency.symbol} ${String.format("%.2f", totalAmount)}",
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

    // --- Weekly Spending ---
    val weeklySpending: StateFlow<Pair<Double, Double>> = transactionRepository.getAllTransactionsPaged(Int.MAX_VALUE)
        .map { transactions ->
            val calendar = Calendar.getInstance()

            // Current Week Range
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val weekStart = calendar.timeInMillis

            calendar.add(Calendar.DAY_OF_WEEK, 6)
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val weekEnd = calendar.timeInMillis

            // Last Week Range
            calendar.timeInMillis = weekStart
            calendar.add(Calendar.DAY_OF_YEAR, -7)
            val lastWeekStart = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, 6)
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            val lastWeekEnd = calendar.timeInMillis

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

    // Manual sync trigger for pull-to-refresh
    fun triggerManualSync() {
        if (_syncStatus.value is SyncStatus.Syncing) return

        viewModelScope.launch {
            // Double-check: Use both flow state AND immediate check
            // The flow might not have updated yet if network just dropped
            if (!isOnline.value || !networkRepository.isNetworkAvailable()) {
                _syncStatus.value = SyncStatus.Error("No internet connection")
                delay(2000)
                _syncStatus.value = SyncStatus.Idle
                return@launch
            }
            
            _syncStatus.value = SyncStatus.Syncing
            try {
                // Optional: Add a minimum delay here if the network is too fast
                // so the spinner doesn't just "flicker" for 10ms.
                val syncJob = launch {
                    transactionRepository.syncTransactionsFromCloud()
                    transactionRepository.syncUnsyncedTransactions()
                }
                // Ensure the spinner shows for at least 500ms even if net is instant
                delay(500)
                syncJob.join() // Wait for actual sync to finish

                _syncStatus.value = SyncStatus.Success

                // REDUCE THIS: 2000ms -> 800ms
                // Long enough to read "Success", short enough to not be annoying
                delay(800)

                _syncStatus.value = SyncStatus.Idle
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.Error(e.message ?: "Sync failed")

                // REDUCE THIS: 3000ms -> 2000ms
                // Errors need slightly more time to read, but 3s is long.
                delay(2000)

                _syncStatus.value = SyncStatus.Idle
            }
        }
    }
}