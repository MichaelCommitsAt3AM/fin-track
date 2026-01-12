package com.example.fintrack.core.domain.model

data class Payment(
    val id: String,
    val debtId: String,
    val amount: Double,
    val date: Long,
    val note: String?
)
