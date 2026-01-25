package com.fintrack.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.fintrack.app.core.worker.BudgetCheckWorker
import com.fintrack.app.core.worker.DebtCheckWorker
import com.fintrack.app.core.worker.GoalCheckWorker
import com.fintrack.app.core.worker.TransactionSyncWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class FinTrackApplication : Application(), Configuration.Provider {
    
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        scheduleWorkers()
    }

    private fun scheduleWorkers() {
        val workManager = WorkManager.getInstance(this)

        // Schedule Transaction Sync Worker (runs when network is available)
        // This syncs any transactions that were added while offline
        val syncConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val transactionSyncWork = OneTimeWorkRequestBuilder<TransactionSyncWorker>()
            .setConstraints(syncConstraints)
            .build()
        
        workManager.enqueueUniqueWork(
            TransactionSyncWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            transactionSyncWork
        )

        // Schedule Budget Sync Worker
        val budgetSyncWork = OneTimeWorkRequestBuilder<com.fintrack.app.core.worker.BudgetSyncWorker>()
            .setConstraints(syncConstraints)
            .build()
            
        workManager.enqueueUniqueWork(
            com.fintrack.app.core.worker.BudgetSyncWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            budgetSyncWork
        )

        // Schedule Saving Sync Worker
        val savingSyncWork = OneTimeWorkRequestBuilder<com.fintrack.app.core.worker.SavingSyncWorker>()
            .setConstraints(syncConstraints)
            .build()
            
        workManager.enqueueUniqueWork(
            com.fintrack.app.core.worker.SavingSyncWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            savingSyncWork
        )

        // Schedule Debt Sync Worker
        val debtSyncWork = OneTimeWorkRequestBuilder<com.fintrack.app.core.worker.DebtSyncWorker>()
            .setConstraints(syncConstraints)
            .build()
            
        workManager.enqueueUniqueWork(
            com.fintrack.app.core.worker.DebtSyncWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            debtSyncWork
        )

        // Schedule Budget Check (Daily)
        val budgetWork = PeriodicWorkRequestBuilder<BudgetCheckWorker>(1, TimeUnit.DAYS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            "BudgetCheck",
            ExistingPeriodicWorkPolicy.KEEP,
            budgetWork
        )

        // Schedule Goal Check (Daily)
        val goalWork = PeriodicWorkRequestBuilder<GoalCheckWorker>(1, TimeUnit.DAYS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            "GoalCheck",
            ExistingPeriodicWorkPolicy.KEEP,
            goalWork
        )

        // Schedule Debt Check (Daily)
        val debtWork = PeriodicWorkRequestBuilder<DebtCheckWorker>(1, TimeUnit.DAYS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            "DebtCheck",
            ExistingPeriodicWorkPolicy.KEEP,
            debtWork
        )
    }
}