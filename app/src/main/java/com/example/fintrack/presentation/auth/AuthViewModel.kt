package com.example.fintrack.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fintrack.domain.repository.AuthRepository
import com.example.fintrack.domain.repository.AuthResult
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// Enum to manage the registration steps
enum class RegistrationStep {
    Email,
    Password
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    // Holds the current user (null if logged out)
    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    // Holds the state of the UI (is it loading? is there an error?)
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // State for the registration step
    private val _registrationStep = MutableStateFlow(RegistrationStep.Email)
    val registrationStep: StateFlow<RegistrationStep> = _registrationStep.asStateFlow()

    // One-time events (like "Navigate to Home" or "Show Toast")
    private val _authEventChannel = Channel<AuthEvent>()
    val authEvent = _authEventChannel.receiveAsFlow()

    init {
        // Check if user is already logged in when app starts
        viewModelScope.launch {
            repository.getAuthState().collect { user ->
                _currentUser.value = user
                if (user != null) {
                    _authEventChannel.send(AuthEvent.NavigateToHome)
                }
            }
        }
    }

    fun onEvent(event: AuthUiEvent) {
        when (event) {
            is AuthUiEvent.EmailChanged -> { // Renamed
                _uiState.value = _uiState.value.copy(email = event.value, error = null)
            }
            is AuthUiEvent.PasswordChanged -> { // Renamed
                _uiState.value = _uiState.value.copy(password = event.value, error = null)
            }
            is AuthUiEvent.ConfirmPasswordChanged -> { // New
                _uiState.value = _uiState.value.copy(confirmPassword = event.value, error = null)
            }
            is AuthUiEvent.CheckEmail -> { // New
                checkEmail()
            }
            is AuthUiEvent.GoBackToEmailStep -> { // New
                _registrationStep.value = RegistrationStep.Email
                _uiState.value = _uiState.value.copy(password = "", confirmPassword = "", error = null)
            }
            is AuthUiEvent.SignUp -> {
                signUp()
            }
            is AuthUiEvent.SignIn -> {
                signIn()
            }
            is AuthUiEvent.SignInWithGoogle -> {
                signInGoogle(event.credential)
            }
        }
    }

    private fun checkEmail() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val email = _uiState.value.email
            if (email.isBlank()) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Email cannot be empty")
                return@launch
            }

            val result = repository.checkEmailExists(email)
            if (result.exists) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Email already in use. Please log in.")
            } else if (result.error != null) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = result.error)
            } else {
                // Success! Email is available.
                _uiState.value = _uiState.value.copy(isLoading = false)
                _registrationStep.value = RegistrationStep.Password
            }
        }
    }

    private fun signUp() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val email = _uiState.value.email
            val password = _uiState.value.password
            val confirmPassword = _uiState.value.confirmPassword


            if (email.isBlank() || password.isBlank()) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Fields cannot be empty")
                return@launch
            }

            if (password != confirmPassword) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Passwords do not match")
                return@launch
            }

            val result = repository.signUpWithEmail(email, password)
            handleAuthResult(result)
        }
    }

    private fun signIn() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val email = _uiState.value.email
            val password = _uiState.value.password

            val result = repository.signInWithEmail(email, password)
            handleAuthResult(result)
        }
    }

    private fun signInGoogle(credential: AuthCredential) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = repository.signInWithGoogle(credential)
            handleAuthResult(result)
        }
    }

    private suspend fun handleAuthResult(result: AuthResult) {
        _uiState.value = _uiState.value.copy(isLoading = false)
        if (result.user != null) {
            _authEventChannel.send(AuthEvent.NavigateToHome)
        } else {
            _uiState.value = _uiState.value.copy(error = result.error ?: "Authentication failed")
        }
    }
}

// --- Helper Classes for State Management ---

data class AuthUiState(
    val isLoading: Boolean = false,
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val error: String? = null
)

sealed class AuthUiEvent {
    data class EmailChanged(val value: String) : AuthUiEvent()
    data class PasswordChanged(val value: String) : AuthUiEvent()
    data class ConfirmPasswordChanged(val value: String) : AuthUiEvent()
    data class SignInWithGoogle(val credential: AuthCredential) : AuthUiEvent()
    object CheckEmail : AuthUiEvent()
    object GoBackToEmailStep : AuthUiEvent()
    object SignIn : AuthUiEvent()
    object SignUp : AuthUiEvent()
}

sealed class AuthEvent {
    object NavigateToHome : AuthEvent()
}