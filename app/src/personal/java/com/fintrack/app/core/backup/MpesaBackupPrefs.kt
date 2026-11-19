package com.fintrack.app.core.backup

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BackupPrefsState(
    val enabled: Boolean = false,
    /** The account the backup was turned on for; a different account never uses it. */
    val uid: String? = null,
    val lastBackupAt: Long = 0L,
    val lastError: String? = null
)

/** Plain on-device flags for the M-Pesa backup. Holds no secrets (the key lives in [BackupKeyCache]). */
class MpesaBackupPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("mpesa_backup_prefs", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(read())
    val state: StateFlow<BackupPrefsState> = _state.asStateFlow()

    private fun read() = BackupPrefsState(
        enabled = prefs.getBoolean("enabled", false),
        uid = prefs.getString("uid", null),
        lastBackupAt = prefs.getLong("last_backup_at", 0L),
        lastError = prefs.getString("last_error", null)
    )

    @Synchronized
    fun setEnabled(enabled: Boolean, uid: String?) {
        prefs.edit().putBoolean("enabled", enabled).putString("uid", uid)
            .putLong("last_backup_at", 0L).remove("last_error").apply()
        _state.value = read()
    }

    @Synchronized
    fun recordResult(lastBackupAt: Long, error: String?) {
        val e = prefs.edit()
        if (error == null) e.putLong("last_backup_at", lastBackupAt).remove("last_error")
        else e.putString("last_error", error)
        e.apply()
        _state.value = read()
    }
}
