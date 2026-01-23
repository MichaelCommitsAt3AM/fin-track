package com.example.fintrack.core.data.repository

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import com.example.fintrack.core.data.local.dao.MpesaTransactionDao
import com.example.fintrack.core.data.local.model.MpesaTransactionEntity
import com.example.fintrack.core.data.mapper.MpesaTransactionMapper
import com.example.fintrack.core.domain.model.LookbackPeriod
import com.example.fintrack.core.domain.model.MpesaTransaction
import com.example.fintrack.core.domain.repository.MpesaTransactionRepository
import com.example.fintrack.core.util.MpesaSmsParser
import com.example.fintrack.core.util.ParsedMpesaTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import javax.inject.Inject

/**
 * Implementation of MpesaTransactionRepository.
 * 
 * CRITICAL: This repository NEVER syncs to Firestore. All data is local-only.
 */
class MpesaTransactionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: MpesaTransactionDao,
    private val parser: MpesaSmsParser
) : MpesaTransactionRepository {
    
    private companion object {
        const val TAG = "MpesaTransactionRepo"
        const val CLUE_SEPARATOR = "||"
        
        /**
         * Parser version for schema evolution.
         * Increment when improving regex patterns to allow selective re-parsing.
         */
        const val PARSER_VERSION = 2
    }

    override suspend fun syncMpesaSms(lookbackPeriod: LookbackPeriod): List<MpesaTransaction> {
        Log.d(TAG, "Starting M-Pesa SMS sync with lookback period: ${lookbackPeriod.months} months")
        
        val startTimestamp = lookbackPeriod.getStartTimestamp()
        val smsUri: Uri = Telephony.Sms.CONTENT_URI
        
        try {
            val cursor: Cursor? = context.contentResolver.query(
                smsUri,
                arrayOf(
                    Telephony.Sms._ID,
                    Telephony.Sms.ADDRESS,
                    Telephony.Sms.BODY,
                    Telephony.Sms.DATE
                ),
                "${Telephony.Sms.DATE} >= ? AND (${Telephony.Sms.ADDRESS} LIKE ? OR ${Telephony.Sms.ADDRESS} LIKE ?)",
                arrayOf(startTimestamp.toString(), "%MPESA%", "%M-PESA%"),
                "${Telephony.Sms.DATE} DESC"
            )
            
            cursor?.use {
                val addressIndex = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIndex = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIndex = it.getColumnIndexOrThrow(Telephony.Sms.DATE)
                val idIndex = it.getColumnIndexOrThrow(Telephony.Sms._ID)
                
                var processedCount = 0
                var skippedCount = 0
                var updatedCount = 0
                
                while (it.moveToNext()) {
                    val smsId = it.getLong(idIndex)
                    val sender = it.getString(addressIndex)
                    val smsBody = it.getString(bodyIndex)
                    val smsDate = it.getLong(dateIndex)
                    
                    // Create unique SMS ID (for reference only)
                    val uniqueSmsId = generateSmsId(smsId.toString(), smsDate)
                    
                    // Parse first to get receipt number (real dedupe key)
                    val parsed = parser.parseSms(smsBody) ?: continue
                    
                    // Check if exists
                    val existing = dao.getTransactionByReceiptNumber(parsed.mpesaReceiptNumber)
                    
                    if (existing != null) {
                        // Check if we need to re-parse due to version update
                        if (existing.parserVersion < PARSER_VERSION) {
                            if (storeTransaction(parsed, smsBody, smsDate, uniqueSmsId)) {
                                updatedCount++
                            }
                        } else {
                            skippedCount++
                        }
                        continue
                    }
                    
                    // Store the parsed transaction
                    if (storeTransaction(parsed, smsBody, smsDate, uniqueSmsId)) {
                        processedCount++
                    }
                }
                
                Log.d(TAG, "M-Pesa sync completed: $processedCount new, $updatedCount updated, $skippedCount skipped")
            }
            
            // Return all transactions in the lookback period (both new and existing)
            val endTime = System.currentTimeMillis()
            // We use the dao directly or the flow helper. Since we are in suspend, we can just consume the flow.
            // Using a distinct query would be more efficient if we had a suspend getAll... but flow.first() is fine.
            return getTransactionsByDateRange(startTimestamp, endTime).first()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing M-Pesa SMS", e)
            throw e
        }
    }
    
    override suspend fun parseAndStoreSms(smsBody: String, smsDate: Long, smsId: String): Boolean {
        try {
            // Parse SMS first
            val parsed = parser.parseSms(smsBody) ?: run {
                Log.d(TAG, "Could not parse SMS: ${smsBody.take(50)}...")
                return false
            }
            
            // Check if already exists (dedupe by receipt number)
            if (dao.getTransactionByReceiptNumber(parsed.mpesaReceiptNumber) != null) {
                Log.d(TAG, "Transaction ${parsed.mpesaReceiptNumber} already exists, skipping")
                return false
            }
            
            // Store the transaction
            return storeTransaction(parsed, smsBody, smsDate, smsId)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing and storing SMS", e)
            return false
        }
    }
    
    /**
     * Store a parsed M-Pesa transaction.
     */
    private suspend fun storeTransaction(
        parsed: ParsedMpesaTransaction,
        smsBody: String,
        smsDate: Long,
        smsId: String
    ): Boolean {
        try {
            // Create entity with parser version
            val entity = MpesaTransactionEntity(
                mpesaReceiptNumber = parsed.mpesaReceiptNumber, // Primary key
                smsId = smsId, // Transport reference only
                amount = parsed.amount,
                type = parsed.type.name,
                merchantName = parsed.merchantName,
                phoneNumber = parsed.phoneNumber,
                paybillNumber = parsed.paybillNumber,
                tillNumber = parsed.tillNumber,
                accountNumber = parsed.accountNumber,
                transactionType = parsed.transactionType.name,
                rawBody = smsBody,
                smartClues = parsed.smartClues.joinToString(CLUE_SEPARATOR),
                parserVersion = PARSER_VERSION, // Track parser version
                timestamp = smsDate,
                createdAt = System.currentTimeMillis()
            )
            
            // Insert to database (LOCAL ONLY - NO FIRESTORE SYNC)
            dao.insertTransaction(entity)
            
            Log.d(TAG, "Stored M-Pesa transaction: ${parsed.mpesaReceiptNumber}, ${parsed.transactionType}, KSH ${parsed.amount}")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error storing transaction", e)
            return false
        }
    }
    
    override fun getAllTransactions(): Flow<List<MpesaTransaction>> {
        return dao.getAllTransactions().map { entities ->
            MpesaTransactionMapper.toDomainList(entities)
        }
    }
    
    override fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<MpesaTransaction>> {
        return dao.getTransactionsByDateRange(startDate, endDate).map { entities ->
            MpesaTransactionMapper.toDomainList(entities)
        }
    }
    
    override fun getRecentTransactions(limit: Int): Flow<List<MpesaTransaction>> {
        return dao.getRecentTransactions(limit).map { entities ->
            MpesaTransactionMapper.toDomainList(entities)
        }
    }
    
    override suspend fun deleteTransaction(receiptNumber: String) {
        dao.deleteTransaction(receiptNumber)
    }
    
    override suspend fun deleteAllTransactions() {
        dao.deleteAllTransactions()
    }
    
    override suspend fun getTransactionCount(): Int {
        return dao.getTransactionCount()
    }
    
    /**
     * Generate a unique SMS ID using hash of SMS thread ID and timestamp.
     */
    private fun generateSmsId(threadId: String, timestamp: Long): String {
        val input = "$threadId-$timestamp"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }.take(32)
    }
}
