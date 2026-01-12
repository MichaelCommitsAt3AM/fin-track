package com.example.fintrack.core.common

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.fintrack.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

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
        showNotification(CHANNEL_BUDGET_ID, NOTIFICATION_ID_BUDGET + System.currentTimeMillis().toInt() % 100, title, message)
    }

    fun showGoalNotification(title: String, message: String) {
        showNotification(CHANNEL_GOALS_ID, NOTIFICATION_ID_GOAL + System.currentTimeMillis().toInt() % 100, title, message)
    }

    fun showDebtNotification(title: String, message: String) {
        showNotification(CHANNEL_DEBT_ID, NOTIFICATION_ID_DEBT + System.currentTimeMillis().toInt() % 100, title, message)
    }

    private fun showNotification(channelId: String, notificationId: Int, title: String, message: String) {
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
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use app icon or specific icon
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
