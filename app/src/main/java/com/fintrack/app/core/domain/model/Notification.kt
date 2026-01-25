package com.fintrack.app.core.domain.model

import java.time.LocalDateTime

data class Notification(
    val id: String,
    val title: String,
    val message: String,
    val type: NotificationType,
    val timestamp: LocalDateTime,
    val isRead: Boolean,
    val iconType: String
)

enum class NotificationType {
    BUDGET,
    GOAL,
    DEBT,
    ACTIVITY,
    SUGGESTION
}
