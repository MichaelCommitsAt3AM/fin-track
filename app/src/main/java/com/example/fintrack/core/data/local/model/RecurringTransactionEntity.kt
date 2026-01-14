package com.example.fintrack.core.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recurring_transactions")
data class RecurringTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val type: String, // Store Enum as String
    val amount: Double,
    val category: String,
    val startDate: Long,
    val frequency: String, // Store Enum as String
    val notes: String?
)