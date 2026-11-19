package com.fintrack.app.core.backup

import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** The passphrase did not unwrap the backup key (or the stored key material was tampered with). */
class WrongPassphraseException : Exception("Wrong backup passphrase")

/**
 * Cryptography for the end-to-end encrypted M-Pesa backup. Pure JVM so it can be unit tested.
 *
 * Nothing here uses, or may ever use, the key embedded in the APK that the main cloud sync still
 * wraps its data key with (audit H2): everything is derived from a passphrase only the user knows.
 *
 * - KEK  = PBKDF2WithHmacSHA256(passphrase, 16 random salt bytes, 310_000 iterations, 256 bit)
 * - DEK  = 256-bit random AES key, wrapped by the KEK with AES-256-GCM (12-byte random IV)
 * - Records are AES-256-GCM under the DEK with a fresh IV each and AAD = document ID, so a
 *   record cannot be moved to another document.
 * - Document IDs = base64url(HMAC-SHA256(macKey, input)) so receipt numbers and merchant names
 *   never appear in the (plaintext) IDs; macKey is derived from the DEK (HKDF-Expand).
 *
 * Never log the passphrase, keys or plaintext.
 */
object BackupCrypto {
    const val VERSION = 1
    const val KDF = "PBKDF2-SHA256"
    const val ITERATIONS = 310_000
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val KEY_BYTES = 32
    private const val GCM_TAG_BITS = 128
    private const val DOC_ID_INFO = "fintrack-mpesa-docid"
    private val WRAP_AAD = "fintrack-mpesa-backup-v1/wrapped-dek".toByteArray(Charsets.UTF_8)
    private val random = SecureRandom()

    /** Key material stored in `users/{uid}/mpesa_backup/meta`. */
    class WrappedKey(
        val iterations: Int,
        val salt: ByteArray,
        val iv: ByteArray,
        val wrappedDek: ByteArray
    )

    /** One encrypted record: stored as `{iv, ciphertext, v}` (base64). */
    class Sealed(val iv: ByteArray, val ciphertext: ByteArray)

    fun newDek(): ByteArray = ByteArray(KEY_BYTES).also { random.nextBytes(it) }

    fun deriveKek(passphrase: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(passphrase, salt, iterations, KEY_BYTES * 8)
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    fun wrapDek(dek: ByteArray, passphrase: CharArray, iterations: Int = ITERATIONS): WrappedKey {
        val salt = ByteArray(SALT_BYTES).also { random.nextBytes(it) }
        val iv = ByteArray(IV_BYTES).also { random.nextBytes(it) }
        val kek = deriveKek(passphrase, salt, iterations)
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(kek, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
            cipher.updateAAD(WRAP_AAD)
            return WrappedKey(iterations, salt, iv, cipher.doFinal(dek))
        } finally {
            kek.fill(0)
        }
    }

    /** @throws WrongPassphraseException when the passphrase is wrong or the data was altered */
    fun unwrapDek(wrapped: WrappedKey, passphrase: CharArray): ByteArray {
        val kek = deriveKek(passphrase, wrapped.salt, wrapped.iterations)
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(kek, "AES"), GCMParameterSpec(GCM_TAG_BITS, wrapped.iv))
            cipher.updateAAD(WRAP_AAD)
            val dek = cipher.doFinal(wrapped.wrappedDek)
            if (dek.size != KEY_BYTES) throw WrongPassphraseException()
            return dek
        } catch (e: GeneralSecurityException) {
            throw WrongPassphraseException()
        } finally {
            kek.fill(0)
        }
    }

    /** Changing the passphrase only re-wraps the same DEK; no record is re-encrypted. */
    fun rewrapDek(wrapped: WrappedKey, oldPassphrase: CharArray, newPassphrase: CharArray): WrappedKey {
        val dek = unwrapDek(wrapped, oldPassphrase)
        try {
            return wrapDek(dek, newPassphrase, wrapped.iterations.coerceAtLeast(ITERATIONS))
        } finally {
            dek.fill(0)
        }
    }

    fun encryptRecord(dek: ByteArray, docId: String, plaintext: ByteArray): Sealed {
        val iv = ByteArray(IV_BYTES).also { random.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(dek, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD(docId.toByteArray(Charsets.UTF_8))
        return Sealed(iv, cipher.doFinal(plaintext))
    }

    /** @throws GeneralSecurityException when the ciphertext, IV or [docId] (AAD) don't match */
    fun decryptRecord(dek: ByteArray, docId: String, sealed: Sealed): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(dek, "AES"), GCMParameterSpec(GCM_TAG_BITS, sealed.iv))
        cipher.updateAAD(docId.toByteArray(Charsets.UTF_8))
        return cipher.doFinal(sealed.ciphertext)
    }

    /**
     * Deterministic, non-reversible document ID for [input] (e.g. a receipt number) under [dek].
     * HKDF-Expand with a single block: macKey = HMAC(dek, info || 0x01).
     */
    fun documentId(dek: ByteArray, input: String): String {
        val macKey = hmac(dek, DOC_ID_INFO.toByteArray(Charsets.UTF_8) + byteArrayOf(1))
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hmac(macKey, input.toByteArray(Charsets.UTF_8)))
        } finally {
            macKey.fill(0)
        }
    }

    private fun hmac(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    // --- base64 helpers for Firestore fields ---
    fun b64(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)
    fun unb64(text: String): ByteArray = Base64.getDecoder().decode(text)
}
