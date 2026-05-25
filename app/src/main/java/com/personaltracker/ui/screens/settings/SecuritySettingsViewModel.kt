package com.personaltracker.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personaltracker.security.BiometricHelper
import com.personaltracker.security.PinManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SecuritySettingsState(
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val biometricAvailable: Boolean = false,
    val biometricEnabled: Boolean = false,
    val investmentPinSet: Boolean = false,
    val credentialsPinSet: Boolean = false,
    val selectedTimeout: SessionTimeout = SessionTimeout.FIVE_MINUTES
)

enum class SessionTimeout(val label: String, val minutes: Int) {
    ONE_MINUTE("1 minute", 1),
    FIVE_MINUTES("5 minutes", 5),
    FIFTEEN_MINUTES("15 minutes", 15),
    THIRTY_MINUTES("30 minutes", 30),
    NEVER("Never", -1)
}

@HiltViewModel
class SecuritySettingsViewModel @Inject constructor(
    private val pinManager: PinManager,
    private val biometricHelper: BiometricHelper
) : ViewModel() {

    private val _state = MutableStateFlow(SecuritySettingsState())
    val state: StateFlow<SecuritySettingsState> = _state.asStateFlow()

    init {
        loadSecurityState()
    }

    private fun loadSecurityState() {
        viewModelScope.launch {
            val biometricAvailable = biometricHelper.isBiometricAvailable()
            val biometricEnabled = pinManager.isBiometricEnabled()
            val investmentPinSet = pinManager.isModulePinSet(PinManager.PinModule.INVESTMENT)
            val credentialsPinSet = pinManager.isModulePinSet(PinManager.PinModule.CREDENTIALS)
            _state.value = _state.value.copy(
                biometricAvailable = biometricAvailable,
                biometricEnabled = biometricEnabled,
                investmentPinSet = investmentPinSet,
                credentialsPinSet = credentialsPinSet
            )
        }
    }

    /**
     * Changes the main application PIN.
     * @param currentPin The user's current PIN for verification.
     * @param newPin     The new PIN to set.
     */
    fun changeAppPin(currentPin: String, newPin: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            val verified = pinManager.verifyAppPin(currentPin)
            if (!verified) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = "Current PIN is incorrect"
                )
                return@launch
            }
            runCatching { pinManager.setAppPin(newPin) }
                .onSuccess {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        successMessage = "App PIN changed successfully"
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to change PIN: ${e.message}"
                    )
                }
        }
    }

    /**
     * Sets a separate PIN for a protected module (Investment vault or Credentials vault).
     * @param module The target [PinManager.PinModule].
     * @param pin    The PIN to assign to the module.
     */
    fun setModulePin(module: PinManager.PinModule, pin: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            runCatching { pinManager.setModulePin(module, pin) }
                .onSuccess {
                    val moduleName = when (module) {
                        PinManager.PinModule.INVESTMENT -> "Investment vault"
                        PinManager.PinModule.CREDENTIALS -> "Credentials vault"
                    }
                    val investmentPinSet = pinManager.isModulePinSet(PinManager.PinModule.INVESTMENT)
                    val credentialsPinSet = pinManager.isModulePinSet(PinManager.PinModule.CREDENTIALS)
                    _state.value = _state.value.copy(
                        isLoading = false,
                        successMessage = "$moduleName PIN set successfully",
                        investmentPinSet = investmentPinSet,
                        credentialsPinSet = credentialsPinSet
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to set module PIN: ${e.message}"
                    )
                }
        }
    }

    /**
     * Enables or disables biometric authentication.
     * @param enabled Whether biometric authentication should be active.
     */
    fun toggleBiometric(enabled: Boolean) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            runCatching { pinManager.setBiometricEnabled(enabled) }
                .onSuccess {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        biometricEnabled = enabled,
                        successMessage = if (enabled) "Biometric authentication enabled"
                        else "Biometric authentication disabled"
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to update biometric setting: ${e.message}"
                    )
                }
        }
    }

    fun setSessionTimeout(timeout: SessionTimeout) {
        _state.value = _state.value.copy(selectedTimeout = timeout)
    }

    fun clearMessages() {
        _state.value = _state.value.copy(successMessage = null, errorMessage = null)
    }
}
