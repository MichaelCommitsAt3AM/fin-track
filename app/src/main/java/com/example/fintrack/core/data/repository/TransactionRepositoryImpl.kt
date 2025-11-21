package com.example.fintrack.core.data.repository

import android.util.Log
import com.example.fintrack.core.data.local.FinanceDatabase
import com.example.fintrack.core.data.local.dao.TransactionDao
import com.example.fintrack.core.data.local.model.TransactionEntity
import com.example.fintrack.core.data.mapper.toDomain
import com.example.fintrack.core.data.mapper.toEntity
import com.example.fintrack.core.domain.model.RecurringTransaction
import com.example.fintrack.core.domain.model.Transaction
import com.example.fintrack.core.domain.model.TransactionType
import com.example.fintrack.core.domain.model.RecurrenceFrequency
import com.example.fintrack.core.domain.repository.TransactionRepository
import com.example.fintrack.data.local.dao.RecurringTransactionDao
import com.example.fintrack.data.local.model.RecurringTransactionEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(
    private val db: FinanceDatabase,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : TransactionRepository {

    private val transactionDao: TransactionDao = db.transactionDao()
    private val recurringTransactionDao: RecurringTransactionDao = db.recurringTransactionDao()

    private fun getUserId(): String? = firebaseAuth.currentUser?.uid

    private fun getUserTransactionsCollection() = firestore.collection("users")
        .document(getUserId() ?: "unknown_user")
        .collection("transactions")

    override suspend fun insertTransaction(transaction: Transaction) {
        val transactionId = if (transaction.id.isEmpty()) {
            getUserTransactionsCollection().document().id
        } else {
            transaction.id
        }

        val transactionWithId = transaction.copy(id = transactionId)

        transactionDao.insertTransaction(transactionWithId.toEntity())

        getUserId()?.let { userId ->
            try {
                getUserTransactionsCollection()
                    .document(transactionId)
                    .set(transactionWithId)
                    .await()
                Log.d("TransactionRepo", "Transaction saved with ID: $transactionId")
            } catch (e: Exception) {
                Log.e("TransactionRepo", "Error saving to Firestore: ${e.message}")
            }
        }
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction.toEntity())

        getUserId()?.let {
            try {
                getUserTransactionsCollection()
                    .document(transaction.id)
                    .set(transaction)
                    .await()
                Log.d("TransactionRepo", "Transaction updated: ${transaction.id}")
            } catch (e: Exception) {
                Log.e("TransactionRepo", "Error updating Firestore: ${e.message}")
            }
        }
    }

    override fun getAllTransactions(): Flow<List<Transaction>> {
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

    override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> {
        return transactionDao.getRecentTransactions(limit).map { entityList ->
            entityList.map { it.toDomain() }
        }
    }

    // ============ RECURRING TRANSACTIONS ============

    override fun getAllRecurringTransactions(): Flow<List<RecurringTransaction>> {
        return recurringTransactionDao.getAllRecurringTransactions().map { entityList ->
            entityList.map { entity ->
                RecurringTransaction(
                    type = TransactionType.valueOf(entity.type),
                    amount = entity.amount,
                    category = entity.category,
                    startDate = entity.startDate,
                    frequency = RecurrenceFrequency.valueOf(entity.frequency),
                    notes = entity.notes
                )
            }
        }
    }

    override suspend fun insertRecurringTransaction(recurringTransaction: RecurringTransaction) {
        // 1. Save Locally
        val entity = RecurringTransactionEntity(
            type = recurringTransaction.type.name,
            amount = recurringTransaction.amount,
            category = recurringTransaction.category,
            startDate = recurringTransaction.startDate,
            frequency = recurringTransaction.frequency.name,
            notes = recurringTransaction.notes
        )
        recurringTransactionDao.insert(entity)

        // 2. Save to Cloud
        getUserId()?.let {
            try {
                firestore.collection("users")
                    .document(it)
                    .collection("recurring_transactions")
                    .add(recurringTransaction)
                    .await()
                Log.d("TransactionRepo", "Recurring transaction saved to cloud")
            } catch (e: Exception) {
                Log.e("TransactionRepo", "Error saving recurring to cloud: ${e.message}")
            }
        }
    }

    override suspend fun deleteRecurringTransaction(recurringTransaction: RecurringTransaction) {
        // 1. Delete locally
        recurringTransactionDao.delete(recurringTransaction.category)
        Log.d("TransactionRepo", "Deleted recurring transaction locally: ${recurringTransaction.category}")

        // 2. Delete from Firestore
        getUserId()?.let { userId ->
            try {
                val snapshot = firestore.collection("users")
                    .document(userId)
                    .collection("recurring_transactions")
                    .whereEqualTo("category", recurringTransaction.category)
                    .whereEqualTo("amount", recurringTransaction.amount)
                    .get()
                    .await()

                snapshot.documents.forEach { doc ->
                    doc.reference.delete().await()
                    Log.d("TransactionRepo", "Deleted recurring transaction from cloud: ${doc.id}")
                }
            } catch (e: Exception) {
                Log.e("TransactionRepo", "Error deleting recurring transaction from cloud: ${e.message}")
            }
        }
    }

    override suspend fun syncTransactionsFromCloud() {
        getUserId()?.let { userId ->
            try {
                Log.d("TransactionRepo", "Starting transaction sync for user: $userId")
                val snapshot = getUserTransactionsCollection().get().await()

                Log.d("TransactionRepo", "Found ${snapshot.size()} transactions in Firestore")

                val entities = snapshot.documents.mapNotNull { doc ->
                    try {
                        val entity = TransactionEntity(
                            id = doc.id,
                            type = doc.getString("type") ?: "",
                            amount = doc.getDouble("amount") ?: 0.0,
                            category = doc.getString("category") ?: "",
                            date = doc.getLong("date") ?: 0L,
                            notes = doc.getString("notes"),
                            paymentMethod = doc.getString("paymentMethod"),
                            tags = doc.get("tags") as? List<String>
                        )
                        Log.d("TransactionRepo", "Mapped transaction: ${entity.id}")
                        entity
                    } catch (e: Exception) {
                        Log.e("TransactionRepo", "Error mapping document ${doc.id}: ${e.message}", e)
                        null
                    }
                }

                if (entities.isNotEmpty()) {
                    Log.d("TransactionRepo", "Inserting ${entities.size} transactions into Room")
                    transactionDao.insertAll(entities)
                    Log.d("TransactionRepo", "Sync completed successfully")
                } else {
                    Log.w("TransactionRepo", "No valid transactions to sync")
                }
            } catch (e: Exception) {
                Log.e("TransactionRepo", "Sync failed: ${e.message}", e)
            }
        } ?: Log.e("TransactionRepo", "Cannot sync: User ID is null")
    }
}
