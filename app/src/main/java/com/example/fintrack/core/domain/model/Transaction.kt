package com.example.fintrack.core.domain.model

// Represents a single transaction in the app's business logic
data class Transaction(
    val id: String= "",
    val userId: String,
    val type: TransactionType,
    val amount: Double,
    val category: String,
    val date: Long,
    val notes: String?,
    val paymentMethod: String?,
    val tags: List<String>?,
    val isPlanned: Boolean = false, // Marks future-dated transactions as planned
    val updatedAt: Long = System.currentTimeMillis(), // For incremental sync
    val deletedAt: Long? = null // Soft delete for sync
)

// Using an enum for transaction type is safer in the domain layer
enum class TransactionType {
    INCOME,
    EXPENSE
}