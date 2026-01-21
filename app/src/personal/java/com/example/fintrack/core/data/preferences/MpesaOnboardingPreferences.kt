package com.example.fintrack.core.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.onboardingDataStore by preferencesDataStore(name = "mpesa_onboarding")

/**
 * DataStore manager for M-Pesa onboarding preferences.
 * Tracks onboarding completion status and user preferences.
 */
@Singleton
class MpesaOnboardingPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private val ONBOARDING_COMPLETED = booleanPreferencesKey("mpesa_onboarding_completed")
        private val LOOKBACK_PERIOD_MONTHS = intPreferencesKey("mpesa_lookback_months")
        private val REAL_TIME_ENABLED = booleanPreferencesKey("mpesa_realtime_enabled")
    }
    
    /**
     * Flow indicating whether M-Pesa onboarding has been completed.
     */
    val isOnboardingCompleted: Flow<Boolean> = 
        context.onboardingDataStore.data.map { preferences ->
            preferences[ONBOARDING_COMPLETED] ?: false
        }
    
    /**
     * Flow of the selected lookback period in months (1, 3, 6, or 12).
     * Default: 3 months
     */
    val lookbackPeriodMonths: Flow<Int> = 
        context.onboardingDataStore.data.map { preferences ->
            preferences[LOOKBACK_PERIOD_MONTHS] ?: 3
        }
    
    /**
     * Flow indicating whether real-time SMS processing is enabled.
     * Default: true
     */
    val isRealTimeEnabled: Flow<Boolean> = 
        context.onboardingDataStore.data.map { preferences ->
            preferences[REAL_TIME_ENABLED] ?: true
        }
    
    /**
     * Mark onboarding as complete.
     */
    suspend fun markOnboardingComplete() {
        context.onboardingDataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED] = true
        }
    }
    
    /**
     * Set the lookback period for SMS scanning.
     * @param months Number of months to look back (1, 3, 6, or 12)
     */
    suspend fun setLookbackPeriod(months: Int) {
        require(months in listOf(1, 3, 6, 12)) { 
            "Lookback period must be 1, 3, 6, or 12 months" 
        }
        context.onboardingDataStore.edit { preferences ->
            preferences[LOOKBACK_PERIOD_MONTHS] = months
        }
    }
    
    /**
     * Enable or disable real-time SMS processing.
     */
    suspend fun setRealTimeEnabled(enabled: Boolean) {
        context.onboardingDataStore.edit { preferences ->
            preferences[REAL_TIME_ENABLED] = enabled
        }
    }
    
    /**
     * Reset onboarding state (for testing or user re-configuration).
     */
    suspend fun resetOnboarding() {
        context.onboardingDataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
