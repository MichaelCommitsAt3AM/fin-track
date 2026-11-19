package com.fintrack.app.core.data.repository

import android.util.Log
import com.fintrack.app.core.data.local.dao.UserDao
import com.fintrack.app.core.data.mapper.toDomain
import com.fintrack.app.core.data.mapper.toEntity
import com.fintrack.app.core.domain.model.User
import com.fintrack.app.core.domain.repository.UserRepository
import com.fintrack.app.core.profile.ProfileManager
import com.fintrack.app.core.profile.SyncGate
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(

    private val userDao: UserDao,
    private val profileManager: ProfileManager,
    private val syncGate: SyncGate,
    private val firestore: FirebaseFirestore,
    private val encryptionManager: com.fintrack.app.core.security.EncryptionManager
) : UserRepository {

    /** Owner of the Room rows (the local profile), not the cloud account. */
    private fun getUserId(): String? = profileManager.profileId()

    private fun cloudUid(): String? = syncGate.cloudUidOrNull()

    private fun getUserDocument(cloudUid: String) =
        firestore.collection("users").document(cloudUid)

    override suspend fun createUser(user: User) {
        // 1. Save locally
        userDao.insertUser(user.toEntity())

        // 2. Save to Firestore (only when sync is allowed)
        val cloudUid = cloudUid() ?: return
        try {
            val userMap = hashMapOf(
                "userId" to user.userId,
                "fullName" to encryptionManager.encrypt(user.fullName),
                "email" to encryptionManager.encrypt(user.email),
                "phoneNumber" to encryptionManager.encrypt(user.phoneNumber),
                "avatarId" to user.avatarId,
                "createdAt" to user.createdAt,
                "updatedAt" to user.updatedAt
            )

            getUserDocument(cloudUid)
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

        // 2. Update in Firestore (only when sync is allowed)
        val cloudUid = cloudUid() ?: return
        try {
            val updateMap = hashMapOf(
                "fullName" to encryptionManager.encrypt(user.fullName),
                "phoneNumber" to encryptionManager.encrypt(user.phoneNumber),
                "avatarId" to user.avatarId,
                "updatedAt" to System.currentTimeMillis()
            )

            getUserDocument(cloudUid)
                .set(updateMap as Map<String, Any>, com.google.firebase.firestore.SetOptions.merge())
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
        val cloudUid = cloudUid()
        if (userId != null && cloudUid != null) {
            try {
                Log.d("UserRepo", "Syncing user from cloud: $userId")

                val userDoc = getUserDocument(cloudUid).get().await()

                if (userDoc.exists()) {
                    // Decrypt fields
                    val fullName = userDoc.getString("fullName")?.let { encryptionManager.decrypt(it) } ?: ""
                    val email = userDoc.getString("email")?.let { encryptionManager.decrypt(it) } ?: ""
                    val phoneNumber = userDoc.getString("phoneNumber")?.let { encryptionManager.decrypt(it) } ?: ""
                    
                    val user = User(
                        userId = userId, // always the local profile, whatever the other phone stored
                        fullName = fullName,
                        email = email,
                        phoneNumber = phoneNumber,
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
            Log.d("UserRepo", "Cloud sync not enabled, skipping user sync")
        }
    }

    override suspend fun deleteUser(userId: String) {
        // 1. Delete locally
        userDao.deleteUser(userId)

        // 2. Delete from Firestore (only when sync is allowed)
        val cloudUid = cloudUid() ?: return
        try {
            getUserDocument(cloudUid).delete().await()
            Log.d("UserRepo", "User deleted: $userId")
        } catch (e: Exception) {
            Log.e("UserRepo", "Error deleting user: ${e.message}")
        }
    }
}
