package com.example.fintrack.core.data.mapper

import com.example.fintrack.core.data.local.model.TransactionEntity
import com.example.fintrack.core.domain.model.Transaction
import com.example.fintrack.core.domain.model.TransactionType

// --- Transaction Mappers ---

fun Transaction.toEntity(): TransactionEntity {
    return TransactionEntity(
        id = this.id,
        userId = this.userId,
        type = this.type.name, // Convert Enum to String
        amount = this.amount,
        category = this.category,
        date = this.date,
        notes = this.notes,
        paymentMethod = this.paymentMethod,
        tags = this.tags
    )
}

fun TransactionEntity.toDomain(): Transaction {
    return Transaction(
        id = this.id,
        userId = this.userId,
        type = TransactionType.valueOf(this.type), // Convert String to Enum
        amount = this.amount,
        category = this.category,
        date = this.date,
        notes = this.notes,
        paymentMethod = this.paymentMethod,
        tags = this.tags
    )
}