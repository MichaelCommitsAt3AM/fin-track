package com.fintrack.app.core.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fintrack.app.core.domain.model.LookbackPeriod
import com.fintrack.app.core.domain.repository.MpesaTransactionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

import kotlinx.coroutines.flow.first
import com.fintrack.app.core.data.preferences.MpesaOnboardingPreferences

/**
 * WorkManager worker for background M-Pesa SMS synchronization.
 * 
 * This worker scans SMS messages for M-Pesa transactions within a specified
 * lookback period and stores them locally in the database.
 */
@HiltWorker
class MpesaSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val mpesaRepository: MpesaTransactionRepository,
    private val mpesaPreferences: MpesaOnboardingPreferences,
    private val backupScheduler: com.fintrack.app.core.backup.MpesaBackupScheduler
) : CoroutineWorker(appContext, workerParams) {
    
    companion object {
        const val TAG = "MpesaSyncWorker"
        const val WORK_NAME = "mpesa_sync_work"
        const val INPUT_LOOKBACK_MONTHS = "lookback_months"
        
    }
    
    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting M-Pesa SMS sync worker")
        
        return try {
            // An explicit rescan (e.g. from Settings) passes the period to scan.
            // The routine periodic sync passes nothing and only checks recent messages:
            // older ones were already imported, and rescanning years of SMS every
            // few hours would waste battery.
            val lookbackPeriod = if (inputData.keyValueMap.containsKey(INPUT_LOOKBACK_MONTHS)) {
                LookbackPeriod.fromMonths(inputData.getInt(INPUT_LOOKBACK_MONTHS, LookbackPeriod.DEFAULT.months))
            } else {
                LookbackPeriod.ROUTINE_SYNC
            }

            Log.d(TAG, "Syncing with lookback period: $lookbackPeriod")
            
            // Perform sync
            mpesaRepository.syncMpesaSms(lookbackPeriod)
            
            val count = mpesaRepository.getTransactionCount()
            Log.d(TAG, "M-Pesa sync completed successfully. Total transactions: $count")
            backupScheduler.requestRun() // no-op unless Cloud backup is on
            
            Result.success()
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied: SMS permission not granted", e)
            Result.failure()
        } catch (e: Exception) {
            Log.e(TAG, "Error during M-Pesa sync", e)
            
            // Retry on failure
            if (runAttemptCount < 3) {
                Log.d(TAG, "Retrying... Attempt ${runAttemptCount + 1}")
                Result.retry()
            } else {
                Log.e(TAG, "Max retry attempts reached")
                Result.failure()
            }
        }
    }
}
