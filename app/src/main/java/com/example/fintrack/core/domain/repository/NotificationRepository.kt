package com.example.fintrack.core.domain.repository

import com.example.fintrack.core.domain.model.Notification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun getAllNotifications(): Flow<List<Notification>>
    fun getNotificationsByType(type: String): Flow<List<Notification>>
    suspend fun insertNotification(notification: Notification)
    suspend fun markAsRead(id: String, isRead: Boolean = true)
    suspend fun markAllAsRead()
    suspend fun deleteNotification(id: String)
    suspend fun clearAll()
    fun getUnreadCount(): Flow<Int>
}
