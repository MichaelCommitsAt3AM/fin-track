package com.fintrack.app.presentation.settings.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.app.core.domain.repository.AuthRepository
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManageSignInMethodsViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignInMethodsUiState())
    val uiState: StateFlow<SignInMethodsUiState> = _uiState.asStateFlow()

    init {
        loadCurrentUserAuthMethods()
    }

    private fun loadCurrentUserAuthMethods() {
        val user = authRepository.getCurrentUser()

        if (user != null) {
            val providers = user.providerData.map { it.providerId }

            _uiState.update { currentState ->
                currentState.copy(
                    email = user.email ?: "No email linked",
                    isEmailPasswordLinked = providers.contains("password"),
                    isGoogleLinked = providers.contains("google.com"),
                    isAppleLinked = providers.contains("apple.com")
                )
            }
        }
    }

    /**
     * Attempts to link the Google credential to the existing account.
     * * @param idToken The ID token returned from Google Sign-In.
     * @param googleEmail The email address associated with the Google account.
     */
    fun linkGoogle(idToken: String, googleEmail: String?) {
        viewModelScope.launch {
            val currentPrimaryEmail = _uiState.value.email

            // 1. Strict Check: Ensure Google email matches the primary account email
            if (googleEmail == null || !googleEmail.equals(currentPrimaryEmail, ignoreCase = true)) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "This Google account is different from your primary email. To use a different account, please log out and change your email on the account settings screen."
                    )
                }
                return@launch
            }

            // 2. Set loading state
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // 3. Create Credential and Attempt Link
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = authRepository.linkWithCredential(credential)

            // 4. Handle Result
            if (result.user != null) {
                loadCurrentUserAuthMethods()
                _uiState.update { it.copy(isLoading = false, errorMessage = null) }
            } else {
                // Pass through repository errors (e.g., Collision exception message)
                _uiState.update { it.copy(isLoading = false, errorMessage = result.error) }
            }
        }
    }

    fun unlinkGoogle() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = authRepository.unlinkProvider("google.com")

            if (result.user != null) {
                loadCurrentUserAuthMethods()
                _uiState.update { it.copy(isLoading = false, errorMessage = null) }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = result.error) }
            }
        }
    }

    // Placeholder for future Apple implementation
    fun linkApple() {
        // Future implementation
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

data class SignInMethodsUiState(
    val email: String = "",
    val isEmailPasswordLinked: Boolean = false,
    val isGoogleLinked: Boolean = false,
    val isAppleLinked: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)