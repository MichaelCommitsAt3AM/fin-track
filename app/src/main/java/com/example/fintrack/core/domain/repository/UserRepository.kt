package com.example.fintrack.core.domain.repository

import com.example.fintrack.core.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {

    suspend fun createUser(user: User)

    suspend fun updateUser(user: User)

    fun getCurrentUser(): Flow<User?>

    suspend fun getCurrentUserOnce(): User?

    suspend fun syncUserFromCloud()

    suspend fun deleteUser(userId: String)
}
