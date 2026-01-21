package com.example.fintrack.core.util

import android.content.Context
import androidx.work.*
import com.example.fintrack.core.domain.model.LookbackPeriod
import com.example.fintrack.core.worker.MpesaSyncWorker
import java.util.concurrent.TimeUnit

/**
 * Helper class for scheduling M-Pesa SMS synchronization.
 * Provides convenient methods for one-time and periodic sync operations.
 */
object MpesaSyncScheduler {
    
    /**
     * Schedule a one-time M-Pesa SMS sync.
     * 
     * @param context Application context
     * @param lookbackPeriod How far back to scan for M-Pesa SMS
     * @param requiresCharging Whether to wait for device to be charging
     * @param requiresDeviceIdle Whether to wait for device to be idle
     */
    fun scheduleOneTimeSync(
        context: Context,
        lookbackPeriod: LookbackPeriod = LookbackPeriod.THREE_MONTHS,
        requiresCharging: Boolean = false,
        requiresDeviceIdle: Boolean = false
    ) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED) // No network needed
            .setRequiresCharging(requiresCharging)
            .setRequiresDeviceIdle(requiresDeviceIdle)
            .build()
        
        val inputData = workDataOf(
            MpesaSyncWorker.INPUT_LOOKBACK_MONTHS to lookbackPeriod.months
        )
        
        val syncRequest = OneTimeWorkRequestBuilder<MpesaSyncWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()
        
        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                MpesaSyncWorker.WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                syncRequest
            )
    }
    
    /**
     * Schedule a periodic M-Pesa SMS sync (daily).
     * 
     * @param context Application context
     * @param lookbackPeriod How far back to scan each time
     */
    fun schedulePeriodicSync(
        context: Context,
        lookbackPeriod: LookbackPeriod = LookbackPeriod.ONE_MONTH
    ) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresCharging(true) // Only when charging for battery efficiency
            .build()
        
        val inputData = workDataOf(
            MpesaSyncWorker.INPUT_LOOKBACK_MONTHS to lookbackPeriod.months
        )
        
        val syncRequest = PeriodicWorkRequestBuilder<MpesaSyncWorker>(
            1, TimeUnit.DAYS // Run once daily
        )
            .setConstraints(constraints)
            .setInputData(inputData)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()
        
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "${MpesaSyncWorker.WORK_NAME}_periodic",
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
    }
    
    /**
     * Cancel all M-Pesa sync work.
     */
    fun cancelAllSync(context: Context) {
        WorkManager.getInstance(context)
            .cancelUniqueWork(MpesaSyncWorker.WORK_NAME)
        WorkManager.getInstance(context)
            .cancelUniqueWork("${MpesaSyncWorker.WORK_NAME}_periodic")
    }
}
