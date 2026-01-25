package com.fintrack.app.core.domain.repository

import com.fintrack.app.core.domain.model.Debt
import com.fintrack.app.core.domain.model.Payment
import kotlinx.coroutines.flow.Flow

interface DebtRepository {
    
    suspend fun insertDebt(debt: Debt)
    
    suspend fun updateDebt(debt: Debt)
    
    fun getDebt(debtId: String): Flow<Debt?>
    
    fun getAllDebts(userId: String): Flow<List<Debt>>
    
    suspend fun deleteDebt(debtId: String)
    
    suspend fun makePayment(payment: Payment, currentDebtBalance: Double)
    
    fun getPaymentsForDebt(debtId: String): Flow<List<Payment>>
    
    suspend fun syncDebtsFromCloud()
    
    suspend fun syncUnsyncedDebts()
}
