package com.example.fintrack.presentation.settings.biometric

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fintrack.core.data.local.LocalAuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BiometricSetupViewModel @Inject constructor(
    private val localAuthManager: LocalAuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BiometricSetupUiState())
    val uiState = _uiState.asStateFlow()

    // Channel for one-time events like Toasts
    private val _eventChannel = Channel<SetupEvent>()
    val eventFlow = _eventChannel.receiveAsFlow()

    fun onDigitEntered(digit: String, onComplete: (String) -> Unit) {
        val currentState = _uiState.value

        if (currentState.firstPin.length == 4) {
            if (currentState.confirmPin.length < 4) {
                val newConfirm = currentState.confirmPin + digit
                val newState = currentState.copy(confirmPin = newConfirm)
                _uiState.value = newState

                if (newConfirm.length == 4) {
                    onComplete(newState.firstPin)
                }
            }
        } else {
            if (currentState.firstPin.length < 4) {
                _uiState.value = currentState.copy(
                    firstPin = currentState.firstPin + digit
                )
            }
        }
    }


    fun onBackspace() {
        val currentState = _uiState.value

        if (currentState.confirmPin.isNotEmpty()) {
            _uiState.value = currentState.copy(confirmPin = currentState.confirmPin.dropLast(1))
        } else if (currentState.firstPin.isNotEmpty()) {
            _uiState.value = currentState.copy(firstPin = currentState.firstPin.dropLast(1))
        }
    }

    fun onConfirm(onSuccess: () -> Unit) {
        val currentState = _uiState.value

        // Validate
        if (currentState.firstPin.length != 4 || currentState.confirmPin.length != 4) {
            viewModelScope.launch { _eventChannel.send(SetupEvent.ShowMessage("PIN must be 4 digits")) }
            return
        }

        if (currentState.firstPin != currentState.confirmPin) {
            // Clear confirm PIN so they can try again
            _uiState.value = currentState.copy(confirmPin = "")
            viewModelScope.launch { _eventChannel.send(SetupEvent.ShowMessage("PINs do not match")) }
            return
        }

        // Save
        viewModelScope.launch(Dispatchers.IO) {
            try {
                localAuthManager.setUserPin(currentState.firstPin)

                // Navigate back on Main thread
                launch(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    _eventChannel.send(SetupEvent.ShowMessage("Error saving settings"))
                }
            }
        }
    }
}

data class BiometricSetupUiState(
    val firstPin: String = "",
    val confirmPin: String = ""
) {
    val isComplete: Boolean get() = firstPin.length == 4 && confirmPin.length == 4
}

sealed class SetupEvent {
    data class ShowMessage(val message: String) : SetupEvent()
}