package com.example.fintrack.presentation.profile_setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fintrack.core.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileSetupUiState(
    val fullName: String = "",
    val avatarUrl: String = "",
    val isLoading: Boolean = false,
    val isGoogleSignIn: Boolean = false
)

sealed class ProfileSetupUiEvent {
    data class OnFullNameChange(val name: String) : ProfileSetupUiEvent()
    object OnCompleteSetup : ProfileSetupUiEvent()
    object OnSkip : ProfileSetupUiEvent()
}

sealed class ProfileSetupEvent {
    object NavigateToHome : ProfileSetupEvent()
    data class ShowError(val message: String) : ProfileSetupEvent()
    data class ShowSuccess(val message: String) : ProfileSetupEvent()
}

@HiltViewModel
class ProfileSetupViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileSetupUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventChannel = Channel<ProfileSetupEvent>()
    val events = _eventChannel.receiveAsFlow()

    init {
        loadUserInfo()
    }

    private fun loadUserInfo() {
        viewModelScope.launch {
            val currentUser = firebaseAuth.currentUser
            if (currentUser != null) {
                val isGoogleSignIn = currentUser.providerData.any {
                    it.providerId == "google.com"
                }

                // Prefill with Google name or "John Doe"
                val defaultName = if (isGoogleSignIn) {
                    currentUser.displayName ?: "John Doe"
                } else {
                    "John Doe"
                }

                _uiState.value = _uiState.value.copy(
                    fullName = defaultName,
                    avatarUrl = currentUser.photoUrl?.toString() ?: "",
                    isGoogleSignIn = isGoogleSignIn
                )
            }
        }
    }

    fun onEvent(event: ProfileSetupUiEvent) {
        when (event) {
            is ProfileSetupUiEvent.OnFullNameChange -> {
                _uiState.value = _uiState.value.copy(fullName = event.name)
            }
            is ProfileSetupUiEvent.OnCompleteSetup -> completeSetup()
            is ProfileSetupUiEvent.OnSkip -> skipSetup()
        }
    }

    private fun completeSetup() {
        val state = _uiState.value

        if (state.fullName.isBlank()) {
            viewModelScope.launch {
                _eventChannel.send(ProfileSetupEvent.ShowError("Please enter your name"))
            }
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            try {
                // Get current user from database
                val currentUser = userRepository.getCurrentUserOnce()

                if (currentUser != null) {
                    // Update the user's full name
                    val updatedUser = currentUser.copy(
                        fullName = state.fullName,
                        updatedAt = System.currentTimeMillis()
                    )
                    userRepository.updateUser(updatedUser)

                    _eventChannel.send(ProfileSetupEvent.ShowSuccess("Profile updated!"))
                    _eventChannel.send(ProfileSetupEvent.NavigateToHome)
                } else {
                    _eventChannel.send(ProfileSetupEvent.ShowError("User not found"))
                }

                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                _eventChannel.send(ProfileSetupEvent.ShowError("Failed to update profile: ${e.message}"))
            }
        }
    }

    private fun skipSetup() {
        viewModelScope.launch {
            _eventChannel.send(ProfileSetupEvent.NavigateToHome)
        }
    }
}
