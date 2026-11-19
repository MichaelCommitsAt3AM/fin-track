package com.fintrack.app.core.data.local

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
    fun getLastTransactionSyncTimestamp(uid: String): Long {
        return prefs.getLong(key(uid), 0L)
    }

    /**
     * Update the last successful sync timestamp for transactions
     */
    fun setLastTransactionSyncTimestamp(uid: String, timestamp: Long = System.currentTimeMillis()) {
        prefs.edit().putLong(key(uid), timestamp).apply()
    }

    /**
     * Clear all sync timestamps (useful for full re-sync)
     */
    fun clearAllTimestamps() {
        prefs.edit().clear().apply()
    }

    /**
     * Watermarks are per cloud account (audit M2): a different account must never inherit another
     * account's watermark. The old global key is ignored (one full re-sync after the update).
     */
    private fun key(uid: String) = "$KEY_LAST_TRANSACTION_SYNC:$uid"

    companion object {
        private const val KEY_LAST_TRANSACTION_SYNC = "last_transaction_sync"
    }
}
