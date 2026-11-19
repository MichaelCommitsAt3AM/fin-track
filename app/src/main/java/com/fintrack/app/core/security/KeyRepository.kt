package com.fintrack.app.core.security

import javax.crypto.SecretKey

interface KeyRepository {
    /** The user whose master key [getOrGenerateMasterKey] returns, or null when signed out. */
    fun currentUserId(): String?

    suspend fun getOrGenerateMasterKey(): SecretKey

    /**
     * Removes the Keystore copy of [uid]'s data key. Used after the cloud copy of the key was
     * deleted, so a later re-upload generates a fresh key instead of reviving a stale one.
     */
    fun deleteLocalMasterKey(uid: String)
}
