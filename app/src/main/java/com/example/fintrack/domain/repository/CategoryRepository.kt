package com.example.fintrack.domain.repository

import com.example.fintrack.domain.model.Category
import kotlinx.coroutines.flow.Flow

// The "contract" for handling category data
interface CategoryRepository {

    suspend fun insertCategory(category: Category)

    suspend fun insertAllCategories(categories: List<Category>)

    fun getAllCategories(): Flow<List<Category>>
}