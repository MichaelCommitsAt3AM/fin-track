package com.example.fintrack.presentation.settings.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fintrack.core.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetPasswordUiState())
    val uiState: StateFlow<SetPasswordUiState> = _uiState.asStateFlow()

    fun onPasswordChange(newValue: String) {
        _uiState.update { it.copy(password = newValue, errorMessage = null) }
    }

    fun onConfirmPasswordChange(newValue: String) {
        _uiState.update { it.copy(confirmPassword = newValue, errorMessage = null) }
    }

    fun setPassword(onSuccess: () -> Unit) {
        val password = _uiState.value.password
        val confirmPassword = _uiState.value.confirmPassword

        if (password.length < 8) {
            _uiState.update { it.copy(errorMessage = "Password must be at least 8 characters long") }
            return
        }

        if (password != confirmPassword) {
            _uiState.update { it.copy(errorMessage = "Passwords do not match") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val user = authRepository.getCurrentUser()

            if (user != null) {
                try {
                    // Firebase API to update (or set) the password
                    // Note: updatePassword works for both changing and setting a new one for provider-linked accounts
                    val task = user.updatePassword(password)
                    task.addOnCompleteListener { result ->
                        if (result.isSuccessful) {
                            _uiState.update { it.copy(isLoading = false) }
                            onSuccess()
                        } else {
                            _uiState.update {
                                it.copy(isLoading = false, errorMessage = result.exception?.message ?: "Failed to set password")
                            }
                        }
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "User not logged in") }
            }
        }
    }
}

data class SetPasswordUiState(
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)