package com.fintrack.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.app.core.data.local.LocalAuthManager
import com.fintrack.app.core.domain.repository.NetworkRepository
import com.fintrack.app.core.profile.ProfileManager
import com.fintrack.app.presentation.home.CurrentUserObserver
import com.fintrack.app.presentation.home.UserUiModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val networkRepository: NetworkRepository,
    private val auth: FirebaseAuth,
    private val currentUserObserver: CurrentUserObserver,
    private val localAuthManager: LocalAuthManager,
    private val profileManager: ProfileManager
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

    // User data for the drawer: live Firestore profile with cloud sync on, on-device profile otherwise
    val currentUser: StateFlow<UserUiModel?> = currentUserObserver.observe()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // App Lock State
    // Controlled by MainActivity based on background activity time
    private val _isAppLocked = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    fun setAppLocked(locked: Boolean) {
        _isAppLocked.value = locked
    }

    /**
     * "Forgot PIN?" on the lock screen. The user can't prove who they are locally, so end the
     * Firebase session and wipe the local PIN/biometric settings *before* lifting the lock.
     * The app then starts at Login, and the user must re-authenticate with their account
     * password and set a new PIN. Simply unlocking here would hand anyone holding the phone
     * full access to the still-signed-in account.
     */
    fun onForgotPin() {
        viewModelScope.launch {
            auth.signOut()
            profileManager.signOutToFreshState()
            localAuthManager.clearLocalAuth()
            _isAppLocked.value = false
        }
    }
}
