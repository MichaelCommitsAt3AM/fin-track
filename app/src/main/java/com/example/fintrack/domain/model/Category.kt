package com.example.fintrack.domain.model

// Represents a category in the app's business logic
data class Category(
    val name: String,
    val iconName: String,
    val colorHex: String,
    val isDefault: Boolean
)