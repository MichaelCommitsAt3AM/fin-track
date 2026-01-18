package com.example.fintrack.core.data.repository

import android.util.Log
import com.example.fintrack.core.data.local.dao.CategoryDao
import com.example.fintrack.core.data.mapper.toDomain
import com.example.fintrack.core.data.mapper.toEntity
import com.example.fintrack.core.domain.model.Category
import com.example.fintrack.core.domain.repository.CategoryRepository
import com.example.fintrack.core.domain.repository.NetworkRepository
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
    private val firebaseAuth: FirebaseAuth,
    private val networkRepository: NetworkRepository
) : CategoryRepository {

    private fun getUserId(): String? = firebaseAuth.currentUser?.uid

    private fun getUserCategoriesCollection() =
        firestore.collection("users")
            .document(getUserId() ?: "unknown_user")
            .collection("categories")

    // --- Initialize Default categories ---
    override suspend fun initDefaultCategories() {
        val userId = getUserId() ?: throw IllegalStateException("User is not logged in")

        val defaults = listOf(
            Category("Food",userId = userId, "restaurant", "#F39C12", CategoryType.EXPENSE, isDefault = true),
            Category("Rent", userId = userId, "house", "#8B5CF6", CategoryType.EXPENSE, isDefault = true),
            Category("Transport", userId = userId, "directions_bus", "#3498DB", CategoryType.EXPENSE, isDefault = true)
        )
        // Insert locally and to cloud
        insertAllCategories(defaults)
    }


    override suspend fun insertCategory(category: Category) {
        val userId = getUserId() ?: throw IllegalStateException("User is not logged in")
        val entity = category.toEntity()

        // OFFLINE-FIRST: 1. Always save locally first with isSynced = false
        categoryDao.insertCategory(entity)
        Log.d("CategoryRepo", "Category saved locally: ${category.name}")

        // 2. If online, attempt to sync to Firestore immediately
        if (networkRepository.isNetworkAvailable()) {
            try {
                val firestoreData = hashMapOf(
                    "name" to entity.name,
                    "userId" to entity.userId,
                    "icon" to entity.iconName,
                    "color" to entity.colorHex,
                    "type" to entity.type,
                    "isDefault" to entity.isDefault
                )

                getUserCategoriesCollection()
                    .document(category.name)
                    .set(firestoreData)
                    .await()
                categoryDao.markAsSynced(category.name, userId)
                Log.d("CategoryRepo", "Category synced to Firestore: ${category.name}")
            } catch (e: Exception) {
                Log.e("CategoryRepo", "Error syncing to Firestore (will retry later): ${e.message}")
            }
        } else {
            Log.d("CategoryRepo", "Offline - category will sync when online")
        }
    }


    // --- Delete Category ---
    override suspend fun deleteCategory(category: Category) {
        val userId = getUserId() ?: return

        // 1. Delete locally
        categoryDao.deleteCategory(category.name, userId)

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
        val userId = getUserId() ?: throw IllegalStateException("User is not logged in")
        
        // 1. Save locally
        categoryDao.insertAll(categories.map { it.toEntity() })
        Log.d("CategoryRepo", "Saved ${categories.size} categories locally")

        // 2. Save to cloud (Batch writing) if online
        if (networkRepository.isNetworkAvailable()) {
            try {
                val batch = firestore.batch()
                val collectionRef = getUserCategoriesCollection()

                categories.forEach { category ->
                    val docRef = collectionRef.document(category.name)
                    val categoryData = hashMapOf(
                        "name" to category.name,
                        "userId" to category.userId,
                        "icon" to category.iconName,
                        "color" to category.colorHex,
                        "type" to category.type.name,
                        "isDefault" to category.isDefault
                    )
                    batch.set(docRef, categoryData)
                }
                batch.commit().await()
                
                // Mark all as synced
                categories.forEach { category ->
                    categoryDao.markAsSynced(category.name, userId)
                }
                Log.d("CategoryRepo", "Categories synced to Firestore")
            } catch (e: Exception) {
                Log.e("CategoryRepo", "Error saving categories to cloud (will retry later): ${e.message}")
            }
        } else {
            Log.d("CategoryRepo", "Offline - categories will sync when online")
        }
    }

    override fun getAllCategories(): Flow<List<Category>> {
        val userId = getUserId() ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return categoryDao.getAllCategories(userId).map { entityList ->
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
                            userId = userId,
                            iconName = doc.getString("icon") ?: doc.getString("iconName") ?: "",
                            colorHex = doc.getString("color") ?: doc.getString("colorHex") ?: "",
                            type = doc.getString("type") ?: "",
                            isDefault = doc.getBoolean("isDefault") ?: false,
                            isSynced = true // Mark as synced since it's from cloud
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

    override suspend fun syncUnsyncedCategories() {
        val userId = getUserId() ?: run {
            Log.e("CategoryRepo", "Cannot sync: User not logged in")
            return
        }

        if (!networkRepository.isNetworkAvailable()) {
            Log.d("CategoryRepo", "Network unavailable, skipping category sync")
            return
        }

        try {
            val unsyncedCategories = categoryDao.getUnsyncedCategories(userId)
            Log.d("CategoryRepo", "Found ${unsyncedCategories.size} unsynced categories")

            unsyncedCategories.forEach { entity ->
                try {
                    val categoryData = hashMapOf(
                        "name" to entity.name,
                        "userId" to entity.userId,
                        "icon" to entity.iconName,
                        "color" to entity.colorHex,
                        "type" to entity.type,
                        "isDefault" to entity.isDefault
                    )
                    
                    getUserCategoriesCollection()
                        .document(entity.name)
                        .set(categoryData)
                        .await()
                    categoryDao.markAsSynced(entity.name, userId)
                    Log.d("CategoryRepo", "Synced category: ${entity.name}")
                } catch (e: Exception) {
                    Log.e("CategoryRepo", "Failed to sync category ${entity.name}: ${e.message}")
                }
            }

            Log.d("CategoryRepo", "Category sync completed")
        } catch (e: Exception) {
            Log.e("CategoryRepo", "Error during category sync: ${e.message}", e)
        }
    }

}