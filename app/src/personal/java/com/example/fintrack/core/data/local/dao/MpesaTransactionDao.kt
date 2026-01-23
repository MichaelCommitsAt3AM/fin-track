package com.example.fintrack.core.data.local.dao

import androidx.room.*
import com.example.fintrack.core.data.local.model.MerchantFrequencyResult
import com.example.fintrack.core.data.local.model.MpesaTransactionEntity
import com.example.fintrack.core.data.local.model.PaybillAnalysisResult
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for M-Pesa transactions.
 * All operations are local-only (no Firestore sync).
 */
@Dao
interface MpesaTransactionDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: MpesaTransactionEntity)
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransactions(transactions: List<MpesaTransactionEntity>)
    
    @Query("SELECT * FROM mpesa_transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<MpesaTransactionEntity>>
    
    @Query("""
        SELECT * FROM mpesa_transactions 
        WHERE timestamp >= :startDate AND timestamp <= :endDate 
        ORDER BY timestamp DESC
    """)
    fun getTransactionsByDateRange(
        startDate: Long, 
        endDate: Long
    ): Flow<List<MpesaTransactionEntity>>
    
    @Query("SELECT * FROM mpesa_transactions WHERE mpesaReceiptNumber = :receiptNumber LIMIT 1")
    suspend fun getTransactionByReceiptNumber(receiptNumber: String): MpesaTransactionEntity?
    
    @Query("DELETE FROM mpesa_transactions WHERE mpesaReceiptNumber = :receiptNumber")
    suspend fun deleteTransaction(receiptNumber: String)
    
    @Query("DELETE FROM mpesa_transactions")
    suspend fun deleteAllTransactions()
    
    @Query("SELECT * FROM mpesa_transactions ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int): Flow<List<MpesaTransactionEntity>>
    
    @Query("""
        SELECT * FROM mpesa_transactions 
        WHERE type = :type 
        ORDER BY timestamp DESC
    """)
    fun getTransactionsByType(type: String): Flow<List<MpesaTransactionEntity>>
    
    @Query("""
        SELECT * FROM mpesa_transactions 
        WHERE transactionType = :transactionType 
        ORDER BY timestamp DESC
    """)
    fun getTransactionsByTransactionType(transactionType: String): Flow<List<MpesaTransactionEntity>>
    
    @Query("SELECT COUNT(*) FROM mpesa_transactions")
    suspend fun getTransactionCount(): Int
    
    @Query("""
        SELECT SUM(amount) FROM mpesa_transactions 
        WHERE type = :type 
        AND timestamp >= :startDate 
        AND timestamp <= :endDate
    """)
    suspend fun getTotalByTypeAndDateRange(
        type: String,
        startDate: Long,
        endDate: Long
    ): Double?

    @Query("""
        SELECT merchantName, COUNT(*) as frequency, SUM(amount) as totalAmount
        FROM mpesa_transactions
        WHERE merchantName IS NOT NULL AND merchantName != ''
        GROUP BY merchantName
        ORDER BY frequency DESC
        LIMIT :limit
    """)
    suspend fun getFrequentMerchants(limit: Int): List<MerchantFrequencyResult>

    @Query("""
        SELECT paybillNumber, merchantName, COUNT(*) as frequency, AVG(amount) as avgAmount
        FROM mpesa_transactions
        WHERE paybillNumber IS NOT NULL AND paybillNumber != ''
        GROUP BY paybillNumber
        HAVING frequency > 1
        ORDER BY frequency DESC
    """)
    suspend fun getRecurringPaybills(): List<PaybillAnalysisResult>
    @Query("""
        SELECT * FROM mpesa_transactions 
        WHERE merchantName = :merchantName 
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    suspend fun getTransactionsByMerchantName(merchantName: String, limit: Int): List<MpesaTransactionEntity>
}
