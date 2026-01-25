package com.fintrack.app.core.domain.model

data class Debt(
    val id: String,
    val userId: String,
    val title: String,
    val originalAmount: Double,
    val currentBalance: Double,
    val minimumPayment: Double,
    val dueDate: Long,
    val interestRate: Double,
    val notes: String?,
    val iconName: String,
    val debtType: DebtType,
    val createdAt: Long
)

enum class DebtType {
    I_OWE,
    OWED_TO_ME
}
