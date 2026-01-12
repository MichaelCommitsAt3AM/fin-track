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

    @Query("SELECT * FROM savings WHERE id = :savingId")
    fun getSaving(savingId: String): Flow<SavingEntity?>

    @Query("SELECT * FROM savings WHERE userId = :userId ORDER BY createdAt DESC")
    fun getAllSavings(userId: String): Flow<List<SavingEntity>>

    @Query("DELETE FROM savings WHERE id = :savingId")
    suspend fun deleteSaving(savingId: String)

    @Query("DELETE FROM savings WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}
