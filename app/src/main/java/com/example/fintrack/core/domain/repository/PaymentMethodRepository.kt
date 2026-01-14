package com.example.fintrack.core.domain.repository

import com.example.fintrack.core.domain.model.PaymentMethod
import kotlinx.coroutines.flow.Flow

interface PaymentMethodRepository {
    
    suspend fun initDefaultPaymentMethods()
    
    fun getAllPaymentMethods(): Flow<List<PaymentMethod>>
    
    suspend fun addPaymentMethod(paymentMethod: PaymentMethod)
    
    suspend fun updatePaymentMethod(paymentMethod: PaymentMethod)
    
    suspend fun setDefaultPaymentMethod(name: String)
    
    suspend fun deactivatePaymentMethod(name: String)
}
