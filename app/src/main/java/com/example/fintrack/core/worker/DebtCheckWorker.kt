package com.example.fintrack.core.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.fintrack.core.common.NotificationHelper
import com.example.fintrack.core.domain.model.DebtType
import com.example.fintrack.core.domain.repository.DebtRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

import com.google.firebase.auth.FirebaseAuth

@HiltWorker
class DebtCheckWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val debtRepository: DebtRepository,
    private val notificationHelper: NotificationHelper,
    private val firebaseAuth: FirebaseAuth
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            checkDebts()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun checkDebts() {
        val userId = firebaseAuth.currentUser?.uid ?: return
        val debts = debtRepository.getAllDebts(userId).first()
        val today = System.currentTimeMillis()
        val prefs = appContext.getSharedPreferences("worker_prefs", Context.MODE_PRIVATE)

        debts.forEach { debt ->
            // Only care about debts I OWE
            if (debt.debtType == DebtType.I_OWE && debt.currentBalance > 0) {
                val daysUntilDue = TimeUnit.MILLISECONDS.toDays(debt.dueDate - today)

                // 1. Upcoming Due Date (7, 3, 1 days)
                if (daysUntilDue == 7L || daysUntilDue == 3L || daysUntilDue == 1L) {
                    val key = "debt_${debt.id}_due_$daysUntilDue"
                    if (!prefs.getBoolean(key, false)) {
                        notificationHelper.showDebtNotification(
                            "Payment Due Reminder",
                            "📅 '${debt.title}' payment of $${String.format("%,.2f", debt.minimumPayment)} due in $daysUntilDue days"
                        )
                        prefs.edit().putBoolean(key, true).apply()
                    }
                }
                
                // 2. Overdue (1 day, 3 days, then weekly)
                if (daysUntilDue < 0) {
                    val overdueDays = -daysUntilDue
                    if (overdueDays == 1L || overdueDays == 3L || overdueDays % 7 == 0L) {
                        val key = "debt_${debt.id}_overdue_$overdueDays"
                        // Weekly reminders shouldn't be blocked forever, but since overdue days changes, the key changes.
                        // So "overdue_7" is different from "overdue_14". This works perfect.
                        
                        if (!prefs.getBoolean(key, false)) {
                             notificationHelper.showDebtNotification(
                                "Payment Overdue!",
                                "🔴 '${debt.title}' payment was due $overdueDays days ago. Please pay ASAP."
                            )
                            prefs.edit().putBoolean(key, true).apply()
                        }
                    }
                }
            }
        }
    }
}
