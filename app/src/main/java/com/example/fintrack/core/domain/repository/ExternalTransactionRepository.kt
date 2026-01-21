package com.example.fintrack.core.domain.repository

import com.example.fintrack.core.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for external transaction sources.
 * 
 * This interface is implemented differently for each build variant:
 * - Store variant: Returns empty flow (no external sources)
 * - Personal variant: Reads M-Pesa SMS messages to extract transactions
 */
interface ExternalTransactionRepository {
    
    /**
     * Observes new transactions from external sources.
     * 
     * @return Flow of transactions from external sources (e.g., SMS messages).
     *         Returns empty flow for store variant.
     */
    fun observeNewTransactions(): Flow<List<Transaction>>
}
