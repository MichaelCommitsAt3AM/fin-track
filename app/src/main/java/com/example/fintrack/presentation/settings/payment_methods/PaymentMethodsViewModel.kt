package com.example.fintrack.presentation.settings.payment_methods

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fintrack.core.domain.model.PaymentMethod
import com.example.fintrack.core.domain.repository.PaymentMethodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PaymentMethodsViewModel @Inject constructor(
    private val paymentMethodRepository: PaymentMethodRepository
) : ViewModel() {

    // Payment methods from repository
    val paymentMethods = paymentMethodRepository.getAllPaymentMethods()

    // UI state for dialogs
    private val _showAddEditDialog = MutableStateFlow(false)
    val showAddEditDialog: StateFlow<Boolean> = _showAddEditDialog.asStateFlow()

    private val _editingPaymentMethod = MutableStateFlow<PaymentMethod?>(null)
    val editingPaymentMethod: StateFlow<PaymentMethod?> = _editingPaymentMethod.asStateFlow()

    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog: StateFlow<Boolean> = _showDeleteDialog.asStateFlow()

    private val _deletingPaymentMethod = MutableStateFlow<PaymentMethod?>(null)
    val deletingPaymentMethod: StateFlow<PaymentMethod?> = _deletingPaymentMethod.asStateFlow()

    // Initialize default payment methods on first launch
    init {
        viewModelScope.launch {
            paymentMethodRepository.initDefaultPaymentMethods()
        }
    }

    fun onAddPaymentMethodClick() {
        _editingPaymentMethod.value = null
        _showAddEditDialog.value = true
    }

    fun onEditPaymentMethod(paymentMethod: PaymentMethod) {
        _editingPaymentMethod.value = paymentMethod
        _showAddEditDialog.value = true
    }

    fun onDismissAddEditDialog() {
        _showAddEditDialog.value = false
        _editingPaymentMethod.value = null
    }

    fun onSavePaymentMethod(
        name: String,
        iconName: String,
        colorHex: String,
        isDefault: Boolean = false
    ) {
        viewModelScope.launch {
            val paymentMethod = PaymentMethod(
                name = name,
                iconName = iconName,
                colorHex = colorHex,
                isDefault = isDefault,
                isActive = true
            )

            val existingMethod = _editingPaymentMethod.value
            if (existingMethod != null) {
                // Update existing
                paymentMethodRepository.updatePaymentMethod(paymentMethod)
            } else {
                // Add new
                paymentMethodRepository.addPaymentMethod(paymentMethod)
            }

            onDismissAddEditDialog()
        }
    }

    fun onSetDefaultPaymentMethod(paymentMethod: PaymentMethod) {
        viewModelScope.launch {
            paymentMethodRepository.setDefaultPaymentMethod(paymentMethod.name)
        }
    }

    fun onDeletePaymentMethodClick(paymentMethod: PaymentMethod) {
        _deletingPaymentMethod.value = paymentMethod
        _showDeleteDialog.value = true
    }

    fun onConfirmDelete() {
        viewModelScope.launch {
            _deletingPaymentMethod.value?.let { method ->
                paymentMethodRepository.deactivatePaymentMethod(method.name)
            }
            _showDeleteDialog.value = false
            _deletingPaymentMethod.value = null
        }
    }

    fun onDismissDeleteDialog() {
        _showDeleteDialog.value = false
        _deletingPaymentMethod.value = null
    }
}
