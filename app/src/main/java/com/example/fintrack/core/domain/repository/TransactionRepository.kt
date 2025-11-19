package com.example.fintrack.core.domain.repository

import com.example.fintrack.core.domain.model.RecurringTransaction
import com.example.fintrack.core.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

// The "contract" for handling transaction data
interface TransactionRepository {

    suspend fun insertTransaction(transaction: Transaction)

    suspend fun updateTransaction(transaction: Transaction)

    // Using Flow to get real-time updates in the UI
    fun getAllTransactions(): Flow<List<Transaction>>

    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>>

    fun getTransactionsByType(type: String): Flow<List<Transaction>>

    // Fetch the latest N transactions (eg limit = 3)
    fun getRecentTransactions(limit: Int): Flow<List<Transaction>>

    suspend fun insertRecurringTransaction(recurringTransaction: RecurringTransaction)

    suspend fun syncTransactionsFromCloud()

}