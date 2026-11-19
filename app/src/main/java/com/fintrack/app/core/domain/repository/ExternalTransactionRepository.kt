package com.fintrack.app.core.domain.repository

import com.fintrack.app.core.domain.model.Transaction
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

    /**
     * Re-categorise an external transaction. External transactions are read-only copies of
     * their source (e.g. an SMS), so the category is stored as a local override instead of
     * editing the transaction itself.
     */
    suspend fun setCategory(transactionId: String, categoryName: String)

    companion object {
        /** Prefix for IDs of external (M-Pesa) transactions, so they're never mistaken for manual ones. */
        const val ID_PREFIX = "mpesa_"

        fun isExternalId(transactionId: String) = transactionId.startsWith(ID_PREFIX)
    }
}
