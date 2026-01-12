package com.example.fintrack

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.fintrack.core.worker.BudgetCheckWorker
import com.example.fintrack.core.worker.DebtCheckWorker
import com.example.fintrack.core.worker.GoalCheckWorker
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