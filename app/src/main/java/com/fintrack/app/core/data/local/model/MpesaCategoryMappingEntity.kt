package com.fintrack.app.core.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for storing M-Pesa transaction to category mappings.
 * This allows M-Pesa transactions to maintain their original data
 * while having explicit category assignments.
 */
@Entity(tableName = "mpesa_category_mappings")
data class MpesaCategoryMappingEntity(
    @PrimaryKey
    val mpesaReceiptNumber: String,
    val categoryName: String,
    val assignedAt: Long = System.currentTimeMillis()
)
