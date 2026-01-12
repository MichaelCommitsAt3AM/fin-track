package com.example.fintrack.core.data.repository

import android.util.Log
import com.example.fintrack.core.domain.repository.AuthRepository
import com.example.fintrack.core.domain.repository.AuthResult
import com.example.fintrack.core.domain.repository.EmailCheckResult
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
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

    // ... (Existing signIn/signUp/verification methods remain unchanged) ...
    override suspend fun sendEmailVerification(): AuthResult {
        return try {
            val user = firebaseAuth.currentUser ?: return AuthResult(null, "User not logged in")
            if (user.isEmailVerified) return AuthResult(user, "Email already verified")
            user.reload().await()
            user.sendEmailVerification().await()
            AuthResult(user, null)
        } catch (e: Exception) {
            AuthResult(null, e.message)
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): AuthResult {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            AuthResult(null, null)
        } catch (e: Exception) {
            AuthResult(null, e.message)
        }
    }

    override suspend fun signInWithEmail(email: String, password: String): AuthResult {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            AuthResult(result.user)
        } catch (e: Exception) {
            AuthResult(null, e.message)
        }
    }

    override suspend fun signUpWithEmail(email: String, password: String): AuthResult {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            AuthResult(result.user)
        } catch (e: Exception) {
            AuthResult(null, e.message)
        }
    }

    override suspend fun signInWithGoogle(credential: AuthCredential): AuthResult {
        return try {
            Log.d("AuthRepository", "signInWithGoogle: Starting Firebase authentication with Google credential")
            val result = firebaseAuth.signInWithCredential(credential).await()
            Log.d("AuthRepository", "signInWithGoogle: Firebase auth successful. User: ${result.user?.email}, UID: ${result.user?.uid}")
            AuthResult(result.user)
        } catch (e: Exception) {
            Log.e("AuthRepository", "signInWithGoogle: Firebase auth failed", e)
            AuthResult(null, e.message)
        }
    }

    // --- NEW IMPLEMENTATION ---
    override suspend fun linkWithCredential(credential: AuthCredential): AuthResult {
        return try {
            val user = firebaseAuth.currentUser
                ?: return AuthResult(null, "No user logged in to link.")

            val result = user.linkWithCredential(credential).await()
            Log.d("AuthRepository", "Account linked successfully: ${result.user?.email}")
            AuthResult(result.user)
        } catch (e: FirebaseAuthUserCollisionException) {
            // Specific handling for when the Google account is already used by another user
            Log.e("AuthRepository", "Link collision", e)
            AuthResult(null, "This Google account is already linked to another FinTrack account.")
        } catch (e: Exception) {
            Log.e("AuthRepository", "Link failed", e)
            AuthResult(null, e.message ?: "Failed to link account.")
        }
    }

    override suspend fun unlinkProvider(providerId: String): AuthResult {
        return try {
            val user = firebaseAuth.currentUser
                ?: return AuthResult(null, "No user logged in.")

            val result = user.unlink(providerId).await()
            Log.d("AuthRepository", "Unlinked provider: $providerId")
            AuthResult(result.user)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Unlink failed", e)
            AuthResult(null, e.message ?: "Failed to unlink account.")
        }
    }
    // --------------------------

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