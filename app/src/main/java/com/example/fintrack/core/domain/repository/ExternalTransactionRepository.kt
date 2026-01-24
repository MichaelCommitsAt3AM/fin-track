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
     * Observes newly added transactions (usually recent ones).
     *
     * @return Flow of recent transactions from external sources.
     */
    fun observeNewTransactions(): Flow<List<Transaction>>

    /**
     * Retrieves all transactions from external sources.
     *
     * @return Flow of all transactions from external sources.
     */
    fun getAllTransactions(): Flow<List<Transaction>>
}
