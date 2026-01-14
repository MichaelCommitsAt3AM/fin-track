package com.example.fintrack.core.domain.use_case

import com.example.fintrack.core.domain.model.Transaction
import com.example.fintrack.core.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// This use case has one job: get all transactions.
class GetTransactionsUseCase @Inject constructor(
    private val repository: TransactionRepository // Hilt injects the repository
) {
    // We override 'invoke' so we can call the class like a function
    operator fun invoke(): Flow<List<Transaction>> {
        return repository.getAllTransactionsPaged(Int.MAX_VALUE)
    }
}