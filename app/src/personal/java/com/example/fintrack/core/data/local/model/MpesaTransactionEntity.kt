package com.example.fintrack.core.data.local.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for M-Pesa SMS transactions.
 * 
 * PRIVACY NOTICE: This data is LOCAL-ONLY and NEVER synced to Firestore.
 * M-Pesa transactions remain on the device for privacy and security.
 * 
 * PRIMARY KEY: mpesaReceiptNumber
 * - Globally unique per Safaricom
 * - Survives device migrations and backups
 * - Never changes or gets reused
 * 
 * smsId is kept as a transport reference only (can change after restore).
 */
@Entity(
    tableName = "mpesa_transactions",
    indices = [
        Index(value = ["mpesaReceiptNumber"], unique = true)
    ]
)
data class MpesaTransactionEntity(
    @PrimaryKey
    val mpesaReceiptNumber: String, // M-Pesa transaction code (e.g., QK87ABCD12) - UNIQUE!
    
    val smsId: String, // Android SMS ID (transport reference only, not for deduplication)
    
    val amount: Double,
    
    val type: String, // "INCOME" or "EXPENSE"
    
    val merchantName: String?, // Cleaned merchant or person name
    
    val phoneNumber: String?, // Phone number for P2P transfers
    
    val paybillNumber: String?, // Paybill number if applicable
    
    val tillNumber: String?, // Till number if applicable
    
    val accountNumber: String?, // Account number for paybill transactions
    
    val transactionType: String, // SEND_MONEY, RECEIVE_MONEY, PAYBILL, TILL, AIRTIME, WITHDRAW
    
    val rawBody: String, // Full SMS text for future parsing improvements
    
    val smartClues: String?, // Pipe-separated list of detected keywords
    
    val parserVersion: Int, // Parser version used to extract this data (for re-parsing old records)
    
    val timestamp: Long, // SMS timestamp (transaction time)
    
    val createdAt: Long = System.currentTimeMillis() // When parsed and inserted
)
