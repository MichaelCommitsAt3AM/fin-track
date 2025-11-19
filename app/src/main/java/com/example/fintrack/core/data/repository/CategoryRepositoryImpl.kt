package com.example.fintrack.core.data.repository

import android.util.Log
import com.example.fintrack.core.data.local.dao.CategoryDao
import com.example.fintrack.core.data.mapper.toDomain
import com.example.fintrack.core.data.mapper.toEntity
import com.example.fintrack.core.domain.model.Category
import com.example.fintrack.core.domain.repository.CategoryRepository
import com.example.fintrack.core.domain.model.CategoryType
import com.example.fintrack.core.data.local.model.CategoryEntity
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

    // --- Initialize Default categories ---
    override suspend fun initDefaultCategories() {
        val defaults = listOf(
            Category("Food", "restaurant", "#F39C12", CategoryType.EXPENSE, isDefault = true),
            Category("Rent", "house", "#8B5CF6", CategoryType.EXPENSE, isDefault = true),
            Category("Transport", "directions_bus", "#3498DB", CategoryType.EXPENSE, isDefault = true)
        )
        // Insert locally and to cloud
        insertAllCategories(defaults)
    }


    override suspend fun insertCategory(category: Category) {
        val entity = category.toEntity()

        // 1. Save locally
        categoryDao.insertCategory(entity)

        // 2. Save to Firestore with consistent field names
        getUserId()?.let {
            try {
                val firestoreData = hashMapOf(
                    "icon" to entity.iconName,  // Map iconName to icon
                    "color" to entity.colorHex,  // Map colorHex to color
                    "type" to entity.type,
                    "isDefault" to entity.isDefault
                )

                getUserCategoriesCollection()
                    .document(category.name)
                    .set(firestoreData)
                    .await()
                Log.d("CategoryRepo", "Category saved: ${category.name}")
            } catch (e: Exception) {
                Log.e("CategoryRepo", "Error saving category to cloud: ${e.message}")
            }
        }
    }


    // --- Delete Category ---
    override suspend fun deleteCategory(category: Category) {
        // 1. Delete locally
        categoryDao.deleteCategory(category.name)

        // 2. Delete from Cloud
        getUserId()?.let {
            try {
                getUserCategoriesCollection()
                    .document(category.name)
                    .delete()
                    .await()
            } catch (e: Exception) {
                Log.e("CategoryRepo", "Error deleting category: ${e.message}")
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

    override suspend fun syncCategoriesFromCloud() {
        getUserId()?.let { userId ->
            try {
                Log.d("CategoryRepo", "Starting category sync for user: $userId")
                val snapshot = getUserCategoriesCollection().get().await()

                Log.d("CategoryRepo", "Found ${snapshot.size()} categories in Firestore")

                val entities = snapshot.documents.mapNotNull { doc ->
                    try {
                        val entity = CategoryEntity(
                            name = doc.id,
                            iconName = doc.getString("icon") ?: doc.getString("iconName") ?: "", // Handle both field names
                            colorHex = doc.getString("color") ?: doc.getString("colorHex") ?: "",
                            type = doc.getString("type") ?: "",
                            isDefault = doc.getBoolean("isDefault") ?: false
                        )
                        Log.d("CategoryRepo", "Mapped category: ${entity.name}")
                        entity
                    } catch (e: Exception) {
                        Log.e("CategoryRepo", "Error mapping document ${doc.id}: ${e.message}", e)
                        null
                    }
                }

                if (entities.isNotEmpty()) {
                    Log.d("CategoryRepo", "Inserting ${entities.size} categories into Room")
                    categoryDao.insertAll(entities)
                    Log.d("CategoryRepo", "Sync completed successfully")
                } else {
                    Log.w("CategoryRepo", "No valid categories to sync")
                }
            } catch (e: Exception) {
                Log.e("CategoryRepo", "Sync failed: ${e.message}", e)
            }
        } ?: Log.e("CategoryRepo", "Cannot sync: User ID is null")
    }

}