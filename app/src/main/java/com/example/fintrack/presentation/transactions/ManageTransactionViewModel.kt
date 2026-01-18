package com.example.fintrack.presentation.transactions

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fintrack.core.data.local.LocalAuthManager
import com.example.fintrack.core.domain.model.Currency
import com.example.fintrack.core.domain.model.TransactionType
import com.example.fintrack.core.domain.repository.CategoryRepository
import com.example.fintrack.core.domain.repository.TransactionRepository
import com.example.fintrack.presentation.settings.getIconByName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManageTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val localAuthManager: LocalAuthManager
) : ViewModel() {

    private val _transactionId = MutableStateFlow<String?>(null)

    val currencyPreference = localAuthManager.currencyPreference
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Currency.KSH
        )

    private val _events = Channel<ManageTransactionEvent>()
    val events = _events.receiveAsFlow()

    // Get the transaction and map it to UI model
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val transaction: StateFlow<TransactionUiModel?> = _transactionId
        .filterNotNull()
        .flatMapLatest { id ->
            combine(
                transactionRepository.getTransactionById(id),
                categoryRepository.getAllCategories()
            ) { transaction, categories ->
                transaction?.let { txn ->
                    // Find category for icon and color
                    val category = categories.find { it.name == txn.category }
                    val icon = category?.iconName?.let { getIconByName(it) }
                        ?: getIconByName("wallet")
                    val color = try {
                        Color(android.graphics.Color.parseColor(category?.colorHex ?: "#CCCCCC"))
                    } catch (e: Exception) {
                        Color.Gray
                    }

                    // Convert to UI model
                    val displayAmount = if (txn.type == TransactionType.INCOME) {
                        txn.amount
                    } else {
                        -kotlin.math.abs(txn.amount)
                    }

                    TransactionUiModel(
                        id = txn.id,
                        amount = displayAmount,
                        category = txn.category,
                        dateMillis = txn.date,
                        description = txn.notes,
                        paymentMethod = txn.paymentMethod,
                        icon = icon,
                        color = color
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun loadTransaction(id: String) {
        _transactionId.value = id
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(id)
            _events.send(ManageTransactionEvent.NavigateBack)
        }
    }
}

data class TransactionUiModel(
    val id: String,
    val amount: Double,
    val category: String,
    val dateMillis: Long,
    val description: String?,
    val paymentMethod: String?,
    val icon: ImageVector,
    val color: Color
)
