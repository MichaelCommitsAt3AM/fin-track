package com.example.fintrack.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.fintrack.core.data.local.model.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity)

    // Get a specific budget for a user's category in a given month and year
    @Query("SELECT * FROM budgets WHERE userId = :userId AND categoryName = :categoryName AND month = :month AND year = :year AND deletedAt IS NULL")
    fun getBudget(userId: String, categoryName: String, month: Int, year: Int): Flow<BudgetEntity?>

    // Get all budgets for a user in a given month and year
    @Query("SELECT * FROM budgets WHERE userId = :userId AND month = :month AND year = :year AND deletedAt IS NULL")
    fun getAllBudgetsForMonth(userId: String, month: Int, year: Int): Flow<List<BudgetEntity>>

    // Hard Delete a specific budget
    @Query("DELETE FROM budgets WHERE userId = :userId AND categoryName = :categoryName AND month = :month AND year = :year")
    suspend fun deleteBudget(userId: String, categoryName: String, month: Int, year: Int)

    // Soft delete
    @Query("UPDATE budgets SET deletedAt = :deletedAt, isSynced = 0, updatedAt = :timestamp WHERE userId = :userId AND categoryName = :categoryName AND month = :month AND year = :year")
    suspend fun softDelete(userId: String, categoryName: String, month: Int, year: Int, deletedAt: Long, timestamp: Long = System.currentTimeMillis())

    // Delete all budgets for a user (useful for logout)
    @Query("DELETE FROM budgets WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    // Get unsynced budgets
    @Query("SELECT * FROM budgets WHERE userId = :userId AND isSynced = 0")
    suspend fun getUnsyncedBudgets(userId: String): List<BudgetEntity>

    // Mark as updated
    @Query("UPDATE budgets SET isSynced = 1 WHERE userId = :userId AND categoryName = :categoryName AND month = :month AND year = :year")
    suspend fun markAsSynced(userId: String, categoryName: String, month: Int, year: Int)
}
