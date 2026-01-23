package com.example.fintrack.core.analytics

import com.example.fintrack.core.data.local.dao.MpesaTransactionDao
import com.example.fintrack.core.domain.model.onboarding.MerchantFrequency
import com.example.fintrack.core.domain.model.onboarding.OnboardingInsights
import com.example.fintrack.core.domain.model.onboarding.RecurringPaybill
import com.example.fintrack.core.util.SmartClueDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Analytics engine for M-Pesa transactions.
 * Uses optimized SQL queries to generate insights for the onboarding flow.
 */
@Singleton
class MpesaAnalyticsEngine @Inject constructor(
    private val mpesaDao: MpesaTransactionDao,
    private val smartClueDetector: SmartClueDetector
) {

    /**
     * Generates a complete insights report based on current M-Pesa data.
     */
    suspend fun generateInsights(): OnboardingInsights = withContext(Dispatchers.IO) {
        val totalCount = mpesaDao.getTransactionCount()
        
        // 1. Get frequent merchants (Top 5)
        val frequentMerchantsRaw = mpesaDao.getFrequentMerchants(5)
        val frequentMerchants = frequentMerchantsRaw.map { result ->
            val clues = smartClueDetector.detectClues(result.merchantName, null)
            val suggestedCategory = smartClueDetector.suggestCategory(clues)
            
            val recentEntities = mpesaDao.getTransactionsByMerchantName(result.merchantName, 5)
            val recentTransactions = com.example.fintrack.core.data.mapper.MpesaTransactionMapper.toDomainList(recentEntities)

            MerchantFrequency(
                merchantName = result.merchantName,
                transactionCount = result.frequency,
                totalAmount = result.totalAmount,
                suggestedCategory = suggestedCategory,
                recentTransactions = recentTransactions
            )
        }
        
        // 2. Get recurring paybills
        // We filter for those with at least 2 transactions
        val recurringPaybillsRaw = mpesaDao.getRecurringPaybills()
        val recurringPaybills = recurringPaybillsRaw.map { result ->
            val nameForCategory = result.merchantName ?: "Paybill ${result.paybillNumber}"
            val clues = smartClueDetector.detectClues(nameForCategory, null)
            val suggestedCategory = smartClueDetector.suggestCategory(clues)
            
            RecurringPaybill(
                paybillNumber = result.paybillNumber,
                merchantName = result.merchantName,
                frequency = result.frequency,
                averageAmount = result.avgAmount,
                suggestedCategory = suggestedCategory
            )
        }
        
        OnboardingInsights(
            totalTransactions = totalCount,
            frequentMerchants = frequentMerchants,
            recurringPaybills = recurringPaybills,
            isLoading = false
        )
    }
}
