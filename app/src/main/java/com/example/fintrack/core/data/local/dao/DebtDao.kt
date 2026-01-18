package com.example.fintrack.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.fintrack.core.data.local.model.DebtEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: DebtEntity)

    @Update
    suspend fun updateDebt(debt: DebtEntity)

    @Query("SELECT * FROM debts WHERE id = :debtId AND deletedAt IS NULL")
    fun getDebt(debtId: String): Flow<DebtEntity?>

    @Query("SELECT * FROM debts WHERE userId = :userId AND deletedAt IS NULL ORDER BY createdAt DESC")
    fun getAllDebts(userId: String): Flow<List<DebtEntity>>

    @Query("DELETE FROM debts WHERE id = :debtId")
    suspend fun deleteDebt(debtId: String)

    // Soft delete
    @Query("UPDATE debts SET deletedAt = :deletedAt, isSynced = 0, updatedAt = :timestamp WHERE id = :debtId")
    suspend fun softDelete(debtId: String, deletedAt: Long, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM debts WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    // Get unsynced debts
    @Query("SELECT * FROM debts WHERE userId = :userId AND isSynced = 0")
    suspend fun getUnsyncedDebts(userId: String): List<DebtEntity>

    // Mark as updated
    @Query("UPDATE debts SET isSynced = 1 WHERE id = :debtId")
    suspend fun markAsSynced(debtId: String)
}
