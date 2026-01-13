package com.example.fintrack.core.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.example.fintrack.presentation.settings.notifications.NotificationPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.notificationDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "notification_preferences"
)

@Singleton
class NotificationPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferenceKeys {
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val BUDGET_ALERTS_ENABLED = booleanPreferencesKey("budget_alerts_enabled")
        val GOAL_UPDATES_ENABLED = booleanPreferencesKey("goal_updates_enabled")
        val DEBT_REMINDERS_ENABLED = booleanPreferencesKey("debt_reminders_enabled")
        val ACTIVITY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("activity_notifications_enabled")
    }

    val preferencesFlow: Flow<NotificationPreferences> = context.notificationDataStore.data
        .map { preferences ->
            NotificationPreferences(
                notificationsEnabled = preferences[PreferenceKeys.NOTIFICATIONS_ENABLED] ?: true,
                budgetAlertsEnabled = preferences[PreferenceKeys.BUDGET_ALERTS_ENABLED] ?: true,
                goalUpdatesEnabled = preferences[PreferenceKeys.GOAL_UPDATES_ENABLED] ?: true,
                debtRemindersEnabled = preferences[PreferenceKeys.DEBT_REMINDERS_ENABLED] ?: true,
                activityNotificationsEnabled = preferences[PreferenceKeys.ACTIVITY_NOTIFICATIONS_ENABLED] ?: true
            )
        }

    suspend fun updateNotificationsEnabled(enabled: Boolean) {
        context.notificationDataStore.edit { preferences ->
            preferences[PreferenceKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun updateBudgetAlertsEnabled(enabled: Boolean) {
        context.notificationDataStore.edit { preferences ->
            preferences[PreferenceKeys.BUDGET_ALERTS_ENABLED] = enabled
        }
    }

    suspend fun updateGoalUpdatesEnabled(enabled: Boolean) {
        context.notificationDataStore.edit { preferences ->
            preferences[PreferenceKeys.GOAL_UPDATES_ENABLED] = enabled
        }
    }

    suspend fun updateDebtRemindersEnabled(enabled: Boolean) {
        context.notificationDataStore.edit { preferences ->
            preferences[PreferenceKeys.DEBT_REMINDERS_ENABLED] = enabled
        }
    }

    suspend fun updateActivityNotificationsEnabled(enabled: Boolean) {
        context.notificationDataStore.edit { preferences ->
            preferences[PreferenceKeys.ACTIVITY_NOTIFICATIONS_ENABLED] = enabled
        }
    }
}
