package com.example.fintrack.core.domain.repository

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

// Using a simple data class for the auth result
data class AuthResult(
    val user: FirebaseUser?,
    val error: String? = null
)

data class EmailCheckResult(
    val exists: Boolean,
    val error: String? = null
)

// The "contract" for handling authentication
interface AuthRepository {

    // Get the currently logged-in user, or null if logged out
    fun getCurrentUser(): FirebaseUser?

    // A flow to observe auth state changes (login/logout)
    fun getAuthState(): Flow<FirebaseUser?>

    // Sign in with email and password
    suspend fun signInWithEmail(email: String, password: String): AuthResult

    // Create a new account with email and password
    suspend fun signUpWithEmail(email: String, password: String): AuthResult

    // Sign in using a Google credential
    suspend fun signInWithGoogle(credential: AuthCredential): AuthResult

    suspend fun checkEmailExists(email: String): EmailCheckResult

    suspend fun sendEmailVerification(): AuthResult
    suspend fun sendPasswordResetEmail(email: String): AuthResult

    // Sign out the current user
    fun signOut()
}