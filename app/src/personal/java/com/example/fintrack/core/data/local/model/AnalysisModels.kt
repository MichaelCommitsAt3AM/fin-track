package com.example.fintrack.core.data.local.model

import androidx.room.ColumnInfo

/**
 * Result of merchant frequency analysis query.
 */
data class MerchantFrequencyResult(
    @ColumnInfo(name = "merchantName") val merchantName: String,
    @ColumnInfo(name = "frequency") val frequency: Int,
    @ColumnInfo(name = "totalAmount") val totalAmount: Double
)

/**
 * Result of paybill analysis query.
 */
data class PaybillAnalysisResult(
    @ColumnInfo(name = "paybillNumber") val paybillNumber: String,
    @ColumnInfo(name = "merchantName") val merchantName: String?,
    @ColumnInfo(name = "frequency") val frequency: Int,
    @ColumnInfo(name = "avgAmount") val avgAmount: Double
)
