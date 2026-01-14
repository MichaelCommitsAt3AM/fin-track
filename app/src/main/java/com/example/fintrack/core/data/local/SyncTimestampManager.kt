package com.example.fintrack.core.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages sync timestamps for incremental sync
 */
@Singleton
class SyncTimestampManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "sync_timestamps",
        Context.MODE_PRIVATE
    )

    /**
     * Get the last successful sync timestamp for transactions
     */
    fun getLastTransactionSyncTimestamp(): Long {
        return prefs.getLong(KEY_LAST_TRANSACTION_SYNC, 0L)
    }

    /**
     * Update the last successful sync timestamp for transactions
     */
    fun setLastTransactionSyncTimestamp(timestamp: Long = System.currentTimeMillis()) {
        prefs.edit().putLong(KEY_LAST_TRANSACTION_SYNC, timestamp).apply()
    }

    /**
     * Clear all sync timestamps (useful for full re-sync)
     */
    fun clearAllTimestamps() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_LAST_TRANSACTION_SYNC = "last_transaction_sync"
    }
}
