package com.example.fintrack.domain.repository

import com.example.fintrack.domain.model.Budget
import kotlinx.coroutines.flow.Flow

// The "contract" for handling budget data
interface BudgetRepository {

    suspend fun insertBudget(budget: Budget)

    fun getBudget(categoryName: String, month: Int, year: Int): Flow<Budget?>

    fun getAllBudgetsForMonth(month: Int, year: Int): Flow<List<Budget>>
}