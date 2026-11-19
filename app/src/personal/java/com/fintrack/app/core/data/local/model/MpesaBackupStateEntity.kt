package com.fintrack.app.core.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local record of what has been uploaded to the encrypted M-Pesa backup, so each run only
 * uploads what changed. Chosen over an isBackedUp flag on each table because it also covers
 * category mappings and merchant rules (whose tables live in the main database, which the
 * store flavor shares and must not gain backup columns) and it lets deletions be detected.
 *
 * [docId] is the (opaque) cloud document ID; [contentHash] is a SHA-256 of the plaintext that
 * was uploaded. Nothing sensitive is stored here.
 */
@Entity(tableName = "mpesa_backup_state")
data class MpesaBackupStateEntity(
    @PrimaryKey
    val docId: String,
    val kind: String, // "tx", "receipt" or "merchant"
    val contentHash: String
)
