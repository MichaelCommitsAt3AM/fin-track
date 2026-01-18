package com.example.fintrack.core.data.repository

import android.util.Log
import com.example.fintrack.core.data.local.dao.BudgetDao
import com.example.fintrack.core.data.mapper.toDomain
import com.example.fintrack.core.data.mapper.toEntity
import com.example.fintrack.core.domain.model.Budget
import com.example.fintrack.core.domain.repository.BudgetRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.collections.map
import javax.inject.Inject

class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val networkRepository: com.example.fintrack.core.domain.repository.NetworkRepository
) : BudgetRepository {

    private fun getUserId(): String? = firebaseAuth.currentUser?.uid

    private fun getUserBudgetsCollection() = firestore.collection("users")
        .document(getUserId() ?: "unknown_user")
        .collection("budgets")

    override suspend fun insertBudget(budget: Budget) {
        val userId = getUserId() ?: return
        val entity = budget.toEntity().copy(
            isSynced = false,
            updatedAt = System.currentTimeMillis()
        )

        // 1. Save locally
        budgetDao.insertBudget(entity)
        Log.d("BudgetRepo", "Budget saved locally: ${budget.categoryName}")

        // 2. Save to Cloud if online
        if (networkRepository.isNetworkAvailable()) {
            try {
                // Create a unique ID for Firestore: "CategoryName_Month_Year"
                val compositeId = "${budget.categoryName}_${budget.month}_${budget.year}"

                val budgetData = hashMapOf(
                    "categoryName" to budget.categoryName,
                    "userId" to userId,
                    "amount" to budget.amount,
                    "month" to budget.month,
                    "year" to budget.year,
                    "updatedAt" to entity.updatedAt,
                    "deletedAt" to null
                )

                getUserBudgetsCollection()
                    .document(compositeId)
                    .set(budgetData)
                    .await()
                
                budgetDao.markAsSynced(userId, budget.categoryName, budget.month, budget.year)
                Log.d("BudgetRepo", "Budget saved to cloud: $compositeId")
            } catch (e: Exception) {
                Log.e("BudgetRepo", "Error saving budget to cloud: ${e.message}")
            }
        } else {
            Log.d("BudgetRepo", "Offline - budget will sync when online")
        }
    }

    override fun getBudget(categoryName: String, month: Int, year: Int): Flow<Budget?> {
        val userId = getUserId() ?: return kotlinx.coroutines.flow.flowOf(null)

        // Read from local (Dao now filters deleted items)
        return budgetDao.getBudget(userId, categoryName, month, year).map { entity ->
            entity?.toDomain()
        }
    }

    override fun getAllBudgetsForMonth(month: Int, year: Int): Flow<List<Budget>> {
        val userId = getUserId() ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        // Read from local (Dao now filters deleted items)
        return budgetDao.getAllBudgetsForMonth(userId, month, year).map { entityList ->
            entityList.map { it.toDomain() }
        }
    }


    override suspend fun deleteBudget(categoryName: String, month: Int, year: Int) {
        val userId = getUserId() ?: return
        val compositeId = "${categoryName}_${month}_${year}"
        val currentTime = System.currentTimeMillis()

        // 1. Soft Delete locally
        budgetDao.softDelete(userId, categoryName, month, year, currentTime)
        Log.d("BudgetRepo", "Budget soft-deleted locally: $compositeId")

        // 2. Delete from Cloud if online
        if (networkRepository.isNetworkAvailable()) {
            try {
                // We choose to DELETE the document entirely from Firestore to cleaner look, 
                // OR adapt "soft-delete" in cloud too. 
                // Given previous logic was hard delete, we'll stick to hard delete in cloud for now,
                // BUT we must ensure the local soft-delete eventually triggers this.
                
                getUserBudgetsCollection()
                    .document(compositeId)
                    .delete()
                    .await()

                Log.d("BudgetRepo", "Budget deleted from cloud: $compositeId")
            } catch (e: Exception) {
                Log.e("BudgetRepo", "Error deleting budget from cloud: ${e.message}")
            }
        } else {
            Log.d("BudgetRepo", "Offline - budget deletion will sync when online")
        }
    }

    override suspend fun syncBudgetsFromCloud() {
        getUserId()?.let { userId ->
            try {
                Log.d("BudgetRepo", "Starting budget sync for user: $userId")
                val snapshot = getUserBudgetsCollection().get().await()

                Log.d("BudgetRepo", "Found ${snapshot.size()} budgets in Firestore")

                val entities = snapshot.documents.mapNotNull { doc ->
                    try {
                        val entity = com.example.fintrack.core.data.local.model.BudgetEntity(
                            categoryName = doc.getString("categoryName") ?: "",
                            userId = userId,
                            amount = doc.getDouble("amount") ?: 0.0,
                            month = doc.getLong("month")?.toInt() ?: 0,
                            year = doc.getLong("year")?.toInt() ?: 0,
                            isSynced = true,
                            deletedAt = null,
                            updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                        )
                        Log.d("BudgetRepo", "Mapped budget: ${entity.categoryName}")
                        entity
                    } catch (e: Exception) {
                        Log.e("BudgetRepo", "Error mapping document ${doc.id}: ${e.message}", e)
                        null
                    }
                }

                if (entities.isNotEmpty()) {
                    Log.d("BudgetRepo", "Inserting ${entities.size} budgets into Room")
                    entities.forEach { budgetDao.insertBudget(it) }
                    Log.d("BudgetRepo", "Sync completed successfully")
                } else {
                    Log.w("BudgetRepo", "No valid budgets to sync")
                }
            } catch (e: Exception) {
                Log.e("BudgetRepo", "Sync failed: ${e.message}", e)
            }
        } ?: Log.e("BudgetRepo", "Cannot sync: User ID is null")
    }

    override suspend fun syncUnsyncedBudgets() {
        val userId = getUserId() ?: run {
            Log.e("BudgetRepo", "Cannot sync: User not logged in")
            return
        }

        if (!networkRepository.isNetworkAvailable()) {
            Log.d("BudgetRepo", "Network unavailable, skipping budget sync")
            return
        }

        try {
            val unsyncedBudgets = budgetDao.getUnsyncedBudgets(userId)
            Log.d("BudgetRepo", "Found ${unsyncedBudgets.size} unsynced budgets")

            unsyncedBudgets.forEach { entity ->
                try {
                    val compositeId = "${entity.categoryName}_${entity.month}_${entity.year}"

                    if (entity.deletedAt != null) {
                        // Pending deletion
                        getUserBudgetsCollection()
                            .document(compositeId)
                            .delete()
                            .await()
                        Log.d("BudgetRepo", "Synced budget deletion: $compositeId")
                        
                        // We can now hard delete locally logic if we want, or keep it as soft delete
                        // Keeping as soft-delete is safer but eventually might want to cleanup. 
                        // For now, we'll just leave it soft-deleted in DB.
                    } else {
                        // Pending insert/update
                        val budgetData = hashMapOf(
                            "categoryName" to entity.categoryName,
                            "userId" to userId,
                            "amount" to entity.amount,
                            "month" to entity.month,
                            "year" to entity.year,
                            "updatedAt" to entity.updatedAt,
                            "deletedAt" to null
                        )

                        getUserBudgetsCollection()
                            .document(compositeId)
                            .set(budgetData)
                            .await()
                        
                        budgetDao.markAsSynced(userId, entity.categoryName, entity.month, entity.year)
                        Log.d("BudgetRepo", "Synced budget update: $compositeId")
                    }
                } catch (e: Exception) {
                    Log.e("BudgetRepo", "Failed to sync budget ${entity.categoryName}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("BudgetRepo", "Error during budget sync: ${e.message}", e)
        }
    }
}