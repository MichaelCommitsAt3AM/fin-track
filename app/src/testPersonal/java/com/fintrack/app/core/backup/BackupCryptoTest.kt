package com.fintrack.app.core.backup

import java.security.GeneralSecurityException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BackupCryptoTest {

    // Few iterations keep the suite fast; one test checks the production default.
    private val fast = 1_000
    private val pass = "correct horse battery".toCharArray()

    @Test
    fun `production iteration count is 310000`() {
        assertEquals(310_000, BackupCrypto.ITERATIONS)
        val dek = BackupCrypto.newDek()
        val wrapped = BackupCrypto.wrapDek(dek, pass)
        assertEquals(310_000, wrapped.iterations)
        assertArrayEquals(dek, BackupCrypto.unwrapDek(wrapped, pass))
    }

    @Test
    fun `wrap and unwrap round-trips the DEK`() {
        val dek = BackupCrypto.newDek()
        assertEquals(32, dek.size)
        val wrapped = BackupCrypto.wrapDek(dek, pass, fast)
        assertEquals(16, wrapped.salt.size)
        assertEquals(12, wrapped.iv.size)
        assertArrayEquals(dek, BackupCrypto.unwrapDek(wrapped, pass))
    }

    @Test
    fun `each wrap uses a fresh salt and iv`() {
        val dek = BackupCrypto.newDek()
        val a = BackupCrypto.wrapDek(dek, pass, fast)
        val b = BackupCrypto.wrapDek(dek, pass, fast)
        assertFalse(a.salt.contentEquals(b.salt))
        assertFalse(a.iv.contentEquals(b.iv))
        assertFalse(a.wrappedDek.contentEquals(b.wrappedDek))
    }

    @Test
    fun `wrong passphrase fails to unwrap`() {
        val wrapped = BackupCrypto.wrapDek(BackupCrypto.newDek(), pass, fast)
        try {
            BackupCrypto.unwrapDek(wrapped, "not the passphrase".toCharArray())
            fail("expected WrongPassphraseException")
        } catch (expected: WrongPassphraseException) {
        }
    }

    @Test
    fun `tampered wrapped key fails to unwrap`() {
        val wrapped = BackupCrypto.wrapDek(BackupCrypto.newDek(), pass, fast)
        val tampered = wrapped.wrappedDek.clone().also { it[0] = (it[0].toInt() xor 1).toByte() }
        try {
            BackupCrypto.unwrapDek(BackupCrypto.WrappedKey(fast, wrapped.salt, wrapped.iv, tampered), pass)
            fail("expected WrongPassphraseException")
        } catch (expected: WrongPassphraseException) {
        }
    }

    @Test
    fun `changing the passphrase only re-wraps the same DEK`() {
        val dek = BackupCrypto.newDek()
        val wrapped = BackupCrypto.wrapDek(dek, pass, fast)
        val newPass = "a different passphrase".toCharArray()
        val rewrapped = BackupCrypto.rewrapDek(wrapped, pass, newPass)
        assertArrayEquals(dek, BackupCrypto.unwrapDek(rewrapped, newPass))
        try {
            BackupCrypto.unwrapDek(rewrapped, pass)
            fail("old passphrase must stop working")
        } catch (expected: WrongPassphraseException) {
        }
    }

    @Test
    fun `record round-trips`() {
        val dek = BackupCrypto.newDek()
        val plaintext = """{"receipt":"QK87ABCD12","amount":150.0}""".toByteArray()
        val sealed = BackupCrypto.encryptRecord(dek, "doc-1", plaintext)
        assertEquals(12, sealed.iv.size)
        assertFalse(String(sealed.ciphertext, Charsets.ISO_8859_1).contains("QK87ABCD12"))
        assertArrayEquals(plaintext, BackupCrypto.decryptRecord(dek, "doc-1", sealed))
    }

    @Test
    fun `every record gets a fresh iv`() {
        val dek = BackupCrypto.newDek()
        val a = BackupCrypto.encryptRecord(dek, "doc", "same".toByteArray())
        val b = BackupCrypto.encryptRecord(dek, "doc", "same".toByteArray())
        assertFalse(a.iv.contentEquals(b.iv))
        assertFalse(a.ciphertext.contentEquals(b.ciphertext))
    }

    @Test
    fun `tampered ciphertext fails`() {
        val dek = BackupCrypto.newDek()
        val sealed = BackupCrypto.encryptRecord(dek, "doc-1", "secret".toByteArray())
        val bad = sealed.ciphertext.clone().also { it[0] = (it[0].toInt() xor 1).toByte() }
        try {
            BackupCrypto.decryptRecord(dek, "doc-1", BackupCrypto.Sealed(sealed.iv, bad))
            fail("expected GeneralSecurityException")
        } catch (expected: GeneralSecurityException) {
        }
    }

    @Test
    fun `record cannot be moved to another document (AAD is the doc id)`() {
        val dek = BackupCrypto.newDek()
        val sealed = BackupCrypto.encryptRecord(dek, "doc-1", "secret".toByteArray())
        try {
            BackupCrypto.decryptRecord(dek, "doc-2", sealed)
            fail("expected GeneralSecurityException")
        } catch (expected: GeneralSecurityException) {
        }
    }

    @Test
    fun `record under another DEK fails`() {
        val sealed = BackupCrypto.encryptRecord(BackupCrypto.newDek(), "doc-1", "secret".toByteArray())
        try {
            BackupCrypto.decryptRecord(BackupCrypto.newDek(), "doc-1", sealed)
            fail("expected GeneralSecurityException")
        } catch (expected: GeneralSecurityException) {
        }
    }

    @Test
    fun `document ids are deterministic per DEK and hide the input`() {
        val dek = BackupCrypto.newDek()
        val id = BackupCrypto.documentId(dek, "QK87ABCD12")
        assertEquals(id, BackupCrypto.documentId(dek, "QK87ABCD12"))
        assertNotEquals(id, BackupCrypto.documentId(dek, "QK87ABCD13"))
        assertNotEquals(id, BackupCrypto.documentId(BackupCrypto.newDek(), "QK87ABCD12"))
        assertFalse(id.contains("QK87ABCD12"))
        // base64url without padding: safe as a Firestore document id
        assertTrue(id.matches(Regex("[A-Za-z0-9_-]{43}")))
    }
}
