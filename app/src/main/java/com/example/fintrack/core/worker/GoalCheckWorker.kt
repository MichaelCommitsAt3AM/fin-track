package com.example.fintrack.core.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.fintrack.core.common.NotificationHelper
import com.example.fintrack.core.domain.repository.SavingRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

import com.google.firebase.auth.FirebaseAuth

@HiltWorker
class GoalCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
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

        savings.forEach { saving ->
            // 1. Check Deadline
            val daysLeft = TimeUnit.MILLISECONDS.toDays(saving.targetDate - today)
            
            if (daysLeft in 1..7 && saving.currentAmount < saving.targetAmount) {
                 notificationHelper.showGoalNotification(
                    "Goal Deadline Approaching",
                    "⏰ $daysLeft days left to reach your ${saving.title} goal! $${String.format("%,.0f", saving.targetAmount - saving.currentAmount)} to go"
                )
            }

            // 2. Milestone Achievements (Simplistic check - ideally needs history to know if JUST reached)
            // Ideally we'd store "last notified percentage" in local storage or SharedPreferences
            // For now, we omit repeated milestone alerts to avoid spam, or simplistic check:
            if (saving.targetAmount > 0) {
                 val percentage = saving.currentAmount / saving.targetAmount
                 if (percentage >= 1.0) {
                      // Goal Achieved - logic to only show once needed? 
                      // For now, assuming user will delete or mark complete, or accept occasional reminders
                     if (percentage >= 1.0 && saving.currentAmount < saving.targetAmount + 100) { // Just reached?
                         // Hard to detect "just reached" without state. 
                         // Check implies we might notify daily if 100%. 
                         // Improvement: We should really check a "completed" flag or last notification time.
                     }
                 }
            }
            
            // 3. Contribution Reminder (2 weeks inactivity)
            // Implementation requires getting contributions history.
            // Assuming we check `lastContributionDate` if available or fetch contributions.
            // savingRepository.getContributionsForSaving(saving.id).first().maxByOrNull { it.date }
            
        }
    }
}
