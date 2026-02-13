package com.fintrack.app.core.security

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.whenever
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class EncryptionManagerTest {

    private lateinit var keyRepository: KeyRepository
    private lateinit var encryptionManager: EncryptionManager
    private lateinit var secretKey: SecretKey

    @Before
    fun setup() {
        keyRepository = mock(KeyRepository::class.java)
        encryptionManager = EncryptionManagerImpl(keyRepository)

        // Generate a real AES key for testing
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        secretKey = keyGen.generateKey()

        runBlocking {
            whenever(keyRepository.getOrGenerateMasterKey()).thenReturn(secretKey)
        }
    }

    @Test
    fun `encrypt adds prefix and produces valid output`() = runBlocking {
        val original = "Secret Data"
        val encrypted = encryptionManager.encrypt(original)
        
        assertTrue(encrypted.startsWith("ENC[v1]:"))
        assertNotEquals(original, encrypted)
    }

    @Test
    fun `decrypt returns original data`() = runBlocking {
        val original = "Sensitive Financial Info"
        val encrypted = encryptionManager.encrypt(original)
        val decrypted = encryptionManager.decrypt(encrypted)
        
        assertEquals(original, decrypted)
    }

    @Test
    fun `encrypt produces different outputs for same input (Random IV)`() = runBlocking {
        val original = "Same Data"
        val encrypted1 = encryptionManager.encrypt(original)
        val encrypted2 = encryptionManager.encrypt(original)
        
        assertNotEquals(encrypted1, encrypted2)
        
        val decrypted1 = encryptionManager.decrypt(encrypted1)
        val decrypted2 = encryptionManager.decrypt(encrypted2)
        
        assertEquals(original, decrypted1)
        assertEquals(original, decrypted2)
    }
    
    @Test
    fun `decrypt returns legacy data as is if no prefix`() = runBlocking {
        val legacyData = "100.50"
        val decrypted = encryptionManager.decrypt(legacyData)
        
        assertEquals(legacyData, decrypted)
    }
    
    @Test
    fun `encrypt empty string returns empty string`() = runBlocking {
        val result = encryptionManager.encrypt("")
        assertEquals("", result)
    }
    
    @Test
    fun `decrypt empty string returns empty string`() = runBlocking {
        val result = encryptionManager.decrypt("")
        assertEquals("", result)
    }
}
