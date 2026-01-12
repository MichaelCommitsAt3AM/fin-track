package com.example.fintrack.core.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.fintrack.core.domain.model.Currency
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "auth_prefs")

@Singleton
class LocalAuthManager @Inject constructor(@ApplicationContext private val context: Context) {

    private val IS_BIOMETRIC_ENABLED = booleanPreferencesKey("is_biometric_enabled")
    private val USER_PIN = stringPreferencesKey("user_pin_hash") // In real app, hash this!
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

    suspend fun setUserPin(pin: String) {
        context.dataStore.edit { it[USER_PIN] = pin }
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
