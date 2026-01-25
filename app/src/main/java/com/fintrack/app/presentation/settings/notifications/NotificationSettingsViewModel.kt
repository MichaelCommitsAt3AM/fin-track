package com.fintrack.app.presentation.settings.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.app.core.data.preferences.NotificationPreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationPreferences(
    val notificationsEnabled: Boolean = true,
    val budgetAlertsEnabled: Boolean = true,
    val goalUpdatesEnabled: Boolean = true,
    val debtRemindersEnabled: Boolean = true,
    val activityNotificationsEnabled: Boolean = true
)

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val preferencesManager: NotificationPreferencesManager
) : ViewModel() {

    private val _preferences = MutableStateFlow(NotificationPreferences())
    val preferences: StateFlow<NotificationPreferences> = _preferences.asStateFlow()

    init {
        loadPreferences()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            preferencesManager.preferencesFlow.collect { prefs ->
                _preferences.value = prefs
            }
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            _preferences.value = _preferences.value.copy(notificationsEnabled = enabled)
            preferencesManager.updateNotificationsEnabled(enabled)
        }
    }

    fun toggleBudgetAlerts(enabled: Boolean) {
        viewModelScope.launch {
            _preferences.value = _preferences.value.copy(budgetAlertsEnabled = enabled)
            preferencesManager.updateBudgetAlertsEnabled(enabled)
        }
    }

    fun toggleGoalUpdates(enabled: Boolean) {
        viewModelScope.launch {
            _preferences.value = _preferences.value.copy(goalUpdatesEnabled = enabled)
            preferencesManager.updateGoalUpdatesEnabled(enabled)
        }
    }

    fun toggleDebtReminders(enabled: Boolean) {
        viewModelScope.launch {
            _preferences.value = _preferences.value.copy(debtRemindersEnabled = enabled)
            preferencesManager.updateDebtRemindersEnabled(enabled)
        }
    }

    fun toggleActivityNotifications(enabled: Boolean) {
        viewModelScope.launch {
            _preferences.value = _preferences.value.copy(activityNotificationsEnabled = enabled)
            preferencesManager.updateActivityNotificationsEnabled(enabled)
        }
    }
}
