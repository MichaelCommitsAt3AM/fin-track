package com.example.fintrack.data.repository

import android.util.Log
import com.example.fintrack.data.local.dao.TransactionDao
import com.example.fintrack.data.mapper.toDomain
import com.example.fintrack.data.mapper.toEntity
import com.example.fintrack.domain.model.Transaction
import com.example.fintrack.domain.repository.TransactionRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val firebaseAuth: FirebaseAuth,     // <-- Inject Auth
    private val firestore: FirebaseFirestore    // <-- Inject Firestore
) : TransactionRepository {

    // Helper to get the current user ID
    private fun getUserId(): String? = firebaseAuth.currentUser?.uid

    // Helper to get the user's specific collection
    private fun getUserTransactionsCollection() = firestore.collection("users")
        .document(getUserId() ?: "unknown_user") // Get the user's document
        .collection("transactions") // Get the transactions sub-collection

    override suspend fun insertTransaction(transaction: Transaction) {
        // 1. Save to local Room database (as before)
        transactionDao.insertTransaction(transaction.toEntity())

        // 2. Save to Cloud Firestore
        getUserId()?.let { userId ->
            try {
                // We use the domain model (Transaction) for Firestore,
                // as it's just a simple data class.
                // We set the document ID to match the Room ID for easy mapping.
                getUserTransactionsCollection()
                    .document(transaction.id.toString())
                    .set(transaction)
                    .await() // Wait for the cloud save to complete
            } catch (e: Exception) {
                Log.e("TransactionRepo", "Error saving to Firestore: ${e.message}")
                // Handle error: maybe add to a "failed sync" queue
            }
        }
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        // 1. Update in local Room database
        transactionDao.updateTransaction(transaction.toEntity())

        // 2. Update in Cloud Firestore
        getUserId()?.let {
            try {
                getUserTransactionsCollection()
                    .document(transaction.id.toString())
                    .set(transaction) // 'set' works for both create and update
                    .await()
            } catch (e: Exception) {
                Log.e("TransactionRepo", "Error updating Firestore: ${e.message}")
            }
        }
    }

    // --- READ OPERATIONS ---
    // For now, our read operations will continue to come from Room,
    // which is the "Single Source of Truth."

    override fun getAllTransactions(): Flow<List<Transaction>> {
        // This logic doesn't change. We read from the local DB.
        return transactionDao.getAllTransactions().map { entityList ->
            entityList.map { it.toDomain() }
        }
    }

    override fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByDateRange(startDate, endDate).map { entityList ->
            entityList.map { it.toDomain() }
        }
    }

    override fun getTransactionsByType(type: String): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByType(type).map { entityList ->
            entityList.map { it.toDomain() }
        }
    }
}