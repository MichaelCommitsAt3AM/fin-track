package com.fintrack.app.presentation.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.app.core.domain.repository.TransactionRepository
import com.fintrack.app.core.profile.AccountLinker
import com.fintrack.app.core.profile.CloudTransferState
import com.fintrack.app.core.profile.ProfileManager
import com.fintrack.app.core.profile.ProfileState
import com.fintrack.app.core.utils.CsvExporter
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AccountSyncViewModel @Inject constructor(
    private val profileManager: ProfileManager,
    private val accountLinker: AccountLinker,
    private val transactionRepository: TransactionRepository,
    private val csvExporter: CsvExporter,
    private val auth: FirebaseAuth,
    private val cloudDataDeleter: com.fintrack.app.core.profile.CloudDataDeleter,
    private val syncGate: com.fintrack.app.core.profile.SyncGate
) : ViewModel() {

    val profileState: StateFlow<ProfileState> = profileManager.state
    val transferState: StateFlow<CloudTransferState> = accountLinker.state

    val email: String get() = auth.currentUser?.email.orEmpty()

    private val _exportEvent = MutableSharedFlow<Uri?>()
    val exportEvent: SharedFlow<Uri?> = _exportEvent.asSharedFlow()

    /** Turns the Cloud sync switch on: uploads everything on this phone, then merges the cloud copy. */
    fun enableSync() {
        viewModelScope.launch {
            profileManager.setCloudSyncEnabled(true)
            accountLinker.startTransfer(pullFromCloud = true)
        }
    }

    private val _deleteState = kotlinx.coroutines.flow.MutableStateFlow<DeleteState>(DeleteState.Idle)
    val deleteState: StateFlow<DeleteState> = _deleteState

    fun dismissDeleteState() {
        _deleteState.value = DeleteState.Idle
    }

    /**
     * Deletes the cloud copy, and only once that has fully succeeded turns sync off. Local data
     * is never touched. On failure (offline, permissions) sync stays on so nothing is half-done.
     */
    fun disableSyncAndDeleteCloud() {
        if (_deleteState.value is DeleteState.Running) return
        val uid = syncGate.cloudUidOrNull() ?: return
        viewModelScope.launch {
            _deleteState.value = DeleteState.Running("Starting...")
            try {
                cloudDataDeleter.deleteAll(uid) { step -> _deleteState.value = DeleteState.Running(step) }
                profileManager.setCloudSyncEnabled(false)
                _deleteState.value = DeleteState.Idle
            } catch (e: com.fintrack.app.core.profile.OfflineException) {
                _deleteState.value = DeleteState.Failed("You are offline. Try again when you are back online.")
            } catch (e: Exception) {
                _deleteState.value = DeleteState.Failed(e.message ?: "Could not delete your cloud data")
            }
        }
    }

    /** Keeps the cloud copy untouched; local data is never affected. */
    fun disableSyncKeepCloud() {
        viewModelScope.launch { profileManager.setCloudSyncEnabled(false) }
    }

    fun exportTransactionsCsv() {
        viewModelScope.launch {
            val transactions = transactionRepository.getAllTransactionsPaged(Int.MAX_VALUE).first()
            val uri = withContext(Dispatchers.IO) { csvExporter.generateTransactionCsv(transactions) }
            _exportEvent.emit(uri)
        }
    }
}

sealed class DeleteState {
    object Idle : DeleteState()
    data class Running(val step: String) : DeleteState()
    data class Failed(val message: String) : DeleteState()
}
