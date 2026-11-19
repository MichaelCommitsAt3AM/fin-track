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
    private val auth: FirebaseAuth
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
