package com.fintrack.app.core.data.sync

import com.fintrack.app.core.data.local.model.TransactionEntity

/**
 * Conflict rules for syncing transactions between Room and Firestore.
 *
 * Kept free of Android/Firebase types so the rules can be unit-tested.
 */
object TransactionSyncMerge {

    /**
     * Whether a document pulled from Firestore may overwrite (or delete) the local row.
     *
     * A local row that is still unsynced holds a change the cloud hasn't seen yet (an edit or a
     * soft delete). Overwriting it with the cloud copy silently loses that change, because the row
     * is then marked synced and never uploaded. So the remote copy only wins if it is strictly
     * newer than the pending local change.
     */
    fun shouldApplyRemote(local: TransactionEntity?, remoteUpdatedAt: Long?): Boolean {
        if (local == null || local.isSynced) return true
        return remoteUpdatedAt != null && remoteUpdatedAt > local.updatedAt
    }

    /**
     * Firestore fields to write for a soft delete.
     *
     * `updatedAt` must move with `deletedAt`: other devices pull with `updatedAt > lastSync`,
     * so a delete that only sets `deletedAt` is never seen by them. Write it with
     * `SetOptions.merge()` rather than `update()`, which fails with NOT_FOUND when the
     * transaction was created and deleted offline and never uploaded.
     */
    fun deletionPatch(deletedAt: Long): Map<String, Any> =
        mapOf("deletedAt" to deletedAt, "updatedAt" to deletedAt)
}
