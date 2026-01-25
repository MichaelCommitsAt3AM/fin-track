package com.fintrack.app.core.data.repository

import android.content.Context
import com.fintrack.app.core.domain.model.Transaction
import com.fintrack.app.core.domain.model.TransactionType
import com.fintrack.app.core.domain.model.MpesaTransaction
import com.fintrack.app.core.data.local.model.MpesaCategoryMappingEntity
import com.fintrack.app.core.data.local.model.MerchantCategoryEntity
import com.fintrack.app.core.domain.repository.ExternalTransactionRepository
import com.fintrack.app.core.domain.repository.MpesaTransactionRepository
import com.fintrack.app.core.data.local.dao.MpesaCategoryMappingDao
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
 * This returns M-Pesa transactions from the local database,
 * which can be useful for viewing recent M-Pesa activity.
 * NOTE: These are separate from the main Transaction entities.
 */
class SmsTransactionRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mpesaRepository: MpesaTransactionRepository,
    private val mappingDao: MpesaCategoryMappingDao,
    private val merchantCategoryDao: com.fintrack.app.core.data.local.dao.MerchantCategoryDao
) : ExternalTransactionRepository {
    
    override fun observeNewTransactions(): Flow<List<Transaction>> {
        // combine M-Pesa transactions with Category Mappings (Receipt & Merchant levels)
        return kotlinx.coroutines.flow.combine(
            mpesaRepository.getRecentTransactions(limit = 50),
            mappingDao.getAllMappings(),
            merchantCategoryDao.getAllMappings()
        ) { mpesaTransactions, receiptMappings, merchantMappings ->
            mapMpesaTransactions(mpesaTransactions, receiptMappings, merchantMappings)
        }
    }

    override fun getAllTransactions(): Flow<List<Transaction>> {
        return kotlinx.coroutines.flow.combine(
            mpesaRepository.getAllTransactions(),
            mappingDao.getAllMappings(),
            merchantCategoryDao.getAllMappings()
        ) { mpesaTransactions, receiptMappings, merchantMappings ->
            mapMpesaTransactions(mpesaTransactions, receiptMappings, merchantMappings)
        }
    }

    private fun mapMpesaTransactions(
        mpesaTransactions: List<MpesaTransaction>,
        receiptMappings: List<MpesaCategoryMappingEntity>,
        merchantMappings: List<MerchantCategoryEntity>
    ): List<Transaction> {
        // Create quick lookup maps
        val receiptMap = receiptMappings.associateBy { it.mpesaReceiptNumber }
        val merchantMap = merchantMappings.associateBy { it.merchantName }

        return mpesaTransactions.map { mpesa ->
            // Determine category precedence:
            // 1. Specific Receipt Mapping (Manual override for specific txn)
            // 2. Merchant Mapping (General rule for this merchant)
            // 3. Smart Clues / Auto-detection

            val receiptCategory = receiptMap[mpesa.mpesaReceiptNumber]?.categoryName
            val merchantCategory = mpesa.merchantName?.let { merchantMap[it]?.categoryName }

            val finalCategory = receiptCategory
                ?: merchantCategory
                ?: determineMpesaCategory(mpesa.transactionType.name, mpesa.smartClues)

            Transaction(
                id = "mpesa_${mpesa.smsId}",
                userId = "local", // M-Pesa transactions are user-agnostic for now
                type = mpesa.type,
                amount = mpesa.amount,
                category = finalCategory,
                date = mpesa.timestamp,
                notes = buildMpesaNotes(mpesa),
                paymentMethod = "M-Pesa",
                tags = buildMpesaTags(mpesa, receiptCategory != null || merchantCategory != null),
                isPlanned = false,
                updatedAt = mpesa.createdAt,
                deletedAt = null
            )
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
                    "FOOD" -> "Food"
                    "UTILITIES" -> "Utilities"
                    "ENTERTAINMENT" -> "Entertainment"
                    "HEALTH" -> "Healthcare"
                    "SHOPPING" -> "Shopping"
                    "AIRTIME" -> "Airtime & Data"
                    "DATA" -> "Airtime & Data"
                    "EDUCATION" -> "Education"
                    "SAVINGS" -> "Savings"
                    "BILLS" -> "Bills & Utilities"
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
    private fun buildMpesaNotes(mpesa: MpesaTransaction): String {
        val parts = mutableListOf<String>()
        
        mpesa.merchantName?.let { parts.add(it) }
        // mpesa.mpesaReceiptNumber.let { parts.add("Receipt: $it") }
        
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
    private fun buildMpesaTags(
        mpesa: MpesaTransaction,
        isAutoCategorized: Boolean
    ): List<String> {
        val tags = mutableListOf("M-Pesa", "Auto-imported", mpesa.transactionType.name)
        if (isAutoCategorized) {
            tags.add("Auto-categorized")
        }
        return tags
    }
}
