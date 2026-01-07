package com.example.fintrack.presentation.home

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class UserUiModel(
    val fullName: String,
    val email: String,
    val avatarId: Int
)

data class TransactionUiModel(
    val name: String,
    val category: String,
    val date: String,
    val amount: Double,
    val icon: ImageVector,
    val color: Color
)

data class SpendingCategoryUiModel(
    val name: String,
    val amount: String,
    val icon: ImageVector,
    val color: Color
)
