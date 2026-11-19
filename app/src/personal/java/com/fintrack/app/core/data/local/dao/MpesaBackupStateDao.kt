package com.fintrack.app.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fintrack.app.core.data.local.model.MpesaBackupStateEntity

@Dao
interface MpesaBackupStateDao {
    @Query("SELECT * FROM mpesa_backup_state")
    suspend fun getAll(): List<MpesaBackupStateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rows: List<MpesaBackupStateEntity>)

    @Query("DELETE FROM mpesa_backup_state WHERE docId IN (:docIds)")
    suspend fun deleteByIds(docIds: List<String>)

    @Query("DELETE FROM mpesa_backup_state")
    suspend fun clear()
}
