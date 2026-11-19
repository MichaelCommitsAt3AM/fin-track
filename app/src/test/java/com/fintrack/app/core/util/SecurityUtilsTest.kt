package com.fintrack.app.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityUtilsTest {

    @Test
    fun `new hashes use versioned PBKDF2 format with stored iterations`() {
        val stored = SecurityUtils.createPinHash("1234")
        val parts = stored.split("$")

        assertEquals("v2", parts[0])
        assertEquals(SecurityUtils.PBKDF2_ITERATIONS.toString(), parts[1])
        assertEquals(4, parts.size)
        assertTrue(SecurityUtils.isHashedPin(stored))
        assertFalse(SecurityUtils.needsRehash(stored))
    }

    @Test
    fun `v2 hash verifies the right PIN and rejects others`() {
        val stored = SecurityUtils.createPinHash("4821", iterations = 1_000)

        assertTrue(SecurityUtils.verifyPin("4821", stored))
        assertFalse(SecurityUtils.verifyPin("4820", stored))
        assertFalse(SecurityUtils.verifyPin("", stored))
    }

    @Test
    fun `same PIN gets a different salt and hash each time`() {
        assertNotEquals(SecurityUtils.createPinHash("1111", 1_000), SecurityUtils.createPinHash("1111", 1_000))
    }

    @Test
    fun `legacy salt-colon-hash still verifies and is flagged for rehash`() {
        val salt = SecurityUtils.generateSalt()
        val legacy = "$salt:${SecurityUtils.hashPin("2580", salt)}"

        assertTrue(SecurityUtils.isHashedPin(legacy))
        assertTrue(SecurityUtils.verifyPin("2580", legacy))
        assertFalse(SecurityUtils.verifyPin("0000", legacy))
        assertTrue(SecurityUtils.needsRehash(legacy))
    }

    @Test
    fun `hash with fewer iterations than current is flagged for rehash`() {
        assertTrue(SecurityUtils.needsRehash(SecurityUtils.createPinHash("1234", iterations = 1_000)))
    }

    @Test
    fun `plaintext and malformed values are handled safely`() {
        assertFalse(SecurityUtils.isHashedPin("1234"))
        assertTrue(SecurityUtils.needsRehash("1234"))
        assertFalse(SecurityUtils.verifyPin("1234", "1234"))
        assertFalse(SecurityUtils.verifyPin("1234", "v2\$notanumber\$xx\$yy"))
        assertFalse(SecurityUtils.verifyPin("1234", "v2\$1000\$onlytwo"))
    }
}
