package com.example.fintrack.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// Defines the "transactions" table in the Room database
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val type: String, // "INCOME" or "EXPENSE"
    val amount: Double,
    val category: String,
    val date: Long, // Store as a timestamp for easy sorting/filtering
    val notes: String?,
    val paymentMethod: String?,
    val tags: List<String>? = null // Room can handle simple lists
)