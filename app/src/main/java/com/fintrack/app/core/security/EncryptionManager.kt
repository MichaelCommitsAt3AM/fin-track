package com.fintrack.app.core.security

interface EncryptionManager {
    suspend fun encrypt(data: String): String
    suspend fun decrypt(encryptedData: String): String
}
