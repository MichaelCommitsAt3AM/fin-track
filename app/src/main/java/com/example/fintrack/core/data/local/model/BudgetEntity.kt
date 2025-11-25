package com.example.fintrack.core.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "budgets",
    primaryKeys = ["categoryName", "userId", "month", "year"]
) // Composite key

data class BudgetEntity(
    val categoryName: String, // Links directly to a CategoryEntity's name
    val userId: String,
    val amount: Double, // The total budget amount for the month
    val month: Int, // e.g., 1 for January, 12 for December
    val year: Int
)