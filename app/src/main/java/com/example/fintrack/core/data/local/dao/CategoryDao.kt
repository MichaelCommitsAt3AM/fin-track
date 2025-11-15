package com.example.fintrack.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.fintrack.core.data.local.model.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    // Insert a new category or update it if it already exists
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    // Insert a list of default categories
    @Insert(onConflict = OnConflictStrategy.IGNORE) // Ignore if category already exists
    suspend fun insertAll(categories: List<CategoryEntity>)

    // Get all categories, ordered by name
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>
}