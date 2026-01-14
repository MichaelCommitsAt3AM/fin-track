package com.example.fintrack.core.data.mapper

import com.example.fintrack.core.domain.model.RecurrenceFrequency
import com.example.fintrack.core.domain.model.RecurringTransaction
import com.example.fintrack.core.domain.model.TransactionType
import com.example.fintrack.core.data.local.model.RecurringTransactionEntity

fun RecurringTransaction.toEntity(): RecurringTransactionEntity {
    return RecurringTransactionEntity(
        id = this.id,
        userId = this.userId,
        type = this.type.name,
        amount = this.amount,
        category = this.category,
        startDate = this.startDate,
        frequency = this.frequency.name,
        notes = this.notes
    )
}

fun RecurringTransactionEntity.toDomain(): RecurringTransaction {
    return RecurringTransaction(
        id = this.id,
        userId = this.userId, // ADD THIS
        type = TransactionType.valueOf(this.type),
        amount = this.amount,
        category = this.category,
        startDate = this.startDate,
        frequency = RecurrenceFrequency.valueOf(this.frequency),
        notes = this.notes
    )
}
