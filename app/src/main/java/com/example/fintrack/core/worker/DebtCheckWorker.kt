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
    @Assisted appContext: Context,
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

        debts.forEach { debt ->
            // Only care about debts I OWE
            if (debt.debtType == DebtType.I_OWE && debt.currentBalance > 0) {
                val daysUntilDue = TimeUnit.MILLISECONDS.toDays(debt.dueDate - today)

                // 7 days, 3 days, 1 day
                if (daysUntilDue == 7L || daysUntilDue == 3L || daysUntilDue == 1L) {
                    notificationHelper.showDebtNotification(
                        "Payment Due Reminder",
                        "📅 ${debt.title} payment of $${String.format("%,.2f", debt.minimumPayment)} due in $daysUntilDue days"
                    )
                }
                
                // Overdue
                if (daysUntilDue < 0) {
                     notificationHelper.showDebtNotification(
                        "Payment Overdue!",
                        "🔴 ${debt.title} payment was due ${-daysUntilDue} days ago."
                    )
                }
            }
        }
    }
}
