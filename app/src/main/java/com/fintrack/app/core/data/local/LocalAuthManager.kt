package com.fintrack.app.core.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fintrack.app.core.domain.model.Currency
import com.fintrack.app.core.util.SecurityUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "auth_prefs")

@Singleton
class LocalAuthManager @Inject constructor(@ApplicationContext private val context: Context) {

    private val IS_BIOMETRIC_ENABLED = booleanPreferencesKey("is_biometric_enabled")
    private val USER_PIN = stringPreferencesKey("user_pin_hashed") // Stores hashed PIN in format "salt:hash"
    private val THEME_PREFERENCE = stringPreferencesKey("theme_preference")
    private val CURRENCY_PREFERENCE = stringPreferencesKey("currency_preference")

    val isBiometricEnabled: Flow<Boolean> =
            context.dataStore.data.map { it[IS_BIOMETRIC_ENABLED] ?: false }

    val userPin: Flow<String?> = context.dataStore.data.map { it[USER_PIN] }

    val themePreference: Flow<String> =
            context.dataStore.data.map { it[THEME_PREFERENCE] ?: "Dark" }

    val currencyPreference: Flow<Currency> =
            context.dataStore.data.map {
                it[CURRENCY_PREFERENCE]?.let { name -> Currency.fromName(name) } ?: Currency.KSH
            }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[IS_BIOMETRIC_ENABLED] = enabled }
    }

    /**
     * Sets and securely hashes the user's PIN.
     * The PIN is salted and hashed using SHA-256 before storage.
     * 
     * @param pin The plaintext PIN (should be validated before calling this)
     */
    suspend fun setUserPin(pin: String) {
        val hashedPin = SecurityUtils.createPinHash(pin)
        context.dataStore.edit { it[USER_PIN] = hashedPin }
    }

    /**
     * Verifies if the provided PIN matches the stored hash.
     * Automatically migrates plaintext PINs to hashed format on first verification.
     * 
     * @param inputPin The PIN entered by the user
     * @return true if PIN is correct, false otherwise
     */
    suspend fun verifyPin(inputPin: String): Boolean {
        val storedPin = context.dataStore.data.first()[USER_PIN] ?: return false
        
        // Check if stored PIN is in plaintext (legacy format) and migrate if needed
        if (!SecurityUtils.isHashedPin(storedPin)) {
            // Legacy plaintext PIN - migrate to hashed format
            if (storedPin == inputPin) {
                // Correct PIN, hash and store it
                setUserPin(inputPin)
                return true
            }
            return false
        }
        
        // Modern hashed PIN - verify using constant-time comparison
        return SecurityUtils.verifyPin(inputPin, storedPin)
    }

    /**
     * Checks if a PIN has been set.
     * @return true if PIN exists (hashed or plaintext), false otherwise
     */
    suspend fun hasPinSet(): Boolean {
        return context.dataStore.data.first()[USER_PIN] != null
    }

    suspend fun setThemePreference(theme: String) {
        context.dataStore.edit { it[THEME_PREFERENCE] = theme }
    }

    suspend fun setCurrencyPreference(currency: Currency) {
        context.dataStore.edit { it[CURRENCY_PREFERENCE] = currency.name }
    }

    /**
     * Clears all local authentication data (PIN and biometric settings). Should be called when user
     * logs out to ensure clean state.
     */
    suspend fun clearLocalAuth() {
        context.dataStore.edit { preferences ->
            preferences.remove(USER_PIN)
            preferences.remove(IS_BIOMETRIC_ENABLED)
        }
    }
}
