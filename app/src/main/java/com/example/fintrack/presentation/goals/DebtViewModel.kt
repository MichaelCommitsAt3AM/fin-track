package com.example.fintrack.presentation.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fintrack.core.domain.model.Debt
import com.example.fintrack.core.domain.model.DebtType
import com.example.fintrack.core.domain.model.Payment
import com.example.fintrack.core.domain.repository.DebtRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class DebtViewModel @Inject constructor(
    private val debtRepository: DebtRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val userId: String?
        get() = firebaseAuth.currentUser?.uid

    // State for all debts
    private val _debts = MutableStateFlow<List<Debt>>(emptyList())
    val debts: StateFlow<List<Debt>> = _debts.asStateFlow()

    // State for current debt (for detail screen)
    private val _currentDebt = MutableStateFlow<Debt?>(null)
    val currentDebt: StateFlow<Debt?> = _currentDebt.asStateFlow()

    // State for payments
    private val _payments = MutableStateFlow<List<Payment>>(emptyList())
    val payments: StateFlow<List<Payment>> = _payments.asStateFlow()

    // Loading and error states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadAllDebts()
    }

    fun loadAllDebts() {
        userId?.let { uid ->
            viewModelScope.launch {
                _isLoading.value = true
                debtRepository.getAllDebts(uid)
                    .catch { e ->
                        _error.value = e.message
                        _isLoading.value = false
                    }
                    .collect { debtsList ->
                        _debts.value = debtsList
                        _isLoading.value = false
                    }
            }
        }
    }

    fun loadDebt(debtId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            debtRepository.getDebt(debtId)
                .catch { e ->
                    _error.value = e.message
                    _isLoading.value = false
                }
                .collect { debt ->
                    _currentDebt.value = debt
                    _isLoading.value = false
                }

            // Also load payments for this debt
            debtRepository.getPaymentsForDebt(debtId)
                .catch { e -> _error.value = e.message }
                .collect { paymentsList ->
                    _payments.value = paymentsList
                }
        }
    }

    fun addDebt(
        title: String,
        originalAmount: Double,
        currentBalance: Double,
        minimumPayment: Double,
        dueDate: Long,
        interestRate: Double,
        notes: String?,
        iconName: String,
        debtType: DebtType
    ) {
        userId?.let { uid ->
            viewModelScope.launch {
                try {
                    _isLoading.value = true
                    val debt = Debt(
                        id = UUID.randomUUID().toString(),
                        userId = uid,
                        title = title,
                        originalAmount = originalAmount,
                        currentBalance = currentBalance,
                        minimumPayment = minimumPayment,
                        dueDate = dueDate,
                        interestRate = interestRate,
                        notes = notes,
                        iconName = iconName,
                        debtType = debtType,
                        createdAt = System.currentTimeMillis()
                    )
                    debtRepository.insertDebt(debt)
                    _isLoading.value = false
                } catch (e: Exception) {
                    _error.value = e.message
                    _isLoading.value = false
                }
            }
        }
    }

    fun updateDebt(debt: Debt) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                debtRepository.updateDebt(debt)
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun deleteDebt(debtId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                debtRepository.deleteDebt(debtId)
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun makePayment(
        debtId: String,
        amount: Double,
        note: String?
    ) {
        viewModelScope.launch {
            try {
                val payment = Payment(
                    id = UUID.randomUUID().toString(),
                    debtId = debtId,
                    amount = amount,
                    date = System.currentTimeMillis(),
                    note = note
                )
                
                // Get current debt balance
                val currentBalance = _currentDebt.value?.currentBalance ?: 0.0
                
                debtRepository.makePayment(payment, currentBalance)
                
                // Reload debt to get updated balance
                loadDebt(debtId)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
