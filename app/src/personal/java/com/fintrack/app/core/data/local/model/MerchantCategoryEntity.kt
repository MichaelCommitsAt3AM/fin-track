package com.fintrack.app.core.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores user-defined category mappings for M-Pesa merchants/paybills.
 */
@Entity(tableName = "merchant_categories")
data class MerchantCategoryEntity(
    @PrimaryKey
    val merchantName: String,
    val categoryName: String,
    val isUserConfirmed: Boolean,
    val updatedAt: Long
)
