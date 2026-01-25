package com.fintrack.app.core.domain.model

data class Contribution(
    val id: String,
    val savingId: String,
    val amount: Double,
    val date: Long,
    val note: String?
)
