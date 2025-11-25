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
    private val firebaseAuth: FirebaseAuth,    // <-- Inject Auth
    private val firestore: FirebaseFirestore   // <-- Inject Firestore
) : BudgetRepository {

    private fun getUserId(): String? = firebaseAuth.currentUser?.uid

    private fun getUserBudgetsCollection() = firestore.collection("users")
        .document(getUserId() ?: "unknown_user")
        .collection("budgets")

    override suspend fun insertBudget(budget: Budget) {
        // 1. Save locally
        budgetDao.insertBudget(budget.toEntity())

        // 2. Save to Cloud
        getUserId()?.let {
            try {
                // Create a unique ID for Firestore: "CategoryName_Month_Year"
                val compositeId = "${budget.categoryName}_${budget.month}_${budget.year}"

                val budgetData = hashMapOf(
                    "categoryName" to budget.categoryName,
                    "userId" to budget.userId, // ADD THIS
                    "amount" to budget.amount,
                    "month" to budget.month,
                    "year" to budget.year
                )

                getUserBudgetsCollection()
                    .document(compositeId)
                    .set(budget)
                    .await()

                Log.d("BudgetRepo", "Budget saved: $compositeId")
            } catch (e: Exception) {
                Log.e("BudgetRepo", "Error saving budget to cloud: ${e.message}")
            }
        }
    }

    override fun getBudget(categoryName: String, month: Int, year: Int): Flow<Budget?> {
        val userId = getUserId() ?: return kotlinx.coroutines.flow.flowOf(null)

        // Read from local
        return budgetDao.getBudget(userId, categoryName, month, year).map { entity ->
            entity?.toDomain()
        }
    }

    override fun getAllBudgetsForMonth(month: Int, year: Int): Flow<List<Budget>> {
        val userId = getUserId() ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        // Read from local
        return budgetDao.getAllBudgetsForMonth(userId, month, year).map { entityList ->
            entityList.map { it.toDomain() }
        }
    }


    override suspend fun deleteBudget(categoryName: String, month: Int, year: Int) {
        val userId = getUserId() ?: return

        // 1. Delete locally
        budgetDao.deleteBudget(userId, categoryName, month, year)

        // 2. Delete from Cloud
        try {
            val compositeId = "${categoryName}_${month}_${year}"
            getUserBudgetsCollection()
                .document(compositeId)
                .delete()
                .await()

            Log.d("BudgetRepo", "Budget deleted: $compositeId")
        } catch (e: Exception) {
            Log.e("BudgetRepo", "Error deleting budget from cloud: ${e.message}")
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
                            userId = userId, // ADD THIS
                            amount = doc.getDouble("amount") ?: 0.0,
                            month = doc.getLong("month")?.toInt() ?: 0,
                            year = doc.getLong("year")?.toInt() ?: 0
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
}