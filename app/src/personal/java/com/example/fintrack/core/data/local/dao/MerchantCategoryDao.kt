package com.example.fintrack.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.fintrack.core.data.local.model.MerchantCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantCategoryDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMapping(mapping: MerchantCategoryEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMappings(mappings: List<MerchantCategoryEntity>)
    
    @Query("SELECT * FROM merchant_categories WHERE merchantName = :merchantName")
    suspend fun getMapping(merchantName: String): MerchantCategoryEntity?
    
    @Query("SELECT * FROM merchant_categories")
    fun getAllMappings(): Flow<List<MerchantCategoryEntity>>
}
