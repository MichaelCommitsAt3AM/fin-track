package com.fintrack.app.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import com.fintrack.app.BuildConfig
import com.fintrack.app.core.profile.SyncGate
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeyRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val syncGate: SyncGate
) : KeyRepository {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val MASTER_KEY_ALIAS_PREFIX = "fintrack_master_key_"
        private const val KEY_SIZE = 256
        private const val TAG = "KeyRepository"
        
        // This is the App KEK (Key Encryption Key). 
        // In a real production app, this should be more obfuscated (NDK, etc).
        // For this implementation, we use a constant derived from BuildConfig or a static string.
        // The key must be 32 bytes for AES-256.
        // We use a simple derivation for now to ensure it's reproducible across devices.
        private val APP_KEK_BYTES = "FinTrackSuperSecretKeyForDEKWait".toByteArray().copyOf(32) // Ensure 32 bytes
    }

    // Only the cloud account (sync on) may use the cloud-wrapped key: local-only users never
    // reach Firestore for a key.
    override fun currentUserId(): String? = syncGate.cloudUidOrNull()

    override suspend fun getOrGenerateMasterKey(): SecretKey {
        val userId = currentUserId() ?: throw IllegalStateException("User not logged in")
        val keyAlias = "$MASTER_KEY_ALIAS_PREFIX$userId"

        // 1. Try to get from Android Keystore (Local Cache)
        val cachedKey = getKeyFromKeystore(keyAlias)
        if (cachedKey != null) {
            Log.d(TAG, "Master key found in Keystore")
            return cachedKey
        }

        // 2. Try to fetch from Firestore (Cloud Source of Truth)
        val encryptedKeyDoc = firestore.collection("users")
            .document(userId)
            .collection("keys")
            .document("master")
            .get()
            .await()

        if (encryptedKeyDoc.exists()) {
            val encryptedDekBase64 = encryptedKeyDoc.getString("encryptedDek")
            val ivBase64 = encryptedKeyDoc.getString("iv")
            
            if (encryptedDekBase64 != null && ivBase64 != null) {
                try {
                    Log.d(TAG, "Master key found in Firestore, decrypting...")
                    val dek = decryptDekWithKek(encryptedDekBase64, ivBase64)
                    saveKeyToKeystore(dek, keyAlias)
                    return dek
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to decrypt DEK from Firestore", e)
                    // Fallthrough to generate new key? 
                    // DANGER: If we generate a new key, we lose access to old data.
                    // For now, re-throw to prevent data loss or corruption.
                    throw IllegalStateException("Failed to decrypt Master Key. Data may be inaccessible.", e)
                }
            }
        }

        // 3. First time user (or key fetch failed/missing): Generate NEW Master Key
        Log.d(TAG, "No Master Key found. Generating new one.")
        val newDek = generateNewAesKey()
        
        // Encrypt DEK with KEK
        val (encryptedDek, iv) = encryptDekWithKek(newDek)
        
        // Upload to Firestore
        val keyData = hashMapOf(
            "encryptedDek" to Base64.encodeToString(encryptedDek, Base64.NO_WRAP),
            "iv" to Base64.encodeToString(iv, Base64.NO_WRAP),
            "createdAt" to System.currentTimeMillis()
        )
        
        firestore.collection("users")
            .document(userId)
            .collection("keys")
            .document("master")
            .set(keyData)
            .await()
            
        // Save to Keystore for future local use
        saveKeyToKeystore(newDek, keyAlias)
        
        return newDek
    }

    private fun getKeyFromKeystore(alias: String): SecretKey? {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            if (keyStore.containsAlias(alias)) {
                val entry = keyStore.getEntry(alias, null)
                if (entry is KeyStore.SecretKeyEntry) {
                    return entry.secretKey
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading from Keystore", e)
        }
        return null
    }

    private fun saveKeyToKeystore(secretKey: SecretKey, alias: String) {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            
            val protectionParam = KeyStore.PasswordProtection(null)
            val entry = KeyStore.SecretKeyEntry(secretKey)
            keyStore.setEntry(alias, entry, protectionParam)
        } catch (e: Exception) {
             // If direct saving fails (often issues with "Imported" keys not being supported in TEE securely without wrapping), 
             // we might need a more complex KeyStore wrapper or just store in SharedPreferences encrypted (which is what we are avoiding).
             // However, Android Keystore supports importing RAW AES keys since API 23.
             // But usually for "secure hardware", it prefers generating them inside.
             // Since we generated it outside (or downloaded it), we import it.
             Log.w(TAG, "Could not save key to AndroidKeyStore directly. Falling back to memory-only for session.", e)
             // Note: In a robust app, we would wrap this key with a KeyStore-generated KeyPair and save the blob to Prefs.
             // For simplicity in this implementation, if Keystore fails, we just keep it in memory (it will be fetched again next launch).
        }
    }

    private fun generateNewAesKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(KEY_SIZE)
        return keyGen.generateKey()
    }

    // --- KEK Logic (App Static Key) ---

    // KEK is compatible with AES/GCM/NoPadding
    private val kekSpec = SecretKeySpec(APP_KEK_BYTES, "AES")

    private fun encryptDekWithKek(dek: SecretKey): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, kekSpec)
        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(dek.encoded)
        return Pair(encryptedBytes, iv)
    }

    private fun decryptDekWithKek(encryptedDekBase64: String, ivBase64: String): SecretKey {
        val encryptedBytes = Base64.decode(encryptedDekBase64, Base64.NO_WRAP)
        val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
        
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, kekSpec, spec)
        
        val decodedKeyBytes = cipher.doFinal(encryptedBytes)
        return SecretKeySpec(decodedKeyBytes, "AES")
    }
}
