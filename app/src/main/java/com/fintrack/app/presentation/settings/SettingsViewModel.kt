package com.fintrack.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.app.core.data.local.LocalAuthManager
import com.fintrack.app.core.domain.model.Currency
import com.fintrack.app.core.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
        private val authRepository: AuthRepository,
        private val localAuthManager: LocalAuthManager,
        private val profileManager: com.fintrack.app.core.profile.ProfileManager,
        currentUserObserver: com.fintrack.app.presentation.home.CurrentUserObserver
) : ViewModel() {

    val profileState = profileManager.state

    val currentUser = currentUserObserver.observe().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
    )

    private val _settingsEventChannel = Channel<SettingsEvent>()
    val settingsEvent = _settingsEventChannel.receiveAsFlow()

    // Observe the local preference for Biometric Auth
    val isBiometricEnabled =
            localAuthManager.isBiometricEnabled.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = false
            )

    val themePreference =
            localAuthManager.themePreference.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = "Dark"
            )

    val currencyPreference =
            localAuthManager.currencyPreference.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = Currency.KSH
            )

    fun onLogout() {
        viewModelScope.launch {
            // Sign out from Firebase first
            authRepository.signOut()
            profileManager.signOutToFreshState()
            
            // Clear local authentication (PIN and biometric settings)
            localAuthManager.clearLocalAuth()

            // Navigate to login - MainActivity will handle the restart
            _settingsEventChannel.send(SettingsEvent.NavigateToLogin)
        }
    }

    fun disableBiometric() {
        viewModelScope.launch { localAuthManager.setBiometricEnabled(false) }
    }

    fun setThemePreference(theme: String) {
        viewModelScope.launch { localAuthManager.setThemePreference(theme) }
    }

    fun setCurrencyPreference(currency: Currency) {
        viewModelScope.launch { localAuthManager.setCurrencyPreference(currency) }
    }
}

sealed class SettingsEvent {
    object NavigateToLogin : SettingsEvent()
}
