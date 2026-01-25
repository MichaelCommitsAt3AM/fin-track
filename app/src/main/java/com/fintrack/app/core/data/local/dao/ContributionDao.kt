package com.fintrack.app.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fintrack.app.core.data.local.model.ContributionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContributionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContribution(contribution: ContributionEntity)

    @Query("SELECT * FROM contributions WHERE savingId = :savingId ORDER BY date DESC")
    fun getContributionsForSaving(savingId: String): Flow<List<ContributionEntity>>

    @Query("DELETE FROM contributions WHERE id = :contributionId")
    suspend fun deleteContribution(contributionId: String)

    @Query("DELETE FROM contributions WHERE savingId = :savingId")
    suspend fun deleteAllForSaving(savingId: String)
}
