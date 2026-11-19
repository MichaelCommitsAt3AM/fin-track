package com.fintrack.app.core.security

import java.util.Base64
import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptionManagerImpl @Inject constructor(
    private val keyRepository: KeyRepository
) : EncryptionManager {

    companion object {
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val TAG = "EncryptionManager"
        private const val PREFIX = "ENC[v1]:"
    }

    private class CachedKey(val userId: String, val key: SecretKey)

    // Cache the key in memory to avoid repeated Keystore/Firestore lookups. The cache is tied to
    // the user it was loaded for: this is a process-wide singleton, so after a logout/login as a
    // different user the old key must not be reused.
    @Volatile
    private var cachedKey: CachedKey? = null

    // Serialises first-time key loading. Without it, parallel syncs right after sign-up each
    // found no key and each generated (and uploaded) a different one; data encrypted with any
    // key but the last uploaded was unrecoverable.
    private val keyMutex = Mutex()

    private suspend fun getMasterKey(): SecretKey {
        val userId = keyRepository.currentUserId()
            ?: throw IllegalStateException("User not logged in")
        cachedKey?.takeIf { it.userId == userId }?.let { return it.key }

        return keyMutex.withLock {
            cachedKey?.takeIf { it.userId == userId }?.key
                ?: keyRepository.getOrGenerateMasterKey().also { cachedKey = CachedKey(userId, it) }
        }
    }

    override suspend fun encrypt(data: String): String {
        if (data.isEmpty()) return ""
        try {
            val masterKey = getMasterKey()
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, masterKey)
            
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
            
            val ivString = Base64.getEncoder().encodeToString(iv)
            val encryptedString = Base64.getEncoder().encodeToString(encryptedBytes)
            
            return "$PREFIX$ivString:$encryptedString"
        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed", e)
            throw e
        }
    }

    override suspend fun decrypt(encryptedData: String): String {
        if (encryptedData.isEmpty()) return ""
        
        // Check for prefix to support lazy migration (legacy data is plaintext)
        if (!encryptedData.startsWith(PREFIX)) {
            return encryptedData
        }
        
        try {
            val masterKey = getMasterKey()
            
            // Remove prefix
            val payload = encryptedData.substring(PREFIX.length)
            val parts = payload.split(":")
            if (parts.size != 2) throw IllegalArgumentException("Invalid encrypted data format")
            
            val iv = Base64.getDecoder().decode(parts[0])
            val encryptedBytes = Base64.getDecoder().decode(parts[1])
            
            val cipher = Cipher.getInstance(ALGORITHM)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, masterKey, spec)
            
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed", e)
            // Return empty or original? 
            // Better to return original logic? No, if it starts with ENC, it MUST be decrypted.
             // If decryption fails (e.g. wrong key), we can't show garbage.
             // We return a placeholder or throw.
             return "[Error: Decryption Failed]" 
        }
    }
}
