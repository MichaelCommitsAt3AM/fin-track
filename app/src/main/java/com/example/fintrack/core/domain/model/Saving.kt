package com.example.fintrack.core.domain.model

data class Saving(
    val id: String,
    val userId: String,
    val title: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val targetDate: Long,
    val notes: String?,
    val iconName: String,
    val createdAt: Long
)
