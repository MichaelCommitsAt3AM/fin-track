package com.fintrack.app.core.common

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.fintrack.app.R
import com.fintrack.app.core.domain.model.Notification
import com.fintrack.app.core.domain.model.NotificationType
import com.fintrack.app.core.domain.repository.NotificationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationRepository: NotificationRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val CHANNEL_BUDGET_ID = "budget_alerts"
        const val CHANNEL_GOALS_ID = "goal_updates"
        const val CHANNEL_DEBT_ID = "debt_reminders"
        
        const val NOTIFICATION_ID_BUDGET = 100
        const val NOTIFICATION_ID_GOAL = 200
        const val NOTIFICATION_ID_DEBT = 300
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT

            // Budget Channel
            val budgetChannel = NotificationChannel(
                CHANNEL_BUDGET_ID,
                "Budget Alerts",
                importance
            ).apply {
                description = "Notifications for budget thresholds and limits"
            }

            // Goals Channel
            val goalsChannel = NotificationChannel(
                CHANNEL_GOALS_ID,
                "Savings Goals",
                importance
            ).apply {
                description = "Updates on your savings goals progress"
            }

            // Debt Channel
            val debtChannel = NotificationChannel(
                CHANNEL_DEBT_ID,
                "Debt Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for debt payments"
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannels(listOf(budgetChannel, goalsChannel, debtChannel))
        }
    }

    fun showBudgetNotification(title: String, message: String) {
        showNotification(
            channelId = CHANNEL_BUDGET_ID,
            notificationId = NOTIFICATION_ID_BUDGET + System.currentTimeMillis().toInt() % 100,
            title = title,
            message = message,
            type = NotificationType.BUDGET,
            iconType = "warning"
        )
    }

    fun showGoalNotification(title: String, message: String) {
        showNotification(
            channelId = CHANNEL_GOALS_ID,
            notificationId = NOTIFICATION_ID_GOAL + System.currentTimeMillis().toInt() % 100,
            title = title,
            message = message,
            type = NotificationType.GOAL,
            iconType = "lightbulb"
        )
    }

    fun showDebtNotification(title: String, message: String) {
        showNotification(
            channelId = CHANNEL_DEBT_ID,
            notificationId = NOTIFICATION_ID_DEBT + System.currentTimeMillis().toInt() % 100,
            title = title,
            message = message,
            type = NotificationType.DEBT,
            iconType = "receipt"
        )
    }

    private fun showNotification(
        channelId: String,
        notificationId: Int,
        title: String,
        message: String,
        type: NotificationType,
        iconType: String
    ) {
        // Save notification to database
        scope.launch {
            val notification = Notification(
                id = UUID.randomUUID().toString(),
                title = title,
                message = message,
                type = type,
                timestamp = LocalDateTime.now(),
                isRead = false,
                iconType = iconType
            )
            notificationRepository.insertNotification(notification)
        }

        // Show system notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))

        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, builder.build())
        }
    }
}
