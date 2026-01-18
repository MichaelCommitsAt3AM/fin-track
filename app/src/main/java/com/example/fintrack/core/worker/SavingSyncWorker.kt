package com.example.fintrack.core.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.WorkerParameters
import com.example.fintrack.core.domain.repository.NetworkRepository
import com.example.fintrack.core.domain.repository.SavingRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Background worker that syncs unsynced savings to Firebase when network is available.
 */
@HiltWorker
class SavingSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val savingRepository: SavingRepository,
    private val networkRepository: NetworkRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "SavingSyncWorker started")

        return try {
            // Double-check network availability
            if (!networkRepository.isNetworkAvailable()) {
                Log.d(TAG, "Network not available, will retry later")
                return Result.retry()
            }

            // Sync all unsynced savings
            savingRepository.syncUnsyncedSavings()
            
            Log.d(TAG, "SavingSyncWorker completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "SavingSyncWorker failed: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "SavingSyncWorker"
        const val WORK_NAME = "SavingSync"

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
