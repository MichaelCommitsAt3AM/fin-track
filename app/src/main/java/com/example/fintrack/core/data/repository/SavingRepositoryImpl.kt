package com.example.fintrack.core.data.repository

import android.util.Log
import com.example.fintrack.core.data.local.dao.SavingDao
import com.example.fintrack.core.data.local.dao.ContributionDao
import com.example.fintrack.core.data.mapper.toDomain
import com.example.fintrack.core.data.mapper.toEntity
import com.example.fintrack.core.domain.model.Saving
import com.example.fintrack.core.domain.model.Contribution
import com.example.fintrack.core.domain.repository.SavingRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class SavingRepositoryImpl @Inject constructor(
    private val savingDao: SavingDao,
    private val contributionDao: ContributionDao,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : SavingRepository {

    private fun getUserId(): String? = firebaseAuth.currentUser?.uid

    private fun getUserSavingsCollection() = firestore.collection("users")
        .document(getUserId() ?: "unknown_user")
        .collection("savings")

    override suspend fun insertSaving(saving: Saving) {
        // Save locally
        savingDao.insertSaving(saving.toEntity())

        // Save to Cloud
        getUserId()?.let {
            try {
                getUserSavingsCollection()
                    .document(saving.id)
                    .set(saving)
                    .await()
                Log.d("SavingRepo", "Saving saved: ${saving.id}")
            } catch (e: Exception) {
                Log.e("SavingRepo", "Error saving to cloud: ${e.message}")
            }
        }
    }

    override suspend fun updateSaving(saving: Saving) {
        // Update locally
        savingDao.updateSaving(saving.toEntity())

        // Update in Cloud
        getUserId()?.let {
            try {
                getUserSavingsCollection()
                    .document(saving.id)
                    .set(saving)
                    .await()
                Log.d("SavingRepo", "Saving updated: ${saving.id}")
            } catch (e: Exception) {
                Log.e("SavingRepo", "Error updating in cloud: ${e.message}")
            }
        }
    }

    override fun getSaving(savingId: String): Flow<Saving?> {
        return savingDao.getSaving(savingId).map { entity ->
            entity?.toDomain()
        }
    }

    override fun getAllSavings(userId: String): Flow<List<Saving>> {
        return savingDao.getAllSavings(userId).map { entityList ->
            entityList.map { it.toDomain() }
        }
    }

    override suspend fun deleteSaving(savingId: String) {
        // Delete locally
        savingDao.deleteSaving(savingId)
        contributionDao.deleteAllForSaving(savingId)

        // Delete from Cloud
        getUserId()?.let {
            try {
                getUserSavingsCollection()
                    .document(savingId)
                    .delete()
                    .await()
                Log.d("SavingRepo", "Saving deleted: $savingId")
            } catch (e: Exception) {
                Log.e("SavingRepo", "Error deleting from cloud: ${e.message}")
            }
        }
    }

    override suspend fun addContribution(contribution: Contribution, currentSavingAmount: Double) {
        // Add contribution locally
        contributionDao.insertContribution(contribution.toEntity())

        // Update saving's current amount
        val savingEntity = savingDao.getSaving(contribution.savingId)
        savingEntity.collect { entity ->
            entity?.let {
                val updatedSaving = it.copy(currentAmount = currentSavingAmount + contribution.amount)
                savingDao.updateSaving(updatedSaving)

                // Update in Cloud
                getUserId()?.let {
                    try {
                        getUserSavingsCollection()
                            .document(updatedSaving.id)
                            .update("currentAmount", updatedSaving.currentAmount)
                            .await()
                    } catch (e: Exception) {
                        Log.e("SavingRepo", "Error updating contribution in cloud: ${e.message}")
                    }
                }
            }
        }
    }

    override fun getContributionsForSaving(savingId: String): Flow<List<Contribution>> {
        return contributionDao.getContributionsForSaving(savingId).map { entityList ->
            entityList.map { it.toDomain() }
        }
    }

    override suspend fun syncSavingsFromCloud() {
        getUserId()?.let { userId ->
            try {
                Log.d("SavingRepo", "Starting savings sync for user: $userId")
                val snapshot = getUserSavingsCollection().get().await()

                Log.d("SavingRepo", "Found ${snapshot.size()} savings in Firestore")

                snapshot.documents.forEach { doc ->
                    try {
                        val saving = doc.toObject(Saving::class.java)
                        saving?.let {
                            savingDao.insertSaving(it.toEntity())
                        }
                    } catch (e: Exception) {
                        Log.e("SavingRepo", "Error mapping document ${doc.id}: ${e.message}", e)
                    }
                }

                Log.d("SavingRepo", "Sync completed successfully")
            } catch (e: Exception) {
                Log.e("SavingRepo", "Sync failed: ${e.message}", e)
            }
        }
    }
}
