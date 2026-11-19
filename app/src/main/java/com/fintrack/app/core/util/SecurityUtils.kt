package com.fintrack.app.core.util

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Security utilities for the FinTrack application.
 * Provides cryptographic functions for PIN hashing and verification.
 *
 * Stored formats:
 * - current: `v2$<iterations>$<salt>$<hash>`, PBKDF2-HMAC-SHA256
 * - legacy:  `<salt>:<hash>`, a single SHA-256 over PIN + salt. A 4-digit PIN hashed this way
 *   is recovered offline in ~10,000 hashes (milliseconds), so legacy hashes are verified once
 *   and then re-hashed (see [needsRehash]).
 */
object SecurityUtils {

    private const val LEGACY_HASH_ALGORITHM = "SHA-256"
    private const val KDF_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val V2_PREFIX = "v2$"
    private const val SALT_LENGTH = 16 // 16 bytes = 128 bits
    private const val KEY_LENGTH_BITS = 256

    /** Work factor for new hashes. Stored with each hash so it can be raised later. */
    const val PBKDF2_ITERATIONS = 120_000

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
     * Legacy single-round SHA-256 hash. Only used to verify hashes written before v2.
     */
    fun hashPin(pin: String, salt: String): String {
        val saltBytes = Base64.getDecoder().decode(salt)
        val combined = pin.toByteArray(Charsets.UTF_8) + saltBytes
        val hashBytes = MessageDigest.getInstance(LEGACY_HASH_ALGORITHM).digest(combined)
        return Base64.getEncoder().encodeToString(hashBytes)
    }

    private fun pbkdf2(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, KEY_LENGTH_BITS)
        try {
            return SecretKeyFactory.getInstance(KDF_ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    /**
     * Creates the string to store for [pin]: `v2$iterations$salt$hash`.
     */
    fun createPinHash(pin: String, iterations: Int = PBKDF2_ITERATIONS): String {
        val salt = ByteArray(SALT_LENGTH).also { SecureRandom().nextBytes(it) }
        val hash = pbkdf2(pin, salt, iterations)
        val encoder = Base64.getEncoder()
        return "$V2_PREFIX$iterations$${encoder.encodeToString(salt)}$${encoder.encodeToString(hash)}"
    }

    /**
     * Verifies a PIN against a stored hash (v2 or legacy) using constant-time comparison.
     *
     * @return true if the PIN is correct, false otherwise (including malformed input)
     */
    fun verifyPin(inputPin: String, storedHash: String): Boolean {
        return try {
            if (storedHash.startsWith(V2_PREFIX)) {
                val parts = storedHash.removePrefix(V2_PREFIX).split("$")
                if (parts.size != 3) return false
                val iterations = parts[0].toInt()
                val salt = Base64.getDecoder().decode(parts[1])
                val expected = Base64.getDecoder().decode(parts[2])
                MessageDigest.isEqual(pbkdf2(inputPin, salt, iterations), expected)
            } else {
                val parts = storedHash.split(":")
                if (parts.size != 2) return false
                val actual = Base64.getDecoder().decode(hashPin(inputPin, parts[0]))
                MessageDigest.isEqual(actual, Base64.getDecoder().decode(parts[1]))
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * True if [storedValue] should be replaced with a fresh [createPinHash] after the next
     * successful verification (legacy format, or fewer iterations than the current default).
     */
    fun needsRehash(storedValue: String): Boolean {
        if (!storedValue.startsWith(V2_PREFIX)) return true
        val iterations = storedValue.removePrefix(V2_PREFIX).substringBefore("$").toIntOrNull()
        return iterations == null || iterations < PBKDF2_ITERATIONS
    }

    /**
     * Checks if a stored value is a hashed PIN (v2 or legacy) rather than a plaintext PIN.
     */
    fun isHashedPin(value: String): Boolean {
        if (value.startsWith(V2_PREFIX)) return value.removePrefix(V2_PREFIX).split("$").size == 3
        val parts = value.split(":")
        if (parts.size != 2) return false

        return try {
            Base64.getDecoder().decode(parts[0])
            Base64.getDecoder().decode(parts[1])
            true
        } catch (e: Exception) {
            false
        }
    }
}
