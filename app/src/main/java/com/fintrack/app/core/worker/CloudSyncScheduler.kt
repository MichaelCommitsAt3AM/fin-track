package com.fintrack.app.core.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.fintrack.app.core.profile.SyncGate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enqueues the cloud sync workers whenever sync becomes possible (account linked, Cloud sync on,
 * signed in) and cancels them when it stops. Replaces the unconditional scheduling that used to
 * live in [com.fintrack.app.FinTrackApplication].
 */
@Singleton
class CloudSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncGate: SyncGate
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var started = false

    /** Call once from Application.onCreate. */
    @Synchronized
    fun start() {
        if (started) return
        started = true
        scope.launch {
            syncGate.canSync.distinctUntilChanged().collect { canSync ->
                if (canSync) enqueueAll() else cancelAll()
            }
        }
    }

    private fun enqueueAll() {
        val workManager = WorkManager.getInstance(context)
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        listOf(
            TransactionSyncWorker.WORK_NAME to OneTimeWorkRequestBuilder<TransactionSyncWorker>(),
            BudgetSyncWorker.WORK_NAME to OneTimeWorkRequestBuilder<BudgetSyncWorker>(),
            SavingSyncWorker.WORK_NAME to OneTimeWorkRequestBuilder<SavingSyncWorker>(),
            DebtSyncWorker.WORK_NAME to OneTimeWorkRequestBuilder<DebtSyncWorker>()
        ).forEach { (name, builder) ->
            workManager.enqueueUniqueWork(name, ExistingWorkPolicy.REPLACE, builder.setConstraints(constraints).build())
        }
    }

    private fun cancelAll() {
        val workManager = WorkManager.getInstance(context)
        listOf(
            TransactionSyncWorker.WORK_NAME,
            BudgetSyncWorker.WORK_NAME,
            SavingSyncWorker.WORK_NAME,
            DebtSyncWorker.WORK_NAME
        ).forEach { workManager.cancelUniqueWork(it) }
    }
}
