package com.fintrack.app.presentation.reports

import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.app.core.di.AppFlavorIntegration
import com.fintrack.app.core.data.local.LocalAuthManager
import com.fintrack.app.core.domain.model.Currency
import com.fintrack.app.core.domain.model.TransactionType
import com.fintrack.app.core.domain.repository.BudgetRepository
import com.fintrack.app.core.domain.repository.CategoryRepository
import com.fintrack.app.core.domain.repository.TransactionRepository
import com.fintrack.app.core.domain.repository.ExternalTransactionRepository
import com.fintrack.app.core.domain.util.SpendingPeriods
import com.fintrack.app.core.utils.CsvExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val csvExporter: CsvExporter,
    private val localAuthManager: LocalAuthManager,
    private val externalTransactionRepository: ExternalTransactionRepository,
    private val flavorIntegration: AppFlavorIntegration
) : ViewModel() {

    private val _currentMonth = MutableStateFlow(YearMonth.now())

    // Spending Trends granularity. Weekly is the default because it's more actionable day-to-day.
    private val _trendPeriod = MutableStateFlow(TrendPeriod.WEEKLY)

    private val firstDayOfWeek = SpendingPeriods.localeFirstDayOfWeek()

    // SharedFlow for one-time events (like triggering the share sheet)
    private val _exportEvent = MutableSharedFlow<Uri?>()
    val exportEvent = _exportEvent.asSharedFlow()

    val currencyPreference = localAuthManager.currencyPreference
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Currency.KSH
        )

    val state: StateFlow<ReportsUiState> = combine(
        transactionRepository.getAllTransactionsPaged(Int.MAX_VALUE),
        if (flavorIntegration.supportsMpesa) externalTransactionRepository.getAllTransactions() else kotlinx.coroutines.flow.flowOf(emptyList()),
        categoryRepository.getAllCategories(),
        combine(_currentMonth, _trendPeriod) { month, period -> month to period },
        currencyPreference
    ) { localTransactions, externalTransactions, categories, (currentMonth, trendPeriod), currency ->

        val transactions = localTransactions + externalTransactions


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

        // 3. Spending Trends, ending at the selected month.
        // Empty periods are kept (as 0) so the x-axis never silently skips a week/month.
        val trend = buildTrend(transactions, currentMonth, trendPeriod)

        ReportsUiState(
            isLoading = false,
            selectedDate = currentMonth,
            totalIncome = income,
            totalExpense = totalExpense,
            categoryBreakdown = breakdown,
            trend = trend,
            trendPeriod = trendPeriod,
            isCurrentMonth = currentMonth == YearMonth.now(),
            currency = currency
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReportsUiState(isLoading = true)
    )

    private fun buildTrend(
        transactions: List<com.fintrack.app.core.domain.model.Transaction>,
        month: YearMonth,
        period: TrendPeriod
    ): List<TrendPoint> = when (period) {
        TrendPeriod.WEEKLY -> {
            // Anchor on the current week when viewing this month, otherwise on the month's last week
            val today = LocalDate.now()
            val anchorDay = if (month == YearMonth.from(today)) today else month.atEndOfMonth()
            val lastWeekStart = SpendingPeriods.weekStartOf(anchorDay, firstDayOfWeek)
            val labelFormat = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
            SpendingPeriods.weeklyExpenseTrend(transactions, lastWeekStart, WEEKS_IN_TREND, firstDayOfWeek)
                .map { TrendPoint(label = it.start.format(labelFormat), detail = "Week of ${it.start.format(labelFormat)}", amount = it.amount) }
        }
        TrendPeriod.MONTHLY -> {
            val shortFormat = DateTimeFormatter.ofPattern("MMM", Locale.getDefault())
            val longFormat = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
            SpendingPeriods.monthlyExpenseTrend(transactions, month, MONTHS_IN_TREND)
                .map { TrendPoint(label = it.start.format(shortFormat), detail = it.start.format(longFormat), amount = it.amount) }
        }
    }

    fun previousMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        // Future months have no data, so don't let the user page into them
        if (_currentMonth.value < YearMonth.now()) {
            _currentMonth.value = _currentMonth.value.plusMonths(1)
        }
    }

    fun setTrendPeriod(period: TrendPeriod) {
        _trendPeriod.value = period
    }

    private companion object {
        const val WEEKS_IN_TREND = 8
        const val MONTHS_IN_TREND = 6
    }

    fun exportData() {
        viewModelScope.launch {
            // Fetch all transactions to export
            val localTransactions = transactionRepository.getAllTransactionsPaged(Int.MAX_VALUE).first()
            val externalTransactions = if (flavorIntegration.supportsMpesa) externalTransactionRepository.getAllTransactions().first() else emptyList()
            val transactions = localTransactions + externalTransactions

            // Generate CSV (IO operation)
            val uri = withContext(Dispatchers.IO) {
                csvExporter.generateTransactionCsv(transactions)
            }

            // Emit the result to the UI
            _exportEvent.emit(uri)
        }
    }
}

// --- Helper Data Classes & Extensions ---

data class ReportsUiState(
    val isLoading: Boolean = false,
    val selectedDate: YearMonth = YearMonth.now(),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val categoryBreakdown: List<CategoryReportData> = emptyList(),
    val trend: List<TrendPoint> = emptyList(),
    val trendPeriod: TrendPeriod = TrendPeriod.WEEKLY,
    val isCurrentMonth: Boolean = true,
    val currency: Currency = Currency.KSH
)

data class CategoryReportData(
    val name: String,
    val amount: Double,
    val percentage: Float,
    val color: Color,
    val transactionCount: Int
)

enum class TrendPeriod(val label: String, val unit: String) {
    WEEKLY("Weeks", "week"),
    MONTHLY("Months", "month")
}

data class TrendPoint(
    val label: String,   // short x-axis label, e.g. "10 Nov" or "Nov"
    val detail: String,  // full label shown when the point is selected
    val amount: Double
)

fun String.toColor(): Color {
    return try {
        Color(android.graphics.Color.parseColor(this))
    } catch (e: Exception) {
        Color.Gray
    }
}
