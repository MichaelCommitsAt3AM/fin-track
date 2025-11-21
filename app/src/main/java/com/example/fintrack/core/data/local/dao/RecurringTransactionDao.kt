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

    @Query("SELECT * FROM recurring_transactions ORDER BY startDate DESC")
    fun getAllRecurringTransactions(): Flow<List<RecurringTransactionEntity>>

    @Query("DELETE FROM recurring_transactions WHERE category = :category")
    suspend fun delete(category: String)

    @Query("DELETE FROM recurring_transactions")
    suspend fun deleteAll()
}