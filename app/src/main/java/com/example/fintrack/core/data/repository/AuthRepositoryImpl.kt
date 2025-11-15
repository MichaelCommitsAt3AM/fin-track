package com.example.fintrack.core.data.repository

import android.util.Log
import com.example.fintrack.core.domain.repository.AuthRepository
import com.example.fintrack.core.domain.repository.AuthResult
import com.example.fintrack.core.domain.repository.EmailCheckResult
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    override fun getCurrentUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }

    override fun getAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        firebaseAuth.addAuthStateListener(authStateListener)
        awaitClose {
            firebaseAuth.removeAuthStateListener(authStateListener)
        }
    }

    override suspend fun sendEmailVerification(): AuthResult {
        return try {
            val user = firebaseAuth.currentUser

            if (user == null) {
                Log.e("AuthRepository", "Cannot send verification: User is null")
                return AuthResult(user = null, error = "User not logged in")
            }

            if (user.isEmailVerified) {
                Log.d("AuthRepository", "Email already verified")
                return AuthResult(user = user, error = "Email already verified")
            }

            // Check Firebase metadata for last email sent time
            user.reload().await()

            // Send the verification email
            user.sendEmailVerification().await()
            Log.d("AuthRepository", "Verification email sent to: ${user.email}")

            AuthResult(user = user, error = null)
        } catch (e: FirebaseAuthException) {
            // Handle Firebase-specific rate limit errors
            when (e.errorCode) {
                "ERROR_TOO_MANY_REQUESTS" -> {
                    Log.e("AuthRepository", "Firebase rate limit exceeded")
                    AuthResult(user = null, error = "Too many requests. Please try again later.")
                }
                else -> {
                    Log.e("AuthRepository", "Failed to send verification email: ${e.errorCode}", e)
                    AuthResult(user = null, error = e.message ?: "Failed to send verification email")
                }
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to send verification email", e)
            AuthResult(user = null, error = e.message ?: "Failed to send verification email")
        }
    }


    override suspend fun sendPasswordResetEmail(email: String): AuthResult {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            AuthResult(user = null, error = null)
        } catch (e: Exception) {
            AuthResult(user = null, error = e.message)
        }
    }

    override suspend fun signInWithEmail(email: String, password: String): AuthResult {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            AuthResult(user = result.user)
        } catch (e: Exception) {
            AuthResult(user = null, error = e.message)
        }
    }

    override suspend fun signUpWithEmail(email: String, password: String): AuthResult {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            Log.d("AuthRepository", "User created: ${result.user?.email}")
            AuthResult(user = result.user)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Signup failed", e)
            AuthResult(user = null, error = e.message)
        }
    }

    override suspend fun signInWithGoogle(credential: AuthCredential): AuthResult {
        return try {
            val result = firebaseAuth.signInWithCredential(credential).await()
            AuthResult(user = result.user)
        } catch (e: Exception) {
            AuthResult(user = null, error = e.message)
        }
    }

    override suspend fun checkEmailExists(email: String): EmailCheckResult {
        return try {
            val methods = firebaseAuth.fetchSignInMethodsForEmail(email).await()
            val emailExists = methods.signInMethods?.isNotEmpty() ?: false
            EmailCheckResult(exists = emailExists)
        } catch (e: Exception) {
            EmailCheckResult(exists = false, error = e.message)
        }
    }

    override fun signOut() {
        firebaseAuth.signOut()
    }
}
