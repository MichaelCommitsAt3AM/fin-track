package com.example.fintrack.core.data.repository

import com.example.fintrack.core.domain.model.Transaction
import com.example.fintrack.core.domain.repository.ExternalTransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject

/**
 * Store flavor implementation of ExternalTransactionRepository.
 * 
 * This is a no-op implementation that returns an empty flow.
 * The store variant does not read from any external sources (e.g., SMS)
 * to comply with Google Play policies.
 */
class StoreTransactionRepository @Inject constructor() : ExternalTransactionRepository {
    
    override fun observeNewTransactions(): Flow<List<Transaction>> {
        // Return empty flow - no external transactions for store build
        return emptyFlow()
    }

    override fun getAllTransactions(): Flow<List<Transaction>> {
        return emptyFlow()
    }
}
