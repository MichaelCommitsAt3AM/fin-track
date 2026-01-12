package com.example.fintrack.core.data.repository

import android.util.Log
import com.example.fintrack.core.data.local.dao.DebtDao
import com.example.fintrack.core.data.local.dao.PaymentDao
import com.example.fintrack.core.data.mapper.toDomain
import com.example.fintrack.core.data.mapper.toEntity
import com.example.fintrack.core.domain.model.Debt
import com.example.fintrack.core.domain.model.Payment
import com.example.fintrack.core.domain.repository.DebtRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class DebtRepositoryImpl @Inject constructor(
    private val debtDao: DebtDao,
    private val paymentDao: PaymentDao,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : DebtRepository {

    private fun getUserId(): String? = firebaseAuth.currentUser?.uid

    private fun getUserDebtsCollection() = firestore.collection("users")
        .document(getUserId() ?: "unknown_user")
        .collection("debts")

    override suspend fun insertDebt(debt: Debt) {
        // Save locally
        debtDao.insertDebt(debt.toEntity())

        // Save to Cloud
        getUserId()?.let {
            try {
                getUserDebtsCollection()
                    .document(debt.id)
                    .set(debt)
                    .await()
                Log.d("DebtRepo", "Debt saved: ${debt.id}")
            } catch (e: Exception) {
                Log.e("DebtRepo", "Error saving to cloud: ${e.message}")
            }
        }
    }

    override suspend fun updateDebt(debt: Debt) {
        // Update locally
        debtDao.updateDebt(debt.toEntity())

        // Update in Cloud
        getUserId()?.let {
            try {
                getUserDebtsCollection()
                    .document(debt.id)
                    .set(debt)
                    .await()
                Log.d("DebtRepo", "Debt updated: ${debt.id}")
            } catch (e: Exception) {
                Log.e("DebtRepo", "Error updating in cloud: ${e.message}")
            }
        }
    }

    override fun getDebt(debtId: String): Flow<Debt?> {
        return debtDao.getDebt(debtId).map { entity ->
            entity?.toDomain()
        }
    }

    override fun getAllDebts(userId: String): Flow<List<Debt>> {
        return debtDao.getAllDebts(userId).map { entityList ->
            entityList.map { it.toDomain() }
        }
    }

    override suspend fun deleteDebt(debtId: String) {
        // Delete locally
        debtDao.deleteDebt(debtId)
        paymentDao.deleteAllForDebt(debtId)

        // Delete from Cloud
        getUserId()?.let {
            try {
                getUserDebtsCollection()
                    .document(debtId)
                    .delete()
                    .await()
                Log.d("DebtRepo", "Debt deleted: $debtId")
            } catch (e: Exception) {
                Log.e("DebtRepo", "Error deleting from cloud: ${e.message}")
            }
        }
    }

    override suspend fun makePayment(payment: Payment, currentDebtBalance: Double) {
        // Add payment locally
        paymentDao.insertPayment(payment.toEntity())

        // Update debt's current balance
        val debt = debtDao.getDebt(payment.debtId)
        debt.collect { entity ->
            entity?.let {
                val updatedDebt = it.copy(currentBalance = currentDebtBalance - payment.amount)
                debtDao.updateDebt(updatedDebt)

                // Update in Cloud
                getUserId()?.let {
                    try {
                        getUserDebtsCollection()
                            .document(updatedDebt.id)
                            .update("currentBalance", updatedDebt.currentBalance)
                            .await()
                    } catch (e: Exception) {
                        Log.e("DebtRepo", "Error updating payment in cloud: ${e.message}")
                    }
                }
            }
        }
    }

    override fun getPaymentsForDebt(debtId: String): Flow<List<Payment>> {
        return paymentDao.getPaymentsForDebt(debtId).map { entityList ->
            entityList.map { it.toDomain() }
        }
    }

    override suspend fun syncDebtsFromCloud() {
        getUserId()?.let { userId ->
            try {
                Log.d("DebtRepo", "Starting debts sync for user: $userId")
                val snapshot = getUserDebtsCollection().get().await()

                Log.d("DebtRepo", "Found ${snapshot.size()} debts in Firestore")

                snapshot.documents.forEach { doc ->
                    try {
                        val debt = doc.toObject(Debt::class.java)
                        debt?.let {
                            debtDao.insertDebt(it.toEntity())
                        }
                    } catch (e: Exception) {
                        Log.e("DebtRepo", "Error mapping document ${doc.id}: ${e.message}", e)
                    }
                }

                Log.d("DebtRepo", "Sync completed successfully")
            } catch (e: Exception) {
                Log.e("DebtRepo", "Sync failed: ${e.message}", e)
            }
        }
    }
}
