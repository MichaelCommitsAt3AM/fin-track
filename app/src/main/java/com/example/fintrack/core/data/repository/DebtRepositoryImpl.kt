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
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class DebtRepositoryImpl @Inject constructor(
    private val debtDao: DebtDao,
    private val paymentDao: PaymentDao,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val networkRepository: com.example.fintrack.core.domain.repository.NetworkRepository
) : DebtRepository {

    private fun getUserId(): String? = firebaseAuth.currentUser?.uid

    private fun getUserDebtsCollection() = firestore.collection("users")
        .document(getUserId() ?: "unknown_user")
        .collection("debts")

    override suspend fun insertDebt(debt: Debt) {
        // Save locally
        val entity = debt.toEntity().copy(
            isSynced = false,
            updatedAt = System.currentTimeMillis()
        )
        debtDao.insertDebt(entity)
        Log.d("DebtRepo", "Debt saved locally: ${debt.id}")

        // Save to Cloud if online
        if (networkRepository.isNetworkAvailable()) {
            getUserId()?.let {
                try {
                     val debtData = hashMapOf(
                        "id" to debt.id,
                        "userId" to debt.userId,
                        "title" to debt.title,
                        "originalAmount" to debt.originalAmount,
                        "currentBalance" to debt.currentBalance,
                        "minimumPayment" to debt.minimumPayment,
                        "dueDate" to debt.dueDate,
                        "interestRate" to debt.interestRate,
                        "notes" to debt.notes,
                        "iconName" to debt.iconName,
                        "debtType" to debt.debtType,
                        "createdAt" to debt.createdAt,
                        "updatedAt" to entity.updatedAt,
                        "deletedAt" to null
                    )
                    
                    getUserDebtsCollection()
                        .document(debt.id)
                        .set(debtData)
                        .await()
                    
                    debtDao.markAsSynced(debt.id)
                    Log.d("DebtRepo", "Debt saved to cloud: ${debt.id}")
                } catch (e: Exception) {
                    Log.e("DebtRepo", "Error saving to cloud: ${e.message}")
                }
            }
        } else {
            Log.d("DebtRepo", "Offline - debt will sync when online")
        }
    }

    override suspend fun updateDebt(debt: Debt) {
        // Update locally
        val entity = debt.toEntity().copy(
            isSynced = false,
            updatedAt = System.currentTimeMillis()
        )
        debtDao.updateDebt(entity)
        Log.d("DebtRepo", "Debt updated locally: ${debt.id}")

        // Update in Cloud
        if (networkRepository.isNetworkAvailable()) {
            getUserId()?.let {
                try {
                    val debtData = hashMapOf(
                        "id" to debt.id,
                        "userId" to debt.userId,
                        "title" to debt.title,
                        "originalAmount" to debt.originalAmount,
                        "currentBalance" to debt.currentBalance,
                        "minimumPayment" to debt.minimumPayment,
                        "dueDate" to debt.dueDate,
                        "interestRate" to debt.interestRate,
                        "notes" to debt.notes,
                        "iconName" to debt.iconName,
                        "debtType" to debt.debtType,
                        "createdAt" to debt.createdAt,
                        "updatedAt" to entity.updatedAt,
                        "deletedAt" to null
                    )
                    
                    getUserDebtsCollection()
                        .document(debt.id)
                        .set(debtData)
                        .await()
                        
                    debtDao.markAsSynced(debt.id)
                    Log.d("DebtRepo", "Debt updated in cloud: ${debt.id}")
                } catch (e: Exception) {
                    Log.e("DebtRepo", "Error updating in cloud: ${e.message}")
                }
            }
        } else {
            Log.d("DebtRepo", "Offline - update will sync when online")
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
        // Soft delete locally
        val currentTime = System.currentTimeMillis()
        debtDao.softDelete(debtId, currentTime)
        
        paymentDao.deleteAllForDebt(debtId) // Hard delete payments for now
        Log.d("DebtRepo", "Debt soft-deleted locally: $debtId")

        // Delete from Cloud
        if (networkRepository.isNetworkAvailable()) {
            getUserId()?.let {
                try {
                    getUserDebtsCollection()
                        .document(debtId)
                        .delete()
                        .await()
                    Log.d("DebtRepo", "Debt deleted from cloud: $debtId")
                } catch (e: Exception) {
                    Log.e("DebtRepo", "Error deleting from cloud: ${e.message}")
                }
            }
        } else {
            Log.d("DebtRepo", "Offline - deletion will sync when online")
        }
    }

    override suspend fun makePayment(payment: Payment, currentDebtBalance: Double) {
        // Add payment locally
        paymentDao.insertPayment(payment.toEntity())

        // Update debt's current balance
         try {
             val entity = debtDao.getDebt(payment.debtId).firstOrNull()
             entity?.let {
                 val updatedDebt = it.copy(
                     currentBalance = currentDebtBalance - payment.amount,
                     updatedAt = System.currentTimeMillis(),
                     isSynced = false
                 )
                 debtDao.updateDebt(updatedDebt)
                 
                 // Update in cloud
                  if (networkRepository.isNetworkAvailable()) {
                    getUserId()?.let { userId ->
                        try {
                            getUserDebtsCollection()
                                .document(updatedDebt.id)
                                .update("currentBalance", updatedDebt.currentBalance, "updatedAt", updatedDebt.updatedAt)
                                .await()
                             debtDao.markAsSynced(updatedDebt.id)
                        } catch (e: Exception) {
                            Log.e("DebtRepo", "Error updating payment in cloud: ${e.message}")
                        }
                    }
                  }
             }
         } catch(e: Exception) {
              Log.e("DebtRepo", "Error making payment: ${e.message}")
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

               val entities = snapshot.documents.mapNotNull { doc ->
                    try {
                        val id = doc.getString("id") ?: doc.id
                        val title = doc.getString("title") ?: ""
                        
                        com.example.fintrack.core.data.local.model.DebtEntity(
                            id = id,
                            userId = userId,
                            title = title,
                            originalAmount = doc.getDouble("originalAmount") ?: 0.0,
                            currentBalance = doc.getDouble("currentBalance") ?: 0.0,
                            minimumPayment = doc.getDouble("minimumPayment") ?: 0.0,
                            dueDate = doc.getLong("dueDate") ?: 0L,
                            interestRate = doc.getDouble("interestRate") ?: 0.0,
                            notes = doc.getString("notes"),
                            iconName = doc.getString("iconName") ?: "attach_money",
                            debtType = doc.getString("debtType") ?: "I_OWE",
                            createdAt = doc.getLong("createdAt") ?: 0L,
                            isSynced = true,
                            deletedAt = null,
                            updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                        )
                    } catch (e: Exception) {
                        Log.e("DebtRepo", "Error mapping document ${doc.id}: ${e.message}", e)
                        null
                    }
                }
                
                if (entities.isNotEmpty()) {
                    entities.forEach { debtDao.insertDebt(it) }
                }

                Log.d("DebtRepo", "Sync completed successfully")
            } catch (e: Exception) {
                Log.e("DebtRepo", "Sync failed: ${e.message}", e)
            }
        }
    }
    
    override suspend fun syncUnsyncedDebts() {
        val userId = getUserId() ?: return
         if (!networkRepository.isNetworkAvailable()) return
         
         try {
             val unsynced = debtDao.getUnsyncedDebts(userId)
             Log.d("DebtRepo", "Found ${unsynced.size} unsynced debts")
             
             unsynced.forEach { entity ->
                 try {
                     if (entity.deletedAt != null) {
                         // Pending delete
                          getUserDebtsCollection()
                            .document(entity.id)
                            .delete()
                            .await()
                         Log.d("DebtRepo", "Synced debt deletion: ${entity.id}")
                     } else {
                         // Pending update/insert
                        val debtData = hashMapOf(
                            "id" to entity.id,
                            "userId" to userId,
                            "title" to entity.title,
                            "originalAmount" to entity.originalAmount,
                            "currentBalance" to entity.currentBalance,
                            "minimumPayment" to entity.minimumPayment,
                            "dueDate" to entity.dueDate,
                            "interestRate" to entity.interestRate,
                            "notes" to entity.notes,
                            "iconName" to entity.iconName,
                            "debtType" to entity.debtType,
                            "createdAt" to entity.createdAt,
                            "updatedAt" to entity.updatedAt,
                            "deletedAt" to null
                        )
                        
                        getUserDebtsCollection()
                            .document(entity.id)
                            .set(debtData)
                            .await()
                            
                        debtDao.markAsSynced(entity.id)
                        Log.d("DebtRepo", "Synced debt update: ${entity.id}")
                     }
                 } catch (e: Exception) {
                      Log.e("DebtRepo", "Failed to sync debt ${entity.id}: ${e.message}")
                 }
             }
         } catch (e: Exception) {
            Log.e("DebtRepo", "Error during debt sync: ${e.message}", e)
         }
    }
}
