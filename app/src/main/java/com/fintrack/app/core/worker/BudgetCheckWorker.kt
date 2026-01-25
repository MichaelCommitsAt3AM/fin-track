package com.fintrack.app.core.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fintrack.app.core.common.NotificationHelper
import com.fintrack.app.core.domain.model.TransactionType
import com.fintrack.app.core.domain.repository.BudgetRepository
import com.fintrack.app.core.domain.repository.TransactionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Calendar

@HiltWorker
class BudgetCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            checkBudgets()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private suspend fun checkBudgets() {
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH) + 1
        val year = calendar.get(Calendar.YEAR)

        // Set start and end of month
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.timeInMillis

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endDate = calendar.timeInMillis

        // Get all budgets for current month
        val budgets = budgetRepository.getAllBudgetsForMonth(month, year).first()
        if (budgets.isEmpty()) return

        // Get all transactions for current month
        val transactions = transactionRepository.getTransactionsByDateRange(startDate, endDate).first()

        // Check each budget
        budgets.forEach { budget ->
            // Filter transactions for this category
            // Note: Category matching should be robust (case insensitive, trim)
            val spent = transactions
                .filter { it.category.equals(budget.categoryName, ignoreCase = true) }
                .sumOf { 
                    when (it.type) {
                        TransactionType.EXPENSE -> it.amount
                        TransactionType.INCOME -> -it.amount
                    }
                }

            if (budget.amount > 0) {
                val percentage = spent / budget.amount
                
                when {
                    percentage >= 1.0 -> {
                        notificationHelper.showBudgetNotification(
                            "Budget Exceeded: ${budget.categoryName}",
                            "🔴 You've spent $${String.format("%,.0f", spent)} of $${String.format("%,.0f", budget.amount)}"
                        )
                    }
                    percentage >= 0.9 -> {
                        notificationHelper.showBudgetNotification(
                            "Budget Alert: ${budget.categoryName}",
                            "⚠️ You've reached 90% of your budget ($${String.format("%,.0f", spent)})"
                        )
                    }
                    percentage >= 0.8 -> {
                        notificationHelper.showBudgetNotification(
                            "Budget Alert: ${budget.categoryName}",
                            "⚠️ You've reached 80% of your budget ($${String.format("%,.0f", spent)})"
                        )
                    }
                     percentage >= 0.7 -> {
                        // Optional: maybe too spammy? Implementing as requested though.
                         notificationHelper.showBudgetNotification(
                            "Budget Alert: ${budget.categoryName}",
                             "⚠️ You've reached 70% of your budget ($${String.format("%,.0f", spent)})"
                        )
                    }
                }
            }
        }
    }
}
