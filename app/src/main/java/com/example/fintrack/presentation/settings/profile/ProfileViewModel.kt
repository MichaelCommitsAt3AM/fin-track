package com.example.fintrack.presentation.settings.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventChannel = Channel<ProfileEvent>()
    val events = _eventChannel.receiveAsFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            val currentUser = firebaseAuth.currentUser
            if (currentUser != null) {
                try {
                    // Load profile from Firestore
                    val userDoc = firestore.collection("users")
                        .document(currentUser.uid)
                        .get()
                        .await()

                    val avatarId = userDoc.getLong("avatarId")?.toInt() ?: 1
                    val phoneNumber = userDoc.getString("phoneNumber") ?: ""

                    _uiState.value = _uiState.value.copy(
                        fullName = currentUser.displayName ?: "",
                        email = currentUser.email ?: "",
                        phoneNumber = phoneNumber,
                        avatarId = avatarId
                    )
                } catch (e: Exception) {
                    // Fallback to Firebase Auth data
                    _uiState.value = _uiState.value.copy(
                        fullName = currentUser.displayName ?: "",
                        email = currentUser.email ?: "",
                        avatarId = 1
                    )
                }
            }
        }
    }

    fun onEvent(event: ProfileUiEvent) {
        when (event) {
            is ProfileUiEvent.OnFullNameChange -> {
                _uiState.value = _uiState.value.copy(fullName = event.name)
            }
            is ProfileUiEvent.OnPhoneNumberChange -> {
                _uiState.value = _uiState.value.copy(phoneNumber = event.phone)
            }
            is ProfileUiEvent.OnAvatarChange -> {
                _uiState.value = _uiState.value.copy(avatarId = event.avatarId)
            }
            is ProfileUiEvent.OnSaveProfile -> saveProfile()
        }
    }

    private fun saveProfile() {
        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            try {
                val currentUser = firebaseAuth.currentUser
                if (currentUser != null) {
                    // Update Firebase Auth display name
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(_uiState.value.fullName)
                        .build()
                    currentUser.updateProfile(profileUpdates).await()

                    // Save additional data to Firestore
                    val userProfile = hashMapOf(
                        "fullName" to _uiState.value.fullName,
                        "email" to _uiState.value.email,
                        "phoneNumber" to _uiState.value.phoneNumber,
                        "avatarId" to _uiState.value.avatarId,
                        "updatedAt" to System.currentTimeMillis()
                    )

                    firestore.collection("users")
                        .document(currentUser.uid)
                        .set(userProfile, com.google.firebase.firestore.SetOptions.merge())
                        .await()

                    _uiState.value = _uiState.value.copy(isLoading = false)
                    _eventChannel.send(ProfileEvent.ShowSuccess("Profile updated successfully"))
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    _eventChannel.send(ProfileEvent.ShowError("User not logged in"))
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                _eventChannel.send(ProfileEvent.ShowError("Failed to update profile: ${e.message}"))
            }
        }
    }
}
