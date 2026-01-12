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

    @Query("SELECT * FROM debts WHERE id = :debtId")
    fun getDebt(debtId: String): Flow<DebtEntity?>

    @Query("SELECT * FROM debts WHERE userId = :userId ORDER BY createdAt DESC")
    fun getAllDebts(userId: String): Flow<List<DebtEntity>>

    @Query("DELETE FROM debts WHERE id = :debtId")
    suspend fun deleteDebt(debtId: String)

    @Query("DELETE FROM debts WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}
