package com.example.fintrack.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val message: String,
    val type: String, // BUDGET, GOAL, DEBT, ACTIVITY, SUGGESTION
    val timestamp: LocalDateTime,
    val isRead: Boolean,
    val iconType: String
)
