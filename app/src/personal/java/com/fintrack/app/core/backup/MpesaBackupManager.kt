package com.fintrack.app.core.backup

import android.util.Log
import com.fintrack.app.core.data.local.dao.MerchantCategoryDao
import com.fintrack.app.core.data.local.dao.MpesaBackupStateDao
import com.fintrack.app.core.data.local.dao.MpesaCategoryMappingDao
import com.fintrack.app.core.data.local.dao.MpesaTransactionDao
import com.fintrack.app.core.data.local.model.MpesaBackupStateEntity
import com.fintrack.app.core.domain.repository.NetworkRepository
import com.fintrack.app.core.profile.AccountMode
import com.fintrack.app.core.profile.OfflineException
import com.fintrack.app.core.profile.ProfileManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.security.GeneralSecurityException
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/** Cloud backup needs a linked account that is currently signed in. */
class BackupNeedsAccountException : Exception("Cloud backup needs an account")

/** There is no backup in the cloud to restore. */
class NoBackupException : Exception("No backup found")

enum class BackupAvailability { NEEDS_ACCOUNT, READY }

data class BackupStatus(
    val availability: BackupAvailability,
    val enabled: Boolean,
    val lastBackupAt: Long,
    val lastError: String?
)

data class RestoreResult(val transactions: Int, val categoryChoices: Int, val skipped: Int)

/**
 * Opt-in, end-to-end encrypted M-Pesa backup (personal flavor only). See [BackupCrypto] for the
 * scheme and [MpesaBackupPayload] for exactly what is uploaded. Independent of the main Cloud
 * sync switch, but it needs a linked account to name the Firestore location.
 */
@Singleton
class MpesaBackupManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val profileManager: ProfileManager,
    private val networkRepository: NetworkRepository,
    private val transactionDao: MpesaTransactionDao,
    private val mappingDao: MpesaCategoryMappingDao,
    private val merchantDao: MerchantCategoryDao,
    private val stateDao: MpesaBackupStateDao,
    private val keyCache: BackupKeyCache,
    private val prefs: MpesaBackupPrefs,
    private val scheduler: MpesaBackupScheduler
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val runMutex = Mutex()

    val status: StateFlow<BackupStatus> = combine(profileManager.state, prefs.state) { profile, p ->
        val ready = profile.mode == AccountMode.ACCOUNT && profile.linkedUid != null
        BackupStatus(
            availability = if (ready) BackupAvailability.READY else BackupAvailability.NEEDS_ACCOUNT,
            enabled = ready && p.enabled && p.uid == profile.linkedUid,
            lastBackupAt = p.lastBackupAt,
            lastError = p.lastError
        )
    }.stateIn(scope, SharingStarted.Eagerly, BackupStatus(BackupAvailability.NEEDS_ACCOUNT, false, 0L, null))

    /** The linked account, if it is also the signed-in one. */
    private fun accountUidOrNull(): String? {
        val s = profileManager.state.value
        val uid = s.linkedUid ?: return null
        return uid.takeIf { s.mode == AccountMode.ACCOUNT && auth.currentUser?.uid == it }
    }

    private fun requireUid(): String = accountUidOrNull() ?: throw BackupNeedsAccountException()

    // ---------------------------------------------------------------- cloud locations

    private fun userDoc(uid: String) = firestore.collection("users").document(uid)
    private fun metaDoc(uid: String): DocumentReference = userDoc(uid).collection(META_COLLECTION).document(META_DOC)
    private fun records(uid: String): CollectionReference = userDoc(uid).collection(RECORDS_COLLECTION)
    private fun mappings(uid: String): CollectionReference = userDoc(uid).collection(MAPPINGS_COLLECTION)

    private suspend fun fetchMeta(uid: String): BackupCrypto.WrappedKey? {
        val d = metaDoc(uid).get().await()
        if (!d.exists()) return null
        return BackupCrypto.WrappedKey(
            iterations = d.getLong("iterations")?.toInt() ?: return null,
            salt = BackupCrypto.unb64(d.getString("salt") ?: return null),
            iv = BackupCrypto.unb64(d.getString("iv") ?: return null),
            wrappedDek = BackupCrypto.unb64(d.getString("wrappedDek") ?: return null)
        )
    }

    private suspend fun writeMeta(uid: String, wrapped: BackupCrypto.WrappedKey, createdAt: Long) {
        metaDoc(uid).set(
            hashMapOf(
                "version" to BackupCrypto.VERSION,
                "kdf" to BackupCrypto.KDF,
                "iterations" to wrapped.iterations,
                "salt" to BackupCrypto.b64(wrapped.salt),
                "wrappedDek" to BackupCrypto.b64(wrapped.wrappedDek),
                "iv" to BackupCrypto.b64(wrapped.iv),
                "createdAt" to createdAt
            )
        ).await()
    }

    /** True when an encrypted backup exists for the signed-in account. False when unknown/offline. */
    suspend fun hasCloudBackup(): Boolean {
        val uid = accountUidOrNull() ?: return false
        return try {
            metaDoc(uid).get().await().exists()
        } catch (e: Exception) {
            false
        }
    }

    // ---------------------------------------------------------------- turn on / off

    /**
     * Turns the backup on. A new account gets a fresh key wrapped by [passphrase]; if a backup
     * already exists in the cloud, [passphrase] must be its passphrase (the existing key is adopted).
     * @throws WrongPassphraseException when a backup exists and the passphrase does not open it
     */
    suspend fun enable(passphrase: CharArray) {
        val uid = requireUid()
        requireOnline()
        val existing = fetchMeta(uid)
        val dek = if (existing != null) {
            BackupCrypto.unwrapDek(existing, passphrase)
        } else {
            BackupCrypto.newDek().also { writeMeta(uid, BackupCrypto.wrapDek(it, passphrase), System.currentTimeMillis()) }
        }
        try {
            keyCache.save(uid, dek)
        } finally {
            dek.fill(0)
        }
        stateDao.clear() // first run uploads everything that differs from the cloud copy
        prefs.setEnabled(true, uid)
        scheduler.requestRun()
    }

    /**
     * Turns the backup off. With [deleteCloud] the meta document and both collections are removed
     * first (needs a connection; nothing changes if that fails). Either way the cached key goes.
     */
    suspend fun disable(deleteCloud: Boolean) {
        if (deleteCloud) {
            val uid = requireUid()
            requireOnline()
            deleteCloudBackup(uid)
        }
        keyCache.clear()
        stateDao.clear()
        prefs.setEnabled(false, null)
    }

    /** "Forgot passphrase": deletes the cloud backup and starts a new one under [newPassphrase]. */
    suspend fun resetBackup(newPassphrase: CharArray) {
        val uid = requireUid()
        requireOnline()
        deleteCloudBackup(uid)
        keyCache.clear()
        stateDao.clear()
        prefs.setEnabled(false, null)
        enable(newPassphrase)
    }

    /** Changing the passphrase only re-wraps the key; no record is touched. */
    suspend fun changePassphrase(old: CharArray, new: CharArray) {
        val uid = requireUid()
        requireOnline()
        val existing = fetchMeta(uid) ?: throw NoBackupException()
        val rewrapped = BackupCrypto.rewrapDek(existing, old, new)
        val createdAt = metaDoc(uid).get().await().getLong("createdAt") ?: System.currentTimeMillis()
        writeMeta(uid, rewrapped, createdAt)
    }

    private suspend fun deleteCloudBackup(uid: String) {
        deleteAll(records(uid))
        deleteAll(mappings(uid))
        metaDoc(uid).delete().await()
    }

    private suspend fun deleteAll(collection: CollectionReference) {
        while (true) {
            val snap = collection.limit(DELETE_BATCH.toLong()).get().await()
            if (snap.isEmpty) return
            val batch = firestore.batch()
            snap.documents.forEach { batch.delete(it.reference) }
            batch.commit().await()
            if (snap.size() < DELETE_BATCH) return
        }
    }

    private fun requireOnline() {
        if (!networkRepository.isNetworkAvailable()) throw OfflineException()
    }

    // ---------------------------------------------------------------- upload

    private class Item(val docId: String, val kind: String, val json: String) {
        val hash: String by lazy { sha256(json) }
        val collectionIsRecords get() = kind == KIND_TX
    }

    /**
     * Uploads what changed since the last run. A no-op (success) when the backup is off, there is
     * no linked account, or the key is not available. Returns the number of records uploaded.
     *
     * Transactions are never deleted from the cloud when they disappear from the phone (the backup
     * is a safety net, not a mirror); removed category choices are.
     */
    suspend fun backupNow(): Int = runMutex.withLock {
        val uid = accountUidOrNull() ?: return 0
        val p = prefs.state.value
        if (!p.enabled || p.uid != uid) return 0
        val dek = keyCache.load(uid) ?: run {
            prefs.recordResult(0L, "Backup key not available on this phone. Re-enter your passphrase in M-Pesa settings.")
            return 0
        }
        try {
            if (!networkRepository.isNetworkAvailable()) throw OfflineException()

            val desired = buildDesiredItems(dek)
            val known = stateDao.getAll().associateBy { it.docId }
            val changed = desired.filter { known[it.docId]?.contentHash != it.hash }

            changed.chunked(UPLOAD_BATCH).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { item ->
                    val sealed = BackupCrypto.encryptRecord(dek, item.docId, item.json.toByteArray(Charsets.UTF_8))
                    val ref = (if (item.collectionIsRecords) records(uid) else mappings(uid)).document(item.docId)
                    batch.set(ref, hashMapOf("iv" to BackupCrypto.b64(sealed.iv), "ciphertext" to BackupCrypto.b64(sealed.ciphertext), "v" to BackupCrypto.VERSION))
                }
                batch.commit().await()
                stateDao.upsertAll(chunk.map { MpesaBackupStateEntity(it.docId, it.kind, it.hash) })
            }

            // Category choices the user removed on the phone are removed from the backup too
            val desiredIds = desired.map { it.docId }.toSet()
            val staleMappings = known.values.filter { it.kind != KIND_TX && it.docId !in desiredIds }
            staleMappings.chunked(UPLOAD_BATCH).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { batch.delete(mappings(uid).document(it.docId)) }
                batch.commit().await()
                stateDao.deleteByIds(chunk.map { it.docId })
            }

            prefs.recordResult(System.currentTimeMillis(), null)
            changed.size
        } catch (e: Exception) {
            // Never log plaintext or keys; the message is just the failure type
            Log.w(TAG, "M-Pesa backup failed: ${e.javaClass.simpleName}")
            prefs.recordResult(0L, e.message ?: "Backup failed")
            throw e
        } finally {
            dek.fill(0)
        }
    }

    private suspend fun buildDesiredItems(dek: ByteArray): List<Item> {
        val items = ArrayList<Item>()
        transactionDao.getAllTransactions().first().forEach { e ->
            items += Item(BackupCrypto.documentId(dek, e.mpesaReceiptNumber), KIND_TX, MpesaBackupPayload.transactionJson(e))
        }
        mappingDao.getAllMappings().first().forEach { m ->
            items += Item(BackupCrypto.documentId(dek, "receipt:" + m.mpesaReceiptNumber), KIND_RECEIPT, MpesaBackupPayload.receiptMappingJson(m))
        }
        merchantDao.getAllMappings().first().forEach { m ->
            items += Item(BackupCrypto.documentId(dek, "merchant:" + m.merchantName), KIND_MERCHANT, MpesaBackupPayload.merchantRuleJson(m))
        }
        return items
    }

    // ---------------------------------------------------------------- restore

    /**
     * Restores the M-Pesa history and category choices from the cloud backup into this phone.
     * Only receipts that are not already in the local database are inserted (existing rows and
     * category choices always win). Restored rows have no SMS text. On success the backup is
     * switched on for this phone so it keeps itself up to date.
     *
     * @param onProgress (processed, total or null) records so far
     * @throws WrongPassphraseException when the passphrase does not open the backup
     * @throws NoBackupException when the account has no backup
     */
    suspend fun restore(passphrase: CharArray, onProgress: (Int, Int?) -> Unit = { _, _ -> }): RestoreResult {
        val uid = requireUid()
        requireOnline()
        val meta = fetchMeta(uid) ?: throw NoBackupException()
        val dek = BackupCrypto.unwrapDek(meta, passphrase)
        try {
            val total = try {
                (records(uid).count().get(com.google.firebase.firestore.AggregateSource.SERVER).await().count +
                    mappings(uid).count().get(com.google.firebase.firestore.AggregateSource.SERVER).await().count).toInt()
            } catch (e: Exception) {
                null
            }
            var processed = 0
            var skipped = 0
            var restoredTx = 0
            var restoredChoices = 0
            val stateRows = ArrayList<MpesaBackupStateEntity>()
            val existingReceipts = transactionDao.getAllTransactions().first().map { it.mpesaReceiptNumber }.toHashSet()

            // M-Pesa transactions
            forEachPage(records(uid)) { doc ->
                processed++
                onProgress(processed, total)
                val json = decrypt(dek, doc.id, doc.getString("iv"), doc.getString("ciphertext")) ?: run { skipped++; return@forEachPage }
                val entity = try {
                    MpesaBackupPayload.transactionFromJson(json)
                } catch (e: Exception) {
                    skipped++
                    return@forEachPage
                }
                // The id is derived from the receipt: a record under any other id is not ours
                if (BackupCrypto.documentId(dek, entity.mpesaReceiptNumber) != doc.id) { skipped++; return@forEachPage }
                if (existingReceipts.add(entity.mpesaReceiptNumber)) {
                    transactionDao.insertTransactions(listOf(entity)) // IGNORE on conflict
                    restoredTx++
                    stateRows += MpesaBackupStateEntity(doc.id, KIND_TX, sha256(MpesaBackupPayload.transactionJson(entity)))
                }
            }

            // Category choices (never overwrite a choice already made on this phone)
            forEachPage(mappings(uid)) { doc ->
                processed++
                onProgress(processed, total)
                val json = decrypt(dek, doc.id, doc.getString("iv"), doc.getString("ciphertext")) ?: run { skipped++; return@forEachPage }
                try {
                    when (MpesaBackupPayload.mappingKind(json)) {
                        KIND_RECEIPT -> {
                            val m = MpesaBackupPayload.receiptMappingFromJson(json)
                            if (BackupCrypto.documentId(dek, "receipt:" + m.mpesaReceiptNumber) != doc.id) { skipped++; return@forEachPage }
                            if (mappingDao.getMappingForReceipt(m.mpesaReceiptNumber) == null) {
                                mappingDao.insert(m)
                                restoredChoices++
                                stateRows += MpesaBackupStateEntity(doc.id, KIND_RECEIPT, sha256(MpesaBackupPayload.receiptMappingJson(m)))
                            }
                        }
                        KIND_MERCHANT -> {
                            val m = MpesaBackupPayload.merchantRuleFromJson(json)
                            if (BackupCrypto.documentId(dek, "merchant:" + m.merchantName) != doc.id) { skipped++; return@forEachPage }
                            if (merchantDao.getMapping(m.merchantName) == null) {
                                merchantDao.insertMapping(m)
                                restoredChoices++
                                stateRows += MpesaBackupStateEntity(doc.id, KIND_MERCHANT, sha256(MpesaBackupPayload.merchantRuleJson(m)))
                            }
                        }
                        else -> skipped++
                    }
                } catch (e: Exception) {
                    skipped++
                }
            }

            // Keep the backup going from this phone, without re-uploading what was just restored
            keyCache.save(uid, dek)
            stateDao.clear()
            stateRows.chunked(500).forEach { stateDao.upsertAll(it) }
            prefs.setEnabled(true, uid)
            scheduler.requestRun()
            return RestoreResult(restoredTx, restoredChoices, skipped)
        } finally {
            dek.fill(0)
        }
    }

    private fun decrypt(dek: ByteArray, docId: String, iv: String?, ciphertext: String?): String? {
        if (iv == null || ciphertext == null) return null
        return try {
            String(BackupCrypto.decryptRecord(dek, docId, BackupCrypto.Sealed(BackupCrypto.unb64(iv), BackupCrypto.unb64(ciphertext))), Charsets.UTF_8)
        } catch (e: GeneralSecurityException) {
            null // altered or foreign record: skip it
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private suspend fun forEachPage(
        collection: CollectionReference,
        handle: suspend (com.google.firebase.firestore.DocumentSnapshot) -> Unit
    ) {
        var last: com.google.firebase.firestore.DocumentSnapshot? = null
        while (true) {
            var q = collection.orderBy(FieldPath.documentId()).limit(PAGE.toLong())
            if (last != null) q = q.startAfter(last)
            val snap = q.get().await()
            if (snap.isEmpty) return
            snap.documents.forEach { handle(it) }
            if (snap.size() < PAGE) return
            last = snap.documents.last()
        }
    }

    companion object {
        private const val TAG = "MpesaBackup"
        const val META_COLLECTION = "mpesa_backup"
        const val META_DOC = "meta"
        const val RECORDS_COLLECTION = "mpesa_backup_records"
        const val MAPPINGS_COLLECTION = "mpesa_backup_mappings"
        const val KIND_TX = "tx"
        const val KIND_RECEIPT = "receipt"
        const val KIND_MERCHANT = "merchant"
        private const val UPLOAD_BATCH = 300
        private const val DELETE_BATCH = 450
        private const val PAGE = 300

        fun sha256(text: String): String =
            MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
    }
}
