package com.fintrack.app.core.domain.model

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
    val transactionCost: Double? = null, // M-Pesa transaction fee (null for non-M-Pesa transactions)
    val isPlanned: Boolean = false, // Marks future-dated transactions as planned
    val updatedAt: Long = System.currentTimeMillis(), // For incremental sync
    val deletedAt: Long? = null // Soft delete for sync
) {
    /**
     * Whether this transaction has actually happened by [now] and should count towards
     * spending/income totals. A future-dated (planned) transaction hasn't happened yet.
     * Decided by date rather than [isPlanned] alone: the flag is only set at insert time, so a
     * planned item whose date has passed counts, and an edit that moved a date into the future
     * does not.
     */
    fun hasOccurred(now: Long): Boolean = date <= now
}

// Using an enum for transaction type is safer in the domain layer
enum class TransactionType {
    INCOME,
    EXPENSE
}