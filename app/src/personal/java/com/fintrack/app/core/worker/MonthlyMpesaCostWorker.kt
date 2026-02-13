package com.fintrack.app.core.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fintrack.app.core.common.NotificationHelper
import com.fintrack.app.core.domain.repository.MpesaTransactionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.ZoneId

@HiltWorker
class MonthlyMpesaCostWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val mpesaRepository: MpesaTransactionRepository,
    private val notificationHelper: NotificationHelper,
    private val timeProvider: com.fintrack.app.core.common.TimeProvider
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "MonthlyMpesaCostWorker"
        private const val PREFS_NAME = "mpesa_monthly_report_prefs"
        private const val KEY_LAST_REPORT_MONTH = "last_report_month" // Format: yyyy-MM
    }

    override suspend fun doWork(): Result {
        return try {
            checkAndSendMonthlyReport()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private suspend fun checkAndSendMonthlyReport() {
        val today = timeProvider.currentDate()
        // We want to report on the PREVIOUS month
        val previousMonth = today.minusMonths(1)
        val reportMonthKey = previousMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))

        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastReportedMonth = sharedPrefs.getString(KEY_LAST_REPORT_MONTH, "")

        // If we already reported for this previous month, do nothing
        if (lastReportedMonth == reportMonthKey) {
            return
        }

        // Calculate start and end timestamps for the previous month
        val zoneId = ZoneId.systemDefault()
        
        val startOfPreviousMonth = previousMonth.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endOfPreviousMonth = previousMonth.with(TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 59).atZone(zoneId).toInstant().toEpochMilli()

        // 1. The "First Run" Problem: Check if we have data covering the entire month
        val oldestTransactionTimestamp = mpesaRepository.getOldestTransactionTimestamp()
        if (oldestTransactionTimestamp != null && oldestTransactionTimestamp > startOfPreviousMonth) {
            // We don't have full data for this month, skip reporting to avoid inaccurate "low spend" reports
            // But mark as reported so we don't keep checking
            sharedPrefs.edit().putString(KEY_LAST_REPORT_MONTH, reportMonthKey).apply()
            return
        }

        // Fetch transactions for previous month
        // Note: first() is used because getTransactionsByDateRange returns a Flow
        val transactions = mpesaRepository.getTransactionsByDateRange(startOfPreviousMonth, endOfPreviousMonth).first()
        val totalCost = transactions.sumOf { it.transactionCost ?: 0.0 }
        
        val monthName = previousMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }
        val title = "Monthly M-Pesa Cost Fact"
        var message = ""

        if (totalCost > 0) {
            message = "Did you know? You spent KSH ${String.format("%.2f", totalCost)} on M-Pesa fees in $monthName."
            
            // 4. Notification UX: Add Trends
            // Compare with the month BEFORE previous
            val monthBeforePrevious = previousMonth.minusMonths(1)
            val startOfMonthBefore = monthBeforePrevious.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay(zoneId).toInstant().toEpochMilli()
            val endOfMonthBefore = monthBeforePrevious.with(TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 59).atZone(zoneId).toInstant().toEpochMilli()
            
            val prevTransactions = mpesaRepository.getTransactionsByDateRange(startOfMonthBefore, endOfMonthBefore).first()
            val prevTotalCost = prevTransactions.sumOf { it.transactionCost ?: 0.0 }
            
            if (prevTotalCost > 0) {
                val diff = totalCost - prevTotalCost
                val percentChange = (diff / prevTotalCost) * 100
                
                if (Math.abs(percentChange) >= 5) { // Only report significant changes
                    val trend = if (percentChange > 0) "up" else "down"
                    message += " That's ${String.format("%.0f", Math.abs(percentChange))}% $trend from last month."
                }
            }
        } else {
            // 3. Edge case: zero-cost months
            message = "Great job! You spent KSH 0.00 on M-Pesa fees in $monthName! \uD83C\uDF89"
        }

        notificationHelper.showReportNotification(
            title = title,
            message = message
        )

        // Mark this month as reported
        sharedPrefs.edit().putString(KEY_LAST_REPORT_MONTH, reportMonthKey).apply()
    }
}
