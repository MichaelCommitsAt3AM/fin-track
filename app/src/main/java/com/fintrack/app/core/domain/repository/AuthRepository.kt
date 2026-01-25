package com.fintrack.app.core.domain.repository

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

data class AuthResult(
    val user: FirebaseUser?,
    val error: String? = null
)

data class EmailCheckResult(
    val exists: Boolean,
    val error: String? = null
)

interface AuthRepository {
    fun getCurrentUser(): FirebaseUser?
    fun getAuthState(): Flow<FirebaseUser?>

    suspend fun signInWithEmail(email: String, password: String): AuthResult
    suspend fun signUpWithEmail(email: String, password: String): AuthResult
    suspend fun signInWithGoogle(credential: AuthCredential): AuthResult

    // --- NEW: Linking Methods ---
    suspend fun linkWithCredential(credential: AuthCredential): AuthResult
    suspend fun unlinkProvider(providerId: String): AuthResult
    // ----------------------------

    suspend fun checkEmailExists(email: String): EmailCheckResult
    suspend fun sendEmailVerification(): AuthResult
    suspend fun sendPasswordResetEmail(email: String): AuthResult
    fun signOut()
}