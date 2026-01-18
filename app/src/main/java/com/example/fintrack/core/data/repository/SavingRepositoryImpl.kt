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
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class SavingRepositoryImpl @Inject constructor(
    private val savingDao: SavingDao,
    private val contributionDao: ContributionDao,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val networkRepository: com.example.fintrack.core.domain.repository.NetworkRepository
) : SavingRepository {

    private fun getUserId(): String? = firebaseAuth.currentUser?.uid

    private fun getUserSavingsCollection() = firestore.collection("users")
        .document(getUserId() ?: "unknown_user")
        .collection("savings")

    override suspend fun insertSaving(saving: Saving) {
        // Save locally
        val entity = saving.toEntity().copy(
            isSynced = false,
            updatedAt = System.currentTimeMillis()
        )
        savingDao.insertSaving(entity)
        Log.d("SavingRepo", "Saving saved locally: ${saving.id}")

        // Save to Cloud if online
        if (networkRepository.isNetworkAvailable()) {
            getUserId()?.let {
                try {
                    val data = saving.toEntity().copy(
                            isSynced = true,
                            updatedAt = System.currentTimeMillis(),
                            deletedAt = null
                    )
                    
                    // We need to map saving + fields to a map or object for Firestore
                    // The simplest is to just use the saving object + updates, 
                    // but we added fields to Entity, not Domain model.
                    // So we must construct the map manually or map appropriately.
                    
                    val savingData = hashMapOf(
                        "id" to saving.id,
                        "userId" to saving.userId,
                        "title" to saving.title,
                        "targetAmount" to saving.targetAmount,
                        "currentAmount" to saving.currentAmount,
                        "targetDate" to saving.targetDate,
                        "notes" to saving.notes,
                        "iconName" to saving.iconName,
                        "createdAt" to saving.createdAt,
                        "updatedAt" to entity.updatedAt,
                        "deletedAt" to null
                    )

                    getUserSavingsCollection()
                        .document(saving.id)
                        .set(savingData)
                        .await()
                    
                    savingDao.markAsSynced(saving.id)
                    Log.d("SavingRepo", "Saving saved to cloud: ${saving.id}")
                } catch (e: Exception) {
                    Log.e("SavingRepo", "Error saving to cloud: ${e.message}")
                }
            }
        } else {
             Log.d("SavingRepo", "Offline - saving will sync when online")
        }
    }

    override suspend fun updateSaving(saving: Saving) {
         // Update locally
        val entity = saving.toEntity().copy(
            isSynced = false,
            updatedAt = System.currentTimeMillis()
        )
        savingDao.updateSaving(entity)
        Log.d("SavingRepo", "Saving updated locally: ${saving.id}")

        // Update in Cloud
        if (networkRepository.isNetworkAvailable()) {
            getUserId()?.let {
                try {
                     val savingData = hashMapOf(
                        "id" to saving.id,
                        "userId" to saving.userId,
                        "title" to saving.title,
                        "targetAmount" to saving.targetAmount,
                        "currentAmount" to saving.currentAmount,
                        "targetDate" to saving.targetDate,
                        "notes" to saving.notes,
                        "iconName" to saving.iconName,
                        "createdAt" to saving.createdAt,
                        "updatedAt" to entity.updatedAt,
                        "deletedAt" to null
                    )
                    
                    getUserSavingsCollection()
                        .document(saving.id)
                        .set(savingData)
                        .await()
                        
                    savingDao.markAsSynced(saving.id)
                    Log.d("SavingRepo", "Saving updated in cloud: ${saving.id}")
                } catch (e: Exception) {
                    Log.e("SavingRepo", "Error updating in cloud: ${e.message}")
                }
            }
        } else {
            Log.d("SavingRepo", "Offline - update will sync when online")
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
        // Soft delete locally
        val currentTime = System.currentTimeMillis()
        savingDao.softDelete(savingId, currentTime)
        
        // We probably should DELETE contributions too? 
        // Or at least visually hide them if saving is gone.
        // For now, let's stick to core saving deletion.
        contributionDao.deleteAllForSaving(savingId) // This is a hard delete for sub-items. Consider soft-delete?
        // Since contributions are children of saving, if saving is soft-deleted, UI won't show it, so contributions are hidden.
        // It's safer to not hard delete contributions yet if we want full reversibility, but current requirement is just offline support.
        
        Log.d("SavingRepo", "Saving soft-deleted locally: $savingId")

        // Delete from Cloud
        if (networkRepository.isNetworkAvailable()) {
            getUserId()?.let {
                try {
                    getUserSavingsCollection()
                        .document(savingId)
                        .delete()
                        .await()
                    Log.d("SavingRepo", "Saving deleted from cloud: $savingId")
                } catch (e: Exception) {
                    Log.e("SavingRepo", "Error deleting from cloud: ${e.message}")
                }
            }
        } else {
            Log.d("SavingRepo", "Offline - deletion will sync when online")
        }
    }

    override suspend fun addContribution(contribution: Contribution, currentSavingAmount: Double) {
        // Add contribution locally
        contributionDao.insertContribution(contribution.toEntity())

        // Update saving's current amount
        val savingEntity = savingDao.getSaving(contribution.savingId)
        // Note: collecting flow here is tricky inside suspend function as it might not emit immediately or emit multiple times.
        // Ideally we should use a one-shot query or first()
        
        // Simplified approach for now as per existing pattern
         
        // THIS LOGIC HAS A FLAW: .collect blocks. We need .first()
        try {
            val entity = savingDao.getSaving(contribution.savingId).firstOrNull()
            entity?.let {
                val updatedSaving = it.copy(
                    currentAmount = currentSavingAmount + contribution.amount,
                    updatedAt = System.currentTimeMillis(),
                    isSynced = false
                )
                savingDao.updateSaving(updatedSaving)

                // Update in Cloud
                 if (networkRepository.isNetworkAvailable()) {
                    getUserId()?.let { userId ->
                        try {
                            getUserSavingsCollection()
                                .document(updatedSaving.id)
                                .update("currentAmount", updatedSaving.currentAmount, "updatedAt", updatedSaving.updatedAt)
                                .await()
                            
                            savingDao.markAsSynced(updatedSaving.id)
                        } catch (e: Exception) {
                            Log.e("SavingRepo", "Error updating contribution in cloud: ${e.message}")
                        }
                    }
                 }
            }
        } catch(e: Exception) {
             Log.e("SavingRepo", "Error adding contribution: ${e.message}")
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

                val entities = snapshot.documents.mapNotNull { doc ->
                    try {
                        // Manual mapping to handle new fields
                        val id = doc.getString("id") ?: doc.id
                        val title = doc.getString("title") ?: ""
                        
                        com.example.fintrack.core.data.local.model.SavingEntity(
                            id = id,
                            userId = userId,
                            title = title,
                            targetAmount = doc.getDouble("targetAmount") ?: 0.0,
                            currentAmount = doc.getDouble("currentAmount") ?: 0.0,
                            targetDate = doc.getLong("targetDate") ?: 0L,
                            notes = doc.getString("notes"),
                            iconName = doc.getString("iconName") ?: "savings",
                            createdAt = doc.getLong("createdAt") ?: 0L,
                            isSynced = true,
                            deletedAt = null,
                            updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                        )
                    } catch (e: Exception) {
                        Log.e("SavingRepo", "Error mapping document ${doc.id}: ${e.message}", e)
                        null
                    }
                }
                
                if (entities.isNotEmpty()) {
                     entities.forEach { savingDao.insertSaving(it) }
                }

                Log.d("SavingRepo", "Sync completed successfully")
            } catch (e: Exception) {
                Log.e("SavingRepo", "Sync failed: ${e.message}", e)
            }
        }
    }
    
    override suspend fun syncUnsyncedSavings() {
        val userId = getUserId() ?: return
        
        if (!networkRepository.isNetworkAvailable()) return
        
        try {
            val unsynced = savingDao.getUnsyncedSavings(userId)
            Log.d("SavingRepo", "Found ${unsynced.size} unsynced savings")
            
            unsynced.forEach { entity ->
                try {
                    if (entity.deletedAt != null) {
                        // Pending delete
                         getUserSavingsCollection()
                            .document(entity.id)
                            .delete()
                            .await()
                         Log.d("SavingRepo", "Synced saving deletion: ${entity.id}")
                         // We maintain soft-delete locally
                    } else {
                        // Pending update/insert
                        val savingData = hashMapOf(
                            "id" to entity.id,
                            "userId" to userId,
                            "title" to entity.title,
                            "targetAmount" to entity.targetAmount,
                            "currentAmount" to entity.currentAmount,
                            "targetDate" to entity.targetDate,
                            "notes" to entity.notes,
                            "iconName" to entity.iconName,
                            "createdAt" to entity.createdAt,
                            "updatedAt" to entity.updatedAt,
                            "deletedAt" to null
                        )
                        
                        getUserSavingsCollection()
                            .document(entity.id)
                            .set(savingData)
                            .await()
                            
                        savingDao.markAsSynced(entity.id)
                        Log.d("SavingRepo", "Synced saving update: ${entity.id}")
                    }
                } catch (e: Exception) {
                     Log.e("SavingRepo", "Failed to sync saving ${entity.id}: ${e.message}")
                }
            }
        } catch (e: Exception) {
             Log.e("SavingRepo", "Error during savings sync: ${e.message}", e)
        }
    }
}
