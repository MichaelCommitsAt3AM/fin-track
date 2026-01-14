package com.example.fintrack.core.data.local.model

import androidx.room.Entity

@Entity(
    tableName = "payment_methods",
    primaryKeys = ["name", "userId"]
)
data class PaymentMethodEntity(
    val name: String,
    val userId: String,
    val iconName: String,
    val colorHex: String,
    val isDefault: Boolean = false,
    val isActive: Boolean = true
)
