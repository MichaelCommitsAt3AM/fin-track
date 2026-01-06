package com.example.fintrack.core.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "auth_prefs")

@Singleton
class LocalAuthManager @Inject constructor(@ApplicationContext private val context: Context) {

    private val IS_BIOMETRIC_ENABLED = booleanPreferencesKey("is_biometric_enabled")
    private val USER_PIN = stringPreferencesKey("user_pin_hash") // In real app, hash this!

    val isBiometricEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[IS_BIOMETRIC_ENABLED] ?: false }

    val userPin: Flow<String?> = context.dataStore.data
        .map { it[USER_PIN] }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[IS_BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun setUserPin(pin: String) {
        context.dataStore.edit { it[USER_PIN] = pin }
    }
}