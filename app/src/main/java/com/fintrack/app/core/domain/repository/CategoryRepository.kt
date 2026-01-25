package com.fintrack.app.core.domain.repository

import com.fintrack.app.core.domain.model.Category
import kotlinx.coroutines.flow.Flow

// The "contract" for handling category data
interface CategoryRepository {

    suspend fun insertCategory(category: Category)

    suspend fun insertAllCategories(categories: List<Category>)

    fun getAllCategories(): Flow<List<Category>>

    suspend fun deleteCategory(category: Category)
    suspend fun initDefaultCategories()
    suspend fun syncCategoriesFromCloud()
    suspend fun syncUnsyncedCategories() // Sync local unsynced categories to cloud
}