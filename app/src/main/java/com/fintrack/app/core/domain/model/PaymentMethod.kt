package com.fintrack.app.core.domain.model

data class PaymentMethod(
    val name: String,
    val iconName: String,
    val colorHex: String,
    val isDefault: Boolean = false,
    val isActive: Boolean = true
)
