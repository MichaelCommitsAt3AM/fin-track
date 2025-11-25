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
    @Query("SELECT * FROM categories WHERE userId = :userId ORDER BY name ASC")
    fun getAllCategories(userId: String): Flow<List<CategoryEntity>>

    // Delete a category for a specific user
    @Query("DELETE FROM categories WHERE name = :name AND userId = :userId")
    suspend fun deleteCategory(name: String, userId: String)

    // Delete all categories for a user (logout)
    @Query("DELETE FROM categories WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}