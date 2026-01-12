package com.example.fintrack.core.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey
    val id: String,
    val debtId: String, // Foreign key reference to DebtEntity
    val amount: Double,
    val date: Long,
    val note: String?
)
