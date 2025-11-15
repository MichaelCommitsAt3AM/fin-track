package com.example.fintrack.core.data.repository

import android.util.Log
import com.example.fintrack.core.data.local.dao.CategoryDao
import com.example.fintrack.core.data.mapper.toDomain
import com.example.fintrack.core.data.mapper.toEntity
import com.example.fintrack.core.domain.model.Category
import com.example.fintrack.core.domain.repository.CategoryRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao, // Hilt will inject this
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) : CategoryRepository {

    private fun getUserId(): String? = firebaseAuth.currentUser?.uid

    private fun getUserCategoriesCollection() =
        firestore.collection("users")
            .document(getUserId() ?: "unknown_user")
            .collection("categories")


    override suspend fun insertCategory(category: Category) {
        // 1. Save locally
        categoryDao.insertCategory(category.toEntity())

        // 2. Save to Firestore
        getUserId()?.let {
            try {
                getUserCategoriesCollection()
                    .document(category.name)
                    .set(category.toEntity()).await()
            } catch (e: Exception) {
                Log.e("CategoryRepo", "Error saving category to cloud: ${e.message}")
            }
        }
    }

    override suspend fun insertAllCategories(categories: List<Category>) {
        // 1. Save locally
        categoryDao.insertAll(categories.map { it.toEntity() })

        // 2. Save to cloud (Batch writing)
        getUserId()?.let {
            try {
                val batch = firestore.batch()
                val collectionRef = getUserCategoriesCollection()

                categories.forEach { category ->
                    val docRef = collectionRef.document(category.name)
                    batch.set(docRef, category)
                }
                batch.commit().await()
            } catch (e: Exception) {
                Log.e("CategoryRepo", "Error saving categories to cloud: ${e.message}")
            }
        }
    }

    override fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories().map { entityList ->
            entityList.map { it.toDomain() }
        }
    }
}