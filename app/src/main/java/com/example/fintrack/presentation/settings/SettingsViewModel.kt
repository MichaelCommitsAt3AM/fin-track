package com.example.fintrack.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fintrack.core.data.local.LocalAuthManager
import com.example.fintrack.core.domain.model.Currency
import com.example.fintrack.core.domain.repository.AuthRepository
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
        private val localAuthManager: LocalAuthManager
) : ViewModel() {

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
            // Clear local authentication (PIN and biometric settings)
            localAuthManager.clearLocalAuth()

            // Sign out from Firebase
            authRepository.signOut()

            // Navigate to login
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
