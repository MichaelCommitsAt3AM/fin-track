package com.fintrack.app.core.profile

import com.fintrack.app.core.data.local.SyncTimestampManager
import com.fintrack.app.core.domain.repository.NetworkRepository
import com.fintrack.app.core.security.EncryptionManager
import com.fintrack.app.core.security.KeyRepository
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

class OfflineException : Exception("No internet connection")

/**
 * Deletes everything FinTrack stored in the cloud for one account: every subcollection under
 * `users/{uid}` that the app writes, the key document, the M-Pesa backup collections (if the
 * personal flavor ever wrote them) and finally the user profile document. Local data is untouched.
 *
 * Deleting is done in batches of at most [BATCH_SIZE] documents (Firestore allows 500 per batch).
 * It is safe to run again after a failure: it simply deletes what is left.
 */
@Singleton
class CloudDataDeleter @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val networkRepository: NetworkRepository,
    private val keyRepository: KeyRepository,
    private val encryptionManager: EncryptionManager,
    private val syncTimestampManager: SyncTimestampManager
) {
    /** @param onProgress receives a short human-readable step, e.g. "Deleting transactions..." */
    suspend fun deleteAll(uid: String, onProgress: (String) -> Unit = {}) {
        if (!networkRepository.isNetworkAvailable()) throw OfflineException()

        val userDoc = firestore.collection("users").document(uid)
        COLLECTIONS.forEach { (name, label) ->
            onProgress("Deleting $label...")
            deleteCollection(userDoc.collection(name))
        }
        onProgress("Deleting your profile...")
        userDoc.delete().await()

        // The cloud copy of the data key is gone: drop the cached one so a later re-upload
        // starts with a fresh key rather than reviving one other devices don't have.
        keyRepository.deleteLocalMasterKey(uid)
        encryptionManager.clearKeyCache()
        syncTimestampManager.clearAllTimestamps()
    }

    private suspend fun deleteCollection(collection: CollectionReference) {
        while (true) {
            val snapshot = collection.limit(BATCH_SIZE.toLong()).get().await()
            if (snapshot.isEmpty) return
            val batch = firestore.batch()
            snapshot.documents.forEach { batch.delete(it.reference) }
            batch.commit().await()
            if (snapshot.size() < BATCH_SIZE) return
        }
    }

    companion object {
        const val BATCH_SIZE = 450

        /** Firestore subcollection name to the label shown while deleting. */
        val COLLECTIONS = listOf(
            "transactions" to "transactions",
            "recurring_transactions" to "recurring transactions",
            "budgets" to "budgets",
            "savings" to "savings",
            "debts" to "debts",
            "categories" to "categories",
            "payment_methods" to "payment methods",
            "mpesa_backup_records" to "M-Pesa backup",
            "mpesa_backup_mappings" to "M-Pesa backup",
            "mpesa_backup" to "M-Pesa backup",
            "keys" to "encryption key"
        )
    }
}
