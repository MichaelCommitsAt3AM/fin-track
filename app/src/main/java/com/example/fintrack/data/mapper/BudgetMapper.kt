package com.example.fintrack.data.mapper

import com.example.fintrack.data.local.model.BudgetEntity
import com.example.fintrack.domain.model.Budget

// --- Budget Mappers ---

fun Budget.toEntity(): BudgetEntity {
    return BudgetEntity(
        categoryName = this.categoryName,
        amount = this.amount,
        month = this.month,
        year = this.year
    )
}

fun BudgetEntity.toDomain(): Budget {
    return Budget(
        categoryName = this.categoryName,
        amount = this.amount,
        month = this.month,
        year = this.year
    )
}