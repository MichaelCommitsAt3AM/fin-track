package com.example.fintrack.presentation.auth.pin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fintrack.core.data.local.LocalAuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PinLoginViewModel @Inject constructor(
    private val localAuthManager: LocalAuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PinLoginUiState())
    val uiState: StateFlow<PinLoginUiState> = _uiState.asStateFlow()

    private val _eventFlow = Channel<PinLoginEvent>()
    val eventFlow = _eventFlow.receiveAsFlow()

    fun onDigitEntered(digit: String) {
        val currentPin = _uiState.value.enteredPin

        if (currentPin.length < 4) {
            val newPin = currentPin + digit
            _uiState.value = _uiState.value.copy(
                enteredPin = newPin,
                isError = false
            )

            // Auto-verify when 4 digits are entered
            if (newPin.length == 4) {
                verifyPin(newPin)
            }
        }
    }

    fun onBackspace() {
        _uiState.value = _uiState.value.copy(
            enteredPin = _uiState.value.enteredPin.dropLast(1),
            isError = false
        )
    }

    private fun verifyPin(pin: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Small delay for UX
            delay(200)

            // Check if PIN is set
            val hasPinSet = localAuthManager.hasPinSet()

            launch(Dispatchers.Main) {
                if (!hasPinSet) {
                    // No PIN set - should not happen, but handle gracefully
                    _eventFlow.send(PinLoginEvent.Error("No PIN configured. Please set up your PIN."))
                    _uiState.value = _uiState.value.copy(
                        enteredPin = "",
                        isError = true
                    )
                } else if (localAuthManager.verifyPin(pin)) {
                    // PIN matches - success!
                    _eventFlow.send(PinLoginEvent.Success)
                } else {
                    // PIN doesn't match - show error
                    _uiState.value = _uiState.value.copy(isError = true)
                    _eventFlow.send(PinLoginEvent.Error("Incorrect PIN. Please try again."))

                    // Clear PIN after showing error
                    delay(500)
                    _uiState.value = _uiState.value.copy(
                        enteredPin = "",
                        isError = false
                    )
                }
            }
        }
    }
}

data class PinLoginUiState(
    val enteredPin: String = "",
    val isError: Boolean = false
)

sealed class PinLoginEvent {
    object Success : PinLoginEvent()
    data class Error(val message: String) : PinLoginEvent()
}