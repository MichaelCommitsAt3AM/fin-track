package com.fintrack.app.core.domain.model

enum class CategoryType {
    INCOME,
    EXPENSE
}

// Represents a category in the app's business logic
data class Category(
    val name: String,
    val userId: String,
    val iconName: String,
    val colorHex: String,
    val type: CategoryType = CategoryType.EXPENSE,
    val isDefault: Boolean
)