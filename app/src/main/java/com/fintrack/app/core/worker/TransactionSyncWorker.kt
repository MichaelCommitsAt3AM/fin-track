package com.fintrack.app.core.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.WorkerParameters
import com.fintrack.app.core.domain.repository.NetworkRepository
import com.fintrack.app.core.domain.repository.TransactionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Background worker that syncs unsynced transactions to Firebase when network is available.
 * This worker is scheduled to run when the app starts and when network connectivity is restored.
 */
@HiltWorker
class TransactionSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val transactionRepository: TransactionRepository,
    private val networkRepository: NetworkRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "TransactionSyncWorker started")

        return try {
            // Double-check network availability
            if (!networkRepository.isNetworkAvailable()) {
                Log.d(TAG, "Network not available, will retry later")
                return Result.retry()
            }

            // Sync all unsynced transactions
            transactionRepository.syncUnsyncedTransactions()
            
            Log.d(TAG, "TransactionSyncWorker completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "TransactionSyncWorker failed: ${e.message}", e)
            // Retry on failure
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "TransactionSyncWorker"
        const val WORK_NAME = "TransactionSync"

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
