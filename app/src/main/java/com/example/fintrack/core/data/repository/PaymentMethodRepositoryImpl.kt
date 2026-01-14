package com.example.fintrack.core.data.repository

import com.example.fintrack.core.data.local.dao.PaymentMethodDao
import com.example.fintrack.core.data.local.model.PaymentMethodEntity
import com.example.fintrack.core.domain.model.PaymentMethod
import com.example.fintrack.core.domain.repository.PaymentMethodRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PaymentMethodRepositoryImpl @Inject constructor(
    private val paymentMethodDao: PaymentMethodDao,
    private val firebaseAuth: FirebaseAuth
) : PaymentMethodRepository {

    private fun getUserId(): String? = firebaseAuth.currentUser?.uid

    override suspend fun initDefaultPaymentMethods() {
        val userId = getUserId() ?: return
        
        // Check if user already has payment methods
        val existing = paymentMethodDao.getDefaultPaymentMethod(userId)
        if (existing != null) return
        
        // Initialize with default payment methods
        val defaultMethods = listOf(
            PaymentMethodEntity(
                name = "Cash",
                userId = userId,
                iconName = "payments",
                colorHex = "#4CAF50",
                isDefault = true,
                isActive = true
            ),
            PaymentMethodEntity(
                name = "Credit Card",
                userId = userId,
                iconName = "credit_card",
                colorHex = "#2196F3",
                isDefault = false,
                isActive = true
            ),
            PaymentMethodEntity(
                name = "Debit Card",
                userId = userId,
                iconName = "credit_card",
                colorHex = "#FF9800",
                isDefault = false,
                isActive = true
            ),
            PaymentMethodEntity(
                name = "Mobile Money",
                userId = userId,
                iconName = "phone_android",
                colorHex = "#9C27B0",
                isDefault = false,
                isActive = true
            ),
            PaymentMethodEntity(
                name = "Bank Transfer",
                userId = userId,
                iconName = "account_balance",
                colorHex = "#607D8B",
                isDefault = false,
                isActive = true
            )
        )
        
        paymentMethodDao.insertAll(defaultMethods)
    }

    override fun getAllPaymentMethods(): Flow<List<PaymentMethod>> {
        val userId = getUserId() ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return paymentMethodDao.getAllPaymentMethods(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addPaymentMethod(paymentMethod: PaymentMethod) {
        val userId = getUserId() ?: return
        paymentMethodDao.insertPaymentMethod(paymentMethod.toEntity(userId))
    }

    override suspend fun updatePaymentMethod(paymentMethod: PaymentMethod) {
        val userId = getUserId() ?: return
        paymentMethodDao.updatePaymentMethod(paymentMethod.toEntity(userId))
    }

    override suspend fun setDefaultPaymentMethod(name: String) {
        val userId = getUserId() ?: return
        // Clear existing default
        paymentMethodDao.clearDefaultPaymentMethod(userId)
        // Set new default
        paymentMethodDao.setDefaultPaymentMethod(userId, name)
    }

    override suspend fun deactivatePaymentMethod(name: String) {
        val userId = getUserId() ?: return
        paymentMethodDao.deactivatePaymentMethod(userId, name)
    }

    // Mapper extensions
    private fun PaymentMethodEntity.toDomain(): PaymentMethod {
        return PaymentMethod(
            name = this.name,
            iconName = this.iconName,
            colorHex = this.colorHex,
            isDefault = this.isDefault,
            isActive = this.isActive
        )
    }

    private fun PaymentMethod.toEntity(userId: String): PaymentMethodEntity {
        return PaymentMethodEntity(
            name = this.name,
            userId = userId,
            iconName = this.iconName,
            colorHex = this.colorHex,
            isDefault = this.isDefault,
            isActive = this.isActive
        )
    }
}
