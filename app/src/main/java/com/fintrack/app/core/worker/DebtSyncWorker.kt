package com.fintrack.app.core.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.WorkerParameters
import com.fintrack.app.core.domain.repository.DebtRepository
import com.fintrack.app.core.domain.repository.NetworkRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Background worker that syncs unsynced debts to Firebase when network is available.
 */
@HiltWorker
class DebtSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val debtRepository: DebtRepository,
    private val networkRepository: NetworkRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "DebtSyncWorker started")

        return try {
            // Double-check network availability
            if (!networkRepository.isNetworkAvailable()) {
                Log.d(TAG, "Network not available, will retry later")
                return Result.retry()
            }

            // Sync all unsynced debts
            debtRepository.syncUnsyncedDebts()
            
            Log.d(TAG, "DebtSyncWorker completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "DebtSyncWorker failed: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "DebtSyncWorker"
        const val WORK_NAME = "DebtSync"

        /**
         * Create work constraints that require network connectivity
         */
        fun getConstraints(): Constraints {
            return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        }
    }
}
