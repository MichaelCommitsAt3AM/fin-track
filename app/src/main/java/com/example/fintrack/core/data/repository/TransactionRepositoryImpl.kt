package com.example.fintrack.core.data.repository

import android.util.Log
import com.example.fintrack.core.data.local.FinanceDatabase
import com.example.fintrack.core.data.local.dao.TransactionDao
import com.example.fintrack.core.data.local.model.TransactionEntity
import com.example.fintrack.core.data.mapper.toDomain
import com.example.fintrack.core.data.mapper.toEntity
import com.example.fintrack.core.domain.model.RecurringTransaction
import com.example.fintrack.core.domain.model.Transaction
import com.example.fintrack.core.domain.repository.NetworkRepository
import com.example.fintrack.core.domain.repository.TransactionRepository
import com.example.fintrack.core.data.local.dao.RecurringTransactionDao
import com.example.fintrack.core.data.local.model.RecurringTransactionEntity
import com.example.fintrack.core.data.local.SyncTimestampManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(
    private val db: FinanceDatabase,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val networkRepository: NetworkRepository,
    private val syncTimestampManager: SyncTimestampManager
) : TransactionRepository {

    private val transactionDao: TransactionDao = db.transactionDao()
    private val recurringTransactionDao: RecurringTransactionDao = db.recurringTransactionDao()

    private fun getUserId(): String? = firebaseAuth.currentUser?.uid

    private fun getUserTransactionsCollection() = firestore.collection("users")
        .document(getUserId() ?: "unknown_user")
        .collection("transactions")

    private fun getUserRecurringTransactionsCollection() = firestore.collection("users")
        .document(getUserId() ?: "unknown_user")
        .collection("recurring_transactions")

    // ============ REGULAR TRANSACTIONS ============

    override suspend fun insertTransaction(transaction: Transaction) {
        val userId = getUserId() ?: throw IllegalStateException("User is not logged in")
        val currentTime = System.currentTimeMillis()

        // If ID is empty, generate a new one
        val transactionId = if (transaction.id.isEmpty()) {
            getUserTransactionsCollection().document().id
        } else {
            transaction.id
        }

        // Auto-mark as planned if date is in the future
        val isPlanned = transaction.date > currentTime

        val transactionWithId = transaction.copy(
            id = transactionId,
            userId = userId,
            isPlanned = isPlanned,
            updatedAt = currentTime // Set updatedAt for sync tracking
        )

        // OFFLINE-FIRST: 1. Always save locally first with isSynced = false
        transactionDao.insertTransaction(transactionWithId.toEntity())
        Log.d("TransactionRepo", "Transaction saved locally: $transactionId ${if (isPlanned) "(Planned)" else ""}")

        // 2. If online, attempt to sync to Firestore immediately
        if (networkRepository.isNetworkAvailable()) {
            try {
                getUserTransactionsCollection()
                    .document(transactionId)
                    .set(transactionWithId)
                    .await()
                transactionDao.markAsSynced(transactionId)
                Log.d("TransactionRepo", "Transaction synced to Firestore: $transactionId")
            } catch (e: Exception) {
                Log.e("TransactionRepo", "Error syncing to Firestore (will retry later): ${e.message}")
            }
        } else {
            Log.d("TransactionRepo", "Offline - transaction will sync when online")
        }
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        // OFFLINE-FIRST: 1. Update locally first, mark as unsynced
        val updatedTransaction = transaction.copy(updatedAt = System.currentTimeMillis())
        val entityToUpdate = updatedTransaction.toEntity()
        transactionDao.updateTransaction(entityToUpdate)
        Log.d("TransactionRepo", "Transaction updated locally: ${transaction.id}")

        // 2. If online, attempt to sync to Firestore immediately
        getUserId()?.let {
            if (networkRepository.isNetworkAvailable()) {
                try {
                    getUserTransactionsCollection()
                        .document(transaction.id)
                        .set(updatedTransaction) // Upload with updatedAt
                        .await()
                    transactionDao.markAsSynced(transaction.id)
                    Log.d("TransactionRepo", "Transaction synced to Firestore: ${transaction.id}")
                } catch (e: Exception) {
                    Log.e("TransactionRepo", "Error syncing to Firestore (will retry later): ${e.message}")
                }
            } else {
                Log.d("TransactionRepo", "Offline - update will sync when online")
            }
        }
    }

    override suspend fun deleteTransaction(transactionId: String) {
        val userId = getUserId() ?: throw IllegalStateException("User is not logged in")
        val currentTime = System.currentTimeMillis()
        
        // Soft delete: mark transaction as deleted
        transactionDao.softDelete(transactionId, currentTime)
        Log.d("TransactionRepo", "Transaction soft-deleted locally: $transactionId")
        
        // If online, sync deletion to Firestore
        if (networkRepository.isNetworkAvailable()) {
            try {
                getUserTransactionsCollection()
                    .document(transactionId)
                    .update("deletedAt", currentTime)
                    .await()
                Log.d("TransactionRepo", "Transaction deletion synced to Firestore: $transactionId")
            } catch (e: Exception) {
                Log.e("TransactionRepo", "Error syncing deletion to Firestore (will retry later): ${e.message}")
            }
        } else {
            Log.d("TransactionRepo", "Offline - deletion will sync when online")
        }
    }

    override fun getTransactionById(transactionId: String): Flow<Transaction?> {
        val userId = getUserId() ?: return kotlinx.coroutines.flow.flowOf(null)
        return transactionDao.getTransactionById(transactionId, userId).map { entity ->
            entity?.toDomain()
        }
    }

    override fun getAllTransactionsPaged(limit: Int): Flow<List<Transaction>> {
        val userId = getUserId() ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return transactionDao.getAllTransactionsPaged(userId, limit).map { entityList ->
            entityList.map { it.toDomain() }
        }
    }

    override fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> {
        val userId = getUserId() ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return transactionDao.getTransactionsByDateRange(userId, startDate, endDate).map { entityList ->
            entityList.map { it.toDomain() }
        }
    }

    override fun getTransactionsByTypePaged(type: String, limit: Int): Flow<List<Transaction>> {
        val userId = getUserId() ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return transactionDao.getTransactionsByTypePaged(userId, type, limit).map { entityList ->
            entityList.map { it.toDomain() }
        }
    }

    override fun searchTransactions(query: String): Flow<List<Transaction>> {
        val userId = getUserId() ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return transactionDao.searchTransactions(userId, query).map { entityList ->
            entityList.map { it.toDomain() }
        }
    }

    override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> {
        val userId = getUserId() ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return transactionDao.getRecentTransactions(userId, limit).map { entityList ->
            entityList.map { it.toDomain() }
        }
    }
    
    override fun getPlannedTransactions(): Flow<List<Transaction>> {
        val userId = getUserId() ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return transactionDao.getPlannedTransactions(userId).map { entityList ->
            entityList.map { it.toDomain() }
        }
    }


    override suspend fun syncTransactionsFromCloud() {
        getUserId()?.let { userId ->
            try {
                // Get last sync timestamp
                val lastSyncTimestamp = syncTimestampManager.getLastTransactionSyncTimestamp()
                val currentSyncTimestamp = System.currentTimeMillis()
                
                Log.d("TransactionRepo", "Incremental sync: fetching transactions updated after $lastSyncTimestamp")
                
                // Query only transactions updated since last sync
                val query = if (lastSyncTimestamp > 0) {
                    // Incremental sync: fetch only changes
                    getUserTransactionsCollection()
                        .whereGreaterThan("updatedAt", lastSyncTimestamp)
                } else {
                    // First sync: fetch all
                    Log.d("TransactionRepo","First sync - fetching all transactions")
                    getUserTransactionsCollection()
                }
                
                val snapshot = query.get().await()

                Log.d("TransactionRepo", "Found ${snapshot.size()} changed transactions")

                val entities = snapshot.documents.mapNotNull { doc ->
                    try {
                        val deletedAt = doc.getLong("deletedAt")
                        
                        // If transaction is marked as deleted, delete it locally
                        if (deletedAt != null) {
                            Log.d("TransactionRepo", "Deleting transaction: ${doc.id}")
                            transactionDao.deleteById(doc.id)
                            null // Don't insert deleted items
                        } else {
                            // Normal transaction - insert/update
                            val entity = TransactionEntity(
                                id = doc.id,
                                userId = userId,
                                type = doc.getString("type") ?: "",
                                amount = doc.getDouble("amount") ?: 0.0,
                                category = doc.getString("category") ?: "",
                                date = doc.getLong("date") ?: 0L,
                                notes = doc.getString("notes"),
                                paymentMethod = doc.getString("paymentMethod"),
                                tags = doc.get("tags") as? List<String>,
                                updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis(),
                                deletedAt = null,
                                isSynced = true, // Mark as synced since it's from cloud
                                isPlanned = doc.getBoolean("isPlanned") ?: false
                            )
                            Log.d("TransactionRepo", "Mapped transaction: ${entity.id}")
                            entity
                        }
                    } catch (e: Exception) {
                        Log.e("TransactionRepo", "Error mapping document ${doc.id}: ${e.message}", e)
                        null
                    }
                }

                if (entities.isNotEmpty()) {
                    Log.d("TransactionRepo", "Inserting/updating ${entities.size} transactions")
                    transactionDao.insertAll(entities)
                }
                
                // Update last sync timestamp
                syncTimestampManager.setLastTransactionSyncTimestamp(currentSyncTimestamp)
                Log.d("TransactionRepo", "Incremental sync completed successfully")
                
            } catch (e: Exception) {
                Log.e("TransactionRepo", "Sync failed: ${e.message}", e)
            }
        } ?: Log.e("TransactionRepo", "Cannot sync: User ID is null")
    }

    // ============ RECURRING TRANSACTIONS ============

    override suspend fun insertRecurringTransaction(recurringTransaction: RecurringTransaction) {
        val userId = getUserId() ?: throw IllegalStateException("User not logged in")

        // Ensure userId is set
        val transactionWithUserId = recurringTransaction.copy(userId = userId)

        // 1. Save locally
        recurringTransactionDao.insert(transactionWithUserId.toEntity())

        // 2. Save to Firestore
        try {
            val docId = if (recurringTransaction.id == 0) {
                getUserRecurringTransactionsCollection().document().id
            } else {
                recurringTransaction.id.toString()
            }

            getUserRecurringTransactionsCollection()
                .document(docId)
                .set(transactionWithUserId)
                .await()

            Log.d("TransactionRepo", "Recurring transaction saved: $docId")
        } catch (e: Exception) {
            Log.e("TransactionRepo", "Error saving recurring transaction: ${e.message}")
        }
    }

    override fun getAllRecurringTransactions(): Flow<List<RecurringTransaction>> {
        val userId = getUserId() ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return recurringTransactionDao.getAllRecurringTransactions(userId).map { entityList ->
            entityList.map { it.toDomain() }
        }
    }

    override suspend fun deleteRecurringTransaction(recurringTransaction: RecurringTransaction) {
        val userId = getUserId() ?: return

        // 1. Delete locally
        recurringTransactionDao.deleteById(recurringTransaction.id)
        Log.d("TransactionRepo", "Deleted recurring transaction locally: ${recurringTransaction.id}")

        // 2. Delete from Firestore
        try {
            getUserRecurringTransactionsCollection()
                .document(recurringTransaction.id.toString())
                .delete()
                .await()

            Log.d("TransactionRepo", "Recurring transaction deleted: ${recurringTransaction.id}")
        } catch (e: Exception) {
            Log.e("TransactionRepo", "Error deleting recurring transaction: ${e.message}")
        }
    }

    override suspend fun syncRecurringTransactionsFromCloud() {
        getUserId()?.let { userId ->
            try {
                Log.d("TransactionRepo", "Starting recurring transactions sync for user: $userId")
                val snapshot = getUserRecurringTransactionsCollection().get().await()

                Log.d("TransactionRepo", "Found ${snapshot.size()} recurring transactions in Firestore")

                val entities = snapshot.documents.mapNotNull { doc ->
                    try {
                        val entity = RecurringTransactionEntity(
                            id = doc.id.toIntOrNull() ?: 0,
                            userId = userId,
                            type = doc.getString("type") ?: "",
                            amount = doc.getDouble("amount") ?: 0.0,
                            category = doc.getString("category") ?: "",
                            startDate = doc.getLong("startDate") ?: 0L,
                            frequency = doc.getString("frequency") ?: "",
                            notes = doc.getString("notes")
                        )
                        Log.d("TransactionRepo", "Mapped recurring transaction: ${entity.id}")
                        entity
                    } catch (e: Exception) {
                        Log.e("TransactionRepo", "Error mapping document ${doc.id}: ${e.message}", e)
                        null
                    }
                }

                if (entities.isNotEmpty()) {
                    Log.d("TransactionRepo", "Inserting ${entities.size} recurring transactions into Room")
                    entities.forEach { recurringTransactionDao.insert(it) }
                    Log.d("TransactionRepo", "Recurring transactions sync completed")
                } else {
                    Log.w("TransactionRepo", "No valid recurring transactions to sync")
                }
            } catch (e: Exception) {
                Log.e("TransactionRepo", "Recurring transactions sync failed: ${e.message}", e)
            }
        } ?: Log.e("TransactionRepo", "Cannot sync: User ID is null")
    }

    override suspend fun syncUnsyncedTransactions() {
        val userId = getUserId() ?: run {
            Log.e("TransactionRepo", "Cannot sync: User not logged in")
            return
        }

        if (!networkRepository.isNetworkAvailable()) {
            Log.d("TransactionRepo", "Network unavailable, skipping sync")
            return
        }

        try {
            val unsyncedTransactions = transactionDao.getUnsyncedTransactions(userId)
            Log.d("TransactionRepo", "Found ${unsyncedTransactions.size} unsynced transactions")

            unsyncedTransactions.forEach { entity ->
                try {
                    // Check if transaction is deleted
                    if (entity.deletedAt != null) {
                        // For deleted transactions, just update the deletedAt field
                        getUserTransactionsCollection()
                            .document(entity.id)
                            .update("deletedAt", entity.deletedAt)
                            .await()
                        Log.d("TransactionRepo", "Synced deletion for transaction: ${entity.id}")
                    } else {
                        // For normal transactions, sync the full document
                        val transaction = entity.toDomain().copy(
                            updatedAt = System.currentTimeMillis()
                        )
                        
                        getUserTransactionsCollection()
                            .document(entity.id)
                            .set(transaction) // Upload with updatedAt
                            .await()
                        Log.d("TransactionRepo", "Synced transaction: ${entity.id}")
                    }
                    
                    // Mark as synced after successful upload
                    transactionDao.markAsSynced(entity.id)
                } catch (e: Exception) {
                    Log.e("TransactionRepo", "Failed to sync transaction ${entity.id}: ${e.message}")
                }
            }

            Log.d("TransactionRepo", "Sync completed")
        } catch (e: Exception) {
            Log.e("TransactionRepo", "Error during sync: ${e.message}", e)
        }
    }
}
