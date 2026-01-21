package com.example.fintrack.core.data.repository

import android.content.Context
import com.example.fintrack.core.domain.model.Transaction
import com.example.fintrack.core.domain.model.TransactionType
import com.example.fintrack.core.domain.repository.ExternalTransactionRepository
import com.example.fintrack.core.domain.repository.MpesaTransactionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Personal flavor implementation of ExternalTransactionRepository.
 * 
 * This implementation uses the MpesaTransactionRepository to provide
 * M-Pesa transactions as external transaction data.
 * 
 * NOTE: This returns M-Pesa transactions from the local database,
 * which can be useful for viewing recent M-Pesa activity.
 * These are separate from the main Transaction entities.
 */
class SmsTransactionRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mpesaRepository: MpesaTransactionRepository
) : ExternalTransactionRepository {
    
    override fun observeNewTransactions(): Flow<List<Transaction>> {
        // Return recent M-Pesa transactions mapped to Transaction domain model
        // This allows the app to display M-Pesa data if needed
        return mpesaRepository.getRecentTransactions(limit = 50).map { mpesaTransactions ->
            mpesaTransactions.map { mpesa ->
                Transaction(
                    id = "mpesa_${mpesa.smsId}",
                    userId = "local", // M-Pesa transactions are user-agnostic for now
                    type = mpesa.type,
                    amount = mpesa.amount,
                    category = determineMpesaCategory(mpesa.transactionType.name, mpesa.smartClues),
                    date = mpesa.timestamp,
                    notes = buildMpesaNotes(mpesa),
                    paymentMethod = "M-Pesa",
                    tags = buildMpesaTags(mpesa),
                    isPlanned = false,
                    updatedAt = mpesa.createdAt,
                    deletedAt = null
                )
            }
        }
    }
    
    /**
     * Determine category based on M-Pesa transaction type and smart clues.
     */
    private fun determineMpesaCategory(
        transactionType: String,
        smartClues: List<String>
    ): String {
        // Check smart clues first for better categorization
        if (smartClues.isNotEmpty()) {
            val categoryFromClues = smartClues.firstOrNull()?.split(":")?.firstOrNull()
            if (categoryFromClues != null) {
                return when (categoryFromClues.uppercase()) {
                    "TRANSPORT" -> "Transport"
                    "FOOD" -> "Food & Dining"
                    "UTILITIES" -> "Utilities"
                    "ENTERTAINMENT" -> "Entertainment"
                    "HEALTH" -> "Healthcare"
                    "SHOPPING" -> "Shopping"
                    "AIRTIME" -> "Airtime & Data"
                    "EDUCATION" -> "Education"
                    else -> "Mobile Money"
                }
            }
        }
        
        // Fallback to transaction type
        return when (transactionType) {
            "AIRTIME" -> "Airtime & Data"
            "PAYBILL", "TILL" -> "Mobile Money"
            "SEND_MONEY", "RECEIVE_MONEY" -> "Transfers"
            "WITHDRAW", "DEPOSIT" -> "Cash"
            else -> "Mobile Money"
        }
    }
    
    /**
     * Build descriptive notes from M-Pesa transaction.
     */
    private fun buildMpesaNotes(mpesa: com.example.fintrack.core.domain.model.MpesaTransaction): String {
        val parts = mutableListOf<String>()
        
        mpesa.merchantName?.let { parts.add(it) }
        mpesa.mpesaReceiptNumber.let { parts.add("Receipt: $it") }
        
        when {
            mpesa.paybillNumber != null -> {
                parts.add("Paybill: ${mpesa.paybillNumber}")
                mpesa.accountNumber?.let { parts.add("A/C: $it") }
            }
            mpesa.tillNumber != null -> {
                parts.add("Till: ${mpesa.tillNumber}")
            }
            mpesa.phoneNumber != null -> {
                parts.add("Phone: ${mpesa.phoneNumber}")
            }
        }
        
        return parts.joinToString(" | ")
    }
    
    /**
     * Build tags from M-Pesa transaction.
     */
    private fun buildMpesaTags(mpesa: com.example.fintrack.core.domain.model.MpesaTransaction): List<String> {
        return listOf("M-Pesa", "Auto-imported", mpesa.transactionType.name)
    }
}
