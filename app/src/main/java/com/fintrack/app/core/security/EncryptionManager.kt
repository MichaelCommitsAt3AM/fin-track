package com.fintrack.app.core.security

interface EncryptionManager {
    suspend fun encrypt(data: String): String
    suspend fun decrypt(encryptedData: String): String

    /** Forgets the in-memory data key (e.g. after the cloud copy of the key was deleted). */
    fun clearKeyCache() {}
}
