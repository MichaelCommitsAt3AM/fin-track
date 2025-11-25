package com.example.fintrack.core.domain.model

// Represents a budget in the app's business logic
data class Budget(
    val categoryName: String,
    val userId: String,
    val amount: Double,
    val month: Int,
    val year: Int
)