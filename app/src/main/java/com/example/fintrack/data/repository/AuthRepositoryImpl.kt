package com.example.fintrack.data.repository

import com.example.fintrack.domain.repository.AuthRepository
import com.example.fintrack.domain.repository.AuthResult
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
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
    // ----------------------------------------

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
            AuthResult(user = result.user)
        } catch (e: Exception) {
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

    override fun signOut() {
        firebaseAuth.signOut()
    }
}