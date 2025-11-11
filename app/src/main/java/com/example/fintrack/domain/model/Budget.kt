package com.example.fintrack.domain.model

// Represents a budget in the app's business logic
data class Budget(
    val categoryName: String,
    val amount: Double,
    val month: Int,
    val year: Int
)