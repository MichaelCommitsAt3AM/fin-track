package com.example.fintrack.core.data.repository

import android.util.Log
import com.example.fintrack.core.data.local.dao.UserDao
import com.example.fintrack.core.data.mapper.toDomain
import com.example.fintrack.core.data.mapper.toEntity
import com.example.fintrack.core.domain.model.User
import com.example.fintrack.core.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : UserRepository {

    private fun getUserId(): String? = firebaseAuth.currentUser?.uid

    private fun getUserDocument(userId: String) =
        firestore.collection("users").document(userId)

    override suspend fun createUser(user: User) {
        // 1. Save locally
        userDao.insertUser(user.toEntity())

        // 2. Save to Firestore
        try {
            val userMap = hashMapOf(
                "userId" to user.userId,
                "fullName" to user.fullName,
                "email" to user.email,
                "phoneNumber" to user.phoneNumber,
                "avatarId" to user.avatarId,
                "createdAt" to user.createdAt,
                "updatedAt" to user.updatedAt
            )

            getUserDocument(user.userId)
                .set(userMap)
                .await()

            Log.d("UserRepo", "User created: ${user.userId}")
        } catch (e: Exception) {
            Log.e("UserRepo", "Error creating user in Firestore: ${e.message}")
        }
    }

    override suspend fun updateUser(user: User) {
        // 1. Update locally
        userDao.updateUser(user.toEntity())

        // 2. Update in Firestore
        try {
            val updateMap = hashMapOf(
                "fullName" to user.fullName,
                "phoneNumber" to user.phoneNumber,
                "avatarId" to user.avatarId,
                "updatedAt" to System.currentTimeMillis()
            )

            getUserDocument(user.userId)
                .update(updateMap as Map<String, Any>)
                .await()

            Log.d("UserRepo", "User updated: ${user.userId}")
        } catch (e: Exception) {
            Log.e("UserRepo", "Error updating user in Firestore: ${e.message}")
        }
    }

    override fun getCurrentUser(): Flow<User?> {
        val userId = getUserId()
        return if (userId != null) {
            userDao.getUserById(userId).map { it?.toDomain() }
        } else {
            kotlinx.coroutines.flow.flowOf(null)
        }
    }

    override suspend fun getCurrentUserOnce(): User? {
        val userId = getUserId() ?: return null
        return userDao.getUserByIdOnce(userId)?.toDomain()
    }

    override suspend fun syncUserFromCloud() {
        val userId = getUserId()
        if (userId != null) {
            try {
                Log.d("UserRepo", "Syncing user from cloud: $userId")

                val userDoc = getUserDocument(userId).get().await()

                if (userDoc.exists()) {
                    val user = User(
                        userId = userDoc.getString("userId") ?: userId,
                        fullName = userDoc.getString("fullName") ?: "",
                        email = userDoc.getString("email") ?: "",
                        phoneNumber = userDoc.getString("phoneNumber") ?: "",
                        avatarId = userDoc.getLong("avatarId")?.toInt() ?: 1,
                        createdAt = userDoc.getLong("createdAt") ?: System.currentTimeMillis(),
                        updatedAt = userDoc.getLong("updatedAt") ?: System.currentTimeMillis()
                    )

                    userDao.insertUser(user.toEntity())
                    Log.d("UserRepo", "User synced successfully")
                } else {
                    Log.w("UserRepo", "User document does not exist in Firestore")
                }
            } catch (e: Exception) {
                Log.e("UserRepo", "Error syncing user: ${e.message}")
            }
        } else {
            Log.e("UserRepo", "Cannot sync: User ID is null")
        }
    }

    override suspend fun deleteUser(userId: String) {
        // 1. Delete locally
        userDao.deleteUser(userId)

        // 2. Delete from Firestore
        try {
            getUserDocument(userId).delete().await()
            Log.d("UserRepo", "User deleted: $userId")
        } catch (e: Exception) {
            Log.e("UserRepo", "Error deleting user: ${e.message}")
        }
    }
}
