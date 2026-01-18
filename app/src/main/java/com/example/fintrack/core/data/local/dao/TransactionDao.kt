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
        AND deletedAt IS NULL
        AND (isPlanned = 0 OR date <= :currentTime)
        ORDER BY date DESC
        LIMIT :limit
    """)
    fun getAllTransactionsPaged(userId: String, limit: Int, currentTime: Long = System.currentTimeMillis()): Flow<List<TransactionEntity>>
    
    // Get all transactions within a specific date range
    @Query("SELECT * FROM transactions WHERE userId = :userId AND deletedAt IS NULL AND date BETWEEN :startDate AND :endDate")
    fun getTransactionsByDateRange(userId: String, startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    // Get transactions by type (e.g., "INCOME" or "EXPENSE"), excluding planned
    @Query("""
        SELECT * FROM transactions 
        WHERE userId = :userId 
        AND deletedAt IS NULL
        AND type = :type 
        AND (isPlanned = 0 OR date <= :currentTime)
        ORDER BY date DESC
        LIMIT :limit
    """)
    fun getTransactionsByTypePaged(userId: String, type: String, limit: Int, currentTime: Long = System.currentTimeMillis()): Flow<List<TransactionEntity>>

    // Search transactions by notes or category
    @Query("""
        SELECT * FROM transactions 
        WHERE userId = :userId 
        AND deletedAt IS NULL
        AND (notes LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%')
        AND (isPlanned = 0 OR date <= :currentTime)
        ORDER BY date DESC
    """)
    fun searchTransactions(userId: String, query: String, currentTime: Long = System.currentTimeMillis()): Flow<List<TransactionEntity>>

    // Get recent transactions, excluding planned
    @Query("""
        SELECT * FROM transactions 
        WHERE userId = :userId 
        AND deletedAt IS NULL
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
        AND deletedAt IS NULL
        AND isPlanned = 1 
        AND date > :currentTime
        ORDER BY date ASC
    """)
    fun getPlannedTransactions(userId: String, currentTime: Long = System.currentTimeMillis()): Flow<List<TransactionEntity>>

    // Get a single transaction by ID
    @Query("SELECT * FROM transactions WHERE id = :transactionId AND userId = :userId AND deletedAt IS NULL LIMIT 1")
    fun getTransactionById(transactionId: String, userId: String): Flow<TransactionEntity?>

    // Soft delete a transaction (also marks as unsynced for offline support)
    @Query("UPDATE transactions SET deletedAt = :deletedAt, isSynced = 0 WHERE id = :transactionId")
    suspend fun softDelete(transactionId: String, deletedAt: Long)

    // Mark a transaction as synced
    @Query("UPDATE transactions SET isSynced = 1 WHERE id = :transactionId")
    suspend fun markAsSynced(transactionId: String)
}