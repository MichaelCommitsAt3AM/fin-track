package com.example.fintrack.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.fintrack.core.data.local.model.SavingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaving(saving: SavingEntity)

    @Update
    suspend fun updateSaving(saving: SavingEntity)

    @Query("SELECT * FROM savings WHERE id = :savingId AND deletedAt IS NULL")
    fun getSaving(savingId: String): Flow<SavingEntity?>

    @Query("SELECT * FROM savings WHERE userId = :userId AND deletedAt IS NULL ORDER BY createdAt DESC")
    fun getAllSavings(userId: String): Flow<List<SavingEntity>>

    @Query("DELETE FROM savings WHERE id = :savingId")
    suspend fun deleteSaving(savingId: String)

    // Soft delete
    @Query("UPDATE savings SET deletedAt = :deletedAt, isSynced = 0, updatedAt = :timestamp WHERE id = :savingId")
    suspend fun softDelete(savingId: String, deletedAt: Long, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM savings WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    // Get unsynced savings
    @Query("SELECT * FROM savings WHERE userId = :userId AND isSynced = 0")
    suspend fun getUnsyncedSavings(userId: String): List<SavingEntity>

    // Mark as updated
    @Query("UPDATE savings SET isSynced = 1 WHERE id = :savingId")
    suspend fun markAsSynced(savingId: String)
}
