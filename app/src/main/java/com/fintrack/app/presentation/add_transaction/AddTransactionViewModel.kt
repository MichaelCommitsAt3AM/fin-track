package com.fintrack.app.presentation.add_transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.app.core.data.local.LocalAuthManager
import com.fintrack.app.core.domain.model.Category
import com.fintrack.app.core.domain.model.Currency
import com.fintrack.app.core.domain.model.RecurrenceFrequency
import com.fintrack.app.core.domain.model.RecurringTransaction
import com.fintrack.app.core.domain.model.Transaction
import com.fintrack.app.core.domain.model.TransactionType
import com.fintrack.app.core.domain.repository.TransactionRepository
import com.fintrack.app.core.domain.repository.PaymentMethodRepository
import com.fintrack.app.core.domain.repository.ExternalTransactionRepository
import com.fintrack.app.core.domain.use_case.GetCategoriesUseCase
import com.fintrack.app.core.profile.ProfileManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class AddTransactionUiState(
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val categories: List<Category> = emptyList(),
    val paymentMethods: List<String> = emptyList(), // Load from database
    val amount: String = "",
    val description: String = "",
    val selectedCategory: String? = null,
    val selectedPaymentMethod: String? = null, // Will be set after loading
    val date: Long = System.currentTimeMillis(),
    // Recurring State
    val isRecurring: Boolean = false,
    val recurrenceFrequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,

    val isLoading: Boolean = false,
    val error: String? = null,
    val isPlannedTransaction: Boolean = false, // NEW: Shows if selected date is in the future
    val isEditMode: Boolean = false, // NEW: Indicates if editing existing transaction
    val transactionId: String? = null, // NEW: ID of transaction being edited
    val isMpesaTransaction: Boolean = false // NEW: Indicates if editing M-Pesa transaction (limited fields editable)
)

sealed class AddTransactionUiEvent {
    data class OnTypeChange(val type: TransactionType) : AddTransactionUiEvent()
    data class OnAmountChange(val amount: String) : AddTransactionUiEvent()
    data class OnDescriptionChange(val description: String) : AddTransactionUiEvent()
    data class OnCategoryChange(val category: String) : AddTransactionUiEvent()
    data class OnPaymentMethodChange(val paymentMethod: String) : AddTransactionUiEvent() // NEW
    data class OnDateChange(val date: Long) : AddTransactionUiEvent()
    data class OnRecurringChange(val isRecurring: Boolean) : AddTransactionUiEvent()
    data class OnFrequencyChange(val frequency: RecurrenceFrequency) : AddTransactionUiEvent()
    object OnSaveTransaction : AddTransactionUiEvent()
}

sealed class AddTransactionEvent {
    object NavigateBack : AddTransactionEvent()
}

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val paymentMethodRepository: PaymentMethodRepository,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val localAuthManager: LocalAuthManager,
    private val externalTransactionRepository: ExternalTransactionRepository,
    private val profileManager: ProfileManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddTransactionUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventChannel = Channel<AddTransactionEvent>()
    val events = _eventChannel.receiveAsFlow()

    val currencyPreference = localAuthManager.currencyPreference
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Currency.KSH
        )

    init {
        loadCategories()
        loadPaymentMethods()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            getCategoriesUseCase().collect { categories ->
                _uiState.value = _uiState.value.copy(categories = categories)
            }
        }
    }

    private fun loadPaymentMethods() {
        viewModelScope.launch {
            // Initialize defaults if needed
            paymentMethodRepository.initDefaultPaymentMethods()
            
            paymentMethodRepository.getAllPaymentMethods().collect { methods ->
                val methodNames = methods.map { it.name }
                // Auto-select the default payment method, or first one if no default
                val defaultMethod = methods.find { it.isDefault }?.name ?: methodNames.firstOrNull()
                _uiState.value = _uiState.value.copy(
                    paymentMethods = methodNames,
                    selectedPaymentMethod = _uiState.value.selectedPaymentMethod ?: defaultMethod
                )
            }
        }
    }

    fun loadTransaction(transactionId: String?) {
        // Only load once: this is called again when returning from e.g. "Add category",
        // and reloading would overwrite a category the user already picked.
        if (transactionId.isNullOrEmpty() || _uiState.value.transactionId == transactionId) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val isMpesa = ExternalTransactionRepository.isExternalId(transactionId)
            // One-shot reads: the old code kept collecting these flows, so any later DB change
            // (like saving a category mapping) would reset the form.
            val transaction = if (isMpesa) {
                externalTransactionRepository.getAllTransactions().first().find { it.id == transactionId }
            } else {
                transactionRepository.getTransactionById(transactionId).first()
            }

            _uiState.value = if (transaction != null) {
                _uiState.value.copy(
                    transactionId = transactionId,
                    isEditMode = true,
                    transactionType = transaction.type,
                    amount = transaction.amount.toString(),
                    description = transaction.notes ?: "",
                    selectedCategory = transaction.category,
                    selectedPaymentMethod = transaction.paymentMethod,
                    date = transaction.date,
                    isPlannedTransaction = transaction.date > System.currentTimeMillis(),
                    isMpesaTransaction = isMpesa,
                    isLoading = false
                )
            } else {
                _uiState.value.copy(isLoading = false, error = "Transaction not found")
            }
        }
    }

    fun onEvent(event: AddTransactionUiEvent) {
        when (event) {
            is AddTransactionUiEvent.OnTypeChange -> {
                _uiState.value = _uiState.value.copy(
                    transactionType = event.type,
                    selectedCategory = null
                )
            }
            is AddTransactionUiEvent.OnAmountChange -> _uiState.value = _uiState.value.copy(amount = event.amount)
            is AddTransactionUiEvent.OnDescriptionChange -> _uiState.value = _uiState.value.copy(description = event.description)
            is AddTransactionUiEvent.OnCategoryChange -> _uiState.value = _uiState.value.copy(selectedCategory = event.category)
            is AddTransactionUiEvent.OnPaymentMethodChange -> _uiState.value = _uiState.value.copy(selectedPaymentMethod = event.paymentMethod) // NEW
            is AddTransactionUiEvent.OnDateChange -> {
                val currentTime = System.currentTimeMillis()
                val isPlanned = event.date > currentTime
                _uiState.value = _uiState.value.copy(
                    date = event.date,
                    isPlannedTransaction = isPlanned
                )
            }
            is AddTransactionUiEvent.OnRecurringChange -> _uiState.value = _uiState.value.copy(isRecurring = event.isRecurring)
            is AddTransactionUiEvent.OnFrequencyChange -> _uiState.value = _uiState.value.copy(recurrenceFrequency = event.frequency)
            is AddTransactionUiEvent.OnSaveTransaction -> saveTransaction()
        }
    }

    private fun saveTransaction() {
        val state = _uiState.value
        val amountDouble = state.amount.toDoubleOrNull()
        val userId = profileManager.profileId() // ADD THIS


        // --- Validation ---
        if (userId == null) {
            _uiState.value = _uiState.value.copy(error = "User not logged in")
            return
        }
        if (state.selectedCategory == null) {
            _uiState.value = _uiState.value.copy(error = "Please select a category")
            return
        }
        if (state.selectedPaymentMethod == null) {
            _uiState.value = _uiState.value.copy(error = "Please select a payment method")
            return
        }
        if (amountDouble == null || amountDouble <= 0) {
            _uiState.value = _uiState.value.copy(error = "Please enter a valid amount")
            return
        }
        // -----------------

        _uiState.value = _uiState.value.copy(isLoading = true)

        // M-Pesa transactions are read-only copies of an SMS: only the category can change,
        // and it's saved as a local override (never as a manual transaction / to the cloud).
        if (state.isMpesaTransaction && !state.transactionId.isNullOrEmpty()) {
            viewModelScope.launch {
                try {
                    externalTransactionRepository.setCategory(state.transactionId, state.selectedCategory)
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    _eventChannel.send(AddTransactionEvent.NavigateBack)
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Couldn't update category: ${e.message}")
                }
            }
            return
        }

        viewModelScope.launch {
            val transaction = Transaction(
                id = state.transactionId ?: "", // Use existing ID if editing
                userId = userId,
                type = state.transactionType,
                amount = amountDouble,
                category = state.selectedCategory,
                date = state.date,
                notes = state.description.ifBlank { null },
                paymentMethod = state.selectedPaymentMethod,
                tags = null
            )
            
            if (state.isEditMode && !state.transactionId.isNullOrEmpty()) {
                // Update existing transaction
                transactionRepository.updateTransaction(transaction)
            } else {
                // Create new transaction
                transactionRepository.insertTransaction(transaction)
            }

            // 2. Save Recurring Rule (If enabled) - Only for new transactions
            if (state.isRecurring && !state.isEditMode) {
                val userId = profileManager.profileId() ?: return@launch

                val recurringRule = RecurringTransaction(
                    type = state.transactionType,
                    userId = userId,
                    amount = amountDouble,
                    category = state.selectedCategory,
                    startDate = state.date,
                    frequency = state.recurrenceFrequency,
                    notes = state.description.ifBlank { null }
                )
                transactionRepository.insertRecurringTransaction(recurringRule)
            }

            _uiState.value = _uiState.value.copy(isLoading = false)
            _eventChannel.send(AddTransactionEvent.NavigateBack)
        }
    }

    fun formatMillisToDate(millis: Long): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(Date(millis))
    }

    fun parseDateToMillis(dateString: String): Long {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.parse(dateString)?.time ?: System.currentTimeMillis()
    }
}
