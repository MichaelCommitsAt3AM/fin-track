package com.example.fintrack.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fintrack.core.domain.repository.NetworkRepository
import com.example.fintrack.presentation.home.UserUiModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val networkRepository: NetworkRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    /**
     * Network connectivity state exposed to the UI
     * true = online, false = offline
     */
    val isOnline: StateFlow<Boolean> = networkRepository.observeNetworkConnectivity()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    // Real-time user data from Firestore
    val currentUser: StateFlow<UserUiModel?> = callbackFlow {
        val user = auth.currentUser
        if (user != null) {
            val listener = firestore.collection("users").document(user.uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        val avatarId = snapshot.getLong("avatarId")?.toInt() ?: 1
                        val fullName = snapshot.getString("fullName") ?: user.displayName ?: "User"
                        val email = user.email ?: ""

                        trySend(
                            UserUiModel(
                                fullName = fullName,
                                email = email,
                                avatarId = avatarId
                            )
                        )
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
        } else {
            trySend(null)
            awaitClose {}
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
}
