package com.example.fintrack.core.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// Defines the "budgets" table
@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey
    val categoryName: String, // Links directly to a CategoryEntity's name
    val amount: Double, // The total budget amount for the month
    val month: Int, // e.g., 1 for January, 12 for December
    val year: Int
)