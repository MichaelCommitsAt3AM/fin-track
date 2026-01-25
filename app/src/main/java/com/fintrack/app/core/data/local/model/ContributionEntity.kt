package com.fintrack.app.core.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contributions")
data class ContributionEntity(
    @PrimaryKey
    val id: String,
    val savingId: String, // Foreign key reference to SavingEntity
    val amount: Double,
    val date: Long,
    val note: String?
)
