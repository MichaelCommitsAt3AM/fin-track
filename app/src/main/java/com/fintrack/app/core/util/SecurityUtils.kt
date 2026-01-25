package com.fintrack.app.core.util

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * Security utilities for the FinTrack application.
 * Provides cryptographic functions for PIN hashing and verification.
 */
object SecurityUtils {

    private const val HASH_ALGORITHM = "SHA-256"
    private const val SALT_LENGTH = 16 // 16 bytes = 128 bits

    /**
     * Generates a cryptographically secure random salt.
     * @return Base64-encoded salt string
     */
    fun generateSalt(): String {
        val salt = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(salt)
        return Base64.getEncoder().encodeToString(salt)
    }

    /**
     * Hashes a PIN with the provided salt using SHA-256.
     * @param pin The PIN to hash (typically 4-6 digits)
     * @param salt The Base64-encoded salt
     * @return Base64-encoded hash
     */
    fun hashPin(pin: String, salt: String): String {
        val saltBytes = Base64.getDecoder().decode(salt)
        val pinBytes = pin.toByteArray(Charsets.UTF_8)
        
        // Combine PIN and salt
        val combined = pinBytes + saltBytes
        
        // Hash using SHA-256
        val digest = MessageDigest.getInstance(HASH_ALGORITHM)
        val hashBytes = digest.digest(combined)
        
        return Base64.getEncoder().encodeToString(hashBytes)
    }

    /**
     * Verifies a PIN against a stored hash using constant-time comparison.
     * This prevents timing attacks.
     * 
     * @param inputPin The PIN entered by the user
     * @param storedHash The stored hash in format "salt:hash"
     * @return true if PIN is correct, false otherwise
     */
    fun verifyPin(inputPin: String, storedHash: String): Boolean {
        return try {
            val parts = storedHash.split(":")
            if (parts.size != 2) return false
            
            val salt = parts[0]
            val expectedHash = parts[1]
            
            val actualHash = hashPin(inputPin, salt)
            
            // Constant-time comparison to prevent timing attacks
            constantTimeEquals(actualHash, expectedHash)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Compares two strings in constant time to prevent timing attacks.
     * @param a First string
     * @param b Second string
     * @return true if strings are equal, false otherwise
     */
    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }

    /**
     * Creates a complete hash string in the format "salt:hash".
     * This is the format stored in DataStore.
     * 
     * @param pin The PIN to hash
     * @return Formatted string "salt:hash"
     */
    fun createPinHash(pin: String): String {
        val salt = generateSalt()
        val hash = hashPin(pin, salt)
        return "$salt:$hash"
    }

    /**
     * Checks if a stored value is a hashed PIN or plaintext.
     * Hashed PINs are in format "salt:hash" where both parts are Base64.
     * 
     * @param value The stored PIN value
     * @return true if it's a hashed PIN, false if it's plaintext
     */
    fun isHashedPin(value: String): Boolean {
        val parts = value.split(":")
        if (parts.size != 2) return false
        
        return try {
            // Try to decode both parts as Base64
            Base64.getDecoder().decode(parts[0])
            Base64.getDecoder().decode(parts[1])
            true
        } catch (e: Exception) {
            false
        }
    }
}
