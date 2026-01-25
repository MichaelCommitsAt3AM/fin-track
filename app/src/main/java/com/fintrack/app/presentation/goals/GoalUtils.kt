package com.fintrack.app.presentation.goals

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.fintrack.app.core.domain.model.Debt
import com.fintrack.app.core.domain.model.Saving
import com.fintrack.app.core.domain.model.DebtType

// Helper function to map icon names to Material Icons
fun iconFromName(iconName: String): ImageVector {
    return when (iconName) {
        "Savings" -> Icons.Default.Savings
        "Computer" -> Icons.Default.Computer
        "BeachAccess" -> Icons.Default.BeachAccess
        "DirectionsCar" -> Icons.Default.DirectionsCar
        "Home" -> Icons.Default.Home
        "School" -> Icons.Default.School
        "HealthAndSafety" -> Icons.Default.HealthAndSafety
        "CardGiftcard" -> Icons.Default.CardGiftcard
        "Flight" -> Icons.Default.Flight
        "Phone" -> Icons.Default.Phone
        "Watch" -> Icons.Default.Watch
        "ShoppingBag" -> Icons.Default.ShoppingBag
        "CreditCard" -> Icons.Default.CreditCard
        else -> Icons.Default.Savings
    }
}

// Helper function to get category icon
fun getCategoryIcon(categoryName: String): ImageVector {
    return when (categoryName.lowercase()) {
        "shopping", "shopping & entertainment" -> Icons.Default.ShoppingBag
        "food & dining", "food", "dining", "restaurant" -> Icons.Default.Restaurant
        "groceries" -> Icons.Default.ShoppingCart
        "transportation", "transport", "car" -> Icons.Default.DirectionsCar
        "utilities", "bills" -> Icons.Default.Receipt
        "entertainment" -> Icons.Default.Movie
        "health", "healthcare" -> Icons.Default.HealthAndSafety
        "education" -> Icons.Default.School
        "home" -> Icons.Default.Home
        "personal care" -> Icons.Default.Face
        "travel" -> Icons.Default.Flight
        "gifts" -> Icons.Default.CardGiftcard
        "insurance" -> Icons.Default.Security
        "savings" -> Icons.Default.Savings
        else -> Icons.Default.AccountBalanceWallet
    }
}

// Mappers moved below


// UI Models
data class DebtGoal(
    val id: String,
    val title: String,
    val originalAmount: Double,
    val currentBalance: Double,
    val minimumPayment: Double,
    val dueDate: Long,
    val interestRate: Double,
    val icon: ImageVector,
    val color: Color,
    val debtType: DebtType,
    val payments: List<UiPayment>
)

data class UiPayment(
    val id: String,
    val amount: Double,
    val date: Long,
    val note: String = ""
)

data class SavingGoal(
    val id: String,
    val title: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val targetDate: Long,
    val icon: ImageVector,
    val color: Color,
    val contributions: List<UiContribution>
)

data class UiContribution(
    val id: String,
    val amount: Double,
    val date: Long,
    val note: String = ""
)

// Mappers
fun Debt.toUiModel(payments: List<com.fintrack.app.core.domain.model.Payment> = emptyList()): DebtGoal {
    return DebtGoal(
        id = this.id,
        title = this.title,
        originalAmount = this.originalAmount,
        currentBalance = this.currentBalance,
        minimumPayment = this.minimumPayment,
        dueDate = this.dueDate,
        interestRate = this.interestRate,
        icon = iconFromName(this.iconName),
        color = Color(0xFFE74C3C), // Default red for debt, or simplify logic based on type
        debtType = this.debtType,
        payments = payments.map { it.toUiModel() }
    )
}

fun com.fintrack.app.core.domain.model.Payment.toUiModel(): UiPayment {
    return UiPayment(
        id = this.id,
        amount = this.amount,
        date = this.date,
        note = this.note ?: ""
    )
}

fun Saving.toUiModel(contributions: List<com.fintrack.app.core.domain.model.Contribution> = emptyList()): SavingGoal {
    return SavingGoal(
        id = this.id,
        title = this.title,
        targetAmount = this.targetAmount,
        currentAmount = this.currentAmount,
        targetDate = this.targetDate,
        icon = iconFromName(this.iconName),
        color = Color(0xFF10B981), // Default green for savings
        contributions = contributions.map { it.toUiModel() }
    )
}

fun com.fintrack.app.core.domain.model.Contribution.toUiModel(): UiContribution {
    return UiContribution(
        id = this.id,
        amount = this.amount,
        date = this.date,
        note = this.note ?: ""
    )
}

// Helper function to get category color
fun getCategoryColor(categoryName: String): Color {
    return when (categoryName.lowercase()) {
        "shopping", "shopping & entertainment" -> Color(0xFF6366F1) // Indigo
        "food & dining", "food", "dining", "restaurant" -> Color(0xFFF97316) // Orange
        "groceries" -> Color(0xFF10B981) // Emerald
        // ... (rest of the existing function)
        "transportation", "transport", "car" -> Color(0xFF3B82F6) // Blue
        "utilities", "bills" -> Color(0xFFF59E0B) // Amber
        "entertainment" -> Color(0xFFA855F7) // Purple
        "health", "healthcare" -> Color(0xFFEF4444) // Red
        "education" -> Color(0xFF8B5CF6) // Violet
        "home" -> Color(0xFF14B8A6) // Teal
        "personal care" -> Color(0xFFEC4899) // Pink
        "travel" -> Color(0xFF06B6D4) // Cyan
        "gifts" -> Color(0xFFF43F5E) // Rose
        "insurance" -> Color(0xFF64748B) // Slate
        "savings" -> Color(0xFF22C55E) // Green
        else -> Color(0xFF6366F1) // Default Indigo
    }
}
