package com.example.fintrack.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fintrack.core.data.local.LocalAuthManager
import com.example.fintrack.core.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val localAuthManager: LocalAuthManager
) : ViewModel() {

    private val _settingsEventChannel = Channel<SettingsEvent>()
    val settingsEvent = _settingsEventChannel.receiveAsFlow()

    // Observe the local preference for Biometric Auth
    val isBiometricEnabled = localAuthManager.isBiometricEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val themePreference = localAuthManager.themePreference
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "Dark"
        )

    fun onLogout() {
        authRepository.signOut()
        viewModelScope.launch {
            _settingsEventChannel.send(SettingsEvent.NavigateToLogin)
        }
    }

    fun disableBiometric() {
        viewModelScope.launch {
            localAuthManager.setBiometricEnabled(false)
        }
    }

    fun setThemePreference(theme: String) {
        viewModelScope.launch {
            localAuthManager.setThemePreference(theme)
        }
    }
}

sealed class SettingsEvent {
    object NavigateToLogin : SettingsEvent()
}