package com.example.fintrack.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.fintrack.core.data.local.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    // Insert a new transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    // Update an existing transaction
    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    // Get all NON-PLANNED transactions (current and past only), ordered by date (newest first)
    // Flow allows the UI to automatically update when data changes
    @Query("""
        SELECT * FROM transactions 
        WHERE userId = :userId 
        AND (isPlanned = 0 OR date <= :currentTime)
        ORDER BY date DESC
    """)
    fun getAllTransactions(userId: String, currentTime: Long = System.currentTimeMillis()): Flow<List<TransactionEntity>>

    // Get all transactions within a specific date range
    @Query("SELECT * FROM transactions WHERE userId  = :userId AND date BETWEEN :startDate AND :endDate")
    fun getTransactionsByDateRange(userId: String, startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    // Get transactions by type (e.g., "INCOME" or "EXPENSE"), excluding planned
    @Query("""
        SELECT * FROM transactions 
        WHERE userId = :userId 
        AND type = :type 
        AND (isPlanned = 0 OR date <= :currentTime)
        ORDER BY date DESC
    """)
    fun getTransactionsByType(userId: String, type: String, currentTime: Long = System.currentTimeMillis()): Flow<List<TransactionEntity>>

    // Get recent transactions, excluding planned
    @Query("""
        SELECT * FROM transactions 
        WHERE userId = :userId 
        AND (isPlanned = 0 OR date <= :currentTime)
        ORDER BY date DESC 
        LIMIT :limit
    """)
    fun getRecentTransactions(userId: String, limit: Int, currentTime: Long = System.currentTimeMillis()): Flow<List<TransactionEntity>>

    // For batch insertion
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    // Delete all transactions for a user (useful for logout)
    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun delete(transactionId: String)
    
    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM transactions WHERE isSynced = 0 AND userId = :userId")
    fun getUnsyncedTransactions(userId: String): List<TransactionEntity>

    // Get only planned (future) transactions
    @Query("""
        SELECT * FROM transactions 
        WHERE userId = :userId 
        AND isPlanned = 1 
        AND date > :currentTime
        ORDER BY date ASC
    """)
    fun getPlannedTransactions(userId: String, currentTime: Long = System.currentTimeMillis()): Flow<List<TransactionEntity>>

    // Mark a transaction as synced
    @Query("UPDATE transactions SET isSynced = 1 WHERE id = :transactionId")
    suspend fun markAsSynced(transactionId: String)
}