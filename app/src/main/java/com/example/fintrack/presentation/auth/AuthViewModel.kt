package com.example.fintrack.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fintrack.core.domain.repository.AuthRepository
import com.example.fintrack.core.domain.repository.AuthResult
import com.example.fintrack.core.domain.repository.CategoryRepository
import com.example.fintrack.util.EmailVerificationRateLimiter // Assuming util is now in core
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

// Enum to manage the registration steps
enum class RegistrationStep {
    Email,
    Password
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val categoryRepository: CategoryRepository
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

    // Rate limiter for email verification
    private val emailVerificationRateLimiter = EmailVerificationRateLimiter()

    // Expose rate limiter state
    val verificationAttemptsRemaining: StateFlow<Int> = MutableStateFlow(5).apply {
        viewModelScope.launch {
            emailVerificationRateLimiter.attemptCount.collect { attempts ->
                value = emailVerificationRateLimiter.getRemainingAttempts()
            }
        }
    }

    val verificationCooldownSeconds: StateFlow<Long> = MutableStateFlow(0L).apply {
        viewModelScope.launch {
            while (true) {
                value = emailVerificationRateLimiter.getCooldownSeconds()
                if (value > 0) {
                    delay(1000) // Update every second
                } else {
                    delay(100) // Check more frequently when not in cooldown
                }
            }
        }
    }

    init {
        // Only observe auth state, don't trigger navigation
        viewModelScope.launch {
            repository.getAuthState().collect { user ->
                _currentUser.value = user
            }
        }
    }

    fun checkAuthStatus() {
        viewModelScope.launch {
            val user = _currentUser.value
            if (user != null) {
                if (user.isEmailVerified) {
                    _authEventChannel.send(AuthEvent.NavigateToHome)
                } else {
                    _authEventChannel.send(AuthEvent.NavigateToEmailVerification)
                }
            }
        }
    }

    fun onEvent(event: AuthUiEvent) {
        when (event) {
            is AuthUiEvent.EmailChanged -> {
                _uiState.value = _uiState.value.copy(email = event.value, error = null)
            }
            is AuthUiEvent.PasswordChanged -> {
                _uiState.value = _uiState.value.copy(password = event.value, error = null)
            }
            is AuthUiEvent.ConfirmPasswordChanged -> {
                _uiState.value = _uiState.value.copy(confirmPassword = event.value, error = null)
            }
            is AuthUiEvent.CheckEmail -> {
                checkEmail()
            }
            is AuthUiEvent.GoBackToEmailStep -> {
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
            is AuthUiEvent.SendPasswordReset -> {
                sendPasswordReset(event.email)
            }
            is AuthUiEvent.ResendVerificationEmail -> {
                sendVerificationEmail()
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

            // Create the account
            val result = repository.signUpWithEmail(email, password)

            if (result.user != null) {

                // Initialize default categories for the new user
                categoryRepository.initDefaultCategories()

                // Send verification email and check for errors
                val verificationResult = repository.sendEmailVerification()

                if (verificationResult.error == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isEmailVerificationSent = true
                    )
                    _authEventChannel.send(AuthEvent.NavigateToEmailVerification)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Account created but failed to send verification email: ${verificationResult.error}"
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false, error = result.error)
            }
        }
    }

    private fun signIn() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val email = _uiState.value.email
            val password = _uiState.value.password

            val result = repository.signInWithEmail(email, password)

            if (result.user != null) {
                if (result.user.isEmailVerified) {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    _authEventChannel.send(AuthEvent.NavigateToHome)
                } else {
                    repository.signOut()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        showEmailVerificationDialog = true,
                        error = "Please verify your email address."
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false, error = result.error ?: "Authentication failed")
            }
        }
    }

    private fun sendVerificationEmail() {
        viewModelScope.launch {
            if (!emailVerificationRateLimiter.canSendEmail()) {
                val cooldownSeconds = emailVerificationRateLimiter.getCooldownSeconds()
                val remainingAttempts = emailVerificationRateLimiter.getRemainingAttempts()

                val errorMessage = when {
                    remainingAttempts == 0 -> "Maximum attempts reached. Please try again later."
                    cooldownSeconds > 0 -> "Please wait ${cooldownSeconds} seconds before resending."
                    else -> "Unable to send email. Please try again."
                }

                _uiState.value = _uiState.value.copy(isLoading = false, error = errorMessage)
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = repository.sendEmailVerification()

            if (result.error == null) {
                emailVerificationRateLimiter.recordAttempt()
                val remainingAttempts = emailVerificationRateLimiter.getRemainingAttempts()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Verification email sent! ($remainingAttempts attempts remaining)"
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Failed to send email: ${result.error}")
            }
        }
    }

    // --- THIS IS THE MISSING FUNCTION YOU NEEDED ---
    fun resetRateLimiter() {
        emailVerificationRateLimiter.reset()
    }
    // -----------------------------------------------

    fun checkEmailVerification() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val user = repository.getCurrentUser()

            user?.reload()?.await()

            if (user?.isEmailVerified == true) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                _authEventChannel.send(AuthEvent.NavigateToHome)
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Email not verified yet.")
            }
        }
    }

    private fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, isPasswordResetSent = false)
            if (email.isBlank()) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Please enter your email")
                return@launch
            }

            val result = repository.sendPasswordResetEmail(email)

            if (result.error == null) {
                _uiState.value = _uiState.value.copy(isLoading = false, isPasswordResetSent = true)
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false, error = result.error)
            }
        }
    }

    fun dismissDialog() {
        _uiState.value = _uiState.value.copy(showEmailVerificationDialog = false)
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

    fun signOut() {
        repository.signOut()
    }
}

// --- Helper Classes for State Management ---

data class AuthUiState(
    val isLoading: Boolean = false,
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isEmailVerificationSent: Boolean = false,
    val isPasswordResetSent: Boolean = false,
    val showEmailVerificationDialog: Boolean = false,
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
    data class SendPasswordReset(val email: String) : AuthUiEvent()
    object ResendVerificationEmail : AuthUiEvent()
}

sealed class AuthEvent {
    object NavigateToHome : AuthEvent()
    object NavigateToLogin : AuthEvent()
    object NavigateToEmailVerification : AuthEvent()
}