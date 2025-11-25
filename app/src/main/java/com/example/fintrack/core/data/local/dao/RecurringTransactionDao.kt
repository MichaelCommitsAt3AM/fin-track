package com.example.fintrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.OnConflictStrategy
import com.example.fintrack.data.local.model.RecurringTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recurringTransaction: RecurringTransactionEntity)

    // Get all recurring transactions for a specific user
    @Query("SELECT * FROM recurring_transactions WHERE userId = :userId ORDER BY startDate DESC")
    fun getAllRecurringTransactions(userId: String): Flow<List<RecurringTransactionEntity>>

    // Delete a specific recurring transaction for a user
    @Query("DELETE FROM recurring_transactions WHERE userId = :userId AND category = :category")
    suspend fun delete(userId: String, category: String)

    // Delete by ID (more precise)
    @Query("DELETE FROM recurring_transactions WHERE id = :id")
    suspend fun deleteById(id: Int)

    // Delete all for a user (useful for logout)
    @Query("DELETE FROM recurring_transactions WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}
