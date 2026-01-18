package com.example.fintrack.core.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "savings")
data class SavingEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val title: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val targetDate: Long,
    val notes: String?,
    val iconName: String, // Store icon identifier as string
    val createdAt: Long,
    val isSynced: Boolean = true,
    val deletedAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
