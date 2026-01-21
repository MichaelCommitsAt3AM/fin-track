package com.example.fintrack.core.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.fintrack.core.domain.model.LookbackPeriod
import com.example.fintrack.core.domain.repository.MpesaTransactionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

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
    private val mpesaRepository: MpesaTransactionRepository
) : CoroutineWorker(appContext, workerParams) {
    
    companion object {
        const val TAG = "MpesaSyncWorker"
        const val WORK_NAME = "mpesa_sync_work"
        const val INPUT_LOOKBACK_MONTHS = "lookback_months"
        
        // Default lookback period if not specified
        const val DEFAULT_LOOKBACK_MONTHS = 3
    }
    
    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting M-Pesa SMS sync worker")
        
        return try {
            // Get lookback period from input data
            val lookbackMonths = inputData.getInt(INPUT_LOOKBACK_MONTHS, DEFAULT_LOOKBACK_MONTHS)
            val lookbackPeriod = when (lookbackMonths) {
                1 -> LookbackPeriod.ONE_MONTH
                6 -> LookbackPeriod.SIX_MONTHS
                12 -> LookbackPeriod.ONE_YEAR
                else -> LookbackPeriod.THREE_MONTHS
            }
            
            Log.d(TAG, "Syncing with lookback period: $lookbackPeriod ($lookbackMonths months)")
            
            // Perform sync
            mpesaRepository.syncMpesaSms(lookbackPeriod)
            
            val count = mpesaRepository.getTransactionCount()
            Log.d(TAG, "M-Pesa sync completed successfully. Total transactions: $count")
            
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
