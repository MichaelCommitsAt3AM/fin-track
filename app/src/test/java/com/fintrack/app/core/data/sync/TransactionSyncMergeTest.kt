package com.fintrack.app.core.data.sync

import com.fintrack.app.core.data.local.model.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionSyncMergeTest {

    private fun local(isSynced: Boolean, updatedAt: Long, deletedAt: Long? = null) = TransactionEntity(
        id = "t1",
        userId = "u1",
        type = "EXPENSE",
        amount = 50.0,
        category = "Food",
        date = 1_000L,
        notes = null,
        paymentMethod = null,
        isSynced = isSynced,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )

    @Test
    fun `remote applies when there is no local row`() {
        assertTrue(TransactionSyncMerge.shouldApplyRemote(null, 100L))
    }

    @Test
    fun `remote applies over a synced local row`() {
        assertTrue(TransactionSyncMerge.shouldApplyRemote(local(isSynced = true, updatedAt = 500L), 100L))
    }

    @Test
    fun `older remote does not overwrite a pending local edit`() {
        // Offline edit at t=500 not yet uploaded; cloud copy last changed at t=400
        assertFalse(TransactionSyncMerge.shouldApplyRemote(local(isSynced = false, updatedAt = 500L), 400L))
    }

    @Test
    fun `remote with same timestamp does not overwrite a pending local edit`() {
        assertFalse(TransactionSyncMerge.shouldApplyRemote(local(isSynced = false, updatedAt = 500L), 500L))
    }

    @Test
    fun `strictly newer remote wins over a pending local edit`() {
        assertTrue(TransactionSyncMerge.shouldApplyRemote(local(isSynced = false, updatedAt = 500L), 600L))
    }

    @Test
    fun `remote without updatedAt never overwrites a pending local change`() {
        assertFalse(TransactionSyncMerge.shouldApplyRemote(local(isSynced = false, updatedAt = 500L), null))
    }

    @Test
    fun `pending local soft delete is not resurrected by an older remote copy`() {
        val deletedLocally = local(isSynced = false, updatedAt = 700L, deletedAt = 700L)
        assertFalse(TransactionSyncMerge.shouldApplyRemote(deletedLocally, 650L))
    }

    @Test
    fun `deletion patch bumps updatedAt so incremental sync sees it`() {
        val patch = TransactionSyncMerge.deletionPatch(1234L)
        assertEquals(mapOf("deletedAt" to 1234L, "updatedAt" to 1234L), patch)
    }
}
