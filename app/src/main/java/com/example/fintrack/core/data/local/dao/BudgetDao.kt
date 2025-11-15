package com.example.fintrack.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.fintrack.core.data.local.model.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    // Insert or update a budget
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity)

    // Get a specific budget for a category in a given month and year
    @Query("SELECT * FROM budgets WHERE categoryName = :categoryName AND month = :month AND year = :year")
    fun getBudget(categoryName: String, month: Int, year: Int): Flow<BudgetEntity?>

    // Get all budgets for a given month and year
    @Query("SELECT * FROM budgets WHERE month = :month AND year = :year")
    fun getAllBudgetsForMonth(month: Int, year: Int): Flow<List<BudgetEntity>>
}