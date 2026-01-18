package com.example.fintrack.presentation.goals.saving

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fintrack.core.data.local.LocalAuthManager
import com.example.fintrack.core.domain.model.Contribution
import com.example.fintrack.core.domain.model.Currency
import com.example.fintrack.core.domain.model.Saving
import com.example.fintrack.core.domain.repository.SavingRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SavingViewModel @Inject constructor(
    private val savingRepository: SavingRepository,
    private val firebaseAuth: FirebaseAuth,
    private val localAuthManager: LocalAuthManager
) : ViewModel() {

    private val userId: String?
        get() = firebaseAuth.currentUser?.uid

    // State for all savings
    private val _savings = MutableStateFlow<List<Saving>>(emptyList())
    val savings: StateFlow<List<Saving>> = _savings.asStateFlow()

    // State for current saving (for detail screen)
    private val _currentSaving = MutableStateFlow<Saving?>(null)
    val currentSaving: StateFlow<Saving?> = _currentSaving.asStateFlow()

    // State for contributions
    private val _contributions = MutableStateFlow<List<Contribution>>(emptyList())
    val contributions: StateFlow<List<Contribution>> = _contributions.asStateFlow()

    // Loading and error states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Currency preference
    val currencyPreference = localAuthManager.currencyPreference
        .stateIn(
            viewModelScope,
            SharingStarted.Companion.WhileSubscribed(5000),
            initialValue = Currency.KSH
        )

    init {
        loadAllSavings()
    }

    fun loadAllSavings() {
        userId?.let { uid ->
            viewModelScope.launch {
                _isLoading.value = true
                savingRepository.getAllSavings(uid)
                    .catch { e ->
                        _error.value = e.message
                        _isLoading.value = false
                    }
                    .collect { savingsList ->
                        _savings.value = savingsList
                        _isLoading.value = false
                    }
            }
        }
    }

    fun loadSaving(savingId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            savingRepository.getSaving(savingId)
                .catch { e ->
                    _error.value = e.message
                    _isLoading.value = false
                }
                .collect { saving ->
                    _currentSaving.value = saving
                    _isLoading.value = false
                }

            // Also load contributions for this saving
            savingRepository.getContributionsForSaving(savingId)
                .catch { e -> _error.value = e.message }
                .collect { contributionsList ->
                    _contributions.value = contributionsList
                }
        }
    }

    fun addSaving(
        title: String,
        targetAmount: Double,
        currentAmount: Double,
        targetDate: Long,
        notes: String?,
        iconName: String
    ) {
        userId?.let { uid ->
            viewModelScope.launch {
                try {
                    _isLoading.value = true
                    val saving = Saving(
                        id = UUID.randomUUID().toString(),
                        userId = uid,
                        title = title,
                        targetAmount = targetAmount,
                        currentAmount = currentAmount,
                        targetDate = targetDate,
                        notes = notes,
                        iconName = iconName,
                        createdAt = System.currentTimeMillis()
                    )
                    savingRepository.insertSaving(saving)
                    _isLoading.value = false
                } catch (e: Exception) {
                    _error.value = e.message
                    _isLoading.value = false
                }
            }
        }
    }

    fun updateSaving(saving: Saving) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                savingRepository.updateSaving(saving)
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun deleteSaving(savingId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                savingRepository.deleteSaving(savingId)
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun addContribution(
        savingId: String,
        amount: Double,
        note: String?
    ) {
        viewModelScope.launch {
            try {
                val contribution = Contribution(
                    id = UUID.randomUUID().toString(),
                    savingId = savingId,
                    amount = amount,
                    date = System.currentTimeMillis(),
                    note = note
                )

                // Get current saving amount
                val  currentAmount = _currentSaving.value?.currentAmount ?: 0.0

                savingRepository.addContribution(contribution, currentAmount)

                // Reload saving to get updated amount
                loadSaving(savingId)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}