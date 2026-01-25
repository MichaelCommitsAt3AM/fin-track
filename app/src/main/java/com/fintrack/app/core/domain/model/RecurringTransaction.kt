package com.fintrack.app.core.domain.model

data class RecurringTransaction(
    val id: Int = 0,
    val userId: String,
    val type: TransactionType,
    val amount: Double,
    val category: String,
    val startDate: Long,
    val frequency: RecurrenceFrequency,
    val notes: String?
)