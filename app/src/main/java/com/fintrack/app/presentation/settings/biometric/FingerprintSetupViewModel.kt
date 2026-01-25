package com.fintrack.app.presentation.settings.biometric

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.app.core.data.local.LocalAuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FingerprintSetupViewModel @Inject constructor(
    private val localAuthManager: LocalAuthManager
) : ViewModel() {

    fun enableBiometric(onSuccess: () -> Unit) {
        viewModelScope.launch {
            localAuthManager.setBiometricEnabled(true)
            onSuccess()
        }
    }
}