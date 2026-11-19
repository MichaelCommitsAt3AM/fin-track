package com.fintrack.app.core.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class MpesaWorkerScheduler @Inject constructor(
    private val backupScheduler: com.fintrack.app.core.backup.MpesaBackupScheduler
) : WorkerScheduler {
    override fun schedule(context: Context) {
        // Daily encrypted M-Pesa backup (does nothing unless the user turned Cloud backup on)
        backupScheduler.schedulePeriodic()

        val workManager = WorkManager.getInstance(context)
        
        // Schedule Monthly M-Pesa Cost Check (Daily check)
        // Checks if a new month has started and sends a report for the previous month
        val mpesaCostWork = PeriodicWorkRequestBuilder<MonthlyMpesaCostWorker>(1, TimeUnit.DAYS)
            .build()
            
        workManager.enqueueUniquePeriodicWork(
            MonthlyMpesaCostWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            mpesaCostWork
        )

        // Schedule Periodic M-Pesa Transaction Sync (Every 6 hours)
        val syncWork = PeriodicWorkRequestBuilder<MpesaSyncWorker>(6, TimeUnit.HOURS)
            .build()
            
        workManager.enqueueUniquePeriodicWork(
            MpesaSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncWork
        )
    }
}
