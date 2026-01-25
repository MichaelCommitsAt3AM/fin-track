package com.fintrack.app.core.data.repository

import com.fintrack.app.core.data.local.dao.NotificationDao
import com.fintrack.app.core.data.local.entity.NotificationEntity
import com.fintrack.app.core.domain.model.Notification
import com.fintrack.app.core.domain.model.NotificationType
import com.fintrack.app.core.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val notificationDao: NotificationDao
) : NotificationRepository {

    override fun getAllNotifications(): Flow<List<Notification>> {
        return notificationDao.getAllNotifications().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getNotificationsByType(type: String): Flow<List<Notification>> {
        return notificationDao.getNotificationsByType(type).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun insertNotification(notification: Notification) {
        notificationDao.insertNotification(notification.toEntity())
    }

    override suspend fun markAsRead(id: String, isRead: Boolean) {
        notificationDao.markAsRead(id, isRead)
    }

    override suspend fun markAllAsRead() {
        notificationDao.markAllAsRead()
    }

    override suspend fun deleteNotification(id: String) {
        notificationDao.deleteNotification(id)
    }

    override suspend fun clearAll() {
        notificationDao.clearAll()
    }

    override fun getUnreadCount(): Flow<Int> {
        return notificationDao.getUnreadCount()
    }

    // Mapper functions
    private fun NotificationEntity.toDomain(): Notification {
        return Notification(
            id = id,
            title = title,
            message = message,
            type = NotificationType.valueOf(type),
            timestamp = timestamp,
            isRead = isRead,
            iconType = iconType
        )
    }

    private fun Notification.toEntity(): NotificationEntity {
        return NotificationEntity(
            id = id,
            title = title,
            message = message,
            type = type.name,
            timestamp = timestamp,
            isRead = isRead,
            iconType = iconType
        )
    }
}
