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

                getUserBudgetsCollection()
                    .document(compositeId)
                    .set(budget)
                    .await()
            } catch (e: Exception) {
                Log.e("BudgetRepo", "Error saving budget to cloud: ${e.message}")
            }
        }
    }

    override fun getBudget(categoryName: String, month: Int, year: Int): Flow<Budget?> {
        // Read from local
        return budgetDao.getBudget(categoryName, month, year).map { entity ->
            entity?.toDomain()
        }
    }

    override fun getAllBudgetsForMonth(month: Int, year: Int): Flow<List<Budget>> {
        // Read from local
        return budgetDao.getAllBudgetsForMonth(month, year).map { entityList ->
            entityList.map { it.toDomain() }
        }
    }
}