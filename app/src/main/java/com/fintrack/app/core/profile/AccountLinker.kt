package com.fintrack.app.core.profile

import android.util.Log
import com.fintrack.app.core.data.local.dao.BudgetDao
import com.fintrack.app.core.data.local.dao.CategoryDao
import com.fintrack.app.core.data.local.dao.DebtDao
import com.fintrack.app.core.data.local.dao.SavingDao
import com.fintrack.app.core.data.local.dao.TransactionDao
import com.fintrack.app.core.domain.repository.BudgetRepository
import com.fintrack.app.core.domain.repository.CategoryRepository
import com.fintrack.app.core.domain.repository.DebtRepository
import com.fintrack.app.core.domain.repository.NetworkRepository
import com.fintrack.app.core.domain.repository.SavingRepository
import com.fintrack.app.core.domain.repository.TransactionRepository
import com.fintrack.app.core.domain.repository.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

sealed class CloudTransferState {
    object Idle : CloudTransferState()
    object Running : CloudTransferState()
    object Done : CloudTransferState()
    data class Failed(val message: String) : CloudTransferState()
}

/**
 * Moves a profile's data to or from the cloud when sync is switched on or a local profile is
 * linked to an account. Runs in an app-level scope so leaving the screen doesn't cancel it.
 * Order matters: upload first so pending local edits are never overwritten by an older cloud copy.
 */
@Singleton
class AccountLinker @Inject constructor(
    private val profileManager: ProfileManager,
    private val syncGate: SyncGate,
    private val firestore: FirebaseFirestore,
    private val networkRepository: NetworkRepository,
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val savingDao: SavingDao,
    private val debtDao: DebtDao,
    private val categoryDao: CategoryDao,
    private val userRepository: UserRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val savingRepository: SavingRepository,
    private val debtRepository: DebtRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow<CloudTransferState>(CloudTransferState.Idle)
    val state: StateFlow<CloudTransferState> = _state.asStateFlow()

    /** True when `users/{uid}` already holds FinTrack data. Throws when offline or denied. */
    suspend fun cloudHasData(uid: String): Boolean {
        val user = firestore.collection("users").document(uid)
        if (user.collection("keys").document("master").get().await().exists()) return true
        return listOf("transactions", "budgets", "savings", "debts", "categories", "recurring_transactions")
            .any { !user.collection(it).limit(1).get().await().isEmpty }
    }

    /**
     * Uploads everything on this phone (marks all rows unsynced, then pushes) and, when
     * [pullFromCloud] is set, merges the cloud copy in afterwards. Call after [ProfileManager]
     * has linked the account / enabled sync so [SyncGate] is open.
     */
    fun startTransfer(pullFromCloud: Boolean) {
        if (_state.value == CloudTransferState.Running) return
        _state.value = CloudTransferState.Running
        scope.launch {
            _state.value = try {
                transfer(pullFromCloud)
                CloudTransferState.Done
            } catch (e: Exception) {
                Log.e(TAG, "Cloud transfer failed: ${e.message}", e)
                CloudTransferState.Failed(e.message ?: "Sync failed")
            }
        }
    }

    private suspend fun transfer(pullFromCloud: Boolean) {
        val profileId = profileManager.profileId() ?: return
        if (syncGate.cloudUidOrNull() == null) return

        transactionDao.markAllUnsynced(profileId)
        budgetDao.markAllUnsynced(profileId)
        savingDao.markAllUnsynced(profileId)
        debtDao.markAllUnsynced(profileId)
        categoryDao.markAllUnsynced(profileId)

        if (!networkRepository.isNetworkAvailable()) {
            // Rows stay marked; the sync workers upload them once there is a connection
            Log.d(TAG, "Offline: upload deferred to the sync workers")
            return
        }

        // Push
        // When combining, the account profile on the cloud wins; do not overwrite it
        if (!pullFromCloud) userRepository.getCurrentUserOnce()?.let { userRepository.createUser(it) }
        categoryRepository.syncUnsyncedCategories()
        transactionRepository.syncUnsyncedTransactions()
        budgetRepository.syncUnsyncedBudgets()
        savingRepository.syncUnsyncedSavings()
        debtRepository.syncUnsyncedDebts()
        transactionRepository.getAllRecurringTransactions().first()
            .forEach { transactionRepository.insertRecurringTransaction(it) }

        if (pullFromCloud) {
            userRepository.syncUserFromCloud()
            categoryRepository.syncCategoriesFromCloud()
            transactionRepository.syncTransactionsFromCloud()
            budgetRepository.syncBudgetsFromCloud()
            savingRepository.syncSavingsFromCloud()
            debtRepository.syncDebtsFromCloud()
            transactionRepository.syncRecurringTransactionsFromCloud()
        }
    }

    private companion object {
        const val TAG = "AccountLinker"
    }
}
