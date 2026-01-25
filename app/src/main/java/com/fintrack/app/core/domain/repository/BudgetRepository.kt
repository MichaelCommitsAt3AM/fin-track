package com.fintrack.app.core.domain.repository

import com.fintrack.app.core.domain.model.Budget
import kotlinx.coroutines.flow.Flow

// The "contract" for handling budget data
interface BudgetRepository {

    suspend fun insertBudget(budget: Budget)

    fun getBudget(categoryName: String, month: Int, year: Int): Flow<Budget?>

    fun getAllBudgetsForMonth(month: Int, year: Int): Flow<List<Budget>>

    suspend fun deleteBudget(categoryName: String, month: Int, year: Int) // ADD THIS

    suspend fun syncBudgetsFromCloud()

    suspend fun syncUnsyncedBudgets()
}