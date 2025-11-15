package com.example.fintrack.core.domain.use_case

import com.example.fintrack.core.domain.model.Transaction
import com.example.fintrack.core.domain.repository.TransactionRepository
import javax.inject.Inject

// This use case has one job: add a transaction.
class AddTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(transaction: Transaction) {
        // You could add business logic here, e.g.:
        // if (transaction.amount <= 0) {
        //    throw InvalidTransactionException("Amount must be greater than zero.")
        // }
        repository.insertTransaction(transaction)
    }
}