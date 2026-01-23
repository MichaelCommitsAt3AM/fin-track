package com.example.fintrack.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.fintrack.core.data.local.model.MpesaCategoryMappingEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for M-Pesa category mappings.
 * Provides methods to store and retrieve category assignments for M-Pesa transactions.
 */
@Dao
interface MpesaCategoryMappingDao {
    
    /**
     * Insert a single category mapping.
     * Replaces existing mapping if receipt number already exists.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mapping: MpesaCategoryMappingEntity)
    
    /**
     * Insert multiple category mappings at once.
     * Useful for batch operations during onboarding.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(mappings: List<MpesaCategoryMappingEntity>)
    
    /**
     * Get the category mapping for a specific M-Pesa receipt number.
     */
    @Query("SELECT * FROM mpesa_category_mappings WHERE mpesaReceiptNumber = :receiptNumber LIMIT 1")
    suspend fun getMappingForReceipt(receiptNumber: String): MpesaCategoryMappingEntity?
    
    /**
     * Get all category mappings as a Flow for reactive updates.
     */
    @Query("SELECT * FROM mpesa_category_mappings")
    fun getAllMappings(): Flow<List<MpesaCategoryMappingEntity>>
    
    /**
     * Delete a specific mapping.
     */
    @Query("DELETE FROM mpesa_category_mappings WHERE mpesaReceiptNumber = :receiptNumber")
    suspend fun deleteMapping(receiptNumber: String)
    
    /**
     * Delete all category mappings.
     */
    @Query("DELETE FROM mpesa_category_mappings")
    suspend fun deleteAllMappings()
}
