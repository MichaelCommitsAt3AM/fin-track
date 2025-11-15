package com.example.fintrack.core.domain.model

// Represents a single transaction in the app's business logic
data class Transaction(
    val id: Int,
    val type: TransactionType,
    val amount: Double,
    val category: String,
    val date: Long,
    val notes: String?,
    val paymentMethod: String?,
    val tags: List<String>?
)

// Using an enum for transaction type is safer in the domain layer
enum class TransactionType {
    INCOME,
    EXPENSE
}