package com.fintrack.app.core.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fintrack.app.core.common.NotificationHelper
import com.fintrack.app.core.domain.repository.SavingRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

import com.google.firebase.auth.FirebaseAuth

@HiltWorker
class GoalCheckWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val savingRepository: SavingRepository,
    private val notificationHelper: NotificationHelper,
    private val firebaseAuth: FirebaseAuth
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            checkGoals()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun checkGoals() {
        val userId = firebaseAuth.currentUser?.uid ?: return
        val savings = savingRepository.getAllSavings(userId).first()
        val today = System.currentTimeMillis()
        
        val prefs = appContext.getSharedPreferences("worker_prefs", Context.MODE_PRIVATE)

        savings.forEach { saving ->
            val daysLeft = TimeUnit.MILLISECONDS.toDays(saving.targetDate - today)
            
            // 1. Deadline Reminders (7, 3, 1 days)
            if (saving.currentAmount < saving.targetAmount) {
                if (daysLeft == 7L || daysLeft == 3L || daysLeft == 1L) {
                    val key = "goal_${saving.id}_deadline_$daysLeft"
                    if (!prefs.getBoolean(key, false)) {
                        notificationHelper.showGoalNotification(
                            "Goal Deadline Approaching",
                            "⏰ $daysLeft days left to reach '${saving.title}'! Need $${String.format("%,.0f", saving.targetAmount - saving.currentAmount)} more."
                        )
                        prefs.edit().putBoolean(key, true).apply()
                    }
                }
            }

            // 2. Milestone Achievements
            if (saving.targetAmount > 0) {
                 val percentage = saving.currentAmount / saving.targetAmount
                 
                 // 100% Complete
                 if (percentage >= 1.0) {
                     val key = "goal_${saving.id}_100"
                     if (!prefs.getBoolean(key, false)) {
                         notificationHelper.showGoalNotification(
                             "Goal Achieved! \uD83C\uDF89",
                             "Congratulations! You've reached your goal for '${saving.title}'!"
                         )
                         prefs.edit().putBoolean(key, true).apply()
                     }
                 }
                 // 75% Milestone
                 else if (percentage >= 0.75) {
                     val key = "goal_${saving.id}_75"
                     if (!prefs.getBoolean(key, false)) {
                         notificationHelper.showGoalNotification(
                             "Almost There!",
                             "You're 75% of the way to '${saving.title}'. Keep it up!"
                         )
                         prefs.edit().putBoolean(key, true).apply()
                     }
                 }
                 // 50% Milestone
                 else if (percentage >= 0.50) {
                     val key = "goal_${saving.id}_50"
                     if (!prefs.getBoolean(key, false)) {
                         notificationHelper.showGoalNotification(
                             "Halfway There!",
                             "You've reached 50% of your '${saving.title}' goal!"
                         )
                         prefs.edit().putBoolean(key, true).apply()
                     }
                 }
            }
        }
    }
}
