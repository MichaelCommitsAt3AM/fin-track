package com.fintrack.app.core.data.mapper

import com.fintrack.app.core.data.local.model.BudgetEntity
import com.fintrack.app.core.domain.model.Budget

// --- Budget Mappers ---

fun Budget.toEntity(): BudgetEntity {
    return BudgetEntity(
        categoryName = this.categoryName,
        userId = this.userId,
        amount = this.amount,
        month = this.month,
        year = this.year
    )
}

fun BudgetEntity.toDomain(): Budget {
    return Budget(
        categoryName = this.categoryName,
        userId = this.userId,
        amount = this.amount,
        month = this.month,
        year = this.year
    )
}