package com.fintrack.app.presentation.settings.mpesa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.app.core.backup.BackupAvailability
import com.fintrack.app.core.backup.MpesaBackupManager
import com.fintrack.app.core.backup.NoBackupException
import com.fintrack.app.core.backup.RestoreResult
import com.fintrack.app.core.backup.WrongPassphraseException
import com.fintrack.app.core.profile.OfflineException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BackupUiState(
    val busy: Boolean = false,
    val error: String? = null,
    val info: String? = null,
    /** Null until checked. True when the account already has an encrypted backup in the cloud. */
    val cloudBackupExists: Boolean? = null,
    val restoreProgress: Pair<Int, Int?>? = null,
    val restoreResult: RestoreResult? = null,
    /** Show "Restore your M-Pesa history?" (new phone, backup found, not yet restored). */
    val restoreOffer: Boolean = false
)

@HiltViewModel
class MpesaBackupViewModel @Inject constructor(
    private val manager: MpesaBackupManager
) : ViewModel() {

    val status = manager.status
    private val _ui = MutableStateFlow(BackupUiState())
    val ui: StateFlow<BackupUiState> = _ui.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val s = manager.status.value
            val exists = s.availability == BackupAvailability.READY && manager.hasCloudBackup()
            _ui.update { it.copy(cloudBackupExists = exists, restoreOffer = exists && !s.enabled) }
        }
    }

    fun dismissOffer() = _ui.update { it.copy(restoreOffer = false) }
    fun clearMessages() = _ui.update { it.copy(error = null, info = null, restoreResult = null) }

    private fun run(block: suspend () -> Unit) {
        if (_ui.value.busy) return
        viewModelScope.launch {
            _ui.update { it.copy(busy = true, error = null, info = null) }
            try {
                block()
            } catch (e: WrongPassphraseException) {
                _ui.update { it.copy(error = "That passphrase is not correct. Check it and try again.") }
            } catch (e: NoBackupException) {
                _ui.update { it.copy(error = "No backup was found for this account.") }
            } catch (e: OfflineException) {
                _ui.update { it.copy(error = "You are offline. Try again when you are back online.") }
            } catch (e: Exception) {
                _ui.update { it.copy(error = e.message ?: "Something went wrong") }
            } finally {
                _ui.update { it.copy(busy = false) }
            }
        }
    }

    fun enable(passphrase: CharArray, onDone: () -> Unit) = run {
        manager.enable(passphrase)
        _ui.update { it.copy(info = "Cloud backup is on. Your first backup will upload shortly.") }
        onDone()
    }

    fun disable(deleteCloud: Boolean) = run {
        manager.disable(deleteCloud)
        _ui.update {
            it.copy(
                info = if (deleteCloud) "Cloud backup is off and your backup was deleted."
                else "Cloud backup is off. Your backup was kept in the cloud.",
                cloudBackupExists = !deleteCloud
            )
        }
    }

    fun backupNow() = run {
        val n = manager.backupNow()
        _ui.update { it.copy(info = if (n == 0) "Your backup is up to date." else "Backed up $n items.") }
    }

    fun restore(passphrase: CharArray) = run {
        _ui.update { it.copy(restoreProgress = 0 to null) }
        try {
            val result = manager.restore(passphrase) { done, total ->
                _ui.update { it.copy(restoreProgress = done to total) }
            }
            _ui.update { it.copy(restoreResult = result, restoreOffer = false, cloudBackupExists = true) }
        } finally {
            _ui.update { it.copy(restoreProgress = null) }
        }
    }

    fun resetBackup(newPassphrase: CharArray) = run {
        manager.resetBackup(newPassphrase)
        _ui.update { it.copy(info = "Your old backup was deleted and a new one is on.", cloudBackupExists = true) }
    }

    fun changePassphrase(old: CharArray, new: CharArray, onDone: () -> Unit) = run {
        manager.changePassphrase(old, new)
        _ui.update { it.copy(info = "Passphrase changed.") }
        onDone()
    }
}
