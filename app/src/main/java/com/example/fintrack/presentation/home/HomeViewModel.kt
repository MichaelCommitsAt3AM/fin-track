package com.example.fintrack.presentation.home

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.material.icons.filled.Category
import com.example.fintrack.core.domain.model.TransactionType
import com.example.fintrack.core.domain.repository.CategoryRepository
import com.example.fintrack.core.domain.repository.TransactionRepository
import com.example.fintrack.presentation.settings.getIconByName
import com.example.fintrack.presentation.ui.theme.FinTrackGreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.tensorflow.lite.support.label.Category
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository
) : ViewModel() {

    // Combine transactions and categories to map icons/colors
    val recentTransactions: StateFlow<List<TransactionUiModel>> = combine(
        // Fetch only the last 3 transactions efficiently from Room
        transactionRepository.getRecentTransactions(limit = 3),
        categoryRepository.getAllCategories()
    ) { transactions, categories ->
        transactions.map { transaction ->
            // Find the category object associated with this transaction
            val category = categories.find { it.name == transaction.category }

            // Determine Icon
            val icon = category?.iconName?.let { getIconByName(it) }
                ?: androidx.compose.material.icons.Icons.Default.Category

            // Determine Color (Default to Gray if parsing fails or missing)
            val color = try {
                Color(android.graphics.Color.parseColor(category?.colorHex ?: "#CCCCCC"))
            } catch (e: Exception) {
                Color.Gray
            }

            // Handle Amount Sign (Negative for Expense, Positive for Income)
            val displayAmount = if (transaction.type == TransactionType.EXPENSE) {
                -transaction.amount
            } else {
                transaction.amount
            }

            TransactionUiModel(
                name = transaction.notes ?: transaction.category,
                category = transaction.category, // Added category name for UI if needed
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

    private fun formatDate(dateMillis: Long): String {
        val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
        return sdf.format(Date(dateMillis))
    }
}

// UI Model specifically for the Home Screen list
data class TransactionUiModel(
    val name: String,
    val category: String,
    val date: String,
    val amount: Double,
    val icon: ImageVector,
    val color: Color
)