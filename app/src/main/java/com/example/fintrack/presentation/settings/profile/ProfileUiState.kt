package com.example.fintrack.presentation.settings.profile

data class ProfileUiState(
    val fullName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val avatarId: Int = 1,
    val availableAvatars: List<Int> = (1..6).toList(),
    val isLoading: Boolean = false
)

sealed class ProfileUiEvent {
    data class OnFullNameChange(val name: String) : ProfileUiEvent()
    data class OnPhoneNumberChange(val phone: String) : ProfileUiEvent()
    data class OnAvatarChange(val avatarId: Int) : ProfileUiEvent()
    object OnSaveProfile : ProfileUiEvent()
}

sealed class ProfileEvent {
    data class ShowSuccess(val message: String) : ProfileEvent()
    data class ShowError(val message: String) : ProfileEvent()
}
