package com.fintrack.app.core.backup

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Keeps the backup DEK on this phone so the passphrase is not needed after every launch.
 *
 * The DEK is encrypted with an AES key generated *inside* the Android Keystore on this device
 * (never imported, non-exportable), and only the ciphertext is stored. A new phone has no such
 * key, so it needs the passphrase. Nothing here is uploaded.
 */
class BackupKeyCache(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun save(uid: String, dek: ByteArray) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keystoreKey())
        val ciphertext = cipher.doFinal(dek)
        prefs.edit()
            .putString(KEY_UID, uid)
            .putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString(KEY_CT, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
            .apply()
    }

    /** The cached DEK for [uid], or null when absent, for another account, or unreadable. */
    fun load(uid: String): ByteArray? {
        if (prefs.getString(KEY_UID, null) != uid) return null
        val iv = prefs.getString(KEY_IV, null)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return null
        val ct = prefs.getString(KEY_CT, null)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return null
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, keystoreKey(), GCMParameterSpec(128, iv))
            cipher.doFinal(ct)
        } catch (e: Exception) {
            null // keystore key lost (e.g. after a restore from a device backup): needs the passphrase
        }
    }

    fun clear() {
        prefs.edit().clear().apply()
        try {
            KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }.deleteEntry(ALIAS)
        } catch (e: Exception) {
            // nothing to delete
        }
    }

    private fun keystoreKey(): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "fintrack_mpesa_backup_cache"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val PREFS = "mpesa_backup_key_cache"
        const val KEY_UID = "uid"
        const val KEY_IV = "iv"
        const val KEY_CT = "ct"
    }
}
