package com.fintrack.app.core.backup

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Schedules [MpesaBackupWorker]: daily, plus on request after an M-Pesa sync or category change. */
@Singleton
class MpesaBackupScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: MpesaBackupPrefs
) {
    private val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    /** Daily run (a no-op while the backup is off). Safe to call on every app start. */
    fun schedulePeriodic() {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<MpesaBackupWorker>(1, TimeUnit.DAYS).setConstraints(constraints).build()
        )
    }

    /**
     * One run soon. Cheap to call often: it does nothing while the backup is off, and runs
     * collapse into a single pending request (KEEP) with a short delay so bursts are batched.
     */
    fun requestRun() {
        if (!prefs.state.value.enabled) return
        WorkManager.getInstance(context).enqueueUniqueWork(
            ONCE_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<MpesaBackupWorker>()
                .setConstraints(constraints)
                .setInitialDelay(15, TimeUnit.SECONDS)
                .build()
        )
    }

    private companion object {
        const val PERIODIC_NAME = "MpesaBackupPeriodic"
        const val ONCE_NAME = "MpesaBackupOnce"
    }
}

@HiltWorker
class MpesaBackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val manager: MpesaBackupManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = try {
        // Returns 0 without doing anything when the backup is off or there is no linked account
        manager.backupNow()
        Result.success()
    } catch (e: Exception) {
        if (runAttemptCount < 3) Result.retry() else Result.failure()
    }
}
