package com.example.fintrack.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.fintrack.core.data.local.model.PaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)

    @Query("SELECT * FROM payments WHERE debtId = :debtId ORDER BY date DESC")
    fun getPaymentsForDebt(debtId: String): Flow<List<PaymentEntity>>

    @Query("DELETE FROM payments WHERE id = :paymentId")
    suspend fun deletePayment(paymentId: String)

    @Query("DELETE FROM payments WHERE debtId = :debtId")
    suspend fun deleteAllForDebt(debtId: String)
}
