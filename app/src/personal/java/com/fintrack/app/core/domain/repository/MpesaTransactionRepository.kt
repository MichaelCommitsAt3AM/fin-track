package com.fintrack.app.core.domain.repository

import com.fintrack.app.core.domain.model.LookbackPeriod
import com.fintrack.app.core.domain.model.MpesaTransaction
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for M-Pesa transactions.
 * 
 * IMPORTANT: All data is local-only and never synced to Firestore.
 */
interface MpesaTransactionRepository {
    
    /**
     * Scan SMS messages for M-Pesa transactions within the specified lookback period.
     * This is a potentially long-running operation and should be called from a Worker.
     * 
     * @param lookbackPeriod How far back to scan for M-Pesa SMS messages
     */
    suspend fun syncMpesaSms(lookbackPeriod: LookbackPeriod = LookbackPeriod.THREE_MONTHS): List<MpesaTransaction>
    
    /**
     * Parse and store a single M-Pesa SMS message.
     * 
     * @param smsBody The SMS message body
     * @param smsDate The SMS timestamp
     * @param smsId Unique identifier for the SMS
     * @return True if successfully parsed and stored, false otherwise
     */
    suspend fun parseAndStoreSms(smsBody: String, smsDate: Long, smsId: String): Boolean
    
    /**
     * Get all M-Pesa transactions.
     */
    fun getAllTransactions(): Flow<List<MpesaTransaction>>
    
    /**
     * Get M-Pesa transactions within a date range.
     */
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<MpesaTransaction>>
    
    /**
     * Get recent M-Pesa transactions.
     */
    fun getRecentTransactions(limit: Int = 10): Flow<List<MpesaTransaction>>
    
    /**
     * Delete a specific M-Pesa transaction by receipt number.
     */
    suspend fun deleteTransaction(receiptNumber: String)
    
    /**
     * Delete all M-Pesa transactions.
     */
    suspend fun deleteAllTransactions()
    
    /**
     * Get total count of M-Pesa transactions.
     */
    suspend fun getTransactionCount(): Int
}
