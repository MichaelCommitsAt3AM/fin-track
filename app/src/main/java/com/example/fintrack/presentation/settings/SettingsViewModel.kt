package com.example.fintrack.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fintrack.core.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _settingsEventChannel = Channel<SettingsEvent>()
    val settingsEvent = _settingsEventChannel.receiveAsFlow()

    fun onLogout() {
        authRepository.signOut()
        viewModelScope.launch {
            _settingsEventChannel.send(SettingsEvent.NavigateToLogin)
        }
    }
}

sealed class SettingsEvent {
    object NavigateToLogin : SettingsEvent()
}