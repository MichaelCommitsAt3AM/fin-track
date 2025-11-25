package com.example.fintrack.core.domain.model

data class User(
    val userId: String,
    val fullName: String,
    val email: String,
    val phoneNumber: String = "",
    val avatarId: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
