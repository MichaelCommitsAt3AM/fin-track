package com.example.fintrack.core.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val title: String,
    val originalAmount: Double,
    val currentBalance: Double,
    val minimumPayment: Double,
    val dueDate: Long,
    val interestRate: Double,
    val notes: String?,
    val iconName: String, // Store icon identifier as string
    val debtType: String, // "I_OWE" or "OWED_TO_ME"
    val createdAt: Long,
    val isSynced: Boolean = true,
    val deletedAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
