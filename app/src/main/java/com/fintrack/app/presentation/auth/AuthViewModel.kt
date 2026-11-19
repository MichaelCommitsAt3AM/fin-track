package com.fintrack.app.presentation.auth

import com.fintrack.app.core.ui.components.NO_AVATAR
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.app.BuildConfig
import com.fintrack.app.core.domain.model.User
import com.fintrack.app.core.domain.repository.AuthRepository
import com.fintrack.app.core.domain.repository.AuthResult
import com.fintrack.app.core.domain.repository.CategoryRepository
import com.fintrack.app.core.domain.repository.TransactionRepository
import com.fintrack.app.core.domain.repository.UserRepository
import com.fintrack.app.core.util.AppLogger
import com.fintrack.app.util.EmailVerificationRateLimiter // Assuming util is now in core
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val userRepository: UserRepository,
    private val profileManager: com.fintrack.app.core.profile.ProfileManager,
    private val accountLinker: com.fintrack.app.core.profile.AccountLinker,
    private val paymentMethodRepository: com.fintrack.app.core.domain.repository.PaymentMethodRepository
) : ViewModel() {

    private val TAG = "AuthViewModel"

    // Holds the current user (null if logged out)
    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private fun isLocalOnly() =
        profileManager.state.value.mode == com.fintrack.app.core.profile.AccountMode.LOCAL_ONLY

    /** True while a local-only user is signing in / registering to back up their data. */
    val isLinking: StateFlow<Boolean> = profileManager.state
        .map { it.mode == com.fintrack.app.core.profile.AccountMode.LOCAL_ONLY }
        .stateIn(viewModelScope, SharingStarted.Eagerly, isLocalOnly())

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
                if (user.isEmailVerified && isLocalOnly()) {
                    beginLink(user)
                } else if (user.isEmailVerified) {
                    profileManager.linkAccount(user.uid)

                    // Refreshes user data from the cloud every time the app is opened
                    syncUserData()

                    _authEventChannel.send(AuthEvent.NavigateToSetup)
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

    private fun syncUserData() {
        viewModelScope.launch {
            // Launch in parallel for speed
            val job1 = launch { userRepository.syncUserFromCloud() }
            val job2 = launch { categoryRepository.syncCategoriesFromCloud() }
            val job3 = launch { transactionRepository.syncTransactionsFromCloud() }
            // You can also add syncBudgetsFromCloud() here later

            // Wait for both to finish (optional, but good if you want to show a loading spinner)
            job1.join()
            job2.join()
            job3.join()
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
                // A local-only user keeps their profile: the account is linked once the email is
                // verified (see checkEmailVerification), so nothing to create here.
                if (!isLocalOnly()) {
                // Data is owned by the profile; the Firebase UID only names the cloud location
                profileManager.linkAccount(result.user.uid)

                // CREATE USER RECORD IN DATABASE
                try {
                    val user = User(
                        userId = profileManager.profileId() ?: result.user.uid,
                        fullName = email.substringBefore("@"), // Default name from email
                        email = email,
                        phoneNumber = "",
                        avatarId = 1, // Default avatar
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    userRepository.createUser(user)
                } catch (e: Exception) {
                    // Log error but continue
                    AppLogger.e(TAG, "Error creating user record", e)
                }

                // Initialize default categories for the new user
                categoryRepository.initDefaultCategories()
                }

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
                if (result.user.isEmailVerified && isLocalOnly()) {
                    beginLink(result.user)
                } else if (result.user.isEmailVerified) {
                    profileManager.linkAccount(result.user.uid)

                    // Load all user's data from the cloud
                    syncUserData()

                    _uiState.value = _uiState.value.copy(isLoading = false)
                    _authEventChannel.send(AuthEvent.NavigateToSetup)
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

            if (user?.isEmailVerified == true && isLocalOnly()) {
                beginLink(user)
            } else if (user?.isEmailVerified == true) {
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
            AppLogger.d(TAG, "Starting Google Sign-In with credential")
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            AppLogger.d(TAG, "Calling repository.signInWithGoogle")
            val result = repository.signInWithGoogle(credential)

            _uiState.value = _uiState.value.copy(isLoading = false)

            if (result.user != null) {
                AppLogger.d(TAG, "Sign-in successful for user: ${AppLogger.sanitizeUserId(result.user.uid)}")
                if (isLocalOnly()) {
                    beginLink(result.user)
                    return@launch
                }
                profileManager.linkAccount(result.user.uid)
                try {
                    // Check if user exists in database
                    AppLogger.d(TAG, "Checking if user exists in database")
                    val existingUser = userRepository.getCurrentUserOnce()

                    if (existingUser == null) {
                        AppLogger.d(TAG, "New user detected, creating user record")
                        // New user - create user record
                        val newUser = User(
                            userId = profileManager.profileId() ?: result.user.uid,
                            fullName = result.user.displayName ?: result.user.email?.substringBefore("@") ?: "User",
                            email = result.user.email ?: "",
                            phoneNumber = result.user.phoneNumber ?: "",
                            avatarId = 1,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        AppLogger.d(TAG, "Creating user in database")
                        userRepository.createUser(newUser)

                        // Initialize default categories for new user
                        AppLogger.d(TAG, "Initializing default categories")
                        categoryRepository.initDefaultCategories()

                        // Navigate to setup for new users
                        AppLogger.d(TAG, "Navigating to setup (new user)")
                        _authEventChannel.send(AuthEvent.NavigateToSetup)
                    } else {
                        AppLogger.d(TAG, "Existing user detected, syncing data")
                        // Existing user - sync their data
                        syncUserData()

                        // Navigate to setup (which will redirect to home if already set up)
                        AppLogger.d(TAG, "Navigating to setup (existing user)")
                        _authEventChannel.send(AuthEvent.NavigateToSetup)
                    }
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Error in Google Sign-In flow", e)
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to complete sign in: ${e.message}"
                    )
                }
            } else {
                AppLogger.e(TAG, "Google Sign-in failed: ${result.error}")
                _uiState.value = _uiState.value.copy(
                    error = result.error ?: "Google Sign-In failed"
                )
            }
        }
    }

// You can remove or update the old handleAuthResult function since we're not using it anymore
// Or keep it for other auth methods if needed

    // Check if the user is existing or new
    suspend fun isNewUser(): Boolean {
        return try {
            val user = userRepository.getCurrentUserOnce()
            // If user exists in database but has default name, they're new
            // Or if user doesn't exist at all
            user == null || user.fullName == user.email.substringBefore("@")
        } catch (e: Exception) {
            true // Default to new user if error
        }
    }

    private suspend fun handleAuthResult(result: AuthResult) {
        _uiState.value = _uiState.value.copy(isLoading = false)
        if (result.user != null) {
            profileManager.linkAccount(result.user.uid)
            // CREATE USER RECORD if it doesn't exist
            try {
                val existingUser = userRepository.getCurrentUserOnce()
                if (existingUser == null) {
                    val newUser = User(
                        userId = profileManager.profileId() ?: result.user.uid,
                        fullName = result.user.displayName ?: result.user.email?.substringBefore("@") ?: "User",
                        email = result.user.email ?: "",
                        phoneNumber = result.user.phoneNumber ?: "",
                        avatarId = 1,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    userRepository.createUser(newUser)

                    // Initialize default categories
                    categoryRepository.initDefaultCategories()
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error creating user", e)
            }

            _authEventChannel.send(AuthEvent.NavigateToHome)
        } else {
            _uiState.value = _uiState.value.copy(error = result.error ?: "Authentication failed")
        }
    }


    // ---------- Continue without an account ----------

    private companion object LocalProfileDefaults {
        /** Local-only users skip profile setup, so they get a generic name and no picture. */
        const val DEFAULT_LOCAL_NAME = "User"
    }

    fun continueWithoutAccount() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val profileId = profileManager.createLocalProfile()
                val now = System.currentTimeMillis()
                userRepository.createUser(
                    User(
                        userId = profileId,
                        fullName = DEFAULT_LOCAL_NAME,
                        email = "",
                        phoneNumber = "",
                        avatarId = NO_AVATAR,
                        createdAt = now,
                        updatedAt = now
                    )
                )
                categoryRepository.initDefaultCategories()
                paymentMethodRepository.initDefaultPaymentMethods()
                _uiState.value = _uiState.value.copy(isLoading = false)
                // No profile setup for local-only users: go straight to the flavor setup
                _authEventChannel.send(AuthEvent.NavigateAfterLocalProfile)
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error creating local profile", e)
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Could not set up the app: ${e.message}")
            }
        }
    }

    // ---------- Link a local-only profile to an account ----------

    private suspend fun beginLink(user: FirebaseUser) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        val hasData = try {
            accountLinker.cloudHasData(user.uid)
        } catch (e: Exception) {
            // Cannot tell whether the account has data: stay local rather than risk a bad merge
            repository.signOut()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Could not check your account. Check your connection and try again."
            )
            return
        }
        if (hasData) {
            _uiState.value = _uiState.value.copy(isLoading = false, pendingCombineUid = user.uid)
        } else {
            completeLink(user.uid, user.email, combine = false)
        }
    }

    fun confirmCombine() {
        val uid = _uiState.value.pendingCombineUid ?: return
        viewModelScope.launch { completeLink(uid, repository.getCurrentUser()?.email, combine = true) }
    }

    fun cancelCombine() {
        repository.signOut() // stays local-only
        _uiState.value = _uiState.value.copy(pendingCombineUid = null)
    }

    private suspend fun completeLink(uid: String, email: String?, combine: Boolean) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        profileManager.linkAccount(uid) // keeps the local profile ID: no Room row is re-keyed
        if (!combine && !email.isNullOrBlank()) {
            userRepository.getCurrentUserOnce()?.let {
                userRepository.updateUser(it.copy(email = email, updatedAt = System.currentTimeMillis()))
            }
        }
        accountLinker.startTransfer(pullFromCloud = combine)
        _uiState.value = _uiState.value.copy(isLoading = false, pendingCombineUid = null)
        _authEventChannel.send(AuthEvent.LinkCompleted)
    }

    fun signOut() {
        repository.signOut()
        viewModelScope.launch {
            // Only an account profile is dropped; a local-only profile (mid-way through linking
            // an account) must keep its data.
            if (profileManager.state.value.mode == com.fintrack.app.core.profile.AccountMode.ACCOUNT) {
                profileManager.signOutToFreshState()
            }
        }
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
    /** Set while we wait for the user to confirm combining with existing cloud data. */
    val pendingCombineUid: String? = null,
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
    object NavigateToSetup : AuthEvent()
    object NavigateToProfileSetup : AuthEvent()
    /** Local-only profile created; skips profile setup. */
    object NavigateAfterLocalProfile : AuthEvent()
    object LinkCompleted : AuthEvent()
}