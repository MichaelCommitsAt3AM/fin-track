package com.fintrack.app.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fintrack.app.core.data.local.model.PaymentMethodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentMethodDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentMethod(paymentMethod: PaymentMethodEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(paymentMethods: List<PaymentMethodEntity>)

    @Update
    suspend fun updatePaymentMethod(paymentMethod: PaymentMethodEntity)

    @Query("SELECT * FROM payment_methods WHERE userId = :userId AND isActive = 1 ORDER BY isDefault DESC, name ASC")
    fun getAllPaymentMethods(userId: String): Flow<List<PaymentMethodEntity>>

    @Query("SELECT * FROM payment_methods WHERE userId = :userId AND name = :name")
    suspend fun getPaymentMethodByName(userId: String, name: String): PaymentMethodEntity?

    @Query("SELECT * FROM payment_methods WHERE userId = :userId AND isDefault = 1 LIMIT 1")
    suspend fun getDefaultPaymentMethod(userId: String): PaymentMethodEntity?

    @Query("UPDATE payment_methods SET isDefault = 0 WHERE userId = :userId")
    suspend fun clearDefaultPaymentMethod(userId: String)

    @Query("UPDATE payment_methods SET isDefault = 1 WHERE userId = :userId AND name = :name")
    suspend fun setDefaultPaymentMethod(userId: String, name: String)

    @Query("UPDATE payment_methods SET isActive = 0 WHERE userId = :userId AND name = :name")
    suspend fun deactivatePaymentMethod(userId: String, name: String)

    @Query("DELETE FROM payment_methods WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}
