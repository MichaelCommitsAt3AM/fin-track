package com.fintrack.app.presentation.home

import com.fintrack.app.core.domain.repository.UserRepository
import com.fintrack.app.core.profile.AccountMode
import com.fintrack.app.core.profile.ProfileManager
import com.fintrack.app.core.profile.SyncGate
import com.fintrack.app.core.security.EncryptionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The signed-in user's name/avatar for the drawer and Home. With cloud sync on it follows the
 * live Firestore document (as before); otherwise it reads the profile stored on the phone.
 */
@Singleton
class CurrentUserObserver @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val encryptionManager: EncryptionManager,
    private val profileManager: ProfileManager,
    private val syncGate: SyncGate,
    private val userRepository: UserRepository
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observe(): Flow<UserUiModel?> =
        profileManager.state
            .map { it.mode }
            .distinctUntilChanged()
            .flatMapLatest { mode ->
                if (mode == AccountMode.NONE) {
                    flowOf(null)
                } else {
                    syncGate.canSync.flatMapLatest { canSync ->
                        val cloudUid = syncGate.cloudUidOrNull()
                        if (canSync && cloudUid != null) cloudUser(cloudUid) else localUser()
                    }
                }
            }

    private fun localUser(): Flow<UserUiModel?> =
        userRepository.getCurrentUser().map { user ->
            UserUiModel(
                fullName = user?.fullName?.takeIf { it.isNotBlank() } ?: auth.currentUser?.displayName ?: "User",
                email = auth.currentUser?.email ?: user?.email.orEmpty(),
                avatarId = user?.avatarId ?: 1
            )
        }

    private fun cloudUser(cloudUid: String): Flow<UserUiModel?> = callbackFlow {
        val user = auth.currentUser
        if (user == null) {
            trySend(null)
            awaitClose {}
            return@callbackFlow
        }
        val listener = firestore.collection("users").document(cloudUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    // Launch coroutine to handle suspending decryption
                    launch {
                        val avatarId = snapshot.getLong("avatarId")?.toInt() ?: 1
                        val encryptedName = snapshot.getString("fullName") ?: user.displayName ?: "User"
                        val fullName = try {
                            encryptionManager.decrypt(encryptedName)
                        } catch (e: Exception) {
                            encryptedName
                        }
                        trySend(UserUiModel(fullName = fullName, email = user.email ?: "", avatarId = avatarId))
                    }
                } else {
                    trySend(
                        UserUiModel(
                            fullName = user.displayName ?: "User",
                            email = user.email ?: "",
                            avatarId = 1
                        )
                    )
                }
            }
        awaitClose { listener.remove() }
    }
}
