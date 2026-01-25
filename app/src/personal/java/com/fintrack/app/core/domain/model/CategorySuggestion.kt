package com.fintrack.app.core.domain.model

/**
 * Represents a suggested category based on M-Pesa transaction analysis.
 * Used during onboarding to present auto-categorization options to users.
 */
data class CategorySuggestion(
    val categoryName: String,
    val iconName: String,
    val colorHex: String,
    val transactionCount: Int,
    val totalAmount: Double,
    val mpesaReceiptNumbers: List<String> // M-Pesa transactions that would be assigned
)
