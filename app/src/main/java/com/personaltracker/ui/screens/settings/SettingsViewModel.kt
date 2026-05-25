package com.personaltracker.ui.screens.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personaltracker.security.PinManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val biometricEnabled: Boolean = false,
    val expenseReminderEnabled: Boolean = true,
    val emiReminderEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val pinManager: PinManager,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    companion object {
        val EXPENSE_REMINDER_KEY = booleanPreferencesKey("expense_reminder_enabled")
        val EMI_REMINDER_KEY = booleanPreferencesKey("emi_reminder_enabled")
    }

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    val biometricEnabled: StateFlow<Boolean> = dataStore.data
        .map { prefs -> prefs[booleanPreferencesKey("biometric_enabled")] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val expenseReminderEnabled: StateFlow<Boolean> = dataStore.data
        .map { prefs -> prefs[EXPENSE_REMINDER_KEY] ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val emiReminderEnabled: StateFlow<Boolean> = dataStore.data
        .map { prefs -> prefs[EMI_REMINDER_KEY] ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun toggleBiometric(enabled: Boolean) {
        viewModelScope.launch {
            pinManager.setBiometricEnabled(enabled)
        }
    }

    fun toggleExpenseReminder(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { prefs -> prefs[EXPENSE_REMINDER_KEY] = enabled }
            _state.value = _state.value.copy(
                message = if (enabled) "Expense reminder enabled" else "Expense reminder disabled"
            )
        }
    }

    fun toggleEmiReminder(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { prefs -> prefs[EMI_REMINDER_KEY] = enabled }
            _state.value = _state.value.copy(
                message = if (enabled) "EMI reminder enabled" else "EMI reminder disabled"
            )
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }
}
